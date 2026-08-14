package com.mes.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.exception.BizException;
import com.mes.common.exception.ErrorCode;
import com.mes.common.result.PageResult;
import com.mes.common.result.Result;
import com.mes.workorder.dto.CreateWorkOrderDTO;
import com.mes.workorder.dto.SubmitReportDTO;
import com.mes.workorder.dto.UpdateWorkOrderDTO;
import com.mes.workorder.entity.WorkOrder;
import com.mes.workorder.entity.WorkReport;
import com.mes.workorder.enums.WorkOrderStatusEnum;
import com.mes.workorder.mapper.WorkReportMapper;
import com.mes.workorder.service.WorkOrderService;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mes.workorder.mapper.WorkOrderMapper;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder> implements WorkOrderService {

    private final WorkReportMapper workReportMapper;
    private final WorkOrderMapper workOrderMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    private static final String ORDER_SEQ_KEY = "wo:seq";
    private static final java.time.format.DateTimeFormatter SEQ_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder create(CreateWorkOrderDTO dto) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setDeleted(0);
        workOrder.setOrderNo(generateOrderNo());
        workOrder.setProductName(dto.getProductName());
        workOrder.setProductModel(dto.getProductModel());
        workOrder.setPlanQuantity(dto.getPlanQuantity());
        workOrder.setCompletedQuantity(0);
        workOrder.setStatus(WorkOrderStatusEnum.CREATED.getCode());
        workOrder.setWorkstationId(dto.getWorkstationId());
        workOrder.setProcessTemplateId(dto.getProcessTemplateId());
        workOrder.setPriority(dto.getPriority() != null ? dto.getPriority() : "MEDIUM");
        workOrder.setPlannedStartTime(dto.getPlannedStartTime());
        workOrder.setPlannedEndTime(dto.getPlannedEndTime());
        workOrder.setRemark(dto.getRemark());
        workOrder.setCreateBy(getCurrentUserId());
        this.save(workOrder);
        return workOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issue(Long id) {
        WorkOrder wo = getByIdOrThrow(id);
        transitionStatus(wo, WorkOrderStatusEnum.ISSUED);
        wo.setIssueBy(getCurrentUserId());
        
        var updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<WorkOrder>()
                .set("status", wo.getStatus())
                .set("issue_by", wo.getIssueBy())
                .set("update_time", LocalDateTime.now())
                .eq("id", id);
        workOrderMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startProduction(Long id) {
        WorkOrder wo = getByIdOrThrow(id);
        transitionStatus(wo, WorkOrderStatusEnum.IN_PRODUCTION);
        
        var updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<WorkOrder>()
                .set("status", wo.getStatus())
                .set("actual_start_time", LocalDateTime.now())
                .set("update_time", LocalDateTime.now())
                .eq("id", id);
        workOrderMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id) {
        WorkOrder wo = getByIdOrThrow(id);
        if (!WorkOrderStatusEnum.IN_PRODUCTION.getCode().equals(wo.getStatus()) 
            && !WorkOrderStatusEnum.PENDING_QC.getCode().equals(wo.getStatus())) {
            throw new BizException(ErrorCode.WORKORDER_STATUS_ERROR.getCode(), "只有生产中或待质检状态的工单可以完成");
        }
        transitionStatus(wo, WorkOrderStatusEnum.COMPLETED);
        wo.setActualEndTime(LocalDateTime.now());
        updateById(wo);
        log.info("工单完成: id={}, orderNo={}", id, wo.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id) {
        WorkOrder wo = getByIdOrThrow(id);
        transitionStatus(wo, WorkOrderStatusEnum.CLOSED);
        updateById(wo);
        log.info("工单关闭: id={}, orderNo={}", id, wo.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, UpdateWorkOrderDTO dto) {
        WorkOrder wo = getByIdOrThrow(id);
        boolean editing = dto.getProductName() != null || dto.getProductModel() != null
                || dto.getPlanQuantity() != null || dto.getWorkstationId() != null
                || dto.getProcessTemplateId() != null || dto.getPlannedStartTime() != null
                || dto.getPlannedEndTime() != null;
        if (editing && !WorkOrderStatusEnum.CREATED.getCode().equals(wo.getStatus())) {
            throw new BizException(ErrorCode.WORKORDER_STATUS_ERROR.getCode(), "仅已创建的工单可以编辑业务信息");
        }
        if (dto.getStatus() != null) {
            WorkOrderStatusEnum target = WorkOrderStatusEnum.valueOf(dto.getStatus());
            transitionStatus(wo, target);
        }
        if (dto.getPriority() != null) {
            wo.setPriority(dto.getPriority());
        }
        if (dto.getRemark() != null) {
            wo.setRemark(dto.getRemark());
        }
        if (dto.getProductName() != null) {
            wo.setProductName(dto.getProductName());
        }
        if (dto.getProductModel() != null) {
            wo.setProductModel(dto.getProductModel());
        }
        if (dto.getPlanQuantity() != null) {
            wo.setPlanQuantity(dto.getPlanQuantity());
        }
        if (dto.getWorkstationId() != null) {
            wo.setWorkstationId(dto.getWorkstationId());
        }
        if (dto.getProcessTemplateId() != null) {
            wo.setProcessTemplateId(dto.getProcessTemplateId());
        }
        if (dto.getPlannedStartTime() != null) {
            wo.setPlannedStartTime(dto.getPlannedStartTime());
        }
        if (dto.getPlannedEndTime() != null) {
            wo.setPlannedEndTime(dto.getPlannedEndTime());
        }
        updateById(wo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkReport submitReport(SubmitReportDTO dto) {
        WorkOrder wo = getByIdOrThrow(dto.getWorkOrderId());
        if (!WorkOrderStatusEnum.IN_PRODUCTION.getCode().equals(wo.getStatus())) {
            throw new BizException(ErrorCode.WORKORDER_STATUS_ERROR.getCode(), "当前工单不在生产中，无法报工");
        }
        WorkReport report = new WorkReport();
        report.setWorkOrderId(dto.getWorkOrderId());
        report.setDeviceId(dto.getDeviceId());
        report.setOperatorId(getCurrentUserId());
        report.setReportQuantity(dto.getReportQuantity());
        report.setQualifiedQuantity(dto.getQualifiedQuantity() != null ? dto.getQualifiedQuantity() : dto.getReportQuantity());
        report.setDefectiveQuantity(dto.getDefectiveQuantity() != null ? dto.getDefectiveQuantity() : 0);
        report.setReportTime(LocalDateTime.now());
        report.setSnStart(dto.getSnStart());
        report.setSnEnd(dto.getSnEnd());
        report.setRemark(dto.getRemark());
        workReportMapper.insert(report);

        // 原子更新已完成数量，避免并发报工时的读改写竞态
        var incrementWrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<WorkOrder>()
                .setSql("completed_quantity = completed_quantity + " + dto.getReportQuantity())
                .eq("id", dto.getWorkOrderId())
                .eq("deleted", 0);
        workOrderMapper.update(null, incrementWrapper);

        // 重新读取最新进度，判断是否达到计划量
        wo = getByIdOrThrow(dto.getWorkOrderId());
        if (wo.getCompletedQuantity() >= wo.getPlanQuantity()) {
            transitionStatus(wo, WorkOrderStatusEnum.PENDING_QC);
            updateById(wo);
        }
        return report;
    }

    @Override
    public PageResult<WorkOrder> queryPage(int current, int size, String status, String keyword) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getDeleted, 0);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(WorkOrder::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(WorkOrder::getOrderNo, keyword)
                    .or().like(WorkOrder::getProductName, keyword));
        }
        wrapper.orderByDesc(WorkOrder::getCreateTime);
        Page<WorkOrder> page = this.page(Page.of(current, size), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords(), current, size);
    }

    private WorkOrder getByIdOrThrow(Long id) {
        WorkOrder wo = this.getById(id);
        if (wo == null) {
            throw new BizException(ErrorCode.WORKORDER_NOT_FOUND);
        }
        return wo;
    }

    private void transitionStatus(WorkOrder wo, WorkOrderStatusEnum target) {
        WorkOrderStatusEnum current = WorkOrderStatusEnum.valueOf(wo.getStatus());
        if (!current.canTransitionTo(target)) {
            throw new BizException(ErrorCode.WORKORDER_STATUS_ERROR.getCode(),
                    "不允许从" + current.getDesc() + "转换到" + target.getDesc());
        }
        wo.setStatus(target.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        log.info("删除工单, id={}", id);
        WorkOrder order = getById(id);
        if (order == null) {
            log.warn("工单不存在, id={}", id);
            return Result.error("工单不存在");
        }

        String status = order.getStatus();
        log.info("工单状态: {}, orderNo: {}", status, order.getOrderNo());
        if (!WorkOrderStatusEnum.CREATED.getCode().equals(status) 
            && !WorkOrderStatusEnum.CLOSED.getCode().equals(status)) {
            log.warn("状态不正确无法删除, status={}", status);
            return Result.error("只有草稿或已关闭的工单可删除");
        }

        baseMapper.deleteById(id);
        log.info("删除成功, id={}", id);

        return Result.ok();
    }


    private String generateOrderNo() {
        // 优先使用Redis自增序列保证并发唯一（带日期前缀，可读性好）
        try {
            Long seq = stringRedisTemplate.opsForValue().increment(ORDER_SEQ_KEY);
            if (seq != null) {
                return "WO" + LocalDateTime.now().format(SEQ_FORMAT) + String.format("%04d", seq % 10000);
            }
        } catch (Exception e) {
            log.warn("Redis生成工单号失败，降级为时间戳: {}", e.getMessage());
        }
        return "WO" + System.currentTimeMillis();
    }

    private Long getCurrentUserId() {
        Long userId = com.mes.common.security.UserContext.getUserId();
        return userId != null ? userId : 1L;
    }
}
