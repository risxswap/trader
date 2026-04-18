package cc.riskswap.trader.base.dao;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cc.riskswap.trader.base.dao.mapper.FundMapper;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.base.dao.query.FundListQuery;
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
        queryWrapper.eq(Fund::getSymbol, code);
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

    public Fund getOldestFund(Set<String> symbols) {
        if (CollectionUtil.isEmpty(symbols)) {
            return null;
        }
        LambdaQueryWrapper<Fund> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Fund::getSymbol, symbols);
        queryWrapper.orderByAsc(Fund::getFoundDate);
        queryWrapper.last("limit 1");
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 删除基金（按code）
     */
    public int deleteByCode(String code) {
        LambdaQueryWrapper<Fund> qw = new LambdaQueryWrapper<>();
        qw.eq(Fund::getSymbol, code);
        return this.getBaseMapper().delete(qw);
    }

    public Page<Fund> pageQuery(FundListQuery q) {
        Page<Fund> page = new Page<>(q.getPageNo(), q.getPageSize());
        LambdaQueryWrapper<Fund> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Fund::getStatus, "L");
        if (q.getKeyword() != null && !q.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(Fund::getSymbol, q.getKeyword()).or().like(Fund::getName, q.getKeyword()));
        }
        if (q.getMarket() != null && !q.getMarket().isEmpty()) {
            wrapper.eq(Fund::getMarket, q.getMarket());
        }
        if (q.getManagement() != null && !q.getManagement().isEmpty()) {
            wrapper.like(Fund::getManagement, q.getManagement());
        }
        if (q.getCustodian() != null && !q.getCustodian().isEmpty()) {
            wrapper.like(Fund::getCustodian, q.getCustodian());
        }
        if (q.getFundType() != null && !q.getFundType().isEmpty()) {
            wrapper.eq(Fund::getFundType, q.getFundType());
        }
        if (q.getManagementFeeMin() != null) {
            wrapper.ge(Fund::getMFee, q.getManagementFeeMin());
        }
        if (q.getManagementFeeMax() != null) {
            wrapper.le(Fund::getMFee, q.getManagementFeeMax());
        }
        if (q.getCustodianFeeMin() != null) {
            wrapper.ge(Fund::getCFee, q.getCustodianFeeMin());
        }
        if (q.getCustodianFeeMax() != null) {
            wrapper.le(Fund::getCFee, q.getCustodianFeeMax());
        }
        if ("listDate".equalsIgnoreCase(q.getSortBy())) {
            if ("desc".equalsIgnoreCase(q.getSortOrder())) {
                wrapper.orderByDesc(Fund::getListDate);
            } else {
                wrapper.orderByAsc(Fund::getListDate);
            }
        } else if ("managementFee".equalsIgnoreCase(q.getSortBy())) {
            if ("desc".equalsIgnoreCase(q.getSortOrder())) {
                wrapper.orderByDesc(Fund::getMFee);
            } else {
                wrapper.orderByAsc(Fund::getMFee);
            }
        } else if ("custodianFee".equalsIgnoreCase(q.getSortBy())) {
            if ("desc".equalsIgnoreCase(q.getSortOrder())) {
                wrapper.orderByDesc(Fund::getCFee);
            } else {
                wrapper.orderByAsc(Fund::getCFee);
            }
        }
        return this.page(page, wrapper);
    }
}