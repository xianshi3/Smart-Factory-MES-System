package com.mes.dashboard.controller;

import com.mes.common.entity.Inventory;
import com.mes.common.entity.InventoryTransaction;
import com.mes.common.result.Result;
import com.mes.common.security.RequireRole;
import com.mes.dashboard.service.BomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard/inventory")
@RequiredArgsConstructor
@Tag(name = "库存管理", description = "库存查询与调整")
public class InventoryController {

    private final BomService bomService;

    @GetMapping("/list")
    @Operation(summary = "获取库存列表")
    public Result<List<Inventory>> list(@RequestParam(required = false) Long materialId,
                                        @RequestParam(required = false) String warehouse) {
        return Result.ok(bomService.listInventory(materialId, warehouse));
    }

    @PostMapping("/adjust")
    @Operation(summary = "调整库存")
    @RequireRole({"ADMIN", "MANAGER"})
    public Result<Inventory> adjust(@RequestBody Map<String, Object> params) {
        Object inventoryIdValue = params.get("inventoryId");
        Object quantityValue = params.get("quantity");
        if (inventoryIdValue == null || quantityValue == null) {
            return Result.fail(400, "参数缺失: inventoryId / quantity 不能为空");
        }
        Long inventoryId = Long.valueOf(inventoryIdValue.toString());
        BigDecimal quantity = new BigDecimal(quantityValue.toString());
        String remark = (String) params.getOrDefault("remark", "");
        return Result.ok(bomService.adjustInventory(inventoryId, quantity, remark));
    }

    @GetMapping("/transaction/list")
    @Operation(summary = "获取库存交易记录")
    public Result<List<InventoryTransaction>> listTransactions(
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String transactionType) {
        return Result.ok(bomService.listTransactions(materialId, transactionType));
    }
}
