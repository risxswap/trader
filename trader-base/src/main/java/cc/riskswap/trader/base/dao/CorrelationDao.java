package cc.riskswap.trader.base.dao;

import cc.riskswap.trader.base.dao.entity.Correlation;
import cc.riskswap.trader.base.dao.entity.CorrelationDuplicateGroup;
import cc.riskswap.trader.base.dao.mapper.CorrelationMapper;
import cc.riskswap.trader.base.dao.query.CorrelationListQuery;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;
import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * 证券相关性DAO
 */
@Repository
public class CorrelationDao extends ServiceImpl<CorrelationMapper, Correlation> {

    public Page<Correlation> pageQuery(CorrelationListQuery query) {
        Page<Correlation> page = new Page<>(query.getPageNo(), query.getPageSize());
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Correlation> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getAsset1())) {
            wrapper.eq(Correlation::getAsset1, query.getAsset1());
        }
        if (StrUtil.isNotBlank(query.getAsset2())) {
            wrapper.eq(Correlation::getAsset2, query.getAsset2());
        }
        if (StrUtil.isNotBlank(query.getPeriod())) {
            wrapper.eq(Correlation::getPeriod, query.getPeriod());
        }
        BigDecimal minCoefficient = query.getMinCoefficient();
        if (minCoefficient != null) {
            wrapper.ge(Correlation::getCoefficient, minCoefficient);
        }
        BigDecimal maxCoefficient = query.getMaxCoefficient();
        if (maxCoefficient != null) {
            wrapper.le(Correlation::getCoefficient, maxCoefficient);
        }
        wrapper.orderByDesc(Correlation::getCreatedAt);
        return this.page(page, wrapper);
    }

    public Correlation getByPrimaryId(Long id) {
        return baseMapper.selectByPrimaryId(id);
    }

    public boolean removeByPrimaryId(Long id) {
        return baseMapper.deleteByPrimaryId(id) > 0;
    }

    public List<CorrelationDuplicateGroup> listDuplicateGroups(int limit, int offset) {
        return baseMapper.selectDuplicateGroups(limit, offset);
    }

    public List<Long> listHistoricalIds(String asset1, String asset2, String period) {
        return baseMapper.selectHistoricalIds(asset1, asset2, period);
    }

    public boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    public Correlation getByUniqueKey(String asset1, String asset2, String period) {
        return baseMapper.selectLatestByUniqueKey(asset1, asset2, period);
    }
}
