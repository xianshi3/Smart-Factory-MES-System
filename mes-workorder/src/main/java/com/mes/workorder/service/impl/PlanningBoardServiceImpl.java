package com.mes.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mes.common.entity.Workstation;
import com.mes.common.mapper.WorkstationMapper;
import com.mes.workorder.dto.planning.*;
import com.mes.workorder.entity.*;
import com.mes.workorder.enums.WorkOrderStatusEnum;
import com.mes.workorder.mapper.*;
import com.mes.workorder.service.PlanningBoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排产看板服务实现（工序级APS排产）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanningBoardServiceImpl implements PlanningBoardService {

    private static final Set<String> FINAL_STATUS = Set.of(
            WorkOrderStatusEnum.COMPLETED.getCode(),
            WorkOrderStatusEnum.CLOSED.getCode());
    private static final String STATUS_PLANNED = "PLANNED";
    private static final String STATUS_FROZEN = "FROZEN";
    private static final String STATUS_RELEASED = "RELEASED";
    private static final int DEFAULT_STEP_DURATION_MIN = 480;
    private static final int UNDO_LIMIT = 20;

    private final WorkOrderMapper workOrderMapper;
    private final WorkstationMapper workstationMapper;
    private final ScheduleItemMapper scheduleItemMapper;
    private final ScheduleLogMapper scheduleLogMapper;
    private final ShiftMapper shiftMapper;
    private final WorkCalendarMapper workCalendarMapper;
    private final ProcStepMapper procStepMapper;

    /** 撤销栈：最近一次变更的快照 */
    private final Deque<UndoSnapshot> undoStack = new ArrayDeque<>();

    // ==================== 查询 ====================

    @Override
    public PlanningBoardVO getBoard(LocalDateTime windowStart, LocalDateTime windowEnd) {
        LocalDateTime start = windowStart != null ? windowStart : LocalDateTime.now().plusDays(-7);
        LocalDateTime end = windowEnd != null ? windowEnd : start.plusDays(14);
        if (!end.isAfter(start)) {
            end = start.plusDays(14);
        }

        List<Workstation> workstations = workstationMapper.selectList(
                new LambdaQueryWrapper<Workstation>()
                        .eq(Workstation::getDeleted, 0)
                        .orderByAsc(Workstation::getId));
        List<WorkOrder> orders = workOrderMapper.selectList(
                new LambdaQueryWrapper<WorkOrder>()
                        .eq(WorkOrder::getDeleted, 0));
        List<ScheduleItem> items = scheduleItemMapper.selectList(
                new LambdaQueryWrapper<ScheduleItem>()
                        .eq(ScheduleItem::getDeleted, 0)
                        .orderByAsc(ScheduleItem::getPlannedStart));
        List<Shift> shifts = shiftMapper.selectList(new LambdaQueryWrapper<>());
        List<WorkCalendarDay> calendar = workCalendarMapper.selectList(
                new LambdaQueryWrapper<WorkCalendarDay>()
                        .ge(WorkCalendarDay::getWorkDate, start.toLocalDate())
                        .le(WorkCalendarDay::getWorkDate, end.toLocalDate()));

        Map<Long, WorkOrder> orderMap = orders.stream()
                .filter(o -> o.getId() != null)
                .collect(Collectors.toMap(WorkOrder::getId, o -> o, (a, b) -> a));

        long workMinutes = countWorkingMinutes(start, end, shifts, calendar);
        Map<Long, Workstation> wsMap = workstations.stream()
                .collect(Collectors.toMap(Workstation::getId, w -> w, (a, b) -> a));

        Map<Long, List<PlanningTaskVO>> byWs = new LinkedHashMap<>();
        for (Workstation ws : workstations) {
            byWs.put(ws.getId(), new ArrayList<>());
        }

        Set<Long> plannedOrderIds = new HashSet<>();
        for (ScheduleItem item : items) {
            WorkOrder o = orderMap.get(item.getWorkOrderId());
            if (o == null || item.getWorkstationId() == null) {
                continue;
            }
            plannedOrderIds.add(item.getWorkOrderId());
            PlanningTaskVO vo = toVO(item, o);
            byWs.computeIfAbsent(item.getWorkstationId(), k -> new ArrayList<>()).add(vo);
        }

        List<PlanningEquipmentVO> equipmentList = new ArrayList<>();
        for (Workstation ws : workstations) {
            List<PlanningTaskVO> tasks = byWs.getOrDefault(ws.getId(), Collections.emptyList()).stream()
                    .sorted(Comparator.comparing(PlanningTaskVO::getPlannedStartTime,
                            Comparator.nullsLast(LocalDateTime::compareTo)))
                    .collect(Collectors.toList());
            PlanningEquipmentVO vo = new PlanningEquipmentVO();
            vo.setId(ws.getId());
            vo.setWorkstationCode(ws.getWorkstationCode());
            vo.setWorkstationName(ws.getWorkstationName());
            vo.setStatus(ws.getStatus());
            vo.setCapacityPerHour(ws.getCapacityPerHour());
            vo.setBottleneck(false);
            vo.setTasks(tasks);
            vo.setTaskCount(tasks.size());
            vo.setLoadRate(calcLoadRate(tasks, start, end, workMinutes));
            equipmentList.add(vo);
        }

        // 瓶颈设备 = 负载最高的设备（并列时标记第一个）
        int maxLoad = equipmentList.stream()
                .mapToInt(e -> e.getLoadRate() == null ? 0 : e.getLoadRate())
                .max().orElse(0);
        if (maxLoad > 0) {
            equipmentList.stream()
                    .filter(e -> e.getLoadRate() != null && e.getLoadRate() == maxLoad)
                    .findFirst()
                    .ifPresent(e -> e.setBottleneck(true));
        }

        List<PlanningTaskVO> unassigned = orders.stream()
                .filter(o -> !plannedOrderIds.contains(o.getId()) && !FINAL_STATUS.contains(o.getStatus()))
                .map(o -> toVO(null, o))
                .sorted(Comparator.comparing(PlanningTaskVO::getPlannedEndTime,
                        Comparator.nullsLast(LocalDateTime::compareTo)))
                .collect(Collectors.toList());

        PlanningBoardVO board = new PlanningBoardVO();
        board.setWindowStart(start);
        board.setWindowEnd(end);
        board.setWorkMinutes(workMinutes);
        board.setEquipment(equipmentList);
        board.setUnassigned(unassigned);
        board.setShifts(shifts.stream().map(this::toShiftVO).collect(Collectors.toList()));
        board.setCalendar(calendar.stream().map(this::toCalendarVO).collect(Collectors.toList()));
        board.setConflicts(detectConflicts(items, wsMap, orderMap));
        board.setLogs(queryLogs());
        return board;
    }

    private List<PlanningConflictVO> detectConflicts(List<ScheduleItem> items, Map<Long, Workstation> wsMap,
                                                     Map<Long, WorkOrder> orderMap) {
        Map<Long, List<ScheduleItem>> byWs = items.stream()
                .filter(i -> i.getWorkstationId() != null && i.getPlannedStart() != null && i.getPlannedEnd() != null)
                .collect(Collectors.groupingBy(ScheduleItem::getWorkstationId));
        List<PlanningConflictVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<ScheduleItem>> e : byWs.entrySet()) {
            List<ScheduleItem> list = e.getValue().stream()
                    .sorted(Comparator.comparing(ScheduleItem::getPlannedStart))
                    .collect(Collectors.toList());
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    ScheduleItem a = list.get(i), b = list.get(j);
                    if (!a.getPlannedEnd().isAfter(b.getPlannedStart())) {
                        break;
                    }
                    if (Objects.equals(a.getWorkOrderId(), b.getWorkOrderId())) {
                        continue;
                    }
                    PlanningConflictVO c = new PlanningConflictVO();
                    c.setWorkstationId(e.getKey());
                    c.setWorkstationName(wsMap.containsKey(e.getKey()) ? wsMap.get(e.getKey()).getWorkstationName() : null);
                    c.setScheduleAId(a.getId());
                    c.setOrderNoA(orderMap.containsKey(a.getWorkOrderId()) ? orderMap.get(a.getWorkOrderId()).getOrderNo() : null);
                    c.setStepNameA(a.getStepName());
                    c.setScheduleBId(b.getId());
                    c.setOrderNoB(orderMap.containsKey(b.getWorkOrderId()) ? orderMap.get(b.getWorkOrderId()).getOrderNo() : null);
                    c.setStepNameB(b.getStepName());
                    // 列表按plannedStart升序，重叠起点=较晚的起点(b)，终点=较早的终点(min)
                    c.setOverlapStart(b.getPlannedStart());
                    c.setOverlapEnd(a.getPlannedEnd().isBefore(b.getPlannedEnd()) ? a.getPlannedEnd() : b.getPlannedEnd());
                    result.add(c);
                }
            }
        }
        return result;
    }

    private List<PlanningLogVO> queryLogs() {
        return scheduleLogMapper.selectList(
                        new LambdaQueryWrapper<ScheduleLog>()
                                .orderByDesc(ScheduleLog::getCreateTime)
                                .last("LIMIT 50"))
                .stream().map(l -> {
                    PlanningLogVO vo = new PlanningLogVO();
                    vo.setId(l.getId());
                    vo.setAction(l.getAction());
                    vo.setActionDesc(l.getActionDesc());
                    vo.setCreateTime(l.getCreateTime());
                    return vo;
                }).collect(Collectors.toList());
    }

    private PlanningShiftVO toShiftVO(Shift s) {
        PlanningShiftVO vo = new PlanningShiftVO();
        vo.setShiftCode(s.getShiftCode());
        vo.setShiftName(s.getShiftName());
        vo.setStartTime(s.getStartTime());
        vo.setEndTime(s.getEndTime());
        vo.setWork(s.getIsWork() == null || s.getIsWork() == 1);
        return vo;
    }

    private PlanningCalendarVO toCalendarVO(WorkCalendarDay d) {
        PlanningCalendarVO vo = new PlanningCalendarVO();
        vo.setWorkDate(d.getWorkDate());
        vo.setWorkday(d.getIsWorkday() == null || d.getIsWorkday() == 1);
        vo.setRemark(d.getRemark());
        return vo;
    }

    /**
     * 设备负载率 = 窗口内排产工时 / 窗口内可用工作分钟（工作日历×班次）
     */
    private Integer calcLoadRate(List<PlanningTaskVO> tasks, LocalDateTime start, LocalDateTime end, long workMinutes) {
        if (tasks.isEmpty() || workMinutes <= 0) {
            return 0;
        }
        long plannedMinutes = tasks.stream()
                .filter(t -> !FINAL_STATUS.contains(t.getStatus()))
                .mapToLong(t -> {
                    if (t.getPlannedStartTime() == null || t.getPlannedEndTime() == null) {
                        return 0;
                    }
                    LocalDateTime s = t.getPlannedStartTime().isBefore(start) ? start : t.getPlannedStartTime();
                    LocalDateTime e = t.getPlannedEndTime().isAfter(end) ? end : t.getPlannedEndTime();
                    return Math.max(0, Duration.between(s, e).toMinutes());
                })
                .sum();
        return (int) Math.min(100, Math.round(plannedMinutes * 100.0 / workMinutes));
    }

    /**
     * 工序明细 → 看板条目；scheduleItem为null表示未排产
     */
    private PlanningTaskVO toVO(ScheduleItem item, WorkOrder o) {
        PlanningTaskVO vo = new PlanningTaskVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setProductName(o.getProductName());
        vo.setProductModel(o.getProductModel());
        vo.setPriority(o.getPriority());
        vo.setStatus(o.getStatus());
        vo.setPlanQuantity(o.getPlanQuantity());
        vo.setCompletedQuantity(o.getCompletedQuantity());
        vo.setProgress(o.getPlanQuantity() == null || o.getPlanQuantity() == 0
                ? 0 : (int) Math.min(100, Math.round(o.getCompletedQuantity() * 100.0 / o.getPlanQuantity())));
        vo.setWorkstationId(o.getWorkstationId());
        vo.setSortOrder(o.getSortOrder());
        vo.setRemark(o.getRemark());
        vo.setActualStartTime(o.getActualStartTime());
        if (item != null) {
            vo.setScheduleId(item.getId());
            vo.setStepNo(item.getStepNo());
            vo.setStepName(item.getStepName());
            vo.setDurationMin(item.getDurationMin());
            vo.setBottleneck(item.getBottleneck() != null && item.getBottleneck() == 1);
            vo.setScheduleStatus(item.getStatus());
            vo.setWorkstationId(item.getWorkstationId());
            vo.setSortOrder(item.getSortOrder());
            vo.setPlannedStartTime(item.getPlannedStart());
            vo.setPlannedEndTime(item.getPlannedEnd());
        } else {
            vo.setPlannedStartTime(o.getPlannedStartTime());
            vo.setPlannedEndTime(o.getPlannedEndTime());
        }
        vo.setPlanStatus(resolvePlanStatus(o));
        return vo;
    }

    /**
     * 看板状态：PENDING-待排产 READY-已排产 RUNNING-运行中 COMPLETED-已完成 DELAYED-延误
     */
    private String resolvePlanStatus(WorkOrder o) {
        String status = o.getStatus();
        if (FINAL_STATUS.contains(status)) {
            return "COMPLETED";
        }
        boolean started = WorkOrderStatusEnum.IN_PRODUCTION.getCode().equals(status)
                || WorkOrderStatusEnum.PENDING_QC.getCode().equals(status);
        boolean pastDue = o.getPlannedEndTime() != null && o.getPlannedEndTime().isBefore(LocalDateTime.now());
        if (pastDue) {
            return "DELAYED";
        }
        if (started) {
            return "RUNNING";
        }
        if (o.getWorkstationId() != null) {
            return "READY";
        }
        return "PENDING";
    }

    // ==================== 变更操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrder(PlanningSaveOrderDTO dto) {
        if (dto.getGroups() == null || dto.getGroups().isEmpty()) {
            return;
        }
        List<WorkOrder> all = workOrderMapper.selectList(
                new LambdaQueryWrapper<WorkOrder>().eq(WorkOrder::getDeleted, 0));
        Map<Long, WorkOrder> orderMap = all.stream()
                .filter(o -> o.getId() != null)
                .collect(Collectors.toMap(WorkOrder::getId, o -> o, (a, b) -> a));
        List<ScheduleItem> items = scheduleItemMapper.selectList(
                new LambdaQueryWrapper<ScheduleItem>().eq(ScheduleItem::getDeleted, 0));
        Map<Long, List<ScheduleItem>> itemsByOrder = items.stream()
                .collect(Collectors.groupingBy(ScheduleItem::getWorkOrderId));

        int seq = 0;
        for (PlanningSaveOrderDTO.EquipmentOrder group : dto.getGroups()) {
            if (group.getEquipmentId() == null) {
                continue;
            }
            List<Long> ids = group.getWorkOrderIds() == null ? Collections.emptyList() : group.getWorkOrderIds();
            seq = 0;
            for (Long id : ids) {
                WorkOrder wo = orderMap.get(id);
                if (wo == null) {
                    continue;
                }
                seq++;
                UpdateWrapper<WorkOrder> uw = new UpdateWrapper<WorkOrder>()
                        .eq("id", id)
                        .eq("deleted", 0)
                        .set("workstation_id", group.getEquipmentId())
                        .set("sort_order", seq);
                workOrderMapper.update(null, uw);
                // 同步工序明细的设备
                List<ScheduleItem> orderItems = itemsByOrder.getOrDefault(id, Collections.emptyList());
                if (!orderItems.isEmpty()) {
                    for (ScheduleItem it : orderItems) {
                        if (it.getStatus() == null || it.getStatus().equals(STATUS_PLANNED)) {
                            UpdateWrapper<ScheduleItem> iw = new UpdateWrapper<ScheduleItem>()
                                    .eq("id", it.getId())
                                    .set("workstation_id", group.getEquipmentId());
                            scheduleItemMapper.update(null, iw);
                        }
                    }
                }
                writeLog(id, null, "ASSIGN", "整单排产到设备" + group.getEquipmentId());
            }
        }
        if (dto.getUnassignedOrderIds() != null) {
            for (Long id : dto.getUnassignedOrderIds()) {
                UpdateWrapper<WorkOrder> uw = new UpdateWrapper<WorkOrder>()
                        .eq("id", id)
                        .eq("deleted", 0)
                        .set("workstation_id", null)
                        .set("sort_order", 0);
                workOrderMapper.update(null, uw);
            }
        }
        log.info("排产顺序保存完成, groups={}, unassigned={}",
                dto.getGroups().size(),
                dto.getUnassignedOrderIds() == null ? 0 : dto.getUnassignedOrderIds().size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(PlanningMoveRequestDTO dto) {
        WorkOrder order = workOrderMapper.selectById(dto.getWorkOrderId());
        if (order == null || (order.getDeleted() != null && order.getDeleted() == 1)) {
            throw new IllegalStateException("工单不存在");
        }
        List<ScheduleItem> items = scheduleItemMapper.selectList(
                new LambdaQueryWrapper<ScheduleItem>()
                        .eq(ScheduleItem::getWorkOrderId, dto.getWorkOrderId())
                        .eq(ScheduleItem::getDeleted, 0));
        List<WorkOrder> orderBefore = new ArrayList<>();
        orderBefore.add(copyOf(order));

        if (!items.isEmpty()) {
            ScheduleItem target = null;
            if (dto.getScheduleId() != null) {
                target = items.stream()
                        .filter(i -> Objects.equals(i.getId(), dto.getScheduleId()))
                        .findFirst().orElse(null);
                if (target == null) {
                    throw new IllegalStateException("排产明细不存在");
                }
            }
            List<ScheduleItem> before = items.stream().map(this::copyOf).collect(Collectors.toList());
            List<ScheduleItem> affected = new ArrayList<>();

            List<ScheduleItem> toMove = target != null ? List.of(target) : items;
            boolean forced = dto.getForce() != null && dto.getForce();
            for (ScheduleItem it : toMove) {
                if (it.getPlannedStart() == null || it.getPlannedEnd() == null) {
                    throw new IllegalStateException("工序[" + it.getStepName() + "]缺少计划时间，无法调整");
                }
                // 冻结/已下发工序任何操作（含强制）均不允许移动
                if (STATUS_FROZEN.equals(it.getStatus()) || STATUS_RELEASED.equals(it.getStatus())) {
                    throw new IllegalStateException("该工序已"
                            + (STATUS_FROZEN.equals(it.getStatus()) ? "冻结" : "下发")
                            + "，不能调整，请先解冻");
                }
            }
            LocalDateTime orderStart = items.stream().map(ScheduleItem::getPlannedStart)
                    .filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(dto.getNewStart());
            for (ScheduleItem it : toMove) {
                LocalDateTime newStart = dto.getNewStart() != null
                        ? (target != null ? dto.getNewStart()
                        : dto.getNewStart().plusMinutes(it.getPlannedStart() == null ? 0 :
                        Duration.between(orderStart, it.getPlannedStart()).toMinutes()))
                        : it.getPlannedStart();
                LocalDateTime newEnd = dto.getNewEnd() != null
                        ? (target != null ? dto.getNewEnd()
                        : newStart.plusMinutes(Duration.between(it.getPlannedStart(), it.getPlannedEnd()).toMinutes()))
                        : newStart.plusMinutes(it.getDurationMin() == null ? 0 : it.getDurationMin());
                it.setWorkstationId(dto.getTargetWorkstationId());
                if (newStart != null) {
                    it.setPlannedStart(newStart);
                }
                it.setPlannedEnd(newEnd);
                affected.add(it);
            }
            if (!forced) {
                checkConflict(dto.getTargetWorkstationId(), affected, dto.getWorkOrderId());
            }
            for (ScheduleItem it : affected) {
                scheduleItemMapper.updateById(it);
            }
            refreshOrderFromItems(order, items);
            workOrderMapper.updateById(order);
            pushUndo(before, items, orderBefore, "拖拽调整");
            writeLog(order.getId(), target != null ? target.getId() : null,
                    "MOVE",
                    (target != null ? "工序[" + target.getStepName() + "]移动" : "整单移动")
                            + "到设备" + dto.getTargetWorkstationId());
        } else {
            // 未排产工单拖入设备：按工艺模板拆分排产（事务内冲突检测，冲突即回滚）
            boolean forced = dto.getForce() != null && dto.getForce();
            List<ScheduleItem> created = createPlanFromTemplate(order, dto);
            if (!forced) {
                checkConflict(dto.getTargetWorkstationId(), created, dto.getWorkOrderId());
            }
            List<ScheduleItem> before = new ArrayList<>();
            pushUndo(before, created, orderBefore, "拖入排产");
            writeLog(order.getId(), null, "ASSIGN", "工单拖入设备" + dto.getTargetWorkstationId() + "排产");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unassign(Long workOrderId) {
        WorkOrder order = workOrderMapper.selectById(workOrderId);
        if (order == null || (order.getDeleted() != null && order.getDeleted() == 1)) {
            throw new IllegalStateException("工单不存在");
        }
        List<ScheduleItem> items = scheduleItemMapper.selectList(
                new LambdaQueryWrapper<ScheduleItem>()
                        .eq(ScheduleItem::getWorkOrderId, workOrderId)
                        .eq(ScheduleItem::getDeleted, 0));
        for (ScheduleItem it : items) {
            if (STATUS_FROZEN.equals(it.getStatus()) || STATUS_RELEASED.equals(it.getStatus())) {
                throw new IllegalStateException("工单已冻结或已下发，不能取消排产");
            }
        }
        List<ScheduleItem> before = items.stream().map(this::copyOf).collect(Collectors.toList());
        for (ScheduleItem it : items) {
            scheduleItemMapper.deleteById(it.getId());
        }
        order.setWorkstationId(null);
        order.setSortOrder(0);
        order.setPlannedStartTime(null);
        order.setPlannedEndTime(null);
        workOrderMapper.updateById(order);
        pushUndo(before, new ArrayList<>(), List.of(copyOf(order)), "取消排产");
        writeLog(order.getId(), null, "UNASSIGN", "工单移回待排产池");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoPlan(PlanningAutoPlanDTO dto) {
        LocalDateTime winStart = dto.getWindowStart() != null ? dto.getWindowStart()
                : LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime winEnd = dto.getWindowEnd() != null ? dto.getWindowEnd() : winStart.plusDays(14);
        boolean onlyPending = dto.getOnlyPending() == null || dto.getOnlyPending();

        List<WorkOrder> orders = workOrderMapper.selectList(
                new LambdaQueryWrapper<WorkOrder>().eq(WorkOrder::getDeleted, 0));
        List<ScheduleItem> allItems = scheduleItemMapper.selectList(
                new LambdaQueryWrapper<ScheduleItem>().eq(ScheduleItem::getDeleted, 0));
        Map<Long, List<ScheduleItem>> itemsByOrder = allItems.stream()
                .collect(Collectors.groupingBy(ScheduleItem::getWorkOrderId));

        // 全量重排：删除PLANNED状态的排产，保留冻结/已下发
        // 注意：含冻结/已下发工序的工单不参与重排，其PLANNED工序一并保留，避免工单工序残缺
        List<ScheduleItem> toDelete = new ArrayList<>();
        if (!onlyPending) {
            Set<Long> lockedOrderIds = allItems.stream()
                    .filter(i -> STATUS_FROZEN.equals(i.getStatus()) || STATUS_RELEASED.equals(i.getStatus()))
                    .map(ScheduleItem::getWorkOrderId)
                    .collect(Collectors.toSet());
            for (ScheduleItem it : allItems) {
                if (STATUS_PLANNED.equals(it.getStatus())
                        && !lockedOrderIds.contains(it.getWorkOrderId())) {
                    toDelete.add(it);
                }
            }
        }
        Map<Long, List<ScheduleItem>> itemsByWs = allItems.stream()
                .filter(i -> !toDelete.contains(i) && i.getWorkstationId() != null)
                .collect(Collectors.groupingBy(ScheduleItem::getWorkstationId));
        Map<Long, Workstation> wsMap = workstationMapper.selectList(
                        new LambdaQueryWrapper<Workstation>().eq(Workstation::getDeleted, 0))
                .stream().collect(Collectors.toMap(Workstation::getId, w -> w, (a, b) -> a));
        List<Shift> shifts = shiftMapper.selectList(new LambdaQueryWrapper<>());
        List<WorkCalendarDay> calendar = workCalendarMapper.selectList(new LambdaQueryWrapper<>());

        List<WorkOrder> candidates = orders.stream()
                .filter(o -> !FINAL_STATUS.contains(o.getStatus()))
                .filter(o -> itemsByOrder.getOrDefault(o.getId(), Collections.emptyList()).stream()
                        .noneMatch(i -> STATUS_FROZEN.equals(i.getStatus()) || STATUS_RELEASED.equals(i.getStatus())))
                .filter(o -> onlyPending
                        ? itemsByOrder.getOrDefault(o.getId(), Collections.emptyList()).isEmpty()
                        : true)
                .sorted(Comparator
                        .comparing((WorkOrder o) -> priorityRank(o.getPriority()))
                        .thenComparing(o -> o.getPlannedEndTime() == null ? LocalDateTime.MAX : o.getPlannedEndTime()))
                .collect(Collectors.toList());

        Map<Long, Long> wsLoad = new HashMap<>();
        for (Map.Entry<Long, List<ScheduleItem>> e : itemsByWs.entrySet()) {
            long sum = e.getValue().stream()
                    .mapToLong(i -> i.getDurationMin() == null ? 0 : i.getDurationMin())
                    .sum();
            wsLoad.put(e.getKey(), sum);
        }
        List<WorkOrder> orderBefore = orders.stream()
                .filter(o -> candidates.stream().anyMatch(c -> c.getId().equals(o.getId())))
                .map(this::copyOf).collect(Collectors.toList());
        List<ScheduleItem> beforeItems = new ArrayList<>();
        for (ScheduleItem it : toDelete) {
            beforeItems.add(copyOf(it));
        }

        List<ScheduleItem> created = new ArrayList<>();
        for (WorkOrder order : candidates) {
            List<ScheduleItem> planned = planOrder(order, winStart, winEnd, shifts, calendar,
                    itemsByWs, wsLoad, wsMap.keySet());
            created.addAll(planned);
        }

        // 写库
        for (ScheduleItem it : toDelete) {
            scheduleItemMapper.deleteById(it.getId());
        }
        for (ScheduleItem it : created) {
            scheduleItemMapper.insert(it);
        }
        // 重算设备内 sort_order
        for (Map.Entry<Long, List<ScheduleItem>> e : itemsByWs.entrySet()) {
            List<ScheduleItem> sorted = e.getValue().stream()
                    .sorted(Comparator.comparing(ScheduleItem::getPlannedStart,
                            Comparator.nullsLast(LocalDateTime::compareTo)))
                    .collect(Collectors.toList());
            for (int i = 0; i < sorted.size(); i++) {
                ScheduleItem it = sorted.get(i);
                it.setSortOrder(i + 1);
                scheduleItemMapper.updateById(it);
            }
        }

        pushUndo(beforeItems, created, orderBefore, "自动排程");
        if (created.size() > 0) {
            writeLog(null, null, onlyPending ? "AUTO_PLAN" : "REPLAN",
                    "自动排程完成，排程工序 " + created.size() + " 条，工单 " + candidates.size() + " 个");
        }
        log.info("自动排程完成: 工单={}, 工序={}, 窗口=[{} ~ {}]", candidates.size(), created.size(), winStart, winEnd);
        return created.size();
    }

    /**
     * 为单个工单按工艺模板拆分排产，返回新建的工序明细
     */
    private List<ScheduleItem> planOrder(WorkOrder order, LocalDateTime winStart, LocalDateTime winEnd,
                                         List<Shift> shifts, List<WorkCalendarDay> calendar,
                                         Map<Long, List<ScheduleItem>> itemsByWs, Map<Long, Long> wsLoad,
                                         Set<Long> wsIds) {
        List<ProcStep> steps = new ArrayList<>();
        if (order.getProcessTemplateId() != null) {
            steps = procStepMapper.selectList(
                    new LambdaQueryWrapper<ProcStep>()
                            .eq(ProcStep::getTemplateId, order.getProcessTemplateId())
                            .eq(ProcStep::getDeleted, 0)
                            .orderByAsc(ProcStep::getSequence));
        }
        List<ScheduleItem> created = new ArrayList<>();
        if (steps.isEmpty()) {
            ProcStep synthetic = new ProcStep();
            synthetic.setStepNo(1);
            synthetic.setStepName("整单生产");
            synthetic.setDurationMin(DEFAULT_STEP_DURATION_MIN);
            steps = List.of(synthetic);
        }

        int maxDurationStep = steps.stream()
                .mapToInt(s -> s.getDurationMin() == null ? DEFAULT_STEP_DURATION_MIN : s.getDurationMin())
                .max().orElse(DEFAULT_STEP_DURATION_MIN);

        LocalDateTime cursor = winStart;
        for (ProcStep step : steps) {
            Long wsId = pickLowestLoadWs(wsIds, wsLoad);
            if (wsId == null) {
                break;
            }
            int dur = step.getDurationMin() == null ? DEFAULT_STEP_DURATION_MIN : step.getDurationMin();
            LocalDateTime[] slot = findSlot(itemsByWs.getOrDefault(wsId, new ArrayList<>()),
                    cursor, dur, winStart, winEnd, shifts, calendar);
            if (slot == null) {
                break;
            }
            ScheduleItem item = new ScheduleItem();
            item.setWorkOrderId(order.getId());
            item.setStepId(step.getId());
            item.setStepNo(step.getStepNo());
            item.setStepName(step.getStepName());
            item.setWorkstationId(wsId);
            item.setDurationMin(dur);
            item.setPlannedStart(slot[0]);
            item.setPlannedEnd(slot[1]);
            item.setSortOrder(itemsByWs.getOrDefault(wsId, new ArrayList<>()).size() + 1);
            item.setStatus(STATUS_PLANNED);
            item.setBottleneck(dur == maxDurationStep ? 1 : 0);
            item.setOperatorId(getCurrentUserId());
            created.add(item);
            itemsByWs.computeIfAbsent(wsId, k -> new ArrayList<>()).add(item);
            wsLoad.merge(wsId, (long) dur, Long::sum);
            cursor = slot[1];
        }
        if (!created.isEmpty()) {
            // 同步工单
            order.setWorkstationId(created.get(0).getWorkstationId());
            order.setSortOrder(created.get(0).getSortOrder());
            order.setPlannedStartTime(created.stream().map(ScheduleItem::getPlannedStart)
                    .filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null));
            order.setPlannedEndTime(created.stream().map(ScheduleItem::getPlannedEnd)
                    .filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null));
            workOrderMapper.updateById(order);
        }
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void undo() {
        UndoSnapshot snap;
        synchronized (undoStack) {
            snap = undoStack.pollLast();
        }
        if (snap == null) {
            throw new IllegalStateException("没有可撤销的操作");
        }
        for (ScheduleItem it : snap.afterItems) {
            scheduleItemMapper.deleteById(it.getId());
        }
        for (ScheduleItem it : snap.beforeItems) {
            if (it.getId() != null) {
                ScheduleItem existing = scheduleItemMapper.selectById(it.getId());
                if (existing != null) {
                    it.setCreateTime(existing.getCreateTime());
                    scheduleItemMapper.updateById(it);
                } else {
                    scheduleItemMapper.insert(it);
                }
            }
        }
        for (WorkOrder wo : snap.beforeOrders) {
            workOrderMapper.updateById(wo);
        }
        writeLog(null, null, "UNDO", "撤销操作：" + snap.actionDesc);
        log.info("撤销操作: {}", snap.actionDesc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int freeze(PlanningFreezeRequestDTO dto) {
        List<ScheduleItem> targets = resolveTargets(dto);
        int cnt = 0;
        List<ScheduleItem> before = targets.stream().map(this::copyOf).collect(Collectors.toList());
        for (ScheduleItem it : targets) {
            if (STATUS_RELEASED.equals(it.getStatus())) {
                continue;
            }
            it.setStatus(STATUS_FROZEN);
            scheduleItemMapper.updateById(it);
            cnt++;
        }
        pushUndo(before, targets, new ArrayList<>(), "冻结排产");
        writeLog(dto.getWorkOrderId(), null, "FREEZE", "冻结排产 " + cnt + " 条");
        return cnt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unfreeze(PlanningFreezeRequestDTO dto) {
        List<ScheduleItem> targets = resolveTargets(dto);
        int cnt = 0;
        List<ScheduleItem> before = targets.stream().map(this::copyOf).collect(Collectors.toList());
        for (ScheduleItem it : targets) {
            if (!STATUS_FROZEN.equals(it.getStatus())) {
                continue;
            }
            it.setStatus(STATUS_PLANNED);
            scheduleItemMapper.updateById(it);
            cnt++;
        }
        pushUndo(before, targets, new ArrayList<>(), "解除冻结");
        writeLog(dto.getWorkOrderId(), null, "UNFREEZE", "解除冻结 " + cnt + " 条");
        return cnt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int release(Long workOrderId) {
        WorkOrder order = workOrderMapper.selectById(workOrderId);
        if (order == null || (order.getDeleted() != null && order.getDeleted() == 1)) {
            throw new IllegalStateException("工单不存在");
        }
        List<ScheduleItem> items = scheduleItemMapper.selectList(
                new LambdaQueryWrapper<ScheduleItem>()
                        .eq(ScheduleItem::getWorkOrderId, workOrderId)
                        .eq(ScheduleItem::getDeleted, 0));
        int cnt = 0;
        List<ScheduleItem> before = items.stream().map(this::copyOf).collect(Collectors.toList());
        for (ScheduleItem it : items) {
            if (STATUS_FROZEN.equals(it.getStatus())) {
                continue;
            }
            it.setStatus(STATUS_RELEASED);
            scheduleItemMapper.updateById(it);
            cnt++;
        }
        pushUndo(before, items, List.of(copyOf(order)), "下发排产");
        writeLog(workOrderId, null, "RELEASE", "下发排产工单 " + order.getOrderNo() + "，工序 " + cnt + " 条");
        return cnt;
    }

    // ==================== 内部工具 ====================

    private List<ScheduleItem> resolveTargets(PlanningFreezeRequestDTO dto) {
        List<ScheduleItem> result = new ArrayList<>();
        if (dto.getScheduleIds() != null && !dto.getScheduleIds().isEmpty()) {
            result = scheduleItemMapper.selectBatchIds(dto.getScheduleIds());
        } else if (dto.getWorkstationId() != null) {
            result = scheduleItemMapper.selectList(new LambdaQueryWrapper<ScheduleItem>()
                    .eq(ScheduleItem::getWorkstationId, dto.getWorkstationId())
                    .eq(ScheduleItem::getDeleted, 0));
        } else if (dto.getWorkOrderId() != null) {
            result = scheduleItemMapper.selectList(new LambdaQueryWrapper<ScheduleItem>()
                    .eq(ScheduleItem::getWorkOrderId, dto.getWorkOrderId())
                    .eq(ScheduleItem::getDeleted, 0));
        }
        return result;
    }

    private void checkConflict(Long workstationId, List<ScheduleItem> affected, Long workOrderId) {
        List<ScheduleItem> others = scheduleItemMapper.selectList(
                new LambdaQueryWrapper<ScheduleItem>()
                        .eq(ScheduleItem::getWorkstationId, workstationId)
                        .eq(ScheduleItem::getDeleted, 0));
        for (ScheduleItem target : affected) {
            if (target.getPlannedStart() == null || target.getPlannedEnd() == null) {
                continue;
            }
            for (ScheduleItem other : others) {
                if (Objects.equals(other.getId(), target.getId())
                        || Objects.equals(other.getWorkOrderId(), workOrderId)) {
                    continue;
                }
                if (other.getPlannedStart() == null || other.getPlannedEnd() == null) {
                    continue;
                }
                if (target.getPlannedStart().isBefore(other.getPlannedEnd())
                        && target.getPlannedEnd().isAfter(other.getPlannedStart())) {
                    WorkOrder otherWo = workOrderMapper.selectById(other.getWorkOrderId());
                    String no = otherWo != null ? otherWo.getOrderNo() : String.valueOf(other.getWorkOrderId());
                    throw new IllegalStateException("时间冲突：与工单[" + no + "]工序[" + other.getStepName()
                            + "]重叠（" + other.getPlannedStart() + " ~ " + other.getPlannedEnd() + "）");
                }
            }
        }
    }

    /**
     * 未排产工单拖入设备：按工艺模板创建工序明细
     */
    private List<ScheduleItem> createPlanFromTemplate(WorkOrder order, PlanningMoveRequestDTO dto) {
        List<ProcStep> steps = new ArrayList<>();
        if (order.getProcessTemplateId() != null) {
            steps = procStepMapper.selectList(
                    new LambdaQueryWrapper<ProcStep>()
                            .eq(ProcStep::getTemplateId, order.getProcessTemplateId())
                            .eq(ProcStep::getDeleted, 0)
                            .orderByAsc(ProcStep::getSequence));
        }
        if (steps.isEmpty()) {
            ProcStep synthetic = new ProcStep();
            synthetic.setStepNo(1);
            synthetic.setStepName("整单生产");
            synthetic.setDurationMin(dto.getDurationMin() != null ? dto.getDurationMin() : DEFAULT_STEP_DURATION_MIN);
            steps = List.of(synthetic);
        }
        int maxDurationStep = steps.stream()
                .mapToInt(s -> s.getDurationMin() == null ? DEFAULT_STEP_DURATION_MIN : s.getDurationMin())
                .max().orElse(DEFAULT_STEP_DURATION_MIN);
        LocalDateTime cursor = dto.getNewStart() != null ? dto.getNewStart() : LocalDateTime.now().withNano(0);
        List<ScheduleItem> created = new ArrayList<>();
        int seq = 0;
        for (ProcStep step : steps) {
            seq++;
            int dur = step.getDurationMin() == null ? DEFAULT_STEP_DURATION_MIN : step.getDurationMin();
            if (step == steps.get(steps.size() - 1) && dto.getDurationMin() != null && steps.size() == 1) {
                dur = dto.getDurationMin();
            }
            ScheduleItem item = new ScheduleItem();
            item.setWorkOrderId(order.getId());
            item.setStepId(step.getId());
            item.setStepNo(step.getStepNo());
            item.setStepName(step.getStepName());
            item.setWorkstationId(dto.getTargetWorkstationId());
            item.setDurationMin(dur);
            item.setPlannedStart(cursor);
            item.setPlannedEnd(cursor.plusMinutes(dur));
            item.setSortOrder(seq);
            item.setStatus(STATUS_PLANNED);
            item.setBottleneck(dur == maxDurationStep ? 1 : 0);
            item.setOperatorId(getCurrentUserId());
            scheduleItemMapper.insert(item);
            created.add(item);
            cursor = item.getPlannedEnd();
        }
        order.setWorkstationId(dto.getTargetWorkstationId());
        order.setSortOrder(1);
        order.setPlannedStartTime(created.get(0).getPlannedStart());
        order.setPlannedEndTime(created.get(created.size() - 1).getPlannedEnd());
        workOrderMapper.updateById(order);
        return created;
    }

    private void refreshOrderFromItems(WorkOrder order, List<ScheduleItem> items) {
        LocalDateTime min = items.stream().map(ScheduleItem::getPlannedStart)
                .filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(order.getPlannedStartTime());
        LocalDateTime max = items.stream().map(ScheduleItem::getPlannedEnd)
                .filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(order.getPlannedEndTime());
        order.setPlannedStartTime(min);
        order.setPlannedEndTime(max);
        // 工单主设备 = 第一道工序（step_no最小）所在设备
        items.stream()
                .filter(i -> i.getWorkstationId() != null)
                .min(Comparator.comparing(ScheduleItem::getStepNo,
                        Comparator.nullsLast(Integer::compareTo)))
                .ifPresent(first -> {
                    order.setWorkstationId(first.getWorkstationId());
                    order.setSortOrder(first.getSortOrder() == null ? 0 : first.getSortOrder());
                });
    }

    private void pushUndo(List<ScheduleItem> before, List<ScheduleItem> after, List<WorkOrder> orders, String desc) {
        synchronized (undoStack) {
            undoStack.addLast(new UndoSnapshot(before, after, orders, desc));
            while (undoStack.size() > UNDO_LIMIT) {
                undoStack.pollFirst();
            }
        }
    }

    private void writeLog(Long workOrderId, Long scheduleId, String action, String desc) {
        try {
            ScheduleLog logEntry = new ScheduleLog();
            logEntry.setWorkOrderId(workOrderId);
            logEntry.setScheduleId(scheduleId);
            logEntry.setAction(action);
            logEntry.setActionDesc(desc);
            logEntry.setOperatorId(getCurrentUserId());
            logEntry.setCreateTime(LocalDateTime.now());
            scheduleLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("写入排产日志失败: {}", e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        return 1L;
    }

    // ==================== APS 排程算法 ====================

    private int priorityRank(String priority) {
        if (priority == null) {
            return 2;
        }
        return switch (priority.toUpperCase()) {
            case "HIGH" -> 3;
            case "LOW" -> 1;
            default -> 2;
        };
    }

    private Long pickLowestLoadWs(Set<Long> wsIds, Map<Long, Long> wsLoad) {
        return wsIds.stream()
                .min(Comparator.comparingLong(w -> wsLoad.getOrDefault(w, 0L)))
                .orElse(null);
    }

    /**
     * 在目标设备上从from起找时长duration的可用工作槽位（尊重工作日历+班次+已占用）
     *
     * @return [start, end] 或 null
     */
    private LocalDateTime[] findSlot(List<ScheduleItem> occupied, LocalDateTime from, int duration,
                                     LocalDateTime winStart, LocalDateTime winEnd,
                                     List<Shift> shifts, List<WorkCalendarDay> calendar) {
        List<LocalDateTime[]> busy = buildBusyIntervals(occupied, winStart, winEnd, shifts, calendar);
        LocalDateTime cursor = from.isAfter(winStart) ? from : winStart;
        if (cursor.isAfter(winEnd)) {
            return null;
        }
        for (LocalDateTime[] b : busy) {
            if (cursor.isAfter(b[1])) {
                continue;
            }
            if (!cursor.isBefore(b[0])) {
                cursor = b[1];
                continue;
            }
            long gap = Duration.between(cursor, b[0]).toMinutes();
            if (gap >= duration) {
                return new LocalDateTime[]{cursor, cursor.plusMinutes(duration)};
            }
            cursor = b[1];
        }
        long tail = Duration.between(cursor, winEnd).toMinutes();
        if (tail >= duration) {
            return new LocalDateTime[]{cursor, cursor.plusMinutes(duration)};
        }
        return null;
    }

    /**
     * 构建忙碌区间（非工作时间 + 已占用），区间不重叠且升序
     */
    private List<LocalDateTime[]> buildBusyIntervals(List<ScheduleItem> occupied,
                                                     LocalDateTime winStart, LocalDateTime winEnd,
                                                     List<Shift> shifts, List<WorkCalendarDay> calendar) {
        Map<LocalDate, WorkCalendarDay> calMap = calendar.stream()
                .collect(Collectors.toMap(WorkCalendarDay::getWorkDate, d -> d, (a, b) -> a));
        List<LocalDateTime[]> busy = new ArrayList<>();

        // 非工作时间
        LocalDate day = winStart.toLocalDate();
        while (!day.isAfter(winEnd.toLocalDate())) {
            WorkCalendarDay cal = calMap.get(day);
            boolean workday = cal == null || cal.getIsWorkday() == null || cal.getIsWorkday() == 1;
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
            if (!workday) {
                busy.add(new LocalDateTime[]{dayStart, dayEnd});
            } else {
                List<LocalDateTime[]> wins = dayWorkingWindows(day, shifts);
                LocalDateTime prev = dayStart;
                for (LocalDateTime[] w : wins) {
                    if (w[0].isAfter(prev)) {
                        busy.add(new LocalDateTime[]{prev, w[0]});
                    }
                    prev = w[1];
                }
                if (prev.isBefore(dayEnd)) {
                    busy.add(new LocalDateTime[]{prev, dayEnd});
                }
            }
            day = day.plusDays(1);
        }

        // 已占用
        for (ScheduleItem it : occupied) {
            if (it.getPlannedStart() == null || it.getPlannedEnd() == null
                    || !it.getPlannedEnd().isAfter(winStart) || !it.getPlannedStart().isBefore(winEnd)) {
                continue;
            }
            LocalDateTime s = it.getPlannedStart().isBefore(winStart) ? winStart : it.getPlannedStart();
            LocalDateTime e = it.getPlannedEnd().isAfter(winEnd) ? winEnd : it.getPlannedEnd();
            if (e.isAfter(s)) {
                busy.add(new LocalDateTime[]{s, e});
            }
        }

        // 合并重叠区间
        busy.sort(Comparator.comparing(b -> b[0]));
        List<LocalDateTime[]> merged = new ArrayList<>();
        for (LocalDateTime[] b : busy) {
            if (merged.isEmpty() || !b[0].isBefore(merged.get(merged.size() - 1)[1])) {
                merged.add(b);
            } else if (b[1].isAfter(merged.get(merged.size() - 1)[1])) {
                merged.get(merged.size() - 1)[1] = b[1];
            }
        }
        return merged;
    }

    /**
     * 一天的班次工作窗口（跨午夜班次拆分为两段）
     */
    private List<LocalDateTime[]> dayWorkingWindows(LocalDate day, List<Shift> shifts) {
        List<LocalDateTime[]> wins = new ArrayList<>();
        for (Shift s : shifts) {
            if (s.getIsWork() == null || s.getIsWork() != 1 || s.getStartTime() == null || s.getEndTime() == null) {
                continue;
            }
            LocalDateTime start = day.atTime(s.getStartTime());
            LocalDateTime end = day.atTime(s.getEndTime());
            if (!end.isAfter(start)) {
                // 跨午夜班次：当晚一段 + 次日凌晨一段
                LocalDateTime midnight = day.plusDays(1).atStartOfDay();
                wins.add(new LocalDateTime[]{start, midnight});
                wins.add(new LocalDateTime[]{midnight, midnight.plusHours(s.getEndTime().getHour()).plusMinutes(s.getEndTime().getMinute())});
            } else {
                wins.add(new LocalDateTime[]{start, end});
            }
        }
        wins.sort(Comparator.comparing(w -> w[0]));
        return wins;
    }

    /**
     * 窗口内可用工作分钟
     */
    private long countWorkingMinutes(LocalDateTime winStart, LocalDateTime winEnd,
                                     List<Shift> shifts, List<WorkCalendarDay> calendar) {
        Map<LocalDate, WorkCalendarDay> calMap = calendar.stream()
                .collect(Collectors.toMap(WorkCalendarDay::getWorkDate, d -> d, (a, b) -> a));
        long total = 0;
        LocalDate day = winStart.toLocalDate();
        while (!day.isAfter(winEnd.toLocalDate())) {
            WorkCalendarDay cal = calMap.get(day);
            boolean workday = cal == null || cal.getIsWorkday() == null || cal.getIsWorkday() == 1;
            if (workday) {
                for (LocalDateTime[] w : dayWorkingWindows(day, shifts)) {
                    if (w[0].isAfter(winEnd) || w[1].isBefore(winStart)) {
                        continue;
                    }
                    LocalDateTime s = w[0].isBefore(winStart) ? winStart : w[0];
                    LocalDateTime e = w[1].isAfter(winEnd) ? winEnd : w[1];
                    total += Math.max(0, Duration.between(s, e).toMinutes());
                }
            }
            day = day.plusDays(1);
        }
        return total;
    }

    // ==================== 快照 ====================

    private ScheduleItem copyOf(ScheduleItem src) {
        if (src == null) {
            return null;
        }
        ScheduleItem c = new ScheduleItem();
        c.setId(src.getId());
        c.setWorkOrderId(src.getWorkOrderId());
        c.setStepId(src.getStepId());
        c.setStepNo(src.getStepNo());
        c.setStepName(src.getStepName());
        c.setWorkstationId(src.getWorkstationId());
        c.setDurationMin(src.getDurationMin());
        c.setPlannedStart(src.getPlannedStart());
        c.setPlannedEnd(src.getPlannedEnd());
        c.setSortOrder(src.getSortOrder());
        c.setStatus(src.getStatus());
        c.setBottleneck(src.getBottleneck());
        c.setOperatorId(src.getOperatorId());
        c.setCreateTime(src.getCreateTime());
        c.setUpdateTime(src.getUpdateTime());
        c.setDeleted(src.getDeleted());
        return c;
    }

    private WorkOrder copyOf(WorkOrder src) {
        if (src == null) {
            return null;
        }
        WorkOrder c = new WorkOrder();
        c.setId(src.getId());
        c.setOrderNo(src.getOrderNo());
        c.setProductName(src.getProductName());
        c.setProductModel(src.getProductModel());
        c.setPlanQuantity(src.getPlanQuantity());
        c.setCompletedQuantity(src.getCompletedQuantity());
        c.setStatus(src.getStatus());
        c.setWorkstationId(src.getWorkstationId());
        c.setProcessTemplateId(src.getProcessTemplateId());
        c.setPriority(src.getPriority());
        c.setSortOrder(src.getSortOrder());
        c.setPlannedStartTime(src.getPlannedStartTime());
        c.setPlannedEndTime(src.getPlannedEndTime());
        c.setActualStartTime(src.getActualStartTime());
        c.setActualEndTime(src.getActualEndTime());
        c.setRemark(src.getRemark());
        c.setCreateBy(src.getCreateBy());
        c.setIssueBy(src.getIssueBy());
        c.setCreateTime(src.getCreateTime());
        c.setUpdateTime(src.getUpdateTime());
        c.setDeleted(src.getDeleted());
        return c;
    }

    /** 撤销快照 */
    @lombok.AllArgsConstructor
    @lombok.Getter
    private static class UndoSnapshot {
        private final List<ScheduleItem> beforeItems;
        private final List<ScheduleItem> afterItems;
        private final List<WorkOrder> beforeOrders;
        private final String actionDesc;
    }
}
