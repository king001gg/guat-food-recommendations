-- 桂航美食推荐排行榜：MySQL 建库脚本（仅创建数据库）
-- 表结构由 Flyway 在应用启动时自动创建/升级，脚本见 db/migration/。
-- 手动部署：mysql -uroot -p < init.sql，然后由应用使用 DB_NAME 连接并执行迁移。

CREATE DATABASE IF NOT EXISTS guatfood
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
