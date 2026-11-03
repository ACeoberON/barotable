package com.barotable.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 프로토타입 화면을 서버 주소로 보여 준다.
 * 주소는 static/js/common.js 의 ROUTES 와 같다.
 * 아직 화면 데이터는 JS에 하드코딩돼 있고, 다음 단계에서 DB 값으로 바꾼다.
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
