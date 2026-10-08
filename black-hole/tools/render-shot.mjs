/**
 * Headless render check + screenshot.
 *
 * Launches a locally installed Chromium/Edge in headless mode, loads index.html
 * (through the Vite dev server if one is running, otherwise from file://),
 * waits for the shader to compile, then writes a PNG.
 *
 * Usage:
 *   node tools/render-shot.mjs [--out screenshots/shot.png] [--w 1280] [--h 720]
 *                              [--wait 20000] [--url http://127.0.0.1:5173/index.html]
 *                              [--preset cinematic] [--scale 0.6]
 *
 * Chromium's software WebGL (SwiftShader) is used when no GPU is available, so
 * this works on headless machines — it is just slow.
 */
import puppeteer from 'puppeteer-core';
import { existsSync, mkdirSync } from 'node:fs';
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
  '/usr/bin/google-chrome',
  '/usr/bin/chromium',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
];
const exe = arg('exe', CANDIDATES.find((p) => existsSync(p)));
if (!exe) {
  console.error('No Chromium/Edge found. Pass --exe <path>.');
  process.exit(2);
}

const width = parseInt(arg('w', '1280'), 10);
const height = parseInt(arg('h', '720'), 10);
const out = resolve(root, arg('out', 'screenshots/black-hole.png'));
const url = arg('url', pathToFileURL(join(root, 'index.html')).href);
const preset = arg('preset', '');
const scale = parseFloat(arg('scale', '0.6'));
const waitMs = parseInt(arg('wait', '45000'), 10);
const showUI = arg('ui', '1') !== '0';

mkdirSync(dirname(out), { recursive: true });

const browser = await puppeteer.launch({
  executablePath: exe,
  headless: true,
  args: [
    '--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu-sandbox',
    '--enable-unsafe-swiftshader',   // software WebGL for headless boxes
    '--use-gl=angle', '--use-angle=swiftshader',
    '--window-size=' + width + ',' + height,
    '--hide-scrollbars',
  ],
});

try {
  const page = await browser.newPage();
  await page.setViewport({ width, height, deviceScaleFactor: 1 });
  const logs = [];
  page.on('console', (m) => logs.push(`[${m.type()}] ${m.text()}`));
  page.on('pageerror', (e) => logs.push(`[pageerror] ${e.message}`));
  page.on('requestfailed', (r) => logs.push(`[requestfailed] ${r.url()} ${r.failure()?.errorText}`));

  await page.goto(url, { waitUntil: 'load', timeout: 60000 });
  await page.waitForFunction('window.__bhBooted === true', { timeout: waitMs });
  console.log('shader compiled and first frames rendered');

  await page.evaluate((s, sc) => {
    const bh = window.__blackhole;
    bh.setQuality(sc);          // 固定内部分辨率，避免自动降分辨率打断累积
    if (s && bh.PRESETS[s]) bh.applyPreset(s);
  }, preset, scale);

  if (!showUI) {
    await page.addStyleTag({ content: '#panel,#title,#hud,#toast{display:none!important}' });
  }

  // let the accumulation converge on a frozen frame
  await page.evaluate(() => { window.__blackhole.pause(); });
  await new Promise((r) => setTimeout(r, 20000));

  const stats = await page.evaluate(() => {
    const bh = window.__blackhole;
    const c = bh.getCamera();
    const r = c.r;
    const bShadow = 3 * Math.sqrt(3) * 0.5;
    return {
      camera: { r: +r.toFixed(3), theta: +c.theta.toFixed(2), phi: +c.phi.toFixed(2) },
      shadowAngleDeg: +(2 * Math.asin(bShadow * Math.sqrt(1 - 1 / r) / r) * 180 / Math.PI).toFixed(3),
      accumFrames: bh.accumPass.count,
      drawCalls: bh.renderer.info.render.calls,
      programs: bh.renderer.info.programs?.length ?? -1,
      uTime: bh.uniforms.uTime.value,
      diskInner: bh.params.diskInner,
      diskOuter: bh.params.diskOuter,
      steps: bh.params.steps,
      gl: bh.renderer.getContext().getParameter(bh.renderer.getContext().VERSION),
      pixel: bh.pixelStats(48),
    };
  });

  await page.screenshot({ path: out });
  console.log('screenshot ->', out);
  console.log(JSON.stringify(stats, null, 2));
  if (logs.length) console.log('page log:\n' + logs.slice(0, 40).join('\n'));
} finally {
  await browser.close();
}
