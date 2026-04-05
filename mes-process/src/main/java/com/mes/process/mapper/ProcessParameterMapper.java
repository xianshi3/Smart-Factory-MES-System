package com.mes.process.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.process.entity.ProcessParameter;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工艺参数Mapper接口
 * @author MES
 * @description 工艺参数数据库操作
 */
@Mapper
public interface ProcessParameterMapper extends BaseMapper<ProcessParameter> {
}
