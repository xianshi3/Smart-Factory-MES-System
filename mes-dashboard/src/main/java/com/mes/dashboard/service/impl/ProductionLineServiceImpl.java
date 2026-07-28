package com.mes.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.entity.ProductionLine;
import com.mes.common.mapper.ProductionLineMapper;
import com.mes.dashboard.service.ProductionLineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionLineServiceImpl implements ProductionLineService {

    private final ProductionLineMapper productionLineMapper;

    @Override
    public ProductionLine createProductionLine(ProductionLine line) {
        line.setDeleted(0);
        productionLineMapper.insert(line);
        log.info("ProductionLine created: {}", line.getLineCode());
        return line;
    }

    @Override
    public ProductionLine updateProductionLine(ProductionLine line) {
        productionLineMapper.updateById(line);
        log.info("ProductionLine updated: {}", line.getId());
        return line;
    }

    @Override
    public void deleteProductionLine(Long id) {
        productionLineMapper.deleteById(id);
        log.info("ProductionLine deleted: {}", id);
    }

    @Override
    public ProductionLine getProductionLine(Long id) {
        return productionLineMapper.selectById(id);
    }

    @Override
    public List<ProductionLine> listProductionLines() {
        return productionLineMapper.selectList(
            new LambdaQueryWrapper<ProductionLine>()
                .eq(ProductionLine::getDeleted, 0)
                .orderByAsc(ProductionLine::getCreateTime)
        );
    }
}
