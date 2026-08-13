package com.mes.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.workorder.entity.WorkCalendarDay;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkCalendarMapper extends BaseMapper<WorkCalendarDay> {
}
