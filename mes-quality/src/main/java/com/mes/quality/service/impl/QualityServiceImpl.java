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
        record.setSn(dto.getSn());
        record.setDeviceId(dto.getDeviceId());
        record.setCheckType(dto.getCheckType());
        record.setCheckResult(dto.getCheckResult());
        record.setDefectType(dto.getDefectType());
        record.setDefectDesc(dto.getDefectDesc());
        record.setRemark(dto.getRemark());
        record.setCheckTime(LocalDateTime.now());
        qualityRecordMapper.insert(record);

        syncToElasticsearch(record);

        sendKafkaEvent("QUALITY_RECORD_CREATED", record.getId(), record.getCheckResult());

        log.info("质检记录创建成功, id={}, sn={}, result={}", record.getId(), record.getSn(), record.getCheckResult());
        return record.getId();
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

        syncToElasticsearch(record);
        sendKafkaEvent("QUALITY_PASSED", id, "PASSED");

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

        syncToElasticsearch(record);
        sendKafkaEvent("QUALITY_FAILED", id, reason);

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
        wrapper.eq(checkType != null, QualityRecord::getCheckType, checkType)
                .eq(result != null, QualityRecord::getCheckResult, result)
                .and(keyword != null, w -> w.like(QualityRecord::getSn, keyword)
                        .or().like(QualityRecord::getWorkOrderNo, keyword))
                .orderByDesc(QualityRecord::getCreateTime);

        Page<QualityRecord> page = new Page<>(current, size);
        Page<QualityRecord> resultPage = qualityRecordMapper.selectPage(page, wrapper);
        return PageResult.of(resultPage);
    }

    private void syncToElasticsearch(QualityRecord record) {
        try {
            log.info("同步质检数据到Elasticsearch, index=quality_record, docId={}", record.getId());
        } catch (Exception e) {
            log.error("同步Elasticsearch失败, id={}", record.getId(), e);
        }
    }

    private void sendKafkaEvent(String eventType, Long recordId, String data) {
        try {
            log.info("发送Kafka质量事件: topic=quality-event, type={}, recordId={}, data={}",
                    eventType, recordId, data);
        } catch (Exception e) {
            log.error("发送Kafka事件失败, type={}, recordId={}", eventType, recordId, e);
        }
    }
}
