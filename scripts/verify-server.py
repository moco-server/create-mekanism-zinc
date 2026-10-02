#!/usr/bin/env python3
"""Build an isolated NeoForge server, clean-boot the production jar, and verify disk persistence."""
from __future__ import annotations

import hashlib
import os
import re
import shutil
import subprocess
import sys
import threading
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RUNTIME_NAME = "template-migration-legacy-server" if len(sys.argv) > 1 and sys.argv[1] == "migration" else "template-migration-server"
RUNTIME = ROOT / ".local" / "verification" / RUNTIME_NAME
WORLD_NAME = "cmz-template-migration"
SERVER_PORT = 25566
INSTALLER_URL = "https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.252/neoforge-21.1.252-installer.jar"
INSTALLER_SHA512 = "11addafebd1dbb583252230c7ff97bf80c8b5f6d980a6ae8ffd409ba302e8beaee87e51c35fd8daf8a589b1c9fce9845dd0406a7b7113185ad349748a3461de5"
EULA_SOURCE = Path(os.environ.get("CMZ_EULA_FILE", ROOT / ".local" / "eula.txt"))
GAME_DIR = Path(os.environ.get("CMZ_GAME_DIR", RUNTIME / "instance"))
JAVA = Path(os.environ.get("JAVA_HOME", "")) / "bin" / "java" if os.environ.get("JAVA_HOME") else Path("java")
GRADLE_HOME = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
DEPENDENCIES = {
    "create-UjX6dr61.jar": ("maven.modrinth/create/UjX6dr61", "11cc8fc049d2f67f6548c7abfada6b82a3adb5c7ca410a742de04bbca76e03862c518721b88d806f6e6d768a4d68531fdb903a85859b25d1484d550cc7bafd4b"),
    "mekanism-5KzzycBT.jar": ("maven.modrinth/mekanism/5KzzycBT", "66745825330a98f3e4a5ea3a44aff8b00870f715c144edc38dd2f61b4240589600b58ae89efac3b54dbb5aa430b1059dac1a28fd71cac5cc9002bbeb5ba3f22b"),
}


# 配布KFF。Maven aggregateではなく、language providerを含む作者配布物を使う。
KFF_URL = "https://cdn.modrinth.com/data/ordsPcFz/versions/tdvjylfz/kotlinforforge-5.5.0-all.jar"
KFF_SHA512 = "4544a80300cac93ce92b44b2b73a386e6e568d7dfc0744dfb98b1f6c6cc3479cece8630155a2887e49b4120ebdb90441ec15a2e053f2d2d31fd58d28d1abb7b4"
OWNERSHIP_TEXT = "Private disposable Create Mekanism Zinc verification runtime.\n"


def mod_version(root: Path) -> str:
    values = [line.partition("=")[2].strip() for line in (root / "gradle.properties").read_text(encoding="utf-8").splitlines()
              if line.partition("=")[0].strip() == "mod_version"]
    if len(values) != 1:
        raise ValueError("Exactly one mod_version property is required")
    validate_version(values[0])
    return values[0]


def validate_version(version: str) -> None:
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._+-]*", version):
        raise ValueError(f"Invalid mod_version: {version!r}")


def checked_artifact(path: Path) -> Path:
    if path.is_symlink():
        raise ValueError(f"Artifact must not be a symlink: {path}")
    if not path.is_file():
        raise FileNotFoundError(f"Build artifact first: {path}")
    return path


def mod_artifact(root: Path, version: str, fixture: bool) -> Path:
    validate_version(version)
    suffix = "-gametests" if fixture else ""
    return checked_artifact(root / "neoforge/build/libs" / f"create-mekanism-zinc-{version}{suffix}.jar")


