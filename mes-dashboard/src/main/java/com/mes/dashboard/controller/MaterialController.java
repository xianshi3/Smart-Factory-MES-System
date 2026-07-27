package com.mes.dashboard.controller;

import com.mes.common.entity.Material;
import com.mes.common.result.Result;
import com.mes.dashboard.service.BomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mes.common.result.PageResult;

@RestController
@RequestMapping("/dashboard/material")
@RequiredArgsConstructor
@Tag(name = "物料管理", description = "物料基础信息CRUD")
public class MaterialController {

    private final BomService bomService;

    @GetMapping("/list")
    @Operation(summary = "获取物料列表")
    public Result<PageResult<Material>> list(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String materialType,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return Result.ok(bomService.listMaterials(keyword, materialType, status, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取物料详情")
    public Result<Material> get(@PathVariable Long id) {
        return Result.ok(bomService.getMaterial(id));
    }

    @PostMapping
    @Operation(summary = "创建物料")
    public Result<Material> create(@RequestBody Material material) {
        return Result.ok(bomService.createMaterial(material));
    }

    @PutMapping
    @Operation(summary = "更新物料")
    public Result<Material> update(@RequestBody Material material) {
        return Result.ok(bomService.updateMaterial(material));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除物料")
    public Result<Void> delete(@PathVariable Long id) {
        bomService.deleteMaterial(id);
        return Result.ok();
    }
}
