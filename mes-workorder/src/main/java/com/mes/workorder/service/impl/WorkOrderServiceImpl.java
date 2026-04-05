package com.mes.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.exception.BizException;
import com.mes.common.exception.ErrorCode;
import com.mes.common.result.PageResult;
import com.mes.workorder.dto.CreateWorkOrderDTO;
import com.mes.workorder.dto.SubmitReportDTO;
import com.mes.workorder.dto.UpdateWorkOrderDTO;
import com.mes.workorder.entity.WorkOrder;
import com.mes.workorder.entity.WorkReport;
import com.mes.workorder.enums.WorkOrderStatusEnum;
import com.mes.workorder.mapper.WorkOrderMapper;
import com.mes.workorder.mapper.WorkReportMapper;
import com.mes.workorder.service.WorkOrderService;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder> implements WorkOrderService {

    private final WorkReportMapper workReportMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder create(CreateWorkOrderDTO dto, Long userId) {
        WorkOrder workOrder = new WorkOrder();
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
        workOrder.setCreateBy(userId);
        this.save(workOrder);
        return workOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issue(Long id) {
        WorkOrder wo = getByIdOrThrow(id);
        transitionStatus(wo, WorkOrderStatusEnum.ISSUED);
        wo.setIssueBy(getCurrentUserId());
        updateById(wo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startProduction(Long id) {
        WorkOrder wo = getByIdOrThrow(id);
        transitionStatus(wo, WorkOrderStatusEnum.IN_PRODUCTION);
        wo.setActualStartTime(LocalDateTime.now());
        updateById(wo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, UpdateWorkOrderDTO dto) {
        WorkOrder wo = getByIdOrThrow(id);
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
        updateById(wo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkReport submitReport(SubmitReportDTO dto, Long operatorId) {
        WorkOrder wo = getByIdOrThrow(dto.getWorkOrderId());
        if (!WorkOrderStatusEnum.IN_PRODUCTION.getCode().equals(wo.getStatus())) {
            throw new BizException(ErrorCode.WORKORDER_STATUS_ERROR.getCode(), "当前工单不在生产中，无法报工");
        }
        WorkReport report = new WorkReport();
        report.setWorkOrderId(dto.getWorkOrderId());
        report.setDeviceId(dto.getDeviceId());
        report.setOperatorId(operatorId);
        report.setReportQuantity(dto.getReportQuantity());
        report.setQualifiedQuantity(dto.getQualifiedQuantity() != null ? dto.getQualifiedQuantity() : dto.getReportQuantity());
        report.setDefectiveQuantity(dto.getDefectiveQuantity() != null ? dto.getDefectiveQuantity() : 0);
        report.setReportTime(LocalDateTime.now());
        report.setSnStart(dto.getSnStart());
        report.setSnEnd(dto.getSnEnd());
        report.setRemark(dto.getRemark());
        workReportMapper.insert(report);

        wo.setCompletedQuantity(wo.getCompletedQuantity() + dto.getReportQuantity());
        if (wo.getCompletedQuantity() >= wo.getPlanQuantity()) {
            transitionStatus(wo, WorkOrderStatusEnum.PENDING_QC);
        }
        updateById(wo);
        return report;
    }

    @Override
    public PageResult<WorkOrder> queryPage(int current, int size, String status, String keyword) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
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

    private String generateOrderNo() {
        return "WO" + System.currentTimeMillis();
    }

    private Long getCurrentUserId() {
        return 1L;
    }
}
