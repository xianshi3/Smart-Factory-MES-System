-- =====================================================
-- V8: AI 分析历史记录表
-- 存储数字孪生 SPC/能耗/产能/AI建议 分析结果
-- =====================================================
DROP TABLE IF EXISTS `ai_analysis_history`;

CREATE TABLE `ai_analysis_history` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` varchar(50) NOT NULL DEFAULT 'default' COMMENT '用户ID',
    `device_code` varchar(50) DEFAULT NULL COMMENT '设备编码',
    `device_name` varchar(100) DEFAULT NULL COMMENT '设备名称',
    `analysis_type` varchar(20) NOT NULL COMMENT '分析类型: spc/energy/capacity/llm',
    `result_data` json DEFAULT NULL COMMENT '分析结果 (JSON)',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`analysis_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 分析历史记录';
