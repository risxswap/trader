package cc.riskswap.trader.executor.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.riskswap.trader.executor.dao.entity.Correlation;
import cc.riskswap.trader.executor.dao.mapper.CorrelationMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CorrelationDao extends ServiceImpl<CorrelationMapper, Correlation> {
}