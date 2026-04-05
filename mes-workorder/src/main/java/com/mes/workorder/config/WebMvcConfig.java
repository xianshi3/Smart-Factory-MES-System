package com.mes.workorder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类 - 配置跨域访问规则
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    /**
     * 配置跨域映射
     * 允许所有来源、所有方法、所有请求头，带凭证
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
