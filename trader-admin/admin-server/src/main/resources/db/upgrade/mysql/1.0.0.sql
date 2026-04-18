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
