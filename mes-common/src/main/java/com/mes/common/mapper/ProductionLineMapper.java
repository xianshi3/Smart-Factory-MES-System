package com.mes.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.common.entity.ProductionLine;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生产线 Mapper 接口
 * @author MES
 * @since 2024
 */
@Mapper
public interface ProductionLineMapper extends BaseMapper<ProductionLine> {
}