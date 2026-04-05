package com.mes.quality.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.quality.entity.QualityRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 质检记录Mapper接口
 * @author MES
 * @description 质检记录数据库操作
 */
@Mapper
public interface QualityRecordMapper extends BaseMapper<QualityRecord> {
}
