package com.scaffold.admin;

import com.scaffold.system.service.InitiationPptService;
import com.scaffold.system.service.InitiationProjectService;
import com.scaffold.system.service.InitiationTemplateService;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class InitiationPptRowInsertionTest {

    @Test
    void embeddedBenefitWorkbookKeepsTemplateFormulasAndReceivesProjectInputs() throws Exception {
        InitiationPptService service = new InitiationPptService(
                mock(InitiationProjectService.class), mock(InitiationTemplateService.class));
        Method build = InitiationPptService.class.getDeclaredMethod(
                "buildEmbeddedBenefitWorkbook", java.util.Map.class);
        build.setAccessible(true);
        var project = java.util.Map.of(
                "projectName", "园区网络项目",
                "agreementYears", 3,
                "sections", java.util.Map.of(
                        "economicBaseDetailValues", java.util.Map.of(
                                "incomeIntegration", java.util.Map.of("ctIncTax", 106, "itIncTax", 53)),
                        "economicBenefitValues", java.util.Map.of(
                                "initialInvestment", java.util.List.of("-20", "-10", "0"),
                                "revenue", java.util.List.of("80", "90", "100"),
                                "expense", java.util.List.of("-30", "-35", "-40"),
                                "terminalResources", java.util.List.of("-2", "0", "0")),
                        "depreciationSchedule", java.util.Map.of(
                                "platform", java.util.Map.of(
                                        "commissioningMonths", java.util.List.of(6, 0, 0),
                                        "investments", java.util.List.of(20, 10, 0)))));

        byte[] bytes = (byte[]) build.invoke(service, project);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var assessment = workbook.getSheet("政企项目投资效益评估表");
            assertEquals("园区网络项目", assessment.getRow(2).getCell(3).getStringCellValue());
            assertEquals(3d, assessment.getRow(3).getCell(3).getNumericCellValue());
            assertEquals(106d, assessment.getRow(6).getCell(5).getNumericCellValue());
            assertEquals(53d, assessment.getRow(6).getCell(6).getNumericCellValue());
            assertEquals("F7+G7", assessment.getRow(6).getCell(7).getCellFormula());
            assertEquals(-20d, assessment.getRow(53).getCell(1).getNumericCellValue());
            assertEquals("-折旧和摊销计算表!E2", assessment.getRow(57).getCell(1).getCellFormula());

            var depreciation = workbook.getSheet("折旧和摊销计算表");
            assertEquals(6d, depreciation.getRow(5).getCell(4).getNumericCellValue());
            assertEquals(20d, depreciation.getRow(6).getCell(4).getNumericCellValue());
            assertTrue(workbook.getForceFormulaRecalculation());
        }
    }

    @Test
    void embeddedFinanceDetailWorkbookKeepsFullPrecision() throws Exception {
        InitiationPptService service = new InitiationPptService(
                mock(InitiationProjectService.class), mock(InitiationTemplateService.class));
        Method build = InitiationPptService.class.getDeclaredMethod(
                "buildEmbeddedFinanceDetailWorkbook", java.util.Map.class);
        build.setAccessible(true);
        var project = java.util.Map.of(
                "projectName", "高精度项目",
                "costItems", java.util.List.of(
                        java.util.Map.of("mode", "合作服务模式", "name", "ICT维保成本",
                                "amountIncTax", "53.3123456789", "taxRate", "6.123456789",
                                "amountExTax", "50.2367891234", "description", "完整精度测试"),
                        java.util.Map.of("mode", "合作服务模式", "name", "模板零值占位", "amountIncTax", "0"),
                        java.util.Map.of("mode", "购销模式", "name", "历史未选明细", "amountIncTax", "99")),
                "incomeItems", java.util.List.of(
                        java.util.Map.of("mode", "合作服务模式", "name", "ICT-维保收入",
                                "amountIncTax", "58.6543219876", "taxRate", "6.123456789",
                                "amountExTax", "55.2700012345", "description", "收入明细"),
                        java.util.Map.of("mode", "投资模式", "name", "历史未选收入", "amountIncTax", "88")),
                "sections", java.util.Map.of(
                        "costPrimaryModes", java.util.List.of("成本部分"),
                        "cooperationCostModes", java.util.List.of("合作服务模式"),
                        "revenueModes", java.util.List.of("合作服务模式")));

        byte[] bytes = (byte[]) build.invoke(service, project);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("sheet1");
            assertEquals("支出", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("收入", sheet.getRow(25).getCell(1).getStringCellValue());
            assertEquals(53.3123456789, sheet.getRow(6).getCell(3).getNumericCellValue(), 1e-12);
            assertEquals(0.06123456789, sheet.getRow(6).getCell(4).getNumericCellValue(), 1e-12);
            assertEquals("D7/(1+E7)", sheet.getRow(6).getCell(5).getCellFormula());
            assertEquals("完整精度测试", sheet.getRow(6).getCell(6).getStringCellValue());
            assertEquals(58.6543219876, sheet.getRow(33).getCell(3).getNumericCellValue(), 1e-12);
            assertEquals("0.0000000000", sheet.getRow(6).getCell(3).getCellStyle().getDataFormatString());
            assertTrue(sheet.getMergedRegions().stream().anyMatch(region -> "A2:G2".equals(region.formatAsString())));
            assertTrue(sheet.getMergedRegions().stream().anyMatch(region -> "B26:G26".equals(region.formatAsString())));
            assertEquals(0, workbook.getActiveSheetIndex());
        }
    }

    @Test
    void investmentSummaryKeepsOnlyNonZeroCategories() throws Exception {
        InitiationPptService service = new InitiationPptService(mock(InitiationProjectService.class), mock(InitiationTemplateService.class));
        Method summary = InitiationPptService.class.getDeclaredMethod("buildInvestmentSummary", java.util.Map.class);
        summary.setAccessible(true);
        String text = (String) summary.invoke(service, java.util.Map.of("costItems", java.util.List.of(
                java.util.Map.of("mode", "投资部分", "amountIncTax", "16.10"),
                java.util.Map.of("mode", "合作服务模式", "amountIncTax", "0"))));
        assertTrue(text.contains("涉及投资16.10万元"));
        assertFalse(text.contains("合作服务成本0.00万元"));
    }

    @Test
    void clientProjectInformationReplacesEveryLineInCombinedTemplateShape() throws Exception {
        InitiationPptService service = new InitiationPptService(mock(InitiationProjectService.class), mock(InitiationTemplateService.class));
        Method fill = InitiationPptService.class.getDeclaredMethod("fillClientDueDiligenceText",
                org.apache.poi.xslf.usermodel.XSLFTextShape.class, java.util.Map.class, String.class);
        fill.setAccessible(true);
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            var shape = ppt.createSlide().createTextBox();
            shape.setText("行业类型：XX\n项目需求：XXX\n客户实地拜访：是/否\n项目交付详细地址：XXX");
            var sections = java.util.Map.of("clientIndustryType", "工业能源", "clientProjectDemand", "生产网建设",
                    "clientSiteVisit", "有", "clientDeliveryAddress", "测试地址");
            fill.invoke(service, shape, java.util.Map.of("sections", sections), shape.getText());
            assertTrue(shape.getText().contains("行业类型：工业能源"));
            assertTrue(shape.getText().contains("项目需求：生产网建设"));
            assertTrue(shape.getText().contains("客户实地拜访：有"));
            assertTrue(shape.getText().contains("项目交付详细地址：测试地址"));
        }
    }

    @Test
    void amountTotalsIgnoreTextAndKeepEveryNumber() throws Exception {
        InitiationPptService service = new InitiationPptService(mock(InitiationProjectService.class), mock(InitiationTemplateService.class));
        Method sum = InitiationPptService.class.getDeclaredMethod("embeddedNumberSum", Object.class);
        sum.setAccessible(true);
        assertEquals(new java.math.BigDecimal("1237.00"), sum.invoke(service, "设备1,200.50万元，服务40万元，调整-3.50万元"));
        assertEquals(java.math.BigDecimal.ZERO, sum.invoke(service, "无"));
    }

    @Test
    void coverAcceptsDepartmentWithoutAppendingBranchSuffix() throws Exception {
        InitiationPptService service = new InitiationPptService(mock(InitiationProjectService.class), mock(InitiationTemplateService.class));
        Method fill = InitiationPptService.class.getDeclaredMethod("fillCoverPage", org.apache.poi.xslf.usermodel.XSLFTextShape.class, java.util.Map.class, String.class);
        fill.setAccessible(true);
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            var shape = ppt.createSlide().createTextBox();
            String title = "关于XX分公司为XXX项目进行立项的请示";
            shape.setText(title);
            fill.invoke(service, shape, java.util.Map.of("projectName", "测试项目", "sections", java.util.Map.of("branchCompany", "政企客户部", "coverApplicant", "张三")), title);
            assertEquals("关于政企客户部为测试项目进行立项的请示", shape.getText());
            var project = java.util.Map.of("projectName", "测试项目", "sections", java.util.Map.of("branchCompany", "政企客户部", "coverApplicant", "张三", "reportPeriod", "2026年9月"));
            fill.invoke(service, shape, project, "XX分公司");
            assertEquals("张三", shape.getText());
            fill.invoke(service, shape, project, "2026年X月");
            assertEquals("2026年9月", shape.getText());
            fill.invoke(service, shape, project, "XX分公司\n2026年X月");
            assertEquals("张三\n2026年9月", shape.getText());
        }
    }

    @Test
    void insertedStyledCellsRemainConnectedToTheirXmlObjects() throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            XSLFTable table = ppt.createSlide().createTable();
            XSLFTableRow styleRow = table.addRow();
            styleRow.addCell().setText("明细");
            styleRow.addCell().setText("样式行");

            InitiationPptService service = new InitiationPptService(
                    mock(InitiationProjectService.class),
                    mock(InitiationTemplateService.class));
            Method insert = InitiationPptService.class.getDeclaredMethod(
                    "insertStyledTableRow", XSLFTable.class, int.class, int.class);
            insert.setAccessible(true);
            insert.invoke(service, table, 1, 0);

            assertDoesNotThrow(() -> {
                table.getRows().get(1).getCells().get(0).setText("新增明细");
                table.getRows().get(1).getCells().get(1).setText("100.00");
                table.getRows().get(1).getCells().get(0).getText();
                table.getRows().get(1).getCells().get(1).getText();
            });
            assertEquals("新增明细", table.getRows().get(1).getCells().get(0).getText());
        }
    }
}
