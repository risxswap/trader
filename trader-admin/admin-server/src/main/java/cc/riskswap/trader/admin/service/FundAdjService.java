package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.base.dao.FundAdjDao;
import cc.riskswap.trader.base.dao.FundDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class FundAdjService {
    @Autowired
    private FundAdjDao fundAdjDao;
    @Autowired
    private FundDao fundDao;
}