def ensure_owned_runtime(root: Path, runtime: Path, game_dir: Path) -> None:
    base = root.resolve() / ".local/verification"
    if runtime.resolve().parent != base or game_dir.resolve() != runtime.resolve() / "instance":
        raise ValueError("Runtime must be directly under .local/verification with a direct instance directory")
    for path in (runtime, game_dir, runtime / "neoforge-install", runtime / "logs",
                 runtime / ".cmz-owned-runtime", game_dir / "mods", game_dir / "libraries"):
        if path.is_symlink():
            raise ValueError(f"Owned runtime path must not be a symlink: {path}")
    runtime.mkdir(parents=True, exist_ok=True)
    marker = runtime / ".cmz-owned-runtime"
    if marker.exists():
        if not marker.is_file() or marker.read_text(encoding="utf-8") != OWNERSHIP_TEXT:
            raise ValueError(f"Unrecognized runtime ownership marker: {marker}")
    else:
        if any(runtime.iterdir()):
            raise ValueError(f"Refusing existing unmarked runtime directory: {runtime}")
        marker.write_text(OWNERSHIP_TEXT, encoding="utf-8")


def verify_hash(path: Path, expected: str) -> None:
    actual = sha512(path)
    if actual != expected:
        raise ValueError(f"SHA-512 mismatch for {path.name}: {actual}")


def download_verified(url: str, expected: str, destination: Path) -> None:
    if destination.is_file() and sha512(destination) == expected:
        return
    temporary = destination.with_suffix(destination.suffix + ".part")
    if destination.is_symlink() or temporary.is_symlink():
        raise ValueError(f"Download target must not be a symlink: {destination}")
    try:
        with urllib.request.urlopen(url, timeout=60) as response, temporary.open("wb") as output:
            shutil.copyfileobj(response, output)
        verify_hash(temporary, expected)
        temporary.replace(destination)
    finally:
        temporary.unlink(missing_ok=True)


def sha512(path: Path) -> str:
    digest = hashlib.sha512()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def cached_dependency(rel: str, expected: str) -> Path:
    matches = list((GRADLE_HOME / "caches/modules-2/files-2.1" / rel).glob("*/*.jar"))
    for candidate in matches:
        if sha512(candidate) == expected:
            return candidate
    raise RuntimeError(f"Pinned Gradle cache artifact missing or hash mismatch: {rel}; checked {len(matches)} candidates")


def prepare(production: Path | None = None, with_kff: bool = True) -> None:
    version = subprocess.run([str(JAVA), "-version"], check=True, capture_output=True, text=True)
    if 'version "21' not in version.stderr:
        raise RuntimeError(f"Java 21 is required for server verification; selected executable: {JAVA}")
    ensure_owned_runtime(ROOT, RUNTIME, GAME_DIR)
    addon = checked_artifact(production) if production is not None else mod_artifact(ROOT, mod_version(ROOT), False)
    install = RUNTIME / "neoforge-install"
    install.mkdir(exist_ok=True)
    installer = RUNTIME / "neoforge-21.1.252-installer.jar"
    download_verified(INSTALLER_URL, INSTALLER_SHA512, installer)
    actual = sha512(installer)
    if not (install / "run.sh").exists():
        with (install / "installer.log").open("w", encoding="utf-8") as log:
            subprocess.run([str(JAVA), "-jar", str(installer), "--installServer"], cwd=install,
                           stdout=log, stderr=subprocess.STDOUT, check=True, timeout=600)
    if not EULA_SOURCE.is_file() or "eula=true" not in EULA_SOURCE.read_text(encoding="utf-8"):
        raise RuntimeError(f"Existing accepted eula=true file required; not found at {EULA_SOURCE}")
    GAME_DIR.mkdir(parents=True, exist_ok=True)
    shutil.copy2(EULA_SOURCE, GAME_DIR / "eula.txt")
    shutil.copy2(install / "run.sh", GAME_DIR / "run.sh")
    shutil.copy2(install / "user_jvm_args.txt", GAME_DIR / "user_jvm_args.txt")
    library_copy = GAME_DIR / "libraries"
    if library_copy.exists():
        shutil.rmtree(library_copy)
    shutil.copytree(install / "libraries", library_copy)
    (GAME_DIR / "run.sh").chmod(0o755)
    (GAME_DIR / "server.properties").write_text(
        f"server-ip=127.0.0.1\nserver-port={SERVER_PORT}\nonline-mode=false\nlevel-name={WORLD_NAME}\n"
        "level-type=minecraft:normal\ngamemode=creative\ndifficulty=peaceful\nspawn-protection=0\nview-distance=5\nsimulation-distance=5\n",
        encoding="utf-8",
    )
    mods = GAME_DIR / "mods"
    mods.mkdir(exist_ok=True)
    for filename, (rel, expected) in DEPENDENCIES.items():
        source = cached_dependency(rel, expected)
        target = mods / filename
        shutil.copy2(source, target)
        if sha512(target) != expected:
            raise RuntimeError(f"Copy hash mismatch for {filename}")
    for old in mods.glob("kotlinforforge-*.jar"):
        old.unlink()
    if with_kff:
        kff = RUNTIME / "kotlinforforge-5.5.0-all.jar"
        download_verified(KFF_URL, KFF_SHA512, kff)
        shutil.copy2(kff, mods / kff.name)
        verify_hash(mods / kff.name, KFF_SHA512)
    for old in mods.glob("create-mekanism-zinc-*.jar"):
        old.unlink()
    shutil.copy2(addon, mods / addon.name)
    print(f"PREPARE_PASS installer_sha512={actual} game_dir={GAME_DIR}", flush=True)


