import fs from 'node:fs';

const roPath = 'src/main/resources/static/i18n/ro.json';
const enPath = 'src/main/resources/static/i18n/en.json';

function load(path) {
  return JSON.parse(fs.readFileSync(path, 'utf8'));
}

function flatten(obj, prefix = '') {
  const out = [];
  for (const key of Object.keys(obj)) {
    const full = prefix ? `${prefix}.${key}` : key;
    const value = obj[key];
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      out.push(...flatten(value, full));
    } else {
      out.push(full);
    }
  }
  return out;
}

const ro = load(roPath);
const en = load(enPath);

const roKeys = flatten(ro).sort();
const enKeys = flatten(en).sort();

const missingInEn = roKeys.filter((k) => !enKeys.includes(k));
const missingInRo = enKeys.filter((k) => !roKeys.includes(k));

if (missingInEn.length || missingInRo.length) {
  console.error('i18n key mismatch detected.');
  if (missingInEn.length) {
    console.error('\nMissing in en.json:');
    missingInEn.forEach((k) => console.error(`- ${k}`));
  }
  if (missingInRo.length) {
    console.error('\nMissing in ro.json:');
    missingInRo.forEach((k) => console.error(`- ${k}`));
  }
  process.exit(1);
}

console.log(`i18n check passed: ${roKeys.length} keys in sync.`);
