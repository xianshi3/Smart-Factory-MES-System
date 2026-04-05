package com.mes.process;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 工艺管理模块启动类
 * @author MES
 * @description 负责工艺模板、工艺参数的管理
 */
@SpringBootApplication
@MapperScan("com.mes.process.mapper")
public class ProcessApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcessApplication.class, args);
    }
}
