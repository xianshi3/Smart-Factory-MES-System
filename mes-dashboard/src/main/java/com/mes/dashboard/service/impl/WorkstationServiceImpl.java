package com.mes.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.entity.Workstation;
import com.mes.common.mapper.WorkstationMapper;
import com.mes.dashboard.service.WorkstationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkstationServiceImpl implements WorkstationService {

    private final WorkstationMapper workstationMapper;

    @Override
    public Workstation createWorkstation(Workstation station) {
        station.setDeleted(0);
        workstationMapper.insert(station);
        log.info("Workstation created: {}", station.getWorkstationCode());
        return station;
    }

    @Override
    public Workstation updateWorkstation(Workstation station) {
        workstationMapper.updateById(station);
        log.info("Workstation updated: {}", station.getId());
        return station;
    }

    @Override
    public void deleteWorkstation(Long id) {
        workstationMapper.deleteById(id);
        log.info("Workstation deleted: {}", id);
    }

    @Override
    public Workstation getWorkstation(Long id) {
        return workstationMapper.selectById(id);
    }

    @Override
    public List<Workstation> listWorkstations() {
        return workstationMapper.selectList(
            new LambdaQueryWrapper<Workstation>()
                .eq(Workstation::getDeleted, 0)
                .orderByAsc(Workstation::getCreateTime)
        );
    }
}
