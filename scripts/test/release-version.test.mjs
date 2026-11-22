import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const script = fileURLToPath(new URL('../release-version.mjs', import.meta.url));

function fixture(t) {
  const cwd = mkdtempSync(join(tmpdir(), 'cmz-release-'));
  t.after(() => rmSync(cwd, { recursive: true, force: true }));
  const write = (file, content) => writeFileSync(join(cwd, file), content);
  const read = (file) => readFileSync(join(cwd, file), 'utf8');
  const run = (...args) => spawnSync(process.execPath, [script, ...args], { cwd, encoding: 'utf8' });
  write('package.json', '{"version":"0.3.0"}\n');
  write('gradle.properties', 'mod_id=create_mekanism_zinc\nmod_version=0.3.0-dev\n');
  return { read, run, write };
}

test('sync updates the Gradle version and check detects version drift', (t) => {
  const { read, run } = fixture(t);
  const drift = run('check');
  assert.notEqual(drift.status, 0);
  assert.match(drift.stderr, /0\.3\.0-dev.*0\.3\.0/);
  assert.equal(run('sync').status, 0);
  assert.equal(read('gradle.properties'), 'mod_id=create_mekanism_zinc\nmod_version=0.3.0\n');
  assert.equal(run('check').status, 0);
});

test('prepare writes notes for the current version and rejects a stale changelog', (t) => {
  const { read, run, write } = fixture(t);
  assert.equal(run('sync').status, 0);
  write('CHANGELOG.md', '# create-mekanism-zinc\n\n## 0.2.0\n\n- Previous change\n');
  const stale = run('prepare', 'notes.md');
  assert.notEqual(stale.status, 0);
  assert.match(stale.stderr, /CHANGELOG\.md.*0\.3\.0/);
  write('CHANGELOG.md', '# create-mekanism-zinc\n\n## 0.3.0\n\n### Patch Changes\n\n- New change\n\n## 0.2.0\n\n- Previous change\n');
  const result = run('prepare', 'notes.md');
  assert.equal(result.status, 0, result.stderr);
  assert.equal(result.stdout, '0.3.0\n');
  assert.equal(read('notes.md'), '### Patch Changes\n\n- New change\n');
});
