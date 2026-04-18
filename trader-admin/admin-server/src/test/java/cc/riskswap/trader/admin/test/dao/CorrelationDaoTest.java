package cc.riskswap.trader.admin.test.dao;

import cc.riskswap.trader.admin.dao.CorrelationDao;
import cc.riskswap.trader.admin.dao.entity.Correlation;
import cc.riskswap.trader.admin.dao.mapper.CorrelationMapper;
import cc.riskswap.trader.admin.dao.query.CorrelationListQuery;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class CorrelationDaoTest {

    @Test
    void pageQueryShouldBuildWrapperAndDelegateToMybatisPage() {
        TestableCorrelationDao correlationDao = new TestableCorrelationDao();

        CorrelationListQuery query = new CorrelationListQuery();
        query.setPageNo(2);
        query.setPageSize(5);
        query.setAsset1("000001");

        Page<Correlation> expected = new Page<>(2, 5);
        expected.setRecords(List.of(new Correlation()));
        correlationDao.pageResult = expected;

        Page<Correlation> actual = correlationDao.pageQuery(query);

        Assertions.assertSame(expected, actual);
        Assertions.assertNotNull(correlationDao.capturedWrapper);
    }

    @Test
    void shouldNotDependOnLegacyMapperExtensions() {
        List<String> methodNames = Arrays.stream(CorrelationMapper.class.getMethods())
                .map(method -> method.getName())
                .toList();

        Assertions.assertFalse(methodNames.contains("selectLatestByUniqueKey"));
        Assertions.assertFalse(methodNames.contains("selectLatestPage"));
        Assertions.assertFalse(methodNames.contains("countLatestPage"));
    }

    private static class TestableCorrelationDao extends CorrelationDao {

        private Page<Correlation> pageResult;
        private Wrapper<Correlation> capturedWrapper;

        @Override
        @SuppressWarnings("unchecked")
        public <E extends IPage<Correlation>> E page(E page, Wrapper<Correlation> queryWrapper) {
            this.capturedWrapper = queryWrapper;
            return (E) pageResult;
        }
    }
}
