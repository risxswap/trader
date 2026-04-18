package cc.riskswap.trader.admin.test.config;

import cc.riskswap.trader.admin.common.model.dto.FundDto;
import cc.riskswap.trader.admin.common.model.dto.FundNavDto;
import cc.riskswap.trader.admin.common.model.query.FundNavListQuery;
import cc.riskswap.trader.admin.dao.entity.Fund;
import cc.riskswap.trader.admin.dao.entity.FundAdj;
import cc.riskswap.trader.admin.dao.entity.FundMarket;
import cc.riskswap.trader.admin.dao.entity.FundNav;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class FundCodeNamingTest {

    @Test
    void shouldUseSymbolFieldInBaseEntitiesAndCodeFieldInAdminModels() {
        assertHasSymbolFieldAndNoCodeField(Fund.class);
        assertHasSymbolFieldAndNoCodeField(FundAdj.class);
        assertHasSymbolFieldAndNoCodeField(FundMarket.class);
        assertHasSymbolFieldAndNoCodeField(FundNav.class);
        assertHasCodeField(FundDto.class);
        assertHasCodeField(FundNavDto.class);
        assertHasCodeField(FundNavListQuery.class);
    }

    @Test
    void shouldUseCodeColumnForFundTables() throws Exception {
        String mysqlScript = new ClassPathResource("db/mysql.sql").getContentAsString(StandardCharsets.UTF_8);
        String clickHouseScript = new ClassPathResource("db/clickhouse.sql").getContentAsString(StandardCharsets.UTF_8);

        List<String> expectedSnippets = List.of(
                "CREATE TABLE IF NOT EXISTS fund (\n  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',\n  code VARCHAR(32) NOT NULL COMMENT '基金代码'",
                "UNIQUE KEY fund_code_uidx (code)",
                "CREATE TABLE IF NOT EXISTS fund_adj (\n  code String COMMENT '基金代码'",
                "ORDER BY (code, time);",
                "CREATE TABLE IF NOT EXISTS fund_market (\n  code String COMMENT '基金代码'",
                "CREATE TABLE IF NOT EXISTS fund_nav (\n  code String COMMENT '基金代码'"
        );

        Assertions.assertTrue(mysqlScript.contains(expectedSnippets.get(0)));
        Assertions.assertTrue(mysqlScript.contains(expectedSnippets.get(1)));
        Assertions.assertTrue(clickHouseScript.contains(expectedSnippets.get(2)));
        Assertions.assertTrue(clickHouseScript.contains(expectedSnippets.get(3)));
        Assertions.assertTrue(clickHouseScript.contains(expectedSnippets.get(4)));
        Assertions.assertTrue(clickHouseScript.contains(expectedSnippets.get(5)));
        Assertions.assertFalse(mysqlScript.contains("symbol VARCHAR(32)"));
        Assertions.assertFalse(clickHouseScript.contains("symbol String COMMENT '基金代码'"));
    }

    private void assertHasSymbolFieldAndNoCodeField(Class<?> type) {
        Assertions.assertDoesNotThrow(() -> type.getDeclaredField("symbol"));
        Assertions.assertThrows(NoSuchFieldException.class, () -> type.getDeclaredField("code"));
    }

    private void assertHasCodeField(Class<?> type) {
        Assertions.assertDoesNotThrow(() -> type.getDeclaredField("code"));
    }
}
