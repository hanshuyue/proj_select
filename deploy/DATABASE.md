# 数据库准备

生产应用和 `install.sh` 均不会自动执行 SQL。MySQL `13316`、Redis `16389` 只是旧文档记录，必须先在服务器确认监听进程及归属；不得停止或修改 `expense-mysql.service`、`expense-redis.service`。

填写 `/etc/selectproject/selectproject.env` 后手动执行：

```bash
# 只允许目标数据库没有任何表；否则立即拒绝
sudo bash /发布目录/deploy/database.sh fresh

# 已有数据库：先 mysqldump 到部署目录 backups/database，再执行幂等结构升级
sudo bash /发布目录/deploy/database.sh upgrade
```

脚本首先校验发布包完整性，必须从完整发布目录运行。`10-scaffold-base.sql` 含 `DROP TABLE IF EXISTS`，但 `fresh` 会先查询目标库，只有表数量为 0 才运行。`upgrade` 永远不运行该文件，也不删除表或业务记录。

`40-collaboration.sql` 与应用开发环境的升级规则对应：补齐用户、文档和旧甄选表字段，按角色编码建立权限，补齐德州组织目录。角色 ID 由数据库分配，不依赖固定数字。普通员工 `external_user` 默认具有甄选及立项填报、修改、PPT生成和本人材料提交功能；采购专员 `biz_admin` 同时具有普通员工角色，并增加审批及模板管理权限。仅超级管理员 `super_admin` 可以设置采购专员身份；升级会撤销采购专员和旧 `sys_admin` 的管理员设置菜单关联，其余旧角色保留。

旧版自注册且仍为 `PENDING` 的账号迁移为可用普通员工；已审核后被停用的账号、其他审核状态及主动停用的角色均保留。升级不删除自定义角色、岗位或组织，也不覆盖人员姓名。生产 profile 禁止启动时迁移，所以仅更新 JAR 不会补齐权限：每次发布先运行 `database.sh upgrade`，再安装程序。

数据库账号至少需要目标库的建表、修改表、索引、创建/删除存储过程和读写权限。执行升级前还需要 `mysqldump` 能读取目标库。完成后应审查备份文件权限为 `600`。MySQL DDL 不保证整体事务回滚，中途失败应修复原因后重跑幂等升级，不能视作所有变更均已撤销。
