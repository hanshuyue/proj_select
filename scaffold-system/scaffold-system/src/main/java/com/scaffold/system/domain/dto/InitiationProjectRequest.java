package com.scaffold.system.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record InitiationProjectRequest(
        String projectName,
        String opportunityNo,
        String customerName,
        String customerType,
        String industryType,
        String departmentName,
        String projectManager,
        Integer reportYear,
        Integer reportMonth,
        Integer agreementYears,
        BigDecimal contractAmountIncTax,
        BigDecimal totalRevenueIncTax,
        BigDecimal totalCostIncTax,
        BigDecimal overallProfitRate,
        String fundRiskLevel,
        String threeLineLevel,
        String status,
        Map<String, Object> sections,
        List<Map<String, Object>> incomeItems,
        List<Map<String, Object>> costItems
) {
}
