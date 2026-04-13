package com.mes.dashboard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 看板管理模块启动类
 * @author MES
 * @description 负责生产看板、设备状态、OEE计算
 */
@SpringBootApplication
@MapperScan("com.mes.dashboard.mapper")
@ComponentScan(basePackages = {"com.mes.dashboard", "com.mes.common"})
@EnableScheduling
public class DashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardApplication.class, args);
    }
}
