package cc.riskswap.trader.base.dao;

import cc.riskswap.trader.base.dao.entity.SystemUpgrade;
import cc.riskswap.trader.base.dao.mapper.SystemUpgradeMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

@Repository
public class SystemUpgradeDao extends ServiceImpl<SystemUpgradeMapper, SystemUpgrade> {
}
