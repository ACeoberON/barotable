package com.barotable.devcheck;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

/** DB 연결 확인 화면: JPA로 MySQL을 실제로 읽고 쓰는 것을 보여 준다 (발표·시연용) */
@Controller
@RequestMapping("/dev/db")
@RequiredArgsConstructor
public class DbCheckController {

    private final DbCheckService service;

    @GetMapping
    public String page(@RequestParam(defaultValue = "2026-10-10") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(required = false) String token,
                       Model model) {
        model.addAttribute("conn", service.connection());
        model.addAttribute("counts", service.counts());
        model.addAttribute("store", service.store());
        model.addAttribute("floors", service.floors());
        model.addAttribute("date", date);
        model.addAttribute("reservations", service.reservations(date));
        model.addAttribute("member", service.memberReservations(4L));
        String t = token == null ? "aB3dE5fG7hJ9kL1mN3pQ5r" : token;
        model.addAttribute("token", t);
        model.addAttribute("tokenResult", service.tokenLookup(t).orElse(null));
        return "dev/db";
    }

    @PostMapping("/overlap")
    public String overlap(@RequestParam Long tableId,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start,
                          RedirectAttributes ra) {
        long n = service.overlaps(tableId, date, start);
        ra.addFlashAttribute("overlapMsg", n == 0
                ? "겹치는 예약 0건 → 예약할 수 있습니다"
                : "겹치는 예약 " + n + "건 → 예약할 수 없습니다");
        return "redirect:/dev/db?date=" + date + "#overlap";
    }

    @PostMapping("/book")
    public String book(@RequestParam Long tableId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start,
                       @RequestParam(defaultValue = "2") int party,
                       @RequestParam(defaultValue = "시연 손님") String name,
                       RedirectAttributes ra) {
        ra.addFlashAttribute("bookMsg", service.book(tableId, date, start, party, name));
        return "redirect:/dev/db?date=" + date + "#reservations";
    }

    @PostMapping("/race")
    public String race(@RequestParam Long tableId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start,
                       @RequestParam(defaultValue = "true") boolean lock,
                       RedirectAttributes ra) throws Exception {
        ra.addFlashAttribute("race", service.race(tableId, date, start, 2, lock));
        ra.addFlashAttribute("raceLock", lock);
        return "redirect:/dev/db?date=" + date + "#race";
    }

    @PostMapping("/reset")
    public String reset(@RequestParam(defaultValue = "2026-10-10") String date, RedirectAttributes ra) {
        ra.addFlashAttribute("resetMsg", "시연으로 만든 예약 " + service.reset() + "건을 지웠습니다");
        return "redirect:/dev/db?date=" + date;
    }
}
