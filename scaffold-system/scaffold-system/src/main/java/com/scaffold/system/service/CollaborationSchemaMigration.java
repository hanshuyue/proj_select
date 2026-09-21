package com.scaffold.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/** 自动修复外部注册与文档中心依赖的数据库结构和德州组织数据。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "scaffold.migrations.enabled", havingValue = "true", matchIfMissing = true)
public class CollaborationSchemaMigration implements ApplicationRunner {
 private static final Logger log=LoggerFactory.getLogger(CollaborationSchemaMigration.class); private final JdbcTemplate jdbc;
 public CollaborationSchemaMigration(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Override public void run(ApplicationArguments args){if(!tableExists("sys_user")||!tableExists("sys_dept"))return;
  Map<String,String> cols=new LinkedHashMap<>();cols.put("real_name","VARCHAR(64) DEFAULT NULL COMMENT '实名认证姓名'");cols.put("must_change_password","TINYINT NOT NULL DEFAULT 0 COMMENT '下次登录必须修改密码'");cols.put("audit_status","VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT '注册状态'");cols.put("audit_remark","VARCHAR(500) DEFAULT NULL COMMENT '注册说明'");cols.put("audit_by","VARCHAR(64) DEFAULT NULL COMMENT '审核人'");cols.put("audit_time","DATETIME DEFAULT NULL COMMENT '审核时间'");cols.forEach(this::addUserColumn);
  jdbc.execute("CREATE TABLE IF NOT EXISTS user_document (id BIGINT NOT NULL AUTO_INCREMENT,title VARCHAR(200) NOT NULL,description VARCHAR(1000),original_name VARCHAR(255) NOT NULL,storage_path VARCHAR(500) NOT NULL,content_type VARCHAR(128),file_size BIGINT NOT NULL DEFAULT 0,status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',feedback TEXT,submitter_id BIGINT NOT NULL,submitter_name VARCHAR(64) NOT NULL,dept_id BIGINT,reviewer_id BIGINT,reviewer_name VARCHAR(64),submit_time DATETIME DEFAULT CURRENT_TIMESTAMP,review_time DATETIME,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,deleted TINYINT NOT NULL DEFAULT 0,PRIMARY KEY(id),KEY idx_document_submitter(submitter_id),KEY idx_document_status(status),KEY idx_document_dept(dept_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
  addDocumentColumn("business_type","VARCHAR(32) NOT NULL DEFAULT 'OTHER' COMMENT '业务类型'");addDocumentColumn("project_id","BIGINT DEFAULT NULL COMMENT '关联项目ID'");addDocumentColumn("project_name","VARCHAR(200) DEFAULT NULL COMMENT '关联项目名称'");addDocumentColumn("revision_no","INT NOT NULL DEFAULT 1 COMMENT '提交轮次'");addDocumentColumn("previous_document_id","BIGINT DEFAULT NULL COMMENT '上一轮文档ID'");
  jdbc.update("INSERT IGNORE INTO sys_role(role_name,role_code,data_scope,sort,status,builtin,remark,create_by,update_by) VALUES('普通员工','external_user','SELF',3,1,1,'填报材料并查看本人数据','system','system'),('采购专员','biz_admin','DEPT_AND_CHILD',2,1,1,'材料审批并拥有普通员工功能','system','system')");
  jdbc.update("UPDATE sys_user SET status=1,audit_status='APPROVED',audit_remark='注册即启用',audit_time=COALESCE(audit_time,NOW()) WHERE create_by='self-register' AND deleted=0 AND audit_status='PENDING'");
  seedDepartments(); simplifyRoles(); seedMenus(); log.info("外部注册与文档中心数据库结构检查完成");
 }
 private void seedDepartments(){String[] names={"德州市公司","德城区","陵城区","禹城市","乐陵市","宁津县","齐河县","临邑县","平原县","夏津县","武城县","庆云县","天衢新区"};long parent=deptId("德州市公司");if(parent==0){jdbc.update("INSERT INTO sys_dept(parent_id,ancestors,dept_name,sort,status,create_by,update_by) VALUES(0,'0',?,1,1,'system','system')",names[0]);parent=deptId(names[0]);}for(int i=1;i<names.length;i++)if(deptId(names[i])==0)jdbc.update("INSERT INTO sys_dept(parent_id,ancestors,dept_name,sort,status,create_by,update_by) VALUES(?,CONCAT('0,',?),?,?,1,'system','system')",parent,parent,names[i],i+1);}
 private void simplifyRoles(){
  // Match the explicit production migration; do not rename accounts or elevate legacy roles.
  jdbc.update("UPDATE sys_role SET role_name='采购专员',remark='材料审批并拥有普通员工功能' WHERE role_code='biz_admin' AND deleted=0");
  jdbc.update("INSERT IGNORE INTO sys_user_role(user_id,role_id) SELECT DISTINCT ur.user_id,e.id FROM sys_user_role ur JOIN sys_role p ON p.id=ur.role_id AND p.role_code='biz_admin' AND p.deleted=0 JOIN sys_role e ON e.role_code='external_user' AND e.deleted=0");
  jdbc.update("INSERT IGNORE INTO sys_user_role(user_id,role_id) SELECT u.id,r.id FROM sys_user u JOIN sys_role r ON r.role_code='external_user' AND r.deleted=0 WHERE u.create_by='self-register' AND u.deleted=0");
 }
 private void seedMenus(){
  jdbc.update("UPDATE sys_menu SET parent_id=0,menu_name='甄选结果',menu_type='dir',icon='document',path='/selection',component='',permission='',sort=5 WHERE id=500");
  jdbc.update("UPDATE sys_menu SET parent_id=500,menu_name='甄选结果管理',menu_type='menu',icon='document',path='/selection',component='selection/index',permission='selection:project:list',sort=1 WHERE id=501");
  jdbc.update("UPDATE sys_menu SET parent_id=500,menu_name='PPT模板管理',menu_type='menu',icon='upload',path='/selection/template',component='selection/template',permission='selection:template:list',sort=3 WHERE id=502");
  jdbc.update("INSERT IGNORE INTO sys_menu(id,parent_id,menu_name,menu_type,icon,path,component,permission,sort,visible,status,create_by,update_by) VALUES(700,0,'材料管理','dir','file','/collaboration','','',7,1,1,'system','system'),(701,700,'成品PPT记录','menu','file','/documents','documents/index','document:list',1,1,1,'system','system'),(702,700,'管理员设置','menu','users','/registration-review','registration-review/index','registration:review',2,1,1,'system','system')");
  jdbc.update("UPDATE sys_menu SET menu_name='PPT提交与审核',icon='file',path='/documents',component='documents/index',permission='document:list',sort=1 WHERE id=701");
  jdbc.update("UPDATE sys_menu SET parent_id=700,menu_name='管理员设置',icon='users',path='/registration-review',component='registration-review/index',permission='registration:review',sort=2 WHERE id=702");
  jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.id IN(700,701) WHERE r.role_code IN('super_admin','biz_admin') AND r.deleted=0");
  jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.id IN(500,501,502,503,5011,5012,5013,5014,5021,5022,5023,600,601,602,603,6011,6012,6013,6014,6031,6032,6033,700,701) WHERE r.role_code IN('biz_admin','super_admin') AND r.deleted=0");
  jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.id IN(500,501,503,5011,5012,5014,600,601,602,6011,6012,6014,700,701) WHERE r.role_code='external_user' AND r.deleted=0");
  jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT id,702 FROM sys_role WHERE role_code='super_admin' AND deleted=0");
  jdbc.update("DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.id=rm.role_id WHERE rm.menu_id=702 AND r.role_code IN('biz_admin','sys_admin')");
 }
 private long deptId(String name){Long id=jdbc.query("SELECT id FROM sys_dept WHERE dept_name=? AND deleted=0 ORDER BY id LIMIT 1",rs->rs.next()?rs.getLong(1):0L,name);return id==null?0:id;}
 private boolean tableExists(String table){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?",Integer.class,table);return n!=null&&n>0;}
 private void addUserColumn(String name,String definition){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sys_user' AND column_name=?",Integer.class,name);if(n!=null&&n>0)return;jdbc.execute("ALTER TABLE sys_user ADD COLUMN "+name+" "+definition);}
 private void addDocumentColumn(String name,String definition){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_document' AND column_name=?",Integer.class,name);if(n!=null&&n>0)return;jdbc.execute("ALTER TABLE user_document ADD COLUMN "+name+" "+definition);}
}
