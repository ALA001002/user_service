package com.bigo.project.bigo.v2ico.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name="symbol_config")
public class SymbolConfig {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "row_id")
    private Long rowId;
    @Basic
    @Column(name = "del_flag")
    private Boolean delFlag;
    @Basic
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Basic
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name="symbol")
    private String symbol;

    @Column(name="period")
    private String period;

    @Column(name="open_flag")
    private Boolean openFlag;

    @Column(name="min_slots")
    private BigDecimal minSlots;

    @Column(name="max_slots")
    private BigDecimal maxSlots;

    @Column(name="margin")
    private BigDecimal margin;

    @Column(name="free_margin")
    private BigDecimal freeMargin;

    @Column(name="swap")
    private BigDecimal swap;

    @Column(name="interest")
    private BigDecimal interest;

}
