-- Idempotent structural/catalog migration for existing databases.
-- It does not delete tables, users, projects, roles, departments, or documents.
DELIMITER $$
DROP PROCEDURE IF EXISTS selectproject_add_column$$
CREATE PROCEDURE selectproject_add_column(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN definition_value VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND column_name = column_name_value
    ) THEN
        SET @statement = CONCAT('ALTER TABLE `', table_name_value,
            '` ADD COLUMN `', column_name_value, '` ', definition_value);
        PREPARE migration_statement FROM @statement;
        EXECUTE migration_statement;
        DEALLOCATE PREPARE migration_statement;
    END IF;
END$$
DELIMITER ;

CALL selectproject_add_column('sys_user', 'real_name', 'VARCHAR(64) DEFAULT NULL');
CALL selectproject_add_column('sys_user', 'must_change_password', 'TINYINT NOT NULL DEFAULT 0');
CALL selectproject_add_column('sys_user', 'audit_status', 'VARCHAR(20) NOT NULL DEFAULT ''APPROVED''');
CALL selectproject_add_column('sys_user', 'audit_remark', 'VARCHAR(500) DEFAULT NULL');
CALL selectproject_add_column('sys_user', 'audit_by', 'VARCHAR(64) DEFAULT NULL');
CALL selectproject_add_column('sys_user', 'audit_time', 'DATETIME DEFAULT NULL');

CREATE TABLE IF NOT EXISTS user_document (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000) DEFAULT NULL,
    business_type VARCHAR(32) NOT NULL DEFAULT 'OTHER',
    project_id BIGINT DEFAULT NULL,
    project_name VARCHAR(200) DEFAULT NULL,
    revision_no INT NOT NULL DEFAULT 1,
    previous_document_id BIGINT DEFAULT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(128) DEFAULT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
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
    PRIMARY KEY (id),
    KEY idx_document_submitter (submitter_id),
    KEY idx_document_status (status),
    KEY idx_document_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CALL selectproject_add_column('user_document', 'business_type', 'VARCHAR(32) NOT NULL DEFAULT ''OTHER''');
CALL selectproject_add_column('user_document', 'project_id', 'BIGINT DEFAULT NULL');
CALL selectproject_add_column('user_document', 'project_name', 'VARCHAR(200) DEFAULT NULL');
CALL selectproject_add_column('user_document', 'revision_no', 'INT NOT NULL DEFAULT 1');
CALL selectproject_add_column('user_document', 'previous_document_id', 'BIGINT DEFAULT NULL');

-- CREATE TABLE IF NOT EXISTS in 20-selection.sql does not upgrade old tables.
CALL selectproject_add_column('biz_selection_project', 'publicity_website', 'VARCHAR(500) DEFAULT ''https://xe.sd.chinamobile.com/pms-portal-react/#/console4''');
CALL selectproject_add_column('biz_selection_project', 'review_report_name', 'VARCHAR(255) DEFAULT NULL');
CALL selectproject_add_column('biz_selection_project', 'review_report_path', 'VARCHAR(500) DEFAULT NULL');

DROP PROCEDURE selectproject_add_column;

INSERT IGNORE INTO sys_role
    (role_name, role_code, data_scope, sort, status, builtin, remark, create_by, update_by)
VALUES
    ('普通员工', 'external_user', 'SELF', 3, 1, 1, '填报材料并查看本人数据', 'system', 'system'),
    ('采购专员', 'biz_admin', 'DEPT_AND_CHILD', 2, 1, 1, '材料审批并拥有普通员工功能', 'system', 'system');

-- Preserve deliberately disabled roles and accounts, and existing custom roles.
UPDATE sys_role SET role_name='采购专员', remark='材料审批并拥有普通员工功能'
WHERE role_code='biz_admin' AND deleted=0;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT DISTINCT ur.user_id, employee.id FROM sys_user_role ur
JOIN sys_role purchaser ON purchaser.id=ur.role_id AND purchaser.role_code='biz_admin' AND purchaser.deleted=0
JOIN sys_role employee ON employee.role_code='external_user' AND employee.deleted=0;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u
JOIN sys_role r ON r.role_code='external_user' AND r.deleted=0
WHERE u.create_by='self-register' AND u.deleted=0;

