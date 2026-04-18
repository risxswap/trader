# trader-base

`trader-base` 是一个基于 Spring Boot 3.5.x 的自动装配基础库，统一提供以下能力：

- 默认加载 `/opt/trader/config.properties` 外部配置，支持可选节点配置覆盖
- Redis 自动装配与模板注入
- MySQL / ClickHouse 双数据源与 MyBatis-Plus 自动装配
- 基于 AOP 的任务日志落库与线程上下文透传
- 硬件信息采集、事件发布、ClickHouse 持久化与 Redis 最新快照缓存

如果你希望在业务项目里直接复用一套统一的基础设施能力，可以把它理解成三件事：

- 用统一配置接入 MySQL / ClickHouse / Redis
- 用统一注解和自动装配完成任务信息上报
- 用统一监控组件自动采集并上报机器监控信息

## 环境要求

- JDK 21
- Maven 3.9+
- Spring Boot 3.5.x
- 可选基础设施：MySQL、ClickHouse、Redis

## 快速开始

### 1. 以 Spring Boot Starter 方式引入

先将本项目打包并发布到你的 Maven 仓库，然后在业务项目中引入：

```xml
<dependency>
    <groupId>cc.riskswap.trader</groupId>
    <artifactId>base</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

业务项目只要是 Spring Boot 应用，正常引入依赖后会自动装配，不需要额外 `@Import` 自动配置类。

自动装配入口由 [org.springframework.boot.autoconfigure.AutoConfiguration.imports](../../trader-base/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports) 声明，指向 [TraderBaseAutoConfiguration](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderBaseAutoConfiguration.java)。

如需关闭某一块能力，可以直接用配置开关：

```properties
trader.mysql.enabled=false
trader.clickhouse.enabled=false
trader.redis.enabled=false
trader.task-log.enabled=false
trader.monitor.enabled=false
```

### 2. 构建项目

```bash
"$PWD/.tools/apache-maven-3.9.9/bin/mvn" clean test
```

### 3. 准备外部配置

框架启动时默认读取：

```properties
/opt/trader/config.properties
```

也可以通过启动参数覆盖默认路径：

```bash
--config.path=/data/trader/custom.properties
```

节点配置是可选项，默认不会读取任何节点配置文件。只有在显式指定 `node.config.path` 时才会加载，并且优先级高于 `config.path`：

```bash
--config.path=/data/trader/common.properties
--node.config.path=/data/trader/node-a.properties
```

当 `node.config.path` 未配置或文件不存在时，会直接跳过节点配置加载。

对应实现见 [ConfigLoader](../../trader-base/src/main/java/cc/riskswap/trader/base/config/ConfigLoader.java#L17-L86)。

### 4. 按业务项目接入的推荐顺序使用

对大多数业务项目，推荐按下面顺序接入：

1. 先配置 MySQL / ClickHouse / Redis，让数据库与缓存能力可用
2. 再给定时任务或批处理方法加上 `@TraderTaskLog`，完成任务信息上报
3. 最后开启 `trader.monitor.enabled=true` 并配置 `trader.node.id`，自动上报监控信息

这样做的好处是：

- 数据访问先稳定下来，业务代码可以直接开始写 Mapper 和 Service
- 任务执行记录会自动沉淀到 MySQL，方便排查失败任务和执行耗时
- 节点监控会自动写入 ClickHouse 和 Redis，方便做监控大盘、巡检和告警

## 推荐配置示例

下面是一份常见的外部配置样例：

```properties
trader.redis.enabled=true
trader.redis.host=127.0.0.1
trader.redis.port=6379
trader.redis.password=your-redis-password
trader.redis.database=0

trader.mysql.enabled=true
trader.mysql.url=jdbc:mysql://127.0.0.1:3306/trader?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
trader.mysql.username=root
trader.mysql.password=your-mysql-password
trader.mysql.driver-class-name=com.mysql.cj.jdbc.Driver

trader.clickhouse.enabled=true
trader.clickhouse.url=jdbc:clickhouse://127.0.0.1:8123/trader
trader.clickhouse.username=default
trader.clickhouse.password=your-clickhouse-password
trader.clickhouse.driver-class-name=com.clickhouse.jdbc.ClickHouseDriver

trader.task-log.enabled=true

trader.monitor.enabled=true
trader.monitor.interval=PT30S
trader.monitor.log-on-publish=true
trader.node.id=node-a
trader.node.type=maker
trader.node.name=node-a

trader.tushare.url=https://api.tushare.pro
trader.tushare.token=your-tushare-token

trader.matrix.homeserver=https://matrix.example.com
trader.matrix.room-id=!your-room-id:matrix.example.com
trader.matrix.access-token=your-matrix-access-token
```

其中 MySQL / ClickHouse 使用扁平写法：

- `trader.mysql.url`、`trader.clickhouse.url`

`ConfigLoader` 会原样加载 `config.properties` 中的其他业务配置，所以 `trader.tushare.*`、`trader.matrix.*` 这类键也可以和基础库配置放在同一个文件中。

如果你的业务项目和 `trader-collector` 一样，使用 Spring Boot 配置文件继续承接这些外部配置，也可以直接这样写：

```yaml
spring:
  datasource:
    mysql:
      url: ${trader.mysql.datasource.url:jdbc:mysql://localhost:3306/trader?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}
      username: ${trader.mysql.datasource.username:root}
      password: ${trader.mysql.datasource.password:root}
    clickhouse:
      url: ${trader.clickhouse.datasource.url:jdbc:clickhouse://localhost:8123/trader}
      username: ${trader.clickhouse.datasource.username:default}
      password: ${trader.clickhouse.datasource.password:}
```

这种写法的重点不是把配置写死在 `application.yml`，而是让业务项目继续通过 `trader.*` 这套统一键名读取数据库配置。

## 最常见的三种用法

### 1. 用它操作数据库

`trader-base` 会自动装配 MySQL / ClickHouse 对应的数据源和 MyBatis 组件，业务项目只需要：

- 在 Mapper 上标记 `@MysqlMapper` 或 `@ClickHouseMapper`
- 把启动类放到能够覆盖业务包路径的位置，或者显式配置扫描包
- 正常在 Service 中注入 Mapper 使用

最常见的职责划分建议是：

- MySQL：放事务型、当前态、配置类数据
- ClickHouse：放监控、统计、快照、历史流水类数据

例如：

```java
package cc.riskswap.trader.demo.service;

import cc.riskswap.trader.demo.mapper.OrderMapper;
import cc.riskswap.trader.demo.mapper.OrderSnapshotMapper;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderSnapshotMapper orderSnapshotMapper;

    public OrderService(OrderMapper orderMapper, OrderSnapshotMapper orderSnapshotMapper) {
        this.orderMapper = orderMapper;
        this.orderSnapshotMapper = orderSnapshotMapper;
    }

    public void saveOrder(OrderEntity order, OrderSnapshotEntity snapshot) {
        orderMapper.insert(order);
        orderSnapshotMapper.insert(snapshot);
    }
}
```

如果你不想直接依赖 Mapper，也可以注入：

- `mysqlSqlSessionTemplate`
- `clickHouseSqlSessionTemplate`

用于更底层的数据库操作。

## 自动装配能力

### 数据源与 Redis

启用后，框架会自动提供这些 Bean：

- MySQL：`mysqlDataSource`、`mysqlSqlSessionFactory`、`mysqlSqlSessionTemplate`
- ClickHouse：`clickHouseDataSource`、`clickHouseSqlSessionFactory`、`clickHouseSqlSessionTemplate`
- Redis：`stringRedisTemplate`、`redisTemplate`

核心自动配置见 [TraderDataSourceAutoConfiguration](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderDataSourceAutoConfiguration.java) 与 [TraderRedisAutoConfiguration](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderRedisAutoConfiguration.java)。

### Mapper 区分规则

MySQL 和 ClickHouse 是对等关系，通过注解区分 Mapper：

- `@MysqlMapper` 绑定到 MySQL
- `@ClickHouseMapper` 绑定到 ClickHouse

对应注解：

- [MysqlMapper](../../trader-base/src/main/java/cc/riskswap/trader/base/datasource/annotation/MysqlMapper.java)
- [ClickHouseMapper](../../trader-base/src/main/java/cc/riskswap/trader/base/datasource/annotation/ClickHouseMapper.java)

### MySQL Mapper 示例

```java
package cc.riskswap.trader.demo.mapper;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@MysqlMapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
}
```

### ClickHouse Mapper 示例

```java
package cc.riskswap.trader.demo.mapper;

import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@ClickHouseMapper
public interface OrderSnapshotMapper extends BaseMapper<OrderSnapshotEntity> {
}
```

### 业务项目包路径示例（`cc.riskswap.trader.base.dao.mapper`）

如果业务项目 Mapper 包是 `cc.riskswap.trader.base.dao.mapper`，推荐把启动类放在 `cc.riskswap.trader.admin`，这样默认扫描即可覆盖该包。

启动类示例：

```java
package cc.riskswap.trader.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
```

Mapper 示例：

```java
package cc.riskswap.trader.base.dao.mapper;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@MysqlMapper
public interface AdminUserMapper extends BaseMapper<AdminUserEntity> {
}
```

```java
package cc.riskswap.trader.base.dao.mapper;

import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@ClickHouseMapper
public interface AdminUserSnapshotMapper extends BaseMapper<AdminUserSnapshotEntity> {
}
```

如果启动类不在 `cc.riskswap.trader.admin`（或其上级包），可以在业务项目显式指定扫描包：

```java
package cc.riskswap.trader.admin.config;

import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.base.datasource.support.AutoConfiguredMapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TraderMapperScanConfig {

    @Bean(name = "mysqlMapperScannerConfigurer")
    public AutoConfiguredMapperScannerConfigurer mysqlMapperScannerConfigurer() {
        AutoConfiguredMapperScannerConfigurer configurer = new AutoConfiguredMapperScannerConfigurer();
        configurer.setBasePackage("cc.riskswap.trader.base.dao.mapper");
        configurer.setAnnotationClass(MysqlMapper.class);
        configurer.setSqlSessionTemplateBeanName("mysqlSqlSessionTemplate");
        configurer.setProcessPropertyPlaceHolders(true);
        return configurer;
    }

    @Bean(name = "clickHouseMapperScannerConfigurer")
    public AutoConfiguredMapperScannerConfigurer clickHouseMapperScannerConfigurer() {
        AutoConfiguredMapperScannerConfigurer configurer = new AutoConfiguredMapperScannerConfigurer();
        configurer.setBasePackage("cc.riskswap.trader.base.dao.mapper");
        configurer.setAnnotationClass(ClickHouseMapper.class);
        configurer.setSqlSessionTemplateBeanName("clickHouseSqlSessionTemplate");
        configurer.setProcessPropertyPlaceHolders(true);
        return configurer;
    }
}
```

### 手动注入示例

```java
package cc.riskswap.trader.demo.service;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class InfrastructureService {

    private final SqlSessionTemplate mysqlSqlSessionTemplate;
    private final SqlSessionTemplate clickHouseSqlSessionTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public InfrastructureService(
            @Qualifier("mysqlSqlSessionTemplate") SqlSessionTemplate mysqlSqlSessionTemplate,
            @Qualifier("clickHouseSqlSessionTemplate") SqlSessionTemplate clickHouseSqlSessionTemplate,
            RedisTemplate<String, Object> redisTemplate) {
        this.mysqlSqlSessionTemplate = mysqlSqlSessionTemplate;
        this.clickHouseSqlSessionTemplate = clickHouseSqlSessionTemplate;
        this.redisTemplate = redisTemplate;
    }
}
```

## 任务日志

在方法上增加 `@TraderTaskLog` 即可开启任务日志：

```java
package cc.riskswap.trader.demo.job;

import cc.riskswap.trader.base.logging.TraderTaskLog;
import org.springframework.stereotype.Service;

@Service
public class SyncJobService {

    @TraderTaskLog("sync-order-task")
    public void syncOrders() {
    }
}
```

该能力会自动：

- 生成 `traceId`
- 记录任务名、执行结果、异常信息和耗时
- 输出开始 / 结束 / 失败日志
- 将日志写入 `task_log` 表
- 清理线程上下文，避免脏数据残留

`task_log` 表由框架在启动时自动初始化，建表 SQL 位于：

- [task-log-mysql.sql](../../trader-base/src/main/resources/sql/task-log-mysql.sql)
- [task-log-h2.sql](../../trader-base/src/main/resources/sql/task-log-h2.sql)

如果任务日志落库失败，不会中断主任务执行。

这部分能力适合用来上报：

- 定时任务执行开始 / 结束 / 失败信息
- 批处理任务执行耗时
- 异常堆栈与业务上下文
- 当前任务处理进度描述

也就是说，业务项目通常不需要自己再写一套“任务执行记录表”和“任务日志切面”。

相关实现：

- [TraderTaskLog](../../trader-base/src/main/java/cc/riskswap/trader/base/logging/TraderTaskLog.java#L1-L15)
- [TraderTaskLogAspect](../../trader-base/src/main/java/cc/riskswap/trader/base/logging/TraderTaskLogAspect.java)
- [TraderTaskLogStore](../../trader-base/src/main/java/cc/riskswap/trader/base/logging/TraderTaskLogStore.java)
- [TraderThreadContext](../../trader-base/src/main/java/cc/riskswap/trader/base/logging/TraderThreadContext.java#L1-L54)

如果你的项目存在异步线程池，可以结合自动装配出的 `TaskDecorator` 透传线程上下文。

## 硬件监控

启用后，框架会自动注册 `HardwareMonitorService`、定时发布器和硬件快照存储器。

这部分能力适合用来自动上报：

- CPU 负载
- 内存剩余
- 磁盘剩余
- JVM 运行时长
- 进程数 / 线程数
- 节点 hostname / IP / nodeId

### 主动拉取快照

```java
package cc.riskswap.trader.demo.service;

import cc.riskswap.trader.base.monitor.HardwareMonitorService;
import cc.riskswap.trader.base.monitor.HardwareSnapshot;
import org.springframework.stereotype.Service;

@Service
public class MonitorQueryService {

    private final HardwareMonitorService hardwareMonitorService;

    public MonitorQueryService(HardwareMonitorService hardwareMonitorService) {
        this.hardwareMonitorService = hardwareMonitorService;
    }

    public HardwareSnapshot current() {
        return hardwareMonitorService.currentSnapshot();
    }
}
```

### 监听硬件事件

```java
package cc.riskswap.trader.demo.listener;

