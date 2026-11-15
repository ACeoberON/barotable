package com.barotable.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final StaffInterceptor staffInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(staffInterceptor)
                .addPathPatterns("/owner/**", "/admin/**", "/admin", "/api/owner/**")
                .excludePathPatterns("/owner/login");
    }
}
