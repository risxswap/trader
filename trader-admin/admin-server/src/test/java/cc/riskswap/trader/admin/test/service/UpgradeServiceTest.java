package cc.riskswap.trader.admin.test.service;

import cc.riskswap.trader.admin.dao.ClickHouseSqlExecDao;
import cc.riskswap.trader.admin.dao.SqlExecDao;
import cc.riskswap.trader.admin.dao.SystemUpgradeStepDao;
import cc.riskswap.trader.admin.dao.entity.SystemUpgradeStep;
import cc.riskswap.trader.admin.service.UpgradeService;
import cc.riskswap.trader.admin.service.UpgradeSqlErrorDecider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UpgradeServiceTest {

    private final UpgradeSqlErrorDecider upgradeSqlErrorDecider = new UpgradeSqlErrorDecider();

    @Test
    void shouldSeparateMysqlAndClickHouseUpgradeFlow() throws Exception {
        SystemUpgradeStepDao systemUpgradeStepDao = Mockito.mock(SystemUpgradeStepDao.class);
        SqlExecDao mysqlSqlExecDao = Mockito.mock(SqlExecDao.class);
        ClickHouseSqlExecDao clickHouseSqlExecDao = Mockito.mock(ClickHouseSqlExecDao.class);
        Environment environment = Mockito.mock(Environment.class);
        PathMatchingResourcePatternResolver resolver = Mockito.mock(PathMatchingResourcePatternResolver.class);

        Mockito.when(systemUpgradeStepDao.countAll()).thenReturn(0L);
        Mockito.when(systemUpgradeStepDao.getByDbTypeAndChecksum(
                Mockito.eq("MYSQL"),
                Mockito.anyString()
        )).thenReturn(null);
        Mockito.when(systemUpgradeStepDao.getByDbTypeAndChecksum(
                Mockito.eq("CLICKHOUSE"),
                Mockito.anyString()
        )).thenReturn(null);
        Mockito.when(environment.getProperty("org.springframework.boot.test.context.SpringBootTestContextBootstrapper"))
                .thenReturn("false");
        Mockito.when(resolver.getResources("classpath*:db/upgrade/mysql/*.sql"))
                .thenReturn(new Resource[]{sqlResource("1.0.0.sql", "ALTER TABLE system_sql_script ADD COLUMN db_type VARCHAR(32);")});
        Mockito.when(resolver.getResources("classpath*:db/upgrade/clickhouse/*.sql"))
                .thenReturn(new Resource[]{sqlResource("1.0.0.sql", "ALTER TABLE correlation ADD COLUMN tag String;")});

        UpgradeService upgradeService = new UpgradeService(
                systemUpgradeStepDao,
                mysqlSqlExecDao,
                clickHouseSqlExecDao,
                upgradeSqlErrorDecider,
                environment,
                resolver,
                "classpath*:db/upgrade/mysql/*.sql",
                "classpath*:db/upgrade/clickhouse/*.sql"
        );

        upgradeService.runOnStartup();

        Mockito.verify(resolver).getResources("classpath*:db/upgrade/mysql/*.sql");
        Mockito.verify(resolver).getResources("classpath*:db/upgrade/clickhouse/*.sql");
        Mockito.verify(mysqlSqlExecDao).exec("ALTER TABLE system_sql_script ADD COLUMN IF NOT EXISTS db_type VARCHAR(32) NOT NULL DEFAULT 'MYSQL'");
        Mockito.verify(mysqlSqlExecDao).exec("DROP INDEX system_sql_script_checksum_uidx ON system_sql_script");
        Mockito.verify(mysqlSqlExecDao).exec("CREATE UNIQUE INDEX system_sql_script_db_type_checksum_uidx ON system_sql_script (db_type, checksum)");
        Mockito.verify(mysqlSqlExecDao).exec("ALTER TABLE system_sql_script ADD COLUMN db_type VARCHAR(32)");
        Mockito.verify(clickHouseSqlExecDao).exec("ALTER TABLE correlation ADD COLUMN tag String");
        ArgumentCaptor<SystemUpgradeStep> captor = ArgumentCaptor.forClass(SystemUpgradeStep.class);
        Mockito.verify(systemUpgradeStepDao, Mockito.times(2)).save(captor.capture());
        List<SystemUpgradeStep> steps = captor.getAllValues();
        Assertions.assertEquals("MYSQL", steps.get(0).getDbType());
        Assertions.assertEquals("CLICKHOUSE", steps.get(1).getDbType());
    }

    @Test
    void shouldLoadSeparatedUpgradeScriptsFromClasspath() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        Resource[] mysqlResources = resolver.getResources("classpath*:db/upgrade/mysql/*.sql");
        Resource[] clickHouseResources = resolver.getResources("classpath*:db/upgrade/clickhouse/*.sql");

        Assertions.assertTrue(Arrays.stream(mysqlResources)
                .anyMatch(resource -> "1.0.0.sql".equals(resource.getFilename())));
        Assertions.assertTrue(Arrays.stream(clickHouseResources)
                .anyMatch(resource -> "1.0.0.sql".equals(resource.getFilename())));
    }

    @Test
    void shouldAvoidClickHouseTableCommentSyntaxInScripts() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        Resource[] clickHouseResources = resolver.getResources("classpath*:db/clickhouse.sql");
        Resource[] clickHouseUpgradeResources = resolver.getResources("classpath*:db/upgrade/clickhouse/*.sql");

        List<String> scripts = Arrays.stream(clickHouseResources)
                .map(this::readResource)
                .collect(Collectors.toList());
        scripts.addAll(Arrays.stream(clickHouseUpgradeResources)
                .map(this::readResource)
                .collect(Collectors.toList()));

        Assertions.assertTrue(scripts.stream().noneMatch(script -> script.contains("\nCOMMENT '")));
    }

    @Test
    void shouldIgnoreDuplicateColumnErrorPerStatementAndContinue() throws Exception {
        SystemUpgradeStepDao systemUpgradeStepDao = Mockito.mock(SystemUpgradeStepDao.class);
        SqlExecDao mysqlSqlExecDao = Mockito.mock(SqlExecDao.class);
        ClickHouseSqlExecDao clickHouseSqlExecDao = Mockito.mock(ClickHouseSqlExecDao.class);
        Environment environment = Mockito.mock(Environment.class);
        PathMatchingResourcePatternResolver resolver = Mockito.mock(PathMatchingResourcePatternResolver.class);

        Mockito.when(systemUpgradeStepDao.countAll()).thenReturn(0L);
        Mockito.when(systemUpgradeStepDao.getByDbTypeAndChecksum(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(null);
        Mockito.when(environment.getProperty("org.springframework.boot.test.context.SpringBootTestContextBootstrapper"))
                .thenReturn("false");
        Mockito.when(resolver.getResources("classpath*:db/upgrade/mysql/*.sql"))
                .thenReturn(new Resource[]{sqlResource("1.0.1.sql", """
                        ALTER TABLE system_task ADD COLUMN enabled TINYINT(1);
                        UPDATE system_task SET enabled = 0;
                        """)});
        Mockito.when(resolver.getResources("classpath*:db/upgrade/clickhouse/*.sql"))
                .thenReturn(new Resource[0]);
        Mockito.doThrow(new RuntimeException("Duplicate column name 'enabled'"))
                .when(mysqlSqlExecDao)
                .exec("ALTER TABLE system_task ADD COLUMN enabled TINYINT(1)");

        UpgradeService upgradeService = new UpgradeService(
                systemUpgradeStepDao,
                mysqlSqlExecDao,
                clickHouseSqlExecDao,
                upgradeSqlErrorDecider,
                environment,
                resolver,
                "classpath*:db/upgrade/mysql/*.sql",
                "classpath*:db/upgrade/clickhouse/*.sql"
        );

        upgradeService.runOnStartup();

        Mockito.verify(mysqlSqlExecDao).exec("UPDATE system_task SET enabled = 0");
        ArgumentCaptor<SystemUpgradeStep> saveCaptor = ArgumentCaptor.forClass(SystemUpgradeStep.class);
        Mockito.verify(systemUpgradeStepDao, Mockito.times(2)).save(saveCaptor.capture());
        ArgumentCaptor<SystemUpgradeStep> updateCaptor = ArgumentCaptor.forClass(SystemUpgradeStep.class);
        Mockito.verify(systemUpgradeStepDao, Mockito.times(2)).updateById(updateCaptor.capture());
        List<SystemUpgradeStep> updatedSteps = updateCaptor.getAllValues();
        Assertions.assertEquals("SUCCESS", updatedSteps.get(0).getStatus());
        Assertions.assertTrue(updatedSteps.get(0).getErrorMessage().contains("Duplicate column name"));
        Assertions.assertEquals("SUCCESS", updatedSteps.get(1).getStatus());
    }

    private Resource sqlResource(String filename, String content) {
        byte[] bytes = Objects.requireNonNull(content.getBytes(StandardCharsets.UTF_8));
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private String readResource(Resource resource) {
        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
