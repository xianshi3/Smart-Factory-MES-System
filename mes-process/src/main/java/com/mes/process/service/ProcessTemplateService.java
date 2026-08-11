package com.mes.process.service;

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

import java.util.List;

/**
 * 工艺模板服务接口
 * @author MES
 * @description 工艺模板相关业务逻辑
 */
public interface ProcessTemplateService {
    /**
     * 创建工艺模板
     * @param dto 创建模板DTO
     * @return 模板ID
     */
    Long create(CreateTemplateDTO dto);

    /**
     * 根据ID查询模板
     * @param id 模板ID
     * @return 模板对象
     */
    ProcessTemplate getById(Long id);

    /**
     * 查询模板详情（含参数与工序步骤）
     */
    TemplateDetailVO getDetail(Long id);

    /**
     * 更新工艺模板
     * @param id 模板ID
     * @param dto 更新内容
     */
    void updateTemplate(Long id, CreateTemplateDTO dto);

    /**
     * 发布工艺模板
     * @param id 模板ID
     */
    void publish(Long id);

    /**
     * 复制工艺模板（含参数与工序步骤）
     * @param id 模板ID
     * @return 新模板ID
     */
    Long copy(Long id);

    /**
     * 校验参数
     * @param dto 参数校验DTO
     * @return 校验结果
     */
    Result<?> checkParameters(ParameterCheckDTO dto);

    /**
     * 分页查询模板
     * @param current 当前页
     * @param size 每页大小
     * @param status 状态筛选
     * @param keyword 关键字
     * @return 分页结果
     */
    PageResult<ProcessTemplate> queryPage(int current, int size, String status, String keyword);

    /**
     * 删除工艺模板
     * @param id 模板ID
     * @param userId 用户ID
     * @return 结果
     */
    Result<Void> delete(Long id, Long userId);

    /**
     * 查询模板参数列表
     */
    List<ProcessParameter> listParameters(Long templateId);

    /**
     * 新增参数
     */
    Long addParameter(ParameterDTO dto);

    /**
     * 更新参数
     */
    void updateParameter(Long paramId, ParameterDTO dto);

    /**
     * 删除参数
     */
    void deleteParameter(Long paramId);

    /**
     * 查询模板工序步骤列表
     */
    List<ProcessStep> listSteps(Long templateId);

    /**
     * 新增工序步骤
     */
    Long addStep(StepDTO dto);

    /**
     * 更新工序步骤
     */
    void updateStep(Long stepId, StepDTO dto);

    /**
     * 删除工序步骤
     */
    void deleteStep(Long stepId);
}
