import { readFileSync, writeFileSync } from 'node:fs';

try {
  const [command, notesFile] = process.argv.slice(2);
  const { version } = JSON.parse(readFileSync('package.json', 'utf8'));
  if (typeof version !== 'string' || !/^\d+\.\d+\.\d+(?:-[\w.-]+)?$/.test(version)) {
    throw new Error('package.json must specify a release version');
  }

  const properties = readFileSync('gradle.properties', 'utf8');
  const versions = [...properties.matchAll(/^mod_version=([^\r\n]+)$/gm)];
  if (versions.length !== 1) throw new Error('gradle.properties must contain exactly one mod_version');

  if (command === 'sync') {
    writeFileSync('gradle.properties', properties.replace(versions[0][0], `mod_version=${version}`));
  } else if (versions[0][1] !== version) {
    throw new Error(`gradle.properties mod_version ${versions[0][1]} differs from package.json ${version}`);
  } else if (command === 'prepare' && notesFile) {
    const changelog = readFileSync('CHANGELOG.md', 'utf8');
    const [latest, previous] = changelog.matchAll(/^##[ \t]+(\S+)[ \t]*$/gm);
    if (latest?.[1] !== version) throw new Error(`CHANGELOG.md must start with an entry for ${version}`);
    const notes = changelog.slice(latest.index + latest[0].length, previous?.index).trim();
    if (!notes) throw new Error(`CHANGELOG.md entry ${version} is empty`);
    writeFileSync(notesFile, `${notes}\n`);
    console.log(version);
  } else if (command !== 'check') {
    throw new Error('Usage: node scripts/release-version.mjs check|sync|prepare <notes-file>');
  }
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
}
