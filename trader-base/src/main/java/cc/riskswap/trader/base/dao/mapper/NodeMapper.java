package cc.riskswap.trader.base.dao.mapper;

import cc.riskswap.trader.base.dao.entity.Node;
import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@MysqlMapper
public interface NodeMapper extends BaseMapper<Node> {
}
