package com.bigo.project.bigo.v2ico.service;

import com.bigo.framework.web.domain.AjaxResult;
import com.bigo.project.bigo.v2ico.entity.SymbolConfig;
import com.bigo.project.bigo.v2ico.repository.SymbolConfigRepository;
import com.bigo.project.bigo.v2ico.request.SymbolConfigReq;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class SymbolConfigService {

    @Resource
    SymbolConfigRepository symbolConfigRepository;

    public void sync(SymbolConfigReq req){
        symbolConfigRepository.findFirstBySymbolAndPeriodAndDelFlagFalse("BTCUSDT","1m");
    }

    public AjaxResult queryConfig(SymbolConfigReq req) {
        String period = req.getPeriod();
        String symbol = req.getSymbol();
        SymbolConfig symbolConfig = symbolConfigRepository.findFirstBySymbolAndPeriodAndDelFlagFalse(symbol, period);
        return AjaxResult.success(symbolConfig);
    }
}
