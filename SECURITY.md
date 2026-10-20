# 安全部署与合规清单

本项目面向**中国大陆**部署时，除常规安全加固外，还需满足备案与内容合规要求。
本清单分「合规（必须）」与「安全加固（建议/必须）」两部分，部署前逐项核对。

## 一、合规（境内公开运营必须）

- [ ] **域名实名认证**：在域名注册商完成（个人身份证）。
- [ ] **ICP 备案**：境内服务器上的网站必须备案，未备案域名会被云厂商封停 80/443。
- [ ] **公安备案**：ICP 备案通过后 30 日内完成。
- [ ] **用户实名**（《网络安全法》第 24 条）：本项目有注册/评分/投稿等 UGC 功能，
      正式公开运营需接入手机号/实名认证（当前仅用户名+密码，为演示简化）。
- [ ] **内容审核**：投稿已有「待审核 → 管理员上架」机制；但**评分评论即时发布**，
      公开运营应加敏感词过滤或先审后发。
- [ ] **隐私政策**（《个人信息保护法》）：页面提供隐私政策，说明数据收集与用途。

> 个人学习/毕设展示风险较低；面向全校公开运营时务必补足上述项。

## 二、部署环境加固

- [ ] 云安全组最小化：仅开 `80/443`，`22`(SSH) 或 `3389`(RDP) 仅限自己 IP，`3306` 不对公网。
- [ ] 服务器本机防火墙（ufw/firewalld）同步最小化。
- [ ] SSH 用密钥登录、禁用密码登录与 root 直连。
- [ ] 系统与 JDK 及时更新安全补丁。
- [ ] MySQL：独立最小权限账号、禁 root 远程、强密码、`bind-address=127.0.0.1`。

## 三、应用安全配置

- [ ] `JWT_SECRET`：`openssl rand -base64 48` 生成，写入环境变量，勿用默认。
- [ ] `DB_PASSWORD`：独立强密码，勿复用。
- [ ] `INIT_PASSWORD`：部署前设为强密码（至少 8 位，勿用 `123456`）；仅在空库首次启动时使用，生产不会创建演示账号。
- [ ] `DEMO_PASSWORD`：仅本地开发使用，生产不要设置；必须与 `INIT_PASSWORD` 不同。
- [ ] `SERVER_ADDRESS=127.0.0.1`：后端只监听回环，由 Nginx 反代，不直接暴露 8080。
- [ ] `MANAGEMENT_ADDRESS=127.0.0.1`：Actuator 管理端口只监听回环（默认 8081）；Nginx 已屏蔽 `/actuator`。
- [ ] **HTTPS**：必须开启（acme.sh/云厂商免费证书）。
- [ ] 上传目录 `uploads/` 独立且定期备份；MySQL 用 `mysqldump` 定时备份。
- [ ] 启用内置备份定时器（Linux `guatfood-backup.timer` / Windows 任务计划程序 + `backup.ps1`），并定期做恢复演练。
- [ ] 首次部署后立即修改 `admin` 初始密码（个人中心 → 账号安全），并确认旧 Token 已失效。

## 四、代码层已内置的安全措施

- 密码 BCrypt 加密存储；登录错误统一提示，不泄露账号是否存在
- JWT 认证 + 角色权限校验（普通用户/管理员，接口级）；生产必须配置至少 32 字节的 `JWT_SECRET`
- **服务端 Token 吊销**：注销、修改密码、管理员重置密码、禁用账号都会递增 `token_version`，旧 Token 立即失效
- **限流**：登录（IP 每分钟 / IP+用户名 15 分钟失败次数）、注册（IP 每小时/每天）、改密（用户 15 分钟尝试次数）、上传/评分（用户每分钟）；超限返回 `429` + `Retry-After`。路径按 Spring 解析后的结果匹配，`;matrix` 参数等变体无法绕过
- **登录等时比较**：用户名不存在时也执行一次 BCrypt 校验，避免通过响应耗时枚举账号
- **审计日志**：登录/注册/注销/改密/重置/用户与内容管理写操作落库；不记录请求体，查询串中的密码/Token 已脱敏，默认保留 180 天
- SQL 全部参数化查询（MyBatis-Plus），防注入
- 越权防护（删除他人评分/点赞/收藏返回 403）
- 未发布内容仅管理员可见（防绕过审核直接访问）
- 上传文件白名单（jpg/png/gif/webp）+ 重压缩，防图片木马
- 错误信息脱敏，不泄露内部异常细节；HTTP 状态与业务码一致（401/403/409/429 等）
- 数据库结构由 Flyway 迁移管理（`baseline-on-migrate`，禁止 `clean`），避免手工改表漂移
- 种子账号密码随机化 / 环境变量注入，不在源码中硬编码
- CI 运行后端集成测试与前端构建；Trivy + Dependabot 持续检查依赖漏洞

## 五、部署后自测

```bash
# 未登录访问后台接口 → 应 401/403
curl -s https://你的域名/api/users | head -c 200
# 排行榜正常
curl -s 'https://你的域名/api/windows?page=1&size=1'
# 上传图片能正常访问
curl -sI https://你的域名/uploads/xxx.jpg
# HTTP 应跳转 HTTPS
curl -sI http://你的域名 | grep -i location
# Nginx 应屏蔽管理端点
curl -sI https://你的域名/actuator/health
# 本机健康检查（生产管理端口只监听 127.0.0.1）
curl -fsS http://127.0.0.1:8081/actuator/health/readiness
# 登录失败应返回 401；连续失败应触发 429（带 Retry-After）
curl -s -o /dev/null -w '%{http_code}\n' -X POST https://你的域名/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"admin","password":"wrong"}'
```

审计日志可在管理后台「审计日志」页面查询，或直接查库：

```sql
SELECT action, actor_username, success, result_code, ip, created_at
FROM audit_log ORDER BY created_at DESC LIMIT 20;
```
