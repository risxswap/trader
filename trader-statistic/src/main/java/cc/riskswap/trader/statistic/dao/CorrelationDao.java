package cc.riskswap.trader.statistic.dao;

import cc.riskswap.trader.statistic.dao.entity.Correlation;
import cc.riskswap.trader.statistic.dao.entity.CorrelationDuplicateGroup;
import cc.riskswap.trader.statistic.dao.mapper.CorrelationMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 证券相关性DAO
 */
@Repository
public class CorrelationDao extends ServiceImpl<CorrelationMapper, Correlation> {

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
