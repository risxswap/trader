package cc.riskswap.trader.base.dao;

import cc.riskswap.trader.base.dao.mapper.SqlExecMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SqlExecDao {
    private final SqlExecMapper mapper;
    public SqlExecDao(SqlExecMapper mapper) {
        this.mapper = mapper;
    }
    public int exec(String sql) {
        return mapper.exec(sql);
    }
}
