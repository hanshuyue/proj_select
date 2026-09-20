-- ============================================================
-- 后台管理系统底座 - 统一初始化脚本
-- 版本: 1.0.0
-- 说明: 包含系统管理、权限控制、工作流等核心功能的表结构和种子数据
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 第一部分: 表结构定义
-- ============================================================

-- ----------------------------
-- 部门表
-- ----------------------------
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '部门 ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '上级部门 ID',
    ancestors VARCHAR(500) NOT NULL DEFAULT '' COMMENT '祖级列表',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    leader VARCHAR(64) DEFAULT NULL COMMENT '部门负责人',
    phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '部门状态：1 正常，0 停用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：1 删除，0 正常',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_sys_dept_parent (parent_id),
    KEY idx_sys_dept_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统部门表';

-- ----------------------------
-- 岗位表
-- ----------------------------
DROP TABLE IF EXISTS sys_post;
CREATE TABLE sys_post (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '岗位 ID',
    post_code VARCHAR(64) NOT NULL COMMENT '岗位编码',
    post_name VARCHAR(100) NOT NULL COMMENT '岗位名称',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '岗位状态：1 正常，0 停用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：1 删除，0 正常',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_post_code (post_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统岗位表';

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    password VARCHAR(128) NOT NULL COMMENT '登录密码密文',
    nickname VARCHAR(64) NOT NULL COMMENT '用户昵称',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    dept_id BIGINT DEFAULT NULL COMMENT '所属部门 ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：1 正常，0 停用',
    must_change_password TINYINT NOT NULL DEFAULT 0 COMMENT '下次登录必须修改密码：1 是，0 否',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：1 删除，0 正常',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_dept (dept_id),
    KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ----------------------------
-- 角色表
-- ----------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色 ID',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据权限范围',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '角色状态：1 正常，0 停用',
    builtin TINYINT NOT NULL DEFAULT 0 COMMENT '是否内置角色：1 是，0 否',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：1 删除，0 正常',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- ----------------------------
-- 菜单权限表
-- ----------------------------
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id BIGINT NOT NULL COMMENT '菜单 ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父级菜单 ID',
    menu_name VARCHAR(100) NOT NULL COMMENT '菜单名称',
    menu_type VARCHAR(16) NOT NULL COMMENT '菜单类型：dir 目录，menu 菜单，btn 按钮',
    icon VARCHAR(64) DEFAULT NULL COMMENT '菜单图标',
    path VARCHAR(200) DEFAULT NULL COMMENT '前端路由路径',
    component VARCHAR(200) DEFAULT NULL COMMENT '前端组件路径',
    permission VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序号',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示：1 显示，0 隐藏',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '菜单状态：1 正常，0 停用',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_sys_menu_parent (parent_id),
    KEY idx_sys_menu_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单和按钮权限表';

-- ----------------------------
-- 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ----------------------------
-- 用户岗位关联表
-- ----------------------------
DROP TABLE IF EXISTS sys_user_post;
CREATE TABLE sys_user_post (
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    post_id BIGINT NOT NULL COMMENT '岗位 ID',
    PRIMARY KEY (user_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户岗位关联表';

-- ----------------------------
-- 角色菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    menu_id BIGINT NOT NULL COMMENT '菜单 ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ----------------------------
-- 角色部门关联表（数据权限）
-- ----------------------------
DROP TABLE IF EXISTS sys_role_dept;
CREATE TABLE sys_role_dept (
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    dept_id BIGINT NOT NULL COMMENT '部门 ID',
    PRIMARY KEY (role_id, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色自定义数据权限部门关联表';

-- ----------------------------
-- 字典类型表
-- ----------------------------
DROP TABLE IF EXISTS sys_dict_type;
CREATE TABLE sys_dict_type (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '字典类型 ID',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典类型名称',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '字典状态：1 正常，0 停用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- ----------------------------
-- 字典数据表
-- ----------------------------
DROP TABLE IF EXISTS sys_dict_data;
CREATE TABLE sys_dict_data (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '字典数据 ID',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    dict_label VARCHAR(100) NOT NULL COMMENT '字典项显示文本',
    dict_value VARCHAR(100) NOT NULL COMMENT '字典项实际值',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '字典项状态：1 正常，0 停用',
    tone VARCHAR(32) DEFAULT NULL COMMENT '前端标签色调',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认项：1 是，0 否',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_sys_dict_data_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- ----------------------------
-- 参数配置表
-- ----------------------------
DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '参数 ID',
    config_name VARCHAR(100) NOT NULL COMMENT '参数名称',
    config_key VARCHAR(100) NOT NULL COMMENT '参数键名',
    config_value VARCHAR(500) NOT NULL COMMENT '参数键值',
    config_type CHAR(1) NOT NULL DEFAULT 'N' COMMENT '是否系统内置：Y 是，N 否',
    builtin TINYINT NOT NULL DEFAULT 0 COMMENT '是否内置参数：1 是，0 否',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数配置表';

-- ----------------------------
-- 操作日志表
-- ----------------------------
DROP TABLE IF EXISTS sys_oper_log;
CREATE TABLE sys_oper_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '操作日志 ID',
    module VARCHAR(100) NOT NULL COMMENT '操作模块',
    action VARCHAR(64) NOT NULL COMMENT '操作类型',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
    request_url VARCHAR(500) DEFAULT NULL COMMENT '请求地址',
    request_params TEXT COMMENT '请求参数',
    oper_name VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    dept_name VARCHAR(100) DEFAULT NULL COMMENT '部门名称',
    ip VARCHAR(64) DEFAULT NULL COMMENT 'IP 地址',
    location VARCHAR(128) DEFAULT NULL COMMENT '操作地点',
    cost BIGINT DEFAULT NULL COMMENT '耗时毫秒',
    result TINYINT NOT NULL DEFAULT 1 COMMENT '操作结果：1 成功，0 失败',
    error_msg TEXT COMMENT '错误信息',
    oper_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_sys_oper_log_time (oper_time),
    KEY idx_sys_oper_log_oper_name (oper_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ----------------------------
-- 登录日志表
-- ----------------------------
DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '登录日志 ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '登录账号',
    ip VARCHAR(64) DEFAULT NULL COMMENT 'IP 地址',
    location VARCHAR(128) DEFAULT NULL COMMENT '登录地点',
    browser VARCHAR(128) DEFAULT NULL COMMENT '浏览器',
    os VARCHAR(128) DEFAULT NULL COMMENT '操作系统',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '登录状态：1 成功，0 失败',
    msg VARCHAR(500) DEFAULT NULL COMMENT '提示消息',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (id),
    KEY idx_sys_login_log_time (login_time),
    KEY idx_sys_login_log_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ----------------------------
-- 文件信息表
-- ----------------------------
DROP TABLE IF EXISTS sys_file;
CREATE TABLE sys_file (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件 ID',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    storage_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
    url VARCHAR(500) NOT NULL COMMENT '访问地址',
    content_type VARCHAR(128) DEFAULT NULL COMMENT '文件类型',
    size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小字节数',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- ----------------------------
-- 代码生成表配置
-- ----------------------------
DROP TABLE IF EXISTS gen_table;
CREATE TABLE gen_table (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '代码生成表 ID',
    table_name VARCHAR(100) NOT NULL COMMENT '数据库表名',
    table_comment VARCHAR(200) DEFAULT NULL COMMENT '表说明',
    class_name VARCHAR(100) NOT NULL COMMENT '后端实体类名',
    module_name VARCHAR(64) NOT NULL COMMENT '模块名称',
    business_name VARCHAR(64) NOT NULL COMMENT '业务名称',
    synced TINYINT NOT NULL DEFAULT 0 COMMENT '是否同步数据库字段：1 是，0 否',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_gen_table_name (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成表配置';

-- ----------------------------
-- 代码生成字段配置
-- ----------------------------
DROP TABLE IF EXISTS gen_table_column;
CREATE TABLE gen_table_column (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '代码生成字段 ID',
    table_id BIGINT NOT NULL COMMENT '代码生成表 ID',
    column_name VARCHAR(100) NOT NULL COMMENT '数据库字段名',
    java_field VARCHAR(100) NOT NULL COMMENT 'Java 字段名',
    java_type VARCHAR(64) NOT NULL COMMENT 'Java 类型',
    jdbc_type VARCHAR(64) NOT NULL COMMENT 'JDBC 类型',
    column_comment VARCHAR(200) DEFAULT NULL COMMENT '字段说明',
    is_insert TINYINT NOT NULL DEFAULT 1 COMMENT '是否新增字段',
    is_edit TINYINT NOT NULL DEFAULT 1 COMMENT '是否编辑字段',
    is_list TINYINT NOT NULL DEFAULT 1 COMMENT '是否列表展示',
    is_query TINYINT NOT NULL DEFAULT 0 COMMENT '是否查询字段',
    query_type VARCHAR(32) NOT NULL DEFAULT '=' COMMENT '查询方式',
    form_type VARCHAR(64) NOT NULL DEFAULT '文本框' COMMENT '表单控件类型',
    required TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (id),
    KEY idx_gen_table_column_table (table_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成字段配置';

-- ----------------------------
-- 工作流业务实例关联表
-- ----------------------------
DROP TABLE IF EXISTS wf_business;
CREATE TABLE wf_business (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '工作流业务绑定 ID',
    business_key VARCHAR(128) NOT NULL COMMENT '业务单据唯一标识',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    business_title VARCHAR(255) DEFAULT NULL COMMENT '业务标题',
    process_definition_id VARCHAR(128) DEFAULT NULL COMMENT '流程定义 ID',
    process_instance_id VARCHAR(128) DEFAULT NULL COMMENT '流程实例 ID',
    business_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '业务审批状态',
    starter VARCHAR(64) DEFAULT NULL COMMENT '流程发起人',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wf_business_key_type (business_key, business_type),
    KEY idx_wf_business_instance (process_instance_id),
    KEY idx_wf_business_status (business_status),
    KEY idx_wf_business_starter (starter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流业务实例关联表';

-- ----------------------------
-- 流程模型业务扩展元数据表
-- ----------------------------
DROP TABLE IF EXISTS wf_model_meta;
CREATE TABLE wf_model_meta (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流程模型元数据 ID',
    model_id VARCHAR(128) DEFAULT NULL COMMENT 'Flowable 模型 ID',
    model_key VARCHAR(100) NOT NULL COMMENT '流程模型标识',
    model_name VARCHAR(100) NOT NULL COMMENT '流程模型名称',
    category VARCHAR(100) DEFAULT NULL COMMENT '流程分类',
    deployed TINYINT NOT NULL DEFAULT 0 COMMENT '是否已部署：1 是，0 否',
    description VARCHAR(500) DEFAULT NULL COMMENT '模型说明',
    bpmn_xml LONGTEXT NULL COMMENT 'BPMN XML内容',
    deployment_id VARCHAR(128) NULL COMMENT 'Flowable部署ID',
    process_definition_id VARCHAR(128) NULL COMMENT 'Flowable流程定义ID',
    version INT NOT NULL DEFAULT 1 COMMENT '流程版本',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wf_model_key (model_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模型业务扩展元数据表';

-- ----------------------------
-- 工作流审批记录表
-- ----------------------------
DROP TABLE IF EXISTS wf_approval_record;
CREATE TABLE wf_approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_id BIGINT NOT NULL COMMENT '业务绑定ID',
    process_instance_id VARCHAR(128) NOT NULL COMMENT '流程实例ID',
    task_id VARCHAR(128) NULL COMMENT 'Flowable任务ID',
    task_name VARCHAR(128) NULL COMMENT '任务节点名称',
    action VARCHAR(32) NOT NULL COMMENT '审批动作',
    comment VARCHAR(1000) NULL COMMENT '审批意见',
    assignee VARCHAR(64) NULL COMMENT '处理人',
    result_status VARCHAR(32) NOT NULL COMMENT '处理后业务状态',
    handle_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
    create_by VARCHAR(64) NULL,
    update_by VARCHAR(64) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_wf_record_business (business_id),
    INDEX idx_wf_record_instance (process_instance_id),
    INDEX idx_wf_record_assignee (assignee),
    INDEX idx_wf_record_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流审批记录表';


-- ============================================================
-- 第二部分: 种子数据
-- ============================================================

-- ----------------------------
-- 部门数据
-- ----------------------------
INSERT INTO sys_dept (id, parent_id, ancestors, dept_name, leader, phone, email, sort, status, create_by, update_by)
VALUES
    (1, 0, '0', '德州市公司', NULL, NULL, NULL, 1, 1, 'system', 'system'),
    (2, 1, '0,1', '德城区', NULL, NULL, NULL, 2, 1, 'system', 'system'),
    (3, 1, '0,1', '陵城区', NULL, NULL, NULL, 3, 1, 'system', 'system'),
    (4, 1, '0,1', '禹城市', NULL, NULL, NULL, 4, 1, 'system', 'system'),
    (5, 1, '0,1', '乐陵市', NULL, NULL, NULL, 5, 1, 'system', 'system'),
    (6, 1, '0,1', '宁津县', NULL, NULL, NULL, 6, 1, 'system', 'system'),
    (7, 1, '0,1', '齐河县', NULL, NULL, NULL, 7, 1, 'system', 'system'),
    (8, 1, '0,1', '临邑县', NULL, NULL, NULL, 8, 1, 'system', 'system'),
    (9, 1, '0,1', '平原县', NULL, NULL, NULL, 9, 1, 'system', 'system'),
    (10, 1, '0,1', '夏津县', NULL, NULL, NULL, 10, 1, 'system', 'system'),
    (11, 1, '0,1', '武城县', NULL, NULL, NULL, 11, 1, 'system', 'system'),
    (12, 1, '0,1', '庆云县', NULL, NULL, NULL, 12, 1, 'system', 'system'),
    (13, 1, '0,1', '天衢新区', NULL, NULL, NULL, 13, 1, 'system', 'system');

-- ----------------------------
-- 岗位数据
-- ----------------------------
INSERT INTO sys_post (id, post_code, post_name, sort, status, remark, create_by, update_by)
VALUES
    (1, 'project_owner', '项目负责人', 1, 1, '负责项目统筹', 'system', 'system'),
    (2, 'material_editor', '材料填报人', 2, 1, '负责材料填报与提交', 'system', 'system'),
    (3, 'business_reviewer', '审核人员', 3, 1, '负责业务审核', 'system', 'system');

-- ----------------------------
-- 角色数据
-- ----------------------------
INSERT INTO sys_role (id, role_name, role_code, data_scope, sort, status, builtin, remark, create_by, update_by)
VALUES
    (1, '超级管理员', 'super_admin', 'ALL', 1, 1, 1, '系统最高权限，不可删除', 'system', 'system'),
    (2, '审批专员', 'biz_admin', 'DEPT_AND_CHILD', 2, 1, 1, '负责材料审核和业务管理', 'system', 'system'),
    (3, '普通员工', 'external_user', 'SELF', 3, 1, 1, '填报材料并查看本人数据', 'system', 'system');

-- ----------------------------
-- 用户数据（默认密码: admin123）
-- ----------------------------
INSERT INTO sys_user (id, username, password, nickname, phone, email, dept_id, status, create_by, update_by)
VALUES
    (1, 'admin', '$2b$10$L7q28dpKkcfrrUzfvZebqe7pvxcKpaSgh7EUenf9Q5z2LiYKxDpIu', '网站管理大王', '138-0011-8800', 'zhoumy@yunyuan.com', 5, 1, 'system', 'system'),
    (2, 'chensy', '$2b$10$L7q28dpKkcfrrUzfvZebqe7pvxcKpaSgh7EUenf9Q5z2LiYKxDpIu', '陈思远', '139-0022-1010', 'chensy@yunyuan.com', 5, 1, 'system', 'system'),
    (3, 'wumin', '$2b$10$L7q28dpKkcfrrUzfvZebqe7pvxcKpaSgh7EUenf9Q5z2LiYKxDpIu', '吴敏', '137-0033-1012', 'wumin@yunyuan.com', 6, 1, 'system', 'system');

-- ----------------------------
-- 用户角色关联
-- ----------------------------
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (2, 2), (3, 2);

-- ----------------------------
-- 用户岗位关联
-- ----------------------------
INSERT INTO sys_user_post (user_id, post_id) VALUES (1, 3), (2, 2), (3, 2);

-- ----------------------------
-- 菜单权限数据（底座核心菜单）
-- ----------------------------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
VALUES
    -- 工作台
    (1, 0, '工作台', 'menu', 'dashboard', '/dashboard', 'dashboard/index', '', 1, 1, 1, 'system', 'system'),

    -- 系统管理
    (100, 0, '系统管理', 'dir', 'gear', '/system', '', '', 2, 1, 1, 'system', 'system'),
    (101, 100, '用户管理', 'menu', 'users', '/system/user', 'system/user/index', 'system:user:list', 1, 1, 1, 'system', 'system'),
    (1011, 101, '用户查询', 'btn', '', '', '', 'system:user:query', 1, 1, 1, 'system', 'system'),
    (1012, 101, '用户新增', 'btn', '', '', '', 'system:user:add', 2, 1, 1, 'system', 'system'),
    (1013, 101, '用户修改', 'btn', '', '', '', 'system:user:edit', 3, 1, 1, 'system', 'system'),
    (1014, 101, '用户删除', 'btn', '', '', '', 'system:user:remove', 4, 1, 1, 'system', 'system'),
    (1015, 101, '重置密码', 'btn', '', '', '', 'system:user:resetPwd', 5, 1, 1, 'system', 'system'),
    (102, 100, '角色管理', 'menu', 'role', '/system/role', 'system/role/index', 'system:role:list', 2, 1, 1, 'system', 'system'),
    (1021, 102, '角色查询', 'btn', '', '', '', 'system:role:query', 1, 1, 1, 'system', 'system'),
    (1022, 102, '角色新增', 'btn', '', '', '', 'system:role:add', 2, 1, 1, 'system', 'system'),
    (1023, 102, '角色修改', 'btn', '', '', '', 'system:role:edit', 3, 1, 1, 'system', 'system'),
    (1024, 102, '角色删除', 'btn', '', '', '', 'system:role:remove', 4, 1, 1, 'system', 'system'),
    (103, 100, '菜单管理', 'menu', 'menu', '/system/menu', 'system/menu/index', 'system:menu:list', 3, 1, 1, 'system', 'system'),
    (1031, 103, '菜单新增', 'btn', '', '', '', 'system:menu:add', 1, 1, 1, 'system', 'system'),
    (1032, 103, '菜单修改', 'btn', '', '', '', 'system:menu:edit', 2, 1, 1, 'system', 'system'),
    (1033, 103, '菜单删除', 'btn', '', '', '', 'system:menu:remove', 3, 1, 1, 'system', 'system'),
    (104, 100, '部门管理', 'menu', 'dept', '/system/dept', 'system/dept/index', 'system:dept:list', 4, 1, 1, 'system', 'system'),
    (1041, 104, '部门新增', 'btn', '', '', '', 'system:dept:add', 1, 1, 1, 'system', 'system'),
    (1042, 104, '部门修改', 'btn', '', '', '', 'system:dept:edit', 2, 1, 1, 'system', 'system'),
    (1043, 104, '部门删除', 'btn', '', '', '', 'system:dept:remove', 3, 1, 1, 'system', 'system'),
    (107, 100, '岗位管理', 'menu', 'post', '/system/post', 'system/post/index', 'system:post:list', 5, 1, 1, 'system', 'system'),
    (1071, 107, '岗位新增', 'btn', '', '', '', 'system:post:add', 1, 1, 1, 'system', 'system'),
    (1072, 107, '岗位修改', 'btn', '', '', '', 'system:post:edit', 2, 1, 1, 'system', 'system'),
    (1073, 107, '岗位删除', 'btn', '', '', '', 'system:post:remove', 3, 1, 1, 'system', 'system'),
    (105, 100, '字典管理', 'menu', 'dict', '/system/dict', 'system/dict/index', 'system:dict:list', 6, 1, 1, 'system', 'system'),
    (1051, 105, '字典类型新增', 'btn', '', '', '', 'system:dict:add', 1, 1, 1, 'system', 'system'),
    (1052, 105, '字典类型修改', 'btn', '', '', '', 'system:dict:edit', 2, 1, 1, 'system', 'system'),
    (1053, 105, '字典类型删除', 'btn', '', '', '', 'system:dict:remove', 3, 1, 1, 'system', 'system'),
    (106, 100, '参数配置', 'menu', 'gear', '/system/config', 'system/config/index', 'system:config:list', 7, 1, 1, 'system', 'system'),
    (1061, 106, '参数新增', 'btn', '', '', '', 'system:config:add', 1, 1, 1, 'system', 'system'),
    (1062, 106, '参数修改', 'btn', '', '', '', 'system:config:edit', 2, 1, 1, 'system', 'system'),
    (1063, 106, '参数删除', 'btn', '', '', '', 'system:config:remove', 3, 1, 1, 'system', 'system'),

    -- 系统监控
    (200, 0, '系统监控', 'dir', 'log', '/monitor', '', '', 3, 1, 1, 'system', 'system'),
    (201, 200, '操作日志', 'menu', 'file', '/monitor/oper-log', 'monitor/operlog/index', 'monitor:operlog:list', 1, 1, 1, 'system', 'system'),
    (202, 200, '登录日志', 'menu', 'lock', '/monitor/login-log', 'monitor/loginlog/index', 'monitor:loginlog:list', 2, 1, 1, 'system', 'system'),

    -- 工作流程
    (300, 0, '工作流程', 'dir', 'flow', '/workflow', '', '', 4, 1, 1, 'system', 'system'),
    (301, 300, '流程模型', 'menu', 'grid', '/workflow/model', 'workflow/model/index', 'workflow:model:list', 1, 1, 1, 'system', 'system'),
    (302, 300, '流程定义', 'menu', 'file', '/workflow/definition', 'workflow/def/index', 'workflow:def:list', 2, 1, 1, 'system', 'system'),
    (303, 300, '我的待办', 'menu', 'bell', '/workflow/todo', 'workflow/todo/index', 'workflow:todo:list', 3, 1, 1, 'system', 'system'),
    (304, 300, '我的已办', 'menu', 'check', '/workflow/done', 'workflow/done/index', 'workflow:done:list', 4, 1, 1, 'system', 'system'),

    -- 系统工具
    (400, 0, '系统工具', 'dir', 'tool', '/tool', '', '', 5, 1, 1, 'system', 'system'),
    (401, 400, '代码生成', 'menu', 'command', '/tool/generator', 'tool/gen/index', 'tool:gen:list', 1, 1, 1, 'system', 'system');

-- ----------------------------
-- 超级管理员拥有所有菜单权限
-- ----------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- ----------------------------
-- 字典类型数据
-- ----------------------------
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark, create_by, update_by)
VALUES
    (1, '用户性别', 'sys_user_sex', 1, '用户性别列表', 'system', 'system'),
    (2, '系统开关', 'sys_normal_disable', 1, '系统正常 / 停用状态', 'system', 'system'),
    (3, '审批结果', 'wf_approve_result', 1, '工作流审批结果', 'system', 'system');

-- ----------------------------
-- 字典数据
-- ----------------------------
INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, tone, is_default, create_by, update_by)
VALUES
    (1, 'sys_user_sex', '男', '0', 1, 1, 'info', 1, 'system', 'system'),
    (2, 'sys_user_sex', '女', '1', 2, 1, 'purple', 0, 'system', 'system'),
    (3, 'sys_user_sex', '未知', '2', 3, 1, 'neutral', 0, 'system', 'system'),
    (4, 'sys_normal_disable', '正常', '1', 1, 1, 'ok', 1, 'system', 'system'),
    (5, 'sys_normal_disable', '停用', '0', 2, 1, 'danger', 0, 'system', 'system'),
    (6, 'wf_approve_result', '通过', 'pass', 1, 1, 'ok', 1, 'system', 'system'),
    (7, 'wf_approve_result', '驳回', 'reject', 2, 1, 'danger', 0, 'system', 'system'),
    (8, 'wf_approve_result', '退回', 'return', 3, 1, 'warn', 0, 'system', 'system'),
    (9, 'wf_approve_result', '撤回', 'revoke', 4, 1, 'neutral', 0, 'system', 'system');

-- ----------------------------
-- 参数配置数据
-- ----------------------------
INSERT INTO sys_config (id, config_name, config_key, config_value, config_type, builtin, remark, create_by, update_by)
VALUES
    (1, '用户管理-账号初始密码', 'sys.user.initPassword', 'Yy@123456', 'Y', 1, '初始化密码', 'system', 'system'),
    (2, '账号自助-验证码开关', 'sys.account.captchaEnabled', 'true', 'Y', 1, '是否开启登录验证码', 'system', 'system'),
    (3, '工作流-审批超时提醒小时', 'wf.task.remindHours', '24', 'N', 0, '待办任务超时提醒阈值', 'system', 'system');


SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 初始化完成
-- 默认管理员账号: admin / admin123
-- ============================================================
