package com.mes.dashboard.service;

import com.mes.common.entity.ProductionLine;

import java.util.List;

public interface ProductionLineService {

    ProductionLine createProductionLine(ProductionLine line);

    ProductionLine updateProductionLine(ProductionLine line);

    void deleteProductionLine(Long id);

    ProductionLine getProductionLine(Long id);

    List<ProductionLine> listProductionLines();
}
