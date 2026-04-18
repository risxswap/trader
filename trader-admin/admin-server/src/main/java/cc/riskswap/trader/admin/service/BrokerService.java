package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.admin.common.model.dto.BrokerDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.param.BrokerParam;
import cc.riskswap.trader.admin.common.model.query.BrokerListQuery;
import cc.riskswap.trader.admin.dao.BrokerDao;
import cc.riskswap.trader.admin.dao.entity.Broker;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrokerService {

    @Autowired
    private BrokerDao brokerDao;

    public PageDto<BrokerDto> list(BrokerListQuery q) {
        cc.riskswap.trader.admin.dao.query.BrokerListQuery listQuery = new cc.riskswap.trader.admin.dao.query.BrokerListQuery();
        BeanUtil.copyProperties(q, listQuery);
        Page<Broker> page = brokerDao.pageQuery(listQuery);
        List<BrokerDto> list = page.getRecords().stream()
                .map(b -> BeanUtil.copyProperties(b, BrokerDto.class))
                .collect(Collectors.toList());
        
        PageDto<BrokerDto> res = new PageDto<>();
        res.setItems(list);
        res.setTotal(page.getTotal());
        res.setPageNo(q.getPageNo());
        res.setPageSize(q.getPageSize());
        return res;
    }

    public void add(BrokerParam param) {
        Broker broker = new Broker();
        BeanUtil.copyProperties(param, broker);
        broker.setCreatedAt(OffsetDateTime.now());
        broker.setUpdatedAt(OffsetDateTime.now());
        brokerDao.save(broker);
    }

    public void update(BrokerParam param) {
        if (param.getId() == null) {
            return;
        }
        Broker broker = new Broker();
        BeanUtil.copyProperties(param, broker);
        broker.setUpdatedAt(OffsetDateTime.now());
        brokerDao.updateById(broker);
    }

    public void delete(Integer id) {
        brokerDao.removeById(id);
    }
    
    public BrokerDto get(Integer id) {
        Broker broker = brokerDao.getById(id);
        if (broker == null) {
            return null;
        }
        return BeanUtil.copyProperties(broker, BrokerDto.class);
    }
}
