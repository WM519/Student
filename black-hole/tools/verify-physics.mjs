/**
 * verify-physics.mjs
 * ---------------------------------------------------------------------------
 * Independent numerical validation of the general-relativistic ray tracer used
 * by index.html.  Geometric units G = c = 1, Schwarzschild radius r_s = 1
 * (so M = r_s/2 = 0.5).
 *
 * Tests
 *   1. Deflection angle from the Binet (orbit) equation against the
 *      post-Newtonian series  alpha = 4M/b + 15*pi*M^2/(4b^2) + 128M^3/(3b^3).
 *   2. The Cartesian null-geodesic ODE used on the GPU
 *         d2x/dl2 = -(3/2) r_s h^2 x / r^5 ,  h = |x x dx/dl|
 *      must reproduce the Binet integrator (it is the same curve).
 *   3. Critical impact parameter for capture, compared with b_c = 3*sqrt(3)*M.
 *   4. Shadow (black hole silhouette) angular radius for a static observer,
 *      sin(alpha_sh) = b_c * sqrt(1 - r_s/r) / r   vs.  ray traced boundary.
 *   5. ISCO location and Keplerian frequency  Omega = sqrt(M/r^3).
 *   6. Gravitational + Doppler shift factors used for the accretion disk.
 *
 * Run:  node tools/verify-physics.mjs
 */

const RS = 1.0;          // Schwarzschild radius (length unit)
const M = RS / 2;        // mass in geometric units
const BC = 3 * Math.sqrt(3) * M; // critical impact parameter = 2.598 r_s

// ---------------------------------------------------------------------------
// Accurate reference integrator: Binet equation in the orbital plane.
//     d2u/dphi2 = -u + (3/2) r_s u^2      with u = 1/r
// ---------------------------------------------------------------------------

/**
 * Integrate the Binet orbit equation until the ray reaches infinity (u -> 0).
 * dir = +1 forward along the direction of motion, dir = -1 backwards in order
 * to reach the *incoming* asymptote.  Returns the orbital angle at which u
 * crosses zero, which is exactly the asymptote angle, or NaN if captured.
 */
function integrateToAsymptote(u0, up0, dir, dphi) {
  const acc = (uu) => -uu + 1.5 * RS * uu * uu;
  let u = u0, up = up0, phi = 0, prevU = u0, prevPhi = 0;
  for (let i = 0; i < 5e7; i++) {
    const h = dir * dphi;
    const k1u = up, k1p = acc(u);
    const k2u = up + 0.5 * h * k1p, k2p = acc(u + 0.5 * h * k1u);
    const k3u = up + 0.5 * h * k2p, k3p = acc(u + 0.5 * h * k2u);
    const k4u = up + h * k3p, k4p = acc(u + h * k3u);
    const nu = u + (h / 6) * (k1u + 2 * k2u + 2 * k3u + k4u);
    const np = up + (h / 6) * (k1p + 2 * k2p + 2 * k3p + k4p);
    prevU = u; prevPhi = phi;
    u = nu; up = np; phi += h;
    if (u <= 0) {
      // u -> 0 linearly near the asymptote: linear interpolation is exact here
      const frac = prevU / (prevU - u);
      return prevPhi + frac * h;
    }
    if (u > 1 / (RS * 1.0000001)) return NaN; // captured
  }
  return NaN;
}

/** Exact asymptote-to-asymptote deflection from the Binet orbit equation. */
function deflectionBinet(b, dphi = 2e-5) {
  const U0 = 1e-9;                     // start at r = 1e9 r_s
  const up0 = Math.sqrt(Math.max(1 / (b * b) - U0 * U0 + RS * U0 ** 3, 0));
  const forward = integrateToAsymptote(U0, +up0, +1, dphi);
  const backward = integrateToAsymptote(U0, +up0, -1, dphi);
  if (!Number.isFinite(forward) || !Number.isFinite(backward)) return { alpha: Infinity };
  return { alpha: forward - backward - Math.PI };
}

/** Deflection series to 3rd post-Newtonian order. */
function deflectionSeries(b) {
  return (
    4 * M / b +
    (15 * Math.PI * M * M) / (4 * b * b) +
    (128 * M * M * M) / (3 * b * b * b)
  );
}

