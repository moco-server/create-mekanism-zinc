#!/usr/bin/env python3
"""Build an isolated NeoForge server, clean-boot the production jar, and verify disk persistence."""
from __future__ import annotations

import hashlib
import os
import shutil
import subprocess
import sys
import threading
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / ".local" / "verification" / "clean-server"
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


def prepare() -> None:
    version = subprocess.run([str(JAVA), "-version"], check=True, capture_output=True, text=True)
    if 'version "21' not in version.stderr:
        raise RuntimeError(f"Java 21 is required for server verification; selected executable: {JAVA}")
    resolved_game_dir = GAME_DIR.resolve()
    resolved_runtime = RUNTIME.resolve()
    if not resolved_game_dir.is_relative_to(resolved_runtime) or resolved_game_dir.parent != resolved_runtime or resolved_game_dir.name != "instance":
        raise RuntimeError(f"CMZ_GAME_DIR must be the disposable instance directory {RUNTIME / 'instance'}")
    RUNTIME.mkdir(parents=True, exist_ok=True)
    ownership = RUNTIME / ".cmz-owned-runtime"
    if not ownership.exists():
        if any(RUNTIME.iterdir()):
            raise RuntimeError(f"Refusing existing unmarked runtime directory: {RUNTIME}")
        ownership.write_text("Private disposable Create Mekanism Zinc verification runtime.\n", encoding="utf-8")
    install = RUNTIME / "neoforge-install"
    install.mkdir(exist_ok=True)
    installer = RUNTIME / "neoforge-21.1.252-installer.jar"
    if not installer.exists() or sha512(installer) != INSTALLER_SHA512:
        with urllib.request.urlopen(INSTALLER_URL, timeout=60) as response, installer.open("wb") as output:
            shutil.copyfileobj(response, output)
    actual = sha512(installer)
    if actual != INSTALLER_SHA512:
        raise RuntimeError(f"NeoForge installer SHA-512 mismatch: {actual}")
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
        "server-ip=127.0.0.1\nserver-port=25565\nonline-mode=false\nlevel-name=cmz-verification\n"
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
    addon = ROOT / "neoforge/build/libs/create-mekanism-zinc-0.2.0-dev.jar"
    if not addon.is_file():
        raise RuntimeError(f"Build production jar first: {addon}")
    for old in mods.glob("create-mekanism-zinc-*.jar"):
        old.unlink()
    shutil.copy2(addon, mods / addon.name)
    print(f"PREPARE_PASS installer_sha512={actual} game_dir={GAME_DIR}", flush=True)


class Server:
    def __init__(self, include_fixture: bool):
        self.logs: list[str] = []
        self.lock = threading.Lock()
        self.process: subprocess.Popen[str] | None = None
        self.include_fixture = include_fixture

    def start(self) -> None:
        mods = GAME_DIR / "mods"
        if self.include_fixture:
            fixture = ROOT / "neoforge/build/libs/create-mekanism-zinc-0.2.0-dev-gametests.jar"
            if not fixture.is_file():
                raise RuntimeError(f"Build test fixture jar first: {fixture}")
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
        print(f"SERVER_JVM_START pid={self.process.pid} fixture={self.include_fixture} world={GAME_DIR / 'cmz-verification'}", flush=True)
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


if __name__ == "__main__":
    if len(sys.argv) != 2 or sys.argv[1] not in {"prepare", "clean-boot", "persistence"}:
        raise SystemExit("Usage: python3 scripts/verify-server.py prepare|clean-boot|persistence")
    {"prepare": prepare, "clean-boot": clean_boot, "persistence": persistence}[sys.argv[1]]()
