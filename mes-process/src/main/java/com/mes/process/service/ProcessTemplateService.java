package com.mes.process.service;

import com.mes.common.result.PageResult;
import com.mes.common.result.Result;
import com.mes.process.dto.CreateTemplateDTO;
import com.mes.process.dto.ParameterCheckDTO;
import com.mes.process.entity.ProcessTemplate;

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
     * 校验参数
     * @param dto 参数校验DTO
     * @return 校验结果
     */
    Result<?> checkParameters(ParameterCheckDTO dto);

    /**
     * 分页查询模板
     * @param current 当前页
     * @param size 每页大小
     * @param keyword 关键字
     * @return 分页结果
     */
    PageResult<ProcessTemplate> queryPage(int current, int size, String keyword);
}
