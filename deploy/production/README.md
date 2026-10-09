# 知讯 Oracle 部署记录

日期：2026-10-07（Asia/Shanghai）。

## 地址与版本
- 网站：https://glint.novo.ccwu.cc/
- 管理后台：https://glint.novo.ccwu.cc/admin/
- 服务器：129.151.25.15，SSH 用户 ubuntu。
- 部署分支：master，已核实其提交比默认 main 更新。
- 部署提交：d9dbaaca540f3124d4e4d329b80b6503baba0369。
- 本地未提交的应用修改未上传，也未覆盖。

## 目录
- Git 源码：/srv/zhixun/repo
- 部署配置和检查脚本：/srv/zhixun/deployment
- 私有环境变量：/srv/zhixun/secrets/production.env（600）
- 管理员凭据：/srv/zhixun/secrets/administrator.txt（600）
- 本机凭据交接：D:/zhixun/.deployment-private/administrator.txt（当前 Windows 用户专有权限，Git 已忽略）
- Nginx：/etc/nginx/sites-available/glint.novo.ccwu.cc
- 首次启用站点备份：/var/backups/zhixun-domain-20261007T143501Z

## 运行与更新
使用独立 Compose 项目 zhixun-production 和独立持久卷。未复用其他站点数据库，也未占用宿主机已有 MySQL 3306 端口。

在本机 PowerShell 中运行：
```powershell
ssh -i "D:/zhixun/.ssh_key" ubuntu@129.151.25.15 "bash /srv/zhixun/deployment/update.sh"
```
更新脚本拒绝覆盖有未提交改动的服务器源码，执行 Git fast-forward 拉取、构建、健康检查和 Nginx reload。不会删除数据库或上传卷，也不会重新生成凭据。

## 基础防护
- 只有宿主机 Nginx 提供公网 HTTP/HTTPS；HTTP 转 HTTPS。
- API、前台和后台容器端口仅绑定 127.0.0.1。
- MySQL、Redis、RabbitMQ、OpenSearch 不发布宿主机端口。
- 数据库使用独立应用用户，Redis 和消息队列使用随机凭据；JWT、AES、SSR 使用独立随机密钥。
- 初始化数据的默认演示账号已禁用，单独创建强密码管理员；密码长度与应用 DTO 的 20 字符限制兼容。
- CORS 限定为该 HTTPS 域名；诊断接口、Swagger、隐藏文件禁止公网访问。
- 只有官方 Cloudflare IP 范围的访问才允许使用访客 IP 头；重写并清理其他转发头，避免伪造来源、主机或协议。
- 认证接口限流；文件预览响应限制脚本执行并设置 nosniff。
- 容器日志限额、自动重启，业务数据持久化。

原 MinIO 镜像无法拉取，采用项目已实现的本地文件存储：MINIO_ENABLED=false，上传目录挂载 uploads_data 持久卷。没有改动应用源码。已有忽略的本地数据库或历史对象存储未另行迁移。

证书仍为原有效证书（到期 2026-11-23 UTC）。仅对 glint 重新配置 webroot 验证方式；Certbot reconfigure 的模拟续期成功，certbot.timer 处于 active。未修改其他域名的证书配置。

## 验证结果
- 后端源代码单元测试：40 项，0 失败、0 错误、0 跳过。
- 生产配置检查：5 项通过。
- 个人主页检查：10 项通过，仅添加公开网站入口，没有添加知讯仓库链接。
- 源站 HTTPS 与服务器访问的公共域名：页面、前端资产、文章和分类 API 通过。
- 管理员登录、匿名管理接口拒绝访问、图片上传后匿名字节一致读回通过。
- 伪造 Forwarded / X-Forwarded-Host 不能降级安全 Cookie。
- 7 个业务/基础设施容器 healthy。
- 原考试系统健康接口正常；没有改动旅行、考试、监控站点配置或数据库。

## 明确的限制
1. 当前 Windows 电脑直接访问公共域名仍报连接重置，而服务器侧公共域名检查成功。尚未确认网络、代理、Cloudflare 或访问策略中的具体原因；没有修改本机网络设置。浏览器端完整业务流程未在该电脑确认。
2. 前台生产依赖 npm audit --omit=dev 报告 6 high、1 moderate、0 critical。涉及 Vue / SSR、Axios、nanoid、postcss、source-map-js 等依赖；这并不等同于逐项已证实可被利用，但也不能宣称不存在安全风险。未执行可能破坏兼容性的强制升级。
3. 没有进行完整应用安全审计、后端依赖漏洞扫描、压力测试或所有业务功能验收。

审计结果：/srv/zhixun/deployment/frontend-production-audit.json。
运行检查：verify-deployment.py 与 verify-security.py（在服务器执行，不打印凭据）。
