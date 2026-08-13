package com.mes.workorder.dto.planning;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 排产看板 - 自动排程请求
 */
@Data
public class PlanningAutoPlanDTO {

    /** 排程窗口开始（默认明天0点） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.LocalDateTime windowStart;

    /** 排程窗口结束（默认+14天） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.LocalDateTime windowEnd;

    /** 是否仅排未排产的工单（默认true；false=全部重排） */
    private Boolean onlyPending;
}
