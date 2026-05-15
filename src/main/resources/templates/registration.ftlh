<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>CyberShield Hub — Registration</title>
  <link rel="stylesheet" href="/game.css">
  <style>
    .bio-bar{background:rgba(139,92,246,.07);border:1px solid rgba(139,92,246,.22);border-radius:var(--r-md);padding:10px 14px;margin-bottom:6px;display:flex;align-items:center;gap:10px;}
    .bio-dot{width:8px;height:8px;border-radius:50%;background:#8b5cf6;animation:pulse 1.2s ease-in-out infinite;flex-shrink:0;}
    @keyframes pulse{0%,100%{opacity:1;transform:scale(1)}50%{opacity:.5;transform:scale(.8)}}
    .bio-txt{font-family:var(--font-ui);font-size:.72rem;color:#c4b5fd;}
    .bio-count{font-family:var(--font-game);font-size:.75rem;color:#a78bfa;margin-left:auto;}
    .bio-progress{height:3px;background:var(--border);border-radius:2px;margin-bottom:14px;overflow:hidden;}
    .bio-progress-fill{height:100%;background:linear-gradient(90deg,#8b5cf6,#06b6d4);border-radius:2px;transition:width .3s ease;}
    .enroll-ok{background:rgba(34,197,94,.08);border:1px solid rgba(34,197,94,.25);border-radius:var(--r-md);padding:10px 14px;font-family:var(--font-ui);font-size:.78rem;color:#4ade80;margin-bottom:10px;display:none;}
    .enroll-ok.show{display:block;}

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
      Як у крипто-гаманці — без цих слів відновлення неможливе.
    </div>

    <button class="btn-copy-seeds" onclick="copySeeds()">📋 Копіювати всі слова</button>

    <div class="seed-grid" id="seedGrid"></div>

    <div class="seed-warn">
      ⚠️ Це єдиний раз коли вони показуються. Після закриття цього вікна — слова більше не будуть доступні на платформі.
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

    <!-- Biometric indicator -->
    <div class="bio-bar">
      <div class="bio-dot"></div>
      <div>
        <div class="bio-txt">🧬 Keystroke biometrics enrollment</div>
      </div>
      <div class="bio-count" id="bioCount">0/10</div>
    </div>
    <div class="bio-progress"><div class="bio-progress-fill" id="bioFill" style="width:0%"></div></div>
    <div class="enroll-ok" id="enrollOk"></div>

    <form method="post" action="/registration" id="regForm">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
      <input type="hidden" name="keystrokeData" id="keystrokeData"/>
      <input type="hidden" name="seedPhrase" id="seedPhraseField"/>
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
    </form>

    <div class="auth-div mt16">Already have an account?</div>
    <div style="text-align:center;"><a href="/login" class="auth-link">← Sign in</a></div>
  </div>
</div>

<script>
// ── Wordlist for seed generation ──────────────────────────────────────
const WORDS = ['abandon','ability','able','about','above','absent','absorb','abstract','absurd',
'abuse','access','accident','account','accuse','achieve','acid','acoustic','acquire','across',
'act','action','actor','actress','actual','adapt','add','addict','address','adjust','admit',
'adult','advance','advice','aerobic','afford','afraid','again','agent','agree','ahead','aim',
'alert','alien','alley','allow','almost','alone','alpha','already','also','alter','always',
'amateur','amazing','among','amount','amused','analyst','anchor','ancient','anger','angle',
'angry','animal','announce','annual','another','answer','antenna','antique','anxiety','apart',
'approve','april','arctic','area','arena','argue','arm','armed','armor','army','around',
'arrange','arrest','arrive','arrow','art','artist','artwork','aspect','assault','asset',
'assist','assume','asthma','athlete','atom','attack','attend','attitude','attract','auction',
'audit','august','aunt','author','auto','autumn','average','avocado','avoid','awake','aware',
'away','awesome','awful','awkward','axis','baby','balance','bamboo','banana','banner','barely',
'bargain','barrel','base','basic','basket','battle','beach','beauty','because','become',
'before','begin','behave','behind','believe','below','bench','benefit','best','betray','better',
'between','beyond','bicycle','bind','biology','bird','birth','bitter','black','blade','blame',
'blast','bleak','bless','blind','blood','blossom','blouse','blue','blur','blush','board',
'boat','body','boil','bomb','bone','bonus','book','boost','border','bounce','brain','brand',
'brave','breach','brick','bridge','brief','bright','bring','brisk','bronze','brother','brown',
'brush','bubble','buddy','budget','build','bulb','bulk','burden','burst','busy','buyer',
'cable','call','calm','camera','cancel','capital','captain','carbon','castle','catalog',
'catch','cattle','cause','caution','cave','ceiling','celery','cement','census','century',
'cereal','certain','chair','chaos','chapter','charge','chase','cheap','check','cheese',
'chef','cherry','chest','chief','child','chimney','choice','chronic','circle','citizen',
'civil','claim','clap','clarify','claw','clay','clean','clerk','clever','click','client',
'cliff','climb','clinic','clip','clock','close','cloud','coach','coast','coconut','code',
'coffee','coil','coin','collect','color','column','combine','come','comfort','comic','common',
'company','concert','conduct','confirm','congress','connect','consider','control','convince',
'cook','cool','copper','copy','coral','core','correct','cost','cotton','couch','country',
'couple','course','cousin','cover','coyote','crack','cradle','craft','crane','crash','crater',
'crawl','crazy','cream','credit','creek','crew','cricket','crime','crisp','critic','cross',
'crowd','crucial','cruel','cruise','crumble','crunch','crush','cry','crystal','cube','culture',
'cup','curious','current','curtain','cycle','damage','damp','dance','danger','daring','dash',
'daughter','deal','debate','debris','decade','december','decide','decline','decorate','decrease',
'delay','deliver','demand','denial','derive','desert','design','detect','develop','device',
'devote','diagram','dial','diamond','diary','dice','diesel','differ','digital','dignity',
'dilemma','dinner','direct','dirt','disagree','discover','disease','dismiss','disorder',
'display','distance','divert','divide','divorce','dizzy','doctor','document','double','dove',
'draft','dragon','drama','drastic','draw','dream','dress','drift','drill','drink','drip',
'drive','drop','drum','dry','duck','dumb','dune','during','dust','dutch','duty','dwarf',
'dynamic','eager','eagle','early','earn','easily','east','easy','echo','effort','eight',
'either','elbow','elder','elite','else','embark','employ','empty','enable','enact','endless',
'engage','engine','enhance','enjoy','enrich','ensure','enter','entire','entry','equal','escape',
'essay','establish','eternal','evidence','evil','exact','example','excess','exchange','excite',
'exclude','exhaust','exhibit','exile','exist','exotic','extra','fabric','face','faculty','faint',
'faith','fall','false','fame','family','famous','fancy','fantasy','far','fashion','fatal',
'fatigue','fault','february','federal','fiction','field','figure','final','finger','finish',
'fire','firm','fiscal','flame','flash','flat','flavor','flee','flight','flip','float',
'flock','floor','flower','fluid','flush','fly','foam','focus','fog','follow','force','forest',
'forget','fortune','forum','forward','fragile','frame','frequent','front','frozen','fruit',
'future','gadget','galaxy','garden','garlic','gather','gauge','genius','ghost','giraffe',
'girl','give','glad','glance','glare','glass','globe','gloom','glory','glove','glow','glue',
'grant','gravity','great','green','grid','grief','grin','grocery','group','grow','grunt','guard',
'guess','guide','guilt','guitar','gun','habit','hair','half','hammer','hamster','hand','happy',
'harsh','harvest','have','hawk','hazard','head','heart','heavy','hedgehog','height','hello',
'hero','hidden','high','hill','hint','hollow','honor','hope','horror','horse','hospital',
'house','humble','humor','hungry','hunt','hurdle','hurry','hurt','hybrid','icon','ignore',
'image','immune','impact','impose','improve','impulse','inbox','incident','include','income',
'index','indoor','industry','infant','inflict','inform','inhale','inject','inner','input',
'inquiry','inside','inspire','install','intact','interest','into','invest','invite','iron',
'island','ivory','jacket','jaguar','jar','jazz','jewel','join','joke','journey','judge',
'juice','jump','jungle','junior','junk','just','kangaroo','keep','ketchup','key','kick',
'kingdom','kitchen','kiwi','knee','knife','knock','know','lab','lamp','language','laptop',
'large','later','laugh','layer','lazy','leader','learn','leave','lecture','left','legend',
'lemon','lend','length','lens','leopard','level','liar','liberty','library','license','lift',
'limit','linear','link','lion','liquid','list','logic','lonely','long','loop','lottery',
'loud','loyal','lucky','luggage','lunar','lunch','luxury','magic','magnet','mango','master',
'match','media','melody','melt','memory','menu','mercy','metal','method','middle','milk',
'million','mimic','mind','mirror','miss','mistake','mix','model','modify','monitor','moon',
'moral','mother','motion','mountain','mouse','movie','much','museum','music','must','mutual',
'mystery','naive','nature','nearby','neck','network','neutral','news','night','noble','noise',
'north','notable','notice','novel','now','number','nurse','object','observe','obtain','ocean',
'october','offer','olive','open','opera','option','orange','orbit','organ','orient','origin',
'orphan','other','output','outside','owner','oyster','ozone','paper','parent','pattern',
'pause','peace','people','permit','person','planet','plastic','plate','please','pledge',
'pluck','plug','plunge','poem','point','polar','policy','pond','portal','pottery','poverty',
'powder','power','practice','praise','predict','prefer','prepare','present','pretty','prevent',
'price','pride','primary','print','priority','prison','problem','process','produce','profit',
'program','project','promote','proof','proud','provide','public','pulse','pumpkin','purchase',
'puzzle','pyramid','quality','quantum','quarter','question','quick','quit','quote','rabbit',
'race','radar','radio','raise','rally','random','range','rapid','rare','rate','rather',
'raven','ready','reality','reason','rebel','rebuild','recall','receive','recipe','record',
'recycle','reduce','reflect','reform','refuse','region','regret','regular','reject','relax',
'rely','remain','remind','remove','render','repeat','report','require','rescue','resist',
'resource','response','result','retire','reunion','review','reward','rhythm','rich','ridge',
'rifle','right','rigid','riot','ripple','risk','ritual','rival','river','road','robot',
'robust','rocket','romance','roof','rookie','route','royal','rubber','rude','rule','runway',
'rural','sad','salmon','salon','salt','salute','same','sample','sand','satisfy','save',
'scale','scan','scatter','scene','scheme','science','scissors','scorpion','scout','search',
'season','seat','secret','secure','seek','segment','select','sell','seminar','senior','sense',
'series','service','session','settle','setup','shadow','shaft','shallow','share','shift',
'shine','ship','shock','shoe','shoot','short','shoulder','shrimp','shrug','shuffle','shy',
'sibling','siege','sight','sign','silent','silly','silver','similar','since','sing','siren',
'sister','situate','size','sketch','skill','skull','slam','sleep','slender','slice','slide',
'slight','slim','slogan','slot','slow','slush','small','smart','smile','smoke','smooth',
'snack','snake','snap','sniff','snow','solar','solution','solve','someone','song','soon',
'sort','south','space','spare','spatial','spawn','speak','special','speed','sphere','spice',
'spider','spike','spin','spirit','split','spoil','sponsor','spray','spread','spring','spy',
'square','squirrel','stable','staff','stage','stairs','stand','start','state','stay',
'steady','steel','stem','step','stereo','stick','still','sting','stock','stomach','stone',
'stop','store','storm','strategy','street','strike','strong','struggle','student','stuff',
'stumble','style','subject','submit','subway','success','sudden','suffer','suggest','suit',
'supply','supreme','sure','surface','surge','surprise','sustain','swallow','swamp','swap',
'swear','sweet','swift','swing','switch','sword','symbol','system','tackle','talent','tank',
'tape','target','task','taste','teach','team','tell','tennis','tent','term','test','theme',
'theory','think','tiger','tiny','tired','title','today','together','toilet','token','tomorrow',
'torch','total','toward','tower','town','track','trade','traffic','tragic','train','transfer',
'travel','treat','tree','trend','trial','trick','trigger','trim','trip','trophy','trouble',
'truck','truly','trumpet','trust','truth','tunnel','turkey','turn','turtle','twice','twin',
'twist','unable','undo','unfair','unhappy','unique','universe','unknown','until','unusual',
'unveil','update','upgrade','upon','urban','usage','used','useful','useless','usual','utility',
'vacant','vacuum','valid','valley','valve','vanish','vapor','various','vault','vendor','venture',
'verify','version','very','veteran','viable','vibrant','vicious','victory','video','view',
'village','vintage','violin','virtual','visa','vivid','vocal','voice','void','volume','vote',
'voyage','wage','wagon','wait','walk','wall','walnut','want','warfare','warm','warrior',
'waste','water','wave','way','wealth','weapon','wear','weird','welcome','western','whale',
'wheat','where','whip','whisper','wide','width','wife','wild','window','wine','wing','winter',
'wire','wise','wish','witness','wonder','wood','world','worry','worth','wrap','wreck','wrestle',
'wrist','write','wrong','yard','year','yellow','young','youth','zebra','zero','zone','zoo'];

function generateSeedPhrase() {
  const words = [];
  const arr = new Uint32Array(10);
  crypto.getRandomValues(arr);
  for (let i = 0; i < 10; i++) {
    words.push(WORDS[arr[i] % WORDS.length]);
  }
  return words;
}

let currentSeed = [];
let seedConfirmed = false;

function showSeedModal() {
  // Validate form first
  const name = document.getElementById('nameField').value.trim();
  const email = document.getElementById('emailField').value.trim();
  const pwd = document.getElementById('pwdField').value;
  if (!name || !email || !pwd || pwd.length < 8) {
    alert('Заповніть всі поля (пароль мінімум 8 символів)');
    return;
  }

  currentSeed = generateSeedPhrase();
  const grid = document.getElementById('seedGrid');
  grid.innerHTML = '';
  currentSeed.forEach((word, i) => {
    const div = document.createElement('div');
    div.className = 'seed-item';
    div.innerHTML = '<span class="seed-num">' + (i+1) + '.</span><span class="seed-word">' + word + '</span>';
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

// ── Keystroke capture ──────────────────────────────────────────────────
const events = [];
const pressMap = {};

function capture(el) {
  el.addEventListener('keydown', e => { if (!pressMap[e.code]) pressMap[e.code] = Date.now(); });
  el.addEventListener('keyup', e => {
    const press = pressMap[e.code];
    if (press) {
      events.push({ key: e.key, pressTime: press, releaseTime: Date.now(), positionInWord: 0 });
      delete pressMap[e.code];
      const count = events.length;
      document.getElementById('bioCount').textContent = count + '/10';
      document.getElementById('bioFill').style.width = Math.min(100, count / 10 * 100) + '%';
      if (count >= 10) tryEnroll();
    }
  });
}
['nameField','emailField','pwdField'].forEach(id => { const el=document.getElementById(id); if(el) capture(el); });

let enrolled = false;
async function tryEnroll() {
  if (enrolled || events.length < 10) return;
  try {
    const res = await fetch('/biometrics/analyze', {
      method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({events})
    });
    const d = await res.json();
    if (!d.error) {
      enrolled = true;
      const ok = document.getElementById('enrollOk');
      ok.textContent = '🧬 Профіль записано — ' + d.uniquenessScore + '% унікальність | ' + d.typingSpeedCpm + ' CPM';
      ok.classList.add('show');
    }
  } catch(_) {}
}
</script>
</body>
</html>