/** Deflection series including the 4th order term (for moderate b). */
function deflectionSeries4(b) {
  return deflectionSeries(b) + (3465 * Math.PI * M ** 4) / (64 * b ** 4);
}

/**
 * Exact deflection from the quadrature of the first integral
 *     dphi/du = 1 / sqrt(1/b^2 - u^2 + r_s u^3)
 *     alpha   = 2 * integral_0^{u_p} dphi/du du  -  pi
 * where u_p is the smallest positive root (the perihelion).  The substitution
 * u = u_p (1 - t^2) removes the inverse-square-root endpoint singularity, so
 * composite Simpson converges to machine precision.  No ODE stepping is
 * involved: this is an independent reference value.
 */
function deflectionExact(b, intervals = 20000) {
  const F = (u) => 1 / (b * b) - u * u + RS * u * u * u;
  // first sign change of F on (0, 1/r_s)
  let lo = 0, hi = null;
  const scan = 200000;
  for (let i = 1; i <= scan; i++) {
    const u = i / (scan * RS);
    if (F(u) < 0) { lo = (i - 1) / (scan * RS); hi = u; break; }
  }
  if (hi === null) return { alpha: Infinity, rPeri: 0 };
  for (let i = 0; i < 200; i++) {
    const mid = 0.5 * (lo + hi);
    if (F(mid) < 0) hi = mid; else lo = mid;
  }
  const up = 0.5 * (lo + hi);
  // At t = 0 (u = u_p) the ratio 2*u_p*t/sqrt(F) has the finite limit
  //     2 * sqrt(u_p / -F'(u_p)) = 2 / sqrt(2 - 3 r_s u_p).
  // Using the limit instead of 0/0 removes the jump that otherwise degrades
  // composite Simpson to first order.
  const f0 = 2 / Math.sqrt(Math.max(2 - 3 * RS * up, 1e-300));
  const f = (t) => {
    const u = up * (1 - t * t);
    // du = -2*u_p*t dt, so the integrand carries a factor t; this cancels the
    // inverse-square-root divergence at the perihelion (t = 0).
    return (2 * up * t) / Math.sqrt(Math.max(F(u), 1e-300));
  };
  let sum = f0 + f(1);
  for (let i = 1; i < intervals; i++) sum += (i % 2 ? 4 : 2) * f(i / intervals);
  const integral = (sum * (1 / intervals)) / 3;
  // total turn from -infinity to +infinity is 2 * integral
  return { alpha: 2 * integral - Math.PI, rPeri: 1 / up };
}

// ---------------------------------------------------------------------------
// GPU integrator: Cartesian null geodesic ODE, adaptive RK4.
// This mirrors the shader in index.html line for line.
// ---------------------------------------------------------------------------

