<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>Media Analysis — Media Shield</title>

  <meta name="_csrf" content="${_csrf.token}"/>
  <meta name="_csrf_header" content="${_csrf.headerName}"/>

  <link rel="stylesheet" href="/game.css">
  <style>
    .module-hero{background:linear-gradient(135deg,rgba(239,68,68,.12),rgba(245,158,11,.08));border:1px solid rgba(239,68,68,.25);border-radius:var(--r-xl);padding:28px 32px;margin-bottom:24px;}
    .module-icon{font-size:3rem;margin-bottom:10px;}
    .module-title{font-family:var(--font-game);font-size:1.4rem;font-weight:900;color:var(--text-white);letter-spacing:1px;}
    .module-desc{font-family:var(--font-ui);font-size:0.85rem;color:var(--text-second);margin-top:6px;}
    .tab-row{display:flex;gap:8px;margin-bottom:20px;}
    .tab-btn{font-family:var(--font-ui);font-size:0.78rem;font-weight:600;letter-spacing:1px;text-transform:uppercase;padding:8px 18px;border-radius:8px;border:1px solid var(--border);background:var(--bg-card2);color:var(--text-second);cursor:pointer;transition:all .2s;}
    .tab-btn.active{border-color:rgba(239,68,68,.5);color:#f87171;background:rgba(239,68,68,.08);}
    .tab-panel{display:none;}.tab-panel.active{display:block;}
    textarea{width:100%;min-height:180px;background:var(--bg-card2);border:1px solid var(--border);border-radius:var(--r-md);padding:14px;font-family:var(--font-ui);font-size:0.85rem;color:var(--text-primary);resize:vertical;outline:none;box-sizing:border-box;}
    textarea:focus{border-color:rgba(239,68,68,.4);}
    .upload-zone{border:2px dashed var(--border);border-radius:var(--r-md);padding:40px;text-align:center;cursor:pointer;transition:all .2s;background:var(--bg-card2);}
    .upload-zone:hover,.upload-zone.dragover{border-color:#f87171;background:rgba(239,68,68,.05);}
    .upload-icon{font-size:2.5rem;margin-bottom:8px;}
    .upload-label{font-family:var(--font-ui);font-size:0.85rem;color:var(--text-second);}
    .btn-analyze{width:100%;padding:13px;background:linear-gradient(135deg,#dc2626,#b91c1c);border:none;border-radius:var(--r-md);font-family:var(--font-game);font-size:0.85rem;font-weight:700;letter-spacing:2px;color:#fff;cursor:pointer;margin-top:14px;transition:opacity .2s;}
    .btn-analyze:hover{opacity:.85;}
    .btn-analyze:disabled{opacity:.4;cursor:not-allowed;}
    /* Results */
    .result-card{background:var(--bg-card2);border:1px solid var(--border);border-radius:var(--r-xl);padding:24px;margin-top:20px;display:none;}
    .result-card.show{display:block;}
    .risk-badge{display:inline-flex;align-items:center;gap:8px;padding:6px 16px;border-radius:20px;font-family:var(--font-game);font-size:0.78rem;font-weight:700;letter-spacing:1px;margin-bottom:18px;}
    .risk-LOW{background:rgba(34,197,94,.12);border:1px solid rgba(34,197,94,.3);color:#4ade80;}
    .risk-MEDIUM{background:rgba(245,158,11,.12);border:1px solid rgba(245,158,11,.3);color:#fbbf24;}
    .risk-HIGH{background:rgba(239,68,68,.12);border:1px solid rgba(239,68,68,.3);color:#f87171;}
    .risk-UNKNOWN{background:rgba(100,116,139,.12);border:1px solid rgba(100,116,139,.3);color:#94a3b8;}
    .metric-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin:16px 0;}
    .metric-box{background:var(--bg-card3);border-radius:var(--r-md);padding:14px;text-align:center;}
    .metric-label{font-family:var(--font-game);font-size:0.62rem;letter-spacing:2px;text-transform:uppercase;color:var(--text-muted);margin-bottom:6px;}
    .metric-val{font-family:var(--font-game);font-size:1.5rem;font-weight:900;}
    .metric-detected{color:#f87171;}.metric-ok{color:#4ade80;}.metric-warn{color:#fbbf24;}
    .score-bar-wrap{height:6px;background:var(--bg-card3);border-radius:3px;margin-top:6px;overflow:hidden;}
    .score-bar{height:100%;border-radius:3px;transition:width .6s ease;}
    .detail-section{margin-top:16px;}
    .detail-title{font-family:var(--font-game);font-size:0.7rem;letter-spacing:2px;text-transform:uppercase;color:var(--text-muted);margin-bottom:10px;}
    .detail-row{display:flex;align-items:flex-start;gap:10px;padding:10px 0;border-bottom:1px solid var(--border);}
    .detail-row:last-child{border-bottom:none;}
    .detail-icon{font-size:1.1rem;flex-shrink:0;margin-top:1px;}
    .detail-name{font-family:var(--font-ui);font-size:0.8rem;font-weight:700;color:var(--text-white);}
    .detail-exp{font-family:var(--font-ui);font-size:0.77rem;color:var(--text-second);margin-top:2px;}
    .techniques-wrap{display:flex;flex-wrap:wrap;gap:6px;margin-top:6px;}
    .technique-tag{background:rgba(239,68,68,.1);border:1px solid rgba(239,68,68,.2);border-radius:6px;padding:3px 10px;font-family:var(--font-ui);font-size:0.7rem;color:#f87171;}
    .summary-box{background:linear-gradient(135deg,rgba(59,130,246,.06),rgba(139,92,246,.04));border:1px solid rgba(59,130,246,.2);border-radius:var(--r-md);padding:14px 18px;margin-top:14px;}
    .summary-text{font-family:var(--font-ui);font-size:0.84rem;color:var(--text-primary);line-height:1.6;}
    .rec-text{font-family:var(--font-ui);font-size:0.8rem;color:var(--cyan);margin-top:8px;font-style:italic;}
    .spinner{display:none;text-align:center;padding:30px;font-family:var(--font-ui);color:var(--text-second);font-size:0.85rem;}
    .spinner.show{display:block;}
    .spin{display:inline-block;width:20px;height:20px;border:2px solid var(--border);border-top-color:#f87171;border-radius:50%;animation:spin .7s linear infinite;vertical-align:middle;margin-right:8px;}
    @keyframes spin{to{transform:rotate(360deg);}}
    .char-count{font-family:var(--font-ui);font-size:0.7rem;color:var(--text-muted);text-align:right;margin-top:4px;}
  </style>
</head>
<body>
<div class="page-wrap">
  <!-- Nav -->
  <header class="gh">
  <a href="/" class="gh-logo"><div class="logo-gem">🛡</div>SECURE<span>MIND</span></a>
  <nav class="gh-nav">
    <a href="/media-analysis" class="nb active-nav">🛡 Media</a>
    <a href="/query-proxy" class="nb">🔒 Proxy</a>
    <a href="/steganography" class="nb">🖼 Steganography</a>
    <a href="/biometrics" class="nb">🧬 Biometrics</a>
  </nav>
  <div class="gh-right">
    <form action="/logout" method="post" style="margin:0;">
      <input type="hidden" name="${(_csrf.parameterName)!''}" value="${(_csrf.token)!''}">
      <button type="submit" class="nb" style="cursor:pointer;background:none;border:none;color:var(--text-muted);">Sign out</button>
    </form>
  </div>
</header>

  <div class="content-wrap" style="max-width:760px;margin:0 auto;">

    <!-- Hero -->
    <div class="module-hero">
      <div class="module-icon">🛡️</div>
      <div class="module-title">MEDIA SHIELD — Media Analysis</div>
      <div class="module-desc">Detect AI bias, fake news, and propaganda in any text.
        Upload an article, news item, or post — get a detailed analysis in seconds.</div>
    </div>

    <!-- Tabs -->
    <div class="tab-row">
      <button class="tab-btn active" onclick="switchTab('text')">✏️ Text</button>
      <button class="tab-btn" onclick="switchTab('file')">📄 File</button>
    </div>

    <!-- Text tab -->
    <div id="tab-text" class="tab-panel active">
      <textarea id="textInput" placeholder="Paste the text of an article, news item, or post for analysis..."
                oninput="updateCount()"></textarea>
      <div class="char-count"><span id="charCount">0</span> characters</div>
      <button class="btn-analyze" id="btnText" onclick="analyzeText()">🔍 ANALYZE TEXT</button>
    </div>

    <!-- File tab -->
    <div id="tab-file" class="tab-panel">
      <div class="upload-zone" id="dropZone" onclick="document.getElementById('fileInput').click()"
           ondragover="onDragOver(event)" ondragleave="onDragLeave(event)" ondrop="onDrop(event)">
        <div class="upload-icon">📂</div>
        <div class="upload-label">Click or drag .txt / .html / .md file</div>
        <div class="upload-label" id="fileName" style="color:var(--cyan);margin-top:6px;"></div>
      </div>
      <input type="file" id="fileInput" accept=".txt,.html,.md,.htm" style="display:none"
             onchange="onFileSelected()">
      <button class="btn-analyze" id="btnFile" onclick="analyzeFile()">🔍 ANALYZE FILE</button>
    </div>

    <!-- Spinner -->
    <div class="spinner" id="spinner"><span class="spin"></span>AI is analyzing text...</div>

    <!-- Result -->
    <div class="result-card" id="resultCard">
      <div id="riskBadge" class="risk-badge"></div>

      <div class="metric-grid">
        <div class="metric-box">
          <div class="metric-label">AI Bias</div>
          <div class="metric-val" id="mAiBias">—</div>
          <div class="score-bar-wrap"><div class="score-bar" id="barAiBias"></div></div>
        </div>
        <div class="metric-box">
          <div class="metric-label">Fake News</div>
          <div class="metric-val" id="mFake">—</div>
          <div class="score-bar-wrap"><div class="score-bar" id="barFake"></div></div>
        </div>
        <div class="metric-box">
          <div class="metric-label">Propaganda</div>
          <div class="metric-val" id="mProp">—</div>
          <div class="score-bar-wrap"><div class="score-bar" id="barProp"></div></div>
        </div>
      </div>

      <div class="detail-section">
        <div class="detail-title">Detailed breakdown</div>
        <div class="detail-row">
          <div class="detail-icon" id="icoAi">🤖</div>
          <div>
            <div class="detail-name">AI Bias</div>
            <div class="detail-exp" id="expAi"></div>
          </div>
        </div>
        <div class="detail-row">
          <div class="detail-icon" id="icoFake">📰</div>
          <div>
            <div class="detail-name">Fake News</div>
            <div class="detail-exp" id="expFake"></div>
          </div>
        </div>
        <div class="detail-row">
          <div class="detail-icon" id="icoProp">📢</div>
          <div>
            <div class="detail-name">Propaganda</div>
            <div class="detail-exp" id="expProp"></div>
            <div class="techniques-wrap" id="techniques"></div>
          </div>
        </div>
      </div>

      <div class="summary-box">
        <div class="summary-text" id="summary"></div>
        <div class="rec-text" id="recommendation"></div>
      </div>
    </div>

  </div>
</div>

<script>
  let selectedFile = null;

  function switchTab(tab) {
    document.querySelectorAll('.tab-btn').forEach((b,i)=>b.classList.toggle('active', (i===0&&tab==='text')||(i===1&&tab==='file')));
    document.getElementById('tab-text').classList.toggle('active', tab==='text');
    document.getElementById('tab-file').classList.toggle('active', tab==='file');
  }

  function updateCount() {
    document.getElementById('charCount').textContent = document.getElementById('textInput').value.length;
  }

  function onDragOver(e){e.preventDefault();document.getElementById('dropZone').classList.add('dragover');}
  function onDragLeave(){document.getElementById('dropZone').classList.remove('dragover');}
  function onDrop(e){e.preventDefault();onDragLeave();const f=e.dataTransfer.files[0];if(f){selectedFile=f;document.getElementById('fileName').textContent='📄 '+f.name;}}
  function onFileSelected(){const f=document.getElementById('fileInput').files[0];if(f){selectedFile=f;document.getElementById('fileName').textContent='📄 '+f.name;}}

  async function analyzeText() {
  const text = document.getElementById('textInput').value.trim();
  if (!text || text.length < 30) { alert('Enter at least 30 characters'); return; }
  setLoading(true);
  try {
    const res = await fetch('/media-analysis/analyze', {
      method:'POST', headers:{'Content-Type':'application/json'},
      body: JSON.stringify({text})
    });

    if (!res.ok) {
      throw new Error('Server returned status ' + res.status);
    }

    const data = await res.json();

    // If the server returned an error inside the JSON
    if (data.error) {
      alert('Service error: ' + data.error);
      return;
    }

    renderResult(data);
  } catch(e) {
    alert('Server connection error: ' + e.message);
  } finally {
    setLoading(false);
  }
}

  async function analyzeFile() {
    if (!selectedFile) { alert('Please select a file'); return; }
    setLoading(true);
    try {
      const fd = new FormData();
      fd.append('file', selectedFile);
      const res = await fetch('/media-analysis/upload', {method:'POST', body:fd});
      const data = await res.json();
      renderResult(data);
    } catch(e) { alert('Error: '+e.message); }
    finally { setLoading(false); }
  }

  function setLoading(on) {
    document.getElementById('spinner').classList.toggle('show', on);
    document.getElementById('resultCard').classList.toggle('show', false);
    document.getElementById('btnText').disabled = on;
    document.getElementById('btnFile').disabled = on;
  }

  function scoreColor(score) {
    if (score < 35) return '#4ade80';
    if (score < 65) return '#fbbf24';
    return '#f87171';
  }

function renderResult(d) {
  if (!d || d.error) { alert('Error: ' + (d ? d.error : 'empty data')); return; }

  console.log("Rendering results. Input object:", d);

  const risk = d.overallRisk || d.overall_risk || 'UNKNOWN';
  const riskLabels = {LOW:'✅ LOW RISK', MEDIUM:'⚠️ MEDIUM RISK', HIGH:'🚨 HIGH RISK', UNKNOWN:'❓ UNKNOWN'};
  const badge = document.getElementById('riskBadge');
  badge.textContent = riskLabels[risk] || risk;
  badge.className = 'risk-badge risk-' + risk;

  // Flexible object search
  const ai = d.aiBias || d.ai_bias || d.aibias || {};
  const fk = d.fakeNews || d.fake_news || d.fakenews || {};
  const pr = d.propaganda || d.propagand_info || {};

  // Fixed metric logic: detected status depends on the MAGNITUDE of the risk percentage
  function setMetric(valId, barId, score) {
    const el = document.getElementById(valId);
    if (!el) return;

    const finalScore = score !== undefined ? score : 0;
    el.textContent = finalScore + '%';

    // If risk is above 45% — dangerous (red), if below — all good (green)
    const isDangerous = finalScore > 45;
    el.className = 'metric-val ' + (isDangerous ? 'metric-detected' : 'metric-ok');

    const bar = document.getElementById(barId);
    if (bar) {
      bar.style.width = finalScore + '%';
      bar.style.background = scoreColor(finalScore);
    }
    return isDangerous;
  }

  // Update percentages and get the real danger status
  const aiDanger = setMetric('mAiBias', 'barAiBias', ai.score || 0);
  const fkDanger = setMetric('mFake',   'barFake',   fk.score || 0);
  const prDanger = setMetric('mProp',   'barProp',   pr.score || 0);

  // Set emoji icons according to the actual threat level
  document.getElementById('icoAi').textContent   = aiDanger ? '🚨' : '✅';
  document.getElementById('icoFake').textContent = fkDanger ? '🚨' : '✅';
  document.getElementById('icoProp').textContent = prDanger ? '🚨' : '✅';

  function setExp(id, val) {
    const el = document.getElementById(id);
    if (el) {
      el.textContent = val || 'No signs of manipulation or disinformation detected.';
      el.style.display = 'block';
    }
  }
  setExp('expAi',   ai.explanation || ai.desc);
  setExp('expFake', fk.explanation || fk.desc);
  setExp('expProp', pr.explanation || pr.desc);

  const tWrap = document.getElementById('techniques');
  if (tWrap) {
    tWrap.innerHTML = '';
    const techniques = pr.techniques || pr.technique_list || [];
    techniques.forEach(t => {
      const span = document.createElement('span');
      span.className = 'technique-tag';
      span.textContent = t;
      tWrap.appendChild(span);
    });
  }

  document.getElementById('summary').textContent        = d.summary || 'Analysis complete.';
  document.getElementById('recommendation').textContent = d.recommendation ? '💡 ' + d.recommendation : '';

  // Force-show the results block
  const resultCard = document.getElementById('resultCard');
  resultCard.classList.add('show');
  resultCard.style.display = 'block';

  resultCard.scrollIntoView({behavior:'smooth', block:'start'});
}
</script>
</body>
</html>
