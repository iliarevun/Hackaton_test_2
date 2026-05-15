<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>CyberShield Hub — Your Digital Security Ecosystem</title>
  <link rel="stylesheet" href="/game.css">
  <style>
    .avatar-menu { position:relative; }
    .avatar-btn { width:36px;height:36px;border-radius:50%;border:2px solid var(--border-glow);cursor:pointer;background:var(--bg-card3);overflow:hidden;display:flex;align-items:center;justify-content:center;font-size:17px;transition:border-color .2s;flex-shrink:0; }
    .avatar-btn:hover { border-color:var(--blue); }
    .avatar-btn img { width:100%;height:100%;object-fit:cover;border-radius:50%; }
    .dropdown { position:absolute;top:calc(100% + 10px);right:0;background:var(--bg-card);border:1px solid var(--border-glow);border-radius:var(--r-lg);padding:6px;min-width:210px;box-shadow:0 12px 40px rgba(0,0,0,.5);opacity:0;pointer-events:none;transform:translateY(-8px) scale(.97);transition:all .18s cubic-bezier(.2,1,.3,1);z-index:500; }
    .dropdown.open { opacity:1;pointer-events:auto;transform:translateY(0) scale(1); }
    .dd-header { padding:10px 12px;border-bottom:1px solid var(--border);margin-bottom:4px; }
    .dd-name  { font-family:var(--font-ui);font-size:.85rem;font-weight:700;color:var(--text-white); }
    .dd-email { font-family:var(--font-body);font-size:.72rem;color:var(--text-muted);margin-top:1px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:180px; }
    .dd-item  { display:flex;align-items:center;gap:10px;padding:9px 12px;border-radius:var(--r-sm);font-family:var(--font-ui);font-size:.82rem;font-weight:600;color:var(--text-second);cursor:pointer;transition:all .14s;text-decoration:none;border:none;background:none;width:100%;text-align:left; }
    .dd-item:hover { background:rgba(59,130,246,.08);color:var(--text-white); }
    .dd-item .di-icon { font-size:16px;width:20px;text-align:center;flex-shrink:0; }
    .dd-divider { height:1px;background:var(--border);margin:4px 0; }
    .dd-item.danger:hover { background:rgba(239,68,68,.08);color:#fca5a5; }

    /* ── Ecosystem section ── */
    .eco-section { padding:32px 0 48px; }
    .eco-title { font-family:var(--font-game);font-size:0.7rem;letter-spacing:3px;text-transform:uppercase;color:var(--text-muted);margin-bottom:20px;text-align:center; }
    .eco-grid { display:grid;grid-template-columns:repeat(2,1fr);gap:14px; }
    @media(max-width:600px){ .eco-grid{grid-template-columns:1fr;} }
    .eco-card { background:var(--bg-card);border:1px solid var(--border);border-radius:var(--r-xl);padding:20px 22px;text-decoration:none;display:flex;align-items:flex-start;gap:16px;transition:all .2s;position:relative;overflow:hidden; }
    .eco-card::before { content:'';position:absolute;inset:0;opacity:0;transition:opacity .2s; }
    .eco-card:hover { transform:translateY(-2px);box-shadow:0 8px 32px rgba(0,0,0,.3); }
    .eco-card.media::before  { background:linear-gradient(135deg,rgba(239,68,68,.06),transparent); }
    .eco-card.proxy::before  { background:linear-gradient(135deg,rgba(59,130,246,.06),transparent); }
    .eco-card.stego::before  { background:linear-gradient(135deg,rgba(139,92,246,.06),transparent); }
    .eco-card.bio::before    { background:linear-gradient(135deg,rgba(16,185,129,.06),transparent); }
    .eco-card:hover::before  { opacity:1; }
    .eco-card.media { border-color:rgba(239,68,68,.18); }
    .eco-card.proxy { border-color:rgba(59,130,246,.18); }
    .eco-card.stego { border-color:rgba(139,92,246,.18); }
    .eco-card.bio   { border-color:rgba(16,185,129,.18); }
    .eco-card:hover.media { border-color:rgba(239,68,68,.4); }
    .eco-card:hover.proxy { border-color:rgba(59,130,246,.4); }
    .eco-card:hover.stego { border-color:rgba(139,92,246,.4); }
    .eco-card:hover.bio   { border-color:rgba(16,185,129,.4); }
    .eco-ico  { font-size:2rem;flex-shrink:0;margin-top:2px; }
    .eco-name { font-family:var(--font-game);font-size:0.85rem;font-weight:900;letter-spacing:1px;margin-bottom:5px; }
    .eco-card.media .eco-name { color:#f87171; }
    .eco-card.proxy .eco-name { color:#60a5fa; }
    .eco-card.stego .eco-name { color:#c4b5fd; }
    .eco-card.bio   .eco-name { color:#34d399; }
    .eco-desc { font-family:var(--font-ui);font-size:0.75rem;color:var(--text-second);line-height:1.5; }
    .eco-tag  { display:inline-flex;align-items:center;gap:4px;margin-top:8px;padding:3px 10px;border-radius:10px;font-family:var(--font-game);font-size:0.6rem;letter-spacing:1px;font-weight:700; }
    .eco-card.media .eco-tag { background:rgba(239,68,68,.1);color:#f87171; }
    .eco-card.proxy .eco-tag { background:rgba(59,130,246,.1);color:#60a5fa; }
    .eco-card.stego .eco-tag { background:rgba(139,92,246,.1);color:#c4b5fd; }
    .eco-card.bio   .eco-tag { background:rgba(16,185,129,.1);color:#34d399; }
  </style>
</head>
<body>
<header class="gh">
  <a href="/" class="gh-logo"><div class="logo-gem">🛡</div>CYBER<span>SHIELD</span></a>
  <nav class="gh-nav">
    <a href="/media-analysis" class="nb">🛡️ Media Shield</a>
    <a href="/query-proxy"    class="nb">🔒 Query Proxy</a>
    <a href="/steganography"  class="nb">🖼️ Secret Frame</a>
    <a href="/biometrics"     class="nb">🧬 Key Shield</a>
    <a href="/report"         class="nb">📊 Report</a>
    <a href="/leaderboard"    class="nb">🏆 Leaderboard</a>
  </nav>
  <div class="gh-right">
    <div class="xp-pill">⚡ <span id="hdrXp">${(progress.totalXp)!0}</span> XP</div>
    <form action="/logout" method="post" style="margin:0;">
      <input type="hidden" name="${(_csrf.parameterName)!''}" value="${(_csrf.token)!''}">
      <button type="submit" class="nb" style="cursor:pointer;background:none;border:none;color:var(--text-muted);">Sign out</button>
    </form>
  </div>
</header>

<div class="pw">
<div class="hero-wrap">
  <div class="rings">
    <div class="ring"></div><div class="ring"></div>
    <div class="ring"></div><div class="ring"></div>
  </div>
  <div class="hero-char">
    <div class="hero-avatar">
      <#if user?? && user.avatar??>
      <img src="/avatars/${user.avatar.id?string('0')}" alt="hero">
      <#else>🦸</#if>
    </div>
    <div class="hero-lvl">LVL<br><#if progress??>${progress.level}<#else>1</#if></div>
  </div>
  <h1 class="hero-title"><span class="shimmer-text">CyberShield Hub</span></h1>
  <p class="hero-quote">"Your data is yours. Protect it."</p>

  <!-- Security stats strip -->
  <div class="hero-modules">
    <div class="mod-card proxy" style="cursor:default;">
      <span class="mod-icon">🔐</span>
      <div class="mod-name">AES-256 Encryption</div>
      <div class="mod-desc">Military-grade steganography with time-limited keys</div>
      <span class="mod-tag">✨ Active</span>
    </div>
    <div class="mod-card media" style="cursor:default;">
      <span class="mod-icon">🤖</span>
      <div class="mod-name">AI-Powered Analysis</div>
      <div class="mod-desc">Gemini AI detects fake news, propaganda and AI bias</div>
      <span class="mod-tag">⚡ Live</span>
    </div>
  </div>
</div>

<!-- ── Privacy Ecosystem ─────────────────────────────────────── -->
<div class="c" style="max-width:820px;">
  <div class="eco-section">
    <div class="eco-title">🛡 CYBERSHIELD ECOSYSTEM</div>
    <div class="eco-grid">

      <a href="/media-analysis" class="eco-card media">
        <div class="eco-ico">🛡️</div>
        <div>
          <div class="eco-name">MEDIA SHIELD</div>
          <div class="eco-desc">Аналіз тексту на упередженість ШІ, фейки та пропаганду. Завантаж статтю — отримай детальний розбір.</div>
          <div class="eco-tag">🔍 AI Analysis</div>
        </div>
      </a>

      <a href="/query-proxy" class="eco-card proxy">
        <div class="eco-ico">🔒</div>
        <div>
          <div class="eco-name">QUERY PROXY</div>
          <div class="eco-desc">Запитуй ШІ анонімно. Автоматично видаляє email, телефони та картки з твоїх запитів.</div>
          <div class="eco-tag">🧹 PII Cleaner</div>
        </div>
      </a>

      <a href="/steganography" class="eco-card stego">
        <div class="eco-ico">🖼️</div>
        <div>
          <div class="eco-name">SECRET FRAME</div>
          <div class="eco-desc">Шифрування AES-256 + LSB в картинці Gemini AI. Ключ доступний 30 секунд — потім зникає назавжди.</div>
          <div class="eco-tag">⏱ 30s Key</div>
        </div>
      </a>

      <a href="/biometrics" class="eco-card bio">
        <div class="eco-ico">🧬</div>
        <div>
          <div class="eco-name">KEY SHIELD</div>
          <div class="eco-desc">Твій ритм набору тексту унікальний як відбиток пальця. Поведінкова біометрія без паролів.</div>
          <div class="eco-tag">🔑 Behavioral ID</div>
        </div>
      </a>

    </div>
  </div>
</div>

</div><!-- pw -->

<div class="toast-wrap" id="toastWrap"></div>
<input type="hidden" name="${csrfParameterName!''}" value="${csrfToken!''}">

<script>
  function toggleDropdown() {
    document.getElementById('dropdown').classList.toggle('open');
  }
  document.addEventListener('click', function(e) {
    var menu = document.getElementById('avatarMenu');
    if (menu && !menu.contains(e.target)) {
      var dd = document.getElementById('dropdown');
      if (dd) dd.classList.remove('open');
    }
  });
  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
      var dd = document.getElementById('dropdown');
      if (dd) dd.classList.remove('open');
    }
  });
</script>
</body>
</html>
