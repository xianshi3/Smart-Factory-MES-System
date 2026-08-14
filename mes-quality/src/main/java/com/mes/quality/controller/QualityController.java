package com.mes.quality.controller;

import com.mes.common.result.PageResult;
import com.mes.common.result.Result;
import com.mes.common.security.RequirePermission;
import com.mes.quality.dto.CreateQualityRecordDTO;
import com.mes.quality.dto.TraceDetailVO;
import com.mes.quality.dto.TraceQueryDTO;
import com.mes.quality.entity.QualityRecord;
import com.mes.quality.entity.Traceability;
import com.mes.quality.service.QualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 质量管理控制器
 * @author MES
 * @description 质检记录与追溯REST API接口
 */
@RestController
@RequestMapping("/quality")
@RequiredArgsConstructor
@Tag(name = "质量管理", description = "质检记录与追溯管理接口")
@RequirePermission("quality:view")
public class QualityController {

    private final QualityService qualityService;

    /**
     * 创建质检记录
     * @param dto 创建质检记录DTO
     * @return 记录ID
     */
    @PostMapping("/record")
    @Operation(summary = "创建质检记录")
    @RequirePermission("quality:create")
    public Result<Long> createRecord(@Valid @RequestBody CreateQualityRecordDTO dto) {
        return Result.ok(qualityService.createRecord(dto));
    }

    /**
     * 删除质检记录
     * @param id 记录ID
     * @return 结果
     */
    @DeleteMapping("/record/{id}")
    @Operation(summary = "删除质检记录")
    @RequirePermission("quality:delete")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        Long userId = com.mes.common.security.UserContext.getUserId();
        qualityService.deleteRecord(id, userId);
        return Result.ok();
    }

    /**
     * 查询质检详情
     * @param id 记录ID
     * @return 质检记录
     */
    @GetMapping("/record/{id}")
    @Operation(summary = "查询质检详情")
    public Result<QualityRecord> getDetail(@PathVariable Long id) {
        return Result.ok(qualityService.getDetail(id));
    }

    /**
     * 质检通过
     * @param id 记录ID
     * @return 结果
     */
    @PostMapping("/record/{id}/pass")
    @Operation(summary = "质检通过")
    @RequirePermission("quality:create")
    public Result<Void> pass(@PathVariable Long id) {
        qualityService.pass(id);
        return Result.ok();
    }

    /**
     * 质检不通过
     * @param id 记录ID
     * @param reason 不通过原因
     * @return 结果
     */
    @PostMapping("/record/{id}/fail")
    @Operation(summary = "质检不通过")
    @RequirePermission("quality:create")
    public Result<Void> fail(@PathVariable Long id, @RequestParam String reason) {
        qualityService.fail(id, reason);
        return Result.ok();
    }

    /**
     * 正向追溯 - SN->工单->工艺链路->质量结果
     * @param sn 产品序列号
     * @return 追溯详情（含工序链路）
     */
    @GetMapping("/trace/forward")
    @Operation(summary = "正向追溯 - SN->工单->工艺->物料")
    public Result<TraceDetailVO> forwardTrace(@RequestParam String sn) {
        return Result.ok(qualityService.forwardTrace(sn));
    }

    /**
     * 反向追溯 - 工单->所有SN记录
     * @param workOrderId 工单ID
     * @return 追溯记录列表
     */
    @GetMapping("/trace/reverse")
    @Operation(summary = "反向追溯 - 工单->所有SN记录")
    public Result<List<Traceability>> reverseTrace(@RequestParam Long workOrderId) {
        return Result.ok(qualityService.reverseTrace(workOrderId));
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
    @GetMapping("/record/page")
    @Operation(summary = "分页查询质检记录")
    public Result<PageResult<QualityRecord>> queryPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String checkType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String keyword) {
        // 防止 size 过大拖垮数据库
        size = Math.min(Math.max(size, 1), 100);
        return Result.ok(qualityService.queryPage(current, size, checkType, result, keyword));
    }
}
