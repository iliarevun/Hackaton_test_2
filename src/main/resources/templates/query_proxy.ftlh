<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>Query Proxy — Privacy Shield</title>
  <link rel="stylesheet" href="/game.css">
  <style>
    .module-hero{background:linear-gradient(135deg,rgba(59,130,246,.12),rgba(139,92,246,.08));border:1px solid rgba(59,130,246,.25);border-radius:var(--r-xl);padding:28px 32px;margin-bottom:24px;}
    .module-icon{font-size:3rem;margin-bottom:10px;}
    .module-title{font-family:var(--font-game);font-size:1.4rem;font-weight:900;color:var(--text-white);letter-spacing:1px;}
    .module-desc{font-family:var(--font-ui);font-size:0.85rem;color:var(--text-second);margin-top:6px;}
    .two-col{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:16px;}
    @media(max-width:600px){.two-col{grid-template-columns:1fr;}}
    .field-label{font-family:var(--font-game);font-size:0.68rem;letter-spacing:2px;text-transform:uppercase;color:var(--text-muted);margin-bottom:6px;}
    textarea,input[type=text]{width:100%;background:var(--bg-card2);border:1px solid var(--border);border-radius:var(--r-md);padding:12px 14px;font-family:var(--font-ui);font-size:0.85rem;color:var(--text-primary);outline:none;resize:vertical;box-sizing:border-box;}
    textarea{min-height:130px;}
    textarea:focus,input[type=text]:focus{border-color:rgba(59,130,246,.4);}
    .btn-row{display:flex;gap:10px;margin-top:4px;}
    .btn-primary{flex:1;padding:12px;background:linear-gradient(135deg,#2563eb,#1d4ed8);border:none;border-radius:var(--r-md);font-family:var(--font-game);font-size:0.82rem;font-weight:700;letter-spacing:2px;color:#fff;cursor:pointer;transition:opacity .2s;}
    .btn-primary:hover{opacity:.85;}
    .btn-secondary{padding:12px 20px;background:var(--bg-card2);border:1px solid var(--border);border-radius:var(--r-md);font-family:var(--font-game);font-size:0.78rem;font-weight:700;letter-spacing:1px;color:var(--text-second);cursor:pointer;transition:all .2s;}
    .btn-secondary:hover{border-color:rgba(59,130,246,.4);color:var(--cyan);}
    .btn-primary:disabled,.btn-secondary:disabled{opacity:.4;cursor:not-allowed;}
    .spinner{display:none;text-align:center;padding:20px;font-family:var(--font-ui);color:var(--text-second);font-size:0.85rem;}
    .spinner.show{display:block;}
    .spin{display:inline-block;width:18px;height:18px;border:2px solid var(--border);border-top-color:#3b82f6;border-radius:50%;animation:spin .7s linear infinite;vertical-align:middle;margin-right:8px;}
    @keyframes spin{to{transform:rotate(360deg);}}
    /* PII preview */
    .pii-card{background:var(--bg-card2);border:1px solid var(--border);border-radius:var(--r-xl);padding:20px;margin-top:16px;display:none;}
    .pii-card.show{display:block;}
    .pii-header{display:flex;align-items:center;gap:10px;margin-bottom:14px;}
    .pii-title{font-family:var(--font-game);font-size:0.78rem;letter-spacing:2px;text-transform:uppercase;color:var(--cyan);}
    .pii-count-badge{background:rgba(59,130,246,.15);border:1px solid rgba(59,130,246,.3);border-radius:12px;padding:2px 10px;font-family:var(--font-game);font-size:0.72rem;color:#60a5fa;}
    .pii-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;}
    .pii-box{background:var(--bg-card3);border-radius:var(--r-md);padding:12px;}
    .pii-box-label{font-family:var(--font-game);font-size:0.62rem;letter-spacing:1.5px;text-transform:uppercase;color:var(--text-muted);margin-bottom:6px;}
    .pii-box-text{font-family:var(--font-ui);font-size:0.78rem;color:var(--text-primary);word-break:break-all;line-height:1.5;}
    .pii-tag{display:inline-block;background:rgba(239,68,68,.1);border:1px solid rgba(239,68,68,.25);border-radius:6px;padding:2px 8px;font-size:0.72rem;color:#f87171;margin:2px;}
    /* Result */
    .result-card{background:var(--bg-card2);border:1px solid var(--border);border-radius:var(--r-xl);padding:24px;margin-top:16px;display:none;}
    .result-card.show{display:block;}
    .result-label{font-family:var(--font-game);font-size:0.68rem;letter-spacing:2px;text-transform:uppercase;color:var(--text-muted);margin-bottom:8px;}
    .result-text{font-family:var(--font-ui);font-size:0.86rem;color:var(--text-primary);line-height:1.7;white-space:pre-wrap;word-break:break-word;}
    .privacy-badge{display:inline-flex;align-items:center;gap:6px;background:rgba(34,197,94,.1);border:1px solid rgba(34,197,94,.25);border-radius:20px;padding:4px 14px;font-family:var(--font-game);font-size:0.7rem;color:#4ade80;margin-bottom:14px;}
    .flow-steps{display:flex;align-items:center;gap:8px;margin:14px 0;flex-wrap:wrap;}
    .flow-step{background:var(--bg-card3);border-radius:var(--r-md);padding:6px 12px;font-family:var(--font-ui);font-size:0.72rem;color:var(--text-second);white-space:nowrap;}
    .flow-arrow{color:var(--text-muted);font-size:0.85rem;}
  </style>
</head>
<body>
<div class="page-wrap">
  <header class="gh">
  <a href="/" class="gh-logo"><div class="logo-gem">🛡</div>SECURE<span>MIND</span></a>
  <nav class="gh-nav">
    <a href="/media-analysis" class="nb">🛡 Media</a>
    <a href="/query-proxy" class="nb active-nav">🔒 Proxy</a>
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

  <div class="content-wrap" style="max-width:800px;margin:0 auto;">
    <div class="module-hero">
      <div class="module-icon">🔒</div>
      <div class="module-title">QUERY PROXY — Private AI</div>
      <div class="module-desc">Query AI safely. The proxy automatically removes emails, phone numbers,
        card numbers, and other private information before sending, then returns a clean response.</div>
    </div>

    <!-- Flow visualization -->
    <div class="flow-steps">
      <div class="flow-step">✏️ Your query</div>
      <div class="flow-arrow">→</div>
      <div class="flow-step">🔍 PII Scanning</div>
      <div class="flow-arrow">→</div>
      <div class="flow-step">🧹 Sanitization</div>
      <div class="flow-arrow">→</div>
      <div class="flow-step">🤖 AI responds</div>
      <div class="flow-arrow">→</div>
      <div class="flow-step">✅ Safe response</div>
    </div>

    <div class="two-col">
      <div>
        <div class="field-label">Your query (may contain personal data)</div>
        <textarea id="queryInput" placeholder="Example: My email john@doe.com, phone +38 050 123 45 67. How to better protect my account?"></textarea>
      </div>
      <div>
        <div class="field-label">System context (optional)</div>
        <textarea id="contextInput" placeholder="Additional instructions for AI (response language, style, topic)..."
                  style="min-height:130px;"></textarea>
      </div>
    </div>

    <div class="btn-row">
      <button class="btn-primary" id="btnAsk" onclick="askProxy()">🔒 QUERY AI SAFELY</button>
      <button class="btn-secondary" id="btnPreview" onclick="previewSanitize()">🔍 Preview sanitization</button>
    </div>

    <div class="spinner" id="spinner"><span class="spin"></span>Processing query...</div>

    <!-- PII Preview -->
    <div class="pii-card" id="piiCard">
      <div class="pii-header">
        <div class="pii-title">🔍 Detected private data</div>
        <div class="pii-count-badge" id="piiCount">0 replacements</div>
      </div>
      <div class="pii-grid">
        <div class="pii-box">
          <div class="pii-box-label">Original query</div>
          <div class="pii-box-text" id="piiOriginal"></div>
        </div>
        <div class="pii-box">
          <div class="pii-box-label">Sanitized query (sent to AI)</div>
          <div class="pii-box-text" id="piiSanitized"></div>
        </div>
      </div>
      <div style="margin-top:10px;">
        <div class="pii-box-label">Detected PII tokens</div>
        <div id="piiTags"></div>
      </div>
    </div>

    <!-- AI Response -->
    <div class="result-card" id="resultCard">
      <div class="privacy-badge">🔒 Query processed privately — your data was not sent to AI</div>
      <div class="result-label">AI Response</div>
      <div class="result-text" id="aiResponse" style="white-space:pre-wrap;"></div>
    </div>
  </div>
</div>

<script>
  async function previewSanitize() {
    const query = document.getElementById('queryInput').value.trim();
    if (!query) { alert('Please enter a query'); return; }
    setLoading(true);
    try {
      const res = await fetch('/query-proxy/sanitize', {
        method: 'POST', headers: {'Content-Type':'application/json'},
        body: JSON.stringify({query})
      });
      const d = await res.json();
      if (d.error) { alert(d.error); return; }

      document.getElementById('piiCount').textContent = d.replacementsCount + ' replacements';
      document.getElementById('piiOriginal').textContent = d.original;
      document.getElementById('piiSanitized').textContent = d.sanitized;

      const tagsEl = document.getElementById('piiTags');
      tagsEl.innerHTML = '';
      if (d.detectedPII && d.detectedPII.length > 0) {
        d.detectedPII.forEach(pii => {
          const span = document.createElement('span');
          span.className = 'pii-tag';
          span.textContent = pii;
          tagsEl.appendChild(span);
        });
      } else {
        tagsEl.innerHTML = '<span style="color:var(--text-muted);font-size:0.78rem;">No private data detected ✅</span>';
      }
      document.getElementById('piiCard').classList.add('show');
      document.getElementById('resultCard').classList.remove('show');
    } catch(e) { alert('Error: '+e.message); }
    finally { setLoading(false); }
  }

  async function askProxy() {
    const query   = document.getElementById('queryInput').value.trim();
    const context = document.getElementById('contextInput').value.trim();
    if (!query) { alert('Please enter a query'); return; }
    setLoading(true);
    document.getElementById('resultCard').classList.remove('show');
    try {
      const res = await fetch('/query-proxy/ask', {
        method: 'POST', headers: {'Content-Type':'application/json'},
        body: JSON.stringify({query, systemContext: context})
      });
      const d = await res.json();
      if (d.error) { alert(d.error); return; }

      // Also show sanitize preview
      document.getElementById('piiCount').textContent = (d.replacementsCount || 0) + ' replacements';
      document.getElementById('piiOriginal').textContent = d.originalQuery || query;
      document.getElementById('piiSanitized').textContent = d.sanitizedQuery || query;

      const tagsEl = document.getElementById('piiTags');
      tagsEl.innerHTML = '';
      (d.detectedPII || []).forEach(pii => {
        const span = document.createElement('span');
        span.className = 'pii-tag';
        span.textContent = pii;
        tagsEl.appendChild(span);
      });
      if (!d.detectedPII || d.detectedPII.length === 0) {
        tagsEl.innerHTML = '<span style="color:var(--text-muted);font-size:0.78rem;">No private data detected ✅</span>';
      }
      document.getElementById('piiCard').classList.add('show');

      const raw = d.aiResponse || '—';
      document.getElementById('aiResponse').innerHTML = mdToHtml(raw);
      document.getElementById('resultCard').classList.add('show');
      document.getElementById('resultCard').scrollIntoView({behavior:'smooth'});
    } catch(e) { alert('Error: '+e.message); }
    finally { setLoading(false); }
  }

  function setLoading(on) {
    document.getElementById('spinner').classList.toggle('show', on);
    document.getElementById('btnAsk').disabled = on;
    document.getElementById('btnPreview').disabled = on;
  }

  // Markdown → HTML renderer for AI responses
  function mdToHtml(text) {
    return text
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/^### (.+)$/gm, '<h4 style="margin:12px 0 4px;color:var(--text-white);font-family:var(--font-game);font-size:.85rem;">$1</h4>')
      .replace(/^## (.+)$/gm,  '<h3 style="margin:14px 0 6px;color:var(--text-white);font-family:var(--font-game);font-size:.95rem;">$1</h3>')
      .replace(/^# (.+)$/gm,   '<h2 style="margin:16px 0 8px;color:var(--text-white);font-family:var(--font-game);">$1</h2>')
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g,     '<em>$1</em>')
      .replace(/^[-*] (.+)$/gm, '<li style="margin:3px 0;">$1</li>')
      .replace(/(<li[\s\S]+?<\/li>)/g, '<ul style="padding-left:18px;margin:6px 0;">$1</ul>')
      .replace(/^\d+\. (.+)$/gm, '<li style="margin:3px 0;">$1</li>')
      .replace(/`([^`]+)`/g, '<code style="background:rgba(255,255,255,.07);padding:1px 5px;border-radius:4px;font-size:.82em;">$1</code>')
      .split('\n\n').join('<br><br>')
      .split('\n').join('<br>');
  }
</script>
</body>
</html>