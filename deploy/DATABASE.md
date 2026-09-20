# 数据库准备

生产应用和 `install.sh` 均不会自动执行 SQL。MySQL `13316`、Redis `16389` 只是旧文档记录，必须先在服务器确认监听进程及归属；不得停止或修改 `expense-mysql.service`、`expense-redis.service`。

填写 `/etc/selectproject/selectproject.env` 后手动执行：

```bash
# 只允许目标数据库没有任何表；否则立即拒绝
sudo bash /发布目录/deploy/database.sh fresh

# 已有数据库：先 mysqldump 到部署目录 backups/database，再执行幂等结构升级
sudo bash /发布目录/deploy/database.sh upgrade
```

`10-scaffold-base.sql` 含 `DROP TABLE IF EXISTS`，但 `fresh` 会先查询目标库，只有表数量为 0 才运行。`upgrade` 永远不运行该文件，也不删除表或业务记录。升级脚本只补充缺失结构和必要的菜单、角色关联；已经存在的同 ID 数据通过 `INSERT IGNORE` 保留。

数据库账号至少需要目标库的建表、修改表、索引和读写权限。执行升级前还需要 `mysqldump` 能读取目标库。完成后应审查备份文件权限为 `600`。
