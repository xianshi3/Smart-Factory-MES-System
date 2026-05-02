package com.mes.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.dashboard.entity.AlarmEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * Alarm Event Mapper Interface
 */
@Mapper
public interface AlarmMapper extends BaseMapper<AlarmEvent> {
}