<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>CyberShield Hub — Registration</title>
  <link rel="stylesheet" href="/game.css">
  <style>
    /* Bio enroll */
    .bio-bar{background:rgba(139,92,246,.07);border:1px solid rgba(139,92,246,.22);border-radius:var(--r-md);padding:10px 14px;margin-bottom:6px;display:flex;align-items:center;gap:10px;}
    .bio-dot{width:8px;height:8px;border-radius:50%;background:#8b5cf6;animation:pulse 1.2s ease-in-out infinite;flex-shrink:0;}
    @keyframes pulse{0%,100%{opacity:1;transform:scale(1)}50%{opacity:.5;transform:scale(.8)}}
    .bio-txt{font-family:var(--font-ui);font-size:.72rem;color:#c4b5fd;}
    .bio-count{font-family:var(--font-game);font-size:.75rem;color:#a78bfa;margin-left:auto;}
    .bio-progress{height:3px;background:var(--border);border-radius:2px;margin-bottom:6px;overflow:hidden;}
    .bio-progress-fill{height:100%;background:linear-gradient(90deg,#8b5cf6,#06b6d4);border-radius:2px;transition:width .3s ease;}
    .enroll-ok{background:rgba(34,197,94,.08);border:1px solid rgba(34,197,94,.25);border-radius:var(--r-md);padding:10px 14px;font-family:var(--font-ui);font-size:.78rem;color:#4ade80;margin-bottom:10px;display:none;}
    .enroll-ok.show{display:block;}

    /* 5-attempt bio section */
    .bio-enroll-section{background:rgba(139,92,246,.06);border:1px solid rgba(139,92,246,.2);border-radius:var(--r-md);padding:14px;margin-bottom:14px;display:none;}
    .bio-enroll-section.show{display:block;}
    .bio-enroll-title{font-family:var(--font-game);font-size:.7rem;letter-spacing:2px;text-transform:uppercase;color:#a78bfa;margin-bottom:6px;}
    .bio-phrase-prompt{font-family:var(--font-ui);font-size:.75rem;color:#c4b5fd;margin-bottom:8px;padding:8px 12px;background:rgba(139,92,246,.1);border-radius:6px;border-left:2px solid #8b5cf6;}
    .attempt-row{display:flex;align-items:center;gap:8px;margin-bottom:6px;}
    .attempt-label{font-family:var(--font-game);font-size:.65rem;color:var(--text-muted);min-width:60px;}
    .attempt-input{flex:1;background:var(--bg-card2);border:1px solid var(--border);border-radius:6px;padding:7px 10px;font-family:var(--font-ui);font-size:.82rem;color:var(--text-primary);outline:none;}
    .attempt-input:focus{border-color:rgba(139,92,246,.5);}
    .attempt-status{font-size:1rem;min-width:20px;text-align:center;}
    .attempts-progress{display:flex;gap:4px;margin:10px 0;}
    .attempt-dot{width:28px;height:4px;border-radius:2px;background:var(--border);transition:background .3s;}
    .attempt-dot.done{background:#8b5cf6;}
    .attempt-dot.current{background:#06b6d4;animation:blink .8s infinite;}
    @keyframes blink{0%,100%{opacity:1}50%{opacity:.4}}
    .bio-result-box{margin-top:10px;padding:10px 14px;border-radius:8px;font-family:var(--font-ui);font-size:.78rem;display:none;line-height:1.5;}
    .bio-result-box.show{display:block;}
    .bio-result-ok{background:rgba(34,197,94,.08);border:1px solid rgba(34,197,94,.25);color:#4ade80;}
    .bio-result-warn{background:rgba(245,158,11,.08);border:1px solid rgba(245,158,11,.3);color:#fbbf24;}

    /* Checkbox toggle */
    .bio-toggle-row{display:flex;align-items:center;gap:10px;padding:10px 14px;background:rgba(139,92,246,.06);border:1px solid rgba(139,92,246,.2);border-radius:var(--r-md);margin-bottom:14px;cursor:pointer;}
    .bio-toggle-row input[type=checkbox]{width:16px;height:16px;cursor:pointer;accent-color:#8b5cf6;}
    .bio-toggle-label{font-family:var(--font-ui);font-size:.8rem;color:#c4b5fd;cursor:pointer;user-select:none;}
    .bio-toggle-sub{font-family:var(--font-ui);font-size:.68rem;color:var(--text-muted);}

    /* Seed phrase modal */
    .seed-overlay{position:fixed;inset:0;background:rgba(0,0,0,.85);backdrop-filter:blur(8px);z-index:9999;display:flex;align-items:center;justify-content:center;padding:20px;}
    .seed-box{background:var(--bg-card);border:1px solid rgba(245,158,11,.35);border-radius:var(--r-2xl);padding:32px 28px;max-width:520px;width:100%;box-shadow:0 24px 80px rgba(0,0,0,.7);}
    .seed-title{font-family:var(--font-game);font-size:1rem;font-weight:900;letter-spacing:2px;color:#fbbf24;margin-bottom:6px;}
    .seed-subtitle{font-family:var(--font-ui);font-size:.78rem;color:var(--text-second);margin-bottom:20px;line-height:1.5;}
    .seed-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:8px;margin-bottom:20px;}
    .seed-item{background:var(--bg-card2);border:1px solid rgba(245,158,11,.2);border-radius:var(--r-md);padding:10px 14px;display:flex;align-items:center;gap:10px;}
    .seed-num{font-family:var(--font-game);font-size:.65rem;color:var(--text-muted);width:20px;flex-shrink:0;}
    .seed-word{font-family:var(--font-mono);font-size:.88rem;color:var(--text-white);font-weight:700;}
    .seed-warn{background:rgba(239,68,68,.08);border:1px solid rgba(239,68,68,.25);border-radius:var(--r-md);padding:10px 14px;font-family:var(--font-ui);font-size:.75rem;color:#f87171;margin-bottom:16px;line-height:1.5;}
    .seed-confirm-wrap{display:flex;align-items:center;gap:10px;margin-bottom:16px;}
    .seed-confirm-wrap input[type=checkbox]{width:18px;height:18px;cursor:pointer;accent-color:#f59e0b;}
    .seed-confirm-wrap label{font-family:var(--font-ui);font-size:.8rem;color:var(--text-second);cursor:pointer;}
    .btn-seed-continue{width:100%;padding:13px;background:linear-gradient(135deg,#b45309,#d97706);border:none;border-radius:var(--r-md);font-family:var(--font-game);font-size:.82rem;font-weight:700;letter-spacing:2px;color:#fff;cursor:pointer;transition:opacity .2s;}
    .btn-seed-continue:disabled{opacity:.35;cursor:not-allowed;}
    .btn-copy-seeds{display:inline-flex;align-items:center;gap:6px;padding:7px 16px;background:rgba(245,158,11,.1);border:1px solid rgba(245,158,11,.3);border-radius:8px;font-family:var(--font-ui);font-size:.75rem;color:#fbbf24;cursor:pointer;transition:all .18s;margin-bottom:14px;}
    .btn-copy-seeds:hover{background:rgba(245,158,11,.18);}
  </style>
</head>
<body>

<!-- ── Seed Phrase Modal ─────────────────────────────────────────── -->
<div class="seed-overlay" id="seedOverlay" style="display:none;">
  <div class="seed-box">
    <div class="seed-title">🔑 RECOVERY SEED PHRASE</div>
    <div class="seed-subtitle">
      Запиши ці 10 слів на <strong style="color:#fbbf24;">папір або в надійне місце</strong>.
      Якщо система заблокує тебе через біометрію — ці слова дозволять оскаржити рішення та відновити доступ.
    </div>
    <button class="btn-copy-seeds" onclick="copySeeds()">📋 Копіювати всі слова</button>
    <div class="seed-grid" id="seedGrid"></div>
    <div class="seed-warn">
      ⚠️ Це єдиний раз коли вони показуються. Після закриття — слова більше не будуть доступні.
    </div>
    <div class="seed-confirm-wrap">
      <input type="checkbox" id="seedConfirm" onchange="document.getElementById('btnSeedContinue').disabled=!this.checked">
      <label for="seedConfirm">Я записав(ла) всі 10 слів і зберіг(ла) їх у безпечному місці</label>
    </div>
    <button class="btn-seed-continue" id="btnSeedContinue" disabled onclick="closeSeedAndSubmit()">
      ✅ Підтверджую — продовжити реєстрацію
    </button>
  </div>
</div>

<div class="auth-page">
  <div class="auth-box" style="max-width:460px;">
    <div class="auth-head">
      <span class="auth-icon">🛡</span>
      <div class="auth-t">CyberShield Hub</div>
      <div class="auth-s">Create your secure account</div>
    </div>

    <#if errorMessage??><div class="alert-err">❌ ${errorMessage}</div></#if>

          <!-- Biometric indicator (passive capture from form fields) -->
  <div class="bio-bar">
    <div class="bio-dot"></div>
    <div><div class="bio-txt">🧬 Keystroke biometrics — фоновий запис</div></div>
    <div class="bio-count" id="bioCount">0/10</div>
  </div>
  <div class="bio-progress"><div class="bio-progress-fill" id="bioFill" style="width:0%"></div></div>

  <!-- ✅ Checkbox: додати біометрію до пароля -->
  <div class="bio-toggle-row" onclick="toggleBioEnroll()">
    <input type="checkbox" id="bioEnrollCheck" onchange="toggleBioEnroll()">
    <div>
      <div class="bio-toggle-label">🧬 Додати біометрію до пароля</div>
      <div class="bio-toggle-sub">Надрукуй одну фразу 5 разів — система запам'ятає твій ритм</div>
    </div>
  </div>

  <!-- 5-attempt biometric enrollment -->
  <div class="bio-enroll-section" id="bioEnrollSection">
    <div class="bio-enroll-title">📝 Запис біометричного профілю</div>
    <div class="bio-phrase-prompt" id="enrollPhrase">→ Друкуй: <strong id="phraseText">завантаження...</strong></div>

    <div class="attempts-progress" id="attemptsDots"></div>

    <div id="attemptFields"></div>

    <div class="bio-result-box" id="bioEnrollResult"></div>
  </div>

  <div class="enroll-ok" id="enrollOk"></div>

  <form method="post" action="/registration" id="regForm">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <input type="hidden" name="keystrokeData" id="keystrokeData"/>
    <input type="hidden" name="seedPhrase" id="seedPhraseField"/>
    <input type="hidden" name="biometricProfile" id="biometricProfileField"/>
    <div class="fg">
      <label class="fl">Name</label>
      <input class="fi" type="text" name="name" id="nameField" placeholder="Your name" required>
    </div>
    <div class="fg">
      <label class="fl">Email</label>
      <input class="fi" type="email" name="email" id="emailField" placeholder="you@example.com" required>
    </div>
    <div class="fg">
      <label class="fl">Password (min. 8 chars)</label>
      <input class="fi" type="password" name="password" id="pwdField" placeholder="••••••••" required minlength="8">
    </div>
    <button class="btn btn-green w100 jc mt8" type="button" onclick="showSeedModal()">🚀 Create account</button>


    <div class="fg" style="margin-top: 14px; display: flex; align-items: center; gap: 10px;">
      <input type="checkbox" name="useBiometricsWithPassword" id="useBioCheck" onchange="toggleBioRegistration()">
      <label for="useBioCheck" style="font-family: var(--font-ui); font-size: 0.85rem; color: #34d399; cursor: pointer;">
        🧬 Посилити пароль біометрією клавіатурного почерку
      </label>
    </div>

    <div id="bioRegSection" style="display: none; margin-top: 14px; padding: 16px; background: var(--bg-card3); border-radius: var(--r-md); border: 1px dashed var(--border);">
      <div class="bio-header">
        <div class="bio-title" style="color: #6ee7b7;">🎯 Етап тренування системи (Спроба: <span id="attemptCount">1</span> / 5)</div>
      </div>
      <div class="bio-prompt" style="margin-top: 6px;">Надрукуй фразу: <em style="color: #93c5fd;">"Захист починається з тебе"</em></div>

      <textarea class="bio-typing-area" id="regBioArea" placeholder="Почни друкувати фразу тут..." style="width:100%; height:60px; margin-top:8px;"></textarea>

      <input type="hidden" name="rawBiometricAttempts" id="rawBiometricAttempts">
    </div>

    <script>
      function toggleBioRegistration() {
        const isChecked = document.getElementById('useBioCheck').checked;
        document.getElementById('bioRegSection').style.display = isChecked ? 'block' : 'none';
      }

      const allAttemptsData = [];
      let currentAttemptEvents = [];
      let regPressMap = {};
      let totalAttempts = 0;

      const regArea = document.getElementById('regBioArea');
      if(regArea) {
        regArea.addEventListener('keydown', e => {
          if (!regPressMap[e.code]) regPressMap[e.code] = Date.now();
        });

        regArea.addEventListener('keyup', e => {
          const press = regPressMap[e.code];
          if (press) {
            currentAttemptEvents.push({
              key: e.key,
              code: e.code,
              pressTime: press,
              releaseTime: Date.now()
            });
            delete regPressMap[e.code];
          }

          // Перевіряємо, чи фраза дописана (наприклад, за довжиною або якщо натиснуто Enter/крапку)
          // Оптимально для тесту: зафіксуємо довжину символів (наприклад, 25 символів) або закінчення вводу
          if (regArea.value.trim() === "Захист починається з тебе" || e.key === "Enter") {
            totalAttempts++;
            allAttemptsData.push(currentAttemptEvents); // Зберігаємо спробу

            // Скидаємо буфери спроби
            currentAttemptEvents = [];
            regPressMap = {};
            regArea.value = "";

            if (totalAttempts < 5) {
              document.getElementById('attemptCount').textContent = (totalAttempts + 1);
              alert('Спроба ' + totalAttempts + ' збережена! Продовжте введення.');
            } else {
              document.getElementById('attemptCount').textContent = "Завершено! 🎉";
              regArea.disabled = true;
              regArea.placeholder = "Дякуємо! Профіль сформовано.";
              // Записуємо фінальний масив спроб у приховану форму
              document.getElementById('rawBiometricAttempts').value = JSON.stringify(allAttemptsData);
              alert('Чудово! Усі 5 спроб записано. Тепер можете завершити реєстрацію.');
            }
          }
        });
      }
    </script>
  </form>

  <div class="auth-div mt16">Already have an account?</div>
  <div style="text-align:center;"><a href="/login" class="auth-link">← Sign in</a></div>
</div>
</div>

<script>
  // ── Wordlist ──────────────────────────────────────────────────────────────────
  const WORDS = ['abandon','ability','able','about','above','absent','absorb','abstract','absurd',
  'abuse','access','accident','account','accuse','achieve','acid','acoustic','acquire','across',
  'act','action','actor','actress','actual','adapt','add','addict','address','adjust','admit',
  'adult','advance','advice','aerobic','afford','afraid','again','agent','agree','ahead','aim',
  'alert','alien','alley','allow','almost','alone','alpha','already','also','alter','always',
  'amateur','amazing','among','amount','amused','analyst','anchor','ancient','anger','angle',
  'angry','animal','announce','annual','another','answer','antenna','antique','anxiety','apart',
  'approve','arctic','area','arena','argue','arm','armed','armor','army','around','arrange',
  'arrest','arrive','arrow','art','artist','artwork','aspect','assault','asset','assist',
  'assume','athlete','atom','attack','attend','attract','auction','audit','august','author',
  'balance','bamboo','banana','banner','barely','bargain','barrel','base','basic','basket',
  'battle','beach','beauty','become','begin','behave','behind','believe','below','bench',
  'benefit','best','betray','better','beyond','bicycle','biology','birth','black','blade',
  'blame','blast','bless','blind','blood','blossom','blue','board','boat','body','boil',
  'bomb','bone','bonus','book','brain','brand','brave','bridge','brief','bright','bring'];

  const ENROLL_PHRASES = [
    'Кібербезпека починається з тебе',
    'Мій цифровий підпис унікальний',
    'Безпечний доступ до системи',
    'Захист даних понад усе',
  ];

  const MAX_ATTEMPTS = 5;
  let currentPhrase = '';
  let attemptEvents = []; // events per attempt
  let completedAttempts = 0;
  let bioEnrollEnabled = false;
  let averagedProfile = null;

  function toggleBioEnroll() {
    const check = document.getElementById('bioEnrollCheck');
    bioEnrollEnabled = check.checked;
    const section = document.getElementById('bioEnrollSection');
    if (bioEnrollEnabled) {
      section.classList.add('show');
      initEnrollAttempts();
    } else {
      section.classList.remove('show');
    }
  }

  function initEnrollAttempts() {
    currentPhrase = ENROLL_PHRASES[Math.floor(Math.random() * ENROLL_PHRASES.length)];
    document.getElementById('phraseText').textContent = currentPhrase;
    completedAttempts = 0;
    attemptEvents = [];
    averagedProfile = null;

    // Build dots
    const dots = document.getElementById('attemptsDots');
    dots.innerHTML = '';
    for (let i = 0; i < MAX_ATTEMPTS; i++) {
      const d = document.createElement('div');
      d.className = 'attempt-dot' + (i === 0 ? ' current' : '');
      d.id = 'dot' + i;
      dots.appendChild(d);
    }

    // Build attempt fields
    const fields = document.getElementById('attemptFields');
    fields.innerHTML = '';
    for (let i = 0; i < MAX_ATTEMPTS; i++) {
      const row = document.createElement('div');
      row.className = 'attempt-row';
      row.id = 'row' + i;
      row.style.display = i === 0 ? 'flex' : 'none';
      row.innerHTML = `
        <span class="attempt-label">Спроба ${i+1}</span>
        <input class="attempt-input" id="inp${i}" type="text"
               placeholder="${currentPhrase}" autocomplete="off" spellcheck="false"/>
        <span class="attempt-status" id="st${i}"></span>`;
      fields.appendChild(row);
    }

    // Attach listeners to first input
    attachAttemptListener(0);
  }

  function attachAttemptListener(idx) {
    const input = document.getElementById('inp' + idx);
    if (!input) return;
    input.focus();

    const evts = [];
    const pm = {};
    input.addEventListener('keydown', e => { if (!pm[e.code]) pm[e.code] = Date.now(); });
    input.addEventListener('keyup', e => {
      const p = pm[e.code];
      if (p) { evts.push({key:e.key, pressTime:p, releaseTime:Date.now(), positionInWord:0}); delete pm[e.code]; }
    });

    input.addEventListener('input', () => {
      // Check if typed text matches the phrase (trimmed)
      if (input.value.trim() === currentPhrase && evts.length >= 8) {
        completeAttempt(idx, evts);
      }
    });

    // Also allow pressing Enter when done
    input.addEventListener('keydown', e => {
      if (e.key === 'Enter' && input.value.trim().length > 5) {
        completeAttempt(idx, evts.slice());
      }
    });
  }

  async function completeAttempt(idx, evts) {
    if (evts.length < 5) {
      document.getElementById('st' + idx).textContent = '❌';
      return;
    }

    document.getElementById('st' + idx).textContent = '✅';
    document.getElementById('dot' + idx).className = 'attempt-dot done';
    document.getElementById('inp' + idx).disabled = true;
    attemptEvents.push(evts);
    completedAttempts++;

    if (completedAttempts < MAX_ATTEMPTS) {
      // Show next row
      const next = document.getElementById('row' + completedAttempts);
      if (next) {
        next.style.display = 'flex';
        if (completedAttempts < MAX_ATTEMPTS)
          document.getElementById('dot' + completedAttempts).className = 'attempt-dot current';
        attachAttemptListener(completedAttempts);
      }
    } else {
      // All 5 done — compute averaged profile
      await computeAveragedProfile();
    }
  }

  async function computeAveragedProfile() {
    const resultBox = document.getElementById('bioEnrollResult');
    resultBox.className = 'bio-result-box show bio-result-warn';
    resultBox.textContent = '⏳ Обчислення середнього профілю...';

    try {
      // Collect all events from all attempts
      const allEvents = attemptEvents.flat();

      const res = await fetch('/biometrics/analyze', {
        method:'POST', headers:{'Content-Type':'application/json'},
        body: JSON.stringify({events: allEvents})
      });
      const d = await res.json();

      if (d.error) throw new Error(d.error);

      averagedProfile = d;
      document.getElementById('biometricProfileField').value = JSON.stringify(d);

      resultBox.className = 'bio-result-box show bio-result-ok';
      resultBox.innerHTML =
        `✅ Профіль зафіксовано з ${MAX_ATTEMPTS} спроб<br>` +
        `🏎 Швидкість: ${d.typingSpeedCpm} CPM | ` +
        `⏱ Середній dwell: ${d.avgDwellMs}мс | ` +
        `🔑 Унікальність: ${d.uniquenessScore}%<br>` +
        `<span style="font-family:monospace;font-size:.7rem;color:#34d399;">hash: ${d.fingerprintHash}</span>`;

      // Also enroll it
      await fetch('/biometrics/enroll', {
        method:'POST', headers:{'Content-Type':'application/json'},
        body: JSON.stringify({events: allEvents})
      });

    } catch(e) {
      resultBox.className = 'bio-result-box show bio-result-warn';
      resultBox.textContent = '⚠️ Помилка обчислення: ' + e.message;
    }
  }

  // ── Seed phrase ────────────────────────────────────────────────────────────────
  function generateSeedPhrase() {
    const words = [];
    const arr = new Uint32Array(10);
    crypto.getRandomValues(arr);
    for (let i = 0; i < 10; i++) words.push(WORDS[arr[i] % WORDS.length]);
    return words;
  }

  let currentSeed = [];

  function showSeedModal() {
    const name = document.getElementById('nameField').value.trim();
    const email = document.getElementById('emailField').value.trim();
    const pwd = document.getElementById('pwdField').value;
    if (!name || !email || !pwd || pwd.length < 8) {
      alert('Заповніть всі поля (пароль мінімум 8 символів)');
      return;
    }
    if (bioEnrollEnabled && completedAttempts < MAX_ATTEMPTS) {
      alert('Будь ласка, завершіть всі 5 спроб біометричного запису');
      return;
    }

    currentSeed = generateSeedPhrase();
    const grid = document.getElementById('seedGrid');
    grid.innerHTML = '';
    currentSeed.forEach((word, i) => {
      const div = document.createElement('div');
      div.className = 'seed-item';
      div.innerHTML = `<span class="seed-num">${i+1}.</span><span class="seed-word">${word}</span>`;
      grid.appendChild(div);
    });

    document.getElementById('seedConfirm').checked = false;
    document.getElementById('btnSeedContinue').disabled = true;
    document.getElementById('seedPhraseField').value = currentSeed.join(' ');
    document.getElementById('seedOverlay').style.display = 'flex';
  }

  function copySeeds() {
    navigator.clipboard.writeText(currentSeed.join(' ')).then(() => {
      const btn = document.querySelector('.btn-copy-seeds');
      btn.textContent = '✅ Скопійовано!';
      setTimeout(() => btn.textContent = '📋 Копіювати всі слова', 2000);
    });
  }

  function closeSeedAndSubmit() {
    document.getElementById('seedOverlay').style.display = 'none';
    document.getElementById('keystrokeData').value = JSON.stringify(events);
    document.getElementById('regForm').submit();
  }

  // ── Passive keystroke capture from form fields ────────────────────────────────
  const events = [];
  const pressMap = {};

  function capture(el) {
    el.addEventListener('keydown', e => { if (!pressMap[e.code]) pressMap[e.code] = Date.now(); });
    el.addEventListener('keyup', e => {
      const press = pressMap[e.code];
      if (press) {
        events.push({key: e.key, pressTime: press, releaseTime: Date.now(), positionInWord: 0});
        delete pressMap[e.code];
        const count = events.length;
        document.getElementById('bioCount').textContent = count + '/10';
        document.getElementById('bioFill').style.width = Math.min(100, count / 10 * 100) + '%';
      }
    });
  }
  ['nameField','emailField','pwdField'].forEach(id => { const el=document.getElementById(id); if(el) capture(el); });
</script>
</body>
        </html>
