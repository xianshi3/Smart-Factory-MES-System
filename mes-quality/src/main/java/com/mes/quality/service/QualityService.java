package com.mes.quality.service;

import com.mes.common.result.PageResult;
import com.mes.quality.dto.CreateQualityRecordDTO;
import com.mes.quality.entity.QualityRecord;
import com.mes.quality.entity.Traceability;

import java.util.List;

/**
 * 质量管理服务接口
 * @author MES
 * @description 质检记录与追溯相关业务逻辑
 */
public interface QualityService {

    /**
     * 创建质检记录
     * @param dto 创建质检记录DTO
     * @return 记录ID
     */
    Long createRecord(CreateQualityRecordDTO dto);

    /**
     * 删除质检记录
     * @param id 记录ID
     * @param userId 操作人ID
     */
    void deleteRecord(Long id, Long userId);

    /**
     * 质检通过
     * @param id 记录ID
     */
    void pass(Long id);

    /**
     * 质检不通过
     * @param id 记录ID
     * @param reason 不通过原因
     */
    void fail(Long id, String reason);

    /**
     * 查询质检详情
     * @param id 记录ID
     * @return 质检记录
     */
    QualityRecord getDetail(Long id);

    /**
     * 正向追溯 - SN->工单->工艺->物料
     * @param sn 产品序列号
     * @return 追溯记录列表
     */
    List<Traceability> forwardTrace(String sn);

    /**
     * 反向追溯 - 工单->所有SN记录
     * @param workOrderId 工单ID
     * @return 追溯记录列表
     */
    List<Traceability> reverseTrace(Long workOrderId);

    /**
     * 分页查询质检记录
     * @param current 当前页
     * @param size 每页大小
     * @param checkType 检验类型
     * @param result 检验结果
     * @param keyword 关键字
     * @return 分页结果
     */
    PageResult<QualityRecord> queryPage(int current, int size, String checkType, String result, String keyword);
}
