package cc.riskswap.trader.admin.test.dao;

import cc.riskswap.trader.base.dao.mapper.CorrelationMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class CorrelationMapperSqlTest {

    @Test
    void shouldNotExposeClickHouseUpdateMethodForCorrelation() {
        List<String> methodNames = Arrays.stream(CorrelationMapper.class.getMethods())
                .map(method -> method.getName())
                .toList();

        Assertions.assertFalse(methodNames.contains("updateByPrimaryId"));
    }

    @Test
    void shouldOnlyExposeBaseMapperContract() {
        Assertions.assertTrue(BaseMapper.class.isAssignableFrom(CorrelationMapper.class));
    }
}
