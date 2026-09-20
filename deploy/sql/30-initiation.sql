CREATE TABLE IF NOT EXISTS biz_initiation_project (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_name VARCHAR(255) NOT NULL,
  opportunity_no VARCHAR(100) NOT NULL,
  customer_name VARCHAR(255) DEFAULT NULL,
  customer_type VARCHAR(50) DEFAULT NULL,
  industry_type VARCHAR(100) DEFAULT NULL,
  department_name VARCHAR(100) DEFAULT NULL,
  project_manager VARCHAR(100) DEFAULT NULL,
  report_year INT DEFAULT NULL,
  report_month INT DEFAULT NULL,
  agreement_years INT DEFAULT NULL,
  contract_amount_inc_tax DECIMAL(18,2) DEFAULT NULL,
  total_revenue_inc_tax DECIMAL(18,2) DEFAULT NULL,
  total_cost_inc_tax DECIMAL(18,2) DEFAULT NULL,
  overall_profit_rate DECIMAL(10,2) DEFAULT NULL,
  fund_risk_level VARCHAR(30) DEFAULT NULL,
  three_line_level VARCHAR(30) DEFAULT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  section_data LONGTEXT DEFAULT NULL,
  version INT NOT NULL DEFAULT 0,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_initiation_project_name (project_name),
  KEY idx_initiation_opportunity_no (opportunity_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_initiation_finance_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  item_type VARCHAR(20) NOT NULL,
  mode_name VARCHAR(100) DEFAULT NULL,
  item_name VARCHAR(255) DEFAULT NULL,
  amount_inc_tax DECIMAL(18,2) DEFAULT NULL,
  tax_rate DECIMAL(10,2) DEFAULT NULL,
  amount_ex_tax DECIMAL(18,2) DEFAULT NULL,
  description VARCHAR(1000) DEFAULT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_initiation_finance_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_initiation_ppt_template (
  id BIGINT NOT NULL AUTO_INCREMENT,
  template_name VARCHAR(255) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  file_size BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  placeholder_keys TEXT DEFAULT NULL,
  default_flag TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_initiation_template_default (default_flag,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_initiation_attachment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  attachment_type VARCHAR(50) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  file_size BIGINT NOT NULL,
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id), KEY idx_initiation_attachment_project(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_menu(id,parent_id,menu_name,menu_type,icon,path,component,permission,sort,visible,status,create_by,update_by)
VALUES
(600,0,'项目立项','dir','folder','/initiation','','',6,1,1,'system','system'),
(601,600,'立项项目管理','menu','document','/initiation','initiation/index','initiation:project:list',1,1,1,'system','system'),
(602,600,'新建立项材料','menu','edit','/initiation/form','initiation/form','initiation:project:add',2,1,1,'system','system'),
(603,600,'PPT模板管理','menu','upload','/initiation/template','initiation/template','initiation:template:list',3,1,1,'system','system'),
(6011,601,'立项新增','btn','','','','initiation:project:add',1,1,1,'system','system'),
(6012,601,'立项修改','btn','','','','initiation:project:edit',2,1,1,'system','system'),
(6013,601,'立项删除','btn','','','','initiation:project:remove',3,1,1,'system','system'),
(6014,601,'导出立项PPT','btn','','','','initiation:project:export',4,1,1,'system','system'),
(6031,603,'上传PPT模板','btn','','','','initiation:template:upload',1,1,1,'system','system'),
(6032,603,'设置默认模板','btn','','','','initiation:template:edit',2,1,1,'system','system'),
(6033,603,'删除PPT模板','btn','','','','initiation:template:remove',3,1,1,'system','system')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name),component=VALUES(component),permission=VALUES(permission);

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT 1,id FROM sys_menu WHERE id IN (600,601,602,603,6011,6012,6013,6014,6031,6032,6033);
