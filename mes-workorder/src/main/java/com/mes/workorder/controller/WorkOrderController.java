package com.mes.workorder.controller;

import com.mes.common.result.PageResult;
import com.mes.common.result.Result;
import com.mes.workorder.dto.CreateWorkOrderDTO;
import com.mes.workorder.dto.SubmitReportDTO;
import com.mes.workorder.dto.UpdateWorkOrderDTO;
import com.mes.workorder.entity.WorkOrder;
import com.mes.workorder.entity.WorkReport;
import com.mes.workorder.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/workorder")
@RequiredArgsConstructor
@Tag(name = "工单管理", description = "工单CRUD、状态流转、报工接口")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping
    @Operation(summary = "创建工单")
    public Result<WorkOrder> create(@RequestBody CreateWorkOrderDTO dto,
                                    @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            log.info("收到创建工单请求: productName={}, productModel={}, planQuantity={}, workstationId={}, processTemplateId={}",
                    dto.getProductName(), dto.getProductModel(), dto.getPlanQuantity(), dto.getWorkstationId(), dto.getProcessTemplateId());
            return Result.ok(workOrderService.create(dto, userId));
        } catch (Exception e) {
            log.error("创建工单失败", e);
            return Result.fail("创建失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询工单详情")
    public Result<WorkOrder> detail(@PathVariable Long id) {
        return Result.ok(workOrderService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工单")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateWorkOrderDTO dto) {
        workOrderService.updateStatus(id, dto);
        return Result.ok();
    }

    @PostMapping("/{id}/issue")
    @Operation(summary = "下发工单")
    public Result<Void> issue(@PathVariable Long id) {
        workOrderService.issue(id);
        return Result.ok();
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "开始生产")
    public Result<Void> start(@PathVariable Long id) {
        workOrderService.startProduction(id);
        return Result.ok();
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成工单")
    public Result<Void> complete(@PathVariable Long id) {
        workOrderService.complete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "关闭工单")
    public Result<Void> close(@PathVariable Long id) {
        workOrderService.close(id);
        return Result.ok();
    }

    @PostMapping("/report")
    @Operation(summary = "提交报工")
    public Result<WorkReport> submitReport(@Valid @RequestBody SubmitReportDTO dto,
                                           @RequestHeader(value = "X-User-Id", required = false) Long operatorId) {
        return Result.ok(workOrderService.submitReport(dto, operatorId));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询工单")
    public Result<PageResult<WorkOrder>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(workOrderService.queryPage(current, size, status, keyword));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工单")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return workOrderService.delete(id, userId);
    }
}
