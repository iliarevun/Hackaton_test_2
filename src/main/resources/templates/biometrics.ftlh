<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>SecureMind — Biometrics</title>
  <link rel="stylesheet" href="/game.css">
  <style>
    .bio-hero{background:linear-gradient(135deg,rgba(16,185,129,.1),rgba(59,130,246,.06));border:1px solid rgba(16,185,129,.2);border-radius:var(--r-xl);padding:24px 28px;margin-bottom:22px;}
    .bio-hero-title{font-family:var(--font-game);font-size:1.2rem;font-weight:900;color:var(--text-white);letter-spacing:2px;}
    .bio-hero-desc{font-family:var(--font-ui);font-size:.82rem;color:var(--text-second);margin-top:5px;line-height:1.5;}

    .tab-row{display:flex;gap:8px;margin-bottom:20px;flex-wrap:wrap;}
    .tab-btn{font-family:var(--font-ui);font-size:.75rem;font-weight:600;letter-spacing:1px;text-transform:uppercase;padding:8px 16px;border-radius:8px;border:1px solid var(--border);background:var(--bg-card2);color:var(--text-second);cursor:pointer;transition:all .18s;}
    .tab-btn.active{border-color:rgba(16,185,129,.5);color:#34d399;background:rgba(16,185,129,.08);}
    .tab-panel{display:none;}.tab-panel.active{display:block;}

    .seg{background:var(--bg-card);border:1px solid var(--border);border-radius:var(--r-xl);padding:20px 22px;margin-bottom:16px;}
    .seg-title{font-family:var(--font-game);font-size:.65rem;letter-spacing:2.5px;text-transform:uppercase;color:var(--text-muted);margin-bottom:12px;}

    .typing-box{width:100%;min-height:80px;background:var(--bg-card2);border:2px solid var(--border);border-radius:var(--r-md);padding:14px;font-family:var(--font-ui);font-size:.95rem;color:var(--text-primary);resize:none;outline:none;box-sizing:border-box;transition:border-color .2s;}
    .typing-box.recording{border-color:rgba(16,185,129,.5);box-shadow:0 0 0 2px rgba(16,185,129,.08);}
    .typing-prompt{background:var(--bg-card3);border-radius:var(--r-md);padding:12px 16px;margin-bottom:10px;font-family:var(--font-ui);font-size:.82rem;color:var(--cyan);border-left:3px solid var(--cyan);}

    .stats-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin:12px 0;}
    .stat-box{background:var(--bg-card3);border-radius:var(--r-md);padding:10px;text-align:center;}
    .stat-lbl{font-family:var(--font-game);font-size:.55rem;letter-spacing:1.5px;text-transform:uppercase;color:var(--text-muted);margin-bottom:4px;}
    .stat-val{font-family:var(--font-game);font-size:1rem;font-weight:900;color:var(--text-white);}

    /* Session progress */
    .session-track{display:flex;gap:8px;margin:12px 0;align-items:center;}
    .session-dot{width:28px;height:28px;border-radius:50%;border:2px solid var(--border);display:flex;align-items:center;justify-content:center;font-size:.7rem;font-family:var(--font-game);color:var(--text-muted);transition:all .3s;flex-shrink:0;}
    .session-dot.done{border-color:#34d399;background:rgba(16,185,129,.15);color:#34d399;}
    .session-dot.active{border-color:#3b82f6;background:rgba(59,130,246,.15);color:#60a5fa;animation:pulse .8s ease-in-out infinite;}
    .session-dot.current{border-color:#f59e0b;background:rgba(245,158,11,.15);color:#fbbf24;}
    @keyframes pulse{0%,100%{opacity:1}50%{opacity:.5}}
    .session-label{font-family:var(--font-ui);font-size:.75rem;color:var(--text-muted);}

    /* Progress bar */
    .prog-wrap{height:4px;background:var(--bg-card3);border-radius:2px;margin:8px 0;overflow:hidden;}
    .prog-fill{height:100%;background:linear-gradient(90deg,#10b981,#3b82f6);border-radius:2px;transition:width .3s;}

    .btn-action{width:100%;padding:12px;background:linear-gradient(135deg,#059669,#047857);border:none;border-radius:var(--r-md);font-family:var(--font-game);font-size:.8rem;font-weight:700;letter-spacing:2px;color:#fff;cursor:pointer;margin-top:10px;transition:opacity .2s;}
    .btn-action:hover{opacity:.85;}.btn-action:disabled{opacity:.38;cursor:not-allowed;}
    .btn-reset{width:100%;padding:9px;background:var(--bg-card2);border:1px solid var(--border);border-radius:var(--r-md);font-family:var(--font-game);font-size:.72rem;letter-spacing:1px;color:var(--text-second);cursor:pointer;margin-top:6px;transition:all .2s;}
    .btn-reset:hover{border-color:#f87171;color:#f87171;}

    .spinner{display:none;text-align:center;padding:18px;}.spinner.show{display:block;}
    .spin{display:inline-block;width:18px;height:18px;border:2px solid var(--border);border-top-color:#10b981;border-radius:50%;animation:sp .7s linear infinite;vertical-align:middle;margin-right:8px;}
    @keyframes sp{to{transform:rotate(360deg);}}

    /* Result cards */
    .result-box{background:var(--bg-card2);border:1px solid var(--border);border-radius:var(--r-xl);padding:20px;margin-top:14px;display:none;}
    .result-box.show{display:block;}

    .fp-display{background:var(--bg-card3);border-radius:var(--r-md);padding:14px;text-align:center;margin-top:12px;}
    .fp-lbl{font-family:var(--font-game);font-size:.6rem;letter-spacing:2px;text-transform:uppercase;color:var(--text-muted);margin-bottom:6px;}
    .fp-hash{font-family:var(--font-mono);font-size:1rem;font-weight:700;color:#34d399;letter-spacing:3px;}

    .metrics-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin:12px 0;}
    .metric-box{background:var(--bg-card3);border-radius:var(--r-md);padding:12px;text-align:center;}
    .metric-lbl{font-family:var(--font-game);font-size:.58rem;letter-spacing:2px;text-transform:uppercase;color:var(--text-muted);margin-bottom:5px;}
    .metric-val{font-family:var(--font-game);font-size:1.2rem;font-weight:900;color:var(--text-white);}
    .metric-unit{font-size:.6rem;color:var(--text-muted);}

    /* Verify result */
    .verify-result{border-radius:var(--r-xl);padding:22px;margin-top:14px;display:none;text-align:center;}
    .verify-result.show{display:block;}
    .verify-result.match{background:rgba(34,197,94,.07);border:1px solid rgba(34,197,94,.25);}
    .verify-result.nomatch{background:rgba(239,68,68,.07);border:1px solid rgba(239,68,68,.25);}
    .verify-icon{font-size:2.5rem;margin-bottom:8px;}
    .verify-verdict{font-family:var(--font-game);font-size:1rem;font-weight:900;letter-spacing:2px;margin-bottom:6px;}
    .verify-sim{font-family:var(--font-game);font-size:1.8rem;font-weight:900;margin:8px 0;}
    .verify-exp{font-family:var(--font-ui);font-size:.8rem;color:var(--text-second);max-width:380px;margin:0 auto;}

    /* Password bind section */
    .pwd-bind-box{background:rgba(245,158,11,.07);border:1px solid rgba(245,158,11,.25);border-radius:var(--r-xl);padding:20px 22px;margin-top:16px;}
    .pwd-bind-title{font-family:var(--font-game);font-size:.72rem;letter-spacing:2px;text-transform:uppercase;color:#fbbf24;margin-bottom:12px;}
    .pwd-bind-row{display:flex;align-items:center;gap:10px;margin-bottom:10px;}
    .pwd-bind-row input[type=checkbox]{width:18px;height:18px;cursor:pointer;accent-color:#f59e0b;flex-shrink:0;}
    .pwd-bind-row label{font-family:var(--font-ui);font-size:.82rem;color:var(--text-second);cursor:pointer;}
    .pwd-field{width:100%;background:var(--bg-card2);border:1px solid var(--border);border-radius:var(--r-md);padding:11px 14px;font-family:var(--font-ui);font-size:.88rem;color:var(--text-primary);outline:none;margin-bottom:8px;box-sizing:border-box;}
    .pwd-field:focus{border-color:rgba(245,158,11,.4);}
    .btn-bind{width:100%;padding:12px;background:linear-gradient(135deg,#b45309,#d97706);border:none;border-radius:var(--r-md);font-family:var(--font-game);font-size:.78rem;font-weight:700;letter-spacing:2px;color:#fff;cursor:pointer;transition:opacity .2s;}
    .btn-bind:hover{opacity:.85;}.btn-bind:disabled{opacity:.38;cursor:not-allowed;}
    .bind-ok{background:rgba(34,197,94,.08);border:1px solid rgba(34,197,94,.25);border-radius:var(--r-md);padding:10px 14px;font-family:var(--font-ui);font-size:.78rem;color:#4ade80;margin-top:8px;display:none;}
    .bind-ok.show{display:block;}

    /* Session average visualization */
    .avg-card{background:var(--bg-card2);border:1px solid rgba(16,185,129,.2);border-radius:var(--r-xl);padding:20px;margin-top:14px;display:none;}
    .avg-card.show{display:block;}
    .avg-title{font-family:var(--font-game);font-size:.68rem;letter-spacing:2px;text-transform:uppercase;color:#34d399;margin-bottom:14px;}
    .avg-session-row{display:flex;align-items:center;gap:10px;margin-bottom:8px;font-family:var(--font-ui);font-size:.75rem;color:var(--text-second);}
    .avg-bar{flex:1;height:6px;background:var(--bg-card3);border-radius:3px;overflow:hidden;}
    .avg-bar-fill{height:100%;border-radius:3px;background:linear-gradient(90deg,#10b981,#3b82f6);}
    .avg-num{width:45px;text-align:right;font-family:var(--font-mono);font-size:.72rem;color:var(--text-primary);}
    .avg-final{background:var(--bg-card3);border-radius:var(--r-md);padding:14px;margin-top:12px;text-align:center;}
    .avg-final-lbl{font-family:var(--font-game);font-size:.6rem;letter-spacing:2px;text-transform:uppercase;color:var(--text-muted);margin-bottom:6px;}
    .avg-final-val{font-family:var(--font-game);font-size:1.6rem;font-weight:900;color:#34d399;}
  </style>
</head>
<body>
<header class="gh">
  <a href="/" class="gh-logo"><div class="logo-gem">🛡</div>SECURE<span>MIND</span></a>
  <nav class="gh-nav">
    <a href="/media-analysis" class="nb">🛡 Media</a>
    <a href="/query-proxy" class="nb">🔒 Proxy</a>
    <a href="/steganography" class="nb">🖼 Steganography</a>
    <a href="/biometrics" class="nb active-nav">🧬 Biometrics</a>
  </nav>
  <div class="gh-right">
    <form action="/logout" method="post" style="margin:0;">
      <input type="hidden" name="${(_csrf.parameterName)!''}" value="${(_csrf.token)!''}">
      <button type="submit" class="nb" style="cursor:pointer;background:none;border:none;color:var(--text-muted);">Sign out</button>
    </form>
  </div>
</header>

<div class="pw">
  <div class="c" style="max-width:760px;padding-top:28px;padding-bottom:60px;">

    <div class="bio-hero">
      <div class="bio-hero-title">🧬 KEYSTROKE BIOMETRICS</div>
      <div class="bio-hero-desc">
        Your typing rhythm is a unique digital signature. Complete 5 sessions of typing the same text,
        the system averages the results and creates a stable profile. You can then link it to your password.
      </div>
    </div>

    <div class="tab-row">
      <button class="tab-btn active" onclick="switchTab('train')">🏋️ Training (5 sessions)</button>
      <button class="tab-btn" onclick="switchTab('verify')">🔐 Verification</button>
      <button class="tab-btn" onclick="switchTab('analyze')">📊 Analysis</button>
    </div>

    <!-- ══ TRAIN TAB ══════════════════════════════════════════════════════ -->
    <div id="tab-train" class="tab-panel active">

      <div class="seg">
        <div class="seg-title">Step 1 — Type the text naturally 5 times</div>
        <div class="typing-prompt">
          Type this sentence each time at your normal pace:<br>
          <strong id="trainPrompt" style="color:var(--text-white);">"Cybersecurity starts with habit"</strong>
        </div>

        <!-- Session progress dots -->
        <div class="session-track">
          <div class="session-dot" id="dot0">1</div>
          <div style="flex:1;height:2px;background:var(--border);"></div>
          <div class="session-dot" id="dot1">2</div>
          <div style="flex:1;height:2px;background:var(--border);"></div>
          <div class="session-dot" id="dot2">3</div>
          <div style="flex:1;height:2px;background:var(--border);"></div>
          <div class="session-dot" id="dot3">4</div>
          <div style="flex:1;height:2px;background:var(--border);"></div>
          <div class="session-dot" id="dot4">5</div>
          <div class="session-label" id="sessionLabel" style="margin-left:10px;">Session 1 of 5</div>
        </div>

        <textarea class="typing-box" id="trainInput" placeholder="Start typing here..."
                  onkeydown="trainKeyDown(event)" onkeyup="trainKeyUp(event)"></textarea>

        <div class="stats-grid">
          <div class="stat-box"><div class="stat-lbl">Keys</div><div class="stat-val" id="tCount">0</div></div>
          <div class="stat-box"><div class="stat-lbl">CPM</div><div class="stat-val" id="tCpm">0</div></div>
          <div class="stat-box"><div class="stat-lbl">Dwell</div><div class="stat-val" id="tDwell">—</div></div>
          <div class="stat-box"><div class="stat-lbl">Sessions</div><div class="stat-val" id="tSessions" style="color:#34d399;">0/5</div></div>
        </div>
        <div class="prog-wrap"><div class="prog-fill" id="trainProg" style="width:0%"></div></div>

        <button class="btn-action" id="btnCapture" onclick="captureSession()" disabled>
          ✅ RECORD SESSION
        </button>
        <button class="btn-reset" onclick="resetTraining()">🔄 Reset all sessions</button>
      </div>

      <!-- Average visualization -->
      <div class="avg-card" id="avgCard">
        <div class="avg-title">📊 Averaged profile across sessions</div>
        <div id="avgSessions"></div>
        <div class="avg-final">
          <div class="avg-final-lbl">Average CPM (5 sessions)</div>
          <div class="avg-final-val" id="avgCpm">—</div>
        </div>
        <div class="fp-display" style="margin-top:12px;">
          <div class="fp-lbl">🔑 Behavioral fingerprint</div>
          <div class="fp-hash" id="trainHash">—</div>
        </div>

        <!-- Password binding -->
        <div class="pwd-bind-box" style="margin-top:16px;">
          <div class="pwd-bind-title">🔐 Link to password (optional)</div>
          <div class="pwd-bind-row">
            <input type="checkbox" id="bindCheck" onchange="toggleBind(this.checked)">
            <label for="bindCheck">Add biometrics to password — the system will verify both factors at login</label>
          </div>
          <div id="bindForm" style="display:none;">
            <input type="password" class="pwd-field" id="bindPassword" placeholder="Enter your password for confirmation">
            <button class="btn-bind" id="btnBind" onclick="bindBiometrics()">🔗 LINK BIOMETRICS</button>
          </div>
          <div class="bind-ok" id="bindOk">✅ Biometrics linked to password! Automatic verification will occur at login.</div>
        </div>
      </div>

      <div class="spinner" id="trainSpinner"><span class="spin"></span>Analyzing...</div>
    </div>

    <!-- ══ VERIFY TAB ══════════════════════════════════════════════════════ -->
    <div id="tab-verify" class="tab-panel">
      <div class="seg">
        <div class="seg-title">Verification — is it really you?</div>
        <div class="typing-prompt">
          Type the same sentence:
          <strong style="color:var(--text-white);">"Cybersecurity starts with habit"</strong>
        </div>
        <textarea class="typing-box" id="verifyInput" placeholder="Start typing..."
                  onkeydown="verifyKeyDown(event)" onkeyup="verifyKeyUp(event)"></textarea>

        <div class="stats-grid">
          <div class="stat-box"><div class="stat-lbl">Keys</div><div class="stat-val" id="vCount">0</div></div>
          <div class="stat-box"><div class="stat-lbl">CPM</div><div class="stat-val" id="vCpm">0</div></div>
          <div class="stat-box"><div class="stat-lbl">Dwell</div><div class="stat-val" id="vDwell">—</div></div>
          <div class="stat-box"><div class="stat-lbl">Readiness</div><div class="stat-val" id="vReady" style="color:#34d399;">0%</div></div>
        </div>
        <div class="prog-wrap"><div class="prog-fill" id="verifyProg" style="width:0%"></div></div>

        <button class="btn-action" id="btnVerify" onclick="doVerify()" disabled>🔐 VERIFY</button>
        <button class="btn-reset" onclick="resetVerify()">🔄 Reset</button>
      </div>

      <div class="spinner" id="verifySpinner"><span class="spin"></span>Verifying...</div>

      <div class="verify-result" id="verifyResult">
        <div class="verify-icon" id="vIcon"></div>
        <div class="verify-verdict" id="vVerdict"></div>
        <div class="verify-sim" id="vSim"></div>
        <div class="verify-exp" id="vExp"></div>
      </div>
    </div>

    <!-- ══ ANALYZE TAB ══════════════════════════════════════════════════════ -->
    <div id="tab-analyze" class="tab-panel">
      <div class="seg">
        <div class="seg-title">Quick analysis — view your profile</div>
        <textarea class="typing-box" id="analyzeInput" placeholder="Type anything..."
                  onkeydown="analyzeKeyDown(event)" onkeyup="analyzeKeyUp(event)"></textarea>

        <div class="stats-grid">
          <div class="stat-box"><div class="stat-lbl">Keys</div><div class="stat-val" id="aCount">0</div></div>
          <div class="stat-box"><div class="stat-lbl">CPM</div><div class="stat-val" id="aCpm">0</div></div>
          <div class="stat-box"><div class="stat-lbl">Dwell</div><div class="stat-val" id="aDwell">—</div></div>
          <div class="stat-box"><div class="stat-lbl">Uniqueness</div><div class="stat-val" id="aUnique" style="color:#34d399;">—</div></div>
        </div>
        <div class="prog-wrap"><div class="prog-fill" id="analyzeProg" style="width:0%"></div></div>

        <button class="btn-action" id="btnAnalyze" onclick="doAnalyze()" disabled>📊 ANALYZE</button>
        <button class="btn-reset" onclick="resetAnalyze()">🔄 Reset</button>
      </div>

      <div class="spinner" id="analyzeSpinner"><span class="spin"></span>Analyzing...</div>

      <div class="result-box" id="analyzeResult">
        <div class="metrics-grid">
          <div class="metric-box"><div class="metric-lbl">CPM</div><div class="metric-val" id="rCpm">—</div></div>
          <div class="metric-box"><div class="metric-lbl">Dwell avg</div><div class="metric-val"><span id="rDwell">—</span><span class="metric-unit"> ms</span></div></div>
          <div class="metric-box"><div class="metric-lbl">Uniqueness</div><div class="metric-val" style="color:#34d399;"><span id="rUnique">—</span><span class="metric-unit">%</span></div></div>
          <div class="metric-box"><div class="metric-lbl">Flight avg</div><div class="metric-val"><span id="rFlight">—</span><span class="metric-unit"> ms</span></div></div>
          <div class="metric-box"><div class="metric-lbl">Errors</div><div class="metric-val"><span id="rErr">—</span><span class="metric-unit">%</span></div></div>
          <div class="metric-box"><div class="metric-lbl">Risk</div><div class="metric-val" id="rRisk">—</div></div>
        </div>
        <div class="fp-display">
          <div class="fp-lbl">🔑 Fingerprint of this session</div>
          <div class="fp-hash" id="rHash">—</div>
        </div>
      </div>
    </div>

  </div>
</div>

<script>
  // ── State ────────────────────────────────────────────────────────────────
  const REQUIRED_KEYS = 8;
  const TOTAL_SESSIONS = 5;

  // Train state
  let trainEvents = [], trainPressMap = {}, trainStart = null;
  let completedSessions = []; // array of {events, cpm, avgDwell}

  // Verify state
  let verifyEvents = [], verifyPressMap = {}, verifyStart = null;

  // Analyze state
  let analyzeEvents = [], analyzePressMap = {}, analyzeStart = null;

  // ── Tab switch ────────────────────────────────────────────────────────────
  function switchTab(t) {
    ['train','verify','analyze'].forEach((id,i) => {
      document.querySelectorAll('.tab-btn')[i].classList.toggle('active', id===t);
      document.getElementById('tab-'+id).classList.toggle('active', id===t);
    });
  }

  // ── TRAIN ─────────────────────────────────────────────────────────────────
  function trainKeyDown(e) {
    if (!trainPressMap[e.code]) { trainPressMap[e.code] = Date.now(); if (!trainStart) trainStart = Date.now(); }
  }
  function trainKeyUp(e) {
    const p = trainPressMap[e.code];
    if (p) { trainEvents.push({key:e.key,pressTime:p,releaseTime:Date.now(),positionInWord:0}); delete trainPressMap[e.code]; updateTrainLive(); }
  }

  function updateTrainLive() {
    const n = trainEvents.length;
    const elapsed = trainStart ? (Date.now()-trainStart)/60000 : 0;
    const cpm = elapsed > 0 ? Math.round(n/elapsed) : 0;
    const dwells = trainEvents.map(e=>e.releaseTime-e.pressTime).filter(d=>d>=0&&d<2000);
    const avgDwell = dwells.length ? Math.round(dwells.reduce((a,b)=>a+b,0)/dwells.length) : 0;

    document.getElementById('tCount').textContent = n;
    document.getElementById('tCpm').textContent = cpm;
    document.getElementById('tDwell').textContent = avgDwell ? avgDwell+'ms' : '—';
    document.getElementById('tSessions').textContent = completedSessions.length+'/5';
    document.getElementById('trainProg').style.width = Math.min(100, n/REQUIRED_KEYS*100)+'%';
    document.getElementById('btnCapture').disabled = n < REQUIRED_KEYS;
    document.getElementById('trainInput').classList.toggle('recording', n > 0);
    updateSessionDots();
  }

  function updateSessionDots() {
    for (let i = 0; i < 5; i++) {
      const dot = document.getElementById('dot'+i);
      dot.className = 'session-dot';
      if (i < completedSessions.length) dot.classList.add('done');
      else if (i === completedSessions.length) dot.classList.add('current');
    }
    const label = document.getElementById('sessionLabel');
    if (completedSessions.length >= 5) label.textContent = '✅ All 5 sessions recorded!';
    else label.textContent = 'Session '+(completedSessions.length+1)+' of 5';
  }

  async function captureSession() {
    if (trainEvents.length < REQUIRED_KEYS) return;
    document.getElementById('trainSpinner').classList.add('show');
    document.getElementById('btnCapture').disabled = true;

    try {
      const res = await fetch('/biometrics/analyze', {
        method:'POST', headers:{'Content-Type':'application/json'},
        body: JSON.stringify({events: trainEvents})
      });
      const d = await res.json();
      if (d.error) { alert(d.error); return; }

      const elapsed = (Date.now()-trainStart)/60000;
      const cpm = Math.round(trainEvents.length/elapsed);
      completedSessions.push({events:[...trainEvents], cpm, avgDwell:d.avgDwellMs, hash:d.fingerprintHash});

      // Reset for next session
      trainEvents = []; trainPressMap = {}; trainStart = null;
      document.getElementById('trainInput').value = '';
      updateTrainLive();

      if (completedSessions.length >= TOTAL_SESSIONS) {
        await enrollFinal();
      } else {
        showSessionProgress();
      }
    } catch(ex) { alert('Error: '+ex.message); }
    finally { document.getElementById('trainSpinner').classList.remove('show'); }
  }

  function showSessionProgress() {
    const card = document.getElementById('avgCard');
    const container = document.getElementById('avgSessions');
    container.innerHTML = '';
    const maxCpm = Math.max(...completedSessions.map(s=>s.cpm));

    completedSessions.forEach((s,i) => {
      const row = document.createElement('div');
      row.className = 'avg-session-row';
      row.innerHTML =
        '<span style="width:55px;flex-shrink:0;">Session '+(i+1)+'</span>'+
        '<div class="avg-bar"><div class="avg-bar-fill" style="width:'+Math.round(s.cpm/maxCpm*100)+'%"></div></div>'+
        '<span class="avg-num">'+s.cpm+' CPM</span>';
      container.appendChild(row);
    });
    card.classList.add('show');
  }

  async function enrollFinal() {
    // Average all 5 sessions and enroll
    const allEvents = completedSessions.flatMap(s => s.events);
    document.getElementById('trainSpinner').classList.add('show');
    try {
      const res = await fetch('/biometrics/enroll', {
        method:'POST', headers:{'Content-Type':'application/json'},
        body: JSON.stringify({events: allEvents})
      });
      const d = await res.json();

      // Show average visualization
      showSessionProgress();
      const avgCpm = Math.round(completedSessions.reduce((a,s)=>a+s.cpm,0)/completedSessions.length);
      document.getElementById('avgCpm').textContent = avgCpm + ' CPM';
      document.getElementById('trainHash').textContent = d.fingerprintHash || '—';
      document.getElementById('avgCard').classList.add('show');
      alert('🎉 Profile created! Average CPM: '+avgCpm+'. You can now verify.');
    } catch(ex) { alert('Registration error: '+ex.message); }
    finally { document.getElementById('trainSpinner').classList.remove('show'); }
  }

  function resetTraining() {
    completedSessions = []; trainEvents = []; trainPressMap = {}; trainStart = null;
    document.getElementById('trainInput').value = '';
    document.getElementById('avgCard').classList.remove('show');
    document.getElementById('bindOk').classList.remove('show');
    document.getElementById('bindCheck').checked = false;
    document.getElementById('bindForm').style.display = 'none';
    updateTrainLive();
  }

  // ── Password bind ─────────────────────────────────────────────────────────
  function toggleBind(checked) {
    document.getElementById('bindForm').style.display = checked ? 'block' : 'none';
  }

  async function bindBiometrics() {
    const pwd = document.getElementById('bindPassword').value.trim();
    if (!pwd) { alert('Enter your password'); return; }
    if (completedSessions.length < 5) { alert('Complete 5 training sessions first'); return; }
    document.getElementById('btnBind').disabled = true;

    try {
      const allEvents = completedSessions.flatMap(s => s.events);
      // Re-enroll with the combined events (already done, just mark as password-bound in memory)
      const res = await fetch('/biometrics/enroll', {
        method:'POST', headers:{'Content-Type':'application/json'},
        body: JSON.stringify({events: allEvents, bindToPassword: true})
      });
      const d = await res.json();
      if (d.error) { alert(d.error); return; }
      document.getElementById('bindOk').classList.add('show');
      document.getElementById('bindPassword').value = '';
    } catch(ex) { alert('Error: '+ex.message); }
    finally { document.getElementById('btnBind').disabled = false; }
  }

  // ── VERIFY ────────────────────────────────────────────────────────────────
  function verifyKeyDown(e) { if (!verifyPressMap[e.code]) { verifyPressMap[e.code]=Date.now(); if(!verifyStart)verifyStart=Date.now(); } }
  function verifyKeyUp(e) {
    const p = verifyPressMap[e.code];
    if (p) { verifyEvents.push({key:e.key,pressTime:p,releaseTime:Date.now(),positionInWord:0}); delete verifyPressMap[e.code]; updateVerifyLive(); }
  }
  function updateVerifyLive() {
    const n = verifyEvents.length;
    const elapsed = verifyStart ? (Date.now()-verifyStart)/60000 : 0;
    const cpm = elapsed>0?Math.round(n/elapsed):0;
    const dwells=verifyEvents.map(e=>e.releaseTime-e.pressTime).filter(d=>d>=0&&d<2000);
    const avgD=dwells.length?Math.round(dwells.reduce((a,b)=>a+b,0)/dwells.length):0;
    document.getElementById('vCount').textContent=n;
    document.getElementById('vCpm').textContent=cpm;
    document.getElementById('vDwell').textContent=avgD?avgD+'ms':'—';
    document.getElementById('vReady').textContent=Math.min(100,Math.round(n/REQUIRED_KEYS*100))+'%';
    document.getElementById('verifyProg').style.width=Math.min(100,n/REQUIRED_KEYS*100)+'%';
    document.getElementById('btnVerify').disabled=n<REQUIRED_KEYS;
  }

  async function doVerify() {
    document.getElementById('verifySpinner').classList.add('show');
    document.getElementById('btnVerify').disabled=true;
    document.getElementById('verifyResult').classList.remove('show');
    try {
      const res=await fetch('/biometrics/verify',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({events:verifyEvents})});
      const d=await res.json();
      const el=document.getElementById('verifyResult');
      const isMatch=d.isMatch;
      el.className='verify-result show '+(isMatch?'match':'nomatch');
      const icons={STRONG_MATCH:'🟢',MATCH:'🟡',WEAK_MATCH:'🟠',NO_MATCH:'🔴'};
      const verdicts={STRONG_MATCH:'FULL MATCH',MATCH:'MATCH',WEAK_MATCH:'PARTIAL MATCH',NO_MATCH:'NO MATCH'};
      document.getElementById('vIcon').textContent=icons[d.verdict]||'❓';
      document.getElementById('vVerdict').textContent=verdicts[d.verdict]||d.verdict;
      document.getElementById('vVerdict').style.color=isMatch?'#4ade80':'#f87171';
      document.getElementById('vSim').textContent=d.similarity+'%';
      document.getElementById('vSim').style.color=isMatch?'#34d399':'#f87171';
      document.getElementById('vExp').textContent=d.explanation||'';
    } catch(ex){alert('Error: '+ex.message);}
    finally{document.getElementById('verifySpinner').classList.remove('show');document.getElementById('btnVerify').disabled=false;}
  }

  function resetVerify(){verifyEvents=[];verifyPressMap={};verifyStart=null;document.getElementById('verifyInput').value='';updateVerifyLive();document.getElementById('verifyResult').classList.remove('show');}

  // ── ANALYZE ───────────────────────────────────────────────────────────────
  function analyzeKeyDown(e){if(!analyzePressMap[e.code]){analyzePressMap[e.code]=Date.now();if(!analyzeStart)analyzeStart=Date.now();}}
  function analyzeKeyUp(e){
    const p=analyzePressMap[e.code];
    if(p){analyzeEvents.push({key:e.key,pressTime:p,releaseTime:Date.now(),positionInWord:0});delete analyzePressMap[e.code];updateAnalyzeLive();}
  }
  function updateAnalyzeLive(){
    const n=analyzeEvents.length;
    const elapsed=analyzeStart?(Date.now()-analyzeStart)/60000:0;
    const cpm=elapsed>0?Math.round(n/elapsed):0;
    document.getElementById('aCount').textContent=n;
    document.getElementById('aCpm').textContent=cpm;
    document.getElementById('analyzeProg').style.width=Math.min(100,n/REQUIRED_KEYS*100)+'%';
    document.getElementById('btnAnalyze').disabled=n<REQUIRED_KEYS;
  }
  async function doAnalyze(){
    document.getElementById('analyzeSpinner').classList.add('show');
    document.getElementById('btnAnalyze').disabled=true;
    try{
      const res=await fetch('/biometrics/analyze',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({events:analyzeEvents})});
      const d=await res.json();
      if(d.error){alert(d.error);return;}
      document.getElementById('rCpm').textContent=d.typingSpeedCpm;
      document.getElementById('rDwell').textContent=d.avgDwellMs;
      document.getElementById('rFlight').textContent=d.avgFlightMs;
      document.getElementById('rUnique').textContent=d.uniquenessScore;
      document.getElementById('rErr').textContent=d.errorRate;
      document.getElementById('rRisk').textContent=d.riskLevel;
      document.getElementById('rRisk').style.color=d.riskLevel==='LOW'?'#4ade80':d.riskLevel==='MEDIUM'?'#fbbf24':'#f87171';
      document.getElementById('rHash').textContent=d.fingerprintHash||'—';
      document.getElementById('aUnique').textContent=d.uniquenessScore+'%';
      document.getElementById('analyzeResult').classList.add('show');
    }catch(ex){alert('Error: '+ex.message);}
    finally{document.getElementById('analyzeSpinner').classList.remove('show');document.getElementById('btnAnalyze').disabled=false;}
  }
  function resetAnalyze(){analyzeEvents=[];analyzePressMap={};analyzeStart=null;document.getElementById('analyzeInput').value='';updateAnalyzeLive();document.getElementById('analyzeResult').classList.remove('show');}

  updateSessionDots();
</script>
</body>
</html>
