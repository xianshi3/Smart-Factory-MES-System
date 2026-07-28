package com.mes.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.mes.common.entity.Bom;
import com.mes.common.entity.BomItem;
import com.mes.common.entity.Inventory;
import com.mes.common.entity.InventoryTransaction;
import com.mes.common.entity.Material;
import com.mes.dashboard.mapper.BomItemMapper;
import com.mes.dashboard.mapper.BomMapper;
import com.mes.dashboard.mapper.InventoryMapper;
import com.mes.dashboard.mapper.InventoryTransactionMapper;
import com.mes.dashboard.mapper.MaterialMapper;
import com.mes.dashboard.service.BomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mes.common.result.PageResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class BomServiceImpl implements BomService {

    private final MaterialMapper materialMapper;
    private final BomMapper bomMapper;
    private final BomItemMapper bomItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;

    private static final AtomicLong txCounter = new AtomicLong(0);

    // ==================== Material ====================

    @Override
    public Material createMaterial(Material material) {
        material.setDeleted(0);
        materialMapper.insert(material);
        log.info("Material created: {}", material.getMaterialCode());
        return material;
    }

    @Override
    public Material updateMaterial(Material material) {
        materialMapper.updateById(material);
        log.info("Material updated: {}", material.getId());
        return material;
    }

    @Override
    public void deleteMaterial(Long id) {
        materialMapper.deleteById(id);
        log.info("Material deleted: {}", id);
    }

    @Override
    public Material getMaterial(Long id) {
        return materialMapper.selectById(id);
    }

    @Override
    public PageResult<Material> listMaterials(String keyword, String materialType, String status, int page, int size) {
        Page<Material> p = new Page<>(page, size);
        LambdaQueryWrapper<Material> query = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            query.and(w -> w.like(Material::getMaterialCode, keyword)
                    .or()
                    .like(Material::getMaterialName, keyword));
        }
        if (StringUtils.isNotBlank(materialType)) {
            query.eq(Material::getMaterialType, materialType);
        }
        if (StringUtils.isNotBlank(status)) {
            query.eq(Material::getStatus, status);
        }
        query.eq(Material::getDeleted, 0);
        query.orderByDesc(Material::getCreateTime);
        Page<Material> result = materialMapper.selectPage(p, query);
        return PageResult.of(result);
    }

    // ==================== BOM ====================

    @Override
    @Transactional
    public Bom createBom(Bom bom) {
        bom.setDeleted(0);
        if (StringUtils.isBlank(bom.getBomCode())) {
            bom.setBomCode(generateBomCode());
        }
        try {
            bomMapper.insert(bom);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            bom.setBomCode(generateBomCode());
            bomMapper.insert(bom);
        }
        log.info("BOM created: {}", bom.getBomCode());
        return bom;
    }

    private String generateBomCode() {
        String prefix = "BOM-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        LambdaQueryWrapper<Bom> wrapper = new LambdaQueryWrapper<Bom>()
            .like(Bom::getBomCode, prefix)
            .orderByDesc(Bom::getCreateTime)
            .last("LIMIT 1");
        Bom last = bomMapper.selectOne(wrapper);
        int next = 1;
        if (last != null && last.getBomCode() != null) {
            String suffix = last.getBomCode().substring(prefix.length());
            try { next = Integer.parseInt(suffix) + 1; } catch (NumberFormatException ignored) {}
        }
        return prefix + String.format("%04d", next);
    }

    @Override
    public Bom updateBom(Bom bom) {
        bomMapper.updateById(bom);
        log.info("BOM updated: {}", bom.getId());
        return bom;
    }

    @Override
    public void deleteBom(Long id) {
        bomMapper.deleteById(id);
        bomItemMapper.delete(new LambdaQueryWrapper<BomItem>().eq(BomItem::getBomId, id));
        log.info("BOM deleted: {}", id);
    }

    @Override
    public Bom getBom(Long id) {
        return bomMapper.selectById(id);
    }

    @Override
    public List<Bom> listBoms(String keyword, String status) {
        LambdaQueryWrapper<Bom> query = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            query.and(w -> w.like(Bom::getBomCode, keyword)
                    .or()
                    .like(Bom::getBomName, keyword));
        }
        if (StringUtils.isNotBlank(status)) {
            query.eq(Bom::getStatus, status);
        }
        query.eq(Bom::getDeleted, 0);
        query.orderByDesc(Bom::getCreateTime);
        return bomMapper.selectList(query);
    }

    // ==================== BOM Item ====================

    @Override
    public BomItem createBomItem(BomItem bomItem) {
        bomItem.setDeleted(0);
        bomItemMapper.insert(bomItem);
        log.info("BOM item created: bomId={}, materialId={}", bomItem.getBomId(), bomItem.getMaterialId());
        return bomItem;
    }

    @Override
    public BomItem updateBomItem(BomItem bomItem) {
        bomItemMapper.updateById(bomItem);
        return bomItem;
    }

    @Override
    public void deleteBomItem(Long id) {
        bomItemMapper.deleteById(id);
        log.info("BOM item deleted: {}", id);
    }

    @Override
    public List<BomItem> listBomItems(Long bomId) {
        LambdaQueryWrapper<BomItem> query = new LambdaQueryWrapper<>();
        query.eq(BomItem::getBomId, bomId);
        query.eq(BomItem::getDeleted, 0);
        query.orderByAsc(BomItem::getSequence);
        return bomItemMapper.selectList(query);
    }

    @Override
    @Transactional
    public void validateBom(Long bomId) {
        Bom bom = bomMapper.selectById(bomId);
        if (bom == null) {
            throw new RuntimeException("BOM not found: " + bomId);
        }

        List<BomItem> items = listBomItems(bomId);
        if (items.isEmpty()) {
            throw new RuntimeException("BOM has no items: " + bom.getBomCode());
        }

        for (BomItem item : items) {
            Material material = materialMapper.selectById(item.getMaterialId());
            if (material == null) {
                throw new RuntimeException("Material not found: id=" + item.getMaterialId()
                        + " in BOM: " + bom.getBomCode());
            }

            if (item.getMaterialId().equals(bom.getProductId())) {
                throw new RuntimeException("Circular reference: material id=" + item.getMaterialId()
                        + " is the same as product in BOM: " + bom.getBomCode());
            }
        }

        bom.setStatus("VALIDATED");
        bomMapper.updateById(bom);
        log.info("BOM validated: {}", bom.getBomCode());
    }

    // ==================== Inventory ====================

    @Override
    public List<Inventory> listInventory(Long materialId, String warehouse) {
        LambdaQueryWrapper<Inventory> query = new LambdaQueryWrapper<>();
        if (materialId != null) {
            query.eq(Inventory::getMaterialId, materialId);
        }
        if (StringUtils.isNotBlank(warehouse)) {
            query.eq(Inventory::getWarehouse, warehouse);
        }
        query.eq(Inventory::getDeleted, 0);
        query.orderByDesc(Inventory::getCreateTime);
        return inventoryMapper.selectList(query);
    }

    @Override
    @Transactional
    public Inventory adjustInventory(Long inventoryId, BigDecimal quantity, String remark) {
        Inventory inv = inventoryMapper.selectById(inventoryId);
        if (inv == null) {
            throw new RuntimeException("Inventory not found: " + inventoryId);
        }

        BigDecimal oldQuantity = inv.getQuantity();
        BigDecimal newQuantity = oldQuantity.add(quantity);
        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Insufficient inventory: current=" + oldQuantity + ", adjust=" + quantity);
        }

        inv.setQuantity(newQuantity);
        BigDecimal oldAvailable = inv.getAvailableQuantity();
        inv.setAvailableQuantity(oldAvailable.add(quantity));
        inv.setLastTransactionTime(LocalDateTime.now());
        inventoryMapper.updateById(inv);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setTransactionNo(generateTransactionNo());
        tx.setMaterialId(inv.getMaterialId());
        tx.setTransactionType(quantity.compareTo(BigDecimal.ZERO) >= 0 ? "IN" : "OUT");
        tx.setQuantity(quantity);
        tx.setBalanceAfter(newQuantity);
        tx.setBatchNo(inv.getBatchNo());
        tx.setReferenceType("ADJUST");
        tx.setReferenceId(inventoryId);
        tx.setRemark(remark);
        tx.setCreateBy("SYSTEM");
        tx.setDeleted(0);
        inventoryTransactionMapper.insert(tx);

        log.info("Inventory adjusted: id={}, old={}, new={}, delta={}", inventoryId, oldQuantity, newQuantity, quantity);
        return inv;
    }

    @Override
    public List<InventoryTransaction> listTransactions(Long materialId, String transactionType) {
        LambdaQueryWrapper<InventoryTransaction> query = new LambdaQueryWrapper<>();
        if (materialId != null) {
            query.eq(InventoryTransaction::getMaterialId, materialId);
        }
        if (StringUtils.isNotBlank(transactionType)) {
            query.eq(InventoryTransaction::getTransactionType, transactionType);
        }
        query.eq(InventoryTransaction::getDeleted, 0);
        query.orderByDesc(InventoryTransaction::getCreateTime);
        return inventoryTransactionMapper.selectList(query);
    }

    private String generateTransactionNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = txCounter.incrementAndGet() % 999999;
        return "TX" + datePart + String.format("%06d", seq);
    }
}
