package cc.riskswap.trader.executor.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.riskswap.trader.executor.dao.entity.Fund;
import cc.riskswap.trader.executor.dao.mapper.FundMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FundDao extends ServiceImpl<FundMapper, Fund> {

    public List<Fund> listAll() {
        return this.list();
    }
}