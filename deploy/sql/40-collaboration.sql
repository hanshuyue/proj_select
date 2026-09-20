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

DROP PROCEDURE selectproject_add_column;

INSERT IGNORE INTO sys_role
    (id, role_name, role_code, data_scope, sort, status, builtin, remark, create_by, update_by)
VALUES
    (7, '普通员工', 'external_user', 'SELF', 7, 1, 1, '普通员工', 'system', 'system');

INSERT IGNORE INTO sys_menu
    (id, parent_id, menu_name, menu_type, icon, path, component, permission,
     sort, visible, status, create_by, update_by)
VALUES
    (700, 0, '材料管理', 'dir', 'file', '/collaboration', '', '', 7, 1, 1, 'system', 'system'),
    (701, 700, 'PPT提交与审核', 'menu', 'file', '/documents', 'documents/index', 'document:list', 1, 1, 1, 'system', 'system'),
    (702, 700, '管理员设置', 'menu', 'users', '/registration-review', 'registration-review/index', 'registration:review', 2, 1, 1, 'system', 'system');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.id IN (700, 701, 702)
WHERE role.role_code IN ('super_admin', 'biz_admin') AND role.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.id IN (700, 701)
WHERE role.role_code = 'external_user' AND role.deleted = 0;
