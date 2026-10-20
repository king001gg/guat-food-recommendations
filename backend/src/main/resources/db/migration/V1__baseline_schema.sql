-- ============================================================
-- V1 baseline: 桂航美食推荐排行榜初始表结构
-- 兼容 MySQL 8 与 H2 (MODE=MySQL)；不包含任何种子数据。
-- 已存在的旧库会被 Flyway baseline 到 V1，不会重复执行本文件。
-- ============================================================

CREATE TABLE `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `username`   VARCHAR(50)  NOT NULL,
    `password`   VARCHAR(255) NOT NULL,
    `nickname`   VARCHAR(50)  DEFAULT NULL,
    `avatar`     VARCHAR(255) DEFAULT NULL,
    `role`       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    `status`     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_user_username` UNIQUE (`username`)
);

CREATE TABLE `canteen` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(50)  NOT NULL,
    `location`   VARCHAR(100) DEFAULT NULL,
    `sort_order` INT          DEFAULT 0,
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_canteen_name` UNIQUE (`name`)
);

CREATE TABLE `food_window` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `canteen_id`  BIGINT       NOT NULL,
    `name`        VARCHAR(50)  NOT NULL,
    `description` TEXT         DEFAULT NULL,
    `cover_image` VARCHAR(255) DEFAULT NULL,
    `location`    VARCHAR(50)  DEFAULT NULL,
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    `view_count`  INT          NOT NULL DEFAULT 0,
    `created_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
CREATE INDEX `idx_window_canteen_id` ON `food_window` (`canteen_id`);
CREATE INDEX `idx_window_name` ON `food_window` (`name`);

CREATE TABLE `dish` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `window_id`   BIGINT       NOT NULL,
    `name`        VARCHAR(50)  NOT NULL,
    `description` TEXT         DEFAULT NULL,
    `image`       VARCHAR(255) DEFAULT NULL,
    `price`       DECIMAL(6,2) DEFAULT 0,
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    `view_count`  INT          NOT NULL DEFAULT 0,
    `created_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
CREATE INDEX `idx_dish_window_id` ON `dish` (`window_id`);
CREATE INDEX `idx_dish_name` ON `dish` (`name`);

CREATE TABLE `rating` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT      NOT NULL,
    `target_type` VARCHAR(20) NOT NULL,
    `target_id`   BIGINT      NOT NULL,
    `taste`       INT         NOT NULL DEFAULT 3,
    `value_score` INT         NOT NULL DEFAULT 3,
    `portion`     INT         NOT NULL DEFAULT 3,
    `comment`     TEXT        DEFAULT NULL,
    `created_at`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_rating_user_target` UNIQUE (`user_id`, `target_type`, `target_id`)
);
CREATE INDEX `idx_rating_target` ON `rating` (`target_type`, `target_id`);

CREATE TABLE `like_record` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT      NOT NULL,
    `target_type` VARCHAR(20) NOT NULL,
    `target_id`   BIGINT      NOT NULL,
    `created_at`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_like_user_target` UNIQUE (`user_id`, `target_type`, `target_id`)
);
CREATE INDEX `idx_like_target` ON `like_record` (`target_type`, `target_id`);

CREATE TABLE `favorite` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT      NOT NULL,
    `target_type` VARCHAR(20) NOT NULL,
    `target_id`   BIGINT      NOT NULL,
    `created_at`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_fav_user_target` UNIQUE (`user_id`, `target_type`, `target_id`)
);
CREATE INDEX `idx_fav_target` ON `favorite` (`target_type`, `target_id`);
