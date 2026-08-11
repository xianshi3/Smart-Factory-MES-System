package com.mes.process.controller;

import com.mes.common.result.PageResult;
import com.mes.common.result.Result;
import com.mes.process.dto.CreateTemplateDTO;
import com.mes.process.dto.ParameterCheckDTO;
import com.mes.process.dto.ParameterDTO;
import com.mes.process.dto.StepDTO;
import com.mes.process.dto.TemplateDetailVO;
import com.mes.process.entity.ProcessParameter;
import com.mes.process.entity.ProcessStep;
import com.mes.process.entity.ProcessTemplate;
import com.mes.process.service.ProcessTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工艺模板控制器
 * @author MES
 * @description 工艺模板REST API接口
 */
@Tag(name = "工艺模板管理")
@RestController
@RequestMapping("/process/template")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessTemplateService processTemplateService;

    /**
     * 创建工艺模板
     * @param dto 创建模板DTO
     * @return 模板ID
     */
    @Operation(summary = "创建工艺模板")
    @PostMapping
    public Result<Long> create(@RequestBody CreateTemplateDTO dto) {
        Long id = processTemplateService.create(dto);
        return Result.ok(id);
    }

    /**
     * 查询模板详情
     * @param id 模板ID
     * @return 模板对象
     */
    @Operation(summary = "查询模板详情")
    @GetMapping("/{id}")
    public Result<ProcessTemplate> getById(@PathVariable Long id) {
        ProcessTemplate template = processTemplateService.getById(id);
        if (template == null) {
            return Result.fail(404, "模板不存在");
        }
        return Result.ok(template);
    }

    /**
     * 查询模板详情（含参数与工序步骤）
     * @param id 模板ID
     * @return 模板详情
     */
    @Operation(summary = "查询模板详情(含参数与工序)")
    @GetMapping("/{id}/detail")
    public Result<TemplateDetailVO> getDetail(@PathVariable Long id) {
        return Result.ok(processTemplateService.getDetail(id));
    }

    /**
     * 复制工艺模板
     * @param id 模板ID
     * @return 新模板ID
     */
    @Operation(summary = "复制工艺模板")
    @PostMapping("/{id}/copy")
    public Result<Long> copy(@PathVariable Long id) {
        return Result.ok(processTemplateService.copy(id));
    }

    /**
     * 更新模板
     * @param id 模板ID
     * @param dto 更新内容
     * @return 结果
     */
    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody CreateTemplateDTO dto) {
        processTemplateService.updateTemplate(id, dto);
        return Result.ok();
    }

    /**
     * 发布模板
     * @param id 模板ID
     * @return 结果
     */
    @Operation(summary = "发布模板")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        processTemplateService.publish(id);
        return Result.ok();
    }

    /**
     * 参数实时校验
     * @param dto 参数校验DTO
     * @return 校验结果
     */
    @Operation(summary = "参数实时校验")
    @PostMapping("/parameter/check")
    public Result<?> checkParameters(@RequestBody ParameterCheckDTO dto) {
        return processTemplateService.checkParameters(dto);
    }

    /**
     * 分页查询模板
     * @param current 当前页
     * @param size 每页大小
     * @param status 状态筛选
     * @param keyword 关键字
     * @return 分页结果
     */
    @Operation(summary = "分页查询模板")
    @GetMapping("/page")
    public Result<PageResult<ProcessTemplate>> queryPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(processTemplateService.queryPage(current, size, status, keyword));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工艺模板")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return processTemplateService.delete(id, userId);
    }

    // ==================== 工艺参数管理 ====================

    @Operation(summary = "查询模板参数列表")
    @GetMapping("/{id}/parameters")
    public Result<List<ProcessParameter>> listParameters(@PathVariable Long id) {
        return Result.ok(processTemplateService.listParameters(id));
    }

    @Operation(summary = "新增工艺参数")
    @PostMapping("/{id}/parameters")
    public Result<Long> addParameter(@PathVariable Long id, @RequestBody ParameterDTO dto) {
        dto.setTemplateId(id);
        return Result.ok(processTemplateService.addParameter(dto));
    }

    @Operation(summary = "更新工艺参数")
    @PutMapping("/parameters/{paramId}")
    public Result<Void> updateParameter(@PathVariable Long paramId, @RequestBody ParameterDTO dto) {
        processTemplateService.updateParameter(paramId, dto);
        return Result.ok();
    }

    @Operation(summary = "删除工艺参数")
    @DeleteMapping("/parameters/{paramId}")
    public Result<Void> deleteParameter(@PathVariable Long paramId) {
        processTemplateService.deleteParameter(paramId);
        return Result.ok();
    }

    // ==================== 工序步骤管理 ====================

    @Operation(summary = "查询模板工序步骤列表")
    @GetMapping("/{id}/steps")
    public Result<List<ProcessStep>> listSteps(@PathVariable Long id) {
        return Result.ok(processTemplateService.listSteps(id));
    }

    @Operation(summary = "新增工序步骤")
    @PostMapping("/{id}/steps")
    public Result<Long> addStep(@PathVariable Long id, @RequestBody StepDTO dto) {
        dto.setTemplateId(id);
        return Result.ok(processTemplateService.addStep(dto));
    }

    @Operation(summary = "更新工序步骤")
    @PutMapping("/steps/{stepId}")
    public Result<Void> updateStep(@PathVariable Long stepId, @RequestBody StepDTO dto) {
        processTemplateService.updateStep(stepId, dto);
        return Result.ok();
    }

    @Operation(summary = "删除工序步骤")
    @DeleteMapping("/steps/{stepId}")
    public Result<Void> deleteStep(@PathVariable Long stepId) {
        processTemplateService.deleteStep(stepId);
        return Result.ok();
    }
}
