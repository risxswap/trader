# 数据库升级区分 MySQL 与 ClickHouse Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让数据库升级脚本按 MySQL 和 ClickHouse 分目录执行，同时继续共用 MySQL 中的升级记录表，并在记录中区分数据源。

**Architecture:** 保留 `UpgradeService` 作为统一启动入口，但把升级执行拆成两条流水线：MySQL 升级脚本走 MySQL 执行器，ClickHouse 升级脚本走 ClickHouse 执行器。升级记录仍写入 `system_sql_script`，新增 `db_type` 字段并用 `db_type + checksum` 做幂等判定。

**Tech Stack:** Spring Boot 3、MyBatis-Plus、MySQL、ClickHouse、JUnit 5、Mockito

---

## File Structure

**Modify**
- `admin-server/src/main/java/cc/riskswap/trader/admin/service/UpgradeService.java`
- `admin-server/src/main/java/cc/riskswap/trader/admin/dao/entity/SystemUpgradeStep.java`
- `admin-server/src/main/java/cc/riskswap/trader/admin/dao/SqlExecDao.java`
- `admin-server/src/main/java/cc/riskswap/trader/admin/dao/mapper/SqlExecMapper.java`
- `admin-server/src/main/resources/db/mysql.sql`
- `admin-server/src/main/resources/application.yml`

**Create**
- `admin-server/src/main/java/cc/riskswap/trader/admin/dao/ClickHouseSqlExecDao.java`
- `admin-server/src/main/java/cc/riskswap/trader/admin/dao/mapper/ClickHouseSqlExecMapper.java`
- `admin-server/src/main/resources/db/upgrade/mysql/1.0.0.sql`
- `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/UpgradeServiceTest.java`

**Delete**
- `admin-server/src/main/resources/db/upgrade/1.0.0.sql`

---

### Task 1: 调整升级记录模型，支持按数据源区分

**Files:**
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/entity/SystemUpgradeStep.java`
- Modify: `admin-server/src/main/resources/db/mysql.sql`
- Create: `admin-server/src/main/resources/db/upgrade/mysql/1.0.0.sql`
- Delete: `admin-server/src/main/resources/db/upgrade/1.0.0.sql`
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/UpgradeServiceTest.java`

- [ ] **Step 1: 写失败测试，表达升级记录必须带 `dbType`**

```java
Assertions.assertEquals("MYSQL", step.getDbType());
Assertions.assertEquals("CLICKHOUSE", step.getDbType());
```

- [ ] **Step 2: 运行单测确认失败**

Run: `./mvnw -pl admin-server -Dtest=UpgradeServiceTest test`
Expected: FAIL，提示 `SystemUpgradeStep` 缺少 `dbType` 或断言失败。

- [ ] **Step 3: 给实体增加 `dbType` 字段**

```java
private String dbType;
```

- [ ] **Step 4: 更新 MySQL 初始化脚本和升级脚本**

```sql
db_type VARCHAR(32) NOT NULL,
UNIQUE KEY system_sql_script_db_type_checksum_uidx (db_type, checksum)
```

- [ ] **Step 5: 把旧的 `db/upgrade/1.0.0.sql` 移到 `db/upgrade/mysql/1.0.0.sql`**

```text
旧路径: admin-server/src/main/resources/db/upgrade/1.0.0.sql
新路径: admin-server/src/main/resources/db/upgrade/mysql/1.0.0.sql
```

- [ ] **Step 6: 运行单测确认字段和脚本结构已对齐**

Run: `./mvnw -pl admin-server -Dtest=UpgradeServiceTest test`
Expected: PASS

- [ ] **Step 7: 提交本任务**

```bash
git add admin-server/src/main/java/cc/riskswap/trader/admin/dao/entity/SystemUpgradeStep.java admin-server/src/main/resources/db/mysql.sql admin-server/src/main/resources/db/upgrade/mysql/1.0.0.sql
git rm admin-server/src/main/resources/db/upgrade/1.0.0.sql
git commit -m "feat: add db type to upgrade records"
```

### Task 2: 拆分 SQL 执行器，分别连接 MySQL 与 ClickHouse