function traceRay({ pos, dirLocal, maxSteps = 400, escapeR = 300, radiusStep = 0.18, angleStep = 0.03, maxStepLen = Infinity }) {
  const rCam = Math.hypot(...pos);
  const fCam = 1 - RS / rCam;
  const er = pos.map((v) => v / rCam);
  const c = dirLocal[0] * er[0] + dirLocal[1] * er[1] + dirLocal[2] * er[2];
  const sf = Math.sqrt(Math.max(fCam, 1e-6));
  // coordinate velocity of the photon in the static observer's frame
  const v = [
    dirLocal[0] + (sf - 1) * c * er[0],
    dirLocal[1] + (sf - 1) * c * er[1],
    dirLocal[2] + (sf - 1) * c * er[2],
  ];
  let x = pos.slice();
  let vel = v.slice();
  let prevDir = normalize(vel);
  const hvec = cross(x, vel);
  const h2 = dot(hvec, hvec);
  const E = sf;                     // conserved energy with local frequency normalised to 1
  const b = Math.sqrt(h2) / E;

  const acc = (p) => {
    const r = Math.hypot(...p);
    const inv5 = 1 / (r * r * r * r * r);
    return [-1.5 * RS * h2 * p[0] * inv5, -1.5 * RS * h2 * p[1] * inv5, -1.5 * RS * h2 * p[2] * inv5];
  };

  let captured = false, escaped = false, i = 0, turn = 0;
  for (; i < maxSteps; i++) {
    const r = Math.hypot(...x);
    if (r < RS * 1.02) { captured = true; break; }
    if (r > escapeR && dot(x, vel) > 0) { escaped = true; break; }

    const vlen = Math.hypot(...vel);
    const dRad = (radiusStep * r) / Math.max(vlen, 1e-9);
    const dAng = h2 > 1e-12 ? (angleStep * vlen * r ** 5) / (1.5 * RS * h2) : 1e9;
    const dl = Math.min(dRad, dAng, maxStepLen);

    const k1x = vel, k1v = acc(x);
    const x2 = add(x, scale(k1x, 0.5 * dl)), v2 = add(vel, scale(k1v, 0.5 * dl));
    const k2x = v2, k2v = acc(x2);
    const x3 = add(x, scale(k2x, 0.5 * dl)), v3 = add(vel, scale(k2v, 0.5 * dl));
    const k3x = v3, k3v = acc(x3);
    const x4 = add(x, scale(k3x, dl)), v4 = add(vel, scale(k3v, dl));
    const k4x = v4, k4v = acc(x4);

    x = add(x, scale(add(add(k1x, scale(k2x, 2)), add(scale(k3x, 2), k4x)), dl / 6));
    vel = add(vel, scale(add(add(k1v, scale(k2v, 2)), add(scale(k3v, 2), k4v)), dl / 6));

    // accumulate the turning of the direction of motion
    const vn = normalize(vel);
    turn += Math.acos(Math.max(-1, Math.min(1, vn[0] * prevDir[0] + vn[1] * prevDir[1] + vn[2] * prevDir[2])));
    prevDir = vn;
  }
  const r = Math.hypot(...x);
  // direction of motion at the escape point; converges to the asymptotic
  // direction with an O(M/r) error, so the escape radius must be large.
  const dirInf = normalize(vel);
  return { captured, escaped, b, E, h2, rEnd: r, steps: i, dirInf, turn, Ly: hvec[1] };
}

const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
const add = (a, b) => [a[0] + b[0], a[1] + b[1], a[2] + b[2]];
const scale = (a, s) => [a[0] * s, a[1] * s, a[2] * s];
const normalize = (a) => { const n = Math.hypot(...a) || 1; return scale(a, 1 / n); };
const reject = (a, b) => add(a, scale(b, -dot(a, b)));

