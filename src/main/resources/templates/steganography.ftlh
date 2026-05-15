<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>Steganography — SecretFrame</title>
  <link rel="stylesheet" href="/game.css">
  <style>
    :root { --steg-purple:#8b5cf6; --steg-cyan:#06b6d4; --steg-red:#ef4444; }

    .steg-hero { background:linear-gradient(135deg,rgba(139,92,246,.13),rgba(6,182,212,.07));
      border:1px solid rgba(139,92,246,.3); border-radius:var(--r-xl); padding:28px 32px; margin-bottom:24px; }
    .steg-hero-top { display:flex; align-items:center; gap:16px; margin-bottom:8px; }
    .steg-icon { font-size:2.6rem; }
    .steg-title { font-family:var(--font-game); font-size:1.35rem; font-weight:900; color:var(--text-white); letter-spacing:1px; }
    .steg-desc { font-family:var(--font-ui); font-size:0.83rem; color:var(--text-second); line-height:1.6; }
    .steg-flow { display:flex; align-items:center; gap:6px; flex-wrap:wrap; margin-top:14px; }
    .sf-step { background:rgba(139,92,246,.1); border:1px solid rgba(139,92,246,.2); border-radius:8px;
      padding:5px 12px; font-family:var(--font-ui); font-size:0.72rem; color:#c4b5fd; }
    .sf-arr { color:rgba(139,92,246,.5); font-size:0.85rem; }

    /* tabs */
    .tab-row { display:flex; gap:8px; margin-bottom:22px; }
    .tab-btn { font-family:var(--font-ui); font-size:0.78rem; font-weight:600; letter-spacing:1px;
      text-transform:uppercase; padding:9px 20px; border-radius:9px; border:1px solid var(--border);
      background:var(--bg-card2); color:var(--text-second); cursor:pointer; transition:all .2s; }
    .tab-btn.active { border-color:rgba(139,92,246,.55); color:#c4b5fd; background:rgba(139,92,246,.09); }
    .tab-panel { display:none; } .tab-panel.active { display:block; }

    /* fields */
    .field-label { font-family:var(--font-game); font-size:0.65rem; letter-spacing:2px;
      text-transform:uppercase; color:var(--text-muted); margin-bottom:6px; }
    textarea, input[type=text] { width:100%; background:var(--bg-card2); border:1px solid var(--border);
      border-radius:var(--r-md); padding:13px 15px; font-family:var(--font-ui); font-size:0.86rem;
      color:var(--text-primary); outline:none; resize:vertical; box-sizing:border-box; }
    textarea { min-height:130px; }
    textarea:focus, input[type=text]:focus { border-color:rgba(139,92,246,.45); }

    /* theme chips */
    .theme-row { display:flex; gap:8px; flex-wrap:wrap; margin-bottom:16px; }
    .theme-chip { background:var(--bg-card2); border:1px solid var(--border); border-radius:20px;
      padding:6px 14px; font-family:var(--font-ui); font-size:0.75rem; color:var(--text-second);
      cursor:pointer; transition:all .18s; display:flex; align-items:center; gap:5px; }
    .theme-chip:hover { border-color:rgba(139,92,246,.4); color:#c4b5fd; }
    .theme-chip.active { border-color:rgba(139,92,246,.6); color:#c4b5fd; background:rgba(139,92,246,.1); }

    /* upload zone */
    .upload-zone { border:2px dashed var(--border); border-radius:var(--r-md); padding:28px;
      text-align:center; cursor:pointer; transition:all .2s; background:var(--bg-card2); margin-bottom:14px; }
    .upload-zone:hover,.upload-zone.over { border-color:#8b5cf6; background:rgba(139,92,246,.05); }
    .uz-icon { font-size:2rem; margin-bottom:6px; }
    .uz-label { font-family:var(--font-ui); font-size:0.82rem; color:var(--text-second); }
    .uz-file { font-family:var(--font-ui); font-size:0.78rem; color:#8b5cf6; margin-top:5px; font-weight:600; }

    /* or divider */
    .or-divider { display:flex; align-items:center; gap:12px; margin:14px 0;
      font-family:var(--font-game); font-size:0.65rem; letter-spacing:2px; color:var(--text-muted); }
    .or-divider::before,.or-divider::after { content:''; flex:1; height:1px; background:var(--border); }

    /* btn */
    .btn-encrypt { width:100%; padding:14px; background:linear-gradient(135deg,#7c3aed,#0891b2);
      border:none; border-radius:var(--r-md); font-family:var(--font-game); font-size:0.85rem;
      font-weight:700; letter-spacing:2px; color:#fff; cursor:pointer; margin-top:6px; transition:opacity .2s; }
    .btn-encrypt:hover { opacity:.87; } .btn-encrypt:disabled { opacity:.4; cursor:not-allowed; }
    .btn-decrypt { width:100%; padding:13px; background:linear-gradient(135deg,#059669,#0284c7);
      border:none; border-radius:var(--r-md); font-family:var(--font-game); font-size:0.85rem;
      font-weight:700; letter-spacing:2px; color:#fff; cursor:pointer; margin-top:10px; transition:opacity .2s; }
    .btn-decrypt:hover { opacity:.87; } .btn-decrypt:disabled { opacity:.4; cursor:not-allowed; }

    /* spinner */
    .spinner { display:none; text-align:center; padding:28px 0; }
    .spinner.show { display:block; }
    .spin-ring { display:inline-block; width:32px; height:32px; border:3px solid var(--border);
      border-top-color:#8b5cf6; border-radius:50%; animation:spin .75s linear infinite; }
    .spin-label { font-family:var(--font-ui); font-size:0.82rem; color:var(--text-second); margin-top:10px; }
    @keyframes spin { to { transform:rotate(360deg); } }

    /* result card */
    .result-card { background:var(--bg-card2); border:1px solid var(--border); border-radius:var(--r-xl);
      padding:26px; margin-top:22px; display:none; }
    .result-card.show { display:block; }

    /* stego image preview */
    .stego-img-wrap { position:relative; border-radius:var(--r-lg); overflow:hidden; margin-bottom:18px;
      background:var(--bg-card3); display:flex; align-items:center; justify-content:center; min-height:180px; }
    .stego-img { max-width:100%; max-height:360px; border-radius:var(--r-lg); display:block; }
    .stego-img-badge { position:absolute; top:10px; right:10px; background:rgba(0,0,0,.65);
      backdrop-filter:blur(6px); border:1px solid rgba(139,92,246,.4); border-radius:8px;
      padding:4px 12px; font-family:var(--font-game); font-size:0.68rem; letter-spacing:1px; color:#c4b5fd; }

    /* key reveal box */
    .key-box { background:linear-gradient(135deg,rgba(239,68,68,.1),rgba(245,158,11,.07));
      border:2px solid rgba(239,68,68,.35); border-radius:var(--r-lg); padding:20px 24px; margin-bottom:16px; }
    .key-box-title { font-family:var(--font-game); font-size:0.72rem; letter-spacing:2px;
      text-transform:uppercase; color:#fca5a5; margin-bottom:10px; display:flex; align-items:center; gap:8px; }
    .key-token { font-family:monospace; font-size:1.3rem; font-weight:900; color:#fff;
      letter-spacing:4px; background:rgba(0,0,0,.3); border-radius:8px; padding:10px 16px;
      margin-bottom:12px; word-break:break-all; user-select:all; }
    .key-warning { font-family:var(--font-ui); font-size:0.78rem; color:#fca5a5; line-height:1.5; }

    /* countdown */
    .countdown-wrap { display:flex; align-items:center; gap:14px; margin-bottom:14px; }
    .countdown-ring-svg { flex-shrink:0; }
    .countdown-info { flex:1; }
    .countdown-num { font-family:var(--font-game); font-size:2rem; font-weight:900; color:#f87171; line-height:1; }
    .countdown-label { font-family:var(--font-ui); font-size:0.75rem; color:var(--text-muted); margin-top:2px; }
    .countdown-dead { font-family:var(--font-game); font-size:0.78rem; color:#6b7280; margin-top:4px;
      padding:6px 12px; background:rgba(0,0,0,.2); border-radius:6px; display:none; }
    .countdown-dead.show { display:block; }

    /* copy btn */
    .btn-copy { display:inline-flex; align-items:center; gap:6px; padding:8px 18px;
      background:rgba(139,92,246,.15); border:1px solid rgba(139,92,246,.35); border-radius:8px;
      font-family:var(--font-ui); font-size:0.78rem; font-weight:600; color:#c4b5fd;
      cursor:pointer; transition:all .18s; }
    .btn-copy:hover { background:rgba(139,92,246,.25); }
    .btn-copy.copied { color:#4ade80; border-color:rgba(34,197,94,.4); background:rgba(34,197,94,.1); }

    /* download btn */
    .btn-download { display:inline-flex; align-items:center; gap:8px; padding:10px 22px;
      background:linear-gradient(135deg,#7c3aed,#0891b2); border:none; border-radius:var(--r-md);
      font-family:var(--font-game); font-size:0.8rem; font-weight:700; letter-spacing:1px; color:#fff;
      cursor:pointer; transition:opacity .2s; text-decoration:none; }
    .btn-download:hover { opacity:.85; }

    /* decrypt result */
    .decrypt-result { background:rgba(34,197,94,.07); border:1px solid rgba(34,197,94,.25);
      border-radius:var(--r-xl); padding:22px; margin-top:18px; display:none; }
    .decrypt-result.show { display:block; }
    .decrypt-text { font-family:var(--font-ui); font-size:0.88rem; color:var(--text-primary);
      white-space:pre-wrap; word-break:break-word; line-height:1.7;
      background:var(--bg-card3); border-radius:var(--r-md); padding:14px; margin-top:10px; }
    .decrypt-file-box { display:flex; align-items:center; gap:14px; margin-top:10px;
      background:var(--bg-card3); border-radius:var(--r-md); padding:14px; }
    .decrypt-file-icon { font-size:2rem; }
    .decrypt-file-name { font-family:var(--font-ui); font-size:0.85rem; font-weight:700; color:var(--text-white); }
    .decrypt-file-size { font-family:var(--font-ui); font-size:0.72rem; color:var(--text-muted); margin-top:2px; }
    .decrypt-err { background:rgba(239,68,68,.08); border:1px solid rgba(239,68,68,.25);
      border-radius:var(--r-md); padding:14px 18px; margin-top:14px; display:none;
      font-family:var(--font-ui); font-size:0.82rem; color:#f87171; }
    .decrypt-err.show { display:block; }
  </style>
</head>
<body>
<div class="page-wrap">
  <nav class="topnav">
    <a href="/" class="nav-logo">HEALTH HUB</a>
    <div class="nav-links">
      <a href="/media-analysis" class="nav-link">🛡 Медіа</a>
      <a href="/query-proxy"    class="nav-link">🔒 Проксі</a>
      <a href="/steganography"  class="nav-link active">🖼 Стеганографія</a>
      <a href="/biometrics"     class="nav-link">🧬 Біометрія</a>
    </div>
  </nav>

  <div class="content-wrap" style="max-width:760px;margin:0 auto;">

    <!-- Hero -->
    <div class="steg-hero">
      <div class="steg-hero-top">
        <div class="steg-icon">🖼️</div>
        <div>
          <div class="steg-title">SECRET FRAME — Стеганографія</div>
        </div>
      </div>
      <div class="steg-desc">
        Твій текст або файл шифрується AES-256, потім ховається в красиву картинку згенеровану Gemini AI.
        Зображення виглядає як звичайне фото — ніхто не знає що всередині.
        Ключ доступний <strong style="color:#f87171">лише 30 секунд</strong> — після цього отримати його на платформі неможливо.
      </div>
      <div class="steg-flow">
        <div class="sf-step">✏️ Текст або файл</div><div class="sf-arr">→</div>
        <div class="sf-step">🔐 AES-256</div><div class="sf-arr">→</div>
        <div class="sf-step">🤖 Gemini генерує фото</div><div class="sf-arr">→</div>
        <div class="sf-step">👻 LSB embed</div><div class="sf-arr">→</div>
        <div class="sf-step">⏱ Ключ 30с</div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tab-row">
      <button class="tab-btn active" onclick="switchTab('encrypt')">🔐 Зашифрувати</button>
      <button class="tab-btn"        onclick="switchTab('decrypt')">🔓 Розшифрувати</button>
    </div>

    <!-- ══ ENCRYPT TAB ══════════════════════════════════════════════════ -->
    <div id="tab-encrypt" class="tab-panel active">

      <!-- Text input -->
      <div class="field-label">Секретний текст</div>
      <textarea id="secretText" placeholder="Введи текст який треба сховати..."></textarea>

      <div class="or-divider">або файл / фото</div>

      <!-- File upload -->
      <div class="upload-zone" id="dropZone"
           onclick="document.getElementById('fileInput').click()"
           ondragover="dov(event)" ondragleave="dol(event)" ondrop="dop(event)">
        <div class="uz-icon">📎</div>
        <div class="uz-label">Перетягни або клікни щоб завантажити файл / фото / документ</div>
        <div class="uz-file" id="uzFileName"></div>
      </div>
      <input type="file" id="fileInput" style="display:none" onchange="onFile()">

      <!-- Theme -->
      <div class="field-label">Тема обкладинки (Gemini згенерує фото)</div>
      <div class="theme-row" id="themeRow">
        <div class="theme-chip active" data-theme="A peaceful forest path in autumn, photorealistic">🌲 Ліс</div>
        <div class="theme-chip"       data-theme="A serene ocean beach at sunset, photorealistic">🌊 Океан</div>
        <div class="theme-chip"       data-theme="A cozy european city street with cafe lights at night, photorealistic">🏙 Місто</div>
        <div class="theme-chip"       data-theme="Abstract digital art with vibrant neon colors, futuristic">🎨 Абстракція</div>
        <div class="theme-chip"       data-theme="Mountain landscape with snow peaks and clear sky, photorealistic">🏔 Гори</div>
        <div class="theme-chip"       data-theme="Cherry blossom garden in spring, soft light, photorealistic">🌸 Сакура</div>
      </div>

      <button class="btn-encrypt" id="btnEncrypt" onclick="doEncrypt()">🔐 ЗАШИФРУВАТИ ТА СХОВАТИ</button>

      <!-- Spinner -->
      <div class="spinner" id="encSpinner">
        <div class="spin-ring"></div>
        <div class="spin-label">Gemini генерує зображення та ховає дані...</div>
      </div>

      <!-- Result -->
      <div class="result-card" id="encResult">

        <!-- Stego image -->
        <div class="stego-img-wrap">
          <img id="stegoImg" class="stego-img" alt="Stego image">
          <div class="stego-img-badge">🔐 Дані сховано всередині</div>
        </div>

        <!-- Key box -->
        <div class="key-box">
          <div class="key-box-title">
            🔑 ОДНОРАЗОВИЙ КЛЮЧ — СКОПІЮЙ ЗАРАЗ
          </div>

          <!-- Countdown -->
          <div class="countdown-wrap">
            <svg class="countdown-ring-svg" width="64" height="64" viewBox="0 0 64 64">
              <circle cx="32" cy="32" r="27" fill="none" stroke="rgba(239,68,68,.15)" stroke-width="5"/>
              <circle id="cdCircle" cx="32" cy="32" r="27" fill="none" stroke="#ef4444" stroke-width="5"
                      stroke-dasharray="169.6" stroke-dashoffset="0"
                      stroke-linecap="round" transform="rotate(-90 32 32)"/>
              <text x="32" y="37" text-anchor="middle" font-size="16" font-weight="900"
                    fill="#f87171" font-family="monospace" id="cdNum">30</text>
            </svg>
            <div class="countdown-info">
              <div class="countdown-num" id="cdBig">30</div>
              <div class="countdown-label">секунд залишилось</div>
              <div class="countdown-dead" id="cdDead">⛔ Ключ знищено. На платформі відновлення неможливе.</div>
            </div>
          </div>

          <div class="key-token" id="keyToken"></div>
          <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap;margin-bottom:12px;">
            <button class="btn-copy" id="btnCopy" onclick="copyKey()">📋 Копіювати ключ</button>
            <a id="btnDownloadImg" class="btn-download" download="secret_frame.png">⬇️ Завантажити зображення</a>
          </div>
          <div class="key-warning">
            ⚠️ Збережи ключ зараз — після 30 секунд він зникне назавжди. Без ключа розшифрувати зображення неможливо.
          </div>
        </div>

      </div>
    </div>

    <!-- ══ DECRYPT TAB ══════════════════════════════════════════════════ -->
    <div id="tab-decrypt" class="tab-panel">

      <div class="field-label" style="margin-top:4px;">Завантаж зашифроване зображення</div>
      <div class="upload-zone" id="dropZoneDec"
           onclick="document.getElementById('decImage').click()"
           ondragover="dov2(event)" ondragleave="dol2(event)" ondrop="dop2(event)">
        <div class="uz-icon">🖼️</div>
        <div class="uz-label">Завантаж PNG зображення зі схованими даними</div>
        <div class="uz-file" id="decFileName"></div>
      </div>
      <input type="file" id="decImage" accept="image/png" style="display:none" onchange="onDecFile()">

      <div class="field-label" style="margin-top:16px;">Ключ доступу</div>
      <input type="text" id="decKey" placeholder="XXXXXXXXXXXXXXXXXXXX" style="font-family:monospace;font-size:1rem;letter-spacing:3px;text-transform:uppercase">

      <button class="btn-decrypt" id="btnDecrypt" onclick="doDecrypt()">🔓 РОЗШИФРУВАТИ</button>

      <div class="spinner" id="decSpinner">
        <div class="spin-ring" style="border-top-color:#10b981"></div>
        <div class="spin-label">Витягуємо та розшифровуємо дані...</div>
      </div>

      <div class="decrypt-err" id="decErr"></div>

      <div class="decrypt-result" id="decResult">
        <div style="font-family:var(--font-game);font-size:0.72rem;letter-spacing:2px;text-transform:uppercase;color:#4ade80;margin-bottom:10px;">
          ✅ РОЗШИФРОВАНО УСПІШНО
        </div>
        <div id="decContent"></div>
      </div>

    </div>

  </div><!-- content-wrap -->
</div>

<script>
  // ── State ──────────────────────────────────────────────────────────────
  let selectedFile = null;
  let selectedDecFile = null;
  let currentKey = null;
  let countdownInterval = null;
  const TOTAL = 30;
  const CIRCUM = 169.6;

  // ── Tab switch ─────────────────────────────────────────────────────────
  function switchTab(tab) {
    document.querySelectorAll('.tab-btn').forEach((b,i) => b.classList.toggle('active', (i===0&&tab==='encrypt')||(i===1&&tab==='decrypt')));
    document.getElementById('tab-encrypt').classList.toggle('active', tab==='encrypt');
    document.getElementById('tab-decrypt').classList.toggle('active', tab==='decrypt');
  }

  // ── Theme chips ────────────────────────────────────────────────────────
  document.querySelectorAll('.theme-chip').forEach(c => c.addEventListener('click', () => {
    document.querySelectorAll('.theme-chip').forEach(x => x.classList.remove('active'));
    c.classList.add('active');
  }));
  function getTheme() {
    const active = document.querySelector('.theme-chip.active');
    return active ? active.dataset.theme : '';
  }

  // ── File upload (encrypt) ──────────────────────────────────────────────
  function dov(e){e.preventDefault();document.getElementById('dropZone').classList.add('over');}
  function dol(){document.getElementById('dropZone').classList.remove('over');}
  function dop(e){e.preventDefault();dol();const f=e.dataTransfer.files[0];if(f)setFile(f);}
  function onFile(){const f=document.getElementById('fileInput').files[0];if(f)setFile(f);}
  function setFile(f){selectedFile=f;document.getElementById('uzFileName').textContent='📄 '+f.name+' ('+fmtSize(f.size)+')';}

  // ── File upload (decrypt) ──────────────────────────────────────────────
  function dov2(e){e.preventDefault();document.getElementById('dropZoneDec').classList.add('over');}
  function dol2(){document.getElementById('dropZoneDec').classList.remove('over');}
  function dop2(e){e.preventDefault();dol2();const f=e.dataTransfer.files[0];if(f){selectedDecFile=f;document.getElementById('decFileName').textContent='🖼 '+f.name;}}
  function onDecFile(){const f=document.getElementById('decImage').files[0];if(f){selectedDecFile=f;document.getElementById('decFileName').textContent='🖼 '+f.name;}}

  // ── ENCRYPT ───────────────────────────────────────────────────────────
  async function doEncrypt() {
    const text = document.getElementById('secretText').value.trim();
    if (!text && !selectedFile) { alert('Введи текст або завантаж файл'); return; }

    setLoading('enc', true);
    document.getElementById('encResult').classList.remove('show');

    try {
      const fd = new FormData();
      if (text) fd.append('text', text);
      if (selectedFile) fd.append('file', selectedFile);
      fd.append('imageTheme', getTheme());

      const res  = await fetch('/steganography/encrypt', { method:'POST', body:fd });
      const data = await res.json();

      if (data.error) { alert('Помилка: ' + data.error); return; }

      // Show stego image
      const imgSrc = 'data:'+data.mimeType+';base64,'+data.stegoImageBase64;
      document.getElementById('stegoImg').src = imgSrc;
      const dlLink = document.getElementById('btnDownloadImg');
      dlLink.href = imgSrc;

      // Show key
      currentKey = data.key;
      document.getElementById('keyToken').textContent = fmtKey(data.key);

      document.getElementById('encResult').classList.add('show');
      startCountdown(data.expiresIn || 30);
      document.getElementById('encResult').scrollIntoView({behavior:'smooth'});

    } catch(e) { alert('Помилка: '+e.message); }
    finally { setLoading('enc', false); }
  }

  // ── Countdown ─────────────────────────────────────────────────────────
  function startCountdown(seconds) {
    clearInterval(countdownInterval);
    let remaining = seconds;
    const circle  = document.getElementById('cdCircle');
    const cdBig   = document.getElementById('cdBig');
    const cdNum   = document.getElementById('cdNum');
    const cdDead  = document.getElementById('cdDead');
    const btnCopy = document.getElementById('btnCopy');
    cdDead.classList.remove('show');
    btnCopy.classList.remove('copied');
    btnCopy.disabled = false;

    function tick() {
      const pct = remaining / TOTAL;
      circle.style.strokeDashoffset = CIRCUM * (1 - pct);
      circle.style.stroke = remaining > 15 ? '#ef4444' : remaining > 7 ? '#f59e0b' : '#9ca3af';
      cdBig.textContent = remaining;
      cdBig.style.color = remaining > 15 ? '#f87171' : remaining > 7 ? '#fbbf24' : '#6b7280';
      cdNum.textContent = remaining;

      if (remaining <= 0) {
        clearInterval(countdownInterval);
        cdDead.classList.add('show');
        document.getElementById('btnCopy').disabled = true;
      }
      remaining--;
    }
    tick();
    countdownInterval = setInterval(tick, 1000);
  }

  // ── Copy key ──────────────────────────────────────────────────────────
  function copyKey() {
    if (!currentKey) return;
    navigator.clipboard.writeText(currentKey).then(() => {
      const btn = document.getElementById('btnCopy');
      btn.textContent = '✅ Скопійовано!';
      btn.classList.add('copied');
      setTimeout(() => { btn.textContent = '📋 Копіювати ключ'; btn.classList.remove('copied'); }, 2000);
    });
  }

  function fmtKey(k) {
    // Format as XXXX-XXXX-XXXX-XXXX-XXXX-XXXX
    return k.match(/.{1,4}/g).join('-');
  }

  // ── DECRYPT ───────────────────────────────────────────────────────────
  async function doDecrypt() {
    if (!selectedDecFile) { alert('Завантаж зображення'); return; }
    const key = document.getElementById('decKey').value.trim();
    if (!key) { alert('Введи ключ'); return; }

    setLoading('dec', true);
    document.getElementById('decResult').classList.remove('show');
    document.getElementById('decErr').classList.remove('show');

    try {
      const fd = new FormData();
      fd.append('image', selectedDecFile);
      fd.append('key', key.replace(/-/g,''));

      const res  = await fetch('/steganography/decrypt', { method:'POST', body:fd });
      const data = await res.json();

      if (!data.success) {
        const errEl = document.getElementById('decErr');
        errEl.textContent = '⛔ ' + (data.error || 'Помилка розшифрування');
        errEl.classList.add('show');
        return;
      }

      const content = document.getElementById('decContent');
      if (data.type === 'text') {
        content.innerHTML = '<div class="decrypt-text">' + escHtml(data.plaintext) + '</div>';
      } else {
        // File — offer download
        const bytes = atob(data.fileBase64);
        const arr   = new Uint8Array(bytes.length);
        for(let i=0;i<bytes.length;i++) arr[i]=bytes.charCodeAt(i);
        const blob  = new Blob([arr]);
        const url   = URL.createObjectURL(blob);
        content.innerHTML =
          '<div class="decrypt-file-box">' +
            '<div class="decrypt-file-icon">' + fileIcon(data.fileName) + '</div>' +
            '<div>' +
              '<div class="decrypt-file-name">' + escHtml(data.fileName) + '</div>' +
              '<div class="decrypt-file-size">' + fmtSize(arr.length) + '</div>' +
            '</div>' +
            '<a href="' + url + '" download="' + escHtml(data.fileName) + '" class="btn-download" style="margin-left:auto;">⬇️ Завантажити</a>' +
          '</div>';
      }
      document.getElementById('decResult').classList.add('show');
      document.getElementById('decResult').scrollIntoView({behavior:'smooth'});

    } catch(e) { alert('Помилка: '+e.message); }
    finally { setLoading('dec', false); }
  }

  // ── Helpers ───────────────────────────────────────────────────────────
  function setLoading(mode, on) {
    document.getElementById(mode+'Spinner').classList.toggle('show', on);
    document.getElementById('btn'+(mode==='enc'?'Encrypt':'Decrypt')).disabled = on;
  }
  function fmtSize(b) {
    if(b<1024)return b+' B'; if(b<1048576)return (b/1024).toFixed(1)+' KB'; return (b/1048576).toFixed(1)+' MB';
  }
  function escHtml(s) {
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  }
  function fileIcon(name) {
    if(!name)return '📄'; const ext=name.split('.').pop().toLowerCase();
    const m={jpg:'🖼',jpeg:'🖼',png:'🖼',gif:'🖼',webp:'🖼',pdf:'📕',doc:'📘',docx:'📘',
             xls:'📗',xlsx:'📗',txt:'📄',zip:'🗜',mp4:'🎬',mp3:'🎵'};
    return m[ext]||'📄';
  }
</script>
</body>
</html>
