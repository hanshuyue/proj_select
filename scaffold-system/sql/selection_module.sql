CREATE TABLE IF NOT EXISTS biz_selection_project (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_name VARCHAR(200) NOT NULL,
    opportunity_no VARCHAR(100) NOT NULL,
    department_name VARCHAR(100) DEFAULT NULL,
    report_year INT NOT NULL,
    report_month INT NOT NULL,
    service_limit_ex_tax DECIMAL(18,2) DEFAULT NULL,
    service_limit_inc_tax DECIMAL(18,2) DEFAULT NULL,
    service_tax_rate DECIMAL(8,4) DEFAULT NULL,
    resale_budget_ex_tax DECIMAL(18,2) DEFAULT NULL,
    resale_budget_inc_tax DECIMAL(18,2) DEFAULT NULL,
    resale_tax_rate DECIMAL(8,4) DEFAULT NULL,
    fee_min_ex_tax DECIMAL(18,2) DEFAULT NULL,
    fee_min_inc_tax DECIMAL(18,2) DEFAULT NULL,
    fee_tax_rate DECIMAL(8,4) DEFAULT NULL,
    selected_company VARCHAR(200) DEFAULT NULL,
    candidate_company VARCHAR(200) DEFAULT NULL,
    publicity_method VARCHAR(200) DEFAULT NULL,
    publicity_website VARCHAR(500) DEFAULT 'https://xe.sd.chinamobile.com/pms-portal-react/#/console4',
    publicity_enabled TINYINT NOT NULL DEFAULT 1,
    execution_method VARCHAR(500) DEFAULT NULL,
    cooperation_company VARCHAR(200) DEFAULT NULL,
    expansion_project VARCHAR(200) DEFAULT NULL,
    review_report_name VARCHAR(255) DEFAULT NULL,
    review_report_path VARCHAR(500) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_by VARCHAR(64) DEFAULT NULL,
    update_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_selection_project_name (project_name),
    KEY idx_selection_project_opportunity (opportunity_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='甄选结果项目';

-- 老版本数据库请由 SelectionSchemaMigration 在应用启动时自动补充
-- publicity_website、review_report_name、review_report_path 字段。

CREATE TABLE IF NOT EXISTS biz_selection_bidder (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    bidder_name VARCHAR(200) NOT NULL,
    service_ex_tax DECIMAL(18,2) DEFAULT NULL,
    service_tax_rate DECIMAL(8,4) DEFAULT NULL,
    service_inc_tax DECIMAL(18,2) DEFAULT NULL,
    resale_ex_tax DECIMAL(18,2) DEFAULT NULL,
    resale_tax_rate DECIMAL(8,4) DEFAULT NULL,
    resale_inc_tax DECIMAL(18,2) DEFAULT NULL,
    fee_amount DECIMAL(18,2) DEFAULT NULL,
    price_score DECIMAL(10,2) DEFAULT NULL,
    business_score DECIMAL(10,2) DEFAULT NULL,
    total_score DECIMAL(10,2) DEFAULT NULL,
    ranking INT DEFAULT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_selection_bidder_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='甄选投标人明细';

CREATE TABLE IF NOT EXISTS biz_selection_ppt_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_name VARCHAR(200) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    placeholder_keys TEXT DEFAULT NULL,
    default_flag TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_selection_template_default (default_flag, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='甄选结果PPT模板';

INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
VALUES
    (500, 0, '甄选结果', 'dir', 'document', '/selection', '', '', 5, 1, 1, 'system', 'system'),
    (501, 500, '甄选结果管理', 'menu', 'document', '/selection', 'selection/index', 'selection:project:list', 1, 1, 1, 'system', 'system'),
    (503, 500, '甄选结果填报', 'menu', 'edit', '/selection/form', 'selection/form', 'selection:project:add', 2, 1, 1, 'system', 'system'),
    (502, 500, 'PPT模板管理', 'menu', 'upload', '/selection/template', 'selection/template', 'selection:template:list', 3, 1, 1, 'system', 'system'),
    (5011, 501, '甄选结果新增', 'btn', '', '', '', 'selection:project:add', 1, 1, 1, 'system', 'system'),
    (5012, 501, '甄选结果修改', 'btn', '', '', '', 'selection:project:edit', 2, 1, 1, 'system', 'system'),
    (5013, 501, '甄选结果删除', 'btn', '', '', '', 'selection:project:remove', 3, 1, 1, 'system', 'system'),
    (5014, 501, '导出甄选PPT', 'btn', '', '', '', 'selection:project:export', 4, 1, 1, 'system', 'system'),
    (5021, 502, '上传PPT模板', 'btn', '', '', '', 'selection:template:upload', 1, 1, 1, 'system', 'system'),
    (5022, 502, '设置默认模板', 'btn', '', '', '', 'selection:template:edit', 2, 1, 1, 'system', 'system'),
    (5023, 502, '删除PPT模板', 'btn', '', '', '', 'selection:template:remove', 3, 1, 1, 'system', 'system');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 500 AND 5023;
