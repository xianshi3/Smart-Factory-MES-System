package com.mes.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.common.entity.Workstation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工位 Mapper 接口
 * @author MES
 * @since 2024
 */
@Mapper
public interface WorkstationMapper extends BaseMapper<Workstation> {
}