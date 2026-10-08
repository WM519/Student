/**
 * Creates a fully offline copy of the simulator:
 *   vendor/three.module.js      – Three.js build
 *   vendor/addons/…             – OrbitControls + post-processing addons
 *   index-offline.html          – same app, import map pointing at ./vendor
 *
 * Run:  npm install && npm run offline
 * Then serve the folder (npm run dev) and open /index-offline.html.
 * (Browsers refuse to import local ES modules over file://, so a static server
 *  — Vite, `python -m http.server`, nginx … — is required for this variant.)
 */
import { cpSync, mkdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const threePkg = join(root, 'node_modules', 'three');
if (!existsSync(threePkg)) {
  console.error('node_modules/three not found — run `npm install` first.');
  process.exit(1);
}

const vendor = join(root, 'vendor');
mkdirSync(join(vendor, 'addons'), { recursive: true });
// 优先使用压缩版构建（体积约为一半，API 完全一致）
const minBuild = join(threePkg, 'build', 'three.module.min.js');
cpSync(existsSync(minBuild) ? minBuild : join(threePkg, 'build', 'three.module.js'),
  join(vendor, 'three.module.js'));
// Three.js marks the build as a CJS/ESM hybrid; keep the sourceMappingURL out of the way
for (const sub of ['controls', 'postprocessing', 'shaders']) {
  cpSync(join(threePkg, 'examples', 'jsm', sub), join(vendor, 'addons', sub), { recursive: true });
}

const html = readFileSync(join(root, 'index.html'), 'utf8');
const offline = html.replace(
  /<script type="importmap">[\s\S]*?<\/script>/,
  `<script type="importmap">
{
  "imports": {
    "three": "./vendor/three.module.js",
    "three/addons/": "./vendor/addons/"
  }
}
</script>`
).replace(
  'npm install &amp;&amp; npm run offline',
  'npm install &amp;&amp; npm run offline'
);
writeFileSync(join(root, 'index-offline.html'), offline);

console.log('vendor/three.module.js  written');
console.log('vendor/addons/         written (controls, postprocessing, shaders)');
console.log('index-offline.html     written');
console.log('\nserve with `npm run dev` and open http://127.0.0.1:5173/index-offline.html');