import cc.riskswap.trader.base.monitor.HardwareSnapshotEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class HardwareSnapshotListener {

    @EventListener
    public void onSnapshot(HardwareSnapshotEvent event) {
        System.out.println(event.getSnapshot().getCpuLoad());
    }
}
```

### 存储行为

- 每次发布快照后，框架会将快照写入 ClickHouse 表 `node_monitor`
- 同时会将当前节点最新快照写入 Redis Hash `node:monitor`
- Redis 结构为：`key=node:monitor`、`hashKey=trader.node.id`、`value=JSON`
- `trader.node.id` 为空时，会跳过硬件快照存储

如果你的目标是“把监控信息上报出去”，那这里可以直接理解为：

- ClickHouse 负责存放监控历史，适合查询趋势和做报表
- Redis 负责存放节点最新状态，适合快速展示实时看板

业务项目只需要保证：

- `trader.monitor.enabled=true`
- `trader.node.id` 已配置
- ClickHouse 和 Redis 连接可用

满足这三个条件后，监控信息会自动定时采集并上报，不需要你在业务代码里手动写存储逻辑。

`node_monitor` 表由框架在启动时自动初始化，建表 SQL 位于：

- [hardware-monitor-clickhouse.sql](../../trader-base/src/main/resources/sql/hardware-monitor-clickhouse.sql)
- [hardware-monitor-h2.sql](../../trader-base/src/main/resources/sql/hardware-monitor-h2.sql)

核心实现见：

- [TraderMonitorAutoConfiguration](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderMonitorAutoConfiguration.java#L22-L51)
- [HardwareMonitorService](../../trader-base/src/main/java/cc/riskswap/trader/base/monitor/HardwareMonitorService.java#L1-L6)
- [HardwareMonitorPublisher](../../trader-base/src/main/java/cc/riskswap/trader/base/monitor/HardwareMonitorPublisher.java#L1-L37)
- [HardwareSnapshotStorage](../../trader-base/src/main/java/cc/riskswap/trader/base/monitor/HardwareSnapshotStorage.java#L23-L150)
- [TraderNodeProperties](../../trader-base/src/main/java/cc/riskswap/trader/base/config/TraderNodeProperties.java#L5-L16)

## 可用配置项

### MySQL

见 [TraderMysqlProperties](../../trader-base/src/main/java/cc/riskswap/trader/base/datasource/mysql/TraderMysqlProperties.java)：

- `trader.mysql.enabled`
- `trader.mysql.url`
- `trader.mysql.username`
- `trader.mysql.password`
- `trader.mysql.driver-class-name`
- `trader.mysql.datasource.url`
- `trader.mysql.datasource.username`
- `trader.mysql.datasource.password`
- `trader.mysql.datasource.driver-class-name`

### ClickHouse

见 [TraderClickHouseProperties](../../trader-base/src/main/java/cc/riskswap/trader/base/datasource/clickhouse/TraderClickHouseProperties.java)：

- `trader.clickhouse.enabled`
- `trader.clickhouse.url`
- `trader.clickhouse.username`
- `trader.clickhouse.password`
- `trader.clickhouse.driver-class-name`
- `trader.clickhouse.datasource.url`
- `trader.clickhouse.datasource.username`
- `trader.clickhouse.datasource.password`
- `trader.clickhouse.datasource.driver-class-name`

### Redis

- `trader.redis.enabled`
- `trader.redis.host`
- `trader.redis.port`
- `trader.redis.password`
- `trader.redis.database`

### 任务日志

- `trader.task-log.enabled`

### 硬件监控

见 [TraderMonitorProperties](../../trader-base/src/main/java/cc/riskswap/trader/base/monitor/TraderMonitorProperties.java#L1-L37)：

- `trader.monitor.enabled`
- `trader.monitor.interval`
- `trader.monitor.log-on-publish`
- `trader.node.id`
- `trader.node.type`
- `trader.node.name`

## 接入建议

- 业务项目主包路径建议覆盖需要扫描的 Mapper 所在包
- MySQL 与 ClickHouse 实体建议分别维护，避免语义混用
- 外部配置文件适合存放数据库、Redis、监控等环境差异项
- 需要关闭某个能力时，直接将对应 `enabled=false`
- 优先把“数据库访问、任务信息上报、监控信息上报”统一交给 `trader-base`，业务项目只保留领域逻辑

## 常见排查

- 启动后没有加载外部配置：确认 `config.path` 是否正确，或确认默认文件 `/opt/trader/config.properties` 是否存在；如配置了 `node.config.path`，再确认该文件路径存在且可读
- 任务日志未落库：确认 `trader.task-log.enabled=true` 且 MySQL 已启用，同时检查 `trader.mysql.url` 或 `trader.mysql.datasource.url` 是否已配置
- 硬件信息未写入 Redis / ClickHouse：确认 `trader.monitor.enabled=true`、`trader.node.id` 已配置，并检查 `trader.clickhouse.url` / `trader.redis.host` 是否已配置
- H2 测试环境建表失败：优先使用仓库内置的 H2 SQL 脚本，不要直接复用 ClickHouse 原生建表语法
