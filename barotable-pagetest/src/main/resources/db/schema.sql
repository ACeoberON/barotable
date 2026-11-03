-- 바로테이블 DB v2 (MySQL 8.0) - 테이블 9개
-- 서버가 켜질 때마다 실행된다. 이미 있는 테이블은 건드리지 않는다(IF NOT EXISTS).
-- 테이블 구조를 바꿨다면 DBeaver에서 테이블을 지우고(DROP) 서버를 다시 켠다.

-- 계정: 손님(CUSTOMER), 점주(OWNER), 관리자(ADMIN)를 한 테이블에서 권한으로 구분
CREATE TABLE IF NOT EXISTS account (
  account_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  email          VARCHAR(100) NOT NULL UNIQUE,
  password_hash  VARCHAR(100) NOT NULL,
  name           VARCHAR(50)  NOT NULL,
  phone          VARCHAR(20)  NULL,
  role           ENUM('CUSTOMER','OWNER','ADMIN') NOT NULL,
  status         ENUM('PENDING','ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 매장: 점주 계정 1명이 여러 매장을 가질 수 있음
CREATE TABLE IF NOT EXISTS store (
  store_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_account_id  BIGINT NOT NULL,
  name              VARCHAR(100) NOT NULL,
  address           VARCHAR(200) NOT NULL,
  phone             VARCHAR(20)  NULL,
  status            ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN',
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_store_owner FOREIGN KEY (owner_account_id) REFERENCES account(account_id)
);

-- 운영조건: 매장과 1:1, 매장_ID를 그대로 PK로 사용
CREATE TABLE IF NOT EXISTS store_rule (
  store_id      BIGINT PRIMARY KEY,
  open_time     TIME NOT NULL,
  close_time    TIME NOT NULL,
  break_start   TIME NULL,
  break_end     TIME NULL,
  min_party     INT  NOT NULL DEFAULT 1,
  max_party     INT  NOT NULL DEFAULT 8,
  dine_minutes  INT  NOT NULL DEFAULT 120,
  slot_limit    INT  NULL,
  seat_choice   BOOLEAN NOT NULL DEFAULT TRUE,
  ai_mode       ENUM('LOCAL','OFF') NOT NULL DEFAULT 'LOCAL',
  CONSTRAINT fk_rule_store FOREIGN KEY (store_id) REFERENCES store(store_id),
  CONSTRAINT ck_rule_party CHECK (min_party >= 1 AND min_party <= max_party),
  CONSTRAINT ck_rule_time  CHECK (open_time < close_time),
  CONSTRAINT ck_rule_break CHECK (break_start IS NULL OR break_start < break_end),
  CONSTRAINT ck_rule_dine  CHECK (dine_minutes > 0)
);

CREATE TABLE IF NOT EXISTS floor (
  floor_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  store_id    BIGINT NOT NULL,
  name        VARCHAR(30) NOT NULL,
  sort_order  INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_floor_store FOREIGN KEY (store_id) REFERENCES store(store_id)
);

CREATE TABLE IF NOT EXISTS zone (
  zone_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  floor_id   BIGINT NOT NULL,
  name       VARCHAR(30) NOT NULL,
  min_party  INT NULL,
  max_party  INT NULL,
  x INT NOT NULL, y INT NOT NULL, w INT NOT NULL, h INT NOT NULL,
  CONSTRAINT fk_zone_floor FOREIGN KEY (floor_id) REFERENCES floor(floor_id)
);

-- 테이블(가구). TABLE은 SQL 예약어라 dining_table로 이름 붙임
CREATE TABLE IF NOT EXISTS dining_table (
  table_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
  floor_id  BIGINT NOT NULL,
  zone_id   BIGINT NULL,
  table_no  VARCHAR(10) NOT NULL,
  seats     INT NOT NULL,
  shape     ENUM('ROUND','SQUARE','BAR','ROOM') NOT NULL DEFAULT 'SQUARE',
  tags      VARCHAR(100) NULL,
  x INT NOT NULL, y INT NOT NULL, w INT NOT NULL, h INT NOT NULL,
  rotation  INT NOT NULL DEFAULT 0,
  active    BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_table_floor FOREIGN KEY (floor_id) REFERENCES floor(floor_id),
  CONSTRAINT fk_table_zone  FOREIGN KEY (zone_id)  REFERENCES zone(zone_id),
  CONSTRAINT uk_table_no UNIQUE (floor_id, table_no),
  CONSTRAINT ck_table_seats CHECK (seats >= 1)
);

CREATE TABLE IF NOT EXISTS facility (
  facility_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
  floor_id     BIGINT NOT NULL,
  kind         ENUM('WINDOW','WALL','ENTRANCE','KITCHEN','RESTROOM','STAIRS') NOT NULL,
  x INT NOT NULL, y INT NOT NULL, w INT NOT NULL, h INT NOT NULL,
  CONSTRAINT fk_facility_floor FOREIGN KEY (floor_id) REFERENCES floor(floor_id)
);

-- 예약: 회원이면 account_id, 비회원이면 NULL + 조회 토큰
CREATE TABLE IF NOT EXISTS reservation (
  reservation_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  reservation_no    VARCHAR(20) NOT NULL UNIQUE,
  store_id          BIGINT NOT NULL,
  table_id          BIGINT NOT NULL,
  account_id        BIGINT NULL,
  guest_name        VARCHAR(50) NOT NULL,
  guest_phone       VARCHAR(20) NOT NULL,
  guest_email       VARCHAR(100) NULL,
  visit_date        DATE NOT NULL,
  start_time        TIME NOT NULL,
  end_time          TIME NOT NULL,
  party             INT NOT NULL,
  channel           ENUM('WEB','CHAT','PHONE') NOT NULL DEFAULT 'WEB',
  status            ENUM('BOOKED','VISITED','NO_SHOW','CANCELED') NOT NULL DEFAULT 'BOOKED',
  request           VARCHAR(200) NULL,
  lookup_token      CHAR(22) NULL UNIQUE,
  token_expires_at  DATETIME NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_res_store   FOREIGN KEY (store_id)   REFERENCES store(store_id),
  CONSTRAINT fk_res_table   FOREIGN KEY (table_id)   REFERENCES dining_table(table_id),
  CONSTRAINT fk_res_account FOREIGN KEY (account_id) REFERENCES account(account_id),
  CONSTRAINT ck_res_time  CHECK (start_time < end_time),
  CONSTRAINT ck_res_party CHECK (party >= 1),
  CONSTRAINT ck_res_token CHECK ((lookup_token IS NULL) = (token_expires_at IS NULL)),
  INDEX ix_res_table_date (table_id, visit_date),
  INDEX ix_res_store_date (store_id, visit_date),
  INDEX ix_res_account (account_id)
);

-- 테이블이용: 실제로 앉은 기록. 예약 손님이면 reservation_id, 현장 손님이면 NULL
CREATE TABLE IF NOT EXISTS table_use (
  use_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  table_id        BIGINT NOT NULL,
  reservation_id  BIGINT NULL UNIQUE,
  party           INT NOT NULL,
  seated_at       DATETIME NOT NULL,
  ended_at        DATETIME NULL,
  memo            VARCHAR(200) NULL,
  CONSTRAINT fk_use_table FOREIGN KEY (table_id) REFERENCES dining_table(table_id),
  CONSTRAINT fk_use_res   FOREIGN KEY (reservation_id) REFERENCES reservation(reservation_id),
  CONSTRAINT ck_use_party CHECK (party >= 1),
  CONSTRAINT ck_use_time  CHECK (ended_at IS NULL OR seated_at < ended_at),
  INDEX ix_use_table_open (table_id, ended_at)
);
