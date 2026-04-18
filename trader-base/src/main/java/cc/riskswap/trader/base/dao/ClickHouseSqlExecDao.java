package cc.riskswap.trader.base.dao;

import cc.riskswap.trader.base.dao.mapper.ClickHouseSqlExecMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ClickHouseSqlExecDao {
    private final ClickHouseSqlExecMapper mapper;

    public ClickHouseSqlExecDao(ClickHouseSqlExecMapper mapper) {
        this.mapper = mapper;
    }

    public int exec(String sql) {
        return mapper.exec(sql);
    }
}
