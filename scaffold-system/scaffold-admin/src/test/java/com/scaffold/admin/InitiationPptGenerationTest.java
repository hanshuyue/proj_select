package com.scaffold.admin;

import com.scaffold.system.service.InitiationPptService;
import com.scaffold.system.service.InitiationProjectService;
import com.scaffold.system.service.InitiationTemplateService;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFRelation;
import org.junit.jupiter.api.Test;
import org.apache.poi.sl.usermodel.PaintStyle;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InitiationPptGenerationTest {
    @Test
    void selectedModesProduceExactlyOneAppendixEachAndKeepAllThreeLines() throws Exception {
        String configured = System.getProperty("dict.template.path", "");
        Path template = Path.of(configured);
        assumeTrue(!configured.isBlank() && Files.isRegularFile(template));
        InitiationProjectService projects = mock(InitiationProjectService.class);
        InitiationTemplateService templates = mock(InitiationTemplateService.class);
        Map<String, Object> templateRow = Map.of("id", 1L);
        when(templates.resolve(null)).thenReturn(templateRow);
        when(templates.path(templateRow)).thenReturn(template);
        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("economicBenefitValues", Map.of("netProfit", List.of("整体利润保留验证")));
        sections.put("threeLineSelections", List.of("高线"));
        sections.put("threeLineRows", List.of(
                Map.of("level", "红线", "satisfaction", "红线填写验证"),
                Map.of("level", "底线", "satisfaction", "底线填写验证"),
                Map.of("level", "高线", "satisfaction", "高线填写验证")));
        when(projects.detail(1L)).thenReturn(Map.of("projectName", "模式联动测试", "sections", sections));
        int baseline = -1;
        for (int count : new int[]{0, 1, 2, 5}) {
            List<Map<String, Object>> appendices = new java.util.ArrayList<>();
            Map<String, Object> currentModeValues = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                currentModeValues.put("mode" + i, Map.of("netProfit", List.of("利润验证" + i)));
                appendices.add(Map.of("key", "mode" + i,
                    "title", "测试模式" + i,
                    "values", Map.of("netProfit", List.of("旧副本数据"))));
            }
            sections.put("modeEconomicBenefitValues", currentModeValues);
            sections.put("economicBenefitAppendices", appendices);
            byte[] bytes = new InitiationPptService(projects, templates).generate(1L, null);
            try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(bytes))) {
                if (baseline < 0) baseline = ppt.getSlides().size();
                assertEquals(baseline + count, ppt.getSlides().size());
                String text = ppt.getSlides().stream().map(this::slideText).collect(java.util.stream.Collectors.joining("\n"));
                assertTrue(text.contains("整体利润保留验证"), "整体测算必须随基础数据保留");
                assertTrue(text.contains("红线填写验证"));
                assertTrue(text.contains("底线填写验证"));
                assertTrue(text.contains("高线填写验证"));
                for (int i = 0; i < count; i++) {
                    final int modeIndex = i;
                    List<XSLFSlide> matching = ppt.getSlides().stream()
                            .filter(slide -> slideText(slide).contains("测试模式" + modeIndex)).toList();
                    assertEquals(1, matching.size());
                    assertEquals(0, matching.get(0).getXmlObject().selectPath(
                            "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main' .//p:custDataLst").length,
                            "Cloned tables must not carry source tag IDs pointing to the new slide layout");
                    assertTrue(slideText(matching.get(0)).contains("利润验证" + i));
                    assertFalse(slideText(matching.get(0)).contains("旧副本数据"));
                }
            }
        }
    }

    @Test
    void writesWebFormValuesIntoTheRealDictTemplate() throws Exception {
        String configured = System.getProperty("dict.template.path", "");
        Path template = Path.of(configured);
        assumeTrue(!configured.isBlank() && Files.isRegularFile(template),
                "Run with -Ddict.template.path=<DICT template.pptx>");

        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("cityName", "测试市");
        sections.put("bureauName", "测试局");
        sections.put("platformName", "测试平台");
        sections.put("branchCompany", "测试分公司");
        sections.put("reportPeriod", "2026年7月");
        sections.put("projectSubject", "测试项目主体");
        sections.put("acquisitionMethod", "公开投标");
        sections.put("bidDate", "2026年7月28日");
        sections.put("fundingSource", "财政资金");
        sections.put("projectOverview", "项目概述写入验证");
        sections.put("businessModel", "合作服务");
        sections.put("businessModelPart", "合作服务");
        sections.put("propertyOwnership", "客户");
        sections.put("groupName", "测试集团");
        sections.put("groupSummary", "集团情况写入验证");
        sections.put("customerTreeSummary", "客户树情况写入验证");
        sections.put("groupTable", Map.of("成员数量（户）2025", "成员10户，其中中高端5户、占比50%",
                "成员数量份额（%）2025", "V网成员186户，V网渗透率59.13%，V网活跃率40.86%",
                "信息化收入（万元）2025", "100", "信息化收入（万元）2026", "125", "信息化收入（万元）growth", "999"));
        sections.put("treeTable", Map.of("V网成员数（户）2025", "V网成员8户，V网渗透率80%，V网活跃率70%"));
        sections.put("groupPeriod1", "2025年1-6月");
        sections.put("groupPeriod2", "2026年1-6月");
        sections.put("treePeriod1", "2025年1-5月");
        sections.put("treePeriod2", "2026年1-5月");
        sections.put("constructionContent", """
                该项目部署在客户侧机房，主要包括视频解析运算服务。
                应用层：建设AI解析、应急调度等功能模块。
                平台层：系统部署在客户侧机房，安全性更高。
                传输层：提供数据专线，保证业务稳定。
                感知层：汇聚天网、各委办局等资源。
                """);
        sections.put("capabilityDemand", "IT平台、云主机和数据专线需求写入验证");
        sections.put("sevenFusionSummary", "融云、融5G及融AI分析写入验证");
        sections.put("fusionLeftTable", Map.of(
                "融云", Map.of("canIntegrate", "是", "integrated", "已融", "amount", "35", "remark", "融云备注验证"),
                "九天能力", Map.of("canIntegrate", "是", "integrated", "规划中", "amount", "12")
        ));
        sections.put("fusionRightTable", Map.of(
                "专业公司方案", Map.of("canIntegrate", "是", "integrated", "已融", "amount", "8", "remark", "中台备注验证"),
                "硬件集成", Map.of("canIntegrate", "否", "integrated", "未融", "amount", "0", "remark", "硬件备注"),
                "软件集成", Map.of("remark", "软件备注")
        ));
        sections.put("fundSource", "资金保障分析写入验证");
        sections.put("riskSummary", "风险评估写入验证");
        sections.put("deliveryPlan", "交付计划写入验证");
        sections.put("deliveryResponsibility", "交付责任写入验证");
        sections.put("deliveryPartners", "合作伙伴写入验证");
        sections.put("deliverySchedule", "工期情况写入验证");
        sections.put("decisionContent", "决策事项写入验证");
        sections.put("indirectRevenue", "间接收益写入验证");
        sections.put("drivenOpportunity", "带动商机写入验证");
        sections.put("benefitAnalysisValues", Map.of(
                "row01", "20%",
                "row04", "9.5%",
                "row20", "2%"
        ));
        sections.put("benefitAnalysisRows", List.of(
                Map.of("key", "custom01", "no", "", "mode", "自定义综合模式", "category", "效益管控",
                        "metric", "净利润率（%）", "projectValue", "20%", "requirement", "8%及以上"),
                Map.of("key", "custom02", "no", "1.1", "mode", "自定义投资模式", "category", "底线管控",
                        "metric", "净现值率（%）", "projectValue", "9.5%", "requirement", "净现值率大于0"),
                Map.of("key", "custom03", "no", "", "mode", "新增收益模式", "category", "效益管控",
                        "metric", "净利润率（%）", "projectValue", "2%", "requirement", "1%及以上")
        ));
        sections.put("benefitAnalysisSelectedKeys", List.of("custom01", "custom03"));
        sections.put("investmentSummary", "80万元，其中涉及投资30万元、合作服务成本50万元。");
        sections.put("procurementComparisonEnabled", true);
        sections.put("procurementType", "PUBLIC");
        sections.put("procurementPublicDescription", "公开采购场景说明写入验证");
        sections.put("procurementDirectDescription", "直接采购场景说明写入验证");
        sections.put("procurementAssessment", "合理性评审写入验证");
        sections.put("procurementPublicRows", List.of(Map.of(
                "content", "服务器",
                "unit", "台",
                "quantity", "2",
                "standardProduct", "标准服务器",
                "supplier1", "供应商甲",
                "supplier2", "供应商乙",
                "supplier3", "供应商丙",
                "otherSupplier", "供应商丁",
                "difference", "参数差异写入验证",
                "requirement", "采购需求写入验证"
        )));
        sections.put("procurementDirectRows", List.of(Map.of(
                "content", "路由器",
                "unit", "台",
                "quantity", "1",
                "standardProduct", "标准路由器",
                "difference", "直接采购差异写入验证",
                "requirement", "直接采购需求写入验证"
        )));
        sections.put("idcEnabled", true);
        sections.put("idcMainRows", List.of(Map.of(
                "billingUnit", "测试IDC成本",
                "category", "配套分摊",
                "description", "征地",
                "cabinetCount", "47",
                "unitPrice", "4.05元/个/月",
                "contractMonths", "60",
                "totalCost", "1.14",
                "remark", "IDC主表写入验证"
        )));
        sections.put("idcCabinetRows", List.of(Map.of(
                "no", "1", "item", "征地", "depreciationYears", "50",
                "investmentAmount", "1215", "plannedCabinets", "5000",
                "monthlyCost", "4.05", "remark", "机柜配套写入验证"
        )));
        sections.put("idcMaintenanceRows", List.of(Map.of(
                "no", "1", "item", "维护-动环", "period", "60个月",
                "maintenanceFee", "5", "cabinetCount", "120",
                "monthlyCost", "17.36", "remark", "维护成本写入验证"
        )));
        sections.put("idcElectricityRows", List.of(Map.of(
                "cabinet", "47架5kW机柜", "electricityPrice", "0.71",
                "pue", "1.31", "utilization", "70%",
                "monthlyCost", "420.92", "remark", "电费测算写入验证"
        )));
        sections.put("economicBenefitValues", Map.of(
                "initialInvestment", List.of("-50.5", "", "", "", "", "", "", "", "", "", "-50.5"),
                "revenue", List.of("2377.17", "328.35", "328.35", "", "", "", "", "", "", "", "3033.87"),
                "netProfitRate", List.of("8%", "13%", "13%", "", "", "", "", "", "", "", "8.20%"),
                "dynamicPayback", List.of("0.94", "", "", "", "", "", "", "", "", "", "0.94")
        ));
        sections.put("investmentEconomicBenefitEnabled", true);
        sections.put("investmentEconomicBenefitValues", Map.of(
                "netCashInflow", List.of("141.32", "49.27", "", "", "", "", "", "", "", "", "16.00%"),
                "discountedCumulativeNetInflow", List.of("133.95", "", "", "", "", "", "", "", "", "", "8.70%"),
                "dynamicPayback", List.of("0.94", "", "", "", "", "", "", "", "", "", "0.94")
        ));
        sections.put("cooperationEconomicBenefitEnabled", true);
        sections.put("cooperationEconomicBenefitValues", Map.of(
                "revenue", List.of("合作收入2377.17", "", "", "", "", "", "", "", "", "", "合作合计3033.87"),
                "netProfitRate", List.of("合作8%", "", "", "", "", "", "", "", "", "", "合作8.20%"),
                "dynamicPayback", List.of("合作0.94", "", "", "", "", "", "", "", "", "", "合作0.94")
        ));
        sections.put("advancePaymentReviewEnabled", true);
        sections.put("advancePaymentStrategyConclusion", "战略卡位评审结论写入验证");
        sections.put("advancePaymentPreferredOrderConclusion", "集团优单评审结论写入验证");
        sections.put("advancePaymentCapabilityConclusion", "能力沉淀评审结论写入验证");
        sections.put("selectionCompany", "测试移动公司");
        sections.put("selectionPlanDecisionDate", "2026年07月10日");
        sections.put("selectionMode", "竞争性甄选");
        sections.put("selectionAmountSummary", "A包预算100万元，B包预算200万元");
        sections.put("selectionAfterBid", "是");
        sections.put("selectionAfterBidReason", "客户要求标后甄选");
        sections.put("selectionReviewPanel", "由3个部门5名专家组成评审小组");
        sections.put("selectionReviewStandard", "采用综合评分法进行评审");
        sections.put("selectionResultDecisionDate", "2026年07月20日");
        sections.put("selectionReviewDate", "2026年07月18日");
        sections.put("selectionRecommendation", "推荐测试合作伙伴");
        sections.put("selectionWinners", "A包：甲公司；B包：乙公司");
        sections.put("selectionCandidates", "A包：丙公司；B包：丁公司");
        sections.put("selectionPublicationDate", "2026年07月21日");
        sections.put("preDecisionApproved", true);
        sections.put("maintenanceEnabled", true);
        sections.put("preDecisionOpinionsEnabled", true);
        sections.put("existingBusinessEnabled", true);
        sections.put("partnerAppendixEnabled", true);
        sections.put("supplyChainFinanceUsed", "否");
        sections.put("supplyChainFinanceReason", "项目回款周期短");
        sections.put("decisionMode", "合作服务模式");
        sections.put("decisionCostIncTax", "16.1万元");
        sections.put("decisionTotalRevenueIncTax", "18.06");
        sections.put("decisionMainRevenueIncTax", "18.06");
        sections.put("decisionOtherRevenueIncTax", "");
        sections.put("decisionIncludeSelection", "是");
        sections.put("preDecisionSummary", "招投标变化说明写入验证");
        sections.put("fundProjectType", "GOVERNMENT");
        sections.put("fundType", "中央财政资金");
        sections.put("fundProofMaterials", "政府预算公开材料写入验证");
        sections.put("fundSource", "资金保障评估写入验证");
        sections.put("fundHistoryIndustryItem", "工业能源");
        sections.put("fundHistoryIndustryDeduction", "18.22");
        sections.put("fundHistoryCustomerItem", "央、国企业单位");
        sections.put("fundHistoryCustomerDeduction", "8.90");
        sections.put("fundHistorySourceItem", "中央财政资金");
        sections.put("fundHistorySourceDeduction", "50");
        sections.put("fundCurrentHistoryDeduction", "1");
        sections.put("fundCurrentSourceDeduction", "2");
        sections.put("fundCurrentDebtDeduction", "3");
        sections.put("fundCurrentAuditDeduction", "80");
        sections.put("fundCurrentCounterpartyDeduction", "80");
        sections.put("fundSpecialBonusType", "项目为国家重点扶持大项目等特殊情况");
        sections.put("fundSpecialBonusScore", "20");
        sections.put("vendorRiskRows", List.of(
                Map.of("vendorName", "固定厂家甲", "amountRatio", "60", "smeFlag", "是", "spendAmount", "48", "qualification", "软件厂商"),
                Map.of("vendorName", "新增厂家乙", "amountRatio", "40", "smeFlag", "否", "spendAmount", "32", "qualification", "代理商")));
        sections.put("projectReceiptMethod", "收款方式自由填写验证");
        sections.put("projectPaymentMethod", "不应与分类条款叠加的旧付款说明");
        sections.put("projectPaymentMethods", List.of("中小企业按项目节点分期付款"));
        sections.put("projectPaymentDetails", Map.of("中小企业按项目节点分期付款", "付款方式分类填写验证"));
        sections.put("cashFlowRows", List.of(
                Map.of("time", "第1年", "revenueExTax", "50", "revenueIncTax", "53", "receiptIncTax", "30", "expenseIncTax", "20", "investmentConfirmation", "按月确认收入"),
                Map.of("time", "第2年", "revenueExTax", "50", "revenueIncTax", "53", "receiptIncTax", "76", "expenseIncTax", "60")));
        sections.put("preDecisionValues", Map.of(
                "row01", Map.of("preDecision", "8.01%", "initiation", "10.03%", "trend", "↑"),
                "row04", Map.of("preDecision", "39.75%", "initiation", "37.04%", "trend", "↓")
        ));

        Map<String, Object> project = new LinkedHashMap<>();
        project.put("projectName", "测试DICT项目");
        project.put("opportunityNo", "TEST-2026-001");
        project.put("customerName", "测试客户");
        project.put("agreementYears", 3);
        project.put("contractAmountIncTax", new BigDecimal("123.45"));
        // Stale saved headers must not override the details used by the PPT tables.
        project.put("totalRevenueIncTax", new BigDecimal("999.00"));
        project.put("totalCostIncTax", new BigDecimal("888.00"));
        project.put("overallProfitRate", new BigDecimal("20.00"));
        project.put("fundRiskLevel", "低");
        project.put("threeLineLevel", "高线");
        project.put("sections", sections);
        sections.put("revenueModes", List.of("投资模式"));
        project.put("incomeItems", List.of(Map.of(
                "mode", "投资模式",
                "name", "专线收入-新增",
                "amountIncTax", new BigDecimal("100.00"),
                "taxRate", new BigDecimal("6.00"),
                "amountExTax", new BigDecimal("999.00"),
                "description", "收益明细说明写入验证"
        ), Map.of(
                "mode", "购销模式",
                "name", "不应导出的未选收益模式",
                "amountIncTax", new BigDecimal("777.00"),
                "taxRate", new BigDecimal("13.00")
        )));
        sections.put("costPrimaryModes", List.of("投资部分"));
        project.put("costItems", List.of(Map.of("mode", "购销模式", "name", "不应导出的未选模式", "amountIncTax", 999), Map.of(
                "mode", "投资部分",
                "name", "DICT投资",
                "amountIncTax", new BigDecimal("80.00"),
                "taxRate", new BigDecimal("6.00"),
                "amountExTax", new BigDecimal("888.00"),
                "description", "投入明细说明写入验证"
        )));
        Path architectureImage = extractTemplateImage(template);
        project.put("attachments", List.of(
                Map.of(
                        "attachmentType", "ARCHITECTURE_DIAGRAM",
                        "originalFilename", architectureImage.getFileName().toString(),
                        "storagePath", architectureImage.toAbsolutePath().toString()),
                Map.of(
                        "attachmentType", "ADVANCE_REVIEW_STRATEGY_IMAGE",
                        "originalFilename", architectureImage.getFileName().toString(),
                        "storagePath", architectureImage.toAbsolutePath().toString()),
                Map.of(
                        "attachmentType", "ADVANCE_REVIEW_PREFERRED_ORDER_IMAGE",
                        "originalFilename", architectureImage.getFileName().toString(),
                        "storagePath", architectureImage.toAbsolutePath().toString()),
                Map.of(
                        "attachmentType", "ADVANCE_REVIEW_CAPABILITY_IMAGE",
                        "originalFilename", architectureImage.getFileName().toString(),
                        "storagePath", architectureImage.toAbsolutePath().toString())
        ));

        InitiationProjectService projects = mock(InitiationProjectService.class);
        InitiationTemplateService templates = mock(InitiationTemplateService.class);
        Map<String, Object> templateRow = Map.of("id", 1L, "storagePath", template.toString());
        when(projects.detail(1L)).thenReturn(project);
        when(templates.resolve(null)).thenReturn(templateRow);
        when(templates.path(templateRow)).thenReturn(template);

        byte[] generated = new InitiationPptService(projects, templates).generate(1L, null);
        Path output = Files.createTempFile(Path.of("target"), "initiation-real-template-test-", ".pptx");
        Files.write(output, generated);

        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(generated))) {
            assertEquals(0, ppt.getPackagePart()
                    .getRelationshipsByType(XSLFRelation.NOTES.getRelation()).size(),
                    "presentation.xml.rels must not directly reference notes slides");
            assertSlideContains(ppt.getSlides().get(0), "测试DICT项目");
            try (XMLSlideShow sourceCover = new XMLSlideShow(Files.newInputStream(template))) {
                if (slideText(sourceCover.getSlides().get(0)).contains("关于XX分公司为XXX项目进行立项的请示")) {
                    assertSlideContains(ppt.getSlides().get(0), "关于测试分公司为测试DICT项目进行立项的请示");
                    assertFalse(slideText(ppt.getSlides().get(0)).contains("测试DICT项目项目"));
                }
            }
            assertSlideContains(ppt.getSlides().get(0), "测试分公司\n2026年7月");
            assertSlideContains(ppt.getSlides().get(1), "测试项目主体");
            assertSlideContains(ppt.getSlides().get(1), "项目概述写入验证");
            assertFalse(slideText(ppt.getSlides().get(1)).contains("产权归属"));
            assertSlideContains(ppt.getSlides().get(2), "集团情况写入验证");
            assertSlideContains(ppt.getSlides().get(2), "测试项目主体集团拓展现状");
            assertSlideContains(ppt.getSlides().get(2), "25.00");
            assertSlideContains(ppt.getSlides().get(2), "V网成员186户，V网渗透率59.13%，V网活跃率40.86%");
            String allText = ppt.getSlides().stream().map(this::slideText).collect(java.util.stream.Collectors.joining("\n"));
            assertTrue(allText.contains("七、项目交付"));
            assertTrue(allText.contains("□  交付责任：交付责任写入验证"));
            assertTrue(allText.contains("□  合作伙伴：合作伙伴写入验证"));
            assertTrue(allText.contains("□  工期情况：工期情况写入验证"));
            assertTrue(allText.contains("□  项目收款方式"));
            assertTrue(allText.contains("收款方式自由填写验证"));
            assertTrue(allText.contains("□  项目付款方式"));
            assertTrue(allText.contains("付款方式分类填写验证"));
            assertTemplateCheckBullet(ppt, "收款方式自由填写验证");
            assertTemplateCheckBullet(ppt, "付款方式分类填写验证");
            assertFalse(allText.contains("不应与分类条款叠加的旧付款说明"));
            assertFalse(allText.contains("中小企业按项目节点分期付款"));
            assertFalse(allText.contains("否则按台账管理"));
            assertTrue(allText.contains("□  项目协议期内现金流"));
            assertTrue(allText.contains("• 是否同意通过合作服务模式投入成本16.1万元（含税）建设该项目"));
            assertTrue(allText.contains("其他业务收入0万元（含税）。"));
            assertTrue(allText.contains("• 是否同意该项目标后甄选结果？"));
            assertTrue(allText.contains("□ 供应链金融合作："));
            assertTrue(allText.contains("➢ 若未使用供应链金融： 该项目因项目回款周期短原因"));
            assertSlideContains(ppt.getSlides().get(3), "视频解析运算服务");
            assertSlideContains(ppt.getSlides().get(3), "应用层：建设AI解析、应急调度等功能模块。");
            assertSlideContains(ppt.getSlides().get(3), "汇聚天网");
            assertTrue(ppt.getSlides().get(3).getShapes().stream()
                    .filter(XSLFPictureShape.class::isInstance)
                    .map(XSLFPictureShape.class::cast)
                    .anyMatch(picture -> picture.getAnchor().getY() >= 140
                            && picture.getAnchor().getWidth() >= 100));
            assertEquals(29, ppt.getSlides().size());
            assertSlideContains(ppt.getSlides().get(2), "2025年1-6月");
            assertSlideContains(ppt.getSlides().get(2), "2026年1-5月");
            assertSlideContains(ppt.getSlides().get(4), "IT平台、云主机和数据专线需求写入验证");
            assertSlideContains(ppt.getSlides().get(4), "融云、融5G及融AI分析写入验证");
            assertSlideContains(ppt.getSlides().get(4), "融云备注验证");
            assertSlideContains(ppt.getSlides().get(4), "中台备注验证");
            assertOverviewFormatting(ppt.getSlides().get(1));
            assertGroupFormatting(ppt.getSlides().get(2));
            XSLFTextShape overview = ppt.getSlides().get(1).getShapes().stream()
                    .filter(XSLFTextShape.class::isInstance)
                    .map(XSLFTextShape.class::cast)
                    .filter(shape -> shape.getText().contains("项目主体："))
                    .findFirst()
                    .orElseThrow();
            assertTrue(overview.getTextParagraphs().stream()
                    .noneMatch(paragraph -> paragraph.getText() == null
                            || paragraph.getText().replaceAll("[\\s\\p{Z}\\p{C}]+", "").isEmpty()));
            XSLFTable middlePlatform = ppt.getSlides().get(4).getShapes().stream()
                    .filter(XSLFTable.class::isInstance)
                    .map(XSLFTable.class::cast)
                    .filter(table -> table.getRows().stream()
                            .flatMap(row -> row.getCells().stream())
                            .anyMatch(cell -> cell.getText().contains("融中台")))
                    .findFirst()
                    .orElseThrow();
            assertEquals("中台备注验证", middlePlatform.getRows().get(1).getCells().get(6).getText());
            assertEquals("硬件备注\n软件备注", middlePlatform.getRows().get(8).getCells().get(6).getText());
            assertTrue(middlePlatform.getRows().get(9).getCells().get(6).getText().isBlank());
            assertFalse(slideText(ppt.getSlides().get(5)).contains("不应导出的未选模式"));
            assertTrue(middlePlatform.getRows().subList(2, 8).stream()
                    .allMatch(row -> row.getCells().get(6).getText().isBlank()));
            assertEquals("是", middlePlatform.getRows().get(5).getCells().get(3).getText());
            assertSlideContains(ppt.getSlides().get(5), "总投入：80.00万元，其中涉及投资80.00万元");
            assertFalse(slideText(ppt.getSlides().get(5)).contains("合作服务成本0.00万元"));
            assertSlideContains(ppt.getSlides().get(5), "投入明细说明写入验证");
            assertSlideContains(ppt.getSlides().get(5), "75.47");
            assertSlideContains(ppt.getSlides().get(6), "公开采购场景说明写入验证");
            assertSlideContains(ppt.getSlides().get(6), "服务器");
            assertSlideContains(ppt.getSlides().get(6), "供应商甲");
            assertSlideContains(ppt.getSlides().get(7), "IDC主表写入验证");
            assertSlideContains(ppt.getSlides().get(7), "机柜配套写入验证");
            assertSlideContains(ppt.getSlides().get(7), "电费测算写入验证");
            assertSlideContains(ppt.getSlides().get(8), "总收益：100.00万元");
            assertSlideContains(ppt.getSlides().get(8), "94.34");
            assertFalse(slideText(ppt.getSlides().get(8)).contains("999"));
            assertSlideContains(ppt.getSlides().get(8), "间接收益：间接收益写入验证");
            assertSlideContains(ppt.getSlides().get(8), "带动商机：带动商机写入验证");
            assertSlideContains(ppt.getSlides().get(8), "收益明细说明写入验证");
            assertFalse(slideText(ppt.getSlides().get(8)).contains("不应导出的未选收益模式"));
            assertSlideContains(ppt.getSlides().get(9), "20%");
            assertSlideContains(ppt.getSlides().get(9), "自定义综合模式");
            assertSlideContains(ppt.getSlides().get(9), "新增收益模式");
            assertFalse(slideText(ppt.getSlides().get(9)).contains("自定义投资模式"));
            assertFalse(slideText(ppt.getSlides().get(9)).contains("9.5%"));
            assertSlideContains(ppt.getSlides().get(9), "项目净利润率20.00%");
            assertSlideContains(ppt.getSlides().get(10), "招投标变化说明写入验证");
            assertSlideContains(ppt.getSlides().get(10), "8.01%");
            assertSlideContains(ppt.getSlides().get(10), "10.03%");
            assertSlideContains(ppt.getSlides().get(11), "3033.87");
            assertSlideContains(ppt.getSlides().get(11), "8.20%");
            assertSlideContains(ppt.getSlides().get(11), "0.94");
            assertSlideContains(ppt.getSlides().get(12), "16.00%");
            assertSlideContains(ppt.getSlides().get(12), "8.70%");
            assertSlideContains(ppt.getSlides().get(13), "合作合计3033.87");
            assertSlideContains(ppt.getSlides().get(13), "合作8.20%");
            assertSlideContains(ppt.getSlides().get(14), "战略卡位评审结论写入验证");
            assertSlideContains(ppt.getSlides().get(14), "集团优单评审结论写入验证");
            assertSlideContains(ppt.getSlides().get(14), "能力沉淀评审结论写入验证");
            assertTrue(ppt.getSlides().get(14).getShapes().stream()
                    .filter(XSLFPictureShape.class::isInstance)
                    .map(XSLFPictureShape.class::cast)
                    .filter(picture -> picture.getAnchor().getY() >= 250)
                    .count() >= 3);
            assertSlideContains(ppt.getSlides().get(15), "测试移动公司2026年07月10日");
            assertSlideContains(ppt.getSlides().get(15), "竞争性甄选");
            assertSlideContains(ppt.getSlides().get(15), "A包预算100万元");
            assertSlideContains(ppt.getSlides().get(15), "评审委员会推荐意见");
            assertFalse(slideText(ppt.getSlides().get(15)).contains("推荐测试合作伙伴"));
            assertSlideContains(ppt.getSlides().get(15), "A包：甲公司");
            assertSlideContains(ppt.getSlides().get(15), "2026年07月21日");
            assertSlideContains(ppt.getSlides().get(18), "政府预算公开材料写入验证");
            assertSlideContains(ppt.getSlides().get(18), "资金保障评估写入验证");
            assertSlideContains(ppt.getSlides().get(18), "工业能源");
            assertSlideContains(ppt.getSlides().get(18), "央、国企业单位");
            assertSlideContains(ppt.getSlides().get(18), "本项得分：100-77.12=22.88分");
            assertSlideContains(ppt.getSlides().get(18), "本项得分：100-6=94分");
            assertSlideContains(ppt.getSlides().get(18), "项目为国家重点扶持大项目等特殊情况");
            assertSlideContains(ppt.getSlides().get(18), "最终得分=22.88×40%+94×60%+20=85.552分");
            assertSlideContains(ppt.getSlides().get(19), "固定厂家甲");
            assertSlideContains(ppt.getSlides().get(19), "新增厂家乙");
            assertSlideContains(ppt.getSlides().get(20), "收款方式自由填写验证");
            assertSlideContains(ppt.getSlides().get(20), "付款方式分类填写验证");
            assertSlideContains(ppt.getSlides().get(20), "第2年");
            assertSlideContains(ppt.getSlides().get(20), "100");
            assertTrue(ppt.getSlides().stream().noneMatch(slide -> slideText(slide).contains("预决策")));
            assertTrue(ppt.getSlides().stream().anyMatch(slide -> slideText(slide).contains("招投标")));
        }
        sections.put("procurementComparisonEnabled", false);
        sections.put("idcEnabled", false);
        sections.put("investmentEconomicBenefitEnabled", false);
        sections.put("cooperationEconomicBenefitEnabled", false);
        sections.put("advancePaymentReviewEnabled", false);
        byte[] withoutComparison = new InitiationPptService(projects, templates).generate(1L, null);
        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(withoutComparison))) {
            assertEquals(24, ppt.getSlides().size());
            assertTrue(ppt.getSlides().stream().noneMatch(slide -> slide.getShapes().stream()
                    .filter(XSLFTextShape.class::isInstance)
                    .map(XSLFTextShape.class::cast)
                    .anyMatch(shape -> shape.getText().contains("集采产品差异性对比"))));
            assertSlideContains(ppt.getSlides().get(6), "总收益：100.00万元");
            assertSlideContains(ppt.getSlides().get(9), "3033.87");
        }
    }

    private Path extractTemplateImage(Path template) throws Exception {
        try (ZipFile zip = new ZipFile(template.toFile())) {
            ZipEntry image = zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().startsWith("ppt/media/"))
                    .filter(entry -> {
                        String name = entry.getName().toLowerCase();
                        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
                    })
                    .findFirst()
                    .orElseThrow();
            String suffix = image.getName().substring(image.getName().lastIndexOf('.'));
            Path output = Path.of("target", "architecture-upload-test" + suffix);
            try (var input = zip.getInputStream(image)) {
                Files.copy(input, output, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return output;
        }
    }

    private void assertOverviewFormatting(XSLFSlide slide) {
        XSLFTextShape overview = slide.getShapes().stream()
                .filter(XSLFTextShape.class::isInstance)
                .map(XSLFTextShape.class::cast)
                .filter(shape -> shape.getText().contains("商务模式："))
                .findFirst()
                .orElseThrow();
        assertTrue(overview.getTextParagraphs().stream()
                .noneMatch(paragraph -> paragraph.getText() == null || paragraph.getText().isBlank()));
        overview.getTextParagraphs().stream()
                .filter(paragraph -> paragraph.getText().contains("："))
                .forEach(paragraph -> {
                    var valueRun = paragraph.getTextRuns().stream()
                            .filter(run -> run.getRawText() != null && !run.getRawText().isBlank())
                            .skip(1)
                            .findFirst()
                            .orElseThrow();
                    PaintStyle paint = valueRun.getFontColor();
                    assertTrue(paint instanceof PaintStyle.SolidPaint);
                    assertTrue(Color.BLACK.equals(((PaintStyle.SolidPaint) paint).getSolidColor().getColor()));
                });
    }

    private void assertGroupFormatting(XSLFSlide slide) {
        XSLFTextShape summary = slide.getShapes().stream()
                .filter(XSLFTextShape.class::isInstance)
                .map(XSLFTextShape.class::cast)
                .filter(shape -> shape.getText().contains("本集团情况："))
                .findFirst()
                .orElseThrow();
        summary.getTextParagraphs().stream()
                .filter(paragraph -> paragraph.getText().contains("情况："))
                .forEach(paragraph -> {
                    var valueRun = paragraph.getTextRuns().stream()
                            .filter(run -> run.getRawText() != null && !run.getRawText().isBlank())
                            .skip(1)
                            .findFirst()
                            .orElseThrow();
                    PaintStyle paint = valueRun.getFontColor();
                    assertTrue(paint instanceof PaintStyle.SolidPaint);
                    assertTrue(Color.BLACK.equals(((PaintStyle.SolidPaint) paint).getSolidColor().getColor()));
                });
        List<XSLFTable> tables = slide.getShapes().stream()
                .filter(XSLFTable.class::isInstance)
                .map(XSLFTable.class::cast)
                .toList();
        assertTrue(tables.stream().anyMatch(table ->
                table.getNumberOfRows() > 8
                        && table.getCell(5, 1).getGridSpan() == 3
                        && table.getCell(6, 1).getGridSpan() == 3
                        && table.getCell(7, 1).getGridSpan() == 3
                        && table.getCell(8, 1).getGridSpan() == 3));
    }

    private void assertSlideContains(XSLFSlide slide, String expected) {
        StringBuilder text = new StringBuilder();
        collectText(slide.getShapes(), text);
        assertTrue(text.toString().contains(expected),
                () -> "Slide did not contain: " + expected + "\nActual: " + text);
    }

    private void assertTemplateCheckBullet(XMLSlideShow ppt, String expectedText) {
        boolean matches = ppt.getSlides().stream()
                .flatMap(slide -> slide.getShapes().stream())
                .filter(XSLFTextShape.class::isInstance)
                .map(XSLFTextShape.class::cast)
                .flatMap(shape -> shape.getTextParagraphs().stream())
                .anyMatch(paragraph -> paragraph.getText().contains(expectedText)
                        && paragraph.isBullet()
                        && "Wingdings".equals(paragraph.getBulletFont())
                        && "ü".equals(paragraph.getBulletCharacter()));
        assertTrue(matches, () -> "Expected template Wingdings check bullet for: " + expectedText);
    }

    private String slideText(XSLFSlide slide) {
        StringBuilder text = new StringBuilder();
        collectText(slide.getShapes(), text);
        return text.toString();
    }

    private void collectText(List<XSLFShape> shapes, StringBuilder text) {
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFTextShape textShape) {
                text.append(textShape.getText()).append('\n');
            }
            if (shape instanceof XSLFTable table) {
                table.getRows().forEach(row -> row.getCells()
                        .forEach(cell -> text.append(cell.getText()).append('\n')));
            }
            if (shape instanceof org.apache.poi.xslf.usermodel.XSLFGroupShape group) {
                collectText(group.getShapes(), text);
            }
        }
    }
}
