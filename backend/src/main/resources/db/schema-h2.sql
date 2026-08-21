-- 桂航美食推荐排行榜 H2 数据库初始化 (开发环境)
-- H2 以 MySQL 兼容模式运行；种子数据由 DataInitializer 在首次启动时写入

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `username`    VARCHAR(50)  NOT NULL,
    `password`    VARCHAR(255) NOT NULL,
    `nickname`    VARCHAR(50)  DEFAULT NULL,
    `avatar`      VARCHAR(255) DEFAULT NULL,
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    `created_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
);

-- 食堂表
CREATE TABLE IF NOT EXISTS `canteen` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(50) NOT NULL,
    `location`   VARCHAR(100) DEFAULT NULL,
    `sort_order` INT         DEFAULT 0,
    `created_at` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
);

-- 档口/窗口表
CREATE TABLE IF NOT EXISTS `food_window` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `canteen_id`   BIGINT       NOT NULL,
    `name`         VARCHAR(50)  NOT NULL,
    `description`  TEXT         DEFAULT NULL,
    `cover_image`  VARCHAR(255) DEFAULT NULL,
    `location`     VARCHAR(50)  DEFAULT NULL,
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    `view_count`   INT          NOT NULL DEFAULT 0,
    `created_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_window_canteen_id` (`canteen_id`),
    KEY `idx_window_name` (`name`)
);

-- 菜品表
CREATE TABLE IF NOT EXISTS `dish` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT,
    `window_id`    BIGINT        NOT NULL,
    `name`         VARCHAR(50)   NOT NULL,
    `description`  TEXT          DEFAULT NULL,
    `image`        VARCHAR(255)  DEFAULT NULL,
    `price`        DECIMAL(6,2)  DEFAULT 0,
    `status`       VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED',
    `view_count`   INT           NOT NULL DEFAULT 0,
    `created_at`   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_dish_window_id` (`window_id`),
    KEY `idx_dish_name` (`name`)
);

-- 评分表 (多维: 口味/性价比/分量, 1-5 星)
CREATE TABLE IF NOT EXISTS `rating` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `target_type` VARCHAR(20)  NOT NULL,
    `target_id`   BIGINT       NOT NULL,
    `taste`       INT          NOT NULL DEFAULT 3,
    `value_score` INT          NOT NULL DEFAULT 3,
    `portion`     INT          NOT NULL DEFAULT 3,
    `comment`     TEXT         DEFAULT NULL,
    `created_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rating_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_rating_target` (`target_type`, `target_id`)
);

-- 点赞表
CREATE TABLE IF NOT EXISTS `like_record` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT      NOT NULL,
    `target_type` VARCHAR(20) NOT NULL,
    `target_id`   BIGINT      NOT NULL,
    `created_at`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_like_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_like_target` (`target_type`, `target_id`)
);

-- 收藏表
CREATE TABLE IF NOT EXISTS `favorite` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT      NOT NULL,
    `target_type` VARCHAR(20) NOT NULL,
    `target_id`   BIGINT      NOT NULL,
    `created_at`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fav_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_fav_target` (`target_type`, `target_id`)
);
