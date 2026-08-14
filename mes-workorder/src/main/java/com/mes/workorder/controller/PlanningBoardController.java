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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 生产调度看板（排产看板）接口
 *
 * 异常统一由 GlobalExceptionHandler 处理：
 * - IllegalStateException（排产冲突/冻结拦截等业务状态）→ 400 + 业务消息透传
 * - BizException → 400；其余异常 → 500 通用消息（不泄露内部细节）
 */
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
        return Result.ok(planningBoardService.getBoard(windowStart, windowEnd));
    }

    @PostMapping("/save-order")
    @Operation(summary = "保存拖拽后的排产顺序（整单级）")
    @RequirePermission("planning:edit")
    public Result<Void> saveOrder(@Valid @RequestBody PlanningSaveOrderDTO dto) {
        planningBoardService.saveOrder(dto);
        return Result.ok();
    }

    @PostMapping("/move")
    @Operation(summary = "拖拽调整（工序换设备/改时间/拉伸）")
    @RequirePermission("planning:edit")
    public Result<Void> move(@Valid @RequestBody PlanningMoveRequestDTO dto) {
        planningBoardService.move(dto);
        return Result.ok();
    }

    @PostMapping("/unassign")
    @Operation(summary = "拖回待排产池（取消排产）")
    @RequirePermission("planning:edit")
    public Result<Void> unassign(@RequestParam Long workOrderId) {
        planningBoardService.unassign(workOrderId);
        return Result.ok();
    }

    @PostMapping("/auto-plan")
    @Operation(summary = "自动排程（APS：优先级+交期+工序拆分+负载均衡+工作日历）")
    @RequirePermission("planning:edit")
    public Result<Integer> autoPlan(@RequestBody(required = false) PlanningAutoPlanDTO dto) {
        if (dto == null) {
            dto = new PlanningAutoPlanDTO();
        }
        return Result.ok(planningBoardService.autoPlan(dto));
    }

    @PostMapping("/undo")
    @Operation(summary = "撤销上一次排产变更")
    @RequirePermission("planning:edit")
    public Result<Void> undo() {
        planningBoardService.undo();
        return Result.ok();
    }

    @PostMapping("/freeze")
    @Operation(summary = "冻结排产（设备行/工单/指定工序）")
    @RequirePermission("planning:edit")
    public Result<Integer> freeze(@RequestBody PlanningFreezeRequestDTO dto) {
        return Result.ok(planningBoardService.freeze(dto));
    }

    @PostMapping("/unfreeze")
    @Operation(summary = "解除冻结")
    @RequirePermission("planning:edit")
    public Result<Integer> unfreeze(@RequestBody PlanningFreezeRequestDTO dto) {
        return Result.ok(planningBoardService.unfreeze(dto));
    }

    @PostMapping("/release")
    @Operation(summary = "下发排产（标记RELEASED）")
    @RequirePermission("planning:edit")
    public Result<Integer> release(@RequestParam Long workOrderId) {
        return Result.ok(planningBoardService.release(workOrderId));
    }

    @DeleteMapping("/logs")
    @Operation(summary = "清空变更日志（仅审计记录，不影响排产与撤销）")
    @RequirePermission("planning:edit")
    public Result<Void> clearLogs() {
        planningBoardService.clearLogs();
        return Result.ok();
    }
}
