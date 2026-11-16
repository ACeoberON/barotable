/* 화면 이동: 서버에서 열면 서버 주소, 파일로 열면 같은 폴더의 html (static/js/common.js ROUTES와 같은 주소) */
window.goOwner = function (name) {
  const R = {
    'owner-login': ['login.html', '/owner/login'], 'owner-signup': ['signup.html', '/owner/signup'],
    'owner-onboarding-welcome': ['onboarding-welcome.html', '/owner/onboarding/welcome'],
    'owner-onboarding-holidays': ['onboarding-holidays.html', '/owner/onboarding/holidays'],
    'owner-onboarding-preview': ['onboarding-preview.html', '/owner/onboarding/preview'],
    'owner-editor': ['editor.html', '/owner/seat-map'], 'owner-dashboard': ['dashboard.html', '/owner/dashboard']
  }[name];
  location.href = document.body.dataset.server === 'true' ? R[1] : R[0];
};
