# SelectProject 生产部署文档

文档版本：V2.1（2026-09-21）

部署地址：`http://120.224.187.51:3333`

部署根目录：`/home/glory/workmobile/selectproject`

## 1. 部署结构与隔离原则

| 组件 | 监听地址 | 公网开放 |
|---|---|---|
| Nginx | `0.0.0.0:3333` | 是 |
| Spring Boot | `127.0.0.1:18082` | 否 |
| MySQL | 以生产机核验结果为准 | 否 |
| Redis | 以生产机核验结果为准 | 否 |

前端生产构建固定使用同源 `/api`；开发环境仍由 Vite 将 `/api` 代理到 `VITE_API_PROXY_TARGET`，默认 `http://localhost:8080`。后端没有显式 profile 时使用 `dev`，生产 systemd 命令行明确指定 `prod`。

旧文档记录的 MySQL `13316`、Redis `16389` 尚未连接服务器验证，部署前必须核验。不得停止或修改 `expense-mysql.service`、`expense-redis.service` 等其他项目服务；不得覆盖 `/etc/nginx/nginx.conf`。

生产目录：

```text
/home/glory/workmobile/selectproject/
├── app/gems-platform-server.jar
├── web/
├── releases/<版本>/
├── backups/
├── logs/
└── data/                       # 安装和升级均不覆盖
    ├── documents/
    ├── selection/templates/standard.pptx
    └── initiation/templates/standard.pptx
```

## 2. 本地构建与验证

要求 Windows PowerShell、Node.js 22+、npm、Maven、JDK 和 Python 3。打包脚本会安装锁定的前端依赖、运行前端单元测试及生产构建、运行相关 Java/PPT 测试、构建可执行 JAR，并扫描 JAR 内应用及依赖的基础 class 字节码来计算服务器最低 Java 版本。

```powershell
Set-Location D:\workmobile\base_test1
powershell -ExecutionPolicy Bypass -File .\deploy\package.ps1
```

固定输出：

```text
outputs/selectproject-release.tar.gz
outputs/selectproject-release.tar.gz.sha256
```

发布包只包含 `app/`、`web/`、`templates/`、`deploy/`。打包采用显式白名单，并生成 `deploy/SHA256SUMS`；不包含 `node_modules`、Maven缓存、真实生产配置和运行时业务数据。

## 3. 上传与解压

```powershell
scp .\outputs\selectproject-release.tar.gz* glory@120.224.187.51:/home/glory/workmobile/selectproject/incoming/
```

服务器执行：

```bash
set -euo pipefail
APP_ROOT=/home/glory/workmobile/selectproject
cd "$APP_ROOT/incoming"
sha256sum -c selectproject-release.tar.gz.sha256
RELEASE_DIR="$APP_ROOT/releases/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$RELEASE_DIR"
tar -xzf selectproject-release.tar.gz -C "$RELEASE_DIR"
```

## 4. 首次安装

```bash
sudo bash "$RELEASE_DIR/deploy/install.sh" \
  /home/glory/workmobile/selectproject \
  "$RELEASE_DIR"
```

首次执行会创建 `/etc/selectproject/selectproject.env`，设为 root 所有、权限 `600`，然后返回状态码 2 并停止，不启动应用。编辑该文件，将所有 `CHANGE_ME` 替换为生产值。必须设置数据库、Redis、随机 JWT 密钥及三个持久化目录；后端地址和端口必须保持 `127.0.0.1:18082`。

核验旧端口记录，确认服务归属：

```bash
sudo ss -ltnp | grep -E ':(13316|16389)\b' || true
sudo systemctl status expense-mysql.service expense-redis.service --no-pager || true
```

这些命令仅查看状态。根据实际结果填写环境文件，不对其他服务做任何修改。

准备目标数据库后，参照发布包的 `deploy/DATABASE.md` 执行以下二者之一：

```bash
# 新库：目标数据库必须已创建且完全没有表
sudo bash "$RELEASE_DIR/deploy/database.sh" fresh

# 已有库：先自动 mysqldump，再执行不删业务数据的幂等升级
sudo bash "$RELEASE_DIR/deploy/database.sh" upgrade
```

