package com.mes.dashboard.service;

import com.mes.common.entity.Bom;
import com.mes.common.entity.BomItem;
import com.mes.common.entity.Inventory;
import com.mes.common.entity.InventoryTransaction;
import com.mes.common.entity.Material;

import java.util.List;

public interface BomService {

    // Material
    Material createMaterial(Material material);

    Material updateMaterial(Material material);

    void deleteMaterial(Long id);

    Material getMaterial(Long id);

    com.mes.common.result.PageResult<Material> listMaterials(String keyword, String materialType, String status, int page, int size);

    // BOM
    Bom createBom(Bom bom);

    Bom updateBom(Bom bom);

    void deleteBom(Long id);

    Bom getBom(Long id);

    List<Bom> listBoms(String keyword, String status);

    // BOM Item
    BomItem createBomItem(BomItem bomItem);

    BomItem updateBomItem(BomItem bomItem);

    void deleteBomItem(Long id);

    List<BomItem> listBomItems(Long bomId);

    void validateBom(Long bomId);

    // Inventory
    List<Inventory> listInventory(Long materialId, String warehouse);

    Inventory adjustInventory(Long inventoryId, java.math.BigDecimal quantity, String remark);

    List<InventoryTransaction> listTransactions(Long materialId, String transactionType);
}
