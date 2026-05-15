<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>CyberShield Hub — Sign in</title>
  <link rel="stylesheet" href="/game.css">
  <style>
    .google-btn {
      display:flex; align-items:center; justify-content:center; gap:12px;
      width:100%; padding:13px 16px; border-radius:var(--r-md);
      background:rgba(255,255,255,.06); border:1px solid rgba(255,255,255,.14);
      font-family:var(--font-ui); font-size:.88rem; font-weight:700;
      color:var(--text-primary); cursor:pointer; transition:all .18s;
      text-decoration:none; letter-spacing:.3px;
    }
    .google-btn:hover { background:rgba(255,255,255,.11); border-color:rgba(255,255,255,.28); transform:translateY(-2px); box-shadow:0 6px 22px rgba(0,0,0,.35); }
    .google-btn:active { transform:translateY(0); }
    .divider { display:flex; align-items:center; gap:12px; margin:18px 0; font-family:var(--font-ui); font-size:.72rem; letter-spacing:1.5px; text-transform:uppercase; color:var(--text-muted); }
    .divider::before, .divider::after { content:''; flex:1; height:1px; background:var(--border); }

    /* ── Biometric bar ── */
    .bio-bar { background:rgba(16,185,129,.07); border:1px solid rgba(16,185,129,.2); border-radius:var(--r-md); padding:10px 14px; margin-bottom:14px; display:flex; align-items:center; gap:10px; }
    .bio-dot { width:8px; height:8px; border-radius:50%; background:#10b981; animation:pulse 1.2s ease-in-out infinite; flex-shrink:0; }
    @keyframes pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:.5;transform:scale(.8)} }
    .bio-txt { font-family:var(--font-ui); font-size:.72rem; color:#34d399; }
    .bio-count { font-family:var(--font-game); font-size:.75rem; color:#6ee7b7; margin-left:auto; }

    /* verify result */
    .verify-ok  { background:rgba(34,197,94,.08); border:1px solid rgba(34,197,94,.25); border-radius:var(--r-md); padding:10px 14px; font-family:var(--font-ui); font-size:.78rem; color:#4ade80; margin-top:10px; display:none; }
    .verify-warn{ background:rgba(245,158,11,.08); border:1px solid rgba(245,158,11,.25); border-radius:var(--r-md); padding:10px 14px; font-family:var(--font-ui); font-size:.78rem; color:#fbbf24; margin-top:10px; display:none; }
    .verify-ok.show, .verify-warn.show { display:block; }
  </style>
</head>
<body>
<div class="auth-page">
  <div class="auth-box">

    <div class="auth-head">
      <span class="auth-icon">⚡</span>
      <div class="auth-t">🛡 CyberShield Hub</div>
      <div class="auth-s">Sign in to your hero account</div>
    </div>

    <#if RequestParameters?? && RequestParameters.error??>
    <div class="alert-err">❌ Incorrect email or password</div>
    </#if>
    <#if RequestParameters?? && RequestParameters.logout??>
    <div class="alert-ok">✅ You have been signed out</div>
    </#if>
    <#if confirmMessage??><div class="alert-ok">${confirmMessage}</div></#if>
    <#if message_confirmed??><div class="alert-ok">✅ ${message_confirmed}</div></#if>
    <#if message_already_confirmed??><div class="alert-err">${message_already_confirmed}</div></#if>

    <!-- Google OAuth -->
    <form action="/oauth2/authorization/google" method="GET" style="margin:0;">
      <button type="submit" class="google-btn">
        <svg width="20" height="20" viewBox="0 0 24 24"><path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/><path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/><path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" fill="#FBBC05"/><path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/></svg>
        Sign in via Google
      </button>
    </form>

    <div class="divider">або</div>

    <!-- Biometric tracking indicator -->
    <div class="bio-bar">
      <div class="bio-dot"></div>
      <div class="bio-txt">🧬 Keystroke biometrics активний</div>
      <div class="bio-count" id="bioCount">0 клавіш</div>
    </div>

    <!-- Email / Password -->
    <form method="post" action="/login" id="loginForm">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
      <input type="hidden" name="keystrokeData" id="keystrokeData"/>
      <div class="fg">
        <label class="fl">Email</label>
        <input class="fi" type="email" name="username" id="emailField"
               placeholder="hero@cybershieldhub.com" required autocomplete="email">
      </div>
      <div class="fg">
        <label class="fl">Password</label>
        <input class="fi" type="password" name="password" id="pwdField"
               placeholder="••••••••" required autocomplete="current-password">
      </div>
      <button class="btn btn-blue w100 jc mt8" type="submit" id="signInBtn">⚡ Sign in</button>
    </form>

    <div class="verify-ok"  id="verifyOk"></div>
    <div class="verify-warn" id="verifyWarn"></div>

    <div style="text-align:center;font-size:0.8rem;color:var(--text-muted);margin-top:16px;">
      No account? <a href="/registration" class="auth-link">Create hero →</a>
    </div>

  </div>
</div>

<script>
// ── Keystroke capture ─────────────────────────────────────────────────
const events = [];
const pressMap = {};
let startTime = null;

function capture(el) {
  el.addEventListener('keydown', e => {
    if (!pressMap[e.code]) { pressMap[e.code] = Date.now(); if (!startTime) startTime = Date.now(); }
  });
  el.addEventListener('keyup', e => {
    const press = pressMap[e.code];
    if (press) {
      events.push({ key: e.key, pressTime: press, releaseTime: Date.now(), positionInWord: 0 });
      delete pressMap[e.code];
      document.getElementById('bioCount').textContent = events.length + ' клавіш';
    }
  });
}

capture(document.getElementById('emailField'));
capture(document.getElementById('pwdField'));

// ── On submit: try biometric verify, then submit ───────────────────────
document.getElementById('loginForm').addEventListener('submit', async function(e) {
  document.getElementById('keystrokeData').value = JSON.stringify(events);

  if (events.length >= 5) {
    try {
      const res = await fetch('/biometrics/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ events })
      });
      const d = await res.json();
      if (d.isMatch !== undefined) {
        if (d.isMatch) {
          document.getElementById('verifyOk').textContent =
            '🧬 Біометрія: ' + d.verdict + ' (' + d.similarity + '%)';
          document.getElementById('verifyOk').classList.add('show');
        } else {
          document.getElementById('verifyWarn').textContent =
            '⚠️ Незнайомий ритм набору (' + d.similarity + '%) — ' + d.explanation;
          document.getElementById('verifyWarn').classList.add('show');
        }
      }
    } catch (_) { /* no profile yet — skip silently */ }
  }
});
</script>
</body>
</html>
