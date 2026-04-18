package cc.riskswap.trader.base.dao;

import cc.riskswap.trader.base.dao.entity.SystemTaskRunLog;
import cc.riskswap.trader.base.dao.mapper.SystemTaskRunLogMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

@Repository("adminSystemTaskRunLogDao")
public class SystemTaskRunLogDao extends ServiceImpl<SystemTaskRunLogMapper, SystemTaskRunLog> {
}
