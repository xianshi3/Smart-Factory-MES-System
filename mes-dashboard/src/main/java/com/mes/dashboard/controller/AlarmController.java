package com.mes.dashboard.controller;

import com.mes.dashboard.entity.AlarmEvent;
import com.mes.dashboard.service.AlarmService;
import com.mes.common.result.Result;
import com.mes.common.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Alarm Management Controller
 */
@Tag(name = "Alarm Management", description = "Alarm event REST API")
@RestController
@RequestMapping("/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @Operation(summary = "Create alarm")
    @PostMapping
    @RequireRole({"ADMIN", "MANAGER"})
    public Result<Void> createAlarm(@RequestBody AlarmEvent alarm) {
        alarmService.createAlarm(alarm);
        return Result.ok();
    }

    @Operation(summary = "Get all alarms")
    @GetMapping
    public Result<List<AlarmEvent>> getAllAlarms() {
        return Result.ok(alarmService.getAllAlarms());
    }

    @Operation(summary = "Get alarms by status")
    @GetMapping("/status/{status}")
    public Result<List<AlarmEvent>> getAlarmsByStatus(@PathVariable String status) {
        return Result.ok(alarmService.getAlarmsByStatus(status));
    }

    @Operation(summary = "Acknowledge alarm")
    @PostMapping("/{alarmId}/ack")
    @RequireRole({"ADMIN", "MANAGER"})
    public Result<Void> acknowledgeAlarm(@PathVariable Long alarmId, @RequestParam String userId) {
        alarmService.acknowledgeAlarm(alarmId, userId);
        return Result.ok();
    }

    @Operation(summary = "Resolve alarm")
    @PostMapping("/{alarmId}/resolve")
    @RequireRole({"ADMIN", "MANAGER"})
    public Result<Void> resolveAlarm(@PathVariable Long alarmId, @RequestParam String userId, @RequestParam String remarks) {
        alarmService.resolveAlarm(alarmId, userId, remarks);
        return Result.ok();
    }

    @Operation(summary = "Delete alarm")
    @DeleteMapping("/{alarmId}")
    @RequireRole({"ADMIN", "MANAGER"})
    public Result<Void> deleteAlarm(@PathVariable Long alarmId) {
        alarmService.deleteAlarm(alarmId);
        return Result.ok();
    }

    @Operation(summary = "Get active alarm count")
    @GetMapping("/count/active")
    public Result<Long> getActiveAlarmCount() {
        return Result.ok(alarmService.getActiveAlarmCount());
    }
}