package com.mes.dashboard.service;

import com.mes.common.entity.Workstation;

import java.util.List;

public interface WorkstationService {

    Workstation createWorkstation(Workstation station);

    Workstation updateWorkstation(Workstation station);

    void deleteWorkstation(Long id);

    Workstation getWorkstation(Long id);

    List<Workstation> listWorkstations();
}
