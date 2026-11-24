package com.barotable.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URI;
import java.util.Set;

@Component
public class CsrfInterceptor implements HandlerInterceptor {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!MUTATING_METHODS.contains(request.getMethod())) return true;

        String source = request.getHeader("Origin");
        if (source == null) source = request.getHeader("Referer");
        if (source == null || sameHost(source, request.getServerName())) return true;

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"다른 사이트에서 보낸 요청은 처리할 수 없어요.\"}");
        return false;
    }

    private boolean sameHost(String source, String requestHost) {
        try {
            URI uri = URI.create(source);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && requestHost.equalsIgnoreCase(uri.getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
