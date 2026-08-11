package com.mes.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.mes.process.mapper.ProcessParameterMapper;
import com.mes.process.mapper.ProcessStepMapper;
import com.mes.process.mapper.ProcessTemplateMapper;
import com.mes.process.service.ProcessTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 工艺模板服务实现类
 * @author MES
 * @description 工艺模板业务逻辑实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessTemplateServiceImpl implements ProcessTemplateService {

    private final ProcessTemplateMapper processTemplateMapper;
    private final ProcessParameterMapper processParameterMapper;
    private final ProcessStepMapper processStepMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String TEMPLATE_CACHE_PREFIX = "mes:process:template:";

    /**
     * 创建工艺模板
     * @param dto 创建模板DTO
     * @return 模板ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateTemplateDTO dto) {
        // 检查模板编码是否已存在
        Long count = processTemplateMapper.selectCount(
            new LambdaQueryWrapper<ProcessTemplate>()
                .eq(ProcessTemplate::getTemplateCode, dto.getTemplateCode())
        );
        if (count > 0) {
            throw new RuntimeException("模板编码 " + dto.getTemplateCode() + " 已存在，请使用不同的编码");
        }

        ProcessTemplate template = new ProcessTemplate();
        template.setDeleted(0);
        template.setTemplateName(dto.getTemplateName());
        template.setTemplateCode(dto.getTemplateCode());
        template.setProductModel(dto.getProductModel());
        template.setDescription(dto.getDescription());
        template.setStatus("DRAFT");
        processTemplateMapper.insert(template);

        if (dto.getParameters() != null && !dto.getParameters().isEmpty()) {
            List<ProcessParameter> parameters = new ArrayList<>();
            for (int i = 0; i < dto.getParameters().size(); i++) {
                CreateTemplateDTO.ParameterItem item = dto.getParameters().get(i);
                ProcessParameter param = new ProcessParameter();
                param.setTemplateId(template.getId());
                param.setParamName(item.getParamName());
                param.setParamCode(item.getParamCode());
                param.setParamValue(item.getParamValue());
                param.setMinValue(item.getMinValue());
                param.setMaxValue(item.getMaxValue());
                param.setUnit(item.getUnit());
                param.setSortOrder(i + 1);
                parameters.add(param);
            }
            parameters.forEach(processParameterMapper::insert);
        }

        log.info("创建工艺模板成功: id={}, name={}", template.getId(), template.getTemplateName());
        return template.getId();
    }

    /**
     * 根据ID查询模板
     * @param id 模板ID
     * @return 模板对象
     */
    @Override
    public ProcessTemplate getById(Long id) {
        return processTemplateMapper.selectById(id);
    }

    /**
     * 查询模板详情（含参数与工序步骤）
     */
    @Override
    public TemplateDetailVO getDetail(Long id) {
        ProcessTemplate template = processTemplateMapper.selectById(id);
        if (template == null) {
            throw new RuntimeException("工艺模板不存在: " + id);
        }
        TemplateDetailVO vo = new TemplateDetailVO();
        vo.setTemplate(template);
        vo.setParameters(processParameterMapper.selectList(
                new LambdaQueryWrapper<ProcessParameter>()
                        .eq(ProcessParameter::getTemplateId, id)
                        .orderByAsc(ProcessParameter::getSortOrder)));
        vo.setSteps(processStepMapper.selectList(
                new LambdaQueryWrapper<ProcessStep>()
                        .eq(ProcessStep::getTemplateId, id)
                        .orderByAsc(ProcessStep::getSequence)));
        return vo;
    }

    /**
     * 更新工艺模板
     * @param id 模板ID
     * @param dto 更新内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long id, CreateTemplateDTO dto) {
        ProcessTemplate template = processTemplateMapper.selectById(id);
        if (template == null) {
            throw new RuntimeException("工艺模板不存在: " + id);
        }
        if ("PUBLISHED".equals(template.getStatus())) {
            throw new RuntimeException("已发布的模板不能直接修改，请先创建新版本");
        }

        var updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ProcessTemplate>()
                .set("template_name", dto.getTemplateName())
                .set("template_code", dto.getTemplateCode())
                .set("product_model", dto.getProductModel())
                .set("description", dto.getDescription())
                .eq("id", id);
        processTemplateMapper.update(null, updateWrapper);

        LambdaQueryWrapper<ProcessParameter> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ProcessParameter::getTemplateId, id);
        processParameterMapper.delete(deleteWrapper);

        if (dto.getParameters() != null && !dto.getParameters().isEmpty()) {
            List<ProcessParameter> parameters = new ArrayList<>();
            for (int i = 0; i < dto.getParameters().size(); i++) {
                CreateTemplateDTO.ParameterItem item = dto.getParameters().get(i);
                ProcessParameter param = new ProcessParameter();
                param.setTemplateId(id);
                param.setParamName(item.getParamName());
                param.setParamCode(item.getParamCode());
                param.setParamValue(item.getParamValue());
                param.setMinValue(item.getMinValue());
                param.setMaxValue(item.getMaxValue());
                param.setUnit(item.getUnit());
                param.setSortOrder(i + 1);
                parameters.add(param);
            }
            parameters.forEach(processParameterMapper::insert);
        }

        log.info("更新工艺模板成功: id={}", id);
    }

    /**
     * 发布工艺模板
     * @param id 模板ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        ProcessTemplate template = processTemplateMapper.selectById(id);
        if (template == null) {
            throw new RuntimeException("工艺模板不存在: " + id);
        }
        template.setStatus("PUBLISHED");
        processTemplateMapper.updateById(template);

        stringRedisTemplate.opsForValue().set(
                TEMPLATE_CACHE_PREFIX + id,
                "PUBLISHED",
                24,
                TimeUnit.HOURS
        );

        log.info("发布工艺模板成功: id={}", id);
    }

    /**
     * 复制工艺模板（含参数与工序步骤）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id) {
        ProcessTemplate template = processTemplateMapper.selectById(id);
        if (template == null) {
            throw new RuntimeException("工艺模板不存在: " + id);
        }
        ProcessTemplate copy = new ProcessTemplate();
        copy.setDeleted(0);
        copy.setTemplateName(template.getTemplateName() + "-副本");
        copy.setTemplateCode(template.getTemplateCode() + "-COPY-" + System.currentTimeMillis() % 10000);
        copy.setProductModel(template.getProductModel());
        copy.setDescription(template.getDescription());
        copy.setStatus("DRAFT");
        processTemplateMapper.insert(copy);
        Long newId = copy.getId();

        List<ProcessParameter> params = processParameterMapper.selectList(
                new LambdaQueryWrapper<ProcessParameter>().eq(ProcessParameter::getTemplateId, id));
        for (int i = 0; i < params.size(); i++) {
            ProcessParameter p = params.get(i);
            p.setId(null);
            p.setTemplateId(newId);
            p.setSortOrder(p.getSortOrder() != null ? p.getSortOrder() : i + 1);
            processParameterMapper.insert(p);
        }

        List<ProcessStep> steps = processStepMapper.selectList(
                new LambdaQueryWrapper<ProcessStep>().eq(ProcessStep::getTemplateId, id));
        for (ProcessStep s : steps) {
            s.setId(null);
            s.setTemplateId(newId);
            processStepMapper.insert(s);
        }

        log.info("复制工艺模板成功: {} -> {}", id, newId);
        return newId;
    }

    /**
     * 校验参数
     * @param dto 参数校验DTO
     * @return 校验结果
     */
    @Override
    public Result<?> checkParameters(ParameterCheckDTO dto) {
        LambdaQueryWrapper<ProcessParameter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessParameter::getTemplateId, dto.getTemplateId())
               .orderByAsc(ProcessParameter::getSortOrder);
        List<ProcessParameter> parameters = processParameterMapper.selectList(wrapper);

        if (parameters == null || parameters.isEmpty()) {
            return Result.error("模板参数配置为空");
        }

        // 请求参数值可能为空，此时回退使用模板默认值校验（null 防护）
        Map<String, Double> inputValues = dto.getParamValues() != null ? dto.getParamValues() : new HashMap<>();
        List<String> errors = new ArrayList<>();

        for (ProcessParameter param : parameters) {
            Double value = inputValues.get(param.getParamCode());
            if (value == null && param.getParamValue() != null && !param.getParamValue().trim().isEmpty()) {
                try {
                    value = Double.parseDouble(param.getParamValue().trim());
                } catch (NumberFormatException ignored) {
                    // 默认值不是数字时忽略，按未提供处理
                }
            }
            if (value == null) {
                errors.add("参数[" + param.getParamName() + "]未提供值且无默认值");
                continue;
            }
            if (param.getMinValue() != null && value < param.getMinValue()) {
                errors.add("参数[" + param.getParamName() + "]值" + value +
                        "低于下限" + param.getMinValue() + (param.getUnit() != null ? param.getUnit() : ""));
            }
            if (param.getMaxValue() != null && value > param.getMaxValue()) {
                errors.add("参数[" + param.getParamName() + "]值" + value +
                        "超过上限" + param.getMaxValue() + (param.getUnit() != null ? param.getUnit() : ""));
            }
        }

        if (!errors.isEmpty()) {
            return Result.fail(400, "参数校验不通过: " + String.join(", ", errors));
        }

        return Result.ok("参数校验通过");
    }

    /**
     * 分页查询模板
     * @param current 当前页
     * @param size 每页大小
     * @param status 状态筛选
     * @param keyword 关键字
     * @return 分页结果
     */
    @Override
    public PageResult<ProcessTemplate> queryPage(int current, int size, String status, String keyword) {
        Page<ProcessTemplate> page = new Page<>(current, size);
        LambdaQueryWrapper<ProcessTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessTemplate::getDeleted, 0);
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(ProcessTemplate::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(ProcessTemplate::getTemplateName, keyword)
                    .or().like(ProcessTemplate::getTemplateCode, keyword)
                    .or().like(ProcessTemplate::getProductModel, keyword));
        }
        wrapper.orderByDesc(ProcessTemplate::getCreateTime);
        Page<ProcessTemplate> resultPage = processTemplateMapper.selectPage(page, wrapper);
        return PageResult.of(resultPage);
    }

    /**
     * 删除工艺模板
     * 只有 DRAFT 状态可删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id, Long userId) {
        ProcessTemplate template = getById(id);
        if (template == null) {
            return Result.error("模板不存在");
        }

        // 只有 DRAFT 状态可删除
        if (!"DRAFT".equals(template.getStatus())) {
            return Result.error("只有草稿状态的模板可删除");
        }

        // 物理删除参数
        processParameterMapper.delete(
            new LambdaQueryWrapper<ProcessParameter>()
                .eq(ProcessParameter::getTemplateId, id)
        );

        // 物理删除工序步骤
        processStepMapper.delete(
            new LambdaQueryWrapper<ProcessStep>()
                .eq(ProcessStep::getTemplateId, id)
        );

        // 物理删除模板
        processTemplateMapper.deleteById(id);

        log.info("删除工艺模板成功: id={}", id);
        return Result.ok();
    }

    // ==================== 工艺参数管理 ====================

    @Override
    public List<ProcessParameter> listParameters(Long templateId) {
        return processParameterMapper.selectList(
                new LambdaQueryWrapper<ProcessParameter>()
                        .eq(ProcessParameter::getTemplateId, templateId)
                        .orderByAsc(ProcessParameter::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addParameter(ParameterDTO dto) {
        assertTemplateEditable(dto.getTemplateId());
        ProcessParameter param = new ProcessParameter();
        param.setTemplateId(dto.getTemplateId());
        param.setParamName(dto.getParamName());
        param.setParamCode(dto.getParamCode());
        param.setParamValue(dto.getParamValue());
        param.setMinValue(dto.getMinValue());
        param.setMaxValue(dto.getMaxValue());
        param.setUnit(dto.getUnit());
        param.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 1);
        processParameterMapper.insert(param);
        return param.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateParameter(Long paramId, ParameterDTO dto) {
        ProcessParameter param = processParameterMapper.selectById(paramId);
        if (param == null) {
            throw new RuntimeException("工艺参数不存在: " + paramId);
        }
        assertTemplateEditable(param.getTemplateId());
        param.setParamName(dto.getParamName());
        param.setParamCode(dto.getParamCode());
        param.setParamValue(dto.getParamValue());
        param.setMinValue(dto.getMinValue());
        param.setMaxValue(dto.getMaxValue());
        param.setUnit(dto.getUnit());
        if (dto.getSortOrder() != null) {
            param.setSortOrder(dto.getSortOrder());
        }
        processParameterMapper.updateById(param);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteParameter(Long paramId) {
        ProcessParameter param = processParameterMapper.selectById(paramId);
        if (param == null) {
            throw new RuntimeException("工艺参数不存在: " + paramId);
        }
        assertTemplateEditable(param.getTemplateId());
        processParameterMapper.deleteById(paramId);
    }

    // ==================== 工序步骤管理 ====================

    @Override
    public List<ProcessStep> listSteps(Long templateId) {
        return processStepMapper.selectList(
                new LambdaQueryWrapper<ProcessStep>()
                        .eq(ProcessStep::getTemplateId, templateId)
                        .orderByAsc(ProcessStep::getSequence));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addStep(StepDTO dto) {
        assertTemplateEditable(dto.getTemplateId());
        ProcessStep step = new ProcessStep();
        step.setTemplateId(dto.getTemplateId());
        step.setStepNo(dto.getStepNo());
        step.setStepName(dto.getStepName());
        step.setStepDesc(dto.getStepDesc());
        step.setDurationMin(dto.getDurationMin());
        step.setSequence(dto.getSequence());
        if (step.getSequence() == null) {
            Long maxSeq = processStepMapper.selectCount(
                    new LambdaQueryWrapper<ProcessStep>().eq(ProcessStep::getTemplateId, dto.getTemplateId()));
            step.setSequence(maxSeq == null ? 1 : maxSeq.intValue() + 1);
        }
        processStepMapper.insert(step);
        return step.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStep(Long stepId, StepDTO dto) {
        ProcessStep step = processStepMapper.selectById(stepId);
        if (step == null) {
            throw new RuntimeException("工序步骤不存在: " + stepId);
        }
        assertTemplateEditable(step.getTemplateId());
        step.setStepNo(dto.getStepNo());
        step.setStepName(dto.getStepName());
        step.setStepDesc(dto.getStepDesc());
        step.setDurationMin(dto.getDurationMin());
        if (dto.getSequence() != null) {
            step.setSequence(dto.getSequence());
        }
        processStepMapper.updateById(step);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStep(Long stepId) {
        ProcessStep step = processStepMapper.selectById(stepId);
        if (step == null) {
            throw new RuntimeException("工序步骤不存在: " + stepId);
        }
        assertTemplateEditable(step.getTemplateId());
        processStepMapper.deleteById(stepId);
    }

    private void assertTemplateEditable(Long templateId) {
        ProcessTemplate template = processTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("工艺模板不存在: " + templateId);
        }
        if ("PUBLISHED".equals(template.getStatus())) {
            throw new RuntimeException("已发布的模板不能修改，请复制为草稿后编辑");
        }
    }
}
