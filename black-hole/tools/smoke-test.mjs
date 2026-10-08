/**
 * Interactive smoke test: drives the real UI (presets, sliders, switches,
 * buttons, keyboard shortcuts) in a headless browser and asserts that the
 * app reacts without errors.
 *
 * Usage: node tools/smoke-test.mjs
 */
import puppeteer from 'puppeteer-core';
import { existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const exe = ['C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  '/usr/bin/google-chrome'].find((p) => existsSync(p));
if (!exe) { console.error('no browser'); process.exit(2); }

let failures = 0;
const check = (n, ok, d) => { console.log(`  ${ok ? 'PASS' : 'FAIL'}  ${n}${d ? '   ' + d : ''}`); if (!ok) failures++; };
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const browser = await puppeteer.launch({
  executablePath: exe, headless: true,
  args: ['--no-sandbox', '--enable-unsafe-swiftshader', '--use-gl=angle', '--use-angle=swiftshader'],
});
try {
  const page = await browser.newPage();
  await page.setViewport({ width: 800, height: 500, deviceScaleFactor: 1 });
  const errors = [];
  page.on('pageerror', (e) => errors.push(e.message));
  page.on('console', (m) => { if (m.type() === 'error') errors.push('console: ' + m.text()); });

  await page.goto(pathToFileURL(join(root, 'index.html')).href, { waitUntil: 'load', timeout: 60000 });
  await page.waitForFunction('window.__bhBooted === true', { timeout: 180000 });
  await page.evaluate(() => window.__blackhole.setQuality(0.4));
  check('页面启动、着色器编译成功', true);

  // 1. 控制面板确实被构建出来
  const ui = await page.evaluate(() => ({
    groups: document.querySelectorAll('#panelBody .group').length,
    ranges: document.querySelectorAll('#panelBody input[type=range]').length,
    switches: document.querySelectorAll('#panelBody .switch input').length,
    selects: document.querySelectorAll('#panelBody select').length,
    hud: document.getElementById('hud').textContent.length,
  }));
  check('控制面板构建完整', ui.ranges >= 10 && ui.switches >= 3 && ui.selects >= 3 && ui.hud > 100,
    `${ui.groups} 组 / ${ui.ranges} 滑杆 / ${ui.switches} 开关 / ${ui.selects} 下拉 / HUD ${ui.hud} 字`);
  const hudText = await page.evaluate(() => document.getElementById('hud').textContent);
  check('物理读数面板有内容',
    /阴影角半径/.test(hudText) && /ISCO/.test(hudText) && /°/.test(hudText),
    hudText.replace(/\s+/g, ' ').slice(0, 90) + '…');

  // 2. 五个预设视角都能应用且相机确实移动
  const presetResult = await page.evaluate(async () => {
    const bh = window.__blackhole;
    const out = [];
    for (const k of Object.keys(bh.PRESETS)) {
      bh.applyPreset(k);
      await new Promise((r) => setTimeout(r, 6000));   // 等飞行动画结束（软件渲染帧率很低）
      const c = bh.getCamera();
      out.push({ k, r: c.r, target: Math.hypot(bh.camera.position.x, bh.camera.position.y, bh.camera.position.z) });
    }
    return out;
  });
  const wanted = { cinematic: 38, edgeon: 46, topdown: 44, closeup: 8, lensing: 26 };
  check('预设视角全部切到目标机位',
    presetResult.every((p) => Math.abs(p.r - p.target) < 0.05 && Math.abs(p.r - wanted[p.k]) < 0.6),
    presetResult.map((p) => `${p.k}:${p.r.toFixed(0)}`).join(' '));

  // 3. 滑杆 / 开关 / 下拉的事件绑定
  const widgets = await page.evaluate(() => {
    const bh = window.__blackhole;
    const find = (label) => [...document.querySelectorAll('#panelBody .row')]
      .find((r) => r.textContent.includes(label));
    const setRange = (label, value) => {
      const inp = find(label).querySelector('input[type=range]');
      inp.value = value; inp.dispatchEvent(new Event('input', { bubbles: true }));
    };
    setRange('内缘半径', 5.5);
    setRange('峰值温度', 15000);
    const sw = find('相对论光束效应').querySelector('input');
    const swBefore = bh.params.beaming;
    sw.checked = !sw.checked; sw.dispatchEvent(new Event('change', { bubbles: true }));
    const sel = find('渲染分辨率').querySelector('select');
    sel.value = '0.5'; sel.dispatchEvent(new Event('change', { bubbles: true }));
    return {
      inner: bh.params.diskInner, temp: bh.params.diskTemp,
      beamingFlipped: bh.params.beaming !== swBefore, quality: bh.params.quality,
      uniformInner: bh.uniforms.uDiskInner.value,
    };
  });
  check('滑杆写入参数与 uniform', widgets.inner === 5.5 && widgets.uniformInner === 5.5,
    `r_in=${widgets.inner} uDiskInner=${widgets.uniformInner}`);
  check('温度滑杆生效', widgets.temp === 15000, `T=${widgets.temp}`);
  check('开关生效', widgets.beamingFlipped === true);
  check('下拉生效', widgets.quality === '0.5', `quality=${widgets.quality}`);

  // 4. 键盘快捷键
  await page.keyboard.press('Space');
  const paused = await page.evaluate(() => window.__blackhole.params.timeScale);
  await page.keyboard.press('Space');
  const resumed = await page.evaluate(() => window.__blackhole.params.timeScale);
  check('空格冻结 / 继续', paused === 0 && resumed > 0, `冻结=${paused} 继续=${resumed}`);
  await page.keyboard.press('KeyH');
  const hidden = await page.evaluate(() => document.getElementById('panel').classList.contains('hidden'));
  await page.keyboard.press('KeyH');
  check('H 键隐藏 / 显示面板', hidden === true);

  // 5. 冻结后累积采样确实在推进
  await page.evaluate(() => { window.__blackhole.setQuality(0.35); window.__blackhole.pause(); });
  await sleep(4000);
  const acc = await page.evaluate(() => window.__blackhole.accumPass.count);
  check('冻结时累积采样推进', acc > 2, `${acc} 帧`);

  // 6. 截图按钮触发下载路径（不真正落盘，只验证不抛异常）
  await page.evaluate(() => {
    window.__shotFired = false;
    const a = document.createElement('a');
    a.click = () => { window.__shotFired = true; };
    HTMLAnchorElement.prototype.click = function () { window.__shotFired = true; };
    document.getElementById('btnShot').click();
  });
  await sleep(1500);
  check('保存 PNG 按钮可用', await page.evaluate(() => window.__shotFired === true));

  check('运行期间无 JS 错误', errors.length === 0, errors.slice(0, 3).join(' | '));
  console.log(`\n${failures === 0 ? 'ALL UI CHECKS PASSED' : failures + ' FAILURE(S)'}`);
} finally { await browser.close(); }
process.exit(failures ? 1 : 0);
