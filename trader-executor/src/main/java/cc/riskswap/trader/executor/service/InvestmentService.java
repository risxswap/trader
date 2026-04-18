package cc.riskswap.trader.executor.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cc.riskswap.trader.executor.common.enums.InvestmentStatusEnum;
import cc.riskswap.trader.executor.common.model.dto.InvestmentInfoDto;
import cc.riskswap.trader.executor.dao.InvestmentDao;
import cc.riskswap.trader.executor.dao.entity.Investment;
import cc.riskswap.trader.executor.lock.InvestmentLock;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class InvestmentService {

    @Value("${trader.node.id}")
    private String executorId;

    @Autowired
    private InvestmentDao investmentDao;

    @Autowired
    private InvestmentLock investmentLock;
    
    /**
     * 恢复正在运行的任务
     */
    public void recoverInvestments() {
        // No longer scheduling jobs directly.
        // The admin scheduler triggers strategy tasks instead.
    }
    
    /**
     * 获取正在运行的投资任务
     */
    public List<InvestmentInfoDto> getRunningInvestments() {
        // As scheduling is moved to admin/trader-base, this method could return empty 
        // or return all investments marked as RUNNING in the DB.
        return Collections.emptyList();
    }

    /**
     * 添加并启动投资
     */
    public void startInvestment(Investment investment) {
        if (investment == null) {
            return;
        }
        Long investmentId = investment.getId();
        investmentDao.updateStatusAndExecutorId(investmentId, InvestmentStatusEnum.RUNNING.code, executorId);
        log.info("Investment status updated to RUNNING: group={}, id={}", investment.getGroupName(), investmentId);
    }

    /**
     * 停止投资
     */
    public void stopInvestment(Investment investment) {
        if (investment == null) {
            return;
        }
        Long investmentId = investment.getId();
        investmentDao.updateStatusAndExecutorId(investmentId, InvestmentStatusEnum.STOPPED.code, executorId);
        log.info("Investment status updated to STOPPED: group={}, id={}", investment.getGroupName(), investmentId);
    }

    /**
     * 删除任务
     */
    public void deleteInvestment(Long investmentId) {
        investmentDao.removeById(investmentId);
        log.info("Investment deleted: id={}", investmentId);
    }

    public void execute(Long investmentId) {
        Investment investment = investmentDao.getById(investmentId);
        if (investment == null) {
            return;
        }
        log.info("executing investment {}", investment.getGroupName());
    }

    public void trigger(Long investmentId) {
        Investment investment = investmentDao.getById(investmentId);
        if (investment == null) {
            return;
        }
        log.info("triggering investment {}", investment.getGroupName());
    }

    public void init(Long investmentId) {
        Investment investment = investmentDao.getById(investmentId);
        if (investment == null) {
            return;
        }
        log.info("start to init investment {}", investment.getGroupName());
    }
}
