package com.mes.workorder.service;

import com.mes.workorder.dto.planning.PlanningAutoPlanDTO;
import com.mes.workorder.dto.planning.PlanningBoardVO;
import com.mes.workorder.dto.planning.PlanningFreezeRequestDTO;
import com.mes.workorder.dto.planning.PlanningMoveRequestDTO;
import com.mes.workorder.dto.planning.PlanningSaveOrderDTO;

import java.time.LocalDateTime;

/**
 * 排产看板服务
 */
public interface PlanningBoardService {

    /**
     * 查询排产看板数据（工序级甘特 + 负载率 + 冲突 + 日志 + 班次日历）
     */
    PlanningBoardVO getBoard(LocalDateTime windowStart, LocalDateTime windowEnd);

    /**
     * 保存拖拽后的排产顺序（兼容旧版整单拖拽）
     */
    void saveOrder(PlanningSaveOrderDTO dto);

    /**
     * 工序/整单拖拽调整（换设备、改时间、拉伸缩短），带冲突检测
     */
    void move(PlanningMoveRequestDTO dto);

    /**
     * 拖回待排产池（取消排产）
     */
    void unassign(Long workOrderId);

    /**
     * 自动排程（APS：优先级+交期+工序拆分+负载均衡+工作日历）
     *
     * @return 排程的工序数
     */
    int autoPlan(PlanningAutoPlanDTO dto);

    /**
     * 撤销上一次排产变更
     */
    void undo();

    /**
     * 冻结排产（设备行/工单/指定工序），冻结后不可拖动
     */
    int freeze(PlanningFreezeRequestDTO dto);

    /**
     * 解除冻结
     */
    int unfreeze(PlanningFreezeRequestDTO dto);

    /**
     * 下发排产（标记RELEASED）
     */
    int release(Long workOrderId);
}
