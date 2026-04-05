package com.mes.workorder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.result.PageResult;
import com.mes.workorder.dto.CreateWorkOrderDTO;
import com.mes.workorder.dto.SubmitReportDTO;
import com.mes.workorder.dto.UpdateWorkOrderDTO;
import com.mes.workorder.entity.WorkOrder;
import com.mes.workorder.entity.WorkReport;

public interface WorkOrderService extends IService<WorkOrder> {
    WorkOrder create(CreateWorkOrderDTO dto, Long userId);
    void issue(Long id);
    void startProduction(Long id);
    void updateStatus(Long id, UpdateWorkOrderDTO dto);
    WorkReport submitReport(SubmitReportDTO dto, Long operatorId);
    PageResult<WorkOrder> queryPage(int current, int size, String status, String keyword);
}
