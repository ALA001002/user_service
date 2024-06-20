package com.bigo.project.bigo.v2ico.repository;

import com.bigo.project.bigo.v2ico.entity.SymbolConfig;
import com.bigo.project.bigo.wallet.dao.BaseRepository;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SymbolConfigRepository extends BaseRepository<SymbolConfig> {
    SymbolConfig findFirstBySymbolAndPeriodAndDelFlagFalse(String symbol, String period);

}
