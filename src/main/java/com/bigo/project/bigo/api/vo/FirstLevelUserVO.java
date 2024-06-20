package com.bigo.project.bigo.api.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FirstLevelUserVO {

    /**
     * 用户总数
     */
    private Long userNum;

    /**
     * 充值数量
     */
    private BigDecimal rechargeAmount;

    /**
     * 提现金额
     */
    private BigDecimal withdrawAmount;
}
