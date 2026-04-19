package cc.riskswap.trader.collector.test.repository.tushare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import cc.riskswap.trader.collector.common.enums.ExchangeEnum;
import cc.riskswap.trader.collector.repository.tushare.FundTushare;
import cc.riskswap.trader.collector.repository.tushare.TushareManager;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.base.dao.entity.FundAdj;
import cc.riskswap.trader.base.dao.entity.FundMarket;
import cc.riskswap.trader.base.dao.entity.FundNav;

class FundCodeMappingTest {

    @Test
    void entitiesExposeCodeFieldInsteadOfSymbol() {
        assertNotNull(findField(Fund.class, "code"));
        assertNotNull(findField(FundMarket.class, "code"));
        assertNotNull(findField(FundAdj.class, "code"));
        assertNotNull(findField(FundNav.class, "code"));

        assertNull(findField(Fund.class, "symbol"));
        assertNull(findField(FundMarket.class, "symbol"));
        assertNull(findField(FundAdj.class, "symbol"));
        assertNull(findField(FundNav.class, "symbol"));
    }

    @Test
    void fundTushareMapsTsCodeIntoCodeField() throws Exception {
        TushareManager tushareManager = mock(TushareManager.class);
        FundTushare fundTushare = new FundTushare();
        ReflectionTestUtils.setField(fundTushare, "tushareManager", tushareManager);

        when(tushareManager.post(eq("fund_basic"), anyString(), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn("{\"data\":{\"items\":[[\"165509.SZ\",\"某基金\",\"某管理公司\",\"某托管公司\",\"股票型\",\"20200101\",null,\"20200110\",\"20191210\",null,10.0,0.5,0.1,5.0,1.0,100.0,null,\"沪深300\",\"L\",\"成长型\",\"开放式\",\"某受托人\",\"20200115\",\"20200115\",\"E\"]]}}");
        when(tushareManager.parseDate(anyString())).thenReturn(OffsetDateTime.parse("2020-01-15T00:00:00+08:00"));

        List<Fund> fundList = fundTushare.list(1, 10, "E", "L");

        assertEquals(1, fundList.size());
        Field codeField = Fund.class.getDeclaredField("code");
        codeField.setAccessible(true);
        assertEquals("165509.SZ", codeField.get(fundList.get(0)));
        assertEquals(ExchangeEnum.SZSE.code, fundList.get(0).getExchange());
    }

    private Field findField(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
