package com.basis.configuration;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    // 注册 Sa-Token 拦截器，打开注解式鉴权功能
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，定义鉴权规则
        registry.addInterceptor(new SaInterceptor(handle -> {
            StpUtil.checkLogin();
        })).addPathPatterns("/**")
        .excludePathPatterns("/auth/login")
        .excludePathPatterns("/auth/register")
        .excludePathPatterns("/auth/logout")
        .excludePathPatterns("/user/avatar-upload/callback")
        .excludePathPatterns("/file/upload/callback")
        .excludePathPatterns("/doc.html");
    }
}