重新执行安装。脚本会验证发布包哈希、环境文件、实际 Java、端口占用、Nginx include 目录及 worker 用户，备份旧程序后安装。标准模板仅在持久化目标文件不存在时复制。

```bash
sudo bash "$RELEASE_DIR/deploy/install.sh" \
  /home/glory/workmobile/selectproject \
  "$RELEASE_DIR"

sudo systemctl enable --now selectproject
```

安装器把独立站点配置写入 Nginx 已启用的 `conf.d` 或 `sites-enabled` 目录，不覆盖主配置。若 3333 或 18082 被其他进程占用，或者目标配置文件不是本项目管理的文件，安装会拒绝继续。

## 5. 验收与日志

```bash
sudo systemctl status selectproject --no-pager
sudo journalctl -u selectproject -n 200 --no-pager
curl --fail --show-error http://127.0.0.1:18082/api/health
curl --fail --show-error http://127.0.0.1:3333/api/health
curl -I http://127.0.0.1:3333/
sudo nginx -t
```

业务验收应至少覆盖新账号注册后直接登录、普通员工甄选/立项填报与 PPT 导出、采购专员同时填报及审批、附件上传下载及 SPA 子路由刷新。切换权限后退出并重新登录以刷新权限。防火墙和云安全组只需放行 TCP 3333；18082、MySQL、Redis 不应对公网开放。

### 版本与配置一致性排查

开发使用 `dev`，生产使用 `prod`；数据库地址、密码和持久化目录应因环境而异，角色与功能授权规则应一致。开发数据库/Redis 可使用 `DEV_DB_URL`、`DEV_DB_USERNAME`、`DEV_DB_PASSWORD`、`DEV_REDIS_HOST`、`DEV_REDIS_PORT`、`DEV_REDIS_PASSWORD` 覆盖。生产值只写入 `/etc/selectproject/selectproject.env`，不可提交仓库。

发布同时打包前端、后端和 SQL。`build-info.txt` 记录基础提交和构建时间，`source-manifest.json` 记录实际源文件摘要（含未提交改动），`SHA256SUMS` 记录构建产物摘要。安装后这三个文件保存在 `app/`，程序回滚时一同恢复。对比本地发布目录和云端 `app/source-manifest.json` 的 `sourceSha256`，再校验实际 JAR/静态文件，避免仅凭 Git 提交号判断版本：

```bash
cat /home/glory/workmobile/selectproject/app/build-info.txt
python3 -c 'import json; print(json.load(open("/home/glory/workmobile/selectproject/app/source-manifest.json"))["sourceSha256"])'
cd /home/glory/workmobile/selectproject
tr -d '\r' < app/SHA256SUMS | grep -E '  (app/|web/)' | sha256sum -c -
```

如果产物一致但功能不同，检查是否已执行本次发布的数据库升级、账号角色及角色是否停用，并退出重登和刷新页面。旧版本没有上述安装元数据时，先比较 JAR 和前端文件摘要。本地审计不代表已核实云服务器运行版本。

## 6. 升级与回滚

升级前执行数据库 `upgrade`，它会先把数据库备份到 `backups/database/`。再按新版本目录运行 `install.sh`。安装器会备份当前 `app/`、`web/`、systemd 和 Nginx 配置；失败时自动恢复它们。`data/` 和 `/etc/selectproject/selectproject.env` 从不纳入版本覆盖。

需要人工回滚时，从安装输出记录的备份目录恢复 `app/` 和 `web/`，恢复对应 systemd/Nginx 文件后执行：

```bash
sudo systemctl daemon-reload
sudo nginx -t
sudo systemctl reload nginx
sudo systemctl restart selectproject
```

数据库只在确认需要时用对应的 `backups/database/*.sql` 人工恢复；程序回滚不会自动回滚数据库或业务文件。

## 7. 安全说明

- `scaffold-system/sql/scaffold_base_init.sql` 包含删表语句，只能由 `database.sh fresh` 在确认目标库表数量为零后执行。
- 生产关闭应用启动时的自动 schema、组织和角色迁移，数据库变更必须显式运行并保留备份。
- 旧仓库中的 `data/` 是运行数据，不属于发布模板；批准发布的两个标准模板单独位于 `deploy/templates/`。
- Git 不应提交真实环境文件、密钥、数据库导出、业务附件、用户上传模板或生成文档。
