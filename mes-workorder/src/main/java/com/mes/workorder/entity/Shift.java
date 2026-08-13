package com.mes.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 排产班次实体
 * 对应数据库表 mes_shift（无deleted列，不继承BaseEntity）
 */
@Data
@TableName("mes_shift")
public class Shift {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 班次编码: DAY/NIGHT */
    @TableField("shift_code")
    private String shiftCode;

    /** 班次名称 */
    @TableField("shift_name")
    private String shiftName;

    /** 开始时间 */
    @TableField("start_time")
    private LocalTime startTime;

    /** 结束时间 */
    @TableField("end_time")
    private LocalTime endTime;

    /** 是否排产可用: 1是 0否 */
    @TableField("is_work")
    private Integer isWork;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
