<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>HEALTH HUB — Leaderboard</title>
  <link rel="stylesheet" href="/game.css">
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
  <div class="c" style="padding-top:28px;padding-bottom:60px;max-width:720px;">

    <div style="text-align:center;margin-bottom:28px;">
      <h1 style="font-family:var(--font-game);font-size:1.5rem;letter-spacing:3px;text-transform:uppercase;color:var(--gold);text-shadow:0 0 20px var(--glow-gold);">🏆 LEADERBOARD</h1>
      <div style="color:var(--text-muted);font-size:0.78rem;margin-top:4px;">Overall leaderboard по XP</div>
    </div>

    <!-- TOP 3 podium -->
    <#if entries?? && (entries?size >= 3)>
    <div style="display:flex;justify-content:center;align-items:flex-end;gap:14px;margin-bottom:28px;flex-wrap:wrap;">

      <!-- 2nd place -->
      <div style="text-align:center;flex:1;min-width:90px;max-width:140px;">
        <div style="width:64px;height:64px;border-radius:50%;border:3px solid #94a3b8;background:var(--bg-card2);display:flex;align-items:center;justify-content:center;font-size:26px;margin:0 auto 8px;box-shadow:0 0 16px rgba(148,163,184,.3);overflow:hidden;">
          <#if entries[1].avatarId??>
          <img src="/avatars/${entries[1].avatarId?string('0')}" style="width:100%;height:100%;object-fit:cover;">
            <#else>🥈</#if>
        </div>
        <div style="font-family:var(--font-ui);font-size:.75rem;font-weight:700;color:#94a3b8;margin-bottom:3px;">${entries[1].name!'Hero'}</div>
        <div style="font-family:var(--font-game);font-size:.78rem;color:#94a3b8;">${entries[1].totalXp} XP</div>
        <div style="height:60px;background:rgba(148,163,184,.15);border:1px solid rgba(148,163,184,.25);border-radius:8px 8px 0 0;margin-top:8px;display:flex;align-items:center;justify-content:center;font-family:var(--font-game);font-size:1.1rem;font-weight:900;color:#94a3b8;">2</div>
      </div>

      <!-- 1st place -->
      <div style="text-align:center;flex:1;min-width:100px;max-width:160px;">
        <div style="width:80px;height:80px;border-radius:50%;border:3px solid var(--gold);background:var(--bg-card2);display:flex;align-items:center;justify-content:center;font-size:34px;margin:0 auto 8px;box-shadow:0 0 24px var(--glow-gold);overflow:hidden;">
          <#if entries[0].avatarId??>
          <img src="/avatars/${entries[0].avatarId?string('0')}" style="width:100%;height:100%;object-fit:cover;">
            <#else>🥇</#if>
        </div>
        <div style="font-family:var(--font-game);font-size:.65rem;letter-spacing:2px;color:var(--gold-bright);margin-bottom:2px;">👑 LEADER</div>
        <div style="font-family:var(--font-ui);font-size:.82rem;font-weight:700;color:var(--gold-bright);margin-bottom:3px;">${entries[0].name!'Hero'}</div>
        <div style="font-family:var(--font-game);font-size:.85rem;color:var(--gold-bright);">${entries[0].totalXp} XP</div>
        <div style="height:90px;background:rgba(245,158,11,.12);border:1px solid rgba(245,158,11,.3);border-radius:8px 8px 0 0;margin-top:8px;display:flex;align-items:center;justify-content:center;font-family:var(--font-game);font-size:1.4rem;font-weight:900;color:var(--gold);">1</div>
      </div>

      <!-- 3rd place -->
      <div style="text-align:center;flex:1;min-width:90px;max-width:140px;">
        <div style="width:64px;height:64px;border-radius:50%;border:3px solid #c08040;background:var(--bg-card2);display:flex;align-items:center;justify-content:center;font-size:26px;margin:0 auto 8px;box-shadow:0 0 14px rgba(180,120,60,.3);overflow:hidden;">
          <#if entries[2].avatarId??>
          <img src="/avatars/${entries[2].avatarId?string('0')}" style="width:100%;height:100%;object-fit:cover;">
            <#else>🥉</#if>
        </div>
        <div style="font-family:var(--font-ui);font-size:.75rem;font-weight:700;color:#c08040;margin-bottom:3px;">${entries[2].name!'Hero'}</div>
        <div style="font-family:var(--font-game);font-size:.78rem;color:#c08040;">${entries[2].totalXp} XP</div>
        <div style="height:44px;background:rgba(180,120,60,.12);border:1px solid rgba(180,120,60,.25);border-radius:8px 8px 0 0;margin-top:8px;display:flex;align-items:center;justify-content:center;font-family:var(--font-game);font-size:1.1rem;font-weight:900;color:#c08040;">3</div>
      </div>

    </div>
  </#if>

          <!-- Full list -->
  <div class="card">
    <div class="sec-title gold mb16">Всі participants</div>

    <#if entries?? && entries?has_content>
    <#list entries as e>
    <div class="lb-row <#if e?index==0>r1<#elseif e?index==1>r2<#elseif e?index==2>r3</#if>">

      <!-- Rank -->
      <div class="lb-rank">
        <#if e?index==0>🥇<#elseif e?index==1>🥈<#elseif e?index==2>🥉<#else>${e?index+1}</#if>
    </div>

    <!-- Avatar -->
    <div style="width:40px;height:40px;border-radius:50%;background:var(--bg-card3);border:2px solid var(--border);overflow:hidden;display:flex;align-items:center;justify-content:center;font-size:18px;flex-shrink:0;">
      <#if e.avatarId??>
      <img src="/avatars/${e.avatarId?string('0')}" style="width:100%;height:100%;object-fit:cover;">
        <#else>🦸</#if>
    </div>

    <!-- Info -->
    <div style="flex:1;min-width:0;">
      <div style="font-family:var(--font-ui);font-weight:700;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${e.name!'Hero'}</div>
      <div style="font-size:0.72rem;color:var(--text-muted);">
        Level ${e.level}
        <#if (e.streakDays > 0)> • 🔥 ${e.streakDays} days</#if>
      <#if (e.workoutsDone > 0)> • 💪 ${e.workoutsDone} workouts</#if>
  </div>
</div>

<!-- XP -->
<div style="text-align:right;flex-shrink:0;">
  <div style="font-family:var(--font-game);font-size:0.92rem;color:var(--gold-bright);">${e.totalXp} XP</div>
  <div style="font-size:0.68rem;color:var(--text-muted);">LVL ${e.level}</div>
</div>

</div>
</#list>
<#else>
<div style="text-align:center;padding:48px 20px;">
<div style="font-size:56px;margin-bottom:14px;">🏆</div>
<div style="font-family:var(--font-game);font-size:.85rem;letter-spacing:2px;color:var(--text-white);margin-bottom:8px;">NOTHING ЩО EMPTY</div>
<div style="color:var(--text-muted);font-size:.8rem;margin-bottom:20px;">Be first hero у leaderboardу!</div>
<a href="/physical" class="btn btn-green btn-sm">💪 Start workout</a>
</div>
        </#if>
        </div>

        </div>
        </div>
        </body>
        </html>