/** Local direction at distance rCam, angle psi from the *inward* radial axis. */
function localDir(rCam, psi) {
  return [-Math.cos(psi), 0, Math.sin(psi)];
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

const f3 = (x, n = 3) => (Number.isFinite(x) ? x.toFixed(n) : String(x));
let failures = 0;
const check = (name, ok, detail) => {
  console.log(`${ok ? "  PASS" : "  FAIL"}  ${name}${detail ? "   " + detail : ""}`);
  if (!ok) failures++;
};

console.log("Schwarzschild null-geodesic verification  (units: r_s = 1, M = 0.5)");
console.log("critical impact parameter b_c = 3*sqrt(3)*M =", f3(BC, 6), "r_s\n");

console.log("[1] deflection angle  Binet integrator vs post-Newtonian series");
const seriesTol = { 1000: 1e-8, 100: 1e-6, 20: 1e-4, 10: 5e-3 };
for (const b of [1000, 100, 20, 10]) {
  const num = deflectionBinet(b, 2e-5).alpha;
  const exact = deflectionExact(b).alpha;
  const ref = deflectionSeries4(b);
  const err = Math.abs(num - exact) / Math.abs(exact);
  console.log(
    `      b = ${String(b).padStart(4)} r_s   exact quadrature = ${exact.toExponential(9)} rad\n` +
    `                        Binet ODE        = ${num.toExponential(9)} rad   rel.diff ${err.toExponential(2)}\n` +
    `                        4th-order series = ${ref.toExponential(9)} rad   rel.diff ${(Math.abs(ref - exact) / Math.abs(exact)).toExponential(2)}`
  );
  check(`Binet ODE = exact quadrature, b=${b}`, err < 1e-8);
  check(`PN series converges to exact quadrature, b=${b}`, Math.abs(ref - exact) / Math.abs(exact) < seriesTol[b]);
}
{
  // Einstein's first-order result must emerge at large impact parameter: b*alpha -> 4M
  const b = 1000;
  const prod = b * deflectionExact(b).alpha;
  const twoTerm = 4 * M + (15 * Math.PI * M * M) / (4 * b);
  const threeTerm = twoTerm + (128 * M ** 3) / (3 * b * b);
  console.log(`      b*alpha at b = 1000 r_s : ${prod.toFixed(9)}`);
  console.log(`        4M + 15pi M^2/(4b)         = ${twoTerm.toFixed(9)}`);
  console.log(`        4M + 15pi M^2/(4b) + ...   = ${threeTerm.toFixed(9)}`);
  check("b*alpha -> 4M + 15pi M^2/(4b) + ...", Math.abs(prod - threeTerm) < 1e-7,
    `residual ${Math.abs(prod - threeTerm).toExponential(2)}`);
}

console.log("\n[1b] deflection integrator convergence (step size independence)");
{
  const b = 8;
  const a1 = deflectionBinet(b, 1e-4).alpha;
  const a2 = deflectionBinet(b, 2e-5).alpha;
  check("step-size convergence", Math.abs(a1 - a2) / a2 < 1e-6, `rel.diff = ${(Math.abs(a1 - a2) / a2).toExponential(2)}`);
}

console.log("\n[2] Cartesian GPU ODE vs exact quadrature (camera at 1e6 r_s, escape at 1e9 r_s)");
const cartesianDeflection = (b, radiusStep, angleStep) => {
  const rCam = 1e6;
  const f = 1 - RS / rCam;
  const psi = Math.asin(Math.min((b * Math.sqrt(f)) / rCam, 1));
  const res = traceRay({
    pos: [rCam, 0, 0], dirLocal: localDir(rCam, psi),
    maxSteps: 400000, escapeR: 1e9, radiusStep, angleStep,
  });
  const d0 = normalize(localDir(rCam, psi));
  return Math.acos(Math.max(-1, Math.min(1, dot(d0, res.dirInf))));
};
for (const b of [30, 12, 6, 4]) {
  const ang = cartesianDeflection(b, 0.18, 0.03);
  const exact = deflectionExact(b).alpha;
  const rel = Math.abs(ang - exact) / exact;
  console.log(
    `      b = ${String(b).padStart(3)} r_s   GPU-ODE (fine steps) = ${ang.toFixed(9)} rad   exact = ${exact.toFixed(9)} rad` +
    `   diff = ${(ang - exact).toExponential(2)} rad (rel ${rel.toExponential(1)})`
  );
  check(`Cartesian ODE (shader step control) b=${b}`, rel < 1e-4);
}
{
  // The GPU integrator is 4th order accurate: halving both step controls must
  // shrink the error by roughly 2^4.  This also proves the Cartesian ODE is the
  // same curve as the Binet equation rather than an approximation of it.
  const b = 6;
  const exact = deflectionExact(b).alpha;
  const errs = [];
  for (const [rStep, aStep] of [[0.18, 0.03], [0.09, 0.015], [0.045, 0.0075]]) {
    errs.push(Math.abs(cartesianDeflection(b, rStep, aStep) - exact));
  }
  console.log(
    `      RK4 convergence at b = 6 r_s :  errors ${errs.map((e) => e.toExponential(2)).join(" -> ")}` +
    `   ratios ${(errs[0] / errs[1]).toFixed(1)}, ${(errs[1] / errs[2]).toFixed(1)}`
  );
  check("4th-order convergence of the GPU integrator",
    errs[0] / errs[1] > 8 && errs[1] / errs[2] > 8 && errs[2] < 1e-7);
}

console.log("\n[3] critical impact parameter (capture threshold)");
{
  // bisect on the impact parameter to locate the capture boundary
  // capture happens for b < b_c, escape for b > b_c
  let lo = 2.0, hi = 3.0;
  for (let i = 0; i < 200; i++) {
    const mid = 0.5 * (lo + hi);
    const res = deflectionBinet(mid, 1e-4);
    if (Number.isFinite(res.alpha)) hi = mid; else lo = mid;
  }
  const bcNum = 0.5 * (lo + hi);
  console.log(`      bisection gives b_c = ${bcNum.toFixed(9)} r_s   closed form = ${BC.toFixed(9)} r_s`);
  check("b_c = 3*sqrt(3)*M", Math.abs(bcNum - BC) < 1e-6);
  // photon sphere from d(1/b^2)/du = 0  ->  r = 1.5 r_s
  const rPh = 1.5 * RS;
  console.log(`      photon sphere r_ph = 1.5 r_s = ${rPh} ; b at photon sphere = ${(rPh / Math.sqrt(1 - RS / rPh)).toFixed(9)} r_s`);
  check("photon sphere impact parameter", Math.abs(rPh / Math.sqrt(1 - RS / rPh) - BC) < 1e-9);
}

console.log("\n[4] shadow angular radius for a static observer (r = 18 r_s)");
{
  const rCam = 18;
  // ray-traced boundary of the shadow: bisect on the local angle psi
  let lo = 0, hi = Math.PI / 2;
  for (let i = 0; i < 60; i++) {
    const mid = 0.5 * (lo + hi);
    const res = traceRay({ pos: [rCam, 0, 0], dirLocal: localDir(rCam, mid), maxSteps: 20000, escapeR: 3000 });
    if (res.captured) lo = mid; else hi = mid;
  }
  const psiC = 0.5 * (lo + hi);
  const analytic = Math.asin((BC * Math.sqrt(1 - RS / rCam)) / rCam);
  console.log(`      ray traced  psi_c = ${((psiC * 180) / Math.PI).toFixed(4)} deg   (b = ${(rCam * Math.sin(psiC) / Math.sqrt(1 - RS / rCam)).toFixed(6)} r_s)`);
  console.log(`      analytic    alpha = ${((analytic * 180) / Math.PI).toFixed(4)} deg   (b_c = ${BC.toFixed(6)} r_s)`);
  check("shadow radius matches closed form", Math.abs(psiC - analytic) < 2e-4);
  console.log(`      => shadow diameter = ${((2 * analytic * 180) / Math.PI).toFixed(3)} deg`);
}

console.log("\n[5] innermost stable circular orbit and Keplerian frequency");
{
  // V_eff(r) = -M/r + L^2/(2r^2) - M L^2 / r^3 ;  ISCO from V''=0 at the circular orbit
  const rIsco = 6 * M;
  const om = Math.sqrt(M / rIsco ** 3);
  console.log(`      ISCO r = 6M = ${rIsco} r_s   Omega(ISCO) = ${om.toFixed(6)} (geometric)`);
  check("ISCO = 6M", Math.abs(rIsco - 3 * RS) < 1e-12);
  // verify by numerically locating the ISCO from d(r_circ)/dr of L(r)
  const Lcirc = (r) => Math.sqrt((M * r * r) / (r - 3 * M));
  const dLdr = (r, h = 1e-5) => (Lcirc(r + h) - Lcirc(r - h)) / (2 * h);
  let lo = 3.1 * M, hi = 20 * M;
  for (let i = 0; i < 200; i++) {
    const mid = 0.5 * (lo + hi);
    if (dLdr(mid) < 0) lo = mid; else hi = mid;
  }
  console.log(`      numerically minimised L(r) -> r = ${(0.5 * (lo + hi)).toFixed(6)} r_s`);
  check("ISCO from minimum of L(r)", Math.abs(0.5 * (lo + hi) - rIsco) < 1e-4);
  // orbital period at ISCO in geometric units
  console.log(`      period at ISCO  T = 2*pi/Omega = ${((2 * Math.PI) / om).toFixed(3)} (r_s/c units)`);
}

console.log("\n[6] frequency shift factors for the accretion disk");
{
  // static emitter: g = sqrt(f(r_emit)/f(r_obs))
  const rObs = 18, rEm = 10;
  const gStatic = Math.sqrt((1 - RS / rEm) / (1 - RS / rObs));
  console.log(`      static emitter at 10 r_s seen from 18 r_s : g = ${gStatic.toFixed(6)}`);
  check("static redshift", gStatic < 1 && gStatic > 0);

  // circular emitter: g = 1 / ( sqrt(f_obs) u^t (1 - Omega L/E) )
  // A photon launched radially (L = 0) has no Doppler term, so the shift must
  // collapse to the pure gravitational one: g = sqrt(1-3M/r) / sqrt(1-r_s/r_obs).
  const rCam = 18;
  const res = traceRay({ pos: [rCam, 0, 0], dirLocal: [-1, 0, 0], maxSteps: 20000, escapeR: 3000 });
  const Ly = res.Ly;
  const fObs = 1 - RS / rCam;
  const ut = 1 / Math.sqrt(1 - 1.5 * RS / rEm);
  const om = Math.sqrt(M / rEm ** 3);
  const g = 1 / (Math.sqrt(fObs) * ut * (1 - (om * Ly) / res.E));
  const gGrav = Math.sqrt(1 - 1.5 * RS / rEm) / Math.sqrt(fObs); // L = 0 limit
  console.log(`      radial ray L = ${Ly.toExponential(2)}, g(L=0) = ${g.toFixed(6)} = sqrt(1-3M/r)/sqrt(f_obs) = ${gGrav.toFixed(6)}`);
  check("Keplerian shift reduces to gravitational redshift when L=0", Math.abs(g - gGrav) < 1e-9);
  // static emitter limit must reproduce sqrt(f_em/f_obs) exactly
  const uStatic = 1 / Math.sqrt(1 - RS / rEm);
  const gStat = 1 / (Math.sqrt(fObs) * uStatic * 1);
  check("static emitter redshift", Math.abs(gStat - Math.sqrt((1 - RS / rEm) / fObs)) < 1e-12,
    `g = ${gStat.toFixed(6)}`);

  // Doppler beaming: approaching side must be boosted by g^4 > 1, receding side dimmed
  const bEdge = 5;
  const psi = Math.asin((bEdge * Math.sqrt(fObs)) / rCam);
  const plus = traceRay({ pos: [rCam, 0, 0], dirLocal: [-Math.cos(psi), 0, +Math.sin(psi)], maxSteps: 20000, escapeR: 3000 });
  const minus = traceRay({ pos: [rCam, 0, 0], dirLocal: [-Math.cos(psi), 0, -Math.sin(psi)], maxSteps: 20000, escapeR: 3000 });
  const gOf = (r, sign) => 1 / (Math.sqrt(fObs) * ut * (1 - (om * sign * Math.abs(r.Ly)) / r.E));
  const gPlus = gOf(plus, +1), gMinus = gOf(minus, -1);
  console.log(
    `      b = 5 r_s :  prograde g = ${gPlus.toFixed(4)} (I x${(gPlus ** 4).toFixed(2)})   retrograde g = ${gMinus.toFixed(4)} (I x${(gMinus ** 4).toFixed(2)})`
  );
  check("Doppler/beaming asymmetry present", gPlus > 1 && gMinus < 1, `ratio = ${(gPlus / gMinus).toFixed(3)}`);
}

console.log("\n[7] high-order images: a ray can cross the equatorial plane several times");
{
  const rCam = 18;
  const psiOf = (b) => Math.asin(Math.min((b * Math.sqrt(1 - RS / rCam)) / rCam, 1));
  const inside = traceRay({ pos: [rCam, 1e-9, 0], dirLocal: localDir(rCam, psiOf(BC - 0.005)), maxSteps: 20000, escapeR: 3000 });
  const outside = traceRay({ pos: [rCam, 1e-9, 0], dirLocal: localDir(rCam, psiOf(BC + 0.005)), maxSteps: 20000, escapeR: 3000 });
  console.log(`      b = b_c - 0.005 : captured = ${inside.captured}, steps = ${inside.steps}`);
  console.log(`      b = b_c + 0.005 : captured = ${outside.captured}, turning = ${(outside.turn * 180 / Math.PI).toFixed(1)} deg, steps = ${outside.steps}`);
  check("ray just inside b_c is captured", inside.captured);
  check("ray just outside b_c winds around the hole (>180 deg)", outside.turn > Math.PI);
}

console.log(`\n${failures === 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED"}`);
process.exit(failures === 0 ? 0 : 1);
