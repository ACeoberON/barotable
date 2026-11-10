package com.barotable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 화면이 쓰는 API 시험 (실제 MySQL 사용).
 * 시험 날짜(오늘+30일)의 예약과 시험용 가입 계정은 끝나면 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate DAY = LocalDate.now().plusDays(30);
    static final String SIGNUP_EMAIL = "apitest@example.com";

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from reservation where visit_date = ?", DAY);
        jdbc.update("delete from account where email = ?", SIGNUP_EMAIL);
    }

    private String booking(String table, String time, int party) {
        return """
               {"table":"%s","date":"%s","time":"%s","party":%d,"name":"홍길동","phone":"010-1234-0000","email":"hong@example.com","request":"창가"}
               """.formatted(table, DAY, time, party);
    }

    @Test
    void 화면_첫_데이터는_DB_좌석도와_예약을_담는다() throws Exception {
        String js = mvc.perform(get("/api/boot.js?scope=owner")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(js).startsWith("window.BT_DB = ");
        JsonNode db = om.readTree(js.substring("window.BT_DB = ".length(), js.lastIndexOf(';')));
        assertThat(db.get("floors")).hasSize(2);
        assertThat(db.get("store").get("dineMinutes").asInt()).isEqualTo(90);
        // 손님 화면용에는 예약자 연락처가 비어 있다
        String guest = mvc.perform(get("/api/boot.js")).andReturn().getResponse().getContentAsString();
        assertThat(guest).doesNotContain("010-2345-1111");
    }

    @Test
    void 비회원_예약_저장_후_토큰조회_겹침거절_취소() throws Exception {
        String body = mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(booking("T03", "19:00", 4)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode r = om.readTree(body);
        String no = r.get("no").asText(), token = r.get("token").asText();
        assertThat(no).startsWith("BT-" + DAY.toString().replace("-", "") + "-");
        assertThat(token).hasSize(22);

        // 같은 테이블 19:30 → 겹침 거절(409), 20:30 → 허용
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(booking("T03", "19:30", 2)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("이미 예약된")));
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(booking("T03", "20:30", 2)))
                .andExpect(status().isOk());

        // 규칙 위반: 2인 테이블에 4명, 브레이크 타임
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(booking("T01", "12:00", 4)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(booking("T04", "15:30", 2)))
                .andExpect(status().isConflict());
        // 2명이 단체석(T08, 6인)은 불가
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(booking("T08", "12:00", 2)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("단체석")));

        // 메일 링크(토큰)로 조회, 예약번호+연락처로 조회
        mvc.perform(get("/api/reservations/lookup").param("t", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.no").value(no)).andExpect(jsonPath("$.table").value("T03"));
        mvc.perform(post("/api/reservations/lookup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"no\":\"" + no + "\",\"phone\":\"01012340000\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/reservations/lookup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"no\":\"" + no + "\",\"phone\":\"010-9999-9999\"}"))
                .andExpect(status().isNotFound());

        // 취소: 틀린 토큰은 거부, 맞는 토큰은 취소
        mvc.perform(post("/api/reservations/" + no + "/cancel").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"wrong\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/reservations/" + no + "/cancel").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("cancelled"));
        // 취소된 자리는 다시 예약 가능
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(booking("T03", "19:00", 4)))
                .andExpect(status().isOk());
    }

    @Test
    void 회원가입_로그인_회원예약은_계정으로_조회() throws Exception {
        MockHttpSession s = new MockHttpSession();
        mvc.perform(post("/api/member/signup").session(s).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + SIGNUP_EMAIL + "\",\"password\":\"password1\",\"name\":\"시험회원\",\"phone\":\"010-5555-0000\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("시험회원"));
        // 비밀번호는 해시로 저장
        String hash = jdbc.queryForObject("select password_hash from account where email = ?", String.class, SIGNUP_EMAIL);
        assertThat(hash).startsWith("$2").doesNotContain("password1");
        // 같은 이메일 재가입 거절
        mvc.perform(post("/api/member/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + SIGNUP_EMAIL + "\",\"password\":\"password1\",\"name\":\"시험회원\",\"phone\":\"010-5555-0000\"}"))
                .andExpect(status().isConflict());

        // 틀린 비밀번호 거절, 맞으면 로그인
        mvc.perform(post("/api/member/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + SIGNUP_EMAIL + "\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
        // 로그인하면 세션 번호가 새로 발급되므로, 응답 뒤의 세션을 이어서 쓴다
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/api/member/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + SIGNUP_EMAIL + "\",\"password\":\"password1\"}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
        mvc.perform(get("/api/member/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(SIGNUP_EMAIL));

        // 회원 예약: 토큰 없이 계정으로 저장
        mvc.perform(post("/api/reservations").session(session).contentType(MediaType.APPLICATION_JSON).content(booking("T05", "18:00", 2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").doesNotExist());
        mvc.perform(get("/api/member/reservations").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].table").value("T05"));

        // 로그아웃 후에는 내 예약을 볼 수 없다
        mvc.perform(post("/api/member/logout").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/member/reservations")).andExpect(status().isUnauthorized());
    }

    @Test
    void 시연용_회원계정으로_로그인() throws Exception {
        mvc.perform(post("/api/member/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"member@example.com\",\"password\":\"barotable\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("김바로"));
    }

    @Test
    void 점주_상태변경과_테이블이동() throws Exception {
        String a = om.readTree(mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(booking("T04", "18:00", 4)))
                .andReturn().getResponse().getContentAsString()).get("no").asText();
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(booking("T07", "18:30", 4)))
                .andExpect(status().isOk());

        // T07은 18:30 예약과 겹쳐서 이동 거절, T08(6인석)은 허용
        mvc.perform(patch("/api/owner/reservations/" + a).contentType(MediaType.APPLICATION_JSON).content("{\"table\":\"T07\"}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/owner/reservations/" + a).contentType(MediaType.APPLICATION_JSON).content("{\"table\":\"T08\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.table").value("T08"));
        mvc.perform(patch("/api/owner/reservations/" + a).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"noshow\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("noshow"));
        assertThat(jdbc.queryForObject("select status from reservation where reservation_no = ?", String.class, a)).isEqualTo("NO_SHOW");
    }
}
