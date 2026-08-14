package com.mes.quality;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 质量管理模块启动类
 * @author MES
 * @description 负责质检记录与追溯管理
 */
@SpringBootApplication
@MapperScan({"com.mes.quality.mapper", "com.mes.common.mapper"})
@ComponentScan(basePackages = {"com.mes.quality", "com.mes.common"})
public class QualityApplication {

    public static void main(String[] args) {
        SpringApplication.run(QualityApplication.class, args);
    }
}
