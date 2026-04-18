package cc.riskswap.trader.admin.dao;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cc.riskswap.trader.admin.dao.query.ExchangeListQuery;
import cc.riskswap.trader.admin.dao.entity.Exchange;
import cc.riskswap.trader.admin.dao.mapper.ExchangeMapper;
import cn.hutool.core.util.StrUtil;

@Repository
public class ExchangeDao extends ServiceImpl<ExchangeMapper, Exchange> {

    public Page<Exchange> pageQuery(ExchangeListQuery q) {
        Page<Exchange> page = new Page<>(q.getPageNo(), q.getPageSize());
        LambdaQueryWrapper<Exchange> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(q.getKeyword())) {
            wrapper.and(w -> w.like(Exchange::getCode, q.getKeyword())
                    .or().like(Exchange::getName, q.getKeyword()));
        }
        wrapper.orderByDesc(Exchange::getCreatedAt);
        return this.page(page, wrapper);
    }
}
