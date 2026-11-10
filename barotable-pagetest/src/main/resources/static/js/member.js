/* 회원 로그인 · 회원가입
 * - 서버에서 열면: POST /api/member/login · /signup · /logout (비밀번호는 서버에서 BCrypt 해시로 비교)
 * - GitHub Pages(정적)에서 열면: data.js의 MEMBERS로 흉내 (member@example.com / barotable)
 * - 로그인 상태는 sessionStorage('bt.member')에 저장한다. 비밀번호는 저장하지 않는다.
 * - 로그인하면 손님 화면 위쪽에 이름과 로그아웃이 보이고, 예약자 정보 화면(confirm)에 이름·연락처·이메일이 미리 채워진다.
 * 실제 구현: POST /member/login, POST /member/logout (Spring Security, 역할 MEMBER)
 */
window.Member = (function () {
  const KEY = 'bt.member';
  const current = () => BT.sget(KEY, null);
  /* 서버 모드: 결과를 Promise로 돌려준다 (성공하면 회원 정보, 실패하면 오류 메시지) */
  function serverLogin(email, password) {
    return API.post('/api/member/login', { email: String(email || '').trim(), password })
      .then(me => { BT.sset(KEY, me); return me; });
  }
  function serverSignup(body) {
    return API.post('/api/member/signup', body).then(me => { BT.sset(KEY, me); return me; });
  }
  function login(email, password) {
    const e = String(email || '').trim().toLowerCase();
    const m = (BT_DATA.MEMBERS || []).find(x => x.email.toLowerCase() === e && x.password === password);
    if (!m) return null;
    const me = { email: m.email, name: m.name, phone: m.phone };
    BT.sset(KEY, me);
    return me;
  }
  const logout = () => { BT.sremove(KEY); if (window.API && API.on) API.post('/api/member/logout').catch(() => {}); };

  /* 위쪽 '회원 로그인' 버튼 → 로그인 중이면 '이름 님' + '로그아웃' */
  function renderHeader() {
    const me = current();
    document.querySelectorAll('.member-name').forEach(el => el.remove());
    document.querySelectorAll('a[data-go="member-login"]').forEach(a => {
      if (!a.dataset.label) a.dataset.label = a.textContent;
      if (!me) {
        a.textContent = a.dataset.label; a.removeAttribute('title'); a.href = BT.url('member-login'); delete a.dataset.member;
        return;
      }
      a.textContent = '로그아웃'; a.title = `${me.name} 님 로그아웃`; a.href = '#'; a.dataset.member = 'logout';
      if (a.closest('.site-head')) {
        const name = document.createElement('span');
        name.className = 'head-link member-name'; name.textContent = `${me.name} 님`;
        a.parentNode.insertBefore(name, a);
      }
    });
  }
  document.addEventListener('click', e => {
    const a = e.target.closest('a[data-member="logout"]');
    if (!a) return;
    e.preventDefault(); e.stopPropagation();
    logout(); renderHeader(); BT.toast('로그아웃했어요');
  }, true);

  /* 회원 로그인 화면 */
  function initLoginPage() {
    const form = document.getElementById('member-login-form');
    if (!form) return;
    const msg = document.getElementById('member-login-message');
    const show = (text, good) => { msg.hidden = false; msg.textContent = text; msg.classList.toggle('good', !!good); };
    const me = current();
    if (me) show(`${me.name} 님으로 로그인되어 있어요. 다른 계정으로 로그인하면 바뀝니다.`, true);
    form.addEventListener('submit', e => {
      e.preventDefault();
      const email = document.getElementById('member-email'), pw = document.getElementById('member-password');
      if (!email.value.trim() || !email.checkValidity()) { show('이메일 주소를 확인해 주세요.'); email.focus(); return; }
      if (!pw.value) { show('비밀번호를 입력해 주세요.'); pw.focus(); return; }
      const done = m => {
        BT.toast(`${m.name} 님, 환영합니다`);
        const next = new URLSearchParams(location.search).get('next');
        setTimeout(() => BT.go(['reserve', 'confirm', 'lookup'].includes(next) ? next : 'index'), 400);
      };
      if (window.API && API.on) {
        serverLogin(email.value, pw.value).then(done).catch(err => { show(err.message); pw.value = ''; pw.focus(); });
        return;
      }
      const m = login(email.value, pw.value);
      if (!m) { show('이메일 또는 비밀번호가 맞지 않아요.'); pw.value = ''; pw.focus(); return; }
      done(m);
    });
  }

  /* 회원가입 (서버에서만 저장. 정적 화면에서는 안내만) */
  function initSignup() {
    const form = document.getElementById('member-signup-form');
    if (!form) return;
    const msg = document.getElementById('member-signup-message');
    const show = (text, good) => { msg.hidden = false; msg.textContent = text; msg.classList.toggle('good', !!good); };
    const phone = document.getElementById('su-phone');
    phone.addEventListener('input', () => {
      const d = phone.value.replace(/\D/g, '').slice(0, 11);
      phone.value = d.length > 7 ? `${d.slice(0,3)}-${d.slice(3,7)}-${d.slice(7)}` : d.length > 3 ? `${d.slice(0,3)}-${d.slice(3)}` : d;
    });
    form.addEventListener('submit', e => {
      e.preventDefault();
      const v = id => document.getElementById(id).value.trim();
      const body = { name: v('su-name'), phone: v('su-phone'), email: v('su-email'), password: document.getElementById('su-password').value };
      if (body.name.length < 2) return show('이름을 두 글자 이상 입력해 주세요.');
      if (!/^01[016789]\d{7,8}$/.test(body.phone.replace(/\D/g, ''))) return show('올바른 휴대전화 번호를 입력해 주세요.');
      if (!document.getElementById('su-email').checkValidity() || !body.email) return show('이메일 주소를 확인해 주세요.');
      if (body.password.length < 8) return show('비밀번호는 8자 이상으로 정해 주세요.');
      if (body.password !== document.getElementById('su-password2').value) return show('비밀번호 확인이 맞지 않아요.');
      if (!(window.API && API.on)) return show('회원가입은 서버에서 실행할 때 저장돼요. 지금은 시연용 계정으로 로그인해 주세요.');
      serverSignup(body).then(m => { BT.toast(`${m.name} 님, 가입을 환영합니다`); setTimeout(() => BT.go('index'), 500); })
        .catch(err => show(err.message));
    });
  }

  /* 서버 세션이 끝났으면(서버 재시작 등) 화면의 로그인 표시도 지운다 */
  if (window.API && API.on && current()) {
    API.get('/api/member/me').then(me => BT.sset(KEY, me)).catch(() => { BT.sremove(KEY); renderHeader(); });
  }

  renderHeader();
  initLoginPage();
  initSignup();
  return { current, login, logout };
})();
