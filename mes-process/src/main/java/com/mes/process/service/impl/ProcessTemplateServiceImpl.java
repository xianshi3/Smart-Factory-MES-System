package com.mes.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mes.common.result.PageResult;
import com.mes.common.result.Result;
import com.mes.process.dto.CreateTemplateDTO;
import com.mes.process.dto.ParameterCheckDTO;
import com.mes.process.entity.ProcessParameter;
import com.mes.process.entity.ProcessTemplate;
import com.mes.process.mapper.ProcessParameterMapper;
import com.mes.process.mapper.ProcessTemplateMapper;
import com.mes.process.service.ProcessTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
        // 检查模板编码是否已存在（包括已删除的记录）
        // 使用原生SQL查询避免 @TableLogic 自动过滤
        Long count = processTemplateMapper.selectCount(
            new LambdaQueryWrapper<ProcessTemplate>()
                .eq(ProcessTemplate::getTemplateCode, dto.getTemplateCode())
                .last("AND (deleted = 0 OR deleted IS NULL)")
        );
        if (count > 0) {
            throw new RuntimeException("模板编码 " + dto.getTemplateCode() + " 已存在，请使用不同的编码");
        }

        ProcessTemplate template = new ProcessTemplate();
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

        Map<String, Double> inputValues = dto.getParamValues();
        List<String> errors = new ArrayList<>();

        for (ProcessParameter param : parameters) {
            Double value = inputValues.get(param.getParamCode());
            if (value == null) {
                errors.add("参数[" + param.getParamName() + "]未提供值");
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
     * @param keyword 关键字
     * @return 分页结果
     */
    @Override
    public PageResult<ProcessTemplate> queryPage(int current, int size, String keyword) {
        Page<ProcessTemplate> page = new Page<>(current, size);
        LambdaQueryWrapper<ProcessTemplate> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(ProcessTemplate::getTemplateName, keyword)
                   .or()
                   .like(ProcessTemplate::getTemplateCode, keyword)
                   .or()
                   .like(ProcessTemplate::getProductModel, keyword);
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

        // 逻辑删除 - 使用wrapper避免乐观锁问题
        var updateWrapper = new UpdateWrapper<ProcessTemplate>()
                .set("deleted", 1)
                .set("deleted_time", LocalDateTime.now())
                .set("deleted_by", userId)
                .eq("id", id);
        processTemplateMapper.update(null, updateWrapper);

        // 级联删除参数 - 使用wrapper避免乐观锁问题
        var paramUpdateWrapper = new UpdateWrapper<ProcessParameter>()
                .set("deleted", 1)
                .set("deleted_time", LocalDateTime.now())
                .set("deleted_by", userId)
                .eq("template_id", id);
        processParameterMapper.update(null, paramUpdateWrapper);

        log.info("删除工艺模板成功: id={}", id);
        return Result.ok();
    }
}
