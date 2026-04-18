package cc.riskswap.trader.admin.dao.base.mapper;

import cc.riskswap.trader.admin.dao.base.entity.Node;
import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@MysqlMapper
public interface NodeMapper extends BaseMapper<Node> {
}
