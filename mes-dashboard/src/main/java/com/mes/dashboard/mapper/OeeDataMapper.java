package com.mes.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.dashboard.entity.OeeData;
import org.apache.ibatis.annotations.Mapper;

/**
 * OEE数据Mapper接口
 * @author MES
 * @description OEE数据数据库操作
 */
@Mapper
public interface OeeDataMapper extends BaseMapper<OeeData> {
}
