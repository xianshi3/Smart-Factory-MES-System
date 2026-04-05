package com.mes.process.dto;

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
    private String templateName;
    /** 模板编码 */
    private String templateCode;
    /** 产品型号 */
    private String productModel;
    /** 参数列表 */
    private List<ParameterItem> parameters;

    /**
     * 参数项
     */
    @Data
    public static class ParameterItem {
        /** 参数名称 */
        private String paramName;
        /** 参数编码 */
        private String paramCode;
        /** 参数值 */
        private String paramValue;
        /** 最小值 */
        private Double minValue;
        /** 最大值 */
        private Double maxValue;
        /** 单位 */
        private String unit;
    }
}
