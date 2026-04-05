package com.mes.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.dashboard.entity.DeviceStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备状态Mapper接口
 * @author MES
 * @description 设备状态数据库操作
 */
@Mapper
public interface DeviceStatusMapper extends BaseMapper<DeviceStatus> {
}
