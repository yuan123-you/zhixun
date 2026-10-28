# 知讯完整修复与发布记录（2026-10-28）

## 授权与基线

用户已明确要求“全面修复并推送部署”。沿用现有 `origin/master` 和已核实的 Oracle 生产环境，不新建站点，不修改其他项目。开始基线为 `e6656fa`；生产源码也干净且位于相同提交。

本记录补充前一轮 `2026-10-28-code-review.md`，其中的旧测试数量和“未推送部署”描述为前一轮的历史状态，不是本轮最终状态。

## 修复内容

1. 保留前轮已通过测试的作品状态越权、缓存可见性、软删除、评论父节点校验、并发计数、重复审核/删除、写请求重试和令牌刷新终止修复。
2. 新增共享 SQL 可见性约束：在计数、排序和分页之前校验公开、作者本人、粉丝、互关权限；管理员的合法作品查询不受影响。
3. 修复登录态与匿名信息流混用缓存、推荐序列先分页后过滤、关注列表使用单页缓存充当完整序列，以及游标先 LIMIT 后过滤导致的缺页。登录态最新/热门和关注分页使用带权限的 SQL 查询；匿名最新/热门和推荐 ID 序列缓存继续保留，推荐读取时先复核权限再分页。准确性优先于将不完整时间线作为分页来源。
4. HTTP/CDN 缓存隔离：已登录的作品列表、详情、推荐、最新和热门响应改为 `private, no-store`；匿名公共变体增加 `Vary: Authorization, Cookie`。关注数据也禁止持久缓存，避免私有内容被共享缓存保存。
5. 文件安全策略下沉后端：本地上传与文件代理响应使用不允许脚本或同源身份的 CSP sandbox，保留原始文件字节和 nosniff，不再只依赖生产 Nginx 的保护。
6. CORS 允许两个前端实际发送的 `X-Client-Type` 头；生产配置测试与已有的两个明确 HTTPS 来源保持一致，没有放开通配来源。
7. 修复依赖扫描告警：更新 DOMPurify、Axios、Vue、构建/测试工具及相关传递依赖；管理端图表升级到匹配的 ECharts 6 / Vue-ECharts 8，并加入真实 SVG 渲染兼容性用例。三个原本使用 jsdom 的用例文件现有显式测试依赖，不依赖隐式 peer 安装。
8. 前端组件按需加载，保留中文配置、图标、全局指令和消息服务。移除强制聚合整个 Element Plus 的分包配置，而非抬高告警阈值。最大 JS 分包：用户端 316,098 字节、管理端 583,265 字节，均小于原有 1,000,000 字节告警预算。
9. 前端 Docker 构建使用锁文件 `npm ci --legacy-peer-deps --ignore-scripts`，使发布依赖与验证版本一致。

每项应用缺陷先以失败测试复现，再修改生产逻辑；依赖风险以官方 registry 的 npm audit 失败结果作为升级前信号；分包问题通过读取真实产物的预算检查复现。

## 发布前验证

| 检查 | 实测结果 |
| --- | --- |
| 后端 `mvn -B verify` | 325 项，0 失败、0 错误、3 跳过；322 项实际通过，打包成功。 |
| 独立 MySQL 内容回归 | `mvn -B -Dreview.nativeMysql=true -Dtest=ContentAccessRegressionTest test`：57 项全部通过。 |
| 原 MySQL vendor 用例 | 9 项全部通过，已包含于上面的全量 325 项中，不再因 fixture 缺失跳过。 |
| 用户端干净安装 | `npm ci --legacy-peer-deps --ignore-scripts --no-audit` 成功。 |
| 用户端测试与生产构建 | 19 个文件、218 项通过；类型检查和 Vite 构建成功。 |
| 管理端干净安装 | `npm ci --legacy-peer-deps --ignore-scripts --no-audit` 成功。 |
| 管理端测试与生产构建 | 2 个文件、13 项通过，含 line/bar/pie 三种真实图表渲染；类型检查和构建成功。 |
| 两端完整依赖扫描 | 使用官方 npm registry，含开发依赖的 `npm audit --json` 均为 0 vulnerabilities。扫描结果仅代表该时点的已知 advisory，不是对未来漏洞的保证。 |
| 分包预算 | `python tests/check_frontend_bundles.py`：两端通过。 |
| 保密检查 | 9 项通过。 |
| 生产配置检查 | 5 项通过。 |
| 浏览器产物验证 | 两端实际生产构建的登录页正常显示表单；检查时两页均无 warning/error。未在浏览器内提交生产账户密码。 |
| 服务器预检 | 源码干净、Compose 配置合法、Nginx 配置检查通过；现有数据库 migration 版本为 5，本轮无 SQL 迁移变更。 |

