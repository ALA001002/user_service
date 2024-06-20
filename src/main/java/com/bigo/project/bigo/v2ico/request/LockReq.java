package com.bigo.project.bigo.v2ico.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LockReq {

    Long userId;

    BigDecimal amount;

    BigDecimal balance;
}
