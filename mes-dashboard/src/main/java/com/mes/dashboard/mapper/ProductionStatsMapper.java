package com.mes.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.dashboard.entity.ProductionStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生产统计Mapper接口
 * @author MES
 * @description 生产统计数据操作
 */
@Mapper
public interface ProductionStatsMapper extends BaseMapper<ProductionStats> {
}