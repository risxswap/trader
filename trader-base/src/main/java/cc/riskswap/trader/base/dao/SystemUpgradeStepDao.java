package cc.riskswap.trader.base.dao;

import cc.riskswap.trader.base.dao.entity.SystemUpgradeStep;
import cc.riskswap.trader.base.dao.mapper.SystemUpgradeStepMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

@Repository
public class SystemUpgradeStepDao extends ServiceImpl<SystemUpgradeStepMapper, SystemUpgradeStep> {

    public long countAll() {
        return this.count();
    }

    public SystemUpgradeStep getByDbTypeAndChecksum(String dbType, String checksum) {
        LambdaQueryWrapper<SystemUpgradeStep> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemUpgradeStep::getDbType, dbType);
        wrapper.eq(SystemUpgradeStep::getChecksum, checksum);
        return this.getOne(wrapper);
    }
}
