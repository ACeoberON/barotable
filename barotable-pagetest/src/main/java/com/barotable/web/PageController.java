package com.barotable.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 프로토타입 화면을 서버 주소로 보여 준다.
 * 주소는 static/js/common.js 의 ROUTES 와 같다.
 * 화면 데이터는 /api/boot.js(DB 값)를 받아 쓰고, 저장은 /api/** 로 한다 (com.barotable.api 패키지).
 * GitHub Pages(정적)에서는 하드코딩 데이터(static/js/data.js)로 그대로 동작한다.
 */
@Controller
public class PageController {

    // 손님
    @GetMapping("/")                    public String index()    { return "index"; }
    @GetMapping("/reserve")             public String reserve()  { return "reserve"; }
    @GetMapping("/reserve/confirm")     public String confirm()  { return "confirm"; }
    @GetMapping("/reserve/complete")    public String complete() { return "complete"; }
    @GetMapping("/reservations/lookup") public String lookup()   { return "lookup"; }
    @GetMapping("/member/login")        public String memberLogin() { return "member-login"; }

    // 점주, 관리자
    @GetMapping("/owner/login")     public String ownerLogin()     { return "owner/login"; }
    @GetMapping("/owner/dashboard") public String ownerDashboard() { return "owner/dashboard"; }
    @GetMapping("/owner/guests")    public String ownerGuests()    { return "owner/guests"; }
    @GetMapping("/owner/schedule")  public String ownerSchedule()  { return "owner/schedule"; }
    @GetMapping("/owner/seat-map")  public String ownerSeatMap()   { return "owner/editor"; }
    @GetMapping("/owner/settings")  public String ownerSettings()  { return "owner/settings"; }
    @GetMapping("/admin")           public String admin()          { return "admin/dashboard"; }
}
