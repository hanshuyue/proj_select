package com.scaffold.system.domain.dto;

import java.math.BigDecimal;

public record SelectionBidderRequest(
        Long id,
        String bidderName,
        BigDecimal serviceExTax,
        BigDecimal serviceTaxRate,
        BigDecimal serviceIncTax,
        BigDecimal resaleExTax,
        BigDecimal resaleTaxRate,
        BigDecimal resaleIncTax,
        BigDecimal feeAmount,
        BigDecimal priceScore,
        BigDecimal businessScore,
        BigDecimal totalScore,
        Integer ranking
) {
}
