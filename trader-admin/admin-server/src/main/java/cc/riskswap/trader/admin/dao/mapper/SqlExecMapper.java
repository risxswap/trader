package cc.riskswap.trader.admin.dao.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;

@MysqlMapper
public interface SqlExecMapper {
    @Update("${sql}")
    int exec(@Param("sql") String sql);
}