class Server:
    def __init__(self, include_fixture: bool, fixture: Path | None = None):
        self.logs: list[str] = []
        self.lock = threading.Lock()
        self.process: subprocess.Popen[str] | None = None
        self.include_fixture = include_fixture
        self.fixture = fixture

    def start(self) -> None:
        mods = GAME_DIR / "mods"
        if self.include_fixture:
            fixture = checked_artifact(self.fixture) if self.fixture is not None else mod_artifact(ROOT, mod_version(ROOT), True)
            shutil.copy2(fixture, mods / fixture.name)
        else:
            for fixture in mods.glob("*gametests.jar"):
                fixture.unlink()
        server_env = dict(os.environ)
        if os.environ.get("JAVA_HOME"):
            server_env["JAVA_HOME"] = os.environ["JAVA_HOME"]
            server_env["PATH"] = str(JAVA.parent) + os.pathsep + server_env.get("PATH", "")
        self.process = subprocess.Popen([str(JAVA), "@user_jvm_args.txt",
                                         "@libraries/net/neoforged/neoforge/21.1.252/unix_args.txt", "nogui"],
                                        cwd=GAME_DIR, stdin=subprocess.PIPE,
                                        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1,
                                        env=server_env)
        print(f"SERVER_JVM_START pid={self.process.pid} fixture={self.include_fixture} world={GAME_DIR / WORLD_NAME}", flush=True)
        def read_output() -> None:
            assert self.process and self.process.stdout
            log_dir = RUNTIME / "logs"
            log_dir.mkdir(exist_ok=True)
            log_path = log_dir / f"server-{self.process.pid}-{time.time_ns()}.log"
            with log_path.open("w", encoding="utf-8") as log:
                for line in self.process.stdout:
                    with self.lock:
                        self.logs.append(line.rstrip())
                    log.write(line)
                    log.flush()
                    print(line, end="", flush=True)
        threading.Thread(target=read_output, daemon=True).start()
        self.wait_for("Done (", 300)

    def wait_for(self, marker: str, timeout: int) -> None:
        end = time.monotonic() + timeout
        while time.monotonic() < end:
            if self.process and self.process.poll() is not None:
                raise RuntimeError(f"Server exited with {self.process.returncode} before {marker}")
            with self.lock:
                if any(marker in line for line in self.logs):
                    return
            time.sleep(0.2)
        raise TimeoutError(f"Timed out waiting for server log marker {marker!r}")

    def command(self, command: str, marker: str, timeout: int = 45) -> None:
        assert self.process and self.process.stdin
        with self.lock:
            start_at = len(self.logs)
        self.process.stdin.write(command + "\n")
        self.process.stdin.flush()
        end = time.monotonic() + timeout
        while time.monotonic() < end:
            if self.process.poll() is not None:
                raise RuntimeError(f"Server exited while running {command}: {self.process.returncode}")
            with self.lock:
                recent = self.logs[start_at:]
                failure = next((line for line in recent if "CMZ_PERSIST_FAIL" in line or "CMZ_PERSIST_SEED_FAIL" in line or "CMZ_PERSIST_CHECK_FAIL" in line), None)
                if failure:
                    raise RuntimeError(f"Fixture command failed: {failure}")
                if any(marker in line for line in recent):
                    return
            time.sleep(0.2)
        raise TimeoutError(f"Timed out waiting for command marker {marker!r}")

    def stop(self) -> None:
        try:
            if not self.process or self.process.poll() is not None:
                return
            assert self.process.stdin
            self.process.stdin.write("save-all flush\nstop\n")
            self.process.stdin.flush()
            try:
                code = self.process.wait(timeout=90)
            except subprocess.TimeoutExpired:
                self.process.terminate()
                try:
                    self.process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    self.process.kill()
                    self.process.wait()
                raise TimeoutError("Server did not stop gracefully")
            if code:
                raise RuntimeError(f"Server stopped with exit code {code}")
            print(f"SERVER_JVM_EXIT pid={self.process.pid} exit_code={code}", flush=True)
        finally:
            for fixture in (GAME_DIR / "mods").glob("*gametests.jar"):
                fixture.unlink()


