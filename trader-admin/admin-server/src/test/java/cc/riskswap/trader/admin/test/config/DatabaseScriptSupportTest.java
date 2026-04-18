package cc.riskswap.trader.admin.test.config;

import cc.riskswap.trader.admin.config.DatabaseScriptSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DatabaseScriptSupportTest {

    @Test
    void shouldSplitSqlStatementsAndIgnoreComments() {
        String sql = """
                -- mysql init
                CREATE TABLE broker (
                    id BIGINT PRIMARY KEY
                );

                /* clickhouse table */
                CREATE TABLE fund_nav (
                    symbol String,
                    time DateTime
                ) ENGINE = MergeTree
                ORDER BY (symbol, time);
                """;

        List<String> statements = DatabaseScriptSupport.splitStatements(sql);

        Assertions.assertEquals(2, statements.size());
        Assertions.assertTrue(statements.get(0).startsWith("CREATE TABLE broker"));
        Assertions.assertTrue(statements.get(1).contains("ENGINE = MergeTree"));
    }
}
