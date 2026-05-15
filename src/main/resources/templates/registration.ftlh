<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>HEALTHHUB — Registration</title>
  <link rel="stylesheet" href="/game.css">
</head>
<body>
<div class="auth-page">
  <div class="auth-box" style="max-width:460px;">
    <div class="auth-head">
      <span class="auth-icon">🦸</span>
      <div class="auth-t">HealthHub</div>
      <div class="auth-s">Create your hero and start the journey</div>
    </div>
    <#if errorMessage??><div class="alert-err">❌ ${errorMessage}</div></#if>
    <form method="post" action="/registration" enctype="multipart/form-data">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
      <div class="fg">
        <label class="fl">Hero name</label>
        <input class="fi" type="text" name="name" placeholder="Enter your name" required>
      </div>
      <div class="fg">
        <label class="fl">Email</label>
        <input class="fi" type="email" name="email" placeholder="hero@healthhub.com" required>
      </div>
      <div class="fg">
        <label class="fl">Password (min. 8 chars)</label>
        <input class="fi" type="password" name="password" placeholder="••••••••" required minlength="8">
      </div>
      <div class="fg">
        <label class="fl">Phone</label>
        <input class="fi" type="text" name="phoneNumber" placeholder="+1...">
      </div>
      <div class="fg">
        <label class="fl">Avatar (optional)</label>
        <input class="fi" type="file" name="fileAvatar" accept="image/*" style="padding:10px;cursor:pointer;">
      </div>
      <button class="btn btn-green w100 jc mt8" type="submit">🚀 Start adventure</button>
    </form>
    <div class="auth-div mt16">already have an account?</div>
    <div style="text-align:center;"><a href="/login" class="auth-link">← Sign in</a></div>
  </div>
</div>
</body>
</html>
