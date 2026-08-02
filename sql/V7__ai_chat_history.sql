-- =====================================================
-- V7: AI 生产助理对话历史
-- 新增 ai_chat_conversations + ai_chat_messages 表
-- 支持多轮对话持久化、逻辑删除
-- =====================================================
DROP TABLE IF EXISTS `ai_chat_messages`;
DROP TABLE IF EXISTS `ai_chat_conversations`;

CREATE TABLE `ai_chat_conversations` (
    `id` varchar(36) NOT NULL COMMENT '对话ID (UUID)',
    `user_id` varchar(50) NOT NULL DEFAULT 'default' COMMENT '用户ID',
    `title` varchar(200) NOT NULL DEFAULT '新对话' COMMENT '对话标题',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 对话记录';

CREATE TABLE `ai_chat_messages` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` varchar(36) NOT NULL COMMENT '所属对话ID',
    `role` varchar(20) NOT NULL COMMENT '角色: user / assistant',
    `content` text NOT NULL COMMENT '消息内容 (Markdown)',
    `steps` json DEFAULT NULL COMMENT 'Agent 执行步骤',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 消息记录';
