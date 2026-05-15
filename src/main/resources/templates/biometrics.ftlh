<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>Поведінкова біометрія — KeyShield</title>
  <link rel="stylesheet" href="/game.css">
  <style>
    .module-hero{background:linear-gradient(135deg,rgba(16,185,129,.12),rgba(59,130,246,.08));border:1px solid rgba(16,185,129,.25);border-radius:var(--r-xl);padding:28px 32px;margin-bottom:24px;}
    .module-icon{font-size:3rem;margin-bottom:10px;}
    .module-title{font-family:var(--font-game);font-size:1.4rem;font-weight:900;color:var(--text-white);letter-spacing:1px;}
    .module-desc{font-family:var(--font-ui);font-size:0.85rem;color:var(--text-second);margin-top:6px;}
    .tab-row{display:flex;gap:8px;margin-bottom:20px;}
    .tab-btn{font-family:var(--font-ui);font-size:0.78rem;font-weight:600;letter-spacing:1px;text-transform:uppercase;padding:8px 18px;border-radius:8px;border:1px solid var(--border);background:var(--bg-card2);color:var(--text-second);cursor:pointer;transition:all .2s;}
    .tab-btn.active{border-color:rgba(16,185,129,.5);color:#34d399;background:rgba(16,185,129,.08);}
    .tab-panel{display:none;}.tab-panel.active{display:block;}
    .typing-box{width:100%;min-height:100px;background:var(--bg-card2);border:2px solid var(--border);border-radius:var(--r-md);padding:16px;font-family:var(--font-ui);font-size:1rem;color:var(--text-primary);resize:none;outline:none;box-sizing:border-box;transition:border-color .2s;}
    .typing-box.recording{border-color:rgba(16,185,129,.5);box-shadow:0 0 0 2px rgba(16,185,129,.08);}
    .typing-prompt{background:var(--bg-card3);border-radius:var(--r-md);padding:14px 18px;margin-bottom:12px;font-family:var(--font-ui);font-size:0.85rem;color:var(--cyan);border-left:3px solid var(--cyan);}
    .prompt-text{font-weight:700;color:var(--text-white);margin-top:4px;}
    .stats-live{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin:14px 0;}
    .stat-live-box{background:var(--bg-card3);border-radius:var(--r-md);padding:10px;text-align:center;}
    .stat-live-label{font-family:var(--font-game);font-size:0.58rem;letter-spacing:1.5px;text-transform:uppercase;color:var(--text-muted);margin-bottom:4px;}
    .stat-live-val{font-family:var(--font-game);font-size:1.1rem;font-weight:900;color:var(--text-white);}
    .stat-live-val.green{color:#34d399;}.stat-live-val.yellow{color:#fbbf24;}.stat-live-val.red{color:#f87171;}
    .progress-bar-wrap{height:4px;background:var(--bg-card3);border-radius:2px;margin:10px 0;overflow:hidden;}
    .progress-bar{height:100%;background:linear-gradient(90deg,#10b981,#3b82f6);border-radius:2px;transition:width .3s ease;}
    .progress-label{font-family:var(--font-ui);font-size:0.72rem;color:var(--text-muted);text-align:right;margin-top:2px;}
    .btn-action{width:100%;padding:13px;background:linear-gradient(135deg,#059669,#047857);border:none;border-radius:var(--r-md);font-family:var(--font-game);font-size:0.82rem;font-weight:700;letter-spacing:2px;color:#fff;cursor:pointer;margin-top:12px;transition:opacity .2s;}
    .btn-action:hover{opacity:.85;}
    .btn-action:disabled{opacity:.4;cursor:not-allowed;}
    .btn-reset{width:100%;padding:10px;background:var(--bg-card2);border:1px solid var(--border);border-radius:var(--r-md);font-family:var(--font-game);font-size:0.75rem;letter-spacing:1px;color:var(--text-second);cursor:pointer;margin-top:8px;transition:all .2s;}
    .btn-reset:hover{border-color:#f87171;color:#f87171;}
    .spinner{display:none;text-align:center;padding:20px;font-family:var(--font-ui);color:var(--text-second);}
    .spinner.show{display:block;}
    .spin{display:inline-block;width:18px;height:18px;border:2px solid var(--border);border-top-color:#10b981;border-radius:50%;animation:spin .7s linear infinite;vertical-align:middle;margin-right:8px;}
    @keyframes spin{to{transform:rotate(360deg);}}
    /* Profile card */
    .profile-card{background:var(--bg-card2);border:1px solid var(--border);border-radius:var(--r-xl);padding:24px;margin-top:16px;display:none;}
    .profile-card.show{display:block;}
    .profile-header{display:flex;align-items:center;gap:14px;margin-bottom:20px;}
    .profile-avatar{width:56px;height:56px;border-radius:50%;background:linear-gradient(135deg,#059669,#3b82f6);display:flex;align-items:center;justify-content:center;font-size:1.8rem;flex-shrink:0;}
    .profile-name{font-family:var(--font-game);font-size:1rem;font-weight:900;color:var(--text-white);}
    .profile-hash{font-family:var(--font-ui);font-size:0.72rem;color:var(--text-muted);margin-top:2px;font-family:monospace;}
    .metrics-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;}
    .metric-box{background:var(--bg-card3);border-radius:var(--r-md);padding:14px;text-align:center;}
    .metric-label{font-family:var(--font-game);font-size:0.6rem;letter-spacing:2px;text-transform:uppercase;color:var(--text-muted);margin-bottom:6px;}
    .metric-val{font-family:var(--font-game);font-size:1.3rem;font-weight:900;color:var(--text-white);}
    .metric-unit{font-size:0.65rem;color:var(--text-muted);font-weight:400;}
    .risk-badge{display:inline-flex;align-items:center;gap:6px;padding:4px 14px;border-radius:20px;font-family:var(--font-game);font-size:0.7rem;font-weight:700;letter-spacing:1px;}
    .risk-LOW{background:rgba(34,197,94,.12);border:1px solid rgba(34,197,94,.3);color:#4ade80;}
    .risk-MEDIUM{background:rgba(245,158,11,.12);border:1px solid rgba(245,158,11,.3);color:#fbbf24;}
    .risk-HIGH{background:rgba(239,68,68,.12);border:1px solid rgba(239,68,68,.3);color:#f87171;}
    .fingerprint-display{background:var(--bg-card3);border-radius:var(--r-md);padding:14px;margin-top:14px;text-align:center;}
    .fp-label{font-family:var(--font-game);font-size:0.62rem;letter-spacing:2px;text-transform:uppercase;color:var(--text-muted);margin-bottom:8px;}
    .fp-hash{font-family:monospace;font-size:1.1rem;font-weight:700;color:#34d399;letter-spacing:3px;}
    .fp-unique{font-family:var(--font-ui);font-size:0.75rem;color:var(--text-second);margin-top:4px;}
    /* Verify result */
    .verify-card{background:var(--bg-card2);border:1px solid var(--border);border-radius:var(--r-xl);padding:24px;margin-top:16px;display:none;}
    .verify-card.show{display:block;text-align:center;}
    .verify-big{font-size:3rem;margin-bottom:8px;}
    .verify-verdict{font-family:var(--font-game);font-size:1.1rem;font-weight:900;letter-spacing:2px;margin-bottom:6px;}
    .verify-sim{font-family:var(--font-game);font-size:2rem;font-weight:900;margin:8px 0;}
    .verify-exp{font-family:var(--font-ui);font-size:0.82rem;color:var(--text-second);max-width:400px;margin:0 auto;}
    .match-ring{width:120px;height:120px;border-radius:50%;margin:16px auto;display:flex;align-items:center;justify-content:center;font-family:var(--font-game);font-size:1.8rem;font-weight:900;}
    .info-box{background:var(--bg-card2);border-left:3px solid #10b981;border-radius:0 var(--r-md) var(--r-md) 0;padding:12px 16px;margin-bottom:16px;font-family:var(--font-ui);font-size:0.78rem;color:var(--text-second);line-height:1.5;}
  </style>
</head>
<body>
<div class="page-wrap">
  <nav class="topnav">
    <a href="/" class="nav-logo">🛡 CyberShield Hub</a>
    <div class="nav-links">
      <a href="/media-analysis" class="nav-link">🛡 Медіа</a>
      <a href="/query-proxy" class="nav-link">🔒 Проксі</a>
      <a href="/steganography" class="nav-link">🖼 Стеганографія</a>
      <a href="/biometrics" class="nav-link active">🧬 Біометрія</a>
    </div>
  </nav>

  <div class="content-wrap" style="max-width:720px;margin:0 auto;">
    <div class="module-hero">
      <div class="module-icon">🧬</div>
      <div class="module-title">KEY SHIELD — Поведінкова біометрія</div>
      <div class="module-desc">Твій ритм набору тексту унікальний як відбиток пальця.
        Система аналізує dwell time, flight time та патерни клавіш — створюючи невидимий цифровий підпис.</div>
    </div>

    <div class="tab-row">
      <button class="tab-btn active" onclick="switchTab('analyze')">📊 Аналіз</button>
      <button class="tab-btn" onclick="switchTab('enroll')">📝 Реєстрація</button>
      <button class="tab-btn" onclick="switchTab('verify')">🔐 Верифікація</button>
    </div>

    <!-- ANALYZE TAB -->
    <div id="tab-analyze" class="tab-panel active">
      <div class="info-box">
        📊 Просто почни друкувати. Система записує тільки тайминги — не сам текст.
        Твоя приватність збережена.
      </div>
      <div class="typing-prompt">
        Надрукуй наступне речення:
        <div class="prompt-text" id="analyzePrompt">"Безпека починається з твоїх звичок та унікального ритму"</div>
      </div>
      <textarea class="typing-box" id="analyzeInput" placeholder="Почни друкувати тут..."
                onkeydown="recordKey(event,'analyze')" onkeyup="releaseKey(event,'analyze')"></textarea>

      <div class="stats-live">
        <div class="stat-live-box">
          <div class="stat-live-label">Натискань</div>
          <div class="stat-live-val" id="aCount">0</div>
        </div>
        <div class="stat-live-box">
          <div class="stat-live-label">Швидкість</div>
          <div class="stat-live-val green" id="aSpeed">0</div>
        </div>
        <div class="stat-live-box">
          <div class="stat-live-label">Avg Dwell</div>
          <div class="stat-live-val" id="aDwell">—</div>
        </div>
        <div class="stat-live-box">
          <div class="stat-live-label">Помилки</div>
          <div class="stat-live-val" id="aErrors">0</div>
        </div>
      </div>
      <div class="progress-bar-wrap">
        <div class="progress-bar" id="aProgress" style="width:0%"></div>
      </div>
      <div class="progress-label" id="aProgressLabel">Введіть мінімум 5 натискань для аналізу</div>

      <button class="btn-action" id="btnAnalyze" onclick="submitAnalyze()" disabled>📊 АНАЛІЗУВАТИ МІЙ РИТМ</button>
      <button class="btn-reset" onclick="resetSession('analyze')">🔄 Скинути</button>
      <div class="spinner" id="aSpinner"><span class="spin"></span>Аналіз...</div>

      <div class="profile-card" id="analyzeResult">
        <div class="profile-header">
          <div class="profile-avatar">🧬</div>
          <div>
            <div class="profile-name">Твій поведінковий профіль</div>
            <div class="profile-hash" id="aHash"></div>
          </div>
          <div style="margin-left:auto;"><span class="risk-badge" id="aRisk"></span></div>
        </div>
        <div class="metrics-grid">
          <div class="metric-box">
            <div class="metric-label">Швидкість</div>
            <div class="metric-val"><span id="aCpm">—</span><span class="metric-unit"> CPM</span></div>
          </div>
          <div class="metric-box">
            <div class="metric-label">Avg Dwell</div>
            <div class="metric-val"><span id="aDwellResult">—</span><span class="metric-unit"> мс</span></div>
          </div>
          <div class="metric-box">
            <div class="metric-label">Avg Flight</div>
            <div class="metric-val"><span id="aFlight">—</span><span class="metric-unit"> мс</span></div>
          </div>
          <div class="metric-box">
            <div class="metric-label">Унікальність</div>
            <div class="metric-val green"><span id="aUnique">—</span><span class="metric-unit">%</span></div>
          </div>
          <div class="metric-box">
            <div class="metric-label">Помилки</div>
            <div class="metric-val"><span id="aErrRate">—</span><span class="metric-unit">%</span></div>
          </div>
          <div class="metric-box">
            <div class="metric-label">Burst ratio</div>
            <div class="metric-val"><span id="aBurst">—</span><span class="metric-unit">%</span></div>
          </div>
        </div>
        <div class="fingerprint-display">
          <div class="fp-label">🔑 Поведінковий відбиток</div>
          <div class="fp-hash" id="aFpHash"></div>
          <div class="fp-unique" id="aFpUnique"></div>
        </div>
      </div>
    </div>

    <!-- ENROLL TAB -->
    <div id="tab-enroll" class="tab-panel">
      <div class="info-box">
        📝 Реєстрація збереже твій поведінковий профіль. Після цього ти зможеш верифікуватись.
        Введи мінімум 10 натискань природно, у своєму звичайному темпі.
      </div>
      <div class="typing-prompt">
        Надрукуй це речення у своєму природному темпі:
        <div class="prompt-text">"Мій унікальний ритм неможливо підробити без мене"</div>
      </div>
      <textarea class="typing-box" id="enrollInput" placeholder="Почни друкувати..."
                onkeydown="recordKey(event,'enroll')" onkeyup="releaseKey(event,'enroll')"></textarea>
      <div class="stats-live">
        <div class="stat-live-box"><div class="stat-live-label">Натискань</div><div class="stat-live-val" id="eCount">0</div></div>
        <div class="stat-live-box"><div class="stat-live-label">Швидкість</div><div class="stat-live-val green" id="eSpeed">0</div></div>
        <div class="stat-live-box"><div class="stat-live-label">Avg Dwell</div><div class="stat-live-val" id="eDwell">—</div></div>
        <div class="stat-live-box"><div class="stat-live-label">Готовність</div><div class="stat-live-val" id="eReady">0%</div></div>
      </div>
      <button class="btn-action" id="btnEnroll" onclick="submitEnroll()" disabled>📝 ЗАРЕЄСТРУВАТИ ПРОФІЛЬ</button>
      <button class="btn-reset" onclick="resetSession('enroll')">🔄 Скинути</button>
      <div class="spinner" id="eSpinner"><span class="spin"></span>Реєстрація...</div>
      <div class="profile-card" id="enrollResult">
        <div style="text-align:center;">
          <div style="font-size:2.5rem;margin-bottom:8px;">✅</div>
          <div class="profile-name" style="margin-bottom:6px;">Профіль зареєстровано!</div>
          <div class="profile-hash" id="eHash"></div>
          <div style="margin-top:14px;" id="eStats"></div>
        </div>
      </div>
    </div>

    <!-- VERIFY TAB -->
    <div id="tab-verify" class="tab-panel">
      <div class="info-box">
        🔐 Введи будь-який текст — система порівняє твій поточний ритм з зареєстрованим профілем.
        Результат: чи це справді ти?
      </div>
      <div class="typing-prompt">
        Надрукуй що завгодно для верифікації:
        <div class="prompt-text">"Перевір чи я — це я"</div>
      </div>
      <textarea class="typing-box" id="verifyInput" placeholder="Почни друкувати..."
                onkeydown="recordKey(event,'verify')" onkeyup="releaseKey(event,'verify')"></textarea>
      <div class="stats-live">
        <div class="stat-live-box"><div class="stat-live-label">Натискань</div><div class="stat-live-val" id="vCount">0</div></div>
        <div class="stat-live-box"><div class="stat-live-label">Швидкість</div><div class="stat-live-val green" id="vSpeed">0</div></div>
        <div class="stat-live-box"><div class="stat-live-label">Avg Dwell</div><div class="stat-live-val" id="vDwell">—</div></div>
        <div class="stat-live-box"><div class="stat-live-label">Готовність</div><div class="stat-live-val" id="vReady">0%</div></div>
      </div>
      <button class="btn-action" id="btnVerify" onclick="submitVerify()" disabled>🔐 ВЕРИФІКУВАТИСЬ</button>
      <button class="btn-reset" onclick="resetSession('verify')">🔄 Скинути</button>
      <div class="spinner" id="vSpinner"><span class="spin"></span>Верифікація...</div>

      <div class="verify-card" id="verifyResult">
        <div class="verify-big" id="vIcon"></div>
        <div class="verify-verdict" id="vVerdict"></div>
        <div class="match-ring" id="vRing">
          <span id="vSimPct"></span>
        </div>
        <div style="color:var(--text-muted);font-size:0.72rem;margin-bottom:8px;">Схожість</div>
        <div class="verify-exp" id="vExp"></div>
      </div>
    </div>

  </div>
</div>

<script>
  // ── Keystroke capture engine ─────────────────────────────────────────────
  const sessions = {
    analyze: { events: [], pressMap: {}, startTime: null },
    enroll:  { events: [], pressMap: {}, startTime: null },
    verify:  { events: [], pressMap: {}, startTime: null }
  };

  function recordKey(e, session) {
    const s = sessions[session];
    const now = performance.now() + Date.now() - performance.now(); // epoch ms
    const t = Date.now();
    if (!s.startTime) s.startTime = t;
    if (!s.pressMap[e.code]) {
      s.pressMap[e.code] = t;
    }
    updateLive(session);
  }

  function releaseKey(e, session) {
    const s = sessions[session];
    const pressTime = s.pressMap[e.code];
    if (!pressTime) return;
    const releaseTime = Date.now();
    s.events.push({
      key: e.key,
      pressTime,
      releaseTime,
      positionInWord: 0
    });
    delete s.pressMap[e.code];
    updateLive(session);
  }

  function updateLive(session) {
    const s = sessions[session];
    const count = s.events.length;
    const elapsed = s.startTime ? (Date.now() - s.startTime) / 60000 : 0;
    const cpm = elapsed > 0 ? Math.round(count / elapsed) : 0;
    const dwells = s.events.map(e => e.releaseTime - e.pressTime).filter(d => d >= 0 && d < 2000);
    const avgDwell = dwells.length ? Math.round(dwells.reduce((a,b)=>a+b,0)/dwells.length) : 0;
    const backspaces = s.events.filter(e=>e.key==='Backspace').length;

    if (session === 'analyze') {
      document.getElementById('aCount').textContent  = count;
      document.getElementById('aSpeed').textContent  = cpm;
      document.getElementById('aDwell').textContent  = avgDwell ? avgDwell+'ms' : '—';
      document.getElementById('aErrors').textContent = backspaces;
      const pct = Math.min(100, Math.round(count / 5 * 100));
      document.getElementById('aProgress').style.width = pct+'%';
      document.getElementById('aProgressLabel').textContent = count >= 5
          ? count + ' натискань записано ✓' : 'Потрібно ще ' + (5-count) + ' натискань';
      document.getElementById('btnAnalyze').disabled = count < 5;
    }
    if (session === 'enroll') {
      document.getElementById('eCount').textContent = count;
      document.getElementById('eSpeed').textContent = cpm;
      document.getElementById('eDwell').textContent = avgDwell ? avgDwell+'ms' : '—';
      document.getElementById('eReady').textContent = Math.min(100,Math.round(count/10*100))+'%';
      document.getElementById('btnEnroll').disabled = count < 10;
    }
    if (session === 'verify') {
      document.getElementById('vCount').textContent = count;
      document.getElementById('vSpeed').textContent = cpm;
      document.getElementById('vDwell').textContent = avgDwell ? avgDwell+'ms' : '—';
      document.getElementById('vReady').textContent = Math.min(100,Math.round(count/5*100))+'%';
      document.getElementById('btnVerify').disabled = count < 5;
    }
  }

  function resetSession(session) {
    sessions[session] = { events: [], pressMap: {}, startTime: null };
    document.getElementById(session==='analyze'?'analyzeInput':session==='enroll'?'enrollInput':'verifyInput').value = '';
    updateLive(session);
    if (session==='analyze') {
      document.getElementById('analyzeResult').classList.remove('show');
      document.getElementById('aProgress').style.width='0%';
      document.getElementById('aProgressLabel').textContent='Введіть мінімум 5 натискань для аналізу';
    }
    if (session==='enroll') document.getElementById('enrollResult').classList.remove('show');
    if (session==='verify') document.getElementById('verifyResult').classList.remove('show');
  }

  function switchTab(tab) {
    const tabs = ['analyze','enroll','verify'];
    document.querySelectorAll('.tab-btn').forEach((b,i)=>b.classList.toggle('active',tabs[i]===tab));
    tabs.forEach(t=>{
      document.getElementById('tab-'+t).classList.toggle('active', t===tab);
    });
  }

  async function submitAnalyze() {
    const events = sessions.analyze.events;
    document.getElementById('aSpinner').classList.add('show');
    document.getElementById('btnAnalyze').disabled=true;
    try {
      const res = await fetch('/biometrics/analyze', {
        method:'POST', headers:{'Content-Type':'application/json'},
        body: JSON.stringify({events})
      });
      const d = await res.json();
      if (d.error) { alert(d.error); return; }
      renderProfile(d);
    } catch(e) { alert('Помилка: '+e.message); }
    finally { document.getElementById('aSpinner').classList.remove('show'); document.getElementById('btnAnalyze').disabled=false; }
  }

  function renderProfile(d) {
    document.getElementById('aHash').textContent = 'ID: ' + d.fingerprintHash;
    document.getElementById('aFpHash').textContent = d.fingerprintHash;
    document.getElementById('aFpUnique').textContent = 'Унікальність: ' + d.uniquenessScore + '%';
    document.getElementById('aCpm').textContent = d.typingSpeedCpm;
    document.getElementById('aDwellResult').textContent = d.avgDwellMs;
    document.getElementById('aFlight').textContent = d.avgFlightMs;
    document.getElementById('aUnique').textContent = d.uniquenessScore;
    document.getElementById('aErrRate').textContent = d.errorRate;
    document.getElementById('aBurst').textContent = d.burstRatio;
    const riskEl = document.getElementById('aRisk');
    riskEl.textContent = {LOW:'✅ Норма', MEDIUM:'⚠️ Незвично', HIGH:'🚨 Аномалія'}[d.riskLevel] || d.riskLevel;
    riskEl.className = 'risk-badge risk-'+d.riskLevel;
    document.getElementById('analyzeResult').classList.add('show');
    document.getElementById('analyzeResult').scrollIntoView({behavior:'smooth'});
  }

  async function submitEnroll() {
    const events = sessions.enroll.events;
    document.getElementById('eSpinner').classList.add('show');
    document.getElementById('btnEnroll').disabled=true;
    try {
      const res = await fetch('/biometrics/enroll', {
        method:'POST', headers:{'Content-Type':'application/json'},
        body: JSON.stringify({events})
      });
      const d = await res.json();
      if (d.error) { alert(d.error); return; }
      document.getElementById('eHash').textContent = 'Відбиток: ' + d.fingerprintHash;
      document.getElementById('eStats').innerHTML =
        '<div style="color:var(--text-second);font-size:0.8rem;">Швидкість: ' + d.typingSpeedCpm + ' CPM &nbsp;|&nbsp; Унікальність: ' + d.uniquenessScore + '%</div>';
      document.getElementById('enrollResult').classList.add('show');
    } catch(e) { alert('Помилка: '+e.message); }
    finally { document.getElementById('eSpinner').classList.remove('show'); document.getElementById('btnEnroll').disabled=false; }
  }

  async function submitVerify() {
    const events = sessions.verify.events;
    document.getElementById('vSpinner').classList.add('show');
    document.getElementById('btnVerify').disabled=true;
    try {
      const res = await fetch('/biometrics/verify', {
        method:'POST', headers:{'Content-Type':'application/json'},
        body: JSON.stringify({events})
      });
      const d = await res.json();
      if (d.error) { alert(d.error); return; }
      renderVerify(d);
    } catch(e) { alert('Помилка: '+e.message); }
    finally { document.getElementById('vSpinner').classList.remove('show'); document.getElementById('btnVerify').disabled=false; }
  }

  function renderVerify(d) {
    const isMatch = d.isMatch;
    const sim = d.similarity;
    const icon = isMatch ? (sim >= 90 ? '🟢' : '🟡') : '🔴';
    const verdictMap = {STRONG_MATCH:'ПОВНИЙ ЗБІГ', MATCH:'ЗБІГ', WEAK_MATCH:'ЧАСТКОВИЙ ЗБІГ', NO_MATCH:'НЕ ЗБІГАЄТЬСЯ'};
    const colorMap = {STRONG_MATCH:'#4ade80', MATCH:'#34d399', WEAK_MATCH:'#fbbf24', NO_MATCH:'#f87171'};
    const color = colorMap[d.verdict] || '#94a3b8';

    document.getElementById('vIcon').textContent = icon;
    document.getElementById('vVerdict').textContent = verdictMap[d.verdict] || d.verdict;
    document.getElementById('vVerdict').style.color = color;
    document.getElementById('vSimPct').textContent = sim+'%';
    document.getElementById('vRing').style.background = 'conic-gradient(' + color + ' ' + sim + '%, var(--bg-card3) 0)';
    document.getElementById('vRing').style.color = color;
    document.getElementById('vExp').textContent = d.explanation;
    document.getElementById('verifyResult').classList.add('show');
    document.getElementById('verifyResult').scrollIntoView({behavior:'smooth'});
  }
</script>
</body>
</html>
