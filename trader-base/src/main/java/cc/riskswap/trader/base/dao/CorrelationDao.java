package cc.riskswap.trader.base.dao;

import cc.riskswap.trader.base.dao.entity.Correlation;
import cc.riskswap.trader.base.dao.mapper.CorrelationMapper;
import cc.riskswap.trader.base.dao.query.CorrelationListQuery;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;

/**
 * 证券相关性DAO
 */
@Repository
public class CorrelationDao extends ServiceImpl<CorrelationMapper, Correlation> {

    public Page<Correlation> pageQuery(CorrelationListQuery query) {
        Page<Correlation> page = new Page<>(query.getPageNo(), query.getPageSize());
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Correlation> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (cn.hutool.core.util.StrUtil.isNotBlank(query.getAsset1())) {
            wrapper.eq(Correlation::getSymbol1, query.getAsset1());
        }
        if (cn.hutool.core.util.StrUtil.isNotBlank(query.getAsset2())) {
            wrapper.eq(Correlation::getSymbol2, query.getAsset2());
        }
        if (cn.hutool.core.util.StrUtil.isNotBlank(query.getPeriod())) {
            wrapper.eq(Correlation::getPeriod, query.getPeriod());
        }
        if (query.getMinCoefficient() != null) {
            wrapper.ge(Correlation::getCoefficient, query.getMinCoefficient());
        }
        if (query.getMaxCoefficient() != null) {
            wrapper.le(Correlation::getCoefficient, query.getMaxCoefficient());
        }
        wrapper.orderByDesc(Correlation::getCreatedAt);
        return this.page(page, wrapper);
    }
}
