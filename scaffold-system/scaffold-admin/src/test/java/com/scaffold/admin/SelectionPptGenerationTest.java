package com.scaffold.admin;

import com.scaffold.system.mapper.SelectionMapper;
import com.scaffold.system.service.PptTemplateService;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFObjectShape;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Ole10Native;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGraphicalObjectFrame;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SelectionPptGenerationTest {
    private static final Charset GBK = Charset.forName("GBK");

    @Test
    void generatesFromProvidedFixedTemplate() throws Exception {
        SelectionMapper mapper = mock(SelectionMapper.class);
        Path template = Path.of(System.getProperty("selection.template.path", "../../deploy/templates/selection/standard.pptx"));
        when(mapper.selectProject(1L)).thenReturn(Map.ofEntries(
                Map.entry("id", 1L), Map.entry("projectName", "智慧园区"),
                Map.entry("opportunityNo", "SJ-2026-001"), Map.entry("departmentName", "政企客户部"),
                Map.entry("reportYear", 2026), Map.entry("reportMonth", 7),
                Map.entry("selectedCompany", "甲方科技有限公司"), Map.entry("candidateCompany", "乙方科技有限公司"),
                Map.entry("publicityEnabled", 1), Map.entry("publicityMethod", "门户"),
                Map.entry("publicityWebsite", "https://xe.sd.chinamobile.com/pms-portal-react/#/console4"),
                Map.entry("executionMethod", "与中选合作伙伴签署承诺书"),
                Map.entry("cooperationCompany", "甲方科技有限公司"), Map.entry("expansionProject", "智慧园区")
        ));
        when(mapper.selectBidders(1L)).thenReturn(List.of(
                bidder("甲方科技有限公司", 90, 1),
                bidder("乙方科技有限公司", 86, 2),
                bidder("丙方科技有限公司", 82, 3),
                bidder("丁方科技有限公司", 78, 4)
        ));
        when(mapper.selectTemplate(2L)).thenReturn(Map.of("storagePath", template.toString()));
        Path reviewReport = Files.createTempFile("selection-review-", ".pdf");
        Files.writeString(reviewReport, "%PDF-1.4\n% embedded selection review report\n%%EOF\n");
        when(mapper.selectReviewReport(1L)).thenReturn(Map.of(
                "reviewReportName", "评审报告.pdf", "reviewReportPath", reviewReport.toString()));

        byte[] generated;
        try {
            generated = new PptTemplateService(mapper).generatePptFromTemplate(1L, 2L);
        } finally {
            Files.deleteIfExists(reviewReport);
        }

        assertTrue(generated.length > 100_000);
        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(generated))) {
            assertEquals(4, ppt.getSlides().size());
            String allText = ppt.getSlides().stream().flatMap(s -> s.getShapes().stream())
                    .filter(org.apache.poi.xslf.usermodel.XSLFTextShape.class::isInstance)
                    .map(org.apache.poi.xslf.usermodel.XSLFTextShape.class::cast)
                    .map(org.apache.poi.xslf.usermodel.XSLFTextShape::getText).reduce("", String::concat);
            assertTrue(allText.contains("智慧园区"));
            assertTrue(allText.contains("附件："));
            assertTrue(!allText.contains("附件：评审报告.pdf"));
            assertTrue(allText.contains("甲方科技有限公司"));
            assertTrue(allText.contains("https://xe.sd.chinamobile.com/pms-portal-react/#/console4"));
            assertTrue(allText.contains("与中选合作伙伴签署承诺书"));
            assertTrue(!allText.contains("与中选合作伙伴签署常规合同（其他成本类项目）"));
            var projectShape = ppt.getSlides().get(1).getShapes().stream()
                    .filter(org.apache.poi.xslf.usermodel.XSLFTextShape.class::isInstance)
                    .map(org.apache.poi.xslf.usermodel.XSLFTextShape.class::cast)
                    .filter(s -> s.getText().contains("项目名称及基本信息"))
                    .findFirst().orElseThrow();
            assertTrue(projectShape.getTextParagraphs().size() >= 6);
            assertTrue(!projectShape.getText().contains("评审委员会推荐意见"));
            var bidderTable = ppt.getSlides().get(1).getShapes().stream()
                    .filter(org.apache.poi.xslf.usermodel.XSLFTable.class::isInstance)
                    .map(org.apache.poi.xslf.usermodel.XSLFTable.class::cast)
                    .findFirst().orElseThrow();
            var firstDataRow = bidderTable.getRows().stream()
                    .filter(r -> !r.getCells().isEmpty() && "1".equals(r.getCells().get(0).getText().trim()))
                    .findFirst().orElseThrow();
            assertEquals("40", firstDataRow.getCells().get(8).getText());
            assertEquals("90", firstDataRow.getCells().get(10).getText());
            assertEquals("1", firstDataRow.getCells().get(11).getText());
            XSLFObjectShape attachment = ppt.getSlides().get(1).getShapes().stream()
                    .filter(XSLFObjectShape.class::isInstance)
                    .map(XSLFObjectShape.class::cast).findFirst().orElseThrow();
            assertEquals("评审报告.pdf", attachment.getCTOleObject().getName());
            assertTrue(attachment.getCTOleObject().getShowAsIcon());
            var objectProps = ((CTGraphicalObjectFrame) attachment.getXmlObject())
                    .getNvGraphicFramePr().getCNvPr();
            assertEquals("评审报告.pdf", objectProps.getName());
            assertEquals("评审报告.pdf", objectProps.getTitle());
            assertEquals("评审报告.pdf", objectProps.getDescr());
            try (POIFSFileSystem poifs = new POIFSFileSystem(attachment.getObjectData().getInputStream())) {
                NativeOleData embedded = readNativeOleData(poifs);
                assertEquals("评审报告.pdf", embedded.label());
                assertEquals("评审报告.pdf", embedded.fileName());
                assertEquals("评审报告.pdf", embedded.command());
                assertArrayEquals("%PDF-1.4\n% embedded selection review report\n%%EOF\n".getBytes(),
                        embedded.data());
            }
        }
    }

    private Map<String, Object> bidder(String name, int score, int rank) {
        return Map.ofEntries(
                Map.entry("bidderName", name), Map.entry("serviceExTax", new BigDecimal("100000")),
                Map.entry("serviceTaxRate", new BigDecimal("6")), Map.entry("serviceIncTax", new BigDecimal("106000")),
                Map.entry("resaleExTax", new BigDecimal("200000")), Map.entry("resaleTaxRate", new BigDecimal("6")),
                Map.entry("resaleIncTax", new BigDecimal("212000")), Map.entry("feeAmount", new BigDecimal("3000")),
                Map.entry("priceScore", new BigDecimal(score - 50)), Map.entry("businessScore", new BigDecimal("50")),
                Map.entry("totalScore", new BigDecimal(score)), Map.entry("ranking", rank)
        );
    }

    private NativeOleData readNativeOleData(POIFSFileSystem poifs) throws Exception {
        byte[] bytes;
        try (DocumentInputStream input = poifs.createDocumentInputStream(Ole10Native.OLE10_NATIVE)) {
            bytes = input.readAllBytes();
        }
        int pos = 4;
        pos += 2;
        ReadString label = readGbkZeroTerminated(bytes, pos);
        pos = label.next();
        ReadString fileName = readGbkZeroTerminated(bytes, pos);
        pos = fileName.next();
        pos += 4;
        int commandLength = readIntLE(bytes, pos);
        pos += 4;
        String command = new String(bytes, pos, Math.max(0, commandLength - 1), GBK);
        pos += commandLength;
        int dataSize = readIntLE(bytes, pos);
        pos += 4;
        byte[] data = java.util.Arrays.copyOfRange(bytes, pos, pos + dataSize);
        return new NativeOleData(label.value(), fileName.value(), command, data);
    }

    private ReadString readGbkZeroTerminated(byte[] bytes, int start) {
        int end = start;
        while (end < bytes.length && bytes[end] != 0) end++;
        return new ReadString(new String(bytes, start, end - start, GBK), end + 1);
    }

    private int readIntLE(byte[] bytes, int pos) {
        return (bytes[pos] & 0xff) | ((bytes[pos + 1] & 0xff) << 8)
                | ((bytes[pos + 2] & 0xff) << 16) | ((bytes[pos + 3] & 0xff) << 24);
    }

    private record ReadString(String value, int next) {}
    private record NativeOleData(String label, String fileName, String command, byte[] data) {}

}
