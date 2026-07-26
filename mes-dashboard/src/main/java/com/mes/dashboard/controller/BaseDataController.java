package com.mes.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.entity.Workstation;
import com.mes.common.entity.ProductionLine;
import com.mes.common.mapper.WorkstationMapper;
import com.mes.common.mapper.ProductionLineMapper;
import com.mes.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 基础数据管理控制器
 * 负责生产线和工位的CRUD操作
 * @author MES
 * @since 2024
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "基础数据管理", description = "工位和生产线管理")
public class BaseDataController {

    private final WorkstationMapper workstationMapper;
    private final ProductionLineMapper productionLineMapper;

    /**
     * 获取生产线列表
     * @return 生产线列表
     */
    @GetMapping("/production-line/list")
    @Operation(summary = "获取生产线列表")
    public Result<List<ProductionLine>> listProductionLines() {
        List<ProductionLine> lines = productionLineMapper.selectList(
            new LambdaQueryWrapper<ProductionLine>()
                .orderByAsc(ProductionLine::getCreateTime)
        );
        return Result.ok(lines);
    }

    /**
     * 创建生产线
     * @param line 生产线信息
     * @return 操作结果
     */
    @PostMapping("/production-line")
    @Operation(summary = "创建生产线")
    public Result<Void> createProductionLine(@RequestBody ProductionLine line) {
        productionLineMapper.insert(line);
        return Result.ok();
    }

    /**
     * 更新生产线
     * @param line 生产线信息
     * @return 操作结果
     */
    @PutMapping("/production-line")
    @Operation(summary = "更新生产线")
    public Result<Void> updateProductionLine(@RequestBody ProductionLine line) {
        productionLineMapper.updateById(line);
        return Result.ok();
    }

    /**
     * 删除生产线
     * @param id 生产线ID
     * @return 操作结果
     */
    @DeleteMapping("/production-line/{id}")
    @Operation(summary = "删除生产线")
    public Result<Void> deleteProductionLine(@PathVariable Long id) {
        productionLineMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 获取工位列表
     * @return 工位列表
     */
    @GetMapping("/workstation/list")
    @Operation(summary = "获取工位列表")
    public Result<List<Workstation>> listWorkstations() {
        List<Workstation> stations = workstationMapper.selectList(
            new LambdaQueryWrapper<Workstation>()
                .orderByAsc(Workstation::getCreateTime)
        );
        return Result.ok(stations);
    }

    /**
     * 创建工位
     * @param station 工位信息
     * @return 操作结果
     */
    @PostMapping("/workstation")
    @Operation(summary = "创建工位")
    public Result<Void> createWorkstation(@RequestBody Workstation station) {
        workstationMapper.insert(station);
        return Result.ok();
    }

    /**
     * 更新工位
     * @param station 工位信息
     * @return 操作结果
     */
    @PutMapping("/workstation")
    @Operation(summary = "更新工位")
    public Result<Void> updateWorkstation(@RequestBody Workstation station) {
        workstationMapper.updateById(station);
        return Result.ok();
    }

    /**
     * 删除工位
     * @param id 工位ID
     * @return 操作结果
     */
    @DeleteMapping("/workstation/{id}")
    @Operation(summary = "删除工位")
    public Result<Void> deleteWorkstation(@PathVariable Long id) {
        workstationMapper.deleteById(id);
        return Result.ok();
    }
}