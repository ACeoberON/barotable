/* =========================================================
 * DB 값 연결 (data.js 바로 다음, common.js 앞에서 불러온다)
 * - 서버(Spring Boot)에서 열면 /api/boot.js 가 window.BT_DB 에 DB 값을 넣어 준다.
 *   그 값으로 하드코딩 데이터(BT_DATA)의 매장·좌석도·예약을 바꿔 끼운다.
 * - GitHub Pages(정적 파일)에서 열면 BT_DB 가 없으므로 아무것도 하지 않고 하드코딩 데이터를 그대로 쓴다.
 * - window.API: 서버에 저장하는 요청(예약 저장·취소, 로그인 등). API.on 이 false 면 서버가 없는 것.
 * ========================================================= */
(function () {
  const DB = window.BT_DB;
  const D = window.BT_DATA;

  window.API = {
    on: !!DB,
    async call(method, url, body) {
      const res = await fetch(url, {
        method, credentials: 'same-origin',
        headers: body ? { 'Content-Type': 'application/json' } : {},
        body: body ? JSON.stringify(body) : undefined
      });
      let data = null;
      try { data = await res.json(); } catch (_) { data = null; }
      if (!res.ok) {
        const err = new Error((data && data.message) || '서버와 통신하지 못했어요. 잠시 후 다시 시도해 주세요.');
        err.status = res.status; throw err;
      }
      return data;
    },
    get(url) { return this.call('GET', url); },
    post(url, body) { return this.call('POST', url, body || {}); },
    patch(url, body) { return this.call('PATCH', url, body || {}); }
  };

  if (!DB) return;

  /* 매장·운영조건: DB 값으로 덮어쓰기 (DB에 없는 항목은 하드코딩 값 유지) */
  Object.assign(D.STORE, DB.store, { hours: Object.assign({}, D.STORE.hours, DB.store.hours) });
  /* 좌석도 */
  D.FLOORS = DB.floors;
  /* 예약 (날짜가 들어 있어서 오늘 목록·다른 날 목록이 DB 그대로 나뉜다) */
  D.OWNER_RESERVATIONS = DB.reservations;
  if (DB.noShowHistory) D.NOSHOW_HISTORY = DB.noShowHistory;
  /* 시연용 하드코딩 회원 계정은 쓰지 않는다 (로그인은 서버에서 확인) */
  D.MEMBERS = [];

  /* 예전에 정적 화면에서 남긴 값이 DB 값을 덮지 않게 정리
   *  bt.floors: 좌석도 편집기 임시 저장, bt.store: 운영 조건 임시 저장, bt.res: 예약 변경 기록 */
  try {
    if (sessionStorage.getItem('bt.db') !== '1') {
      ['bt.floors', 'bt.store', 'bt.res', 'bt.draft', 'bt.member'].forEach(k => sessionStorage.removeItem(k));
      sessionStorage.setItem('bt.db', '1');
    }
  } catch (_) {}
})();