剩余 3 项是 H2 条件下跳过的 MySQL 特有隔离场景，不通过改测试条件伪造覆盖；另行真实 MySQL vendor 测试已覆盖对应数据库行为。本地 fixture 使用隔离目录、专用测试 schema 和仅 loopback 的端口，没有连接或修改生产业务库。

## 发布约束与回滚

- 仅构建/替换 `server`、`client`、`admin` 三个应用；不运行全服务 update 脚本，不重启数据库、Redis、RabbitMQ、OpenSearch 或其他站点。
- 更新前保留三个旧镜像标签、全部运行容器快照和私有数据库压缩备份；备份保留在服务器的受限 release 目录，不进入 Git。
- 生产 Git 更新必须 fast-forward 且匹配已验证提交，不允许 reset 或 force push。
- 应用健康检查失败时自动使用旧镜像恢复三应用，不自动恢复数据库或覆盖新写入的数据。
- 上线后只定向清除作品/信息流缓存，不清空 Redis，不删除 JWT、刷新令牌、验证码或其他业务数据。
- 修复后续状态迁移逻辑，不擅自重算历史评论计数。没有进行全接口渗透、压力测试或所有登录后 UI 的人工验收；测试和依赖扫描通过不等于全系统无缺陷保证。

## 实际发布结果

- 应用提交：`890c720bdabe62d1f3551b256c066ff1d6c69568`，已 fast-forward 推送到 `origin/master`。
- 最终验收时间：**2026-10-28 15:23:56（Asia/Shanghai）**。
- 用户端入口：`https://zhixun.iflare.cn/zhixun/`；管理端入口：`https://zhixun.iflare.cn/admin/`。
- 运行镜像：`zhixun-production-server:890c720`、`zhixun-production-client:890c720`、`zhixun-production-admin:890c720`，三个容器均 healthy。验收时运行容器的镜像 ID 与构建镜像一致。
- 镜像 ID：server `sha256:df21473938b71f6f5d9af90a7316d1f3c9362bc0b50f43950ac9d16b0526dbda`；client `sha256:d0c00aaa05453b0e42d3740ddab26bc9a1aa3152c699eca0d7ee1eea67bc0716`；admin `sha256:df3c0f307f92dd94095835b10ba1c8f4ff373ca158eecde50d068b848a8621f9`。
- 发布证据目录：`/srv/zhixun/deployment/releases/890c720-20261028T072317Z`，包含 build/up 日志、前后容器快照、`verification.json`、`container-scope.json` 和 `images.json`。
- 数据库备份：该目录的 `database-before.sql.gz`，233,084 字节，权限 600，`gzip -t` 通过。没有导入、重建或回滚生产业务数据。
- 回滚镜像保留为三应用各自的 `rollback-before-890c720` 标签；对应 `rollback.compose.json` 保留在证据目录。
- 仅三应用被替换。比较首次发布前与最终发布后快照，数据库、Redis、RabbitMQ、OpenSearch 及其他站点容器 ID 均未变化。

### 实际线上检查

- 用户端和管理端 HTML 返回 200 且 no-store；用户端 4 个、管理端 3 个入口/预加载/样式资产返回 200，SHA-256 与运行镜像内文件逐一一致。
- 分类、最新、热门第二页、推荐 API 返回正常 HTTP/业务状态。
- 匿名查询草稿得到空列表且 total=0；匿名管理接口返回 401。
- 使用服务器既有管理员凭据完成登录、管理接口读取、令牌刷新和刷新后读取；凭据与令牌只在服务器内存处理，没有输出或写入本报告。
- 已认证作品列表实际返回 `private, no-store`；配置允许的另一 HTTPS 来源携带 `X-Client-Type` 的真实跨域预检通过。
- 绕过 Nginx直接请求后端上传路径，确认后端自身返回不允许脚本/同源身份的 sandbox CSP。
- 公网浏览器重新加载了新入口脚本 `/zhixun/assets/index-DpmosAwg.js`；首页显示 10 条内容，前 6 张图片解码成功；最新信息流切换、发现页懒加载、管理端登录页均正常，检查时无 warning/error。浏览器未提交生产账户凭据，登录后管理台仍以 API 和图表运行时测试验证，不声称已人工遍历所有管理页面。

### 首次验收误判与回滚记录

首次发布到 `/srv/zhixun/deployment/releases/890c720-20261028T071710Z` 时，三个应用已健康，但临时验收脚本把同源 OPTIONS 的正常响应（无需 CORS 响应头）当作跨域预检失败，触发自动恢复旧镜像。确认旧版本已健康后，将验收改成配置中允许的另一 HTTPS 来源，再次部署相同应用提交，最终所有检查通过。没有为通过检查放宽生产 CORS 规则，也没有恢复数据库或删除业务数据。

本地隔离 MySQL fixture 和两个临时前端预览服务已停止。后续仅补充发布文档提交，应用镜像仍对应上面的经过验证的代码提交；不因文档变更重启应用。
