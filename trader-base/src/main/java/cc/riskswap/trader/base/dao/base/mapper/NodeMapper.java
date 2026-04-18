package cc.riskswap.trader.base.dao.base.mapper;

import cc.riskswap.trader.base.dao.base.entity.Node;
import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@MysqlMapper
public interface NodeMapper extends BaseMapper<Node> {
}
