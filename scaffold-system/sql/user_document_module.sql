ALTER TABLE sys_user ADD COLUMN real_name VARCHAR(64) DEFAULT NULL COMMENT '实名认证姓名' AFTER nickname;
ALTER TABLE sys_user ADD COLUMN must_change_password TINYINT NOT NULL DEFAULT 0 COMMENT '下次登录必须修改密码' AFTER status;
ALTER TABLE sys_user ADD COLUMN audit_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT '注册审核状态' AFTER status;
ALTER TABLE sys_user ADD COLUMN audit_remark VARCHAR(500) DEFAULT NULL COMMENT '注册审核意见' AFTER audit_status;
ALTER TABLE sys_user ADD COLUMN audit_by VARCHAR(64) DEFAULT NULL COMMENT '审核人' AFTER audit_remark;
ALTER TABLE sys_user ADD COLUMN audit_time DATETIME DEFAULT NULL COMMENT '审核时间' AFTER audit_by;
ALTER TABLE sys_user ADD UNIQUE KEY uk_sys_user_phone (phone);

INSERT IGNORE INTO sys_role (id,role_name,role_code,data_scope,sort,status,builtin,remark,create_by,update_by)
VALUES (7,'外部用户','external_user','SELF',7,1,1,'注册后直接使用文档提交功能','system','system');

CREATE TABLE IF NOT EXISTS user_document (
 id BIGINT NOT NULL AUTO_INCREMENT,
 title VARCHAR(200) NOT NULL,
 description VARCHAR(1000) DEFAULT NULL,
 business_type VARCHAR(32) NOT NULL DEFAULT 'OTHER' COMMENT 'SELECTION/INITIATION/OTHER',
 project_id BIGINT DEFAULT NULL,
 project_name VARCHAR(200) DEFAULT NULL,
 revision_no INT NOT NULL DEFAULT 1 COMMENT '提交轮次',
 previous_document_id BIGINT DEFAULT NULL COMMENT '上一轮文档ID',
 original_name VARCHAR(255) NOT NULL,
 storage_path VARCHAR(500) NOT NULL,
 content_type VARCHAR(128) DEFAULT NULL,
 file_size BIGINT NOT NULL DEFAULT 0,
 status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/APPROVED/REVISION_REQUIRED',
 feedback TEXT DEFAULT NULL,
 submitter_id BIGINT NOT NULL,
 submitter_name VARCHAR(64) NOT NULL,
 dept_id BIGINT DEFAULT NULL,
 reviewer_id BIGINT DEFAULT NULL,
 reviewer_name VARCHAR(64) DEFAULT NULL,
 submit_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 review_time DATETIME DEFAULT NULL,
 update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted TINYINT NOT NULL DEFAULT 0,
 PRIMARY KEY(id), KEY idx_document_submitter(submitter_id), KEY idx_document_status(status), KEY idx_document_dept(dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部用户文档提交与点评';

INSERT IGNORE INTO sys_menu (id,parent_id,menu_name,menu_type,icon,path,component,permission,sort,visible,status,create_by,update_by) VALUES
(700,0,'材料管理','dir','file','/collaboration','','',7,1,1,'system','system'),
(701,700,'成品PPT记录','menu','file','/documents','documents/index','document:list',1,1,1,'system','system'),
(702,700,'管理员设置','menu','users','/registration-review','registration-review/index','registration:review',2,1,1,'system','system');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id) VALUES
(1,700),(1,701),(1,702),(2,700),(2,701),(2,702),(3,700),(3,701),(3,702),(7,700),(7,701);
