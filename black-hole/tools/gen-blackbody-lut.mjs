/**
 * gen-blackbody-lut.mjs
 * ---------------------------------------------------------------------------
 * Generates the blackbody chromaticity lookup table embedded in index.html.
 *
 * Physics:
 *   Planck spectral radiance (per unit wavelength)
 *        B(lambda, T) = 2 h c^2 / lambda^5 * 1 / (exp(hc / (lambda k T)) - 1)
 *   converted to CIE 1931 XYZ with the analytic colour matching functions of
 *   Wyman, Sloan & Shirley (JCGT 2013), then to linear sRGB and normalised to
 *   unit luminance (so the table carries *colour only*; brightness is handled
 *   separately by the Stefan-Boltzmann T^4 law in the shader).
 *
 * Run:  node tools/gen-blackbody-lut.mjs
 */

const HCK_UM = 14387.77; // hc/k in micrometre * kelvin

// Planck's law, lambda in micrometres, arbitrary overall scale.
function planck(lum, T) {
  const x = HCK_UM / (lum * T);
  const bose = Math.exp(-x) / Math.max(1 - Math.exp(-x), 1e-300);
  const l2 = lum * lum;
  return bose / (l2 * l2 * lum);
}

// Multi-lobe Gaussian fits to the CIE 1931 2-deg colour matching functions.
function g(x, mu, s1, s2) {
  const s = x < mu ? s1 : s2;
  const t = (x - mu) / s;
  return Math.exp(-0.5 * t * t);
}
function ciexyz(nm) {
  const x =
    1.056 * g(nm, 599.8, 37.9, 31.0) +
    0.362 * g(nm, 442.0, 16.0, 26.7) -
    0.065 * g(nm, 501.1, 20.4, 26.2);
  const y = 0.821 * g(nm, 568.8, 46.9, 40.5) + 0.286 * g(nm, 530.9, 16.3, 31.1);
  const z = 1.217 * g(nm, 437.0, 11.8, 36.0) + 0.681 * g(nm, 459.0, 26.0, 13.8);
  return [x, y, z];
}

export function blackbodyRGB(T, stepNm = 1) {
  let X = 0, Y = 0, Z = 0;
  for (let nm = 380; nm <= 780; nm += stepNm) {
    const B = planck(nm * 1e-3, T) * ciexyz(nm)[0];
    const B2 = planck(nm * 1e-3, T) * ciexyz(nm)[1];
    const B3 = planck(nm * 1e-3, T) * ciexyz(nm)[2];
    X += B;
    Y += B2;
    Z += B3;
  }
  // CIE XYZ -> linear sRGB (IEC 61966-2-1)
  let r = 3.2406 * X - 1.5372 * Y - 0.4986 * Z;
  let gg = -0.9689 * X + 1.8758 * Y + 0.0415 * Z;
  let b = 0.0557 * X - 0.204 * Y + 1.057 * Z;
  r = Math.max(r, 0);
  gg = Math.max(gg, 0);
  b = Math.max(b, 0);
  const lum = 0.2126 * r + 0.7152 * gg + 0.0722 * b;
  return [r / lum, gg / lum, b / lum];
}

const N = 32;
const T0 = 600, T1 = 60000;

if (import.meta.url === `file://${process.argv[1]?.replace(/\\/g, "/")}` || process.argv[1]?.endsWith("gen-blackbody-lut.mjs")) {
  const lines = [];
  const samples = [];
  for (let i = 0; i < N; i++) {
    const f = i / (N - 1);
    const T = T0 * Math.pow(T1 / T0, f);
    const rgb = blackbodyRGB(T, 1);
    lines.push(
      `  vec3(${rgb[0].toFixed(5)}, ${rgb[1].toFixed(5)}, ${rgb[2].toFixed(5)})${i === N - 1 ? "" : ","}`
    );
    samples.push({ T, rgb });
  }
  console.log(`// Log-spaced ${N} entries from ${T0} K to ${T1} K, unit luminance.`);
  console.log(`const vec3 BB_LUT[${N}] = vec3[${N}](`);
  console.log(lines.join("\n"));
  console.log(");");
  console.log("\n// sanity check");
  for (const T of [1000, 2000, 3000, 4500, 6500, 10000, 20000, 50000]) {
    const [r, g_, b] = blackbodyRGB(T, 1);
    const m = Math.max(r, g_, b);
    console.log(
      `// ${String(T).padStart(6)} K -> rgb ${(r / m).toFixed(3)} ${(g_ / m).toFixed(3)} ${(b / m).toFixed(3)}`
    );
  }
}
