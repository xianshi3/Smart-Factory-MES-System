package com.mes.workorder.controller;

import com.mes.common.result.Result;
import com.mes.common.security.RequirePermission;
import com.mes.workorder.dto.planning.PlanningAutoPlanDTO;
import com.mes.workorder.dto.planning.PlanningBoardVO;
import com.mes.workorder.dto.planning.PlanningFreezeRequestDTO;
import com.mes.workorder.dto.planning.PlanningMoveRequestDTO;
import com.mes.workorder.dto.planning.PlanningSaveOrderDTO;
import com.mes.workorder.service.PlanningBoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 生产调度看板（排产看板）接口
 */
@Slf4j
@RestController
@RequestMapping("/workorder/planning")
@RequiredArgsConstructor
@Tag(name = "排产看板", description = "生产调度看板：工序级甘特图、自动排程、拖拽排产、冲突检测、冻结下发")
public class PlanningBoardController {

    private final PlanningBoardService planningBoardService;

    @GetMapping("/board")
    @Operation(summary = "查询排产看板数据")
    public Result<PlanningBoardVO> board(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime windowStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime windowEnd) {
        try {
            return Result.ok(planningBoardService.getBoard(windowStart, windowEnd));
        } catch (Exception e) {
            log.error("查询排产看板失败", e);
            return Result.fail("查询排产看板失败: " + e.getMessage());
        }
    }

    @PostMapping("/save-order")
    @Operation(summary = "保存拖拽后的排产顺序（整单级）")
    @RequirePermission("planning:edit")
    public Result<Void> saveOrder(@Valid @RequestBody PlanningSaveOrderDTO dto) {
        try {
            planningBoardService.saveOrder(dto);
            return Result.ok();
        } catch (Exception e) {
            log.error("保存排产顺序失败", e);
            return Result.fail("保存排产顺序失败: " + e.getMessage());
        }
    }

    @PostMapping("/move")
    @Operation(summary = "拖拽调整（工序换设备/改时间/拉伸）")
    @RequirePermission("planning:edit")
    public Result<Void> move(@Valid @RequestBody PlanningMoveRequestDTO dto) {
        try {
            planningBoardService.move(dto);
            return Result.ok();
        } catch (Exception e) {
            log.error("拖拽调整失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/unassign")
    @Operation(summary = "拖回待排产池（取消排产）")
    @RequirePermission("planning:edit")
    public Result<Void> unassign(@RequestParam Long workOrderId) {
        try {
            planningBoardService.unassign(workOrderId);
            return Result.ok();
        } catch (Exception e) {
            log.error("取消排产失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/auto-plan")
    @Operation(summary = "自动排程（APS：优先级+交期+工序拆分+负载均衡+工作日历）")
    @RequirePermission("planning:edit")
    public Result<Integer> autoPlan(@RequestBody(required = false) PlanningAutoPlanDTO dto) {
        try {
            if (dto == null) {
                dto = new PlanningAutoPlanDTO();
            }
            int count = planningBoardService.autoPlan(dto);
            return Result.ok(count);
        } catch (Exception e) {
            log.error("自动排程失败", e);
            return Result.fail("自动排程失败: " + e.getMessage());
        }
    }

    @PostMapping("/undo")
    @Operation(summary = "撤销上一次排产变更")
    @RequirePermission("planning:edit")
    public Result<Void> undo() {
        try {
            planningBoardService.undo();
            return Result.ok();
        } catch (Exception e) {
            log.error("撤销失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/freeze")
    @Operation(summary = "冻结排产（设备行/工单/指定工序）")
    @RequirePermission("planning:edit")
    public Result<Integer> freeze(@RequestBody PlanningFreezeRequestDTO dto) {
        try {
            return Result.ok(planningBoardService.freeze(dto));
        } catch (Exception e) {
            log.error("冻结排产失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/unfreeze")
    @Operation(summary = "解除冻结")
    @RequirePermission("planning:edit")
    public Result<Integer> unfreeze(@RequestBody PlanningFreezeRequestDTO dto) {
        try {
            return Result.ok(planningBoardService.unfreeze(dto));
        } catch (Exception e) {
            log.error("解除冻结失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/release")
    @Operation(summary = "下发排产（标记RELEASED）")
    @RequirePermission("planning:edit")
    public Result<Integer> release(@RequestParam Long workOrderId) {
        try {
            return Result.ok(planningBoardService.release(workOrderId));
        } catch (Exception e) {
            log.error("下发排产失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/logs")
    @Operation(summary = "清空变更日志（仅审计记录，不影响排产与撤销）")
    @RequirePermission("planning:edit")
    public Result<Void> clearLogs() {
        try {
            planningBoardService.clearLogs();
            return Result.ok();
        } catch (Exception e) {
            log.error("清空变更日志失败", e);
            return Result.fail("清空变更日志失败: " + e.getMessage());
        }
    }
}
