# 바로테이블

식당 예약과 좌석 추천·선택 웹 서비스 (인하공업전문대학 2026-2 심화응용프로젝트)

- [GitHub Pages 미리보기](https://aceoberon.github.io/barotable/)는 시연 데이터로 동작합니다. 서버로 실행하면 MySQL의 예약과 좌석도를 사용합니다.

## 기술 구성

Java 21, Spring Boot 3.5, Gradle, MySQL 8.0, Spring Data JPA, Thymeleaf와 순수 JavaScript를 사용합니다.

## 실행 방법

Java 21과 MySQL 8.0을 준비하고 `barotable-pagetest` 폴더에서 실행합니다. MySQL에 연결할 수 있도록 `DB_PASSWORD` 환경 변수를 설정합니다. 필요하면 `DB_USERNAME`도 설정합니다.

개발 모드는 `dev` 프로필로 실행하며, 기본 주소는 `http://localhost:8080`입니다.

```powershell
.\gradlew.bat bootRun
```

운영 모드는 실행 가능한 JAR를 만든 뒤 `prod` 프로필로 실행합니다. 운영 DB 주소가 기본값과 다르면 `DB_URL` 환경 변수를 설정합니다.

```powershell
.\gradlew.bat bootJar
java -jar .\build\libs\barotable-0.1.0.jar --spring.profiles.active=prod
```

## 구현된 기능

- 손님은 날짜·시간·인원을 골라 규칙에 따른 좌석 추천을 받고 예약할 수 있습니다. 서버는 좌석 조건과 중복 예약을 확인한 뒤 MySQL에 저장합니다.
- 비회원은 예약 조회 링크 또는 예약번호와 연락처로 예약을 조회하고, 예약 시각 전 확정 예약을 취소할 수 있습니다. 회원은 가입·로그인 후 내 예약을 볼 수 있습니다.
- 점주는 로그인 후 예약 목록과 시간표를 보고, 방문 완료·노쇼 처리와 노쇼 되돌리기, 예약 테이블 이동을 할 수 있습니다.

## 미구현 기능과 저장 범위

- 예약 메일 발송과 화면의 실시간 갱신은 미구현입니다.
- 채팅은 규칙으로 입력을 해석하며, 로컬 LLM 단계는 고정 응답을 사용합니다.
- 좌석도 편집, 운영 조건 변경, 휴무일 설정, 워크인 배정은 브라우저에만 저장됩니다.
- Spring Boot 서버에는 HTTPS가 적용되어 있지 않습니다.

## 팀
- 최우진, 지윤석, 이승훈
- 초기 코드: [YSji0421/barotable-pagetest](https://github.com/YSji0421/barotable-pagetest)에서 시작
