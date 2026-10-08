/**
 * Extracts every inline <script type="module"> block from index.html and
 * syntax-checks it.  Fast smoke test that catches typos without a browser.
 * Run: node tools/check-syntax.mjs
 */
import { readFileSync, writeFileSync, mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const html = readFileSync(join(root, 'index.html'), 'utf8');

const blocks = [...html.matchAll(/<script type="module">([\s\S]*?)<\/script>/g)].map((m) => m[1]);
if (blocks.length === 0) {
  console.error('no inline module script found in index.html');
  process.exit(1);
}

const dir = mkdtempSync(join(tmpdir(), 'bh-syntax-'));
let failed = 0;
blocks.forEach((code, i) => {
  const file = join(dir, `block${i}.mjs`);
  writeFileSync(file, code);
  const res = spawnSync(process.execPath, ['--check', file], { encoding: 'utf8' });
  if (res.status === 0) {
    console.log(`  OK    inline module #${i} (${code.split('\n').length} lines)`);
  } else {
    failed++;
    console.error(`  FAIL  inline module #${i}\n${res.stderr}`);
  }
});

// also check the plain classic scripts
const plain = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].map((m) => m[1]);
plain.forEach((code, i) => {
  const file = join(dir, `plain${i}.js`);
  writeFileSync(file, code);
  const res = spawnSync(process.execPath, ['--check', file], { encoding: 'utf8' });
  if (res.status === 0) console.log(`  OK    inline script #${i}`);
  else { failed++; console.error(`  FAIL  inline script #${i}\n${res.stderr}`); }
});

rmSync(dir, { recursive: true, force: true });
console.log(failed ? `\n${failed} block(s) failed` : '\nsyntax OK');
process.exit(failed ? 1 : 0);
