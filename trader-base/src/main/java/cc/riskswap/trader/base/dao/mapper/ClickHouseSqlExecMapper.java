package cc.riskswap.trader.base.dao.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;

@ClickHouseMapper
public interface ClickHouseSqlExecMapper {
    @Update("${sql}")
    int exec(@Param("sql") String sql);
}