-- Only migrate the old pending-registration flow; never reactivate disabled users.
UPDATE sys_user SET status=1, audit_status='APPROVED', audit_remark='注册即启用',
    audit_time=COALESCE(audit_time,NOW())
WHERE create_by='self-register' AND deleted=0 AND audit_status='PENDING';

INSERT IGNORE INTO sys_menu
    (id, parent_id, menu_name, menu_type, icon, path, component, permission,
     sort, visible, status, create_by, update_by)
VALUES
    (700, 0, '材料管理', 'dir', 'file', '/collaboration', '', '', 7, 1, 1, 'system', 'system'),
    (701, 700, 'PPT提交与审核', 'menu', 'file', '/documents', 'documents/index', 'document:list', 1, 1, 1, 'system', 'system'),
    (702, 700, '管理员设置', 'menu', 'users', '/registration-review', 'registration-review/index', 'registration:review', 2, 1, 1, 'system', 'system');

UPDATE sys_menu SET parent_id=0,menu_name='甄选结果',menu_type='dir',icon='document',path='/selection',component='',permission='',sort=5 WHERE id=500;
UPDATE sys_menu SET parent_id=500,menu_name='甄选结果管理',menu_type='menu',icon='document',path='/selection',component='selection/index',permission='selection:project:list',sort=1 WHERE id=501;
UPDATE sys_menu SET parent_id=500,menu_name='PPT模板管理',menu_type='menu',icon='upload',path='/selection/template',component='selection/template',permission='selection:template:list',sort=3 WHERE id=502;
UPDATE sys_menu SET menu_name='PPT提交与审核',icon='file',path='/documents',component='documents/index',permission='document:list',sort=1 WHERE id=701;
UPDATE sys_menu SET parent_id=700,menu_name='管理员设置',icon='users',path='/registration-review',component='registration-review/index',permission='registration:review',sort=2 WHERE id=702;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.id IN (700, 701)
WHERE role.role_code IN ('super_admin', 'biz_admin') AND role.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.id IN (700, 701)
WHERE role.role_code = 'external_user' AND role.deleted = 0;

-- Basic PPT creation, editing, generation and own submission for every employee.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id FROM sys_role role
JOIN sys_menu menu ON menu.id IN (500,501,503,5011,5012,5014,600,601,602,6011,6012,6014,700,701)
WHERE role.role_code IN ('external_user','biz_admin','super_admin') AND role.deleted=0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id FROM sys_role role
JOIN sys_menu menu ON menu.id IN (502,5013,5021,5022,5023,603,6013,6031,6032,6033)
WHERE role.role_code IN ('biz_admin','super_admin') AND role.deleted=0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT id,702 FROM sys_role WHERE role_code='super_admin' AND deleted=0;
DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.id=rm.role_id
WHERE rm.menu_id=702 AND r.role_code IN ('biz_admin','sys_admin');

-- Seed registration departments without deleting or renaming existing organizations.
INSERT INTO sys_dept(parent_id,ancestors,dept_name,sort,status,create_by,update_by)
SELECT 0,'0','德州市公司',1,1,'system','system'
WHERE NOT EXISTS(SELECT 1 FROM sys_dept WHERE dept_name='德州市公司' AND deleted=0);
SET @dezhou_parent=(SELECT MIN(id) FROM sys_dept WHERE dept_name='德州市公司' AND deleted=0);
INSERT INTO sys_dept(parent_id,ancestors,dept_name,sort,status,create_by,update_by)
SELECT @dezhou_parent,CONCAT('0,',@dezhou_parent),d.dept_name,d.sort,1,'system','system'
FROM (
    SELECT '德城区' dept_name,2 sort UNION ALL SELECT '陵城区',3 UNION ALL SELECT '禹城市',4
    UNION ALL SELECT '乐陵市',5 UNION ALL SELECT '宁津县',6 UNION ALL SELECT '齐河县',7
    UNION ALL SELECT '临邑县',8 UNION ALL SELECT '平原县',9 UNION ALL SELECT '夏津县',10
    UNION ALL SELECT '武城县',11 UNION ALL SELECT '庆云县',12 UNION ALL SELECT '天衢新区',13
) d WHERE NOT EXISTS(SELECT 1 FROM sys_dept existing WHERE existing.dept_name=d.dept_name AND existing.deleted=0);
