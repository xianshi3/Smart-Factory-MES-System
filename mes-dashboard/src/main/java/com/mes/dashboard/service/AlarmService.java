package com.mes.dashboard.service;

import com.mes.dashboard.entity.AlarmEvent;

import java.util.List;

/**
 * Alarm Management Service Interface
 */
public interface AlarmService {

    /**
     * Create alarm event
     */
    void createAlarm(AlarmEvent alarm);

    /**
     * Get all alarms
     */
    List<AlarmEvent> getAllAlarms();

    /**
     * Get alarms by status
     */
    List<AlarmEvent> getAlarmsByStatus(String status);

    /**
     * Acknowledge alarm
     */
    void acknowledgeAlarm(Long alarmId, String userId);

    /**
     * Resolve alarm
     */
    void resolveAlarm(Long alarmId, String userId, String remarks);

    /**
     * Delete alarm
     */
    void deleteAlarm(Long alarmId);

    /**
     * Get active alarm count
     */
    long getActiveAlarmCount();
}