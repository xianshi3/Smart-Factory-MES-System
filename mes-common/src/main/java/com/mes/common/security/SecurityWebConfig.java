package com.mes.common.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 自动注册 {@link SecurityInterceptor}。
 * 所有组件扫描 com.mes.common 的服务（各 Spring Boot 应用）会自动生效，无需各服务单独配置。
 */
@Configuration
public class SecurityWebConfig implements WebMvcConfigurer {

    private final PermissionService permissionService;

    public SecurityWebConfig(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SecurityInterceptor(permissionService)).addPathPatterns("/**");
    }
}