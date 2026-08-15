package com.mes.dashboard.service.impl;

import com.mes.dashboard.entity.AlarmEvent;
import com.mes.dashboard.mapper.AlarmMapper;
import com.mes.dashboard.service.AlarmService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Alarm Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService {

    private final AlarmMapper alarmMapper;

    @Override
    public void createAlarm(AlarmEvent alarm) {
        alarm.setDeleted(0);
        alarm.setStatus("ACTIVE");
        alarm.setOccurrenceTime(LocalDateTime.now());
        alarmMapper.insert(alarm);
        log.info("Alarm created: {}", alarm.getAlarmCode());
    }

    @Override
    public List<AlarmEvent> getAllAlarms() {
        return alarmMapper.selectList(
            new LambdaQueryWrapper<AlarmEvent>()
                .eq(AlarmEvent::getDeleted, 0)
                .orderByDesc(AlarmEvent::getOccurrenceTime)
        );
    }

    @Override
    public List<AlarmEvent> getAlarmsByStatus(String status) {
        LambdaQueryWrapper<AlarmEvent> query = new LambdaQueryWrapper<>();
        query.eq(AlarmEvent::getStatus, status);
        return alarmMapper.selectList(query);
    }

    @Override
    public void acknowledgeAlarm(Long alarmId, String userId) {
        AlarmEvent alarm = alarmMapper.selectById(alarmId);
        if (alarm != null) {
            alarm.setStatus("ACKNOWLEDGED");
            alarm.setAckTime(LocalDateTime.now());
            alarm.setAckUser(userId);
            alarmMapper.updateById(alarm);
            log.info("Alarm acknowledged: {} by {}", alarmId, userId);
        }
    }

    @Override
    public void resolveAlarm(Long alarmId, String userId, String remarks) {
        AlarmEvent alarm = alarmMapper.selectById(alarmId);
        if (alarm != null) {
            alarm.setStatus("RESOLVED");
            alarm.setResolveTime(LocalDateTime.now());
            alarm.setResolveUser(userId);
            alarm.setRemarks(remarks);
            alarmMapper.updateById(alarm);
            log.info("Alarm resolved: {} by {}", alarmId, userId);
        }
    }

    @Override
    public void deleteAlarm(Long alarmId) {
        alarmMapper.deleteById(alarmId);
        log.info("Alarm deleted: {}", alarmId);
    }

    @Override
    public long getActiveAlarmCount() {
        LambdaQueryWrapper<AlarmEvent> query = new LambdaQueryWrapper<>();
        query.eq(AlarmEvent::getStatus, "ACTIVE");
        return alarmMapper.selectCount(query);
    }
}