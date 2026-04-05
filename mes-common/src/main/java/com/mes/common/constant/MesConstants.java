package com.mes.common.constant;

/**
 * MES系统常量类
 * 定义系统级常量，包括Redis键、Kafka主题、MQTT主题、状态枚举等
 */
public class MesConstants {
    /** 系统前缀 */
    public static final String MES_PREFIX = "mes:";
    /** Token请求头名称 */
    public static final String TOKEN_HEADER = "Authorization";
    /** Token前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Redis键常量
     */
    public static class RedisKey {
        /** 用户Token键前缀 */
        public static final String USER_TOKEN = MES_PREFIX + "user:token:";
        /** 设备状态键前缀 */
        public static final String DEVICE_STATUS = MES_PREFIX + "device:status:";
        /** 工单缓存键前缀 */
        public static final String WORKORDER_CACHE = MES_PREFIX + "workorder:cache:";
        /** 工艺模板键前缀 */
        public static final String PROCESS_TEMPLATE = MES_PREFIX + "process:template:";
        /** OEE数据键前缀 */
        public static final String OEE_DATA = MES_PREFIX + "oee:data:";
    }

    /**
     * Kafka主题常量
     */
    public static class KafkaTopic {
        /** 设备数据主题 */
        public static final String DEVICE_DATA = "mes-device-data";
        /** 工单事件主题 */
        public static final String WORKORDER_EVENT = "mes-workorder-event";
        /** 质量事件主题 */
        public static final String QUALITY_EVENT = "mes-quality-event";
        /** 告警事件主题 */
        public static final String ALARM_EVENT = "mes-alarm-event";
    }

    /**
     * MQTT主题常量
     */
    public static class MqttTopic {
        /** 设备数据上报主题 */
        public static final String DEVICE_DATA = "mes/device/+/data";
        /** 设备状态变更主题 */
        public static final String DEVICE_STATUS = "mes/device/+/status";
        /** 设备控制指令主题 */
        public static final String DEVICE_CONTROL = "mes/device/+/control";
    }

    /**
     * 工单状态常量
     */
    public static class WorkOrderStatus {
        /** 已创建 */
        public static final String CREATED = "CREATED";
        /** 已下发 */
        public static final String ISSUED = "ISSUED";
        /** 生产中 */
        public static final String IN_PRODUCTION = "IN_PRODUCTION";
        /** 待质检 */
        public static final String PENDING_QC = "PENDING_QC";
        /** 已完成 */
        public static final String COMPLETED = "COMPLETED";
        /** 已关闭 */
        public static final String CLOSED = "CLOSED";
    }

    /**
     * 质量状态常量
     */
    public static class QualityStatus {
        /** 待检测 */
        public static final String PENDING = "PENDING";
        /** 合格 */
        public static final String PASSED = "PASSED";
        /** 不合格 */
        public static final String FAILED = "FAILED";
        /** 返工 */
        public static final String REWORK = "REWORK";
    }
}
