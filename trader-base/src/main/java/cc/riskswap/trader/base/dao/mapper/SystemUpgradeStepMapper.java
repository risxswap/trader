package cc.riskswap.trader.base.dao.mapper;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.base.dao.entity.SystemUpgradeStep;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@MysqlMapper
public interface SystemUpgradeStepMapper extends BaseMapper<SystemUpgradeStep> {
}
