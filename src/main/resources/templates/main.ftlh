<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>HEALTH HUB — Level up your body and mind</title>
  <link rel="stylesheet" href="/game.css">
  <style>
    /* ── Avatar dropdown ── */
    .avatar-menu { position:relative; }
    .avatar-btn {
      width:36px; height:36px; border-radius:50%;
      border:2px solid var(--border-glow); cursor:pointer;
      background:var(--bg-card3); overflow:hidden;
      display:flex; align-items:center; justify-content:center;
      font-size:17px; transition:border-color .2s;
      flex-shrink:0;
    }
    .avatar-btn:hover { border-color:var(--blue); }
    .avatar-btn img { width:100%; height:100%; object-fit:cover; border-radius:50%; }

    .dropdown {
      position:absolute; top:calc(100% + 10px); right:0;
      background:var(--bg-card); border:1px solid var(--border-glow);
      border-radius:var(--r-lg); padding:6px; min-width:210px;
      box-shadow:0 12px 40px rgba(0,0,0,.5);
      opacity:0; pointer-events:none;
      transform:translateY(-8px) scale(.97);
      transition:all .18s cubic-bezier(.2,1,.3,1);
      z-index:500;
    }
    .dropdown.open {
      opacity:1; pointer-events:auto;
      transform:translateY(0) scale(1);
    }

    /* user info at top of dropdown */
    .dd-header {
      padding:10px 12px 10px;
      border-bottom:1px solid var(--border);
      margin-bottom:4px;
    }
    .dd-name { font-family:var(--font-ui); font-size:.85rem; font-weight:700; color:var(--text-white); }
    .dd-email { font-family:var(--font-body); font-size:.72rem; color:var(--text-muted); margin-top:1px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:180px; }
    .dd-xp { font-family:var(--font-game); font-size:.68rem; color:var(--gold-bright); margin-top:4px; }

    /* dropdown items */
    .dd-item {
      display:flex; align-items:center; gap:10px;
      padding:9px 12px; border-radius:var(--r-sm);
      font-family:var(--font-ui); font-size:.82rem; font-weight:600;
      color:var(--text-second); cursor:pointer;
      transition:all .14s; text-decoration:none;
      border:none; background:none; width:100%; text-align:left;
    }
    .dd-item:hover { background:rgba(59,130,246,.08); color:var(--text-white); }
    .dd-item .di-icon { font-size:16px; width:20px; text-align:center; flex-shrink:0; }
    .dd-divider { height:1px; background:var(--border); margin:4px 0; }
    .dd-item.danger:hover { background:rgba(239,68,68,.08); color:#fca5a5; }
  </style>
</head>
<body>
<header class="gh">
  <a href="/" class="gh-logo"><div class="logo-gem">⚡</div>HEALTH<span>HUB</span></a>
  <nav class="gh-nav">
    <a href="/physical"    class="nb phy">💪 Physical</a>
    <a href="/mental"      class="nb men">🧠 Mental</a>
    <a href="/games"       class="nb">🎮 Games</a>
    <a href="/challenges"  class="nb">⚔️ Challenges</a>
    <a href="/report"      class="nb">📊 Report</a>
    <a href="/leaderboard" class="nb">🏆 Leaderboard</a>
    <a href="/profile"     class="nb">👤 Profile</a>
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

  <h1 class="hero-title"><span class="shimmer-text">HealthHub</span></h1>
  <p class="hero-quote">"Your body is the first character worth leveling up"</p>

  <div class="hero-modules">
    <a href="/physical" class="mod-card phy">
      <span class="mod-icon">💪</span>
      <div class="mod-name">Physical health</div>
      <div class="mod-desc">AI workouts, character levels, locations and daily quests</div>
      <span class="mod-tag">⚡ Active</span>
    </a>
    <a href="/mental" class="mod-card men">
      <span class="mod-icon">🧠</span>
      <div class="mod-name">Mental health</div>
      <div class="mod-desc">Mood analysis, psych tests and personalized advice</div>
      <span class="mod-tag">✨ Available</span>
    </a>
  </div>
</div>
</div>

<div class="toast-wrap" id="toastWrap"></div>
<input type="hidden" name="${csrfParameterName!''}" value="${csrfToken!''}">

<script>
  function toggleDropdown() {
    var dd = document.getElementById('dropdown');
    dd.classList.toggle('open');
  }

  // Close on click outside
  document.addEventListener('click', function(e) {
    var menu = document.getElementById('avatarMenu');
    if (menu && !menu.contains(e.target)) {
      var dd = document.getElementById('dropdown');
      if (dd) dd.classList.remove('open');
    }
  });

  // Close on Escape
  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
      var dd = document.getElementById('dropdown');
      if (dd) dd.classList.remove('open');
    }
  });
</script>
</body>
        </html>
