-- 시험 데이터 (프로토타입 바로키친 성수점과 같은 값)
-- INSERT IGNORE: 이미 같은 번호의 행이 있으면 건너뛴다. 서버를 여러 번 켜도 중복되지 않는다.

INSERT IGNORE INTO account (account_id, email, password_hash, name, phone, role, status) VALUES
 (1, 'admin@example.com', '{bcrypt}...', '관리자', NULL, 'ADMIN', 'ACTIVE'),
 (2, 'owner@example.com', '{bcrypt}...', '김점주', '010-1000-0000', 'OWNER', 'ACTIVE'),
 (3, 'owner2@example.com', '{bcrypt}...', '이대표', '010-2000-0000', 'OWNER', 'PENDING'),
 (4, 'minji@example.com', '{bcrypt}...', '김민지', '010-2345-1111', 'CUSTOMER', 'ACTIVE');

INSERT IGNORE INTO store (store_id, owner_account_id, name, address, phone) VALUES
 (1, 2, '바로키친 성수점', '서울 성동구 성수이로 00, 1–2층', '02-000-0000');

INSERT IGNORE INTO store_rule (store_id, open_time, close_time, break_start, break_end, min_party, max_party, dine_minutes, slot_limit, seat_choice, ai_mode)
VALUES (1, '11:30', '22:00', '15:00', '17:00', 1, 8, 90, 8, TRUE, 'LOCAL');

INSERT IGNORE INTO floor (floor_id, store_id, name, sort_order) VALUES (1, 1, '1층', 1), (2, 1, '2층', 2);

INSERT IGNORE INTO zone (zone_id, floor_id, name, min_party, max_party, x, y, w, h) VALUES
 (1, 1, '창가 구역', 2, NULL, 30, 36, 570, 150),
 (2, 1, '홀 중앙', NULL, NULL, 30, 214, 570, 256),
 (3, 1, '바 카운터', NULL, 2, 630, 214, 200, 256),
 (4, 2, '테라스', NULL, 4, 30, 36, 430, 200),
 (5, 2, '라운지', NULL, NULL, 30, 264, 430, 300),
 (6, 2, '단체룸', 5, NULL, 490, 36, 340, 528);

INSERT IGNORE INTO dining_table (table_id, floor_id, zone_id, table_no, seats, shape, tags, x, y, w, h) VALUES
 (1, 1, 1, 'T01', 2, 'ROUND', '창가', 66, 66, 90, 90),
 (2, 1, 1, 'T02', 2, 'ROUND', '창가', 200, 66, 90, 90),
 (3, 1, 1, 'T03', 4, 'SQUARE', '창가,조용함', 350, 72, 160, 80),
 (4, 1, 2, 'T04', 4, 'SQUARE', '중앙', 66, 244, 150, 80),
 (5, 1, 2, 'T05', 2, 'ROUND', '중앙', 262, 240, 90, 90),
 (6, 1, 3, 'T06', 2, 'BAR', '바', 660, 238, 140, 54),
 (7, 1, 2, 'T07', 4, 'SQUARE', '중앙', 404, 244, 150, 80),
 (8, 1, 2, 'T08', 6, 'SQUARE', '단체', 96, 364, 230, 80),
 (9, 1, 2, 'T09', 2, 'ROUND', '입구 근처', 420, 360, 90, 90),
 (10, 1, 3, 'T10', 2, 'BAR', '바', 660, 314, 140, 54),
 (11, 1, 3, 'T11', 2, 'BAR', '바', 660, 390, 140, 54),
 (12, 2, 4, 'T21', 2, 'ROUND', '야외', 60, 84, 90, 90),
 (13, 2, 4, 'T23', 4, 'SQUARE', '야외,전망', 310, 90, 130, 80),
 (14, 2, 5, 'T24', 4, 'SQUARE', '소파', 60, 300, 160, 80),
 (15, 2, 6, 'R01', 8, 'ROOM', '프라이빗', 524, 64, 272, 200);

INSERT IGNORE INTO facility (facility_id, floor_id, kind, x, y, w, h) VALUES
 (1, 1, 'WINDOW', 30, 12, 570, 14), (2, 1, 'KITCHEN', 630, 36, 340, 150), (3, 1, 'ENTRANCE', 400, 556, 160, 30),
 (4, 1, 'STAIRS', 860, 214, 110, 120), (5, 1, 'RESTROOM', 860, 360, 110, 110), (6, 2, 'STAIRS', 860, 36, 110, 120);

-- 2026-10-10(토) 예약. 1~3은 비회원(조회 토큰), 4는 회원(account_id=4)
INSERT IGNORE INTO reservation (reservation_id, reservation_no, store_id, table_id, account_id, guest_name, guest_phone, guest_email,
  visit_date, start_time, end_time, party, channel, status, request, lookup_token, token_expires_at) VALUES
 (1, 'BT-20261010-0001', 1, 5, NULL, '정하늘', '010-1111-4444', 'sky@example.com', '2026-10-10', '18:30', '20:00', 2, 'WEB', 'BOOKED', NULL, 'aB3dE5fG7hJ9kL1mN3pQ5r', '2026-10-11 00:00:00'),
 (2, 'BT-20261010-0002', 1, 9, NULL, '한지우', '010-5555-1212', 'jiwoo@example.com', '2026-10-10', '19:00', '20:30', 2, 'CHAT', 'BOOKED', NULL, 'cD4eF6gH8jK0lM2nP4qR6s', '2026-10-11 00:00:00'),
 (3, 'BT-20261010-0003', 1, 6, NULL, '최도윤', '010-2222-5555', NULL, '2026-10-10', '18:00', '19:30', 2, 'PHONE', 'BOOKED', NULL, NULL, NULL),
 (4, 'BT-20261010-0004', 1, 4, 4, '김민지', '010-2345-1111', 'minji@example.com', '2026-10-10', '12:00', '13:30', 4, 'WEB', 'BOOKED', NULL, NULL, NULL),
 (5, 'BT-20261003-0007', 1, 1, 4, '김민지', '010-2345-1111', 'minji@example.com', '2026-10-03', '18:00', '19:30', 2, 'WEB', 'VISITED', NULL, NULL, NULL);

INSERT IGNORE INTO table_use (use_id, table_id, reservation_id, party, seated_at, ended_at) VALUES
 (1, 1, 5, 2, '2026-10-03 18:02:00', '2026-10-03 19:20:00');
