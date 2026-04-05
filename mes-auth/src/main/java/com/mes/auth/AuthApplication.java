package com.mes.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 认证服务启动类
 * 提供用户登录、注册、Token验证等功能
 */
@SpringBootApplication
@MapperScan("com.mes.auth.mapper")
@ComponentScan(basePackages = {"com.mes.auth", "com.mes.common"})
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
