package com.scaffold.system.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record SelectionProjectRequest(
        String projectName,
        String opportunityNo,
        String departmentName,
        Integer reportYear,
        Integer reportMonth,
        BigDecimal serviceLimitExTax,
        BigDecimal serviceLimitIncTax,
        BigDecimal serviceTaxRate,
        BigDecimal resaleBudgetExTax,
        BigDecimal resaleBudgetIncTax,
        BigDecimal resaleTaxRate,
        BigDecimal feeMinExTax,
        BigDecimal feeMinIncTax,
        BigDecimal feeTaxRate,
        String selectedCompany,
        String candidateCompany,
        String publicityMethod,
        String publicityWebsite,
        Boolean publicityEnabled,
        String executionMethod,
        String cooperationCompany,
        String expansionProject,
        String status,
        List<SelectionBidderRequest> bidders
) {
}