**Files:**
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/SqlExecDao.java`
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/mapper/SqlExecMapper.java`
- Create: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/ClickHouseSqlExecDao.java`
- Create: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/mapper/ClickHouseSqlExecMapper.java`
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/UpgradeServiceTest.java`

- [ ] **Step 1: 写失败测试，表达升级服务会根据库类型选择不同执行器**

```java
Mockito.verify(mysqlSqlExecDao).exec("CREATE TABLE demo_mysql (...)");
Mockito.verify(clickHouseSqlExecDao).exec("CREATE TABLE demo_ck (...) ENGINE = MergeTree ORDER BY id");
```

- [ ] **Step 2: 运行单测确认失败**

Run: `./mvnw -pl admin-server -Dtest=UpgradeServiceTest test`
Expected: FAIL，提示缺少 ClickHouse 执行器或未调用对应 mock。

- [ ] **Step 3: 保留现有 MySQL 执行 DAO，并新增 ClickHouse 执行 DAO/Mapper**

```java
@ClickHouseMapper
public interface ClickHouseSqlExecMapper {
    @Update("${sql}")
    int exec(@Param("sql") String sql);
}
```

- [ ] **Step 4: 保持 DAO 边界清晰**

```java
public class ClickHouseSqlExecDao {
    public int exec(String sql) {
        return mapper.exec(sql);
    }
}
```

- [ ] **Step 5: 运行单测确认不同执行器可被区分调用**

Run: `./mvnw -pl admin-server -Dtest=UpgradeServiceTest test`
Expected: PASS

- [ ] **Step 6: 提交本任务**

```bash
git add admin-server/src/main/java/cc/riskswap/trader/admin/dao/SqlExecDao.java admin-server/src/main/java/cc/riskswap/trader/admin/dao/mapper/SqlExecMapper.java admin-server/src/main/java/cc/riskswap/trader/admin/dao/ClickHouseSqlExecDao.java admin-server/src/main/java/cc/riskswap/trader/admin/dao/mapper/ClickHouseSqlExecMapper.java
git commit -m "feat: add clickhouse upgrade executor"
```

### Task 3: 重构升级服务，按目录和数据源执行

**Files:**
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/service/UpgradeService.java`
- Modify: `admin-server/src/main/resources/application.yml`
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/UpgradeServiceTest.java`

- [ ] **Step 1: 写失败测试，表达升级服务会分别扫描两个目录**

```java
Assertions.assertEquals(
        List.of("classpath*:db/upgrade/mysql/*.sql", "classpath*:db/upgrade/clickhouse/*.sql"),
        scannedLocations
);
```

- [ ] **Step 2: 运行单测确认失败**

Run: `./mvnw -pl admin-server -Dtest=UpgradeServiceTest test`
Expected: FAIL，当前只扫描 `classpath*:db/upgrade/*.sql`

- [ ] **Step 3: 为升级配置增加独立路径**

```yaml
trader:
  db:
    upgrade:
      mysql-location: classpath*:db/upgrade/mysql/*.sql
      clickhouse-location: classpath*:db/upgrade/clickhouse/*.sql
```

- [ ] **Step 4: 在 `UpgradeService` 中拆成两条执行流水线**

```java
runUpgrade("MYSQL", mysqlLocation, mysqlSqlExecDao::exec);
runUpgrade("CLICKHOUSE", clickhouseLocation, clickHouseSqlExecDao::exec);
```

- [ ] **Step 5: 以 `dbType + checksum` 查询幂等记录**

```java
sw.eq(SystemUpgradeStep::getDbType, dbType);
sw.eq(SystemUpgradeStep::getChecksum, fp);
```

- [ ] **Step 6: 记录成功/失败时一并落 `dbType`**

```java
step.setDbType(dbType);
```

- [ ] **Step 7: 运行单测确认目录拆分和幂等逻辑生效**

Run: `./mvnw -pl admin-server -Dtest=UpgradeServiceTest test`
Expected: PASS

- [ ] **Step 8: 提交本任务**

```bash
git add admin-server/src/main/java/cc/riskswap/trader/admin/service/UpgradeService.java admin-server/src/main/resources/application.yml admin-server/src/test/java/cc/riskswap/trader/admin/test/service/UpgradeServiceTest.java
git commit -m "feat: separate mysql and clickhouse upgrade flow"
```

### Task 4: 完成回归测试与构建验证

**Files:**
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/UpgradeServiceTest.java`
- Verify: `admin-server/src/test/java/cc/riskswap/trader/admin/test/system/SystemInitializerTest.java`

- [ ] **Step 1: 补齐回归测试覆盖点**

```java
Assertions.assertAll(
        () -> Assertions.assertEquals("MYSQL", mysqlStep.getDbType()),
        () -> Assertions.assertEquals("CLICKHOUSE", clickhouseStep.getDbType())
);
```

- [ ] **Step 2: 运行升级相关测试**

Run: `./mvnw -pl admin-server -Dtest=UpgradeServiceTest,SystemInitializerTest test`
Expected: PASS

- [ ] **Step 3: 运行完整测试集**

Run: `./mvnw -pl admin-server test`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: 运行编译验证**

Run: `./mvnw -pl admin-server -DskipTests compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: 提交本任务**

```bash
git add admin-server/src/test/java/cc/riskswap/trader/admin/test/service/UpgradeServiceTest.java admin-server/src/test/java/cc/riskswap/trader/admin/test/system/SystemInitializerTest.java
git commit -m "test: cover separated database upgrade flow"
```

