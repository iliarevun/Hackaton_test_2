<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>CyberShield Hub — Registration</title>
  <link rel="stylesheet" href="/game.css">
  <style>
    .bio-bar { background:rgba(139,92,246,.07); border:1px solid rgba(139,92,246,.22); border-radius:var(--r-md); padding:10px 14px; margin-bottom:14px; display:flex; align-items:center; gap:10px; }
    .bio-dot { width:8px; height:8px; border-radius:50%; background:#8b5cf6; animation:pulse 1.2s ease-in-out infinite; flex-shrink:0; }
    @keyframes pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:.5;transform:scale(.8)} }
    .bio-txt  { font-family:var(--font-ui); font-size:.72rem; color:#c4b5fd; }
    .bio-count{ font-family:var(--font-game); font-size:.75rem; color:#a78bfa; margin-left:auto; }
    .bio-progress { height:3px; background:var(--border); border-radius:2px; margin-top:8px; overflow:hidden; }
    .bio-progress-fill { height:100%; background:linear-gradient(90deg,#8b5cf6,#06b6d4); border-radius:2px; transition:width .3s ease; }
    .bio-hint { font-family:var(--font-ui); font-size:.68rem; color:var(--text-muted); margin-top:4px; }
    .enroll-ok { background:rgba(34,197,94,.08); border:1px solid rgba(34,197,94,.25); border-radius:var(--r-md); padding:10px 14px; font-family:var(--font-ui); font-size:.78rem; color:#4ade80; margin-top:8px; display:none; }
    .enroll-ok.show { display:block; }
  </style>
</head>
<body>
<div class="auth-page">
  <div class="auth-box" style="max-width:460px;">
    <div class="auth-head">
      <span class="auth-icon">🦸</span>
      <div class="auth-t">🛡 CyberShield Hub</div>
      <div class="auth-s">Create your hero and start the journey</div>
    </div>

    <#if errorMessage??><div class="alert-err">❌ ${errorMessage}</div></#if>

    <!-- Biometric enroll indicator -->
    <div class="bio-bar">
      <div class="bio-dot"></div>
      <div>
        <div class="bio-txt">🧬 Реєстрація поведінкового профілю</div>
        <div class="bio-hint">Просто пиши — система запам'ятає твій ритм</div>
      </div>
      <div class="bio-count" id="bioCount">0/10</div>
    </div>
    <div class="bio-progress"><div class="bio-progress-fill" id="bioFill" style="width:0%"></div></div>

    <div class="enroll-ok" id="enrollOk">✅ Поведінковий профіль записано!</div>

    <form method="post" action="/registration" enctype="multipart/form-data" id="regForm">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
      <input type="hidden" name="keystrokeData" id="keystrokeData"/>
      <div class="fg">
        <label class="fl">Hero name</label>
        <input class="fi" type="text" name="name" id="nameField" placeholder="Enter your name" required>
      </div>
      <div class="fg">
        <label class="fl">Email</label>
        <input class="fi" type="email" name="email" id="emailField" placeholder="hero@healthhub.com" required>
      </div>
      <div class="fg">
        <label class="fl">Password (min. 8 chars)</label>
        <input class="fi" type="password" name="password" id="pwdField" placeholder="••••••••" required minlength="8">
      </div>
      <div class="fg">
        <label class="fl">Phone</label>
        <input class="fi" type="text" name="phoneNumber" placeholder="+380...">
      </div>
      <div class="fg">
        <label class="fl">Avatar (optional)</label>
        <input class="fi" type="file" name="fileAvatar" accept="image/*" style="padding:10px;cursor:pointer;">
      </div>
      <button class="btn btn-green w100 jc mt8" type="submit">🚀 Start adventure</button>
    </form>

    <div class="auth-div mt16">already have an account?</div>
    <div style="text-align:center;"><a href="/login" class="auth-link">← Sign in</a></div>
  </div>
</div>

<script>
// ── Keystroke capture (enroll) ─────────────────────────────────────────
const events = [];
const pressMap = {};
let enrolled = false;

function capture(el) {
  el.addEventListener('keydown', e => {
    if (!pressMap[e.code]) pressMap[e.code] = Date.now();
  });
  el.addEventListener('keyup', e => {
    const press = pressMap[e.code];
    if (press) {
      events.push({ key: e.key, pressTime: press, releaseTime: Date.now(), positionInWord: 0 });
      delete pressMap[e.code];
      const count = events.length;
      document.getElementById('bioCount').textContent = count + '/10';
      document.getElementById('bioFill').style.width = Math.min(100, count / 10 * 100) + '%';

      // Auto-enroll when 10 keystrokes collected
      if (count === 10 && !enrolled) tryEnroll();
    }
  });
}

['nameField','emailField','pwdField'].forEach(id => {
  const el = document.getElementById(id);
  if (el) capture(el);
});

async function tryEnroll() {
  if (events.length < 10) return;
  try {
    const res = await fetch('/biometrics/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ events })
    });
    const d = await res.json();
    if (!d.error) {
      enrolled = true;
      document.getElementById('enrollOk').classList.add('show');
      document.getElementById('enrollOk').textContent =
        '✅ Профіль записано — унікальність: ' + d.uniquenessScore + '% | ' + d.typingSpeedCpm + ' CPM';
    }
  } catch (_) {}
}

document.getElementById('regForm').addEventListener('submit', function() {
  document.getElementById('keystrokeData').value = JSON.stringify(events);
});
</script>
</body>
</html>
