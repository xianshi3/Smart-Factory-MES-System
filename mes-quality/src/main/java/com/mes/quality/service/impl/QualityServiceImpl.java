package com.mes.quality.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mes.common.result.PageResult;
import com.mes.quality.dto.CreateQualityRecordDTO;
import com.mes.quality.entity.QualityRecord;
import com.mes.quality.entity.Traceability;
import com.mes.quality.mapper.QualityRecordMapper;
import com.mes.quality.mapper.TraceabilityMapper;
import com.mes.quality.service.QualityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 质量管理服务实现类
 * @author MES
 * @description 质检记录与追溯业务逻辑实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityServiceImpl implements QualityService {

    private final QualityRecordMapper qualityRecordMapper;
    private final TraceabilityMapper traceabilityMapper;

    /**
     * 创建质检记录
     * @param dto 创建质检记录DTO
     * @return 记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRecord(CreateQualityRecordDTO dto) {
        QualityRecord record = new QualityRecord();
        record.setWorkOrderId(dto.getWorkOrderId());
        record.setWorkOrderNo(dto.getWorkOrderNo());
        record.setSn(dto.getSn());
        record.setDeviceId(dto.getDeviceId());
        record.setWorkstationId(dto.getWorkstationId());
        record.setOperatorId(dto.getOperatorId());
        record.setCheckType(dto.getCheckType());
        record.setCheckResult(dto.getCheckResult());
        record.setDefectType(dto.getDefectType());
        record.setDefectDesc(dto.getDefectDesc());
        record.setRemark(dto.getRemark());
        record.setCheckTime(LocalDateTime.now());
        qualityRecordMapper.insert(record);

        log.info("质检记录创建成功, id={}, sn={}, result={}", record.getId(), record.getSn(), record.getCheckResult());
        return record.getId();
    }

    /**
     * 删除质检记录
     * @param id 记录ID
     * @param userId 操作人ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id, Long userId) {
        log.info("删除质检记录, id={}", id);
        QualityRecord record = qualityRecordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("质检记录不存在: " + id);
        }

        var updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<QualityRecord>()
                .set("deleted", 1)
                .set("deleted_time", LocalDateTime.now())
                .set("deleted_by", userId)
                .eq("id", id);
        qualityRecordMapper.update(null, updateWrapper);
        log.info("删除成功, id={}", id);
    }

    private Long getCurrentUserId() {
        return 1L;
    }

    /**
     * 质检通过
     * @param id 记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pass(Long id) {
        QualityRecord record = qualityRecordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("质检记录不存在: " + id);
        }
        record.setCheckResult("PASSED");
        record.setUpdateTime(LocalDateTime.now());
        qualityRecordMapper.updateById(record);

        log.info("质检通过, id={}", id);
    }

    /**
     * 质检不通过
     * @param id 记录ID
     * @param reason 不通过原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fail(Long id, String reason) {
        QualityRecord record = qualityRecordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("质检记录不存在: " + id);
        }
        record.setCheckResult("FAILED");
        record.setDefectDesc(reason);
        record.setUpdateTime(LocalDateTime.now());
        qualityRecordMapper.updateById(record);

        log.info("质检不通过, id={}, reason={}", id, reason);
    }

    /**
     * 查询质检详情
     * @param id 记录ID
     * @return 质检记录
     */
    @Override
    public QualityRecord getDetail(Long id) {
        return qualityRecordMapper.selectById(id);
    }

    /**
     * 正向追溯 - SN->工单->工艺->物料
     * @param sn 产品序列号
     * @return 追溯记录列表
     */
    @Override
    public List<Traceability> forwardTrace(String sn) {
        List<Traceability> traceList = traceabilityMapper.selectBySn(sn);
        log.info("正向追溯查询完成, sn={}, count={}", sn, traceList.size());
        return traceList;
    }

    /**
     * 反向追溯 - 工单->所有SN记录
     * @param workOrderId 工单ID
     * @return 追溯记录列表
     */
    @Override
    public List<Traceability> reverseTrace(Long workOrderId) {
        List<Traceability> traceList = traceabilityMapper.selectByWorkOrderId(workOrderId);
        log.info("反向追溯查询完成, workOrderId={}, count={}", workOrderId, traceList.size());
        return traceList;
    }

    /**
     * 分页查询质检记录
     * @param current 当前页
     * @param size 每页大小
     * @param checkType 检验类型
     * @param result 检验结果
     * @param keyword 关键字
     * @return 分页结果
     */
    @Override
    public PageResult<QualityRecord> queryPage(int current, int size, String checkType, String result, String keyword) {
        LambdaQueryWrapper<QualityRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(checkType != null && !checkType.isEmpty(), QualityRecord::getCheckType, checkType)
                .eq(result != null && !result.isEmpty(), QualityRecord::getCheckResult, result)
                .and(keyword != null && !keyword.isEmpty(), w -> w.like(QualityRecord::getSn, keyword)
                        .or().like(QualityRecord::getWorkOrderNo, keyword))
                .orderByDesc(QualityRecord::getCreateTime);

        Page<QualityRecord> page = new Page<>(current, size);
        Page<QualityRecord> resultPage = qualityRecordMapper.selectPage(page, wrapper);
        return PageResult.of(resultPage);
    }
}