def clean_boot() -> None:
    prepare()
    server = Server(False)
    try:
        server.start()
        server.command("list", "There are", timeout=20)
        print("CLEAN_PRODUCTION_JAR_BOOT_PASS", flush=True)
    finally:
        server.stop()


def persistence() -> None:
    prepare()
    first = Server(True)
    try:
        first.start()
        first.command("cmz_verify seed", "CMZ_PERSIST_SEED_PASS")
        first.command("save-all flush", "Saved the game")
    finally:
        first.stop()
    second = Server(True)
    try:
        second.start()
        second.command("cmz_verify check", "CMZ_PERSIST_CHECK_PASS")
        print("TWO_JVM_WORLD_PERSISTENCE_PASS", flush=True)
    finally:
        second.stop()


def migration() -> None:
    baseline = Path(os.environ.get("CMZ_BASELINE_DIR", ROOT / ".local/template-migration/baseline"))
    legacy = checked_artifact(baseline / "create-mekanism-zinc-0.2.3-dev.jar")
    fixture = checked_artifact(baseline / "create-mekanism-zinc-0.2.3-dev-gametests.jar")
    # 旧版でseedしたworldを、新版fixtureだけでcheckする。新版seedで上書きしない。
    prepare(legacy, with_kff=False)
    first = Server(True, fixture)
    try:
        first.start()
        first.command("cmz_verify seed", "CMZ_PERSIST_SEED_PASS")
        first.command("save-all flush", "Saved the game")
    finally:
        first.stop()
    prepare()
    second = Server(True)
    try:
        second.start()
        second.command("cmz_verify check", "CMZ_PERSIST_CHECK_PASS")
        print("LEGACY_WORLD_MIGRATION_PASS Java=0.2.3-dev Kotlin=" + mod_version(ROOT), flush=True)
    finally:
        second.stop()
    # 保存後の通常JARはfixtureが無くても起動できる。
    clean_boot()


if __name__ == "__main__":
    if len(sys.argv) != 2 or sys.argv[1] not in {"prepare", "clean-boot", "persistence", "migration"}:
        raise SystemExit("Usage: python3 scripts/verify-server.py prepare|clean-boot|persistence|migration")
    {"prepare": prepare, "clean-boot": clean_boot, "persistence": persistence, "migration": migration}[sys.argv[1]]()
