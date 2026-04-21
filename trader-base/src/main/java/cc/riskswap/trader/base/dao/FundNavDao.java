package cc.riskswap.trader.base.dao;

import java.time.OffsetDateTime;
import java.util.List;
import java.time.ZoneId;
import java.util.Collections;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cc.riskswap.trader.base.dao.entity.FundNav;
import cc.riskswap.trader.base.dao.mapper.FundNavMapper;
import cc.riskswap.trader.base.dao.query.FundNavListQuery;
import cn.hutool.core.util.StrUtil;

@Repository
public class FundNavDao extends ServiceImpl<FundNavMapper, FundNav> {

    public Page<FundNav> pageQuery(FundNavListQuery q) {
        Page<FundNav> page = new Page<>(q.getPageNo(), q.getPageSize());
        LambdaQueryWrapper<FundNav> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(q.getCode())) {
            wrapper.eq(FundNav::getCode, q.getCode());
        }
        if (q.getStartTime() != null) {
            wrapper.ge(FundNav::getTime, q.getStartTime().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
        }
        if (q.getEndTime() != null) {
            wrapper.lt(FundNav::getTime, q.getEndTime().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
        }
        wrapper.orderByDesc(FundNav::getTime);
        return this.page(page, wrapper);
    }

    public List<FundNav> listByCodeAndStartTime(String code, OffsetDateTime startTime) {
        LambdaQueryWrapper<FundNav> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundNav::getCode, code);
        wrapper.ge(FundNav::getTime, startTime);
        wrapper.orderByAsc(FundNav::getTime);
        return this.list(wrapper);
    }

    public List<FundNav> listByCodesAndStartTime(List<String> codes, OffsetDateTime startTime) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FundNav> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FundNav::getCode, codes);
        wrapper.ge(FundNav::getTime, startTime);
        wrapper.orderByAsc(FundNav::getCode, FundNav::getTime);
        return this.list(wrapper);
    }

    public OffsetDateTime getLatestNavDate() {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundNav> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(FundNav::getTime);
        queryWrapper.last("LIMIT 1");
        FundNav fundNav = baseMapper.selectOne(queryWrapper);
        return fundNav != null ? fundNav.getTime() : null;
    }

    public void deleteByTime(OffsetDateTime time) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundNav> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(FundNav::getTime, time);
        this.remove(wrapper);
    }

    public void deleteByCode(String code) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FundNav> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(FundNav::getCode, code);
        this.remove(wrapper);
    }
}
