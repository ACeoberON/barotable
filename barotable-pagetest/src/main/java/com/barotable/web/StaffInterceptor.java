package com.barotable.web;

import com.barotable.account.Role;
import com.barotable.api.StaffApiController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class StaffInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        Role role = session == null ? null : (Role) session.getAttribute(StaffApiController.ROLE_KEY);
        boolean adminPath = request.getRequestURI().startsWith("/admin");
        boolean allowed = adminPath ? role == Role.ADMIN : (role == Role.OWNER || role == Role.ADMIN);
        if (allowed) return true;

        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(role == null ? 401 : 403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"점주 로그인이 필요해요.\"}");
        } else {
            response.sendRedirect("/owner/login");
        }
        return false;
    }
}
