CREATE TABLE IF NOT EXISTS fund_adj (
  code String COMMENT '基金代码',
  time DateTime64(3, 'Asia/Shanghai') COMMENT '时间',
  adj_factor Nullable(Float64) COMMENT '复权因子',
  created_at DateTime64(3, 'Asia/Shanghai') DEFAULT now64(3) COMMENT '创建时间',
  updated_at DateTime64(3, 'Asia/Shanghai') DEFAULT now64(3) COMMENT '更新时间'
) ENGINE = MergeTree
ORDER BY (code, time);

CREATE TABLE IF NOT EXISTS fund_market (
  code String COMMENT '基金代码',
  time DateTime64(3, 'Asia/Shanghai') COMMENT '时间',
  open Nullable(Decimal(20, 6)) COMMENT '开盘价',
  high Nullable(Decimal(20, 6)) COMMENT '最高价',
  low Nullable(Decimal(20, 6)) COMMENT '最低价',
  close Nullable(Decimal(20, 6)) COMMENT '收盘价',
  pre_close Nullable(Decimal(20, 6)) COMMENT '前收盘价',
  change Nullable(Decimal(20, 6)) COMMENT '涨跌额',
  pct_chg Nullable(Decimal(20, 6)) COMMENT '涨跌幅',
  vol Nullable(Decimal(20, 6)) COMMENT '成交量',
  amount Nullable(Decimal(20, 6)) COMMENT '成交额',
  created_at DateTime64(3, 'Asia/Shanghai') DEFAULT now64(3) COMMENT '创建时间',
  updated_at DateTime64(3, 'Asia/Shanghai') DEFAULT now64(3) COMMENT '更新时间',
  time_frame Nullable(String) COMMENT '时间周期'
) ENGINE = MergeTree
ORDER BY (code, time);

CREATE TABLE IF NOT EXISTS fund_nav (
  code String COMMENT '基金代码',
  time DateTime64(3, 'Asia/Shanghai') COMMENT '净值日期',
  unit_nav Nullable(Decimal(20, 6)) COMMENT '单位净值',
  accum_nav Nullable(Decimal(20, 6)) COMMENT '累计净值',
  accum_div Nullable(Decimal(20, 6)) COMMENT '累计分红',
  net_asset Nullable(Decimal(20, 6)) COMMENT '净资产',
  total_net_asset Nullable(Decimal(20, 6)) COMMENT '总净资产',
  adj_nav Nullable(Decimal(20, 6)) COMMENT '复权净值',
  created_at DateTime64(3, 'Asia/Shanghai') DEFAULT now64(3) COMMENT '创建时间',
  updated_at DateTime64(3, 'Asia/Shanghai') DEFAULT now64(3) COMMENT '更新时间'
) ENGINE = MergeTree
ORDER BY (code, time);
