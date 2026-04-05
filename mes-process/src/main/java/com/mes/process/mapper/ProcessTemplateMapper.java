package com.mes.process.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.process.entity.ProcessTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工艺模板Mapper接口
 * @author MES
 * @description 工艺模板数据库操作
 */
@Mapper
public interface ProcessTemplateMapper extends BaseMapper<ProcessTemplate> {
}
