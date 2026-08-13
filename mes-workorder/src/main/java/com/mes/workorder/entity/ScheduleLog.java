package com.mes.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排产变更日志实体
 * 对应数据库表 wo_schedule_log（无deleted列，不继承BaseEntity）
 */
@Data
@TableName("wo_schedule_log")
public class ScheduleLog {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 工单ID */
    @TableField("work_order_id")
    private Long workOrderId;

    /** 排产明细ID */
    @TableField("schedule_id")
    private Long scheduleId;

    /** 操作: AUTO_PLAN/REPLAN/MOVE/SWAP/RESIZE/FREEZE/UNFREEZE/RELEASE/HOLD/UNDO/ASSIGN/UNASSIGN */
    private String action;

    /** 操作描述 */
    @TableField("action_desc")
    private String actionDesc;

    /** 操作前快照 */
    @TableField("before_json")
    private String beforeJson;

    /** 操作后快照 */
    @TableField("after_json")
    private String afterJson;

    /** 操作人 */
    @TableField("operator_id")
    private Long operatorId;

    @TableField("create_time")
    private LocalDateTime createTime;
}
