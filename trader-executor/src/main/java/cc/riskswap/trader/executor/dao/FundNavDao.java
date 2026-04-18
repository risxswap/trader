package cc.riskswap.trader.executor.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.riskswap.trader.executor.dao.entity.FundNav;
import cc.riskswap.trader.executor.dao.mapper.FundNavMapper;
import org.springframework.stereotype.Repository;

@Repository
public class FundNavDao extends ServiceImpl<FundNavMapper, FundNav> {
}