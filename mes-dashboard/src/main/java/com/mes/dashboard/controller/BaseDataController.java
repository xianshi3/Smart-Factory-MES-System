package com.mes.dashboard.controller;

import com.mes.common.entity.Workstation;
import com.mes.common.entity.ProductionLine;
import com.mes.common.result.Result;
import com.mes.common.security.RequireRole;
import com.mes.dashboard.service.ProductionLineService;
import com.mes.dashboard.service.WorkstationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "基础数据管理", description = "工位和生产线管理")
public class BaseDataController {

    private final ProductionLineService productionLineService;
    private final WorkstationService workstationService;

    @GetMapping("/production-line/list")
    @Operation(summary = "获取生产线列表")
    public Result<List<ProductionLine>> listProductionLines() {
        return Result.ok(productionLineService.listProductionLines());
    }

    @GetMapping("/production-line/{id}")
    @Operation(summary = "获取生产线详情")
    public Result<ProductionLine> getProductionLine(@PathVariable Long id) {
        return Result.ok(productionLineService.getProductionLine(id));
    }

    @PostMapping("/production-line")
    @Operation(summary = "创建生产线")
    @RequireRole({"ADMIN", "MANAGER"})
    public Result<ProductionLine> createProductionLine(@RequestBody ProductionLine line) {
        return Result.ok(productionLineService.createProductionLine(line));
    }

    @PutMapping("/production-line")
    @Operation(summary = "更新生产线")
    @RequireRole({"ADMIN", "MANAGER"})
    public Result<ProductionLine> updateProductionLine(@RequestBody ProductionLine line) {
        return Result.ok(productionLineService.updateProductionLine(line));
    }

    @DeleteMapping("/production-line/{id}")
    @Operation(summary = "删除生产线")
    @RequireRole({"ADMIN", "MANAGER"})
    public Result<Void> deleteProductionLine(@PathVariable Long id) {
        productionLineService.deleteProductionLine(id);
        return Result.ok();
    }

    @GetMapping("/workstation/list")
    @Operation(summary = "获取工位列表")
    public Result<List<Workstation>> listWorkstations() {
        return Result.ok(workstationService.listWorkstations());
    }

    @GetMapping("/workstation/{id}")
    @Operation(summary = "获取工位详情")
    public Result<Workstation> getWorkstation(@PathVariable Long id) {
        return Result.ok(workstationService.getWorkstation(id));
    }

    @PostMapping("/workstation")
    @Operation(summary = "创建工位")
    @RequireRole({"ADMIN", "MANAGER"})
    public Result<Workstation> createWorkstation(@RequestBody Workstation station) {
        return Result.ok(workstationService.createWorkstation(station));
    }

    @PutMapping("/workstation")
    @Operation(summary = "更新工位")
    @RequireRole({"ADMIN", "MANAGER"})
    public Result<Workstation> updateWorkstation(@RequestBody Workstation station) {
        return Result.ok(workstationService.updateWorkstation(station));
    }

    @DeleteMapping("/workstation/{id}")
    @Operation(summary = "删除工位")
    @RequireRole({"ADMIN", "MANAGER"})
    public Result<Void> deleteWorkstation(@PathVariable Long id) {
        workstationService.deleteWorkstation(id);
        return Result.ok();
    }
}
