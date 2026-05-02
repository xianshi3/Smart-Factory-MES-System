package com.mes.dashboard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Alarm Event Entity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dash_alarm_event")
public class AlarmEvent extends BaseEntity {

    /** Alarm Code */
    private String alarmCode;

    /** Alarm Message */
    private String message;

    /** Alarm Level: CRITICAL/WARNING/INFO */
    private String level;

    /** Alarm Type */
    private String alarmType;

    /** Device Code */
    private String deviceCode;

    /** Device Name */
    private String deviceName;

    /** Status: ACTIVE/ACKNOWLEDGED/RESOLVED */
    private String status;

    /** Occurrence Time */
    private LocalDateTime occurrenceTime;

    /** Acknowledgement Time */
    private LocalDateTime ackTime;

    /** Acknowledged User */
    private String ackUser;

    /** Resolution Time */
    private LocalDateTime resolveTime;

    /** Resolved User */
    private String resolveUser;

    /** Remarks */
    private String remarks;
}