-- 桂航美食推荐排行榜 数据库初始化脚本 (MySQL 生产环境)
-- 建库后手动执行本脚本；表结构与 schema-h2.sql 保持一致
-- 种子数据（管理员/食堂/档口/菜品/评分）由应用首次启动时通过 DataInitializer 自动写入

CREATE DATABASE IF NOT EXISTS guatfood DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE guatfood;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色: USER/ADMIN',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DISABLED',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 食堂表
CREATE TABLE IF NOT EXISTS `canteen` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '食堂ID',
    `name`       VARCHAR(50)  NOT NULL COMMENT '食堂名称',
    `location`   VARCHAR(100) DEFAULT NULL COMMENT '位置/校区',
    `sort_order` INT          DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食堂表';

-- 档口/窗口表
CREATE TABLE IF NOT EXISTS `food_window` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '档口ID',
    `canteen_id`   BIGINT       NOT NULL COMMENT '所属食堂ID',
    `name`         VARCHAR(50)  NOT NULL COMMENT '档口名称',
    `description`  TEXT         DEFAULT NULL COMMENT '简介',
    `cover_image`  VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
    `location`     VARCHAR(50)  DEFAULT NULL COMMENT '位置(楼层/区域)',
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态: PUBLISHED/PENDING',
    `view_count`   INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_window_canteen_id` (`canteen_id`),
    KEY `idx_window_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='档口表';

-- 菜品表
CREATE TABLE IF NOT EXISTS `dish` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
    `window_id`    BIGINT        NOT NULL COMMENT '所属档口ID',
    `name`         VARCHAR(50)   NOT NULL COMMENT '菜品名称',
    `description`  TEXT          DEFAULT NULL COMMENT '简介',
    `image`        VARCHAR(255)  DEFAULT NULL COMMENT '图片URL',
    `price`        DECIMAL(6,2)  DEFAULT 0 COMMENT '价格(元)',
    `status`       VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态: PUBLISHED/PENDING',
    `view_count`   INT           NOT NULL DEFAULT 0 COMMENT '浏览量',
    `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_dish_window_id` (`window_id`),
    KEY `idx_dish_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品表';

-- 评分表
CREATE TABLE IF NOT EXISTS `rating` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '评分ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `target_type` VARCHAR(20) NOT NULL COMMENT '目标类型: WINDOW/DISH',
    `target_id`   BIGINT      NOT NULL COMMENT '目标ID',
    `taste`       INT         NOT NULL DEFAULT 3 COMMENT '口味(1-5)',
    `value_score` INT         NOT NULL DEFAULT 3 COMMENT '性价比(1-5)',
    `portion`     INT         NOT NULL DEFAULT 3 COMMENT '分量(1-5)',
    `comment`     TEXT        DEFAULT NULL COMMENT '评语',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rating_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_rating_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分表';

-- 点赞表
CREATE TABLE IF NOT EXISTS `like_record` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `target_type` VARCHAR(20) NOT NULL COMMENT '目标类型: WINDOW/DISH',
    `target_id`   BIGINT      NOT NULL COMMENT '目标ID',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_like_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_like_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

-- 收藏表
CREATE TABLE IF NOT EXISTS `favorite` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `target_type` VARCHAR(20) NOT NULL COMMENT '目标类型: WINDOW/DISH',
    `target_id`   BIGINT      NOT NULL COMMENT '目标ID',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fav_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_fav_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';
