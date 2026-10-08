/**
 * End-to-end validation of the rendered image.
 *
 * Turns the accretion disk off, freezes the frame, reads the actual pixels back
 * out of the WebGL canvas, locates the black-hole silhouette (the dark disc in
 * the middle of the frame) and compares the measured angular radius with the
 * general-relativistic prediction for a static observer
 *
 *      sin(alpha) = b_c * sqrt(1 - r_s/r) / r ,   b_c = 3*sqrt(3)*M = 2.598 r_s
 *
 * This exercises the whole pipeline: camera model, static-observer frame
 * conversion, geodesic integrator, capture criterion and projection.
 *
 * Usage: node tools/measure-shadow.mjs [--w 520] [--radii 8,12,18,30]
 */
import puppeteer from 'puppeteer-core';
import { existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const arg = (name, dflt) => {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : dflt;
};
const CANDIDATES = [
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
  '/usr/bin/google-chrome', '/usr/bin/chromium',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
];
const exe = arg('exe', CANDIDATES.find((p) => existsSync(p)));
if (!exe) { console.error('No Chromium/Edge found. Pass --exe <path>.'); process.exit(2); }

const side = parseInt(arg('w', '600'), 10);
const radii = arg('radii', '8,12,18,30,60').split(',').map(Number);
const fovDeg = 45;
const BC = 3 * Math.sqrt(3) * 0.5;          // 2.598076 r_s

const browser = await puppeteer.launch({
  executablePath: exe,
  headless: true,
  args: ['--no-sandbox', '--disable-setuid-sandbox', '--enable-unsafe-swiftshader',
    '--use-gl=angle', '--use-angle=swiftshader', '--hide-scrollbars'],
});

let failures = 0;
try {
  const page = await browser.newPage();
  await page.setViewport({ width: side, height: side, deviceScaleFactor: 1 });
  const pageErrors = [];
  page.on('pageerror', (e) => pageErrors.push(e.message));

  await page.goto(pathToFileURL(join(root, 'index.html')).href, { waitUntil: 'load', timeout: 60000 });
  await page.waitForFunction('window.__bhBooted === true', { timeout: 180000 });
  await page.evaluate(() => {
    const bh = window.__blackhole;
    bh.setQuality(0.75);
    bh.params.diskOn = false;      // 纯透镜背景，阴影边界干净
    bh.params.nebula = 0.0;
    bh.params.skyBright = 1.6;
    bh.params.bloom = 0.0;
    bh.params.fov = 45;
    bh.params.steps = 500;
    bh.pause();
  });

  console.log('Schwarzschild 阴影半径：渲染像素测量 vs 广义相对论解析值');
  console.log('(fov = 45°, 画面为正方形, 相机指向黑洞中心)\n');
  console.log('   r_obs/r_s    解析 sin a = b_c sqrt(f)/r      解析角半径      像素测量       相对误差');

  for (const r of radii) {
    await page.evaluate((rr) => {
      const bh = window.__blackhole;
      bh.setCamera(rr, 90, 0);      // 赤道面内，避免盘/银河带干扰
    }, r);
    await new Promise((res) => setTimeout(res, 2500));
    const stats = await page.evaluate((rr) => {
      const bh = window.__blackhole;
      bh.setCamera(rr, 90, 0);
      return bh.pixelStats(600);
    }, r);

    // 外区中位亮度（阴影之外），用其 50% 作为边界阈值
    const prof = stats.radialProfile;
    const outer = prof.slice(Math.round(prof.length * 0.62), Math.round(prof.length * 0.92))
      .filter((v) => v > 0).sort((a, b) => a - b);
    const ref = outer[outer.length >> 1] || 1e-4;
    const thr = ref * 0.5;
    let rp = null;
    for (let i = 1; i < Math.round(prof.length * 0.85); i++) {
      if (prof[i - 1] < thr && prof[i] >= thr) {
        const t = (thr - prof[i - 1]) / (prof[i] - prof[i - 1] || 1);
        rp = (i - 1 + t) / prof.length;
        break;
      }
    }
    if (rp === null) { console.log(`   ${r}  -> 未能在画面中找到阴影边界`); failures++; continue; }

    // 径向像素半径 -> 与光轴的夹角
    const rPix = rp * (side / 2);
    const measured = Math.atan((rPix / (side / 2)) * Math.tan((fovDeg / 2) * Math.PI / 180)) * 180 / Math.PI;
    const analytic = Math.asin(BC * Math.sqrt(1 - 1 / r) / r) * 180 / Math.PI;
    const relErr = Math.abs(measured - analytic) / analytic;
    const ok = relErr < 0.02;
    if (!ok) failures++;
    console.log(
      `   ${String(r).padStart(4)}          ${(BC * Math.sqrt(1 - 1 / r) / r).toFixed(6)}                  ` +
      `${analytic.toFixed(3)}°        ${measured.toFixed(3)}°       ${(relErr * 100).toFixed(2)}%  ${ok ? 'PASS' : 'FAIL'}` +
      `   (R = ${rPix.toFixed(2)} px, 阈值/底光 = ${(thr / (ref || 1)).toFixed(2)})`
    );
  }
  if (pageErrors.length) console.log('\npage errors:\n' + pageErrors.join('\n'));
  console.log(`\n${failures === 0 ? 'ALL SHADOW MEASUREMENTS PASSED' : failures + ' FAILURE(S)'}`);
} finally {
  await browser.close();
}
process.exit(failures ? 1 : 0);
