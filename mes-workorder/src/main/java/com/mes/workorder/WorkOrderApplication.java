package com.mes.workorder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 工单服务启动类
 * 提供工单管理、报工处理等功能
 */
@SpringBootApplication
@MapperScan("com.mes.workorder.mapper")
public class WorkOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkOrderApplication.class, args);
    }
}
