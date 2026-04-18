package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.admin.common.model.dto.InvestmentLogDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.param.InvestmentLogParam;
import cc.riskswap.trader.admin.common.model.query.InvestmentLogQuery;
import cc.riskswap.trader.admin.common.enums.InvestmentLogTypeEnum;
import cc.riskswap.trader.admin.dao.InvestmentLogDao;
import cc.riskswap.trader.admin.dao.entity.InvestmentLog;
import cc.riskswap.trader.admin.channel.WeComChannel;
import cc.riskswap.trader.admin.dao.InvestmentDao;
import cc.riskswap.trader.admin.dao.InvestmentPositionDao;
import cc.riskswap.trader.admin.dao.InvestmentTradingDao;
import cc.riskswap.trader.admin.dao.entity.Investment;
import cc.riskswap.trader.admin.dao.entity.InvestmentPosition;
import cc.riskswap.trader.admin.dao.entity.InvestmentTrading;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 投资日志服务
 */
@Slf4j
@Service
public class InvestmentLogService {

    @Autowired
    private InvestmentLogDao investmentLogDao;

    @Autowired
    private InvestmentDao investmentDao;

    @Autowired
    private InvestmentTradingDao investmentTradingDao;

    @Autowired
    private InvestmentPositionDao investmentPositionDao;

    @Autowired
    private WeComChannel weComChannel;

    public PageDto<InvestmentLogDto> list(InvestmentLogQuery query) {
        cc.riskswap.trader.admin.dao.query.InvestmentLogListQuery listQuery = new cc.riskswap.trader.admin.dao.query.InvestmentLogListQuery();
        BeanUtil.copyProperties(query, listQuery);
        Page<InvestmentLog> page = investmentLogDao.pageQuery(listQuery);

        PageDto<InvestmentLogDto> result = new PageDto<>();
        result.setTotal(page.getTotal());
        result.setPageNo((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        
        List<InvestmentLogDto> items = page.getRecords().stream()
                .map(item -> BeanUtil.copyProperties(item, InvestmentLogDto.class))
                .collect(Collectors.toList());
        result.setItems(items);
        
        return result;
    }

    public void add(InvestmentLogParam param) {
        if (StrUtil.isBlank(param.getType())) {
            param.setType(InvestmentLogTypeEnum.TRADE.code);
        }
        InvestmentLog log = BeanUtil.copyProperties(param, InvestmentLog.class);
        investmentLogDao.save(log);
    }

    public void update(InvestmentLogParam param) {
        if (param.getId() == null) {
            throw new IllegalArgumentException("ID cannot be null for update");
        }
        InvestmentLog log = BeanUtil.copyProperties(param, InvestmentLog.class);
        investmentLogDao.updateById(log);
    }
    
    public void delete(Integer id) {
        investmentLogDao.removeById(id);
    }

    /**
     * 发送投资日志通知
     *
     * @param investmentLogId 投资日志ID
     */
    public void notifyInvestmentLog(Integer investmentLogId) {
        InvestmentLog log = investmentLogDao.getById(investmentLogId);
        if (log == null) {
            return;
        }

        // 检查是否已通知
        if (log.getNotified() != null && log.getNotified() == 1) {
            return;
        }

        Investment investment = investmentDao.getById(log.getInvestmentId());
        if (investment == null) {
            return;
        }

        // 查询交易记录
        List<InvestmentTrading> tradings = investmentTradingDao.getListByInvestmentLogId(investmentLogId);

        // 查询持仓记录
        List<InvestmentPosition> positions = investmentPositionDao.getListByInvestmentLogId(investmentLogId);

        // 构建Markdown消息
        StringBuilder md = new StringBuilder();
        md.append("# ").append(investment.getName()).append(" 投资日志\n");
        md.append("> 日期: ").append(DateUtil.format(log.getRecordDate().toLocalDateTime(), "yyyy-MM-dd")).append("\n");
        md.append("> 资产: ").append(NumberUtil.decimalFormat(",###.##", log.getAsset())).append("\n");
        md.append("> 现金: ").append(NumberUtil.decimalFormat(",###.##", log.getCash())).append("\n");
        md.append("> 盈利: <font color=\"").append(log.getProfit().doubleValue() >= 0 ? "info" : "warning").append("\">")
          .append(NumberUtil.decimalFormat(",###.##", log.getProfit())).append("</font>\n");
        if (StrUtil.isNotBlank(log.getRemark())) {
            md.append("> 备注: ").append(log.getRemark()).append("\n");
        }
        
        md.append("\n### 交易记录\n");
        if (CollUtil.isNotEmpty(tradings)) {
            tradings.forEach(t -> {
                md.append("- ").append(t.getAsset())
                  .append(" ").append(t.getVolume().doubleValue() > 0 ? "买入" : "卖出")
                  .append(" ").append(NumberUtil.decimalFormat(",###.##", t.getVolume().abs()))
                  .append(" @ ").append(NumberUtil.decimalFormat(",###.##", t.getPrice())).append("\n");
            });
        } else {
            md.append("无交易记录\n");
        }

        md.append("\n### 持仓记录\n");
        if (CollUtil.isNotEmpty(positions)) {
            positions.forEach(p -> {
                md.append("- ").append(p.getAsset())
                  .append(": ").append(NumberUtil.decimalFormat(",###.##", p.getQuantity()))
                  .append(" (成本: ").append(NumberUtil.decimalFormat(",###.##", p.getCostPrice())).append(")\n");
            });
        } else {
            md.append("无持仓记录\n");
        }

        // 发送通知
        weComChannel.pushMarkdown("investment_log", investment.getName() + " 投资日志", md.toString());

        // 更新状态
        log.setNotified(1);
        investmentLogDao.updateById(log);
    }
}
