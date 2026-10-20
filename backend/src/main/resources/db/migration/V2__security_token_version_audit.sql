-- ============================================================
-- V2: JWT 吊销（token_version）+ 审计日志 + 常用查询索引
-- 旧库执行本迁移后，所有旧 Token（不含 tv 声明）立即失效，用户需重新登录。
-- ============================================================

ALTER TABLE `user` ADD COLUMN `token_version` INT NOT NULL DEFAULT 0;

-- 已存在的账号统一置为 1；新注册用户从 0 开始，两者都只要求 Token 中的 tv 与库中一致。
UPDATE `user` SET `token_version` = 1;

CREATE TABLE `audit_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `actor_id`       BIGINT       DEFAULT NULL,
    `actor_username` VARCHAR(50)  DEFAULT NULL,
    `actor_role`     VARCHAR(20)  DEFAULT NULL,
    `action`         VARCHAR(50)  NOT NULL,
    `method`         VARCHAR(10)  NOT NULL,
    `path`           VARCHAR(255) NOT NULL,
    `query_string`   VARCHAR(1000) DEFAULT NULL,
    `success`        BOOLEAN      NOT NULL,
    `result_code`    INT          DEFAULT NULL,
    `error_message`  VARCHAR(255) DEFAULT NULL,
    `ip`             VARCHAR(45)  DEFAULT NULL,
    `user_agent`     VARCHAR(512) DEFAULT NULL,
    `duration_ms`    INT          DEFAULT NULL,
    `created_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
CREATE INDEX `idx_audit_created_at` ON `audit_log` (`created_at`);
CREATE INDEX `idx_audit_actor_created` ON `audit_log` (`actor_id`, `created_at`);
CREATE INDEX `idx_audit_action_created` ON `audit_log` (`action`, `created_at`);

-- 个人中心 / 后台列表常用查询
CREATE INDEX `idx_rating_target_created` ON `rating` (`target_type`, `target_id`, `created_at`);
CREATE INDEX `idx_rating_user_created` ON `rating` (`user_id`, `created_at`);
CREATE INDEX `idx_like_user_created` ON `like_record` (`user_id`, `created_at`);
CREATE INDEX `idx_favorite_user_created` ON `favorite` (`user_id`, `created_at`);
