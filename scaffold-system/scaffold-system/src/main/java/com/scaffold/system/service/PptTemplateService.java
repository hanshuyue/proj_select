package com.scaffold.system.service;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.mapper.SelectionMapper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFObjectShape;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.sl.usermodel.ObjectMetaData;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.hpsf.ClassIDPredefined;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGraphicalObjectFrame;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PptTemplateService {
    private static final Pattern TOKEN = Pattern.compile("\\{\\{([a-zA-Z][\\w.]*)}}");
    private static final Charset GBK = Charset.forName("GBK");
    private static final ObjectMetaData PACKAGE_META_DATA = new ObjectMetaData() {
        @Override public String getObjectName() { return "Package"; }
        @Override public String getProgId() { return "Package"; }
        @Override public org.apache.poi.hpsf.ClassID getClassID() { return ClassIDPredefined.OLE_V1_PACKAGE.getClassID(); }
        @Override public String getOleEntry() { return "Package"; }
    };
    private final SelectionMapper mapper;

    public PptTemplateService(SelectionMapper mapper) {
        this.mapper = mapper;
    }

    public byte[] generatePptFromTemplate(Long projectId, Long templateId) {
        Map<String, Object> project = mapper.selectProject(projectId);
        if (project == null) throw new BusinessException(404, "甄选结果不存在");
        List<Map<String, Object>> bidders = mapper.selectBidders(projectId);
        Map<String, Object> reviewReport = mapper.selectReviewReport(projectId);
        Map<String, Object> template = templateId == null ? mapper.selectDefaultTemplate() : mapper.selectTemplate(templateId);
        if (template == null) throw new BusinessException("未配置可用的PPT模板");
        try {
            byte[] source = Files.readAllBytes(Path.of(String.valueOf(template.get("storagePath"))));
            try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(source));
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                Map<String, String> values = values(project);
                List<XSLFSlide> slides = ppt.getSlides();
                for (int i = 0; i < slides.size(); i++) {
                    fillSlide(slides.get(i), i, values, project, bidders);
                }
                embedReviewReport(ppt, slides, reviewReport);
                ppt.write(output);
                return output.toByteArray();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("生成PPT失败：" + e.getMessage());
        }
    }

    /**
     * 将评审报告作为 OLE 对象写入 PPT，使下载后的 PPT 不依赖系统地址也可双击打开附件。
     */
    private void embedReviewReport(XMLSlideShow ppt, List<XSLFSlide> slides, Map<String, Object> report) throws Exception {
        if (report == null || report.get("reviewReportPath") == null || slides.size() < 2) return;
        Path path = Path.of(String.valueOf(report.get("reviewReportPath"))).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) return;

        XSLFTextShape attachmentLabel = null;
        for (XSLFShape shape : slides.get(1).getShapes()) {
            if (shape instanceof XSLFTextShape text && compact(text.getText()).contains("附件：评审报告")) {
                attachmentLabel = text;
                break;
            }
        }
        if (attachmentLabel == null) return;

        String originalName = String.valueOf(report.getOrDefault("reviewReportName", path.getFileName().toString()));
        String displayName = cleanAttachmentName(originalName);
        setTextPreservingFirstRun(attachmentLabel, "附件：");

        java.awt.geom.Rectangle2D labelAnchor = attachmentLabel.getAnchor();
        attachmentLabel.setAnchor(new java.awt.geom.Rectangle2D.Double(
                labelAnchor.getX(), labelAnchor.getY(), Math.min(labelAnchor.getWidth(), 90), labelAnchor.getHeight()));
        java.awt.geom.Rectangle2D objectAnchor = new java.awt.geom.Rectangle2D.Double(
                labelAnchor.getX() + 92, labelAnchor.getY() - 28,
                Math.max(180, labelAnchor.getWidth() - 92), Math.max(84, labelAnchor.getHeight() + 40));
        // 模拟 Office/WPS “显示为图标”的视觉效果：图标下方显示上传文件名，双击对象打开内嵌附件。
        XSLFPictureData icon = ppt.addPicture(attachmentPreview(displayName), PictureData.PictureType.PNG);
        XSLFObjectShape ole = slides.get(1).createOleShape(icon);
        ole.setAnchor(objectAnchor);
        byte[] documentBytes = Files.readAllBytes(path);
        try (POIFSFileSystem poifs = new POIFSFileSystem();
             ByteArrayOutputStream nativeData = new ByteArrayOutputStream()) {
            String nativeDisplayName = oleNativeAnsi(displayName);
            // Ole10Native 的文件名字段是老式 ANSI 结构，不能直接写 Java Unicode 中文。
            // 这里按中文 Windows 的 GBK 字节写入，WPS/Office 双击打开时显示上传文件中文全称。
            writeOle10Native(nativeData, nativeDisplayName, documentBytes);
            org.apache.poi.poifs.filesystem.Ole10Native.createOleMarkerEntry(poifs);
            poifs.getRoot().createDocument(org.apache.poi.poifs.filesystem.Ole10Native.OLE10_NATIVE,
                    new ByteArrayInputStream(nativeData.toByteArray()));
            try (OutputStream stream = ole.updateObjectData(null, PACKAGE_META_DATA)) {
                poifs.writeFilesystem(stream);
            }
        }
        ole.getCTOleObject().setName(displayName);
        ole.getCTOleObject().setShowAsIcon(true);
        var objectProps = ((CTGraphicalObjectFrame) ole.getXmlObject()).getNvGraphicFramePr().getCNvPr();
        objectProps.setName(displayName);
        objectProps.setTitle(displayName);
        objectProps.setDescr(displayName);
    }

    private byte[] attachmentPreview(String filename) throws java.io.IOException {
        int width = 360;
        int height = 170;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawFileIcon(g, filename, width / 2 - 22, 8);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 26));
            FontMetrics metrics = g.getFontMetrics();
            List<String> lines = wrapText(filename, metrics, width - 12, 2);
            int y = 102;
            for (String line : lines) {
                int x = Math.max(0, (width - metrics.stringWidth(line)) / 2);
                g.drawString(line, x, y);
                y += metrics.getHeight();
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    private void drawFileIcon(Graphics2D g, String filename, int x, int y) {
        String suffix = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) suffix = filename.substring(dot + 1).toLowerCase();
        Color accent = suffix.matches("docx?|wps") ? new Color(43, 101, 210)
                : suffix.matches("xlsx?|et") ? new Color(35, 148, 87)
                : suffix.matches("pdf") ? new Color(207, 54, 46) : new Color(128, 128, 128);
        g.setColor(new Color(255, 255, 255, 235));
        g.fillRoundRect(x, y, 44, 58, 5, 5);
        g.setColor(new Color(150, 150, 150));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, 44, 58, 5, 5);
        int[] xs = {x + 31, x + 44, x + 44};
        int[] ys = {y, y + 13, y};
        g.setColor(new Color(232, 232, 232));
        g.fillPolygon(xs, ys, 3);
        g.setColor(new Color(150, 150, 150));
        g.drawLine(x + 31, y, x + 44, y + 13);
        g.setColor(accent);
        g.fillRoundRect(x - 6, y + 34, 28, 24, 4, 4);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 17));
        String mark = suffix.matches("docx?|wps") ? "W" : suffix.matches("xlsx?|et") ? "X" : suffix.matches("pdf") ? "P" : "F";
        g.drawString(mark, x + 1, y + 52);
    }

    private List<String> wrapText(String text, FontMetrics metrics, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String remaining = text;
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            int take = remaining.length();
            while (take > 1 && metrics.stringWidth(remaining.substring(0, take)) > maxWidth) take--;
            String line = remaining.substring(0, take);
            remaining = remaining.substring(take);
            if (!remaining.isEmpty() && lines.size() == maxLines - 1) {
                while (line.length() > 1 && metrics.stringWidth(line + "...") > maxWidth) {
                    line = line.substring(0, line.length() - 1);
                }
                line += "...";
                remaining = "";
            }
            lines.add(line);
        }
        return lines;
    }

    private void fillSlide(XSLFSlide slide, int slideIndex, Map<String, String> values,
                           Map<String, Object> project, List<Map<String, Object>> bidders) {
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTable table) {
                boolean marker = table.getRows().stream().flatMap(r -> r.getCells().stream())
                        .anyMatch(c -> c.getText().contains("{{#bidders}}"));
                if (marker || (slideIndex == 1 && looksLikeBidderTable(table))) renderBidders(table, bidders);
                else for (XSLFTableRow row : table.getRows())
                    for (XSLFTableCell cell : row.getCells()) replaceTokens(cell, values);
            } else if (shape instanceof XSLFTextShape text) {
                replaceTokens(text, values);
                applyLegacyTemplateCompatibility(text, slideIndex, project);
            }
        }
    }

    private Map<String, String> values(Map<String, Object> p) {
        Map<String, String> v = new LinkedHashMap<>();
        p.forEach((key, value) -> v.put(key, format(value)));
        v.put("reportDate", format(p.get("reportYear")) + "年" + format(p.get("reportMonth")) + "月");
        v.put("publicityText", truthy(p.get("publicityEnabled"))
                ? format(p.get("publicityMethod")) + "方式进行结果公示，公示网址：" + format(p.get("publicityWebsite"))
                : "结果不公示");
        v.put("decisionQuestion", "是否同意与" + format(p.get("cooperationCompany")) + "合作，进行"
                + format(p.get("expansionProject")) + "项目的商务拓展？");
        return v;
    }

    private void replaceTokens(XSLFTextShape shape, Map<String, String> values) {
        String original = shape.getText();
        if (original == null || !original.contains("{{")) return;
        Matcher matcher = TOKEN.matcher(original);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) matcher.appendReplacement(result,
                Matcher.quoteReplacement(values.getOrDefault(matcher.group(1), "")));
        matcher.appendTail(result);
        setTextPreservingFirstRun(shape, result.toString());
    }

    private void applyLegacyTemplateCompatibility(XSLFTextShape shape, int page, Map<String, Object> p) {
        String text = compact(shape.getText());
        if (page == 0 && text.contains("政企客户") && text.matches(".*20\\d{2}年\\d{1,2}月.*")) {
            setTextPreservingFirstRun(shape, format(p.get("departmentName")) + "\n"
                    + format(p.get("reportYear")) + "年" + format(p.get("reportMonth")) + "月");
        } else if (page == 0 && text.contains("关于") && text.contains("甄选结果的汇报")) {
            setTextPreservingFirstRun(shape, "关于" + format(p.get("projectName")) + "项目\n甄选结果的汇报");
        } else if (page == 1 && text.contains("项目名称及基本信息")) {
            fillLegacyProjectSummary(shape, p);
        } else if (page == 1 && text.contains("评审委员会推荐意见")) {
            replaceLegacyParagraph(shape, "中选人", "中选人：" + display(p.get("selectedCompany")));
            replaceLegacyParagraph(shape, "候选人", "候选人：" + display(p.get("candidateCompany")));
        } else if (page == 2 && text.contains("根据甄选方案") && text.contains("结果公示")) {
            String publicity = truthy(p.get("publicityEnabled"))
                    ? "根据甄选方案，通过" + display(p.get("publicityMethod")) + "（"
                        + display(p.get("publicityWebsite")) + "）进行结果公示"
                    : "根据甄选方案，结果不公示";
            replaceLegacyParagraph(shape, "根据甄选方案", publicity);
            List<XSLFTextParagraph> paragraphs = shape.getTextParagraphs();
            if (paragraphs.size() > 3) setParagraphTextPreservingStyle(paragraphs.get(3),
                    display(p.get("executionMethod")));
            if (paragraphs.size() > 4) setParagraphTextPreservingStyle(paragraphs.get(4), "");
            if (paragraphs.size() > 5) setParagraphTextPreservingStyle(paragraphs.get(5), "");
        } else if (page == 3 && text.contains("是否同意与") && text.contains("商务拓展")) {
            setTextPreservingFirstRun(shape, "是否同意与" + format(p.get("cooperationCompany"))
                    + "合作，进行" + format(p.get("expansionProject")) + "项目的商务拓展？");
        }
    }

    private void fillLegacyProjectSummary(XSLFTextShape shape, Map<String, Object> p) {
        replaceLegacyParagraph(shape, "商机编号",
                display(p.get("projectName")) + "。（商机编号：" + display(p.get("opportunityNo")) + "）");
        replaceLegacyParagraph(shape, "合作服务部分上限金额",
                "甄选合作服务部分上限金额：" + moneyOrDash(p.get("serviceLimitIncTax")) + "（含税），"
                        + moneyOrDash(p.get("serviceLimitExTax")) + "（不含税），税率"
                        + percentOrDash(p.get("serviceTaxRate")) + "。");
        replaceLegacyParagraph(shape, "受托代销应付账款",
                "甄选受托代销部分：受托代销应付账款" + moneyOrDash(p.get("resaleBudgetIncTax")) + "（含税），"
                        + moneyOrDash(p.get("resaleBudgetExTax")) + "（不含税），税率"
                        + percentOrDash(p.get("resaleTaxRate")) + "；手续费计划最低金额"
                        + moneyOrDash(p.get("feeMinIncTax")) + "（含税），"
                        + moneyOrDash(p.get("feeMinExTax")) + "（不含税），税率"
                        + percentOrDash(p.get("feeTaxRate")) + "。");
    }

    private void replaceLegacyParagraph(XSLFTextShape shape, String marker, String value) {
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            if (paragraph.getText().contains(marker)) {
                setParagraphTextPreservingStyle(paragraph, value);
                return;
            }
        }
    }

    private void setParagraphTextPreservingStyle(XSLFTextParagraph paragraph, String value) {
        List<XSLFTextRun> runs = paragraph.getTextRuns();
        if (runs.isEmpty()) {
            paragraph.addNewTextRun().setText(value);
            return;
        }
        runs.get(0).setText(value == null ? "" : value);
        for (int i = 1; i < runs.size(); i++) runs.get(i).setText("");
    }

    private boolean looksLikeBidderTable(XSLFTable table) {
        String all = table.getRows().stream().flatMap(r -> r.getCells().stream())
                .map(XSLFTableCell::getText).reduce("", (a, b) -> a + b);
        return all.contains("投标人") && all.contains("综合排名");
    }

    private void renderBidders(XSLFTable table, List<Map<String, Object>> bidders) {
        int templateRow = findBidderRow(table);
        if (templateRow < 0) throw new BusinessException("模板投标人表格缺少{{#bidders}}或示例数据行");
        int existingRows = table.getNumberOfRows() - templateRow;
        while (existingRows < bidders.size()) {
            XSLFTableRow source = table.getRows().get(templateRow);
            XSLFTableRow added = table.addRow();
            added.setHeight(source.getHeight());
            for (int c = 0; c < source.getCells().size(); c++) {
                XSLFTableCell cell = added.addCell();
                copyCellStyle(source.getCells().get(c), cell);
            }
            existingRows++;
        }
        for (int i = 0; i < existingRows; i++) {
            XSLFTableRow row = table.getRows().get(templateRow + i);
            if (i < bidders.size()) fillBidderRow(row, bidders.get(i), i + 1);
            else row.getCells().forEach(cell -> setTextPreservingFirstRun(cell, ""));
        }
    }

    private int findBidderRow(XSLFTable table) {
        for (int i = 0; i < table.getRows().size(); i++) {
            XSLFTableRow row = table.getRows().get(i);
            String all = row.getCells().stream().map(XSLFTableCell::getText).reduce("", (a, b) -> a + b);
            if (all.contains("{{#bidders}}")) return i;
            if (!row.getCells().isEmpty() && row.getCells().get(0).getText().trim().equals("1")) return i;
        }
        return -1;
    }

    private void fillBidderRow(XSLFTableRow row, Map<String, Object> b, int index) {
        List<String> data = List.of(
                String.valueOf(index), format(b.get("bidderName")),
                money(b.get("serviceExTax")), percent(b.get("serviceTaxRate")), money(b.get("serviceIncTax")),
                money(b.get("resaleExTax")), percent(b.get("resaleTaxRate")), money(b.get("resaleIncTax")),
                score(b.get("priceScore")), score(b.get("businessScore")),
                score(b.get("totalScore")), format(b.get("ranking"))
        );
        for (int i = 0; i < row.getCells().size(); i++) {
            setTextPreservingFirstRun(row.getCells().get(i), i < data.size() ? data.get(i) : "");
        }
    }

    private void copyCellStyle(XSLFTableCell source, XSLFTableCell target) {
        target.setFillColor(source.getFillColor());
        target.setVerticalAlignment(source.getVerticalAlignment());
        target.setTextDirection(source.getTextDirection());
        if (!source.getTextParagraphs().isEmpty()) {
            XSLFTextParagraph sp = source.getTextParagraphs().get(0);
            XSLFTextParagraph tp = target.addNewTextParagraph();
            tp.setTextAlign(sp.getTextAlign());
            if (!sp.getTextRuns().isEmpty()) {
                XSLFTextRun sr = sp.getTextRuns().get(0);
                XSLFTextRun tr = tp.addNewTextRun();
                tr.setFontFamily(sr.getFontFamily());
                tr.setFontSize(sr.getFontSize());
                tr.setBold(sr.isBold());
                tr.setFontColor(sr.getFontColor());
            }
        }
    }

    private void setTextPreservingFirstRun(XSLFTextShape shape, String value) {
        List<XSLFTextRun> runs = new ArrayList<>();
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) runs.addAll(paragraph.getTextRuns());
        if (runs.isEmpty()) {
            shape.setText(value);
            return;
        }
        XSLFTextRun writable = runs.stream().filter(run -> !"\n".equals(run.getRawText())).findFirst().orElse(null);
        if (writable == null) {
            shape.setText(value);
            return;
        }
        writable.setText(value == null ? "" : value);
        for (XSLFTextRun run : runs) {
            if (run != writable && !"\n".equals(run.getRawText())) run.setText("");
        }
    }

    private String compact(String text) { return text == null ? "" : text.replaceAll("\\s+", ""); }
    private String format(Object value) { return value == null ? "" : String.valueOf(value); }
    private String cleanAttachmentName(String value) {
        String text = value == null ? "" : value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return text.isEmpty() ? "附件文件" : text;
    }
    private String oleNativeAnsi(String value) {
        return new String(value.getBytes(GBK), StandardCharsets.ISO_8859_1);
    }
    private void writeOle10Native(OutputStream output, String ansiFilename, byte[] data) throws java.io.IOException {
        byte[] name = ansiFilename.getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        writeShortLE(payload, 2);
        payload.write(name);
        payload.write(0);
        payload.write(name);
        payload.write(0);
        writeShortLE(payload, 0);
        writeShortLE(payload, 3);
        writeIntLE(payload, name.length + 1);
        payload.write(name);
        payload.write(0);
        writeIntLE(payload, data.length);
        payload.write(data);
        writeShortLE(payload, 0);

        writeIntLE(output, payload.size());
        payload.writeTo(output);
    }
    private void writeShortLE(OutputStream output, int value) throws java.io.IOException {
        output.write(value & 0xff);
        output.write((value >>> 8) & 0xff);
    }
    private void writeIntLE(OutputStream output, int value) throws java.io.IOException {
        output.write(value & 0xff);
        output.write((value >>> 8) & 0xff);
        output.write((value >>> 16) & 0xff);
        output.write((value >>> 24) & 0xff);
    }
    private String display(Object value) {
        String text = format(value).trim();
        return text.isEmpty() ? "—" : text;
    }
    private String moneyOrDash(Object value) { return value == null ? "—" : money(value); }
    private String percentOrDash(Object value) { return value == null ? "—" : percent(value); }
    private String money(Object value) {
        if (value == null) return "";
        return new DecimalFormat("#,##0.00").format(new BigDecimal(String.valueOf(value)));
    }
    private String percent(Object value) {
        if (value == null) return "";
        return new BigDecimal(String.valueOf(value)).stripTrailingZeros().toPlainString() + "%";
    }
    private String score(Object value) {
        if (value == null) return "";
        return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }
    private boolean truthy(Object value) {
        return value instanceof Boolean b ? b : value instanceof Number n && n.intValue() != 0;
    }
}
