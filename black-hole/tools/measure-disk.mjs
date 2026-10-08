/**
 * Validates the relativistic accretion-disk rendering from the actual pixels:
 *
 *  · Doppler beaming: with I ∝ g⁴ the side of the disk rotating towards the
 *    observer must be brighter and bluer than the receding side.
 *  · Turning beaming off must remove the brightness asymmetry (but keep the
 *    gravitational redshift).
 *  · Exposure check: the frame must be neither crushed to black nor blown out.
 *
 * Usage: node tools/measure-disk.mjs [--w 640] [--wait 8000]
 */
import puppeteer from 'puppeteer-core';
import { existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const arg = (n, d) => {
  const i = process.argv.indexOf(`--${n}`);
  return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : d;
};
const exe = ['C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  '/usr/bin/google-chrome'].find((p) => existsSync(p));
if (!exe) { console.error('no browser'); process.exit(2); }

const side = parseInt(arg('w', '640'), 10);
const browser = await puppeteer.launch({
  executablePath: exe, headless: true,
  args: ['--no-sandbox', '--enable-unsafe-swiftshader', '--use-gl=angle', '--use-angle=swiftshader'],
});

let failures = 0;
const check = (name, ok, detail) => {
  console.log(`  ${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '   ' + detail : ''}`);
  if (!ok) failures++;
};

const pageFn = new Function(`
  const bh = window.__blackhole;
  bh.composer.render();
  const c = bh.renderer.domElement;
  const cv = document.createElement('canvas');
  cv.width = c.width; cv.height = c.height;
  const ctx = cv.getContext('2d', { willReadFrequently: true });
  ctx.drawImage(c, 0, 0);
  const d = ctx.getImageData(0, 0, c.width, c.height).data;
  const w = c.width, h = c.height;
  let sum = 0, sat = 0, maxL = 0;
  const lum = new Float32Array(w * h);
  for (let i = 0, p = 0; i < d.length; i += 4, p++) {
    const l = (0.2126 * d[i] + 0.7152 * d[i + 1] + 0.0722 * d[i + 2]) / 255;
    lum[p] = l; sum += l; if (l > maxL) maxL = l;
    if (d[i] > 250 && d[i + 1] > 250 && d[i + 2] > 250) sat++;
  }
  // 盘的左右半区（排除中央 ±3% 与最外侧，只看盘所在的高度带）
  let lSum = 0, lN = 0, rSum = 0, rN = 0, lR = 0, lB = 0, rR = 0, rB = 0;
  for (let y = Math.round(h * 0.30); y < Math.round(h * 0.74); y++) {
    for (let x = 0; x < w; x++) {
      const band = lum[y * w + x];
      if (band < 0.02) continue;
      const i = (y * w + x) * 4;
      if (x < w * 0.44) { lSum += band; lN++; lR += d[i]; lB += d[i + 2]; }
      else if (x > w * 0.56) { rSum += band; rN++; rR += d[i]; rB += d[i + 2]; }
    }
  }
  return {
    mean: sum / (w * h), max: maxL, satFrac: sat / (w * h),
    left: lN ? lSum / lN : 0, right: rN ? rSum / rN : 0, leftN: lN, rightN: rN,
    leftRB: lN ? (lR - lB) / lN : 0, rightRB: rN ? (rR - rB) / rN : 0,
  };
`);

try {
  const page = await browser.newPage();
  await page.setViewport({ width: side, height: side, deviceScaleFactor: 1 });
  page.on('pageerror', (e) => console.log('[pageerror]', e.message));
  await page.goto(pathToFileURL(join(root, 'index.html')).href, { waitUntil: 'load', timeout: 60000 });
  await page.waitForFunction('window.__bhBooted === true', { timeout: 180000 });

  const setup = async (beaming) => {
    await page.evaluate((b) => {
      const bh = window.__blackhole;
      bh.setQuality(0.8);
      bh.applyPreset('cinematic');            // 使用交付默认参数
      Object.assign(bh.params, { beaming: b, timeScale: 0, steps: 420 });
      bh.setCamera(38, 80, 0);                // 盘轴 = +Y，方位角 0 → 不对称沿水平方向
      bh.pause();
      bh.setTime(0);
    }, beaming);
    await new Promise((r) => setTimeout(r, 6000));
    return page.evaluate(pageFn);
  };

  const on = await setup(true);
  const off = await setup(false);

  console.log('吸积盘渲染的物理自检（像素级）\n');
  console.log(`  开光束效应: 左半区平均亮度 ${on.left.toFixed(4)}  右半区 ${on.right.toFixed(4)}  ` +
    `左右比 ${(on.left / on.right).toFixed(3)}`);
  console.log(`  关光束效应: 左半区平均亮度 ${off.left.toFixed(4)}  右半区 ${off.right.toFixed(4)}  ` +
    `左右比 ${(off.left / off.right).toFixed(3)}`);
  console.log(`  颜色（R−B，越大越红）: 开光束 左 ${on.leftRB.toFixed(1)} / 右 ${on.rightRB.toFixed(1)}` +
    `  关光束 左 ${off.leftRB.toFixed(1)} / 右 ${off.rightRB.toFixed(1)}\n`);

  const ratioOn = on.left / on.right;
  const ratioOff = off.left / off.right;
  check('开启 g⁴ 时左右亮度显著不对称',
    Math.max(ratioOn, 1 / ratioOn) > 1.35, `亮暗比 ${Math.max(ratioOn, 1 / ratioOn).toFixed(3)}`);
  check('关闭 g⁴ 时亮度趋于对称', Math.abs(ratioOff - 1) < 0.18, `比值 ${ratioOff.toFixed(3)}`);
  // 更亮的一侧应当同时更蓝（蓝移）
  const brighterIsBluer = ratioOn > 1 ? (on.leftRB < on.rightRB) : (on.rightRB < on.leftRB);
  check('多普勒：亮侧更蓝、暗侧更红', brighterIsBluer,
    `左 R−B ${on.leftRB.toFixed(1)} / 右 R−B ${on.rightRB.toFixed(1)}（左/右亮度比 ${ratioOn.toFixed(3)}）`);
  check('画面曝光合理（未过曝）', on.satFrac < 0.06,
    `纯白像素占比 ${(on.satFrac * 100).toFixed(2)}%, 峰值 ${on.max.toFixed(3)}, 均值 ${on.mean.toFixed(4)}`);
  check('画面亮度分布合理（非全黑、非过亮）', on.mean > 0.03 && on.mean < 0.32, `均值 ${on.mean.toFixed(4)}`);

  console.log(`\n${failures === 0 ? 'ALL DISK CHECKS PASSED' : failures + ' FAILURE(S)'}`);
} finally { await browser.close(); }
process.exit(failures ? 1 : 0);
