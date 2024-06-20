package com.bigo.project.bigo.api.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class MyTeamVO {

    /**
     * 用户ID
     */
    private Long uid;

    /**
     * 用户账号
     */
    private String userName;

    /**
     * 团队人数
     */
    private Long teamNum;

    private Date registerTime;

    /**
     * 总充值
     */
    private BigDecimal rechargeNum;
}
