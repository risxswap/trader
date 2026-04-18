package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.admin.config.DatabaseScriptSupport;
import cc.riskswap.trader.base.dao.ClickHouseSqlExecDao;
import cc.riskswap.trader.base.dao.SqlExecDao;
import cc.riskswap.trader.base.dao.SystemUpgradeStepDao;
import cc.riskswap.trader.base.dao.entity.SystemUpgradeStep;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

@Service
public class UpgradeService {

    private final SystemUpgradeStepDao systemUpgradeStepDao;
    private final SqlExecDao sqlExecDao;
    private final ClickHouseSqlExecDao clickHouseSqlExecDao;
    private final UpgradeSqlErrorDecider upgradeSqlErrorDecider;
    private final Environment environment;
    private final ResourcePatternResolver resolver;
    private final String mysqlUpgradeLocation;
    private final String clickHouseUpgradeLocation;

    public UpgradeService(
            SystemUpgradeStepDao systemUpgradeStepDao,
            SqlExecDao sqlExecDao,
            ClickHouseSqlExecDao clickHouseSqlExecDao,
            UpgradeSqlErrorDecider upgradeSqlErrorDecider,
            Environment environment,
            ResourcePatternResolver resolver,
            @Value("${trader.db.upgrade.mysql-location:classpath*:db/upgrade/mysql/*.sql}") String mysqlUpgradeLocation,
            @Value("${trader.db.upgrade.clickhouse-location:classpath*:db/upgrade/clickhouse/*.sql}") String clickHouseUpgradeLocation
    ) {
        this.systemUpgradeStepDao = systemUpgradeStepDao;
        this.sqlExecDao = sqlExecDao;
        this.clickHouseSqlExecDao = clickHouseSqlExecDao;
        this.upgradeSqlErrorDecider = upgradeSqlErrorDecider;
        this.environment = environment;
        this.resolver = resolver;
        this.mysqlUpgradeLocation = mysqlUpgradeLocation;
        this.clickHouseUpgradeLocation = clickHouseUpgradeLocation;
    }

    public void runOnStartup() {
        String testFlag = environment.getProperty("org.springframework.boot.test.context.SpringBootTestContextBootstrapper");
        if (DatabaseScriptSupport.isSpringBootTestEnvironment(testFlag)) {
            return;
        }
        try {
            systemUpgradeStepDao.countAll();
        } catch (Exception e) {
            return;
        }
        ensureUpgradeStepSchema();
        runUpgrade("MYSQL", mysqlUpgradeLocation, sqlExecDao::exec);
        runUpgrade("CLICKHOUSE", clickHouseUpgradeLocation, clickHouseSqlExecDao::exec);
    }

    private void ensureUpgradeStepSchema() {
        try {
            sqlExecDao.exec("ALTER TABLE system_sql_script ADD COLUMN IF NOT EXISTS db_type VARCHAR(32) NOT NULL DEFAULT 'MYSQL'");
        } catch (Exception ignored) {
        }
        try {
            sqlExecDao.exec("DROP INDEX system_sql_script_checksum_uidx ON system_sql_script");
        } catch (Exception ignored) {
        }
        try {
            sqlExecDao.exec("CREATE UNIQUE INDEX system_sql_script_db_type_checksum_uidx ON system_sql_script (db_type, checksum)");
        } catch (Exception ignored) {
        }
    }

    private void runUpgrade(String dbType, String location, Consumer<String> executor) {
        Resource[] resources;
        String resourceLocation = location == null ? "" : location;
        try {
            resources = resolver.getResources(resourceLocation);
        } catch (Exception e) {
            return;
        }
        if (resources == null || resources.length == 0) {
            return;
        }
        Arrays.sort(resources, (a, b) -> {
            String av = filenameWithoutSuffix(a);
            String bv = filenameWithoutSuffix(b);
            return compareVersion(av, bv);
        });
        for (Resource r : resources) {
            String filename = Objects.requireNonNullElse(r.getFilename(), "");
            if (StrUtil.isBlank(filename)) {
                continue;
            }
            String version = filename.replaceAll("\\.sql$", "");
            if (!version.matches("\\d+\\.\\d+\\.\\d+")) {
                continue;
            }
            try {
                String content = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                List<String> stmts = DatabaseScriptSupport.splitStatements(content);
                for (String sql : stmts) {
                    String fp = fingerprint(sql);
                    SystemUpgradeStep existed = systemUpgradeStepDao.getByDbTypeAndChecksum(dbType, fp);
                    if (existed != null) {
                        continue;
                    }
                    SystemUpgradeStep step = new SystemUpgradeStep();
                    step.setDbType(dbType);
                    step.setVersion(version);
                    step.setSqlText(sql);
                    step.setChecksum(fp);
                    step.setStatus("RUNNING");
                    step.setStartedAt(OffsetDateTime.now());
                    step.setCreatedAt(OffsetDateTime.now());
                    systemUpgradeStepDao.save(step);
                    try {
                        executor.accept(sql);
                        step.setStatus("SUCCESS");
                        step.setFinishedAt(OffsetDateTime.now());
                        step.setUpdatedAt(OffsetDateTime.now());
                        systemUpgradeStepDao.updateById(step);
                    } catch (Exception e) {
                        String errorMessage = upgradeSqlErrorDecider.extractMessage(e);
                        step.setFinishedAt(OffsetDateTime.now());
                        step.setUpdatedAt(OffsetDateTime.now());
                        if (upgradeSqlErrorDecider.isIgnorable(e)) {
                            step.setStatus("SUCCESS");
                            step.setErrorMessage("IGNORED: " + errorMessage);
                            systemUpgradeStepDao.updateById(step);
                            continue;
                        }
                        step.setStatus("FAILED");
                        step.setErrorMessage(errorMessage);
                        systemUpgradeStepDao.updateById(step);
                        throw e;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to execute upgrade script: " + filename, e);
            }
        }
    }

    private String filenameWithoutSuffix(Resource resource) {
        return Objects.requireNonNullElse(resource.getFilename(), "").replaceAll("\\.sql$", "");
    }

    private int compareVersion(String a, String b) {
        String[] as = a.split("\\.");
        String[] bs = b.split("\\.");
        int[] ai = new int[]{parseInt(as, 0), parseInt(as, 1), parseInt(as, 2)};
        int[] bi = new int[]{parseInt(bs, 0), parseInt(bs, 1), parseInt(bs, 2)};
        for (int i = 0; i < 3; i++) {
            if (ai[i] != bi[i]) {
                return Integer.compare(ai[i], bi[i]);
            }
        }
        return 0;
    }

    private int parseInt(String[] arr, int idx) {
        if (arr.length <= idx) return 0;
        try {
            return Integer.parseInt(arr[idx]);
        } catch (Exception e) {
            return 0;
        }
    }

    private String fingerprint(String sql) {
        try {
            String normalized = sql.replaceAll("\\s+", " ").trim().replaceAll(";$", "").toLowerCase(Locale.ROOT);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit((b) & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return StrUtil.sub(sql, 0, 64);
        }
    }
}
