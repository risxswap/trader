CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  username VARCHAR(64) NOT NULL COMMENT '用户名',
  password VARCHAR(255) NOT NULL COMMENT '密码',
  nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否锁定',
  last_login DATETIME(6) DEFAULT NULL COMMENT '最后登录时间',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY users_username_uidx (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS fund (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  code VARCHAR(32) NOT NULL COMMENT '基金代码',
  name VARCHAR(255) DEFAULT NULL COMMENT '基金名称',
  management VARCHAR(255) DEFAULT NULL COMMENT '基金管理人',
  custodian VARCHAR(255) DEFAULT NULL COMMENT '基金托管人',
  fund_type VARCHAR(64) DEFAULT NULL COMMENT '基金类型',
  found_date DATETIME(6) DEFAULT NULL COMMENT '成立日期',
  due_date DATETIME(6) DEFAULT NULL COMMENT '到期日期',
  list_date DATETIME(6) DEFAULT NULL COMMENT '上市日期',
  issue_date DATETIME(6) DEFAULT NULL COMMENT '发行日期',
  delist_date DATETIME(6) DEFAULT NULL COMMENT '退市日期',
  issue_amount DECIMAL(20,6) DEFAULT NULL COMMENT '发行份额',
  m_fee DECIMAL(10,6) DEFAULT NULL COMMENT '管理费率',
  c_fee DECIMAL(10,6) DEFAULT NULL COMMENT '托管费率',
  duration_year DECIMAL(10,4) DEFAULT NULL COMMENT '存续年限',
  p_value DECIMAL(20,6) DEFAULT NULL COMMENT '面值',
  min_amount DECIMAL(20,6) DEFAULT NULL COMMENT '起购金额',
  exp_return DECIMAL(20,6) DEFAULT NULL COMMENT '预期收益率',
  benchmark VARCHAR(1024) DEFAULT NULL COMMENT '业绩比较基准',
  status VARCHAR(32) DEFAULT NULL COMMENT '基金状态',
  invest_type VARCHAR(64) DEFAULT NULL COMMENT '投资类型',
  type VARCHAR(64) DEFAULT NULL COMMENT '产品分类',
  trustee VARCHAR(255) DEFAULT NULL COMMENT '受托人',
  purc_startdate DATETIME(6) DEFAULT NULL COMMENT '申购开始时间',
  redm_startdate DATETIME(6) DEFAULT NULL COMMENT '赎回开始时间',
  market VARCHAR(32) DEFAULT NULL COMMENT '市场',
  exchange VARCHAR(32) DEFAULT NULL COMMENT '交易所',
  correlation VARCHAR(64) DEFAULT NULL COMMENT '相关性标签',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY fund_code_uidx (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='基金基础信息表';

CREATE TABLE IF NOT EXISTS import_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  file VARCHAR(255) DEFAULT NULL COMMENT '导入文件名',
  status VARCHAR(32) DEFAULT NULL COMMENT '导入状态',
  type VARCHAR(32) DEFAULT NULL COMMENT '导入类型',
  remark TEXT DEFAULT NULL COMMENT '备注',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='导入日志表';

CREATE TABLE IF NOT EXISTS msg_push_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  type VARCHAR(32) DEFAULT NULL COMMENT '消息类型',
  content TEXT DEFAULT NULL COMMENT '消息内容',
  status VARCHAR(32) DEFAULT NULL COMMENT '发送状态',
  channel VARCHAR(32) DEFAULT NULL COMMENT '发送渠道',
  title VARCHAR(255) DEFAULT NULL COMMENT '消息标题',
  recipient VARCHAR(255) DEFAULT NULL COMMENT '接收人',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息推送日志表';

CREATE TABLE IF NOT EXISTS exchange (
  code VARCHAR(32) NOT NULL COMMENT '交易所代码',
  name VARCHAR(255) NOT NULL COMMENT '交易所名称',
  timezone VARCHAR(64) DEFAULT NULL COMMENT '时区',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='交易所表';

CREATE TABLE IF NOT EXISTS calendar (
  date DATE NOT NULL COMMENT '交易日期',
  exchange VARCHAR(32) NOT NULL COMMENT '交易所代码',
  open SMALLINT NOT NULL DEFAULT 0 COMMENT '是否开市',
  pre_date DATE DEFAULT NULL COMMENT '上一交易日',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (exchange, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='交易日历表';

CREATE TABLE IF NOT EXISTS investment (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(64) NOT NULL COMMENT '投资名称',
  group_name VARCHAR(64) DEFAULT NULL COMMENT '投资分组',
  target_type VARCHAR(32) DEFAULT NULL COMMENT '标的类型',
  invest_type VARCHAR(32) DEFAULT NULL COMMENT '投资方式',
  broker_id BIGINT DEFAULT NULL COMMENT '券商ID',
  targets JSON DEFAULT NULL COMMENT '投资标的配置',
  budget DECIMAL(20,6) DEFAULT NULL COMMENT '预算金额',
  strategy VARCHAR(64) NOT NULL COMMENT '策略类型',
  strategy_config TEXT DEFAULT NULL COMMENT '策略配置',
  cron VARCHAR(64) NOT NULL DEFAULT '' COMMENT '调度表达式',
  executor_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '执行器ID',
  status VARCHAR(32) DEFAULT 'STOPPED' COMMENT '状态',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY investment_broker_id_idx (broker_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='投资计划表';

CREATE TABLE IF NOT EXISTS investment_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  investment_id BIGINT NOT NULL COMMENT '投资ID',
  type VARCHAR(32) NOT NULL DEFAULT 'TRADE' COMMENT '日志类型',
  record_date DATETIME(6) NOT NULL COMMENT '记录时间',
  cash DECIMAL(20,6) DEFAULT NULL COMMENT '现金',
  asset DECIMAL(20,6) DEFAULT NULL COMMENT '总资产',
  profit DECIMAL(20,6) DEFAULT NULL COMMENT '收益',
  remark TEXT DEFAULT NULL COMMENT '备注',
  notified SMALLINT DEFAULT 0 COMMENT '是否已通知',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY investment_log_investment_id_idx (investment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='投资日志表';

CREATE TABLE IF NOT EXISTS investment_trading (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  investment_log_id BIGINT DEFAULT NULL COMMENT '投资日志ID',
  investment_id BIGINT NOT NULL COMMENT '投资ID',
  asset VARCHAR(32) DEFAULT NULL COMMENT '资产代码',
  asset_type VARCHAR(32) DEFAULT NULL COMMENT '资产类型',
  volume DECIMAL(20,6) DEFAULT NULL COMMENT '成交数量',
  price DECIMAL(20,6) DEFAULT NULL COMMENT '成交价格',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY investment_trading_investment_log_id_idx (investment_log_id),
  KEY investment_trading_investment_id_idx (investment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='投资成交记录表';

CREATE TABLE IF NOT EXISTS investment_position (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  investment_id BIGINT NOT NULL COMMENT '投资ID',
  investment_log_id BIGINT DEFAULT NULL COMMENT '投资日志ID',
  asset VARCHAR(32) DEFAULT NULL COMMENT '资产代码',
  asset_type VARCHAR(32) DEFAULT NULL COMMENT '资产类型',
  quantity DECIMAL(20,6) DEFAULT NULL COMMENT '持仓数量',
  buy_price DECIMAL(20,6) DEFAULT NULL COMMENT '买入价格',
  cost_price DECIMAL(20,6) DEFAULT NULL COMMENT '成本价格',
  side VARCHAR(32) DEFAULT NULL COMMENT '方向',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY investment_position_investment_id_idx (investment_id),
  KEY investment_position_investment_log_id_idx (investment_log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='投资持仓表';

CREATE TABLE IF NOT EXISTS broker (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(64) NOT NULL COMMENT '券商名称',
  code VARCHAR(32) NOT NULL COMMENT '券商编码',
  initial_capital DECIMAL(20,6) DEFAULT NULL COMMENT '初始资金',
  current_capital DECIMAL(20,6) DEFAULT NULL COMMENT '当前资金',
  intro TEXT DEFAULT NULL COMMENT '介绍',
  remark TEXT DEFAULT NULL COMMENT '备注',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY broker_code_uidx (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='券商表';

CREATE TABLE IF NOT EXISTS system_sql_script (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  db_type VARCHAR(32) NOT NULL DEFAULT 'MYSQL' COMMENT '数据库类型',
  version VARCHAR(64) NOT NULL COMMENT '版本号',
  sql_text LONGTEXT NOT NULL COMMENT 'SQL内容',
  checksum VARCHAR(64) NOT NULL COMMENT 'SQL指纹',
  status VARCHAR(16) NOT NULL COMMENT '执行状态',
  error_message LONGTEXT DEFAULT NULL COMMENT '错误信息',
  started_at DATETIME(6) DEFAULT NULL COMMENT '开始执行时间',
  finished_at DATETIME(6) DEFAULT NULL COMMENT '执行完成时间',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY system_sql_script_db_type_checksum_uidx (db_type, checksum),
  KEY system_sql_script_version_idx (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据库升级脚本执行记录表';

CREATE TABLE IF NOT EXISTS task_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    task_group VARCHAR(100) COMMENT '任务分组',
    start_time DATETIME(6) NOT NULL COMMENT '任务开始时间',
    end_time DATETIME(6) COMMENT '任务结束时间',
    status VARCHAR(20) NOT NULL COMMENT '任务状态',
    content TEXT COMMENT '执行内容(Markdown)',
    error_msg TEXT COMMENT '错误信息',
    execution_ms BIGINT COMMENT '执行耗时(毫秒)',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    INDEX idx_task_log_name (task_name),
    INDEX idx_task_log_status (status),
    INDEX idx_task_log_start (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行日志表';

CREATE TABLE IF NOT EXISTS node_group (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(64) NOT NULL COMMENT '分组名称',
  code VARCHAR(64) NOT NULL COMMENT '分组编码',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  is_default_pending TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认待审批分组',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY node_group_code_uidx (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='节点分组表';

CREATE TABLE IF NOT EXISTS node (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  node_id VARCHAR(128) NOT NULL COMMENT '节点标识',
  node_name VARCHAR(128) NOT NULL COMMENT '节点名称',
  node_type VARCHAR(32) NOT NULL COMMENT '节点类型',
  node_group_id BIGINT NOT NULL COMMENT '节点分组ID',
  approval_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '审批状态',
  hostname VARCHAR(255) DEFAULT NULL COMMENT '主机名',
  primary_ip VARCHAR(64) DEFAULT NULL COMMENT '主IP',
  remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
  approved_at DATETIME(6) DEFAULT NULL COMMENT '审批时间',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY node_node_id_uidx (node_id),
  KEY node_node_group_id_idx (node_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='节点表';

INSERT INTO node_group (name, code, sort, is_default_pending)
SELECT '待审批', 'pending', 0, 1
WHERE NOT EXISTS (
  SELECT 1 FROM node_group WHERE code = 'pending'
);


CREATE TABLE IF NOT EXISTS system_task (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_name VARCHAR(64),
    task_type VARCHAR(32) NOT NULL,
    task_code VARCHAR(128) NOT NULL,
    task_name VARCHAR(256) NOT NULL,
    cron VARCHAR(128) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    param_schema TEXT,
    params_json TEXT,
    default_params_json TEXT,
    version BIGINT NOT NULL DEFAULT 1,
    remark VARCHAR(512),
    updated_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY system_task_task_type_task_code_uidx (task_type, task_code),
    KEY system_task_task_type_idx (task_type),
    KEY system_task_status_idx (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS system_task_run_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_name VARCHAR(64) NOT NULL,
    task_code VARCHAR(128) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    params_json TEXT,
    status VARCHAR(32) NOT NULL,
    started_at DATETIME(6),
    finished_at DATETIME(6),
    duration_ms BIGINT,
    error_msg TEXT,
    trace_id VARCHAR(128),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY system_task_run_log_app_name_task_code_idx (app_name, task_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
