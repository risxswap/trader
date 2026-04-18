package cc.riskswap.trader.admin.system;

import cc.riskswap.trader.admin.config.DatabaseScriptSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer {

    @Qualifier("mysqlDataSource")
    private final DataSource mysqlDataSource;

    @Qualifier("clickHouseDataSource")
    private final DataSource clickHouseDataSource;

    private final ResourceLoader resourceLoader;
    private final Environment environment;

    @Value("${trader.db.init.enabled:true}")
    private boolean enabled;

    @Value("${trader.db.init.mysql-script:classpath:db/mysql.sql}")
    private String mysqlScript;

    @Value("${trader.db.init.clickhouse-script:classpath:db/clickhouse.sql}")
    private String clickhouseScript;

    public void initializeOnStartup() {
        if (!enabled) {
            return;
        }
        if (DatabaseScriptSupport.isSpringBootTestEnvironment(environment.getProperty("org.springframework.boot.test.context.SpringBootTestContextBootstrapper"))) {
            return;
        }
        executeScript(mysqlDataSource, mysqlScript);
        executeScript(clickHouseDataSource, clickhouseScript);
    }

    private void executeScript(DataSource dataSource, String location) {
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                return;
            }
            String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            List<String> statements = DatabaseScriptSupport.splitStatements(sql);
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                for (String item : statements) {
                    statement.execute(item);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
