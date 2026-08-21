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
- 🛠️ **管理后台**：数据概览（ECharts）、档口 / 菜品 / 食堂 / 评价 / 用户管理
- 🖼️ **图片上传**：档口封面、菜品图片（本地 `uploads/` 目录）

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router + Axios + ECharts |
| 后端 | Spring Boot 3 + MyBatis-Plus + H2(开发) / MySQL(生产) + JWT + BCrypt |
| 其他 | Hutool、Thumbnailator |

## 目录结构

```
├── backend/                  # Spring Boot 后端
│   └── src/main/java/com/guatfood/
│       ├── common/           # 统一响应 / 异常 / 分页 / 认证工具
│       ├── config/           # JWT / CORS / MyBatis-Plus / 数据初始化
│       ├── controller/       # REST 接口
│       ├── dto/              # 请求体
│       ├── entity/           # 数据模型
│       ├── mapper/           # MyBatis-Plus Mapper
│       └── service/          # 业务逻辑
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

```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

- 默认 `dev` 环境，使用 **H2 内嵌数据库**（文件存储在 `backend/data/`），首次启动自动建表并写入种子数据
- 服务地址：`http://localhost:8080`
- H2 控制台：`http://localhost:8080/h2-console`（JDBC URL：`jdbc:h2:file:./data/guatfood`，用户名 `sa`，密码为空）

> 生产环境：使用 `--spring.profiles.active=prod` 并配置 `application-prod.yml` 中的 MySQL 连接信息。

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

- 访问：`http://localhost:5173`（已配置代理，`/api` 与 `/uploads` 转发至 `http://localhost:8080`）

### 默认账号

首次启动会自动写入演示账号，密码 **不是固定值**，所有演示账号共用同一密码：

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | 由 `INIT_PASSWORD` 指定，或启动日志中随机生成 | 管理员 |
| `zhangsan` / `lisi` / `wangwu` / `zhaoliu` / `sunqi` | 同上 | 普通用户 |

- **指定密码**：`INIT_PASSWORD=你的密码 ./mvnw spring-boot:run`（Windows CMD：先 `set INIT_PASSWORD=你的密码` 再启动）
- **未指定时**：启动时用 `SecureRandom` 随机生成 12 位密码并打印到日志（搜索关键字 `随机生成演示账号密码`）
- 仅在本地开发需要登录后台时用到；也可随时通过注册接口创建自己的账号

## 主要接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/register` / `/api/auth/login` | 注册 / 登录（返回 token + user） |
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

## 排行榜算法说明

- **综合榜**：按 `scoreAvg`（三维平均分）降序，同分按评分数
- **好评榜**：按 `tasteAvg`（口味均分）降序，同分按评分数
- **人气榜**：按 `heat = 评分数×10 + 点赞数×5 + 浏览量` 降序
- **热门榜**：按近 30 天评分数 `recentCount` 降序

## 部署

生产环境使用 MySQL，配置全部通过环境变量注入（`DB_*` / `JWT_SECRET` / `INIT_PASSWORD`）。

- **Windows（单 jar 一键部署）**：见 [`deploy/windows/README.md`](deploy/windows/README.md)
- **Linux 服务器**：Nginx + systemd 托管前端并反向代理 `/api`、`/uploads`（思路同 `deploy/windows/nginx.conf`）

## 注意事项

- JWT 密钥、数据库账号密码、种子账号密码均通过环境变量注入（`JWT_SECRET` / `DB_*` / `INIT_PASSWORD`）；未显式配置时，JWT 密钥与种子密码会在启动时随机生成，**生产环境务必显式配置并妥善保管**
- 上传图片默认保存至 `backend/uploads/`，通过 `/uploads/**` 静态映射访问
- 表名使用 `food_window`（避免 SQL 保留字 `window`）、评分列使用 `value_score`（避免保留字 `value`）
