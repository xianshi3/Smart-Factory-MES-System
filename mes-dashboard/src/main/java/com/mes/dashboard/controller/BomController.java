package com.mes.dashboard.controller;

import com.mes.common.entity.Bom;
import com.mes.common.entity.BomItem;
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

import java.util.List;

@RestController
@RequestMapping("/dashboard/bom")
@RequiredArgsConstructor
@Tag(name = "BOM管理", description = "物料清单（BOM）及BOM行项管理")
public class BomController {

    private final BomService bomService;

    @GetMapping("/list")
    @Operation(summary = "获取BOM列表")
    public Result<List<Bom>> list(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String status) {
        return Result.ok(bomService.listBoms(keyword, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取BOM详情")
    public Result<Bom> get(@PathVariable Long id) {
        return Result.ok(bomService.getBom(id));
    }

    @PostMapping
    @Operation(summary = "创建BOM")
    public Result<Bom> create(@RequestBody Bom bom) {
        return Result.ok(bomService.createBom(bom));
    }

    @PutMapping
    @Operation(summary = "更新BOM")
    public Result<Bom> update(@RequestBody Bom bom) {
        return Result.ok(bomService.updateBom(bom));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除BOM")
    public Result<Void> delete(@PathVariable Long id) {
        bomService.deleteBom(id);
        return Result.ok();
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "验证BOM")
    public Result<Void> validate(@PathVariable Long id) {
        bomService.validateBom(id);
        return Result.ok();
    }

    @GetMapping("/{bomId}/item/list")
    @Operation(summary = "获取BOM行项列表")
    public Result<List<BomItem>> listItems(@PathVariable Long bomId) {
        return Result.ok(bomService.listBomItems(bomId));
    }

    @PostMapping("/{bomId}/item")
    @Operation(summary = "添加BOM行项")
    public Result<BomItem> createItem(@RequestBody BomItem bomItem) {
        return Result.ok(bomService.createBomItem(bomItem));
    }

    @PutMapping("/{bomId}/item")
    @Operation(summary = "更新BOM行项")
    public Result<BomItem> updateItem(@RequestBody BomItem bomItem) {
        return Result.ok(bomService.updateBomItem(bomItem));
    }

    @DeleteMapping("/{bomId}/item/{id}")
    @Operation(summary = "删除BOM行项")
    public Result<Void> deleteItem(@PathVariable Long id) {
        bomService.deleteBomItem(id);
        return Result.ok();
    }
}
