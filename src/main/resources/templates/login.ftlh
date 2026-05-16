<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>CyberShield Hub — Sign in</title>
  <link rel="stylesheet" href="/game.css">
  <style>
    .google-btn{display:flex;align-items:center;justify-content:center;gap:12px;width:100%;padding:13px 16px;border-radius:var(--r-md);background:rgba(255,255,255,.06);border:1px solid rgba(255,255,255,.14);font-family:var(--font-ui);font-size:.88rem;font-weight:700;color:var(--text-primary);cursor:pointer;transition:all .18s;text-decoration:none;}
    .google-btn:hover{background:rgba(255,255,255,.11);border-color:rgba(255,255,255,.28);transform:translateY(-2px);}
    .divider{display:flex;align-items:center;gap:12px;margin:16px 0;font-family:var(--font-ui);font-size:.7rem;letter-spacing:1.5px;text-transform:uppercase;color:var(--text-muted);}
    .divider::before,.divider::after{content:'';flex:1;height:1px;background:var(--border);}

    /* Bio section */
    .bio-section{background:rgba(16,185,129,.06);border:1px solid rgba(16,185,129,.18);border-radius:var(--r-md);padding:12px 14px;margin-bottom:14px;}
    .bio-header{display:flex;align-items:center;gap:8px;margin-bottom:8px;}
    .bio-dot{width:7px;height:7px;border-radius:50%;background:#10b981;animation:pulse 1.2s infinite;flex-shrink:0;}
    @keyframes pulse{0%,100%{opacity:1}50%{opacity:.4}}
    .bio-title{font-family:var(--font-ui);font-size:.72rem;color:#34d399;font-weight:600;}
    .bio-hint{font-family:var(--font-ui);font-size:.68rem;color:var(--text-muted);line-height:1.4;}
    .bio-progress{height:3px;background:var(--bg-card3);border-radius:2px;margin-top:8px;overflow:hidden;}
    .bio-progress-fill{height:100%;background:linear-gradient(90deg,#10b981,#3b82f6);border-radius:2px;width:0%;transition:width .25s;}
    .bio-count{font-family:var(--font-game);font-size:.7rem;color:#6ee7b7;text-align:right;margin-top:3px;}

    /* seed verify */
    .seed-section{background:rgba(245,158,11,.05);border:1px solid rgba(245,158,11,.2);border-radius:var(--r-md);padding:12px 14px;margin-bottom:14px;}
    .seed-hdr{display:flex;align-items:center;gap:8px;margin-bottom:8px;}
    .seed-hdr-icon{font-size:.9rem;}
    .seed-hdr-txt{font-family:var(--font-ui);font-size:.72rem;color:#fbbf24;font-weight:600;}
    .seed-hint{font-family:var(--font-ui);font-size:.68rem;color:var(--text-muted);line-height:1.4;margin-bottom:8px;}
    .seed-input{width:100%;background:var(--bg-card2);border:1px solid rgba(245,158,11,.25);border-radius:8px;padding:9px 12px;font-family:var(--font-ui);font-size:.82rem;color:var(--text-primary);outline:none;box-sizing:border-box;}
    .seed-input:focus{border-color:rgba(245,158,11,.55);}
    .seed-input::placeholder{color:var(--text-muted);}
    .btn-seed-verify{margin-top:8px;width:100%;padding:9px;background:rgba(245,158,11,.12);border:1px solid rgba(245,158,11,.3);border-radius:8px;font-family:var(--font-game);font-size:.7rem;font-weight:700;letter-spacing:1px;color:#fbbf24;cursor:pointer;transition:all .2s;}
    .btn-seed-verify:hover{background:rgba(245,158,11,.2);}

    /* verify results */
    .verify-box{border-radius:var(--r-md);padding:10px 14px;font-family:var(--font-ui);font-size:.78rem;margin-top:10px;display:none;line-height:1.5;}
    .verify-box.show{display:block;}
    .verify-ok{background:rgba(34,197,94,.08);border:1px solid rgba(34,197,94,.25);color:#4ade80;}
    .verify-warn{background:rgba(245,158,11,.08);border:1px solid rgba(245,158,11,.25);color:#fbbf24;}
    .verify-neutral{background:rgba(59,130,246,.08);border:1px solid rgba(59,130,246,.2);color:#93c5fd;}
    .verify-err{background:rgba(239,68,68,.08);border:1px solid rgba(239,68,68,.25);color:#f87171;}

    .bio-typing-area{background:var(--bg-card2);border:1px solid var(--border);border-radius:var(--r-md);padding:10px 12px;font-family:var(--font-ui);font-size:.85rem;color:var(--text-primary);width:100%;outline:none;resize:none;height:60px;box-sizing:border-box;transition:border-color .18s;}
    .bio-typing-area:focus{border-color:rgba(16,185,129,.4);}
    .bio-prompt{font-family:var(--font-ui);font-size:.7rem;color:var(--text-muted);margin-bottom:5px;font-style:italic;}
  </style>
</head>
<body>
<div class="auth-page">
  <div class="auth-box">
    <div class="auth-head">
      <span class="auth-icon">🛡</span>
      <div class="auth-t">CyberShield Hub</div>
      <div class="auth-s">Sign in to your secure account</div>
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
<a href="/oauth2/authorization/google" class="google-btn">
<svg width="18" height="18" viewBox="0 0 24 24"><path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/><path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/><path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" fill="#FBBC05"/><path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/></svg>
Sign in via Google
</a>

<div class="divider">або email / пароль</div>

        <!-- 🧬 Keystroke biometrics -->
<div class="bio-section">
<div class="bio-header">
  <div class="bio-dot"></div>
  <div class="bio-title">🧬 Keystroke Biometrics</div>
</div>
<div class="bio-hint">Надрукуй фразу нижче — система звірить твій ритм набору з профілем</div>
<div class="bio-prompt" style="margin-top:8px;">→ Надрукуй: <em id="bioPromptText">"Захист починається з тебе"</em></div>
<textarea class="bio-typing-area" id="bioTypingArea" placeholder="Друкуй тут..."></textarea>
<div class="bio-progress"><div class="bio-progress-fill" id="bioFill"></div></div>
<div class="bio-count" id="bioCount">0 / 10 клавіш</div>
</div>

<div class="verify-box verify-neutral" id="verifyNeutral">
💡 Введи хоча б 10 символів для перевірки біометрії
</div>
<div class="verify-box verify-ok"  id="verifyOk"></div>
<div class="verify-box verify-warn" id="verifyWarn"></div>

        <!-- 🔑 Seed phrase recovery -->
<div class="seed-section">
<div class="seed-hdr">
  <span class="seed-hdr-icon">🔑</span>
  <span class="seed-hdr-txt">Seed phrase (опційно)</span>
</div>
<div class="seed-hint">
  Якщо біометрія не впізнала тебе — введи 10 слів seed phrase для підтвердження особи
</div>
<input class="seed-input" id="seedInput" type="text"
       placeholder="слово1 слово2 слово3 слово4 слово5 слово6 слово7 слово8 слово9 слово10"
       autocomplete="off" spellcheck="false"/>
<button class="btn-seed-verify" onclick="verifySeed()">🔍 ПЕРЕВІРИТИ SEED PHRASE</button>
<div class="verify-box verify-ok"  id="seedOk"  style="margin-top:8px;"></div>
<div class="verify-box verify-err" id="seedErr" style="margin-top:8px;"></div>
</div>

        <!-- Email / Password form -->
<form method="post" action="/login" id="loginForm">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
<input type="hidden" name="keystrokeData" id="keystrokeData"/>
<div class="fg" style="margin-top:14px;">
  <label class="fl">Email</label>
  <input class="fi" type="email" name="username" id="emailField"
         placeholder="you@example.com" required autocomplete="email">
</div>
<div class="fg">
  <label class="fl">Password</label>
  <input class="fi" type="password" name="password" id="pwdField"
         placeholder="••••••••" required autocomplete="current-password">
</div>
<button class="btn btn-blue w100 jc mt8" type="submit" id="signInBtn">⚡ Sign in</button>
</form>

<div style="text-align:center;font-size:.8rem;color:var(--text-muted);margin-top:14px;">
No account? <a href="/registration" class="auth-link">Create one →</a>
</div>
        </div>
        </div>

<script>
const prompts = [
  '"Захист починається з тебе"',
  '"Кібербезпека — це звичка"',
  '"Твій ритм унікальний"',
  '"Кожен символ — це підпис"'
];
document.getElementById('bioPromptText').textContent =
  prompts[Math.floor(Date.now() / 30000) % prompts.length];

// ── Keystroke capture on typing area ──────────────────────────────────
const bioEvents = [];
const loginEvents = [];
const pressMap = {};

const area = document.getElementById('bioTypingArea');
area.addEventListener('keydown', e => { if (!pressMap[e.code]) pressMap[e.code] = Date.now(); });
area.addEventListener('keyup', e => {
  const press = pressMap[e.code];
  if (press) {
    bioEvents.push({key: e.key, pressTime: press, releaseTime: Date.now(), positionInWord: 0});
    delete pressMap[e.code];
    const n = bioEvents.length;
    document.getElementById('bioCount').textContent = n + ' / 10 клавіш';
    document.getElementById('bioFill').style.width = Math.min(100, n / 10 * 100) + '%';
    if (n >= 10) runVerify();
  }
});

const loginMap = {};
[document.getElementById('emailField'), document.getElementById('pwdField')].forEach(el => {
  el.addEventListener('keydown', e => { if (!loginMap[e.code]) loginMap[e.code] = Date.now(); });
  el.addEventListener('keyup', e => {
    const p = loginMap[e.code];
    if (p) { loginEvents.push({key:e.key,pressTime:p,releaseTime:Date.now(),positionInWord:0}); delete loginMap[e.code]; }
  });
});

let verifyDone = false;
async function runVerify() {
  if (verifyDone) return;
  verifyDone = true;
  try {
    const res = await fetch('/biometrics/verify', {
      method:'POST', headers:{'Content-Type':'application/json'},
      body: JSON.stringify({events: bioEvents})
    });
    const d = await res.json();
    document.getElementById('verifyNeutral').classList.remove('show');
    if (d.error && d.error.includes('Спочатку зареєструйте')) {
      const nb = document.getElementById('verifyNeutral');
      nb.textContent = '🔵 Біометричний профіль не знайдено — перший вхід';
      nb.classList.add('show');
      return;
    }
    if (d.isMatch) {
      document.getElementById('verifyOk').textContent =
        '🧬 ' + d.verdict + ' — схожість ' + d.similarity + '% | ' + d.explanation;
      document.getElementById('verifyOk').classList.add('show');
    } else {
      document.getElementById('verifyWarn').textContent =
        '⚠️ Незнайомий ритм — ' + d.similarity + '% | ' + d.explanation;
      document.getElementById('verifyWarn').classList.add('show');
    }
  } catch(_) {
    document.getElementById('verifyNeutral').textContent = '🔵 Біометрія недоступна';
    document.getElementById('verifyNeutral').classList.add('show');
  }
}

// ── Seed phrase verify ─────────────────────────────────────────────────
async function verifySeed() {
  const words = document.getElementById('seedInput').value.trim().split(/\s+/);
  document.getElementById('seedOk').classList.remove('show');
  document.getElementById('seedErr').classList.remove('show');

  if (words.length !== 10) {
    document.getElementById('seedErr').textContent = '❌ Потрібно рівно 10 слів (зараз: ' + words.length + ')';
    document.getElementById('seedErr').classList.add('show');
    return;
  }

  try {
    const res = await fetch('/biometrics/verify-seed', {
      method:'POST', headers:{'Content-Type':'application/json'},
      body: JSON.stringify({seedPhrase: words.join(' ')})
    });
    const d = await res.json();
    if (d.valid) {
      document.getElementById('seedOk').textContent = '✅ Seed phrase підтверджено — особу верифіковано';
      document.getElementById('seedOk').classList.add('show');
    } else {
      document.getElementById('seedErr').textContent = '❌ Seed phrase не збігається з профілем';
      document.getElementById('seedErr').classList.add('show');
    }
  } catch(e) {
    document.getElementById('seedErr').textContent = '❌ Помилка перевірки: ' + e.message;
    document.getElementById('seedErr').classList.add('show');
  }
}

document.getElementById('loginForm').addEventListener('submit', () => {
  const all = [...bioEvents, ...loginEvents];
  document.getElementById('keystrokeData').value = JSON.stringify(all);
});
</script>
        </body>
        </html>
