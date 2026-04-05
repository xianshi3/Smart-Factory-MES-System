package com.mes.quality.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.quality.entity.Traceability;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 追溯记录Mapper接口
 * @author MES
 * @description 追溯记录数据库操作
 */
@Mapper
public interface TraceabilityMapper extends BaseMapper<Traceability> {

    /**
     * 根据SN查询追溯记录(正向追溯)
     * @param sn 产品序列号
     * @return 追溯记录列表
     */
    List<Traceability> selectBySn(@Param("sn") String sn);

    /**
     * 根据工单ID查询追溯记录(反向追溯)
     * @param workOrderId 工单ID
     * @return 追溯记录列表
     */
    List<Traceability> selectByWorkOrderId(@Param("workOrderId") Long workOrderId);
}
