"""サーバーを起動せず、artifact選択と検証用runtimeの所有境界を検査する。"""
import importlib.util
from pathlib import Path
import tempfile
import subprocess
import sys
import os
import unittest

spec = importlib.util.spec_from_file_location("verify_server", Path(__file__).resolve().parents[1] / "verify-server.py")
verification = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verification)


class ServerVerificationTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)

    def test_selects_current_version_normal_and_fixture_artifacts(self):
        (self.root / "gradle.properties").write_text("mod_version=0.3.0-dev\n", encoding="utf-8")
        libs = self.root / "neoforge/build/libs"
        libs.mkdir(parents=True)
        for suffix in ("", "-gametests"):
            (libs / f"create-mekanism-zinc-0.3.0-dev{suffix}.jar").write_bytes(b"artifact")
        version = verification.mod_version(self.root)
        self.assertEqual("0.3.0-dev", version)
        self.assertEqual(libs / "create-mekanism-zinc-0.3.0-dev.jar", verification.mod_artifact(self.root, version, False))
        self.assertEqual(libs / "create-mekanism-zinc-0.3.0-dev-gametests.jar", verification.mod_artifact(self.root, version, True))

    def test_rejects_missing_artifacts_and_unsafe_version_values(self):
        for value in ("../other", "x/y", "x\\y", "", "0.3.0 dev"):
            with self.subTest(value=value), self.assertRaises(ValueError):
                verification.mod_artifact(self.root, value, False)
        with self.assertRaises(FileNotFoundError):
            verification.mod_artifact(self.root, "0.3.0-dev", False)

    def test_rejects_missing_or_duplicate_version_property(self):
        for content in ("mod_name=zinc\n", "mod_version=0.3.0-dev\nmod_version=0.2.3-dev\n"):
            (self.root / "gradle.properties").write_text(content, encoding="utf-8")
            with self.assertRaises(ValueError):
                verification.mod_version(self.root)

    def test_rejects_symlink_artifact(self):
        libs = self.root / "neoforge/build/libs"
        libs.mkdir(parents=True)
        outside = self.root / "other.jar"
        outside.write_bytes(b"preserve")
        (libs / "create-mekanism-zinc-0.3.0-dev.jar").symlink_to(outside)
        with self.assertRaises(ValueError):
            verification.mod_artifact(self.root, "0.3.0-dev", False)
        self.assertEqual(b"preserve", outside.read_bytes())

    def test_creates_and_reuses_only_marked_owned_runtime(self):
        runtime = self.root / ".local/verification/template-migration-server"
        verification.ensure_owned_runtime(self.root, runtime, runtime / "instance")
        verification.ensure_owned_runtime(self.root, runtime, runtime / "instance")
        self.assertTrue((runtime / ".cmz-owned-runtime").is_file())

    def test_rejects_unowned_existing_runtime_without_modifying_it(self):
        runtime = self.root / ".local/verification/template-migration-server"
        runtime.mkdir(parents=True)
        existing = runtime / "important.txt"
        existing.write_text("preserve", encoding="utf-8")
        with self.assertRaises(ValueError):
            verification.ensure_owned_runtime(self.root, runtime, runtime / "instance")
        self.assertEqual("preserve", existing.read_text(encoding="utf-8"))
        self.assertFalse((runtime / ".cmz-owned-runtime").exists())

    def test_rejects_runtime_or_instance_outside_owned_location(self):
        runtime = self.root / ".local/verification/template-migration-server"
        for run, game in ((self.root / "other", self.root / "other/instance"), (runtime, self.root / "world")):
            with self.subTest(runtime=run, game=game), self.assertRaises(ValueError):
                verification.ensure_owned_runtime(self.root, run, game)

    def test_rejects_mod_directory_symlink_without_writing_outside(self):
        runtime = self.root / ".local/verification/template-migration-server"
        verification.ensure_owned_runtime(self.root, runtime, runtime / "instance")
        (runtime / "instance").mkdir()
        outside = self.root / "outside"
        outside.mkdir()
        (runtime / "instance/mods").symlink_to(outside, target_is_directory=True)
        with self.assertRaises(ValueError):
            verification.ensure_owned_runtime(self.root, runtime, runtime / "instance")
        self.assertEqual([], list(outside.iterdir()))

    def test_migration_cli_selects_legacy_baseline_before_starting_server(self):
        result = subprocess.run(
            [sys.executable, str(Path(verification.__file__)), "migration"],
            env={**os.environ, "CMZ_BASELINE_DIR": str(self.root)},
            capture_output=True, text=True,
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("FileNotFoundError", result.stderr)
        self.assertIn(str(self.root), result.stderr)
        self.assertNotIn("Usage:", result.stderr)

    def test_hash_mismatch_is_rejected(self):
        artifact = self.root / "kff.jar"
        artifact.write_bytes(b"wrong aggregate")
        with self.assertRaises(ValueError):
            verification.verify_hash(artifact, "0" * 128)


if __name__ == "__main__":
    unittest.main()
