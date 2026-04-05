package com.mes.workorder.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkOrderStatusEnum {
    CREATED("CREATED", "已创建"),
    ISSUED("ISSUED", "已下发"),
    IN_PRODUCTION("IN_PRODUCTION", "生产中"),
    PENDING_QC("PENDING_QC", "待质检"),
    COMPLETED("COMPLETED", "已完成"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String desc;

    public boolean canTransitionTo(WorkOrderStatusEnum target) {
        return switch (this) {
            case CREATED -> target == ISSUED || target == CLOSED;
            case ISSUED -> target == IN_PRODUCTION || target == CLOSED;
            case IN_PRODUCTION -> target == PENDING_QC || target == COMPLETED || target == CLOSED;
            case PENDING_QC -> target == COMPLETED || target == CLOSED;
            case COMPLETED -> target == CLOSED;
            case CLOSED -> false;
        };
    }
}
