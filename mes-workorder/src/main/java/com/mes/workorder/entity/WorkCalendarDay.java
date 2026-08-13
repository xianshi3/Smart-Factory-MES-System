package com.mes.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排产工作日历实体
 * 对应数据库表 mes_work_calendar（无deleted列，不继承BaseEntity）
 */
@Data
@TableName("mes_work_calendar")
public class WorkCalendarDay {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 日期 */
    @TableField("work_date")
    private LocalDate workDate;

    /** 是否工作日: 1是 0否 */
    @TableField("is_workday")
    private Integer isWorkday;

    /** 备注(节假日等) */
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;
}
