package cc.riskswap.trader.admin.dao;

import cc.riskswap.trader.admin.dao.entity.SystemUpgrade;
import cc.riskswap.trader.admin.dao.mapper.SystemUpgradeMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

@Repository
public class SystemUpgradeDao extends ServiceImpl<SystemUpgradeMapper, SystemUpgrade> {
}
