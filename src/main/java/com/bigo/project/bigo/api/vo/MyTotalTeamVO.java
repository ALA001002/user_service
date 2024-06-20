package com.bigo.project.bigo.api.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MyTotalTeamVO {

    /**
     * 用户ID
     */
    private Long uid;

    /**
     * 上级uid
     */
    private Long parentUid;
    /**
     * 团队人数
     */
    private BigDecimal balance;
}
