package com.barotable.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final StaffInterceptor staffInterceptor;
    private final CsrfInterceptor csrfInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(csrfInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(staffInterceptor)
                .addPathPatterns("/owner/**", "/admin/**", "/admin", "/api/owner/**")
                .excludePathPatterns("/owner/login", "/owner/signup", "/owner/onboarding/**");
    }
}
