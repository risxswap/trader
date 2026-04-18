package cc.riskswap.trader.base.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.riskswap.trader.base.dao.query.BrokerListQuery;
import cc.riskswap.trader.base.dao.entity.Broker;
import cc.riskswap.trader.base.dao.mapper.BrokerMapper;
import org.springframework.stereotype.Repository;
import cn.hutool.core.util.StrUtil;

@Repository
public class BrokerDao extends ServiceImpl<BrokerMapper, Broker> {

    public Page<Broker> pageQuery(BrokerListQuery q) {
        Page<Broker> page = new Page<>(q.getPageNo(), q.getPageSize());
        LambdaQueryWrapper<Broker> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(q.getKeyword())) {
            wrapper.like(Broker::getName, q.getKeyword())
                   .or()
                   .like(Broker::getCode, q.getKeyword());
        }
        wrapper.orderByDesc(Broker::getCreatedAt);
        return baseMapper.selectPage(page, wrapper);
    }
}
