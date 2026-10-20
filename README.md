# 桂航美食推荐排行榜（Guat Best Food Recommendations）

面向 **桂林航天工业学院（桂航）** 的校园美食推荐排行榜，参考 `delicious` 全栈项目实现。支持 **档口 + 菜品** 两级榜单、**多维评分**（口味 / 性价比 / 分量）、点赞、收藏、投稿与后台管理。

## 功能特性

- 🏆 **两级排行榜**：档口排行 + 菜品排行，支持按食堂、关键词筛选
- 📊 **多维评分机制**：综合榜 / 好评榜 / 人气榜 / 热门榜（近 30 天）
  - 综合分 =（口味 + 性价比 + 分量）÷ 3
  - 人气值 = 评分数 × 10 + 点赞数 × 5 + 浏览量
- ⭐ **评分 / 点赞 / 收藏**：同一用户对同一目标仅保留一条评分
- 📝 **投稿审核**：普通用户投稿进入「待审核」，管理员审核后上架
- 🔐 **JWT 认证 + 角色权限**：普通用户 / 管理员，接口级权限校验
- 🔑 **账号安全**：用户自助改密、管理员重置密码；改密/注销后旧 Token 立即失效
- 🚦 **限流与审计**：登录/注册/改密/上传/评分限流（429 + `Retry-After`），管理操作写入审计日志（保留 180 天）
- 🛠️ **管理后台**：数据概览（ECharts）、档口 / 菜品 / 食堂 / 评价 / 用户 / 审计日志管理
- 🖼️ **图片上传**：档口封面、菜品图片（本地 `uploads/` 目录）

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router + Axios + ECharts |
| 后端 | Spring Boot 3.5 + MyBatis-Plus + Flyway + H2(开发/测试) / MySQL(生产) + JWT + BCrypt |
| 其他 | Caffeine（限流）、Spring AOP（审计）、Actuator（健康检查）、Hutool、Thumbnailator |

## 目录结构

```
├── backend/                  # Spring Boot 后端
│   └── src/main/java/com/guatfood/
│       ├── audit/            # @Audit 注解与审计切面
│       ├── common/           # 统一响应 / 异常 / 分页 / 认证工具
│       ├── config/           # JWT / CORS / MyBatis-Plus / 数据初始化
│       ├── controller/       # REST 接口
│       ├── dto/              # 请求体
│       ├── entity/           # 数据模型
│       ├── mapper/           # MyBatis-Plus Mapper
│       ├── security/         # 限流 / 客户端 IP 解析
│       └── service/          # 业务逻辑
│   └── src/main/resources/
│       ├── db/migration/     # Flyway 迁移脚本（V1 基线、V2 安全/审计）
│       └── seed-images/      # 种子图片
└── frontend/                 # Vue 3 前端
    └── src/
        ├── api/              # 接口封装
        ├── components/       # 公共组件
        ├── router/           # 路由与守卫
        ├── store/            # Pinia 状态
        ├── utils/            # axios 封装
        └── views/            # 页面（含 admin/ 后台）
```

## 快速开始

### 环境要求

- JDK 17+（本项目在 JDK 25 下编译运行通过）
- Node.js 18+、npm
- （可选，生产环境）MySQL 8.x

### 1. 启动后端

必须显式选择运行环境；未指定 `dev` / `prod` 时应用会拒绝启动，避免误用 H2。
首次初始化还必须显式提供强密码，**不会再把密码打印到日志**。

Linux / macOS / Git Bash：

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev \
INIT_PASSWORD='你的开发管理员密码' \
DEMO_PASSWORD='你的开发演示账号密码' \
./mvnw spring-boot:run
```

Windows CMD：

```bat
cd backend
set SPRING_PROFILES_ACTIVE=dev
set INIT_PASSWORD=你的开发管理员密码
set DEMO_PASSWORD=你的开发演示账号密码
mvnw.cmd spring-boot:run
```

- `dev` 使用 **H2 内嵌数据库**（相对当前工作目录，从 `backend/` 启动时文件在 `backend/data/`），首次启动自动建表并写入种子数据
- 种子数据包含「美食图鉴」档口和 51 道图片菜，图片已打包在 `backend/src/main/resources/seed-images/`，通过 `/uploads/seed/**` 访问，部署时无需额外复制
- 服务地址：`http://localhost:8080`
- H2 控制台：`http://localhost:8080/h2-console`（JDBC URL：`jdbc:h2:file:./data/guatfood`，用户名 `sa`，密码为空）
- H2 文件路径是相对**启动目录**的；请固定从 `backend/` 启动，避免在仓库根目录生成 `data/guatfood.mv.db`

> 生产环境：使用 `SPRING_PROFILES_ACTIVE=prod` 并配置 `application-prod.yml` 中的 MySQL 连接信息；生产不会创建演示账号。

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

- 访问：`http://localhost:5173`（已配置代理，`/api` 与 `/uploads` 转发至 `http://localhost:8080`）
- 手机局域网访问：`npm run dev:lan`（绑定 `0.0.0.0`，手机连同一 WiFi 后访问 `http://<电脑IP>:5173`，注意放行防火墙 5173 端口）

### 默认账号

首次启动（空库）时按环境创建账号：

| 账号 | 密码 | 角色 | 说明 |
|---|---|---|---|
| `admin` | `INIT_PASSWORD` | 管理员 | dev / prod 都会创建 |
| `zhangsan` / `lisi` / `wangwu` / `zhaoliu` / `sunqi` | `DEMO_PASSWORD` | 普通用户 | 仅 dev 创建 |

- **生产**：只创建 `admin`，不会写入演示账号或虚假评分；`INIT_PASSWORD` 仅在空库首次启动时使用
- **开发**：管理员与演示账号使用不同密码；两者都必须至少 8 位，且不能是占位值
- **密码不会写入日志**；未配置时启动直接失败，避免弱口令或日志泄露
- 已有数据后修改 `INIT_PASSWORD` **不会**重置密码；请登录后在「个人中心 → 账号安全」修改，
  或由管理员在「用户管理 → 重置密码」处理。改密后该账号所有旧 Token 立即失效
- 已存在旧库首次启动时，Flyway 会自动建立基线并执行后续迁移，无需手工建表

## 主要接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/register` / `/api/auth/login` | 注册 / 登录（返回 token + user） |
| GET | `/api/auth/me` | 当前登录用户（用于刷新前端角色/状态） |
| POST | `/api/auth/logout` | 服务端注销，递增 token_version 使旧 Token 立即失效 |
| PUT | `/api/auth/password` | 修改自己的密码（`oldPassword` / `newPassword`，8-100 位） |
| PUT | `/api/users/{id}/password` | 管理员重置指定用户密码（`newPassword`） |
| GET | `/api/audit-logs` | 审计日志分页查询（管理员，支持 action/actorId/success/start/end） |
| GET | `/api/windows?type=overall&canteenId=&keyword=&page=&size=` | 档口排行榜 |
| GET | `/api/dishes?type=overall&windowId=&canteenId=&keyword=` | 菜品排行榜 |
| GET | `/api/windows/{id}` / `/api/dishes/{id}` | 详情（含统计与当前用户状态） |
| POST | `/api/windows` / `/api/dishes` | 投稿（普通用户为待审核） |
| POST | `/api/ratings` | 提交评分（upsert） |
| GET | `/api/ratings?targetType=&targetId=` | 某目标评分列表 |
| POST | `/api/likes` / `/api/favorites` | 点赞 / 收藏（body: `{targetType, targetId}`） |
| POST | `/api/upload/image` | 上传图片（multipart `file`） |
| GET | `/api/stats/overview` 等 | 后台统计（需管理员） |
| GET/PUT/DELETE | `/api/users`、`/api/windows/all`、`/api/dishes/all`、`/api/ratings/all`、`/api/canteens` | 后台管理（需管理员） |

统一响应结构：`{ "code": 200, "message": "操作成功", "data": ... }`；排行榜分页为 `{ total, page, size, list }`，后台分页为 MyBatis-Plus `Page`（`records`）。

HTTP 状态码与业务码保持一致，便于 Nginx/监控/客户端正确处理：

| 场景 | HTTP / code |
|---|---|
| 成功 | `200` |
| 参数校验失败、上传超限 | `400` |
| 未登录 / Token 失效 / 登录失败 | `401` |
| 已登录但无权限 | `403` |
| 资源不存在 | `404` |
| 方法/媒体类型不支持 | `405` / `415` |
| 重复注册、冲突操作 | `409` |
| 触发限流 | `429` + `Retry-After` |
| 未预期异常 | `500`（不返回内部细节） |

## 排行榜算法说明

- **综合榜**：按 `scoreAvg`（三维平均分）降序，同分按评分数
- **好评榜**：按 `tasteAvg`（口味均分）降序，同分按评分数
- **人气榜**：按 `heat = 评分数×10 + 点赞数×5 + 浏览量` 降序
- **热门榜**：按近 30 天评分数 `recentCount` 降序

## 部署

生产环境使用 MySQL，配置全部通过环境变量注入（`DB_*` / `JWT_SECRET` / `INIT_PASSWORD`；`DEMO_PASSWORD` 仅开发环境使用）。

- **Windows（单 jar 一键部署）**：见 [`deploy/windows/README.md`](deploy/windows/README.md)
- **Linux（Nginx + HTTPS + systemd）**：见 [`deploy/linux/README.md`](deploy/linux/README.md)
- **安全部署与合规清单**：见 [`SECURITY.md`](SECURITY.md)（备案 / UGC 合规 / 加固自测）

## 测试与 CI

- 后端：`cd backend && ./mvnw test`（H2 内存库；覆盖登录/鉴权/注销/改密/重置/限流/审计/HTTP 状态）
- 前端：`cd frontend && npm ci && npm run build`
- GitHub Actions：`.github/workflows/ci.yml` 执行后端 `clean verify` 与前端构建；
  `.github/workflows/security.yml` 用 Trivy 扫描依赖；Dependabot 每周检查 Maven / npm / Actions 更新

## 健康检查与监控

生产启用独立管理端口（默认 `127.0.0.1:8081`），不经过 Nginx，Nginx 也已显式屏蔽 `/actuator`：

- `GET /actuator/health/liveness`：应用是否存活（启动失败时部署脚本自动回滚）
- `GET /actuator/health/readiness`：应用 + 数据库是否就绪（数据库故障不会误触发代码回滚）
- 仅暴露 `health`、`info` 两个端点

## 备份

- Linux：`deploy/linux/backup.sh` + `guatfood-backup.timer`（每天 03:00，默认保留 14 天）
- Windows：`deploy/windows/backup.ps1`（可由任务计划程序定时调用）
- 备份内容：MySQL 全库（`mysqldump`）+ 上传图片目录；恢复步骤见各部署 README

## 注意事项

- JWT 密钥、数据库账号密码、种子账号密码均通过环境变量注入（`JWT_SECRET` / `DB_*` / `INIT_PASSWORD` / `DEMO_PASSWORD`）。dev 未配置 `JWT_SECRET` 时会生成临时随机密钥（重启后登录失效）；**prod 必须显式配置，否则拒绝启动**。首次初始化时，`INIT_PASSWORD`（以及 dev 环境的 `DEMO_PASSWORD`）缺失会拒绝启动，不会生成后打印
- 用户上传的图片默认保存至 `backend/uploads/`（可用 `UPLOAD_PATH` 覆盖），通过 `/uploads/**` 静态映射访问；种子图片打包在 jar 内，不占用该目录
- 生产健康检查端口由 `MANAGEMENT_PORT` / `MANAGEMENT_ADDRESS` 控制（默认仅回环 `8081`）；备份目录与保留天数由 `BACKUP_DIR` / `BACKUP_RETENTION_DAYS` 控制
- 表名使用 `food_window`（避免 SQL 保留字 `window`）、评分列使用 `value_score`（避免保留字 `value`）
