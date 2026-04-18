package cc.riskswap.trader.statistic.dao;


import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cc.riskswap.trader.statistic.dao.entity.Fund;
import cc.riskswap.trader.statistic.dao.mapper.FundMapper;
import cn.hutool.core.collection.CollectionUtil;

/**
 * 基金DAO类
 */
@Repository
public class FundDao extends ServiceImpl<FundMapper, Fund>{

    /**
     * 根据code查询
     * @param code
     * @return
     */
    public Fund getByCode(String code) {
        LambdaQueryWrapper<Fund> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Fund::getCode, code);
        return baseMapper.selectOne(queryWrapper);
    }
    
    /**
     * 获取所有基金列表
     * @return 基金列表
     */
    public List<Fund> listAll() {
        LambdaQueryWrapper<Fund> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Fund::getStatus, "L"); // 只获取已上市的基金
        return baseMapper.selectList(queryWrapper);
    }

    public Fund getOldestFund(Set<String> codes) {
        if (CollectionUtil.isEmpty(codes)) {
            return null;
        }
        LambdaQueryWrapper<Fund> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Fund::getCode, codes);
        queryWrapper.orderByAsc(Fund::getFoundDate);
        queryWrapper.last("limit 1");
        return baseMapper.selectOne(queryWrapper);
    }

}
