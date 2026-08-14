package com.mes.process.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建工艺模板DTO
 * @author MES
 * @description 创建工艺模板请求参数
 */
@Data
public class CreateTemplateDTO {
    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称过长")
    private String templateName;

    /** 模板编码 */
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 50, message = "模板编码过长")
    private String templateCode;

    /** 产品型号 */
    @Size(max = 100, message = "产品型号过长")
    private String productModel;

    /** 描述信息 */
    @Size(max = 500, message = "描述过长")
    private String description;

    /** 参数列表 */
    private List<ParameterItem> parameters;

    /**
     * 参数项
     */
    @Data
    public static class ParameterItem {
        /** 参数名称 */
        @NotBlank(message = "参数名称不能为空")
        @Size(max = 100, message = "参数名称过长")
        private String paramName;

        /** 参数编码 */
        @NotBlank(message = "参数编码不能为空")
        @Size(max = 50, message = "参数编码过长")
        private String paramCode;

        /** 参数值 */
        @Size(max = 100, message = "参数值过长")
        private String paramValue;

        /** 最小值 */
        private Double minValue;

        /** 最大值 */
        private Double maxValue;

        /** 单位 */
        @Size(max = 20, message = "单位过长")
        private String unit;
    }
}
