package com.scaffold.system.service;

import com.scaffold.common.exception.BusinessException;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFRelation;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;

@Service
public class InitiationPptService {
    private static final Logger LOG = LoggerFactory.getLogger(InitiationPptService.class);
    private static final Pattern TOKEN = Pattern.compile("\\{\\{([a-zA-Z][\\w.]*)?(?:\\|([a-z]+))?}}");
    private static final ConcurrentHashMap<Long, Boolean> GENERATING = new ConcurrentHashMap<>();
    private static final String PACKAGE_RELATIONSHIP =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/package";
    private static final String BENEFIT_WORKBOOK_RESOURCE =
            "/templates/initiation-benefit-workbook.xlsx";
    private static final String FINANCE_DETAIL_WORKBOOK_RESOURCE =
            "/templates/initiation-finance-detail-workbook.xlsx";
    private static final String[] GROUP_ROWS = {
            "\u4fe1\u606f\u5316\u6536\u5165\uff08\u4e07\u5143\uff09",
            "\u5176\u4e2d\uff1a\u901a\u4fe1\u670d\u52a1\u6536\u5165",
            "\u5176\u4e2d\uff1a\u7b97\u529b\u670d\u52a1\u6536\u5165",
            "\u5176\u4e2d\uff1a\u667a\u80fd\u670d\u52a1\u6536\u5165",
            "\u6210\u5458\u6536\u5165\uff08\u4e07\u5143\uff09",
            "\u6210\u5458\u6570\u91cf\uff08\u6237\uff09",
            "\u6210\u5458\u6570\u91cf\u4efd\u989d\uff08%\uff09",
            "V\u7f51\u6210\u5458\u6570\uff08\u6237\uff09"
    };
    private static final String[] TREE_ROWS = {
            "\u4fe1\u606f\u5316\u6536\u5165\uff08\u4e07\u5143\uff09",
            "\u8282\u70b9\u7eb3\u7ba1\u7387\uff08%\uff09",
            "\u4e13\u7ebf\u6e17\u900f\u7387\uff08%\uff09",
            "\u79fb\u52a8\u4e91\u6e17\u900f\u7387\uff08%\uff09",
            "\u6210\u5458\u6536\u5165\uff08\u4e07\u5143\uff09",
            "\u6210\u5458\u6570\u91cf\uff08\u6237\uff09",
            "\u6210\u5458\u6570\u91cf\u4efd\u989d\uff08%\uff09",
            "V\u7f51\u6210\u5458\u6570\uff08\u6237\uff09"
    };

    private final InitiationProjectService projects;
    private final InitiationTemplateService templates;

    public InitiationPptService(InitiationProjectService projects, InitiationTemplateService templates) {
        this.projects = projects;
        this.templates = templates;
    }

    public byte[] generate(Long projectId, Long templateId) {
        if (GENERATING.putIfAbsent(projectId, Boolean.TRUE) != null) {
            throw new BusinessException("PPT正在生成中，请稍后再试");
        }
        try {
            Map<String, Object> project = normalizeFinanceDetails(projects.detail(projectId));
            Map<String, Object> template = templates.resolve(templateId);
            try (XMLSlideShow ppt = new XMLSlideShow(Files.newInputStream(templates.path(template)));
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                Map<String, String> values = flatten(project);
                List<XSLFSlide> slides = ppt.getSlides();
                int lastSourceSlide = 31;
                if (slides.size() < lastSourceSlide) {
                    throw new IllegalStateException("PPT模板页数不足，预期至少"
                            + lastSourceSlide + "页，实际" + slides.size() + "页");
                }
                for (int i = 0; i < lastSourceSlide; i++) {
                    replaceTerminology(slides.get(i).getShapes());
                    fill(ppt, slides.get(i), i + 1, project, values);
                }
                while (ppt.getSlides().size() > lastSourceSlide) {
                    ppt.removeSlide(ppt.getSlides().size() - 1);
                }
                boolean dynamicAppendices = sectionMap(project).containsKey("economicBenefitAppendices");
                List<XSLFSlide> modeSlides = new ArrayList<>();
                if (dynamicAppendices) {
                    XSLFSlide source = ppt.getSlides().get(11);
                    for (Map<String, Object> appendix : sectionRows(project, "economicBenefitAppendices")) {
                        XSLFSlide copy = ppt.createSlide(source.getSlideLayout());
                        copy.importContent(source);
                        if (copy.getNotes() != null) copy.removeNotes(ppt.getNotesMaster());
                        removeCopiedTemplateTags(copy);
                        Map<String, Object> modeProject = new LinkedHashMap<>(project);
                        Map<String, Object> modeSections = new LinkedHashMap<>(sectionMap(project));
                        Map<String, Object> currentModeValues = mapOf(
                                mapOf(sectionMap(project).get("modeEconomicBenefitValues"))
                                        .get(n(appendix.get("key"), "")));
                        Map<String, Object> outputValues = currentModeValues.isEmpty()
                                ? mapOf(appendix.get("values")) : currentModeValues;
                        if (outputValues.isEmpty()) {
                            throw new IllegalStateException("模式“" + n(appendix.get("title"), "")
                                    + "”缺少测算结果，请填写并保存对应模式测算表后再生成");
                        }
                        modeSections.put("economicBenefitValues", outputValues);
                        modeProject.put("sections", modeSections);
                        if (fillModeAppendix(copy.getShapes(), modeProject, n(appendix.get("title"), "")) == 0) {
                            throw new IllegalStateException("模板的经济效益附表未匹配到可填写表格，请检查模板结构");
                        }
                        modeSlides.add(copy);
                    }
                }
                boolean governmentFundProject = isGovernmentFundProject(project);
                // Appendix pages are optional. Remove from the end so source indices stay stable.
                if (!sectionFlag(project, "appraisalEnabled")) ppt.removeSlide(30);
                if (!sectionFlag(project, "partnerAppendixEnabled")) ppt.removeSlide(29);
                if (!sectionFlag(project, "existingBusinessEnabled")) ppt.removeSlide(28);
                if (!sectionFlag(project, "preDecisionOpinionsEnabled")) ppt.removeSlide(27);
                if (!sectionFlag(project, "maintenanceEnabled")) ppt.removeSlide(26);
                // The template contains separate government and enterprise fund-assurance pages.
                ppt.removeSlide(governmentFundProject ? 19 : 18);
                if (!isAdvancePaymentReviewEnabled(project)) {
                    ppt.removeSlide(14);
                }
                if (dynamicAppendices || !isCooperationEconomicBenefitEnabled(project)) {
                    ppt.removeSlide(13);
                }
                if (dynamicAppendices || !isInvestmentEconomicBenefitEnabled(project)) {
                    ppt.removeSlide(12);
                }
                // Keep the overall calculation paired with the standard base-data table.
                if (!isPreDecisionApproved(project)) {
                    ppt.removeSlide(10);
                }
                if (!isIdcEnabled(project)) {
                    ppt.removeSlide(7);
                }
                if (!isProcurementComparisonEnabled(project)) {
                    ppt.removeSlide(6);
                }
                int expectedSlideCount = lastSourceSlide - 1
                        - (sectionFlag(project, "maintenanceEnabled") ? 0 : 1)
                        - (sectionFlag(project, "preDecisionOpinionsEnabled") ? 0 : 1)
                        - (sectionFlag(project, "existingBusinessEnabled") ? 0 : 1)
                        - (sectionFlag(project, "partnerAppendixEnabled") ? 0 : 1)
                        - (sectionFlag(project, "appraisalEnabled") ? 0 : 1)
                        - (isPreDecisionApproved(project) ? 0 : 1)
                        - (isProcurementComparisonEnabled(project) ? 0 : 1)
                        - (isIdcEnabled(project) ? 0 : 1)
                        - (isInvestmentEconomicBenefitEnabled(project) ? 0 : 1)
                        - (isCooperationEconomicBenefitEnabled(project) ? 0 : 1)
                        - (isAdvancePaymentReviewEnabled(project) ? 0 : 1);
                if (dynamicAppendices) {
                    expectedSlideCount += modeSlides.size()
                            - (isInvestmentEconomicBenefitEnabled(project) ? 1 : 0)
                            - (isCooperationEconomicBenefitEnabled(project) ? 1 : 0);
                    int insertion = 12 - (isPreDecisionApproved(project) ? 0 : 1)
                            - (isIdcEnabled(project) ? 0 : 1)
                            - (isProcurementComparisonEnabled(project) ? 0 : 1);
                    for (XSLFSlide modeSlide : modeSlides) ppt.setSlideOrder(modeSlide, insertion++);
                }
                removeInvalidPresentationNotesRelationships(ppt);
                removeEmptySlideRelationshipReferences(ppt);
                normalizeShapeIds(ppt);
                replaceEmbeddedWorkbooks(ppt, project);
                ppt.write(out);
                byte[] generated = out.toByteArray();
                validateGeneratedPresentation(generated, expectedSlideCount);
                return generated;
            }
        } catch (Exception e) {
            LOG.error("立项PPT生成失败，projectId={}，templateId={}", projectId, templateId, e);
            throw new BusinessException("立项PPT生成失败：" + rootCauseMessage(e));
        } finally {
            GENERATING.remove(projectId);
        }
    }

    private void removeCopiedTemplateTags(XSLFSlide slide) {
        // POI importContent copies p:tags/@r:id but not its tag relationship.
        // On a new slide rId1 is the layout: retaining a copied tag reference
        // produces invalid OOXML and Office may discard the containing table.
        // These are template metadata, not visible content or financial data.
        XmlObject[] lists = slide.getXmlObject().selectPath(
                "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main' "
                        + ".//p:custDataLst");
        for (XmlObject list : lists) {
            try (XmlCursor cursor = list.newCursor()) {
                cursor.removeXml();
            }
        }
    }

    private int fillModeAppendix(List<XSLFShape> shapes, Map<String, Object> project, String title) {
        int filled = 0;
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFGroupShape group) {
                filled += fillModeAppendix(group.getShapes(), project, title);
            } else if (shape instanceof XSLFTable table) {
                if (fillEconomicBenefitTable(table, project, "economicBenefitValues")) filled++;
            } else if (shape instanceof XSLFTextShape text) {
                if (text.getText().contains("收支分析")) set(text, "附、收支分析【" + title + "】");
                else if (text.getText().contains("经济效益评估表如下")) set(text, title + "经济效益评估表如下：");
            }
        }
        return filled;
    }

    private String rootCauseMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank()
                ? cause.getClass().getSimpleName()
                : message;
    }

    private void normalizeShapeIds(XMLSlideShow ppt) {
        QName idAttribute = new QName("", "id");
        for (XSLFSlide slide : ppt.getSlides()) {
            XmlObject[] properties = slide.getXmlObject().selectPath(
                    "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main' "
                            + ".//p:cNvPr");
            long id = 1;
            for (XmlObject property : properties) {
                try (XmlCursor cursor = property.newCursor()) {
                    cursor.setAttributeText(idAttribute, Long.toString(id++));
                }
            }
        }
        validateShapeIds(ppt);
    }

    private void validateGeneratedPresentation(byte[] generated, int expectedSlideCount) {
        try (XMLSlideShow validation = new XMLSlideShow(new ByteArrayInputStream(generated))) {
            if (validation.getPackagePart()
                    .getRelationshipsByType(XSLFRelation.NOTES.getRelation()).size() > 0) {
                throw new IllegalStateException(
                        "PPT根关系中存在非法的备注页引用，PowerPoint将无法直接打开");
            }
            int actualSlideCount = validation.getSlides().size();
            if (actualSlideCount != expectedSlideCount) {
                throw new IllegalStateException(
                        "PPT生成后页数不一致，预期" + expectedSlideCount + "页，实际" + actualSlideCount + "页");
            }
            for (int i = 0; i < validation.getSlides().size(); i++) {
                validateTextBodies(validation.getSlides().get(i).getShapes(), i + 1);
            }
        } catch (Exception e) {
            throw new IllegalStateException("生成的PPTX结构校验失败", e);
        }
    }

    private void validateTextBodies(List<XSLFShape> shapes, int slideNumber) {
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFTextShape textShape
                    && textShape.getTextParagraphs().isEmpty()) {
                throw new IllegalStateException(
                        "PPT第" + slideNumber + "页存在没有段落的文本框：" + shape.getShapeName());
            }
            if (shape instanceof XSLFGroupShape groupShape) {
                validateTextBodies(groupShape.getShapes(), slideNumber);
            }
        }
    }

    private void validateShapeIds(XMLSlideShow ppt) {
        QName idAttribute = new QName("", "id");
        for (int slideIndex = 0; slideIndex < ppt.getSlides().size(); slideIndex++) {
            XmlObject[] properties = ppt.getSlides().get(slideIndex).getXmlObject().selectPath(
                    "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main' "
                            + ".//p:cNvPr");
            java.util.Set<String> ids = new java.util.HashSet<>();
            for (XmlObject property : properties) {
                String id;
                try (XmlCursor cursor = property.newCursor()) {
                    id = cursor.getAttributeText(idAttribute);
                }
                if (id == null || "0".equals(id) || !ids.add(id)) {
                    throw new IllegalStateException(
                            "Invalid or duplicate shape id on slide " + (slideIndex + 1) + ": " + id);
                }
            }
        }
    }

    private void fill(XMLSlideShow ppt, XSLFSlide slide, int page, Map<String, Object> project, Map<String, String> values) {
        if (page == 7) {
            boolean publicProcurement = !"DIRECT".equalsIgnoreCase(
                    n(sectionMap(project).get("procurementType"), "PUBLIC"));
            XSLFShape publicDescription = null;
            XSLFShape directDescription = null;
            XSLFTable publicTable = null;
            XSLFTable directTable = null;
            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTable table) {
                    int rows = table.getRows().size();
                    int columns = table.getRows().isEmpty() ? 0 : table.getRows().get(0).getCells().size();
                    if (rows == 5 && columns == 10) publicTable = table;
                    if (rows == 3 && columns == 6) directTable = table;
                } else if (shape instanceof XSLFTextShape textShape) {
                    String shapeText = n(textShape.getText(), "");
                    if (shapeText.contains("若后期为公开采购")) publicDescription = shape;
                    if (shapeText.contains("若后期直接采购场景")) directDescription = shape;
                }
            }
            Rectangle2D publicDescriptionAnchor = publicDescription == null ? null : publicDescription.getAnchor();
            Rectangle2D publicTableAnchor = publicTable == null ? null : publicTable.getAnchor();
            for (XSLFShape shape : new ArrayList<>(slide.getShapes())) {
                boolean remove = false;
                if (shape instanceof XSLFTable table) {
                    int rows = table.getRows().size();
                    int columns = table.getRows().isEmpty() ? 0 : table.getRows().get(0).getCells().size();
                    remove = publicProcurement ? rows == 3 && columns == 6 : rows == 5 && columns == 10;
                } else if (shape instanceof XSLFTextShape textShape) {
                    String shapeText = n(textShape.getText(), "");
                    remove = publicProcurement
                            ? shapeText.contains("若后期直接采购场景") || shapeText.contains("一采非标报备合理性评审")
                            : shapeText.contains("若后期为公开采购");
                    remove = remove || shapeText.contains("若不涉及此页可删除");
                }
                if (remove) slide.removeShape(shape);
            }
            // The latest template places the direct-purchase block below the public-purchase block.
            // When DIRECT is selected, move the retained block into the upper template region.
            if (!publicProcurement) {
                if (directDescription instanceof XSLFTextShape textShape && publicDescriptionAnchor != null) {
                    textShape.setAnchor(publicDescriptionAnchor);
                }
                if (directTable != null && publicTableAnchor != null) {
                    directTable.setAnchor(publicTableAnchor);
                }
            }
        }
        if (page == 11) {
            for (XSLFShape shape : new ArrayList<>(slide.getShapes())) {
                String name = shape.getShapeName();
                if (name != null && (name.startsWith("\u4e0a\u7bad\u5934") || name.startsWith("Minus"))) {
                    slide.removeShape(shape);
                }
            }
        }
        int[] pageThreeTableIndex = {0};
        fillShapes(slide.getShapes(), page, project, values, pageThreeTableIndex);
        if (page == 18) reflowClientPerformancePage(slide, project);
        if (page == 22) reflowPaymentCashFlowPage(slide, project);
        if (page == 17 || page == 18 || page == 22 || page == 25 || page == 30) {
            enableTextShrinkToFit(slide.getShapes());
        }
        if (page == 4) {
            fillArchitectureImage(ppt, slide, project);
        }
        if (page == 15) {
            fillAdvancePaymentReviewImages(ppt, slide, project);
        }
    }

    @SuppressWarnings("unchecked")
    private void fillArchitectureImage(XMLSlideShow ppt, XSLFSlide slide, Map<String, Object> project) {
        Object raw = project.get("attachments");
        if (!(raw instanceof List<?> attachments)) {
            return;
        }
        Map<String, Object> selected = null;
        for (Object value : attachments) {
            if (value instanceof Map<?, ?> attachment
                    && "ARCHITECTURE_DIAGRAM".equals(String.valueOf(attachment.get("attachmentType")))) {
                selected = (Map<String, Object>) attachment;
            }
        }
        if (selected == null || selected.get("storagePath") == null) {
            return;
        }
        try {
            Path imagePath = Path.of(String.valueOf(selected.get("storagePath")));
            byte[] bytes = Files.readAllBytes(imagePath);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new BusinessException("\u7cfb\u7edf\u67b6\u6784\u56fe\u683c\u5f0f\u4e0d\u652f\u6301");
            }
            String filename = String.valueOf(selected.getOrDefault("originalFilename", imagePath.getFileName()));
            PictureData.PictureType pictureType = filename.toLowerCase().endsWith(".png")
                    ? PictureData.PictureType.PNG : PictureData.PictureType.JPEG;
            XSLFPictureData pictureData = ppt.addPicture(bytes, pictureType);
            XSLFPictureShape picture = slide.createPicture(pictureData);
            Rectangle2D frame = contain(image.getWidth(), image.getHeight(), 70, 205, 820, 300);
            picture.setAnchor(frame);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("\u7cfb\u7edf\u67b6\u6784\u56fe\u5199\u5165PPT\u5931\u8d25\uff1a" + e.getMessage());
        }
    }

    private void fillAdvancePaymentReviewImages(
            XMLSlideShow ppt,
            XSLFSlide slide,
            Map<String, Object> project) {
        addAttachmentImage(ppt, slide, project, "ADVANCE_REVIEW_STRATEGY_IMAGE",
                28, 254, 296, 234, "\u6218\u7565\u5361\u4f4d\u8bc4\u5ba1\u56fe\u7247");
        addAttachmentImage(ppt, slide, project, "ADVANCE_REVIEW_PREFERRED_ORDER_IMAGE",
                339, 254, 284, 234, "\u96c6\u56e2\u4f18\u5355\u8bc4\u5ba1\u56fe\u7247");
        addAttachmentImage(ppt, slide, project, "ADVANCE_REVIEW_CAPABILITY_IMAGE",
                639, 254, 284, 234, "\u80fd\u529b\u6c89\u6dc0\u8bc4\u5ba1\u56fe\u7247");
    }

    @SuppressWarnings("unchecked")
    private void addAttachmentImage(
            XMLSlideShow ppt,
            XSLFSlide slide,
            Map<String, Object> project,
            String attachmentType,
            double x,
            double y,
            double width,
            double height,
            String label) {
        Object raw = project.get("attachments");
        if (!(raw instanceof List<?> attachments)) {
            return;
        }
        Map<String, Object> selected = null;
        for (Object value : attachments) {
            if (value instanceof Map<?, ?> attachment
                    && attachmentType.equals(String.valueOf(attachment.get("attachmentType")))) {
                selected = (Map<String, Object>) attachment;
            }
        }
        if (selected == null || selected.get("storagePath") == null) {
            return;
        }
        try {
            Path imagePath = Path.of(String.valueOf(selected.get("storagePath")));
            byte[] bytes = Files.readAllBytes(imagePath);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new IllegalArgumentException("\u6587\u4ef6\u4e0d\u662f\u6709\u6548\u7684PNG/JPG\u56fe\u7247");
            }
            String filename = String.valueOf(selected.getOrDefault("originalFilename", imagePath.getFileName()));
            String lower = filename.toLowerCase();
            PictureData.PictureType pictureType = lower.endsWith(".png")
                    ? PictureData.PictureType.PNG : PictureData.PictureType.JPEG;
            XSLFPictureData pictureData = ppt.addPicture(bytes, pictureType);
            XSLFPictureShape picture = slide.createPicture(pictureData);
            picture.setAnchor(contain(image.getWidth(), image.getHeight(), x, y, width, height));
        } catch (Exception e) {
            throw new BusinessException(label + "\u5199\u5165PPT\u5931\u8d25\uff1a" + rootCauseMessage(e));
        }
    }

    private Rectangle2D contain(double imageWidth, double imageHeight,
                                double left, double top, double width, double height) {
        double scale = Math.min(width / imageWidth, height / imageHeight);
        double fittedWidth = imageWidth * scale;
        double fittedHeight = imageHeight * scale;
        return new Rectangle2D.Double(
                left + (width - fittedWidth) / 2,
                top + (height - fittedHeight) / 2,
                fittedWidth,
                fittedHeight
        );
    }

    private void fillShapes(List<XSLFShape> shapes, int page, Map<String, Object> project, Map<String, String> values, int[] pageThreeTableIndex) {
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFGroupShape groupShape) {
                fillShapes(groupShape.getShapes(), page, project, values, pageThreeTableIndex);
                continue;
            }
            if (shape instanceof XSLFTable table) {
                // Finance pages are separate template pages. Detect them by their
                // table semantics first so a newly uploaded template may move
                // total investment / total revenue without breaking the mapping.
                if (financeTableKind(table, "\u542b\u7a0e\u652f\u51fa")) {
                    fillFinanceTable(table, project, "costItems");
                    continue;
                }
                if (financeTableKind(table, "\u542b\u7a0e\u6536\u76ca")) {
                    fillFinanceTable(table, project, "incomeItems");
                    continue;
                }
                if (page == 3 && fillPageThreeTable(table, pageThreeTableIndex[0]++, project)) {
                    continue;
                }
                if (page == 5 && fillCapabilityTable(table, project)) {
                    continue;
                }
                if (page == 6 && fillFinanceTable(table, project, "costItems")) {
                    continue;
                }
                if (page == 7 && fillProcurementComparisonTable(table, project)) {
                    continue;
                }
                if (page == 8 && fillIdcTable(table, project)) {
                    continue;
                }
                if (page == 9 && fillFinanceTable(table, project, "incomeItems")) {
                    continue;
                }
                if (page == 10 && fillBenefitAnalysisTable(table, project)) {
                    continue;
                }
                if (page == 11 && fillPreDecisionTable(table, project)) {
                    continue;
                }
                if (page == 12 && fillEconomicBenefitTable(table, project, "economicBenefitValues")) {
                    continue;
                }
                if (page == 13 && fillEconomicBenefitTable(table, project, "investmentEconomicBenefitValues")) {
                    continue;
                }
                if (page == 14 && fillEconomicBenefitTable(table, project, "cooperationEconomicBenefitValues")) {
                    continue;
                }
                if (page == 17 && fillClientDueDiligenceTable(table, project)) {
                    continue;
                }
                if (page == 18 && fillClientPerformanceTable(table, project)) {
                    continue;
                }
                if ((page == 19 || page == 20) && fillFundScoreTable(table, project)) {
                    continue;
                }
                if (page == 21 && fillVendorRiskTable(table, project)) {
                    continue;
                }
                if (page == 22 && fillCashFlowTable(table, project)) {
                    continue;
                }
                if (page == 23 && fillRiskAssessmentTable(table, project)) {
                    continue;
                }
                if (page == 24 && fillThreeLineTable(table, project)) {
                    continue;
                }
                if (page == 25 && fillDeliveryTable(table, project)) {
                    continue;
                }
                if (page == 27 && fillMaintenanceTable(table, project)) continue;
                if (page == 29 && fillExistingBusinessTable(table, project)) continue;
                if (page == 30 && fillPartnerSelectionTable(table, project)) continue;
                if (page == 31 && fillAppraisalTable(table, project)) continue;
                replaceTable(table, values);
                continue;
            }
            if (shape instanceof XSLFTextShape textShape) {
                replaceText(textShape, values);
                applyLegacyMapping(textShape, page, project);
            }
        }
    }

    /** User-facing terminology changed in 2026; field keys stay stable for saved-record compatibility. */
    private void replaceTerminology(List<XSLFShape> shapes) {
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFGroupShape group) {
                replaceTerminology(group.getShapes());
            } else if (shape instanceof XSLFTable table) {
                for (XSLFTableRow row : table.getRows()) {
                    for (XSLFTableCell cell : row.getCells()) replaceTerminology(cell);
                }
            } else if (shape instanceof XSLFTextShape textShape) {
                replaceTerminology(textShape);
            }
        }
    }

    private void replaceTerminology(XSLFTextShape shape) {
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            String original = paragraph.getText() == null ? "" : paragraph.getText();
            if (!original.contains("预决策")) continue;
            boolean replaced = false;
            for (XSLFTextRun run : paragraph.getTextRuns()) {
                String text = run.getRawText();
                if (text != null && text.contains("预决策")) {
                    run.setText(text.replace("预决策", "招投标"));
                    replaced = true;
                }
            }
            if (!replaced) set(paragraph, original.replace("预决策", "招投标"));
        }
    }

    private void replaceText(XSLFTextShape shape, Map<String, String> values) {
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            Matcher matcher = TOKEN.matcher(paragraph.getText() == null ? "" : paragraph.getText());
            if (!matcher.find()) {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            do {
                String key = matcher.group(1);
                String value = values.getOrDefault(key, "");
                if ("money".equals(matcher.group(2)) && !value.isBlank()) {
                    value = money(value);
                }
                matcher.appendReplacement(builder, Matcher.quoteReplacement(value));
            } while (matcher.find());
            matcher.appendTail(builder);
            set(paragraph, builder.toString());
        }
    }

    private void replaceTable(XSLFTable table, Map<String, String> values) {
        for (XSLFTableRow row : table.getRows()) {
            for (XSLFTableCell cell : row.getCells()) {
                for (XSLFTextParagraph paragraph : cell.getTextParagraphs()) {
                    Matcher matcher = TOKEN.matcher(paragraph.getText() == null ? "" : paragraph.getText());
                    if (!matcher.find()) {
                        continue;
                    }
                    StringBuilder builder = new StringBuilder();
                    do {
                        String key = matcher.group(1);
                        String value = values.getOrDefault(key, "");
                        if ("money".equals(matcher.group(2)) && !value.isBlank()) {
                            value = money(value);
                        }
                        matcher.appendReplacement(builder, Matcher.quoteReplacement(value));
                    } while (matcher.find());
                    matcher.appendTail(builder);
                    set(paragraph, builder.toString());
                }
            }
        }
    }

    private void applyLegacyMapping(XSLFTextShape shape, int page, Map<String, Object> project) {
        String text = shape.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        // Do not couple the two independent finance pages to fixed slide indexes.
        // The latest template places total investment and total revenue on
        // separate pages and uploaded template revisions may renumber them.
        if (text.contains("\u603b\u6295\u5165\uff1a")) {
            replaceParagraphUnicode(shape, "\u603b\u6295\u5165\uff1a", buildInvestmentSummary(project));
            return;
        }
        if (text.contains("\u603b\u6536\u76ca\uff1a")) {
            replaceParagraphUnicode(shape, "\u603b\u6536\u76ca\uff1a",
                    money2(project.get("totalRevenueIncTax")) + "\u4e07\u5143");
            Map<String, Object> sections = sectionMap(project);
            replaceParagraphUnicode(shape, "\u95f4\u63a5\u6536\u76ca\uff1a",
                    n(sections.get("indirectRevenue"), ""));
            String agreementYears = n(project.get("agreementYears"), "");
            replaceParagraphUnicode(shape, "\u534f\u8bae\u671f\uff1a",
                    agreementYears.isBlank() ? "" : agreementYears + "\u5e74");
            replaceParagraphUnicode(shape, "\u5e26\u52a8\u5546\u673a\uff1a",
                    n(sections.get("drivenOpportunity"), ""));
            return;
        }
        if (page == 1) {
            fillCoverPage(shape, project, text);
            return;
        }
        if (page == 2) {
            fillOverviewPageUnicode(shape, project, text);
            return;
        }
        if (page == 3) {
            fillGroupTitle(shape, project, text);
            if (fillGroupPageUnicode(shape, project, text)) {
                return;
            }
            fillGroupPage(shape, project, text);
            return;
        }
        if (page == 4) {
            fillConstructionPage(shape, project, text);
            return;
        }
        if (page == 5) {
            fillCapabilityPage(shape, project, text);
            return;
        }
        if (page == 6) {
            String summary = buildInvestmentSummary(project);
            replaceParagraphUnicode(shape, "\u603b\u6295\u5165\uff1a",
                    summary);
            return;
        }
        if (page == 7) {
            Map<String, Object> sections = sectionMap(project);
            if (text.contains("\u82e5\u540e\u671f\u4e3a\u516c\u5f00\u91c7\u8d2d")) {
                set(shape, n(sections.get("procurementPublicDescription"), ""));
                forceBlack(shape);
            } else if (text.contains("\u82e5\u540e\u671f\u76f4\u63a5\u91c7\u8d2d")) {
                set(shape, n(sections.get("procurementDirectDescription"), ""));
                forceBlack(shape);
            } else if (text.contains("\u4e00\u91c7\u975e\u6807\u62a5\u5907\u5408\u7406\u6027\u8bc4\u5ba1")) {
                set(shape, n(sections.get("procurementAssessment"), ""));
                forceBlack(shape);
            } else if (text.contains("\u82e5\u4e0d\u6d89\u53ca\u6b64\u9875\u53ef\u5220\u9664")) {
                set(shape, "");
            }
            return;
        }
        if (page == 9) {
            String total = money2(project.get("totalRevenueIncTax"));
            replaceParagraphUnicode(shape, "\u603b\u6536\u76ca\uff1a",
                    total + "\u4e07\u5143");
            replaceParagraphUnicode(shape, "\u95f4\u63a5\u6536\u76ca\uff1a",
                    n(sectionMap(project).get("indirectRevenue"), ""));
            String agreementYears = n(project.get("agreementYears"), "");
            replaceParagraphUnicode(shape, "\u534f\u8bae\u671f\uff1a",
                    agreementYears.isBlank() ? "" : agreementYears + "\u5e74");
            replaceParagraphUnicode(shape, "\u5e26\u52a8\u5546\u673a\uff1a",
                    n(sectionMap(project).get("drivenOpportunity"), ""));
            return;
        }
        if (page == 10 && text.contains("\u9879\u76ee\u6574\u4f53\uff1a")) {
            String rate = n(project.get("overallProfitRate"), "");
            set(shape, "\u9879\u76ee\u6574\u4f53\uff1a\u542b\u5408\u4f5c\u670d\u52a1\u3001"
                    + "\u4ee3\u7406\u4eba\u7b49\u5404\u6a21\u5f0f\u6536\u652f\uff0c"
                    + "\u6d4b\u7b97\u9879\u76ee\u51c0\u5229\u6da6\u7387"
                    + (rate.isBlank() ? "" : rate + "%") + "\u3002");
            return;
        }
        if (page == 11 && text.contains("\u9879\u76ee\u6574\u4f53\u542b")) {
            String summary = n(sectionMap(project).get("preDecisionSummary"), "");
            if (!summary.isBlank()) {
                set(shape, summary);
            }
            return;
        }
        if (page == 15) {
            Map<String, Object> sections = sectionMap(project);
            replaceParagraphUnicode(shape, "\u9700\u7b26\u5408\u6218\u7565\u5361\u4f4d\u573a\u666f\uff1a",
                    n(sections.get("advancePaymentStrategyConclusion"), ""));
            replaceParagraphUnicode(shape, "\u9700\u7b26\u5408\u96c6\u56e2\u4f18\u5355\u6807\u51c6\uff1a",
                    n(sections.get("advancePaymentPreferredOrderConclusion"), ""));
            replaceParagraphUnicode(shape, "\u9700\u7b26\u5408\u80fd\u529b\u6c89\u6dc0\u8981\u6c42\uff1a",
                    n(sections.get("advancePaymentCapabilityConclusion"), ""));
            if (text.contains("\u82e5\u4e0d\u6d89\u53ca\u53ef\u5220\u9664")) {
                set(shape, "");
            }
            return;
        }
        if (page == 16) {
            fillSelectionPage(shape, project, text);
            return;
        }
        if (page == 17) {
            fillClientDueDiligenceText(shape, project, text);
            return;
        }
        if (page == 18) {
            if (isGovernmentFundProject(project) && isGovernmentRiskAuditText(text)) {
                clearGovernmentRiskParagraphs(shape);
            }
            fillClientPerformanceText(shape, project, text);
            return;
        }
        if (page == 19 || page == 20) {
            fillFundAnalysis(shape, project, text);
            return;
        }
        if (page == 21) {
            fillVendorRiskSummary(shape, project, text);
            return;
        }
        if (page == 22) {
            fillPaymentMethods(shape, project, text);
            return;
        }
        if (page == 23 && text.contains("\u7ecf\u8bc4\u4f30\uff0c\u672c\u9879\u76ee\u5b58\u5728")) {
            Object risk = sectionMap(project).get("riskSummary");
            if (risk != null && !String.valueOf(risk).isBlank()) {
                writeRiskSummary(shape, String.valueOf(risk));
            }
            return;
        }
        if (page == 24 && text.contains("\u672c\u9879\u76ee\u5c5e\u4e8e")) {
            String levels = joinedSectionValues(project, "threeLineSelections", "、");
            if (levels.isBlank()) levels = n(project.get("threeLineLevel"), "底线");
            set(shape, "经集团新三线评估标准评估，本项目属于" + levels + "项目。");
            return;
        }
        if (page == 25) {
            fillDeliveryPlan(shape, project, text);
            return;
        }
        if (page == 26) { fillDecisionPage(shape, project, text); return; }
        if (page == 28) { fillPreDecisionOpinions(shape, project, text); return; }
        if (page == 29) { fillExistingBusinessText(shape, project, text); return; }
        if (page == 30) { fillSupplyChainFinance(shape, project, text); return; }
        if (page == 31) { fillAppraisalText(shape, project, text); return; }
        if (page == 26 && text.contains("\u662f\u5426\u540c\u610f\u8be5\u9879\u76ee\u65b9\u6848")) {
            String decision = n(sectionMap(project).get("decisionContent"), "");
            if (!decision.isBlank()) {
                set(shape, decision);
            } else {
                String amount = project.get("contractAmountIncTax") == null ? "" :
                        "\uff0c\u9879\u76ee\u7b7e\u7ea6\u91d1\u989d" + money(project.get("contractAmountIncTax")) + "\u4e07\u5143\uff08\u542b\u7a0e\uff09";
                set(shape, "\u662f\u5426\u540c\u610f\u8be5\u9879\u76ee\u65b9\u6848" + amount + "\u3002");
            }
        }
    }

    private boolean financeTableKind(XSLFTable table, String marker) {
        String normalizedMarker = normalize(marker);
        if (normalizedMarker.isBlank() || table.getRows().isEmpty()) return false;
        for (XSLFTableCell cell : table.getRows().get(0).getCells()) {
            if (normalize(cell.getText()).contains(normalizedMarker)) return true;
        }
        return false;
    }

    private void fillCoverPage(XSLFTextShape shape, Map<String, Object> project, String text) {
        Map<String, Object> sections = sectionMap(project);
        String branch = n(sections.get("branchCompany"), n(sections.get("departmentName"), ""));
        String applicant = coverApplicant(sections);
        String projectName = n(project.get("projectName"), "");
        if (text.contains("\u5173\u4e8e") && text.contains("\u5206\u516c\u53f8\u4e3a") && text.contains("\u9879\u76ee")) {
            String normalizedBranch = branch.isBlank() ? "" : branch;
            String normalizedProject = projectName.endsWith("\u9879\u76ee") ? projectName : projectName + "\u9879\u76ee";
            // New cover puts the complete request title in one shape. Older
            // templates may keep the final phrase in a separate text shape.
            String suffix = text.contains("进行立项的请示") ? "进行立项的请示" : "";
            set(shape, "\u5173\u4e8e" + normalizedBranch + "\u4e3a" + normalizedProject + suffix);
            return;
        }
        if (text.contains("\u8fdb\u884c\u7acb\u9879\u7684\u8bf7\u793a")) {
            // The latest cover stores this line as a PowerPoint field rather than a
            // normal text run. Updating the field run is discarded by POI on save,
            // so replace the field with ordinary text while retaining the shape's
            // paragraph-level template formatting.
            shape.clearText();
            XSLFTextParagraph paragraph = shape.addNewTextParagraph();
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText("\u8fdb\u884c\u7acb\u9879\u7684\u8bf7\u793a");
            run.setFontFamily("Microsoft YaHei");
            run.setFontSize(30.0);
            run.setBold(true);
            return;
        }
        if (text.contains("分公司") && text.contains("年") && text.contains("月")) {
            set(shape, buildBranchAndPeriod(sections));
            return;
        }
        if (text.strip().matches(".*XX\\s*\u5206\u516c\u53f8.*")  ) {
            set(shape, applicant);
            return;
        }
        if (text.contains("建设项目方案")) {
            set(shape, buildTitle(project));
            return;
        }
        if (looksLikeBranchAndPeriod(text)) {
            set(shape, text.contains("分公司") ? applicant : n(sections.get("reportPeriod"), ""));
        }
    }

    private boolean looksLikeBranchAndPeriod(String text) {
        return text.contains("分公司") || (text.contains("年") && text.contains("月") && text.length() < 40);
    }

    private String buildTitle(Map<String, Object> project) {
        Map<String, Object> sections = sectionMap(project);
        StringBuilder builder = new StringBuilder();
        appendTitlePart(builder, n(sections.get("cityName"), ""));
        appendTitlePart(builder, n(sections.get("bureauName"), ""));
        appendTitlePart(builder, n(sections.get("platformName"), ""));
        appendTitlePart(builder, n(project.get("projectName"), ""));
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append("建设项目方案");
        return builder.toString();
    }

    private String coverApplicant(Map<String, Object> sections) {
        return sections.containsKey("coverApplicant") ? n(sections.get("coverApplicant"), "")
                : n(sections.get("branchCompany"), n(sections.get("departmentName"), ""));
    }

    private String buildBranchAndPeriod(Map<String, Object> sections) {
        String branchCompany = coverApplicant(sections);
        String reportPeriod = n(sections.get("reportPeriod"), "");
        StringBuilder builder = new StringBuilder();
        if (!branchCompany.isBlank()) {
            builder.append(branchCompany);
        }
        if (!reportPeriod.isBlank()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(reportPeriod);
        }
        return builder.toString();
    }

    private void appendIfNotBlank(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(value);
        }
    }

    private void appendTitlePart(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(value);
        }
    }

    private void fillOverviewPage(XSLFTextShape shape, Map<String, Object> project, String text) {
        Map<String, Object> sections = sectionMap(project);
        if (text.contains("项目主体：")) {
            set(shape, "项目主体：" + n(sections.get("projectSubject"), n(project.get("customerName"), "")));
            return;
        }
        if (text.contains("获取方式：")) {
            set(shape, "获取方式：" + n(sections.get("acquisitionMethod"), "") + "，中标时间" + n(sections.get("bidDate"), ""));
            return;
        }
        if (text.contains("签约金额：")) {
            set(shape, "签约金额：" + money(project.get("contractAmountIncTax")) + "万元（含税）");
            return;
        }
        if (text.contains("资金来源：")) {
            set(shape, "资金来源：" + n(sections.get("fundingSource"), ""));
            return;
        }
        if (text.contains("项目概述：")) {
            set(shape, "项目概述：" + n(sections.get("projectOverview"), ""));
            return;
        }
        if (text.contains("商务模式：")) {
            set(shape, "商务模式：采用“" + n(sections.get("businessModel"), "") + "”模式承建，其中“" + n(sections.get("businessModelPart"), n(sections.get("businessModel"), "")) + "”模式部分产权归属" + n(sections.get("propertyOwnership"), ""));
            return;
        }
        if (text.contains("商机编码：")) {
            replaceParagraph(shape, "商机编码：", n(project.get("opportunityNo"), ""));
        }
    }

    /** Matches the real template text using Unicode escapes so source-file encoding cannot break replacement. */
    private boolean fillOverviewPageUnicode(XSLFTextShape shape, Map<String, Object> project, String text) {
        if (text.contains("合作服务模式") && text.contains("不需要写产权归属")) {
            set(shape, "");
            return true;
        }
        Map<String, Object> sections = sectionMap(project);
        boolean replaced = false;
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            String paragraphText = paragraph.getText() == null ? "" : paragraph.getText();
            if (paragraphText.contains("\u9879\u76ee\u4e3b\u4f53\uff1a")) {
                setLabelValue(paragraph, "\u9879\u76ee\u4e3b\u4f53\uff1a",
                        n(sections.get("projectSubject"), n(project.get("customerName"), "")));
                replaced = true;
            } else if (paragraphText.contains("\u83b7\u53d6\u65b9\u5f0f\uff1a")) {
                setLabelValue(paragraph, "\u83b7\u53d6\u65b9\u5f0f\uff1a",
                        n(sections.get("acquisitionMethod"), "") + "\uff0c\u4e2d\u6807\u65f6\u95f4" + n(sections.get("bidDate"), ""));
                replaced = true;
            } else if (paragraphText.contains("\u7b7e\u7ea6\u91d1\u989d\uff1a")) {
                setLabelValue(paragraph, "\u7b7e\u7ea6\u91d1\u989d\uff1a",
                        money(project.get("contractAmountIncTax")) + "\u4e07\u5143\uff08\u542b\u7a0e\uff09");
                replaced = true;
            } else if (paragraphText.contains("\u8d44\u91d1\u6765\u6e90\uff1a")) {
                setLabelValue(paragraph, "\u8d44\u91d1\u6765\u6e90\uff1a",
                        n(sections.get("fundingSource"), ""));
                replaced = true;
            } else if (paragraphText.contains("\u9879\u76ee\u6982\u8ff0\uff1a")) {
                setLabelValue(paragraph, "\u9879\u76ee\u6982\u8ff0\uff1a",
                        n(sections.get("projectOverview"), ""));
                replaced = true;
            } else if (paragraphText.contains("\u5546\u52a1\u6a21\u5f0f\uff1a")) {
                String model = n(sections.get("businessModel"), "");
                setLabelValue(paragraph, "\u5546\u52a1\u6a21\u5f0f\uff1a",
                        "采用“" + model + (model.endsWith("模式") ? "" : "模式") + "”承建");
                replaced = true;
            } else if (paragraphText.contains("\u5546\u673a\u7f16\u7801\uff1a")) {
                setLabelValue(paragraph, "\u5546\u673a\u7f16\u7801\uff1a",
                        n(project.get("opportunityNo"), ""));
                replaced = true;
            }
        }
        if (replaced) {
            removeEmptyParagraphs(shape);
        }
        return replaced;
    }

    /**
     * Keeps the template's label run untouched and writes only the form value
     * into the following run. The value is black, while font family, size,
     * weight, paragraph spacing and bullet formatting continue to come from
     * the original template.
     */
    private void setLabelValue(XSLFTextParagraph paragraph, String label, String value) {
        String cleanValue = value == null ? "" : value
                .replaceAll("[\\s\\p{Z}\\p{C}]+", " ")
                .strip();
        List<XSLFTextRun> runs = paragraph.getTextRuns();
        XSLFTextRun labelRun = firstTextRun(runs, 0);
        if (labelRun == null) {
            labelRun = paragraph.addNewTextRun();
        }
        int labelIndex = runs.indexOf(labelRun);
        XSLFTextRun valueRun = firstTextRun(runs, labelIndex + 1);
        if (valueRun == null) {
            valueRun = paragraph.addNewTextRun();
        }
        labelRun.setText(label);
        valueRun.setText(cleanValue);
        valueRun.setFontColor(Color.BLACK);
        for (XSLFTextRun run : runs) {
            if (run != labelRun && run != valueRun && !"XSLFLineBreak".equals(run.getClass().getSimpleName())) {
                run.setText("");
            }
        }
        XmlObject[] lineBreaks = paragraph.getXmlObject().selectPath(
                "declare namespace a='http://schemas.openxmlformats.org/drawingml/2006/main' ./a:br");
        for (XmlObject lineBreak : lineBreaks) {
            try (XmlCursor cursor = lineBreak.newCursor()) {
                cursor.removeXml();
            }
        }
    }

    private XSLFTextRun firstTextRun(List<XSLFTextRun> runs, int start) {
        for (int i = Math.max(0, start); i < runs.size(); i++) {
            XSLFTextRun run = runs.get(i);
            if (!"XSLFLineBreak".equals(run.getClass().getSimpleName())) {
                return run;
            }
        }
        return null;
    }

    private void removeEmptyParagraphs(XSLFTextShape shape) {
        for (int i = shape.getTextParagraphs().size() - 1; i >= 0; i--) {
            XSLFTextParagraph paragraph = shape.getTextParagraphs().get(i);
            String paragraphText = paragraph.getText() == null ? "" : paragraph.getText()
                    .replaceAll("[\\s\\p{Z}\\p{C}\u25a1]+", "");
            if (shape.getTextParagraphs().size() > 1
                    && paragraphText.isEmpty()) {
                shape.getTextBody().removeParagraph(i);
            }
        }
    }

    private void fillGroupPage(XSLFTextShape shape, Map<String, Object> project, String text) {
        Map<String, Object> sections = sectionMap(project);
        if (text.contains("本集团情况：")) {
            set(shape, "本集团情况：" + n(sections.get("groupSummary"), ""));
            return;
        }
        if (text.contains("客户树情况：")) {
            set(shape, "客户树情况：" + n(sections.get("customerTreeSummary"), ""));
        }
    }

    private boolean fillGroupPageUnicode(XSLFTextShape shape, Map<String, Object> project, String text) {
        Map<String, Object> sections = sectionMap(project);
        boolean replaced = false;
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            String paragraphText = paragraph.getText() == null ? "" : paragraph.getText();
            if (paragraphText.contains("\u672c\u96c6\u56e2\u60c5\u51b5\uff1a")) {
                setLabelValue(paragraph, "\u672c\u96c6\u56e2\u60c5\u51b5\uff1a",
                        n(sections.get("groupSummary"), ""));
                replaced = true;
            } else if (paragraphText.contains("\u5ba2\u6237\u6811\u60c5\u51b5\uff1a")) {
                setLabelValue(paragraph, "\u5ba2\u6237\u6811\u60c5\u51b5\uff1a",
                        n(sections.get("customerTreeSummary"), ""));
                replaced = true;
            }
        }
        return replaced;
    }

    private void fillGroupTitle(XSLFTextShape shape, Map<String, Object> project, String text) {
        if (!text.contains("\u96c6\u56e2\u62d3\u5c55\u73b0\u72b6")) {
            return;
        }
        String groupName = n(sectionMap(project).get("projectSubject"), n(project.get("customerName"), ""));
        if (groupName.isBlank()) {
            return;
        }
        set(shape, groupName + (groupName.endsWith("\u96c6\u56e2") ? "" : "\u96c6\u56e2")
                + "\u62d3\u5c55\u73b0\u72b6");
    }

    /** Derive both page headings and table values from details, never cached form totals. */
    private Map<String, Object> normalizeFinanceDetails(Map<String, Object> source) {
        Map<String, Object> project = new LinkedHashMap<>(source);
        Map<String, Object> sections = sectionMap(source);
        Set<String> selectedCosts = new HashSet<>();
        Set<String> selectedRevenues = new HashSet<>();
        boolean filterCosts = sections.get("costPrimaryModes") instanceof List<?>;
        boolean filterRevenues = sections.get("revenueModes") instanceof List<?>;
        if (sections.get("costPrimaryModes") instanceof List<?> modes) {
            if (modes.contains("投资部分")) selectedCosts.add("投资部分");
            if (modes.contains("其他部分")) selectedCosts.add("其他部分");
            if (modes.contains("成本部分") || modes.contains("合作服务模式")) {
                if (sections.get("cooperationCostModes") instanceof List<?> costs) {
                    for (Object mode : costs) selectedCosts.add(String.valueOf(mode));
                }
            }
        }
        if (sections.get("revenueModes") instanceof List<?> modes) {
            for (Object mode : modes) selectedRevenues.add(String.valueOf(mode));
        }
        for (String key : List.of("costItems", "incomeItems")) {
            List<Map<String, Object>> normalized = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            Object raw = source.get(key);
            for (Object value : raw instanceof List<?> list ? list : List.of()) {
                if (!(value instanceof Map<?, ?> item)) continue;
                String name = n(item.get("name"), "").trim();
                String mode = n(item.get("mode"), "").trim();
                if ("costItems".equals(key) && filterCosts && !selectedCosts.contains(mode)) continue;
                if ("incomeItems".equals(key) && filterRevenues && !selectedRevenues.contains(mode)) continue;
                if ("小计".equals(name) || "合计".equals(mode)) continue;
                BigDecimal amount = decimalValue(item.get("amountIncTax"));
                // Zero-value template placeholders are not selected finance details.
                if (amount.signum() == 0) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                item.forEach((k, v) -> row.put(String.valueOf(k), v));
                row.put("name", name.isBlank() ? "未命名明细" : name);
                row.put("mode", mode.isBlank() ? "其他" : mode);
                BigDecimal rate = decimalValue(item.get("taxRate")).max(BigDecimal.ZERO);
                row.put("taxRate", rate);
                row.put("amountExTax", amount.divide(
                        BigDecimal.ONE.add(rate.movePointLeft(2)), java.math.MathContext.DECIMAL128));
                normalized.add(row);
                total = total.add(amount);
            }
            project.put(key, normalized);
            project.put("costItems".equals(key) ? "totalCostIncTax" : "totalRevenueIncTax", total);
        }
        return project;
    }

    private String buildInvestmentSummary(Map<String, Object> project) {
        BigDecimal investment = BigDecimal.ZERO;
        BigDecimal cooperation = BigDecimal.ZERO;
        BigDecimal sales = BigDecimal.ZERO;
        BigDecimal other = BigDecimal.ZERO;
        BigDecimal entrusted = BigDecimal.ZERO;
        Object rawItems = project.get("costItems");
        List<?> items = rawItems instanceof List<?> list ? list : List.of();
        for (Object rawItem : items) {
            if (!(rawItem instanceof Map<?, ?> rawMap)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) rawMap;
            BigDecimal amount = decimalValue(item.get("amountIncTax"));
            switch (n(item.get("mode"), "")) {
                case "\u6295\u8d44\u90e8\u5206" -> investment = investment.add(amount);
                case "\u5408\u4f5c\u670d\u52a1\u6a21\u5f0f" -> cooperation = cooperation.add(amount);
                case "\u8d2d\u9500\u6a21\u5f0f" -> sales = sales.add(amount);
                case "\u5176\u4ed6\u6210\u672c" -> other = other.add(amount);
                case "\u5176\u4ed6\u90e8\u5206" -> entrusted = entrusted.add(amount);
                default -> { }
            }
        }
        BigDecimal total = investment.add(cooperation).add(sales).add(other).add(entrusted);
        List<String> details = new ArrayList<>();
        addNonZeroInvestmentDetail(details, "\u6d89\u53ca\u6295\u8d44", investment);
        addNonZeroInvestmentDetail(details, "\u5408\u4f5c\u670d\u52a1\u6210\u672c", cooperation);
        addNonZeroInvestmentDetail(details, "\u8d2d\u9500\u6a21\u5f0f\u6210\u672c", sales);
        addNonZeroInvestmentDetail(details, "\u5176\u4ed6\u6210\u672c", other);
        addNonZeroInvestmentDetail(details, "\u53d7\u6258\u4ee3\u9500\u6b3e", entrusted);
        return moneyText(total) + "\u4e07\u5143" + (details.isEmpty() ? "" : "\uff0c\u5176\u4e2d" + String.join("\u3001", details)) + "\u3002";
    }

    private void enableTextShrinkToFit(List<XSLFShape> shapes) {
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFGroupShape group) enableTextShrinkToFit(group.getShapes());
            else if (shape instanceof XSLFTextShape textShape) textShape.setTextAutofit(TextShape.TextAutofit.NORMAL);
        }
    }

    private void removeInvalidPresentationNotesRelationships(XMLSlideShow ppt) throws Exception {
        // importContent() correctly relates copied notes to the cloned slide,
        // but POI can also leave an illegal PresentationPart -> NotesSlidePart
        // relationship. Desktop PowerPoint then reports that the file needs repair.
        List<String> invalidRelationshipIds = new ArrayList<>();
        for (var relationship : ppt.getPackagePart()
                .getRelationshipsByType(XSLFRelation.NOTES.getRelation())) {
            invalidRelationshipIds.add(relationship.getId());
        }
        for (String relationshipId : invalidRelationshipIds) {
            ppt.getPackagePart().removeRelationship(relationshipId);
        }
    }

    private void removeEmptySlideRelationshipReferences(XMLSlideShow ppt) {
        QName relationshipId = new QName(
                "http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id");
        for (XSLFSlide slide : ppt.getSlides()) {
            try (XmlCursor cursor = slide.getXmlObject().newCursor()) {
                while (cursor.hasNextToken()) {
                    if (cursor.toNextToken() == XmlCursor.TokenType.START
                            && "".equals(cursor.getAttributeText(relationshipId))) {
                        cursor.removeAttribute(relationshipId);
                    }
                }
            }
        }
    }

    private void addNonZeroInvestmentDetail(List<String> details, String label, BigDecimal value) {
        if (value.signum() != 0) details.add(label + moneyText(value) + "\u4e07\u5143");
    }

    private String moneyText(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private boolean fillConstructionPage(XSLFTextShape shape, Map<String, Object> project, String text) {
        String value = n(sectionMap(project).get("constructionContent"), "");
        if (value.isBlank()) return false;
        String current = shape.getText() == null ? "" : shape.getText();
        if (current.contains("\u8be5\u9879\u76ee\u90e8\u7f72\u5728")
                && current.contains("\u4e3b\u8981\u5305\u62ec")) {
            setConstructionDetails(shape, constructionLines(value));
            return true;
        }
        if (current.contains("\u9700\u6309\u4ee5\u4e0b\u7cfb\u7edf\u67b6\u6784")) {
            set(shape, "");
            return true;
        }
        return false;
    }

    private List<String> constructionLines(String value) {
        String normalized = value.replace("\r", "\n")
                .replaceAll("\\n+", "\n")
                .trim();
        List<String> lines = new java.util.ArrayList<>();
        for (String line : normalized.split("\n")) {
            String clean = line.replaceFirst("^[\\s\u2713\u25a1\u2022\u00b7-]+", "").trim();
            if (!clean.isBlank()) {
                lines.add(clean);
            }
        }
        if (lines.size() == 1) {
            String[] parts = normalized.split("(?=(?:\u5e94\u7528\u5c42|\u5e73\u53f0\u5c42|\u4f20\u8f93\u5c42|\u611f\u77e5\u5c42)\uff1a)");
            if (parts.length > 1) {
                lines.clear();
                for (String part : parts) {
                    String clean = part.trim();
                    if (!clean.isBlank()) lines.add(clean);
                }
            }
        }
        return lines.isEmpty() ? List.of(value) : lines;
    }

    private void setConstructionDetails(XSLFTextShape shape, List<String> lines) {
        shape.clearText();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            XSLFTextParagraph paragraph = shape.addNewTextParagraph();
            paragraph.setBullet(false);
            paragraph.setLeftMargin(index == 0 ? 0d : 18d);
            paragraph.setIndent(0d);
            paragraph.setSpaceAfter(3d);
            XSLFTextRun marker = paragraph.addNewTextRun();
            marker.setText(index == 0 ? "\u25a1 " : "\u2713 ");
            marker.setFontFamily("Microsoft YaHei");
            marker.setFontColor(new Color(0x00, 0x70, 0xC0));
            marker.setFontSize(17d);
            marker.setBold(true);
            int colon = line.indexOf('\uff1a');
            if (index > 0 && colon > 0) {
                XSLFTextRun label = paragraph.addNewTextRun();
                label.setText(line.substring(0, colon + 1));
                label.setFontFamily("Microsoft YaHei");
                label.setFontColor(new Color(0x00, 0x70, 0xC0));
                label.setFontSize(17d);
                label.setBold(true);
                line = line.substring(colon + 1).trim();
            }
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText(line);
            run.setFontFamily("Microsoft YaHei");
            run.setFontColor(index == 0 ? new Color(0x00, 0x70, 0xC0) : Color.BLACK);
            run.setFontSize(17d);
        }
    }

    private void fillCapabilityPage(XSLFTextShape shape, Map<String, Object> project, String text) {
        Map<String, Object> sections = sectionMap(project);
        String demand = n(sections.get("capabilityDemand"), n(sections.get("capabilitySupply"), ""));
        String summary = n(sections.get("sevenFusionSummary"), "");
        String current = n(shape.getText(), "");
        if (current.contains("\u6839\u636e\u8be5\u9879\u76ee\u5efa\u8bbe\u5185\u5bb9")
                && current.contains("\u4e03\u878d\u89e3\u6790")) {
            shape.clearText();
            addCapabilityParagraph(shape, "□ \u6839\u636e\u8be5\u9879\u76ee\u5efa\u8bbe\u5185\u5bb9\uff0c\u4e3b\u8981\u5305\u62ec\u4ee5\u4e0b\u9700\u6c42\uff1a", false);
            for (String line : constructionLines(demand)) {
                addCapabilityLabeledParagraph(shape, line);
            }
            addSevenFusionParagraph(shape, summary);
            return;
        }
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            String paragraphText = paragraph.getText() == null ? "" : paragraph.getText();
            if (paragraphText.contains("\u6839\u636e\u8be5\u9879\u76ee\u5efa\u8bbe\u5185\u5bb9")) {
                setLabelValue(paragraph, "\u6839\u636e\u8be5\u9879\u76ee\u5efa\u8bbe\u5185\u5bb9\uff0c\u4e3b\u8981\u5305\u62ec\u4ee5\u4e0b\u9700\u6c42\uff1a", demand);
            } else if (paragraphText.contains("\u4e03\u878d\u89e3\u6790")) {
                setLabelValue(paragraph, "\u7ecf\u201c\u4e03\u878d\u89e3\u6790\u201d\u540e\uff0c", summary);
            }
        }
    }

    private void addCapabilityParagraph(XSLFTextShape shape, String value, boolean indented) {
        if (value == null || value.isBlank()) return;
        XSLFTextParagraph paragraph = shape.addNewTextParagraph();
        paragraph.setBullet(false);
        paragraph.setLeftMargin(indented ? 24d : 0d);
        paragraph.setIndent(0d);
        paragraph.setSpaceAfter(3d);
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(value.strip());
        run.setFontFamily("Microsoft YaHei");
        run.setFontSize(16d);
        run.setFontColor(new Color(0x00, 0x70, 0xC0));
        run.setBold(!indented);
    }

    private boolean fillCapabilityTable(XSLFTable table, Map<String, Object> project) {
        String text = tableText(table);
        boolean left = text.contains("\u878d\u4e91") || text.contains("\u878d5G") || text.contains("\u878d\u7f51");
        boolean right = text.contains("\u878d\u4e2d\u53f0") || text.contains("\u878d\u96c6\u6210") || text.contains("\u878dC");
        if (!left && !right) {
            return false;
        }
        Map<String, Object> sections = sectionMap(project);
        Map<String, Object> source = mapOf(sections.get(left ? "fusionLeftTable" : "fusionRightTable"));
        if (right) {
            source = new LinkedHashMap<>(source);
            Map<String, Object> hardware = new LinkedHashMap<>(mapOf(source.get("硬件集成")));
            Map<String, Object> software = new LinkedHashMap<>(mapOf(source.get("软件集成")));
            String first = n(hardware.get("remark"), "").trim();
            String second = n(software.get("remark"), "").trim();
            hardware.put("remark", first.isEmpty() ? second : second.isEmpty() || first.equals(second) ? first : first + "\n" + second);
            software.remove("remark");
            source.put("硬件集成", hardware);
            source.put("软件集成", software);
            for (int i = 0; i + 1 < table.getRows().size(); i++) {
                XSLFTableRow row = table.getRows().get(i);
                if (row.getCells().stream().anyMatch(cell -> cell.getText().contains("硬件集成"))) {
                    int column = row.getCells().size() - 1;
                    table.getRows().get(i + 1).getCells().get(column).setText("");
                    table.mergeCells(i, i + 1, column, column);
                    break;
                }
            }
        }
        List<String> keys = left
                ? List.of("\u5176\u4ed6AI\u4ea7\u54c1\u80fd\u529b", "\u4e5d\u5929\u80fd\u529b", "\u878d\u4e91", "\u878d5G", "\u878d\u7f51")
                : List.of("\u96c6\u56e2\u6210\u5458\u53f7\u5361\uff08\u4fdd\uff09", "\u96c6\u56e2\u6210\u5458\u53f7\u5361\uff08\u62d3\uff09",
                "\u4e13\u4e1a\u516c\u53f8\u65b9\u6848", "\u96c6\u56e2\u65b9\u6848", "\u7701\u5185\u65b9\u6848", "\u96c6\u56e2\u4e2d\u53f0",
                "\u7701\u5185\u4e2d\u53f0", "\u786c\u4ef6\u96c6\u6210", "\u8f6f\u4ef6\u96c6\u6210", "\u4e91\u7535\u8111", "\u4e09\u5927");
        for (int tableRowIndex = 0; tableRowIndex < table.getRows().size(); tableRowIndex++) {
            XSLFTableRow row = table.getRows().get(tableRowIndex);
            List<XSLFTableCell> cells = row.getCells();
            String rowText = cells.stream().map(XSLFTableCell::getText).reduce("", (a, b) -> a + b);
            String key = keys.stream().filter(rowText::contains).findFirst().orElse(null);
            if (key == null || cells.size() < 5) {
                continue;
            }
            Map<String, Object> rowData = mapOf(source.get(key));
            int firstValueCell = cells.size() - 4;
            XSLFTableCell referenceCell = cells.get(Math.max(0, firstValueCell - 1));
            setCapabilityCell(cells.get(firstValueCell), rowData.get("canIntegrate"), referenceCell);
            setCapabilityCell(cells.get(firstValueCell + 1), rowData.get("integrated"), referenceCell);
            setCapabilityCell(cells.get(firstValueCell + 2), rowData.get("amount"), referenceCell);
            setMergedAwareCapabilityCell(
                    table, tableRowIndex, firstValueCell + 3, rowData.get("remark"), referenceCell);
        }
        return true;
    }

    private void setMergedAwareCapabilityCell(XSLFTable table, int rowIndex, int columnIndex,
                                              Object value, XSLFTableCell referenceCell) {
        if (value == null || String.valueOf(value).isBlank()) {
            return;
        }
        int anchorRow = rowIndex;
        while (anchorRow > 0) {
            List<XSLFTableCell> cells = table.getRows().get(anchorRow).getCells();
            if (columnIndex >= cells.size() || !isVerticalMergeContinuation(cells.get(columnIndex))) {
                break;
            }
            anchorRow--;
        }
        List<XSLFTableCell> anchorCells = table.getRows().get(anchorRow).getCells();
        if (columnIndex < anchorCells.size()) {
            set(anchorCells.get(columnIndex), String.valueOf(value).strip(), referenceCell);
        }
    }

    private boolean isVerticalMergeContinuation(XSLFTableCell cell) {
        try (XmlCursor cursor = cell.getXmlObject().newCursor()) {
            return cursor.getAttributeText(new QName("", "vMerge")) != null;
        }
    }

    private void setCapabilityCell(XSLFTableCell cell, Object value, XSLFTableCell referenceCell) {
        if (value != null && !String.valueOf(value).isBlank()) {
            set(cell, String.valueOf(value), referenceCell);
        }
    }

    private boolean fillPageThreeTable(XSLFTable table, int tableIndex, Map<String, Object> project) {
        String tableText = tableText(table);
        boolean groupTable = tableText.contains("\u5176\u4e2d\uff1a\u901a\u4fe1\u670d\u52a1\u6536\u5165")
                || tableText.contains("\u7b97\u529b\u670d\u52a1\u6536\u5165")
                || tableText.contains("\u667a\u80fd\u670d\u52a1\u6536\u5165");
        boolean treeTable = tableText.contains("\u8282\u70b9\u7eb3\u7ba1\u7387")
                || tableText.contains("\u4e13\u7ebf\u6e17\u900f\u7387")
                || tableText.contains("\u79fb\u52a8\u4e91\u6e17\u900f\u7387");
        if (!groupTable && !treeTable) {
            return false;
        }
        clearTableMerges(table);
        Map<String, Object> sections = sectionMap(project);
        Map<String, Object> source = groupTable ? mapOf(sections.get("groupTable")) : mapOf(sections.get("treeTable"));
        String[] rowKeys = groupTable ? GROUP_ROWS : TREE_ROWS;
        if (!table.getRows().isEmpty()) {
            List<XSLFTableCell> headerCells = table.getRows().get(0).getCells();
            String firstPeriod = n(sections.get(groupTable ? "groupPeriod1" : "treePeriod1"), "");
            String secondPeriod = n(sections.get(groupTable ? "groupPeriod2" : "treePeriod2"), "");
            if (headerCells.size() > 1) set(headerCells.get(1), firstPeriod, headerCells.get(0));
            if (headerCells.size() > 2) set(headerCells.get(2), secondPeriod, headerCells.get(0));
        }
        // Member metrics are free-text descriptions. Merge before writing so
        // POI keeps the value in the surviving left-most cell.
        for (int tableRowIndex = 5; tableRowIndex < Math.min(table.getRows().size(), rowKeys.length + 1); tableRowIndex++) {
            if (table.getRows().get(tableRowIndex).getCells().size() >= 4) {
                table.mergeCells(tableRowIndex, tableRowIndex, 1, 3);
            }
        }
        for (int tableRowIndex = 0; tableRowIndex < table.getRows().size(); tableRowIndex++) {
            XSLFTableRow row = table.getRows().get(tableRowIndex);
            List<XSLFTableCell> cells = row.getCells();
            if (cells.isEmpty()) {
                continue;
            }
            int rowIndex = tableRowIndex >= 5 && tableRowIndex <= 8
                    ? tableRowIndex - 1
                    : matchedRowIndex(cells.get(0).getText(), rowKeys);
            if (rowIndex < 0) {
                continue;
            }
            if (cells.size() > 1) set(cells.get(1), valueFor(source, rowKeys[rowIndex], "2025", rowIndex), cells.get(0));
            if (rowIndex >= 4) continue;
            if (cells.size() > 2) set(cells.get(2), valueFor(source, rowKeys[rowIndex], "2026", rowIndex), cells.get(0));
            if (cells.size() > 3) set(cells.get(3), yearOnYear(
                    valueFor(source, rowKeys[rowIndex], "2025", rowIndex),
                    valueFor(source, rowKeys[rowIndex], "2026", rowIndex)), cells.get(0));
        }
        return true;
    }

    private String yearOnYear(String previous, String current) {
        if (previous == null || current == null || previous.isBlank() || current.isBlank()) return "";
        try {
            BigDecimal before = new BigDecimal(previous.replaceAll("[,，%％]", "").trim());
            BigDecimal after = new BigDecimal(current.replaceAll("[,，%％]", "").trim());
            if (before.signum() == 0) return "—";
            return after.subtract(before).multiply(BigDecimal.valueOf(100)).divide(before, 2, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException ignored) { return ""; }
    }

    @SuppressWarnings("unchecked")
    private boolean fillProcurementComparisonTable(XSLFTable table, Map<String, Object> project) {
        Map<String, Object> sections = sectionMap(project);
        String listKey;
        String[] fields;
        if (table.getRows().size() == 5 && table.getRows().get(0).getCells().size() == 10) {
            listKey = "procurementPublicRows";
            fields = new String[]{"content", "unit", "quantity", "standardProduct",
                    "supplier1", "supplier2", "supplier3", "otherSupplier",
                    "difference", "requirement"};
        } else if (table.getRows().size() == 3 && table.getRows().get(0).getCells().size() == 6) {
            listKey = "procurementDirectRows";
            fields = new String[]{"content", "unit", "quantity", "standardProduct",
                    "difference", "requirement"};
        } else {
            return false;
        }
        Object raw = sections.get(listKey);
        List<?> rows = raw instanceof List<?> list ? list : List.of();
        for (int rowIndex = 2; rowIndex < table.getRows().size(); rowIndex++) {
            Map<String, Object> source = rowIndex - 2 < rows.size()
                    ? mapOf(rows.get(rowIndex - 2)) : Map.of();
            List<XSLFTableCell> cells = table.getRows().get(rowIndex).getCells();
            for (int columnIndex = 0; columnIndex < fields.length && columnIndex < cells.size(); columnIndex++) {
                set(cells.get(columnIndex), n(source.get(fields[columnIndex]), ""));
                forceBlack(cells.get(columnIndex));
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean fillIdcTable(XSLFTable table, Map<String, Object> project) {
        String listKey;
        String[] fields;
        int rows = table.getRows().size();
        int columns = table.getRows().isEmpty() ? 0 : table.getRows().get(0).getCells().size();
        if (rows == 9 && columns == 8) {
            listKey = "idcMainRows";
            fields = new String[]{"billingUnit", "category", "description", "cabinetCount",
                    "unitPrice", "contractMonths", "totalCost", "remark"};
        } else if (rows == 7 && columns == 7) {
            listKey = "idcCabinetRows";
            fields = new String[]{"no", "item", "depreciationYears", "investmentAmount",
                    "plannedCabinets", "monthlyCost", "remark"};
        } else if (rows == 4 && columns == 7) {
            listKey = "idcMaintenanceRows";
            fields = new String[]{"no", "item", "period", "maintenanceFee",
                    "cabinetCount", "monthlyCost", "remark"};
        } else if (rows == 2 && columns == 6) {
            listKey = "idcElectricityRows";
            fields = new String[]{"cabinet", "electricityPrice", "pue",
                    "utilization", "monthlyCost", "remark"};
        } else {
            return false;
        }
        Object raw = sectionMap(project).get(listKey);
        List<?> values = raw instanceof List<?> list ? list : List.of();
        for (int rowIndex = 1; rowIndex < table.getRows().size(); rowIndex++) {
            Map<String, Object> rowValue = rowIndex - 1 < values.size()
                    ? mapOf(values.get(rowIndex - 1)) : Map.of();
            List<XSLFTableCell> cells = table.getRows().get(rowIndex).getCells();
            for (int columnIndex = 0; columnIndex < fields.length && columnIndex < cells.size(); columnIndex++) {
                if (listKey.equals("idcMainRows") && columnIndex == 0 && rowIndex > 1) {
                    continue;
                }
                set(cells.get(columnIndex), n(rowValue.get(fields[columnIndex]), ""));
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean fillBenefitAnalysisTable(XSLFTable table, Map<String, Object> project) {
        if (table.getRows().isEmpty() || table.getRows().get(0).getCells().size() < 6) {
            return false;
        }
        Object dynamicRaw = sectionMap(project).get("benefitAnalysisRows");
        if (dynamicRaw instanceof List<?> dynamicRows) {
            Map<String, Object> sections = sectionMap(project);
            boolean modeSelectionConfigured = sections.get("benefitAnalysisSelectedModes") instanceof List<?>;
            List<?> selectedModes = sections.get("benefitAnalysisSelectedModes") instanceof List<?> list ? list : List.of();
            boolean selectionConfigured = !modeSelectionConfigured && sections.containsKey("benefitAnalysisSelectedKeys");
            List<?> selectedKeys = sections.get("benefitAnalysisSelectedKeys") instanceof List<?> list ? list : List.of();
            Set<String> selectedModeKeys = new HashSet<>();
            selectedModes.forEach(key -> selectedModeKeys.add(n(key, "")));
            Map<String, String> benefitGroupByRowKey = new LinkedHashMap<>();
            for (Object rawRow : dynamicRows) {
                Map<String, Object> row = mapOf(rawRow);
                benefitGroupByRowKey.put(n(row.get("key"), ""), n(row.get("groupKey"), ""));
            }
            String overallGroupKey = dynamicRows.stream()
                    .map(this::mapOf)
                    .filter(row -> "row01".equals(n(row.get("key"), "")))
                    .map(row -> n(row.get("groupKey"), ""))
                    .findFirst().orElse("");
            if (modeSelectionConfigured && !selectedModeKeys.isEmpty()) {
                selectedModeKeys.add(overallGroupKey);
                addDerivedOtherRevenueBenefitGroups(project, benefitGroupByRowKey, selectedModeKeys);
            }
            clearTableMerges(table);
            int outputIndex = 1;
            List<Map<String,Object>> writtenRows = new ArrayList<>();
            for (Object rawRow : dynamicRows) {
                Map<String, Object> rowValue = mapOf(rawRow);
                if (rowValue.isEmpty()) continue;
                boolean overallRow = !overallGroupKey.isBlank()
                        && overallGroupKey.equals(n(rowValue.get("groupKey"), ""));
                if (modeSelectionConfigured && !overallRow
                        && !selectedModeKeys.contains(n(rowValue.get("groupKey"), ""))) continue;
                if (selectionConfigured && !overallRow && selectedKeys.stream().noneMatch(
                        key -> n(key, "").equals(n(rowValue.get("key"), "")))) continue;
                String derivedProfitRate = derivedOtherRevenueProfitRate(project, n(rowValue.get("key"), ""));
                Map<String, Object> outputRow = rowValue;
                if (derivedProfitRate != null) {
                    outputRow = new LinkedHashMap<>(rowValue);
                    outputRow.put("projectValue", derivedProfitRate);
                }
                writtenRows.add(outputRow);
                if (outputIndex >= table.getRows().size()) {
                    int styleIndex = Math.max(1, table.getRows().size() - 1);
                    insertStyledTableRow(table, table.getRows().size(), styleIndex);
                }
                List<XSLFTableCell> cells = table.getRows().get(outputIndex).getCells();
                String[] fields = {"no", "mode", "category", "metric", "projectValue", "requirement"};
                for (int columnIndex = 0; columnIndex < fields.length && columnIndex < cells.size(); columnIndex++) {
                    set(cells.get(columnIndex), n(outputRow.get(fields[columnIndex]), ""));
                }
                outputIndex++;
            }
            for (int rowIndex = table.getRows().size() - 1; rowIndex >= outputIndex; rowIndex--) {
                table.removeRow(rowIndex);
            }
            rebuildBenefitAnalysisMerges(table, writtenRows);
            return true;
        }
        if (table.getRows().size() < 24) return false;
        Object raw = sectionMap(project).get("benefitAnalysisValues");
        Map<String, Object> values = raw instanceof Map<?, ?> map
                ? (Map<String, Object>) map : Map.of();
        for (int index = 0; index < 23; index++) {
            XSLFTableRow row = table.getRows().get(index + 1);
            if (row.getCells().size() > 4) {
                set(row.getCells().get(4), n(values.get(String.format("row%02d", index + 1)), ""));
            }
        }
        return true;
    }

    private void rebuildBenefitAnalysisMerges(XSLFTable table, List<Map<String,Object>> rows) {
        if (table.getRows().isEmpty()) return;
        table.mergeCells(0, 0, 0, 1);
        int groupStart = 1;
        while (groupStart < table.getRows().size()) {
            Map<String,Object> startRow=rows.get(groupStart-1);
            String no = n(startRow.get("no"), "");
            String mode = n(startRow.get("mode"), "");
            int groupEnd = groupStart;
            while (groupEnd + 1 < table.getRows().size()) {
                Map<String,Object> next=rows.get(groupEnd);
                if (!no.equals(n(next.get("no"), "")) || !mode.equals(n(next.get("mode"), ""))) break;
                groupEnd++;
            }
            if (no.isBlank()) {
                set(table.getRows().get(groupStart).getCells().get(0), mode);
                set(table.getRows().get(groupStart).getCells().get(1), "");
                table.mergeCells(groupStart, groupEnd, 0, 1);
            } else {
                if (groupEnd > groupStart) {
                    table.mergeCells(groupStart, groupEnd, 0, 0);
                    table.mergeCells(groupStart, groupEnd, 1, 1);
                }
            }
            int categoryStart = groupStart;
            while (categoryStart <= groupEnd) {
                String category = n(rows.get(categoryStart-1).get("category"), "");
                int categoryEnd = categoryStart;
                while (categoryEnd + 1 <= groupEnd
                        && category.equals(n(rows.get(categoryEnd).get("category"), ""))) {
                    categoryEnd++;
                }
                if (!category.isBlank() && categoryEnd > categoryStart) {
                    table.mergeCells(categoryStart, categoryEnd, 2, 2);
                }
                categoryStart = categoryEnd + 1;
            }
            groupStart = groupEnd + 1;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean fillPreDecisionTable(XSLFTable table, Map<String, Object> project) {
        if (table.getRows().isEmpty() || table.getRows().get(0).getCells().size() < 7) {
            return false;
        }
        Object dynamicRaw = sectionMap(project).get("preDecisionComparisonRows");
        if (dynamicRaw instanceof List<?> dynamicRows) {
            clearTableMerges(table);
            int outputIndex = 1;
            List<Map<String,Object>> writtenRows = new ArrayList<>();
            for (Object rawRow : dynamicRows) {
                Map<String, Object> rowValue = mapOf(rawRow);
                if (rowValue.isEmpty()) continue;
                writtenRows.add(rowValue);
                if (outputIndex >= table.getRows().size()) {
                    insertStyledTableRow(table, table.getRows().size(), Math.max(1, table.getRows().size() - 1));
                }
                String[] fields = {"no", "mode", "category", "metric", "preDecision", "initiation", "trend"};
                List<XSLFTableCell> cells = table.getRows().get(outputIndex).getCells();
                for (int columnIndex = 0; columnIndex < fields.length && columnIndex < cells.size(); columnIndex++) {
                    set(cells.get(columnIndex), n(rowValue.get(fields[columnIndex]), ""));
                }
                outputIndex++;
            }
            for (int rowIndex = table.getRows().size() - 1; rowIndex >= outputIndex; rowIndex--) table.removeRow(rowIndex);
            rebuildBenefitAnalysisMerges(table, writtenRows);
            return true;
        }
        if (table.getRows().size() < 13) return false;
        Map<String, Object> values = mapOf(sectionMap(project).get("preDecisionValues"));
        for (int index = 0; index < 12; index++) {
            Map<String, Object> rowValue = mapOf(values.get(String.format("row%02d", index + 1)));
            List<XSLFTableCell> cells = table.getRows().get(index + 1).getCells();
            if (cells.size() > 4) set(cells.get(4), n(rowValue.get("preDecision"), ""));
            if (cells.size() > 5) set(cells.get(5), n(rowValue.get("initiation"), ""));
            if (cells.size() > 6) set(cells.get(6), n(rowValue.get("trend"), ""));
        }
        return true;
    }

    private boolean fillEconomicBenefitTable(
            XSLFTable table,
            Map<String, Object> project,
            String sectionKey) {
        if (table.getRows().size() < 20 || table.getRows().get(0).getCells().size() < 12) {
            return false;
        }
        String[] keys = {"initialInvestment", "revenue", "expense", "terminalResources",
                "depreciation", "totalProfit", "netProfit", "netProfitRate",
                "depreciationAddBack", "netCashInflow", "cumulativeNetInflow",
                "discountRate", "discountedCashInflow", "discountedCashOutflow",
                "discountedNetInflow", "discountedCumulativeNetInflow",
                "staticPayback", "dynamicPayback"};
        Map<String, Object> values = mapOf(sectionMap(project).get(sectionKey));
        for (int rowIndex = 0; rowIndex < keys.length; rowIndex++) {
            Object raw = values.get(keys[rowIndex]);
            List<?> rowValues = raw instanceof List<?> list ? list : List.of();
            List<XSLFTableCell> cells = table.getRows().get(rowIndex + 2).getCells();
            for (int columnIndex = 0; columnIndex < 11 && columnIndex + 1 < cells.size(); columnIndex++) {
                Object value = columnIndex < rowValues.size() ? rowValues.get(columnIndex) : "";
                set(cells.get(columnIndex + 1), n(value, ""));
            }
        }
        return true;
    }

    /**
     * Replaces the workbook already attached to the fixed Excel icon on the benefit-analysis
     * slide. The slide object and its anchor stay untouched; only the embedded package bytes
     * are updated for the current project.
     */
    private void replaceEmbeddedWorkbooks(XMLSlideShow ppt, Map<String, Object> project) throws Exception {
        byte[] benefitWorkbookBytes = buildEmbeddedBenefitWorkbook(project);
        byte[] financeDetailWorkbookBytes = buildEmbeddedFinanceDetailWorkbook(project);
        boolean benefitReplaced = false;
        boolean financeDetailReplaced = false;
        for (XSLFSlide slide : ppt.getSlides()) {
            PackagePart slidePart = slide.getPackagePart();
            boolean financeDetailSlide = containsText(slide.getShapes(), "总投入");
            for (PackageRelationship relationship : slidePart.getRelationshipsByType(PACKAGE_RELATIONSHIP)) {
                PackagePart embedded = slidePart.getRelatedPart(relationship);
                byte[] replacement = null;
                if (isBenefitWorkbook(embedded)) {
                    replacement = benefitWorkbookBytes;
                    benefitReplaced = true;
                } else if (financeDetailSlide) {
                    replacement = financeDetailWorkbookBytes;
                    financeDetailReplaced = true;
                }
                if (replacement != null) {
                    try (OutputStream output = embedded.getOutputStream()) {
                        output.write(replacement);
                    }
                }
            }
        }
        if (!benefitReplaced) {
            throw new IllegalStateException("PPT模板未找到项目收益测算Excel嵌入对象");
        }
        if (!financeDetailReplaced) {
            throw new IllegalStateException("PPT模板未找到投入情况收支明细Excel嵌入对象");
        }
    }

    private boolean containsText(List<XSLFShape> shapes, String marker) {
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFTextShape text && text.getText().contains(marker)) return true;
            if (shape instanceof XSLFGroupShape group && containsText(group.getShapes(), marker)) return true;
        }
        return false;
    }

    private boolean isBenefitWorkbook(PackagePart part) {
        try (InputStream input = part.getInputStream(); XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            return workbook.getSheet("政企项目投资效益评估表") != null
                    && workbook.getSheet("折旧和摊销计算表") != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private byte[] buildEmbeddedBenefitWorkbook(Map<String, Object> project) throws Exception {
        try (InputStream template = InitiationPptService.class.getResourceAsStream(BENEFIT_WORKBOOK_RESOURCE)) {
            if (template == null) {
                throw new IllegalStateException("项目收益测算Excel模板未随系统发布");
            }
            try (XSSFWorkbook workbook = new XSSFWorkbook(template);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                fillEmbeddedBenefitWorkbook(workbook, project);
                workbook.setForceFormulaRecalculation(true);
                workbook.write(output);
                return output.toByteArray();
            }
        }
    }

    private byte[] buildEmbeddedFinanceDetailWorkbook(Map<String, Object> project) throws Exception {
        try (InputStream template = InitiationPptService.class.getResourceAsStream(FINANCE_DETAIL_WORKBOOK_RESOURCE)) {
            if (template == null) throw new IllegalStateException("收支明细Excel模板未随系统发布");
            try (XSSFWorkbook workbook = new XSSFWorkbook(template);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                fillFullFinanceDetailSheet(workbook, project);
                workbook.setForceFormulaRecalculation(true);
                workbook.write(output);
                return output.toByteArray();
            }
        }
    }

    private void fillFullFinanceDetailSheet(XSSFWorkbook workbook, Map<String, Object> project) {
        Sheet sheet = workbook.getSheet("sheet1");
        if (sheet == null) throw new IllegalStateException("收支明细Excel模板缺少sheet1工作表");
        clearFinanceTemplateInputs(sheet, 4, 20);
        clearFinanceTemplateInputs(sheet, 28, 48);
        Map<String, int[]> costRows = new LinkedHashMap<>();
        costRows.put("投资部分", new int[]{4, 5});
        costRows.put("合作服务模式", new int[]{7, 8, 9, 10});
        costRows.put("购销模式", new int[]{12});
        costRows.put("其他成本", new int[]{13, 14, 15, 16, 17, 18});
        costRows.put("其他部分", new int[]{20});
        Map<String, int[]> incomeRows = new LinkedHashMap<>();
        incomeRows.put("投资模式", new int[]{28, 29, 30, 31, 32});
        incomeRows.put("合作服务模式", new int[]{34, 35, 36, 37});
        incomeRows.put("购销模式", new int[]{39});
        incomeRows.put("受托代销模式", new int[]{40, 41});
        incomeRows.put("其他收入", new int[]{42, 43, 44, 45, 46, 47, 48});
        fillFinanceTemplateRows(sheet, project.get("costItems"), costRows);
        fillFinanceTemplateRows(sheet, project.get("incomeItems"), incomeRows);
        // Rebuild subtotal/total formulas because the supplied reference has a few
        // hand-edited ranges. These formulas are the canonical mode boundaries.
        setFormula(sheet, 6, 4, "SUM(D4:D5)"); setFormula(sheet, 6, 6, "SUM(F4:F5)");
        setFormula(sheet, 11, 4, "SUM(D7:D10)"); setFormula(sheet, 11, 6, "SUM(F7:F10)");
        setFormula(sheet, 19, 4, "SUM(D13:D18)"); setFormula(sheet, 19, 6, "SUM(F13:F18)");
        setFormula(sheet, 21, 4, "SUM(D4:D5,D7:D10,D12:D18,D20)");
        setFormula(sheet, 21, 6, "SUM(F4:F5,F7:F10,F12:F18,F20)");
        setFormula(sheet, 33, 4, "SUM(D28:D32)"); setFormula(sheet, 33, 6, "SUM(F28:F32)");
        setFormula(sheet, 38, 4, "SUM(D34:D37)"); setFormula(sheet, 38, 6, "SUM(F34:F37)");
        setFormula(sheet, 49, 4, "SUM(D42:D48)"); setFormula(sheet, 49, 6, "SUM(F42:F48)");
        setFormula(sheet, 50, 4, "SUM(D28:D32,D34:D37,D39:D48)");
        setFormula(sheet, 50, 6, "SUM(F28:F32,F34:F37,F39:F48)");
        workbook.setActiveSheet(0);
    }

    private void clearFinanceTemplateInputs(Sheet sheet, int firstRow, int lastRow) {
        for (int rowNumber = firstRow; rowNumber <= lastRow; rowNumber++) {
            if (Set.of(6, 11, 19).contains(rowNumber)) continue;
            cell(sheet, rowNumber, 4).setBlank();
            cell(sheet, rowNumber, 5).setBlank();
            cell(sheet, rowNumber, 7).setBlank();
            cell(sheet, rowNumber, 6).setCellFormula("D" + rowNumber + "/(1+E" + rowNumber + ")");
        }
    }

    private void fillFinanceTemplateRows(Sheet sheet, Object rawItems, Map<String, int[]> rowsByMode) {
        Map<String, Integer> offsets = new HashMap<>();
        for (Object raw : rawItems instanceof List<?> list ? list : List.of()) {
            Map<String, Object> item = mapOf(raw);
            String mode = n(item.get("mode"), "").trim();
            String name = n(item.get("name"), "").trim();
            if (name.isBlank() || "小计".equals(name) || "合计".equals(mode)) continue;
            int[] availableRows = rowsByMode.get(mode);
            if (availableRows == null) continue;
            int offset = offsets.getOrDefault(mode, 0);
            if (offset >= availableRows.length) {
                throw new IllegalStateException("收支明细Excel模板中“" + mode + "”预留行不足");
            }
            int rowNumber = availableRows[offset];
            offsets.put(mode, offset + 1);
            cell(sheet, rowNumber, 3).setCellValue(name);
            setStyledNumber(cell(sheet, rowNumber, 4), item.get("amountIncTax"),
                    cell(sheet, rowNumber, 4).getCellStyle(), false);
            setStyledNumber(cell(sheet, rowNumber, 5), item.get("taxRate"),
                    cell(sheet, rowNumber, 5).getCellStyle(), true);
            cell(sheet, rowNumber, 6).setCellFormula("D" + rowNumber + "/(1+E" + rowNumber + ")");
            cell(sheet, rowNumber, 7).setCellValue(n(item.get("description"), ""));
        }
    }

    private void setFormula(Sheet sheet, int rowNumber, int columnNumber, String formula) {
        cell(sheet, rowNumber, columnNumber).setCellFormula(formula);
    }

    private int writeFinanceSection(Sheet sheet, int rowNumber, String titleText,
                                    String incTaxHeader, String exTaxHeader, String itemKey,
                                    Map<String, Object> project, CellStyle titleStyle,
                                    CellStyle headerStyle, CellStyle amountStyle, CellStyle rateStyle) {
        Row title = sheet.createRow(rowNumber - 1);
        title.createCell(0).setCellValue(titleText);
        title.getCell(0).setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNumber - 1, rowNumber - 1, 0, 5));
        String[] headers = {"模式", "明细", incTaxHeader, "税率（%）", exTaxHeader, "说明"};
        Row header = sheet.createRow(rowNumber);
        for (int column = 0; column < headers.length; column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(headerStyle);
        }
        return writeFinanceDetailRows(sheet, rowNumber + 2, itemKey, project, amountStyle, rateStyle);
    }

    private int writeFinanceDetailRows(Sheet sheet, int rowNumber, String itemKey,
                                       Map<String, Object> project,
                                       CellStyle amountStyle, CellStyle rateStyle) {
        Object rawItems = project.get(itemKey);
        List<?> items = rawItems instanceof List<?> list ? list : List.of();
        Set<String> selectedModes = selectedFinanceModes(project, itemKey);
        for (Object raw : items) {
            Map<String, Object> item = mapOf(raw);
            String name = n(item.get("name"), "").trim();
            String mode = n(item.get("mode"), "").trim();
            if (name.isBlank() || "小计".equals(name) || "合计".equals(mode)) continue;
            if (!selectedModes.isEmpty() && !selectedModes.contains(mode)) continue;
            if (decimalValue(item.get("amountIncTax")).compareTo(BigDecimal.ZERO) == 0
                    && decimalValue(item.get("amountExTax")).compareTo(BigDecimal.ZERO) == 0) continue;
            Row row = sheet.createRow(rowNumber - 1);
            row.createCell(0).setCellValue(mode);
            row.createCell(1).setCellValue(name);
            setStyledNumber(row.createCell(2), item.get("amountIncTax"), amountStyle, false);
            setStyledNumber(row.createCell(3), item.get("taxRate"), rateStyle, true);
            if (n(item.get("amountExTax"), "").isBlank()) {
                row.createCell(4).setCellFormula("C" + rowNumber + "/(1+D" + rowNumber + ")");
                row.getCell(4).setCellStyle(amountStyle);
            } else {
                setStyledNumber(row.createCell(4), item.get("amountExTax"), amountStyle, false);
            }
            row.createCell(5).setCellValue(n(item.get("description"), ""));
            rowNumber++;
        }
        return rowNumber;
    }

    private Set<String> selectedFinanceModes(Map<String, Object> project, String itemKey) {
        Set<String> selected = new HashSet<>();
        if ("incomeItems".equals(itemKey)) {
            selected.addAll(sectionStrings(project, "revenueModes"));
            return selected;
        }
        List<String> primaryModes = sectionStrings(project, "costPrimaryModes");
        if (primaryModes.contains("投资部分")) selected.add("投资部分");
        if (primaryModes.contains("成本部分")) {
            selected.addAll(sectionStrings(project, "cooperationCostModes"));
        }
        if (primaryModes.contains("其他部分")) selected.add("其他部分");
        return selected;
    }

    private void setStyledNumber(Cell cell, Object value, CellStyle style, boolean percentInput) {
        BigDecimal number = decimalValue(value);
        if (n(value, "").isBlank()) cell.setBlank();
        else cell.setCellValue(percentInput ? number.movePointLeft(2).doubleValue() : number.doubleValue());
        CellStyle preciseStyle = cell.getSheet().getWorkbook().createCellStyle();
        preciseStyle.cloneStyleFrom(style);
        String raw = n(value, "").trim();
        int dot = raw.indexOf('.');
        int scale = dot < 0 ? 0 : Math.min(14, raw.length() - dot - 1);
        String format = scale == 0 ? "0" : "0." + "0".repeat(scale);
        if (percentInput) format += "%";
        preciseStyle.setDataFormat(cell.getSheet().getWorkbook().createDataFormat().getFormat(format));
        cell.setCellStyle(preciseStyle);
    }

    private void fillEmbeddedBenefitWorkbook(XSSFWorkbook workbook, Map<String, Object> project) {
        Sheet assessment = workbook.getSheet("政企项目投资效益评估表");
        Sheet depreciation = workbook.getSheet("折旧和摊销计算表");
        if (assessment == null || depreciation == null) {
            throw new IllegalStateException("项目收益测算Excel模板缺少必要工作表");
        }

        setCellText(assessment, 3, 4, n(project.get("projectName"), ""));
        setCellNumber(assessment, 4, 4, project.get("agreementYears"));

        String[] detailKeys = {
                "incomeIntegration", "incomeMaintenance", "incomeTelecomBasic",
                "incomeTelecomValueAdded", "incomeFunction", "incomeEquipmentLease",
                "incomeEquipmentSale", "incomeTerminalSale", "incomeTerminalLease", "incomeOther",
                "investmentPlatformHardware", "investmentPlatformSoftware",
                "investmentPlatformIntegration", "investmentPlatformSupporting",
                "investmentPlatformInstallation", "investmentPlatformDesign",
                "investmentTransmissionPipeline", "investmentTransmissionCable",
                "investmentTransmissionEquipment", "investmentOtherConstruction",
                "costConstructionEquipment", "costConstructionIntegration", "costConstructionBid",
                "costConstructionInstallation", "costConstructionOther", "costOperationMaintenance",
                "costOperationRenewal", "costOperationOther", "costDirectMarketing",
                "costDirectCommission", "costDirectTelecomSettlement", "costDirectOther",
                "costNetworkAllocation", "costGroupProduct"
        };
        int[] detailRows = {
                7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 26, 27, 28, 30,
                33, 34, 35, 36, 37, 39, 40, 41, 43, 44, 45, 46, 47, 48
        };
        Map<String, Object> detailValues = mapOf(sectionMap(project).get("economicBaseDetailValues"));
        for (int index = 0; index < detailKeys.length; index++) {
            Map<String, Object> detail = mapOf(detailValues.get(detailKeys[index]));
            setCellNumber(assessment, detailRows[index], 6, detail.get("ctIncTax"));
            setCellNumber(assessment, detailRows[index], 7, detail.get("itIncTax"));
        }

        Map<String, Object> benefitInputs = mapOf(sectionMap(project).get("economicBenefitInputs"));
        Map<String, Object> benefitValues = mapOf(sectionMap(project).get("economicBenefitValues"));
        writeSignedAnnualValues(assessment, 54,
                benefitInputs.getOrDefault("initialInvestment", benefitValues.get("initialInvestment")), -1);
        writeSignedAnnualValues(assessment, 55,
                benefitInputs.getOrDefault("revenue", benefitValues.get("revenue")), 1);
        writeSignedAnnualValues(assessment, 56,
                benefitInputs.getOrDefault("expense", benefitValues.get("expense")), -1);
        writeSignedAnnualValues(assessment, 57,
                benefitInputs.getOrDefault("terminalResources", benefitValues.get("terminalResources")), -1);

        Map<String, Object> schedule = mapOf(sectionMap(project).get("depreciationSchedule"));
        String[] categoryKeys = {"platform", "transmission", "fiberPipeline", "software", "other"};
        int[] commissioningRows = {6, 22, 38, 54, 70};
        int[] investmentRows = {7, 23, 39, 55, 71};
        for (int index = 0; index < categoryKeys.length; index++) {
            Map<String, Object> category = mapOf(schedule.get(categoryKeys[index]));
            writeHorizontalNumbers(depreciation, commissioningRows[index], 5,
                    category.get("commissioningMonths"), 20);
            writeHorizontalNumbers(depreciation, investmentRows[index], 5,
                    category.get("investments"), 11);
        }
    }

    private void writeSignedAnnualValues(Sheet sheet, int rowNumber, Object rawValues, int sign) {
        List<?> values = rawValues instanceof List<?> list ? list : List.of();
        for (int index = 0; index < 10; index++) {
            Object raw = index < values.size() ? values.get(index) : null;
            if (n(raw, "").isBlank()) {
                setCellNumber(sheet, rowNumber, 2 + index, null);
            } else {
                setCellNumber(sheet, rowNumber, 2 + index,
                        decimalValue(raw).abs().multiply(BigDecimal.valueOf(sign)));
            }
        }
    }

    private void writeHorizontalNumbers(Sheet sheet, int rowNumber, int firstColumn,
                                        Object rawValues, int limit) {
        List<?> values = rawValues instanceof List<?> list ? list : List.of();
        for (int index = 0; index < limit; index++) {
            Object value = index < values.size() ? values.get(index) : null;
            setCellNumber(sheet, rowNumber, firstColumn + index, value);
        }
    }

    private void setCellText(Sheet sheet, int rowNumber, int columnNumber, String value) {
        cell(sheet, rowNumber, columnNumber).setCellValue(value == null ? "" : value);
    }

    private void setCellNumber(Sheet sheet, int rowNumber, int columnNumber, Object value) {
        Cell cell = cell(sheet, rowNumber, columnNumber);
        String text = n(value, "").trim().replace(",", "").replace("%", "");
        if (text.isBlank()) {
            cell.setBlank();
            return;
        }
        try {
            cell.setCellValue(Double.parseDouble(text));
        } catch (NumberFormatException ignored) {
            cell.setBlank();
        }
    }

    private Cell cell(Sheet sheet, int rowNumber, int columnNumber) {
        Row row = sheet.getRow(rowNumber - 1);
        if (row == null) row = sheet.createRow(rowNumber - 1);
        Cell cell = row.getCell(columnNumber - 1);
        return cell == null ? row.createCell(columnNumber - 1) : cell;
    }

    @SuppressWarnings("unchecked")
    private boolean fillFinanceTable(XSLFTable table, Map<String, Object> project, String itemKey) {
        Object raw = project.get(itemKey);
        List<?> items = raw instanceof List<?> list ? list : List.of();
        int detailColumn = 1;
        if (!table.getRows().isEmpty()) {
            List<XSLFTableCell> headerCells = table.getRows().get(0).getCells();
            for (int i = 0; i < headerCells.size(); i++) {
                if (normalize(headerCells.get(i).getText()).contains(normalize("\u660e\u7ec6"))) {
                    detailColumn = i;
                    break;
                }
            }
        }
        List<?> effectiveItems = financeItemsWithSubtotals(items);
        boolean changed = false;
        java.util.Set<Integer> usedRows = new java.util.HashSet<>();
        for (Object itemValue : effectiveItems) {
            if (!(itemValue instanceof Map<?, ?> rawItem)) {
                continue;
            }
            Map<String, Object> item = (Map<String, Object>) rawItem;
            String itemName = n(item.get("name"), "");
            if (itemName.isBlank()) {
                continue;
            }
            int targetRow = -1;
            // The web form is authoritative. Consume template body rows in the
            // same order as the CRUD result instead of matching template sample
            // labels, which would drop renamed or newly inserted details.
            for (int rowIndex = 1; rowIndex < table.getRows().size(); rowIndex++) {
                if (usedRows.contains(rowIndex)) continue;
                List<XSLFTableCell> cells = table.getRows().get(rowIndex).getCells();
                boolean totalRow = cells.stream().anyMatch(cell -> "合计".equals(n(cell.getText(), "").trim()));
                if (!totalRow && cells.size() > detailColumn) { targetRow = rowIndex; break; }
            }
            if (targetRow < 0) targetRow = insertFinanceRowBeforeTotal(table);
            if (targetRow >= 0) {
                List<XSLFTableCell> cells = table.getRows().get(targetRow).getCells();
                String mode = n(item.get("mode"), "");
                if ("costItems".equals(itemKey) && detailColumn >= 2) {
                    String section = costSection(mode);
                    if (!cells.isEmpty()) set(cells.get(0), section);
                    if (cells.size() > 1) set(cells.get(1), "成本部分".equals(section) ? mode : "");
                } else if (!cells.isEmpty()) {
                    set(cells.get(0), mode);
                }
                set(cells.get(detailColumn), itemName);
                if (cells.size() > detailColumn + 1) {
                    set(cells.get(detailColumn + 1), money2(item.get("amountIncTax")));
                }
                if (cells.size() > detailColumn + 2) {
                    set(cells.get(detailColumn + 2), n(item.get("taxRate"), ""));
                }
                if (cells.size() > detailColumn + 3) {
                    set(cells.get(detailColumn + 3), money2(item.get("amountExTax")));
                }
                if (cells.size() > detailColumn + 4) {
                    set(cells.get(detailColumn + 4), n(item.get("description"), ""));
                }
                usedRows.add(targetRow);
                changed = true;
            }
        }
        // Template detail rows are examples only. The exported deck must contain
        // exactly the rows entered in the system, plus derived subtotals/total.
        for (int rowIndex = table.getRows().size() - 1; rowIndex >= 1; rowIndex--) {
            List<XSLFTableCell> cells = table.getRows().get(rowIndex).getCells();
            boolean totalRow = cells.stream().anyMatch(cell -> normalize(cell.getText()).equals(normalize("\u5408\u8ba1")));
            if (!totalRow && !usedRows.contains(rowIndex)) table.removeRow(rowIndex);
        }
        String totalKey = "costItems".equals(itemKey) ? "totalCostIncTax" : "totalRevenueIncTax";
      BigDecimal totalIncTax = BigDecimal.ZERO;
BigDecimal totalExTax = BigDecimal.ZERO;
for (Object rawItem : items) {
    if (rawItem instanceof Map<?, ?> item) {
        String name = n(item.get("name"), "");
        if ("\u5c0f\u8ba1".equals(name)) continue;
        totalIncTax = totalIncTax.add(decimalValue(item.get("amountIncTax")));
        totalExTax = totalExTax.add(decimalValue(item.get("amountExTax")));
    }
}
for (int rowIndex = table.getRows().size() - 1; rowIndex >= 0; rowIndex--) {
    List<XSLFTableCell> cells = table.getRows().get(rowIndex).getCells();
    boolean totalRow = cells.stream()
            .anyMatch(cell -> normalize(cell.getText()).equals(normalize("\u5408\u8ba1")));
    if (totalRow && cells.size() > detailColumn + 1) {
        set(cells.get(detailColumn + 1), money2(totalIncTax));
        if (cells.size() > detailColumn + 2) set(cells.get(detailColumn + 2), "");
        if (cells.size() > detailColumn + 3) set(cells.get(detailColumn + 3), money2(totalExTax));
        if (cells.size() > detailColumn + 4) set(cells.get(detailColumn + 4), "");
        changed = true;
        break;
    }
}




//        BigDecimal totalExTax = BigDecimal.ZERO;
//        for (Object rawItem : items) {
//            if (rawItem instanceof Map<?, ?> item) totalExTax = totalExTax.add(decimalValue(item.get("amountExTax")));
//        }
//        for (int rowIndex = table.getRows().size() - 1; rowIndex >= 0; rowIndex--) {
//            List<XSLFTableCell> cells = table.getRows().get(rowIndex).getCells();
//            boolean totalRow = cells.stream()
//                    .anyMatch(cell -> normalize(cell.getText()).equals(normalize("\u5408\u8ba1")));
//            if (totalRow && cells.size() > detailColumn + 1) {
        
        
        
//                set(cells.get(detailColumn + 1), money(project.get(totalKey)));
//                if (cells.size() > detailColumn + 2) set(cells.get(detailColumn + 2), "");
//                if (cells.size() > detailColumn + 3) set(cells.get(detailColumn + 3), money(totalExTax));
//                if (cells.size() > detailColumn + 4) set(cells.get(detailColumn + 4), "");
//                changed = true;
//                break;
//            }
//        }
        rebuildFinanceMerges(table, itemKey, detailColumn);
        return changed;
    }

    private int insertFinanceRowBeforeTotal(XSLFTable table) {
        int totalRow = table.getRows().size();
        for (int rowIndex = 1; rowIndex < table.getRows().size(); rowIndex++) {
            if (table.getRows().get(rowIndex).getCells().stream()
                    .anyMatch(cell -> "合计".equals(n(cell.getText(), "").trim()))) {
                totalRow = rowIndex;
                break;
            }
        }
        int styleRowIndex = Math.max(1, Math.min(totalRow - 1, table.getRows().size() - 1));
        insertStyledTableRow(table, totalRow, styleRowIndex);
        return totalRow;
    }

    private XSLFTableRow insertStyledTableRow(XSLFTable table, int rowIndex, int styleRowIndex) {
        XSLFTableRow styleRow = table.getRows().get(styleRowIndex);
        XSLFTableRow inserted = table.insertRow(rowIndex);
        inserted.setHeight(styleRow.getHeight());
        int cells = Math.min(inserted.getXmlObject().sizeOfTcArray(), styleRow.getXmlObject().sizeOfTcArray());
        for (int i = 0; i < cells; i++) {
            // Do not replace the complete CTTableCell. XSLFTableRow keeps
            // high-level cell wrappers for the cells created by insertRow();
            // replacing their XML objects disconnects those wrappers and a
            // later getText()/setText() throws XmlValueDisconnectedException.
            // Cell appearance is carried by tcPr, so copy only that subtree
            // and keep the inserted cell/text body connected.
            var sourceCell = styleRow.getXmlObject().getTcArray(i);
            var targetCell = inserted.getXmlObject().getTcArray(i);
            if (sourceCell.isSetTcPr()) {
                targetCell.setTcPr(sourceCell.getTcPr());
            }
        }
        return inserted;
    }

    private void clearTableMerges(XSLFTable table) {
        for (var row : table.getCTTable().getTrList()) {
            for (var cell : row.getTcList()) {
                if (cell.isSetRowSpan()) cell.unsetRowSpan();
                if (cell.isSetGridSpan()) cell.unsetGridSpan();
                if (cell.isSetHMerge()) cell.unsetHMerge();
                if (cell.isSetVMerge()) cell.unsetVMerge();
            }
        }
    }

    private void rebuildFinanceMerges(XSLFTable table, String itemKey, int detailColumn) {
        // Removing template rows does not update DrawingML's rowSpan/vMerge
        // attributes. Clear every inherited merge and rebuild it from the
        // rows that actually survived, otherwise PowerPoint renders totals and
        // newly added rows inside stale template merge regions.
        clearTableMerges(table);
        if (table.getRows().isEmpty()) return;

        if ("costItems".equals(itemKey) && detailColumn >= 2) {
            table.mergeCells(0, 0, 0, 1);
        }

        int totalRow = -1;
        for (int rowIndex = 1; rowIndex < table.getRows().size(); rowIndex++) {
            List<XSLFTableCell> cells = table.getRows().get(rowIndex).getCells();
            if (cells.stream().anyMatch(cell -> "合计".equals(n(cell.getText(), "").trim()))) {
                totalRow = rowIndex;
                break;
            }
        }
        int dataEnd = totalRow >= 0 ? totalRow : table.getRows().size();

        if ("costItems".equals(itemKey) && detailColumn >= 2) {
            int sectionStart = 1;
            while (sectionStart < dataEnd) {
                String section = n(table.getRows().get(sectionStart).getCells().get(0).getText(), "");
                int sectionEnd = sectionStart;
                while (sectionEnd + 1 < dataEnd
                        && section.equals(n(table.getRows().get(sectionEnd + 1).getCells().get(0).getText(), ""))) {
                    sectionEnd++;
                }
                if ("成本部分".equals(section)) {
                    if (sectionEnd > sectionStart) table.mergeCells(sectionStart, sectionEnd, 0, 0);
                    int modeStart = sectionStart;
                    while (modeStart <= sectionEnd) {
                        String mode = n(table.getRows().get(modeStart).getCells().get(1).getText(), "");
                        int modeEnd = modeStart;
                        while (modeEnd + 1 <= sectionEnd
                                && mode.equals(n(table.getRows().get(modeEnd + 1).getCells().get(1).getText(), ""))) {
                            modeEnd++;
                        }
                        if (modeEnd > modeStart) table.mergeCells(modeStart, modeEnd, 1, 1);
                        modeStart = modeEnd + 1;
                    }
                } else {
                    for (int rowIndex = sectionStart; rowIndex <= sectionEnd; rowIndex++) {
                        set(table.getRows().get(rowIndex).getCells().get(1), "");
                    }
                    table.mergeCells(sectionStart, sectionEnd, 0, 1);
                }
                sectionStart = sectionEnd + 1;
            }
        } else {
            int modeStart = 1;
            while (modeStart < dataEnd) {
                String mode = n(table.getRows().get(modeStart).getCells().get(0).getText(), "");
                int modeEnd = modeStart;
                while (modeEnd + 1 < dataEnd
                        && mode.equals(n(table.getRows().get(modeEnd + 1).getCells().get(0).getText(), ""))) {
                    modeEnd++;
                }
                if (modeEnd > modeStart) table.mergeCells(modeStart, modeEnd, 0, 0);
                modeStart = modeEnd + 1;
            }
        }

        if (totalRow >= 0) table.mergeCells(totalRow, totalRow, 0, detailColumn);
    }

    private List<Map<String,Object>> financeItemsWithSubtotals(List<?> items) {
        Map<String,List<Map<String,Object>>> groups = new LinkedHashMap<>();
        for (Object raw : items) {
            if (raw instanceof Map<?,?> map) {
                @SuppressWarnings("unchecked") Map<String,Object> item = (Map<String,Object>) map;
                String mode = n(item.get("mode"), "");
                String name = n(item.get("name"), "");
                if (!mode.isBlank() && !name.isBlank()
                        && !"\u5c0f\u8ba1".equals(name)
                        && !"\u5408\u8ba1".equals(mode)) {
                    groups.computeIfAbsent(mode, key -> new java.util.ArrayList<>()).add(item);
                }
            }
        }
        List<Map<String,Object>> result = new java.util.ArrayList<>();
        for (Map.Entry<String,List<Map<String,Object>>> entry : groups.entrySet()) {
            result.addAll(entry.getValue());
            BigDecimal inc = BigDecimal.ZERO, ex = BigDecimal.ZERO;
            for (Map<String,Object> item : entry.getValue()) {
                inc = inc.add(decimalValue(item.get("amountIncTax")));
                ex = ex.add(decimalValue(item.get("amountExTax")));
            }
            Map<String,Object> subtotal = new LinkedHashMap<>();
            subtotal.put("mode", entry.getKey()); subtotal.put("name", "\u5c0f\u8ba1");
            subtotal.put("amountIncTax", inc); subtotal.put("amountExTax", ex);
            result.add(subtotal);
        }
        return result;
    }

    private String costSection(String mode) {
        if ("投资部分".equals(mode)) return "投资部分";
        if (java.util.Set.of("合作服务模式", "购销模式", "其他成本").contains(mode)) return "成本部分";
        if ("其他部分".equals(mode)) return "其他部分";
        return mode;
    }

    private BigDecimal embeddedNumberSum(Object value) {
        BigDecimal sum = BigDecimal.ZERO;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)")
                .matcher(n(value, "").replace(",", "").replace("，", ""));
        while (matcher.find()) sum = sum.add(new BigDecimal(matcher.group()));
        return sum;
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(value)); } catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    private boolean fillVendorRiskTable(XSLFTable table, Map<String, Object> project) {
        if (table.getRows().size() < 2 || table.getRows().get(0).getCells().size() != 12
                || !tableText(table).contains("后向单位名称")) return false;
        List<Map<String, Object>> rows = sectionRows(project, "vendorRiskRows");
        if (rows.isEmpty()) rows = List.of(Map.of());
        resizeBodyRows(table, 2, rows.size(), false);
        String[] keys = {"vendorName", "amountRatio", "smeFlag", "spendAmount", "spendRemark",
                "qualification", "agentLevel", "agentEquipment", "cooperationCount",
                "businessRisk", "evidence", "remark"};
        for (int index = 0; index < rows.size(); index++) {
            List<XSLFTableCell> cells = table.getRows().get(index + 2).getCells();
            for (int column = 0; column < keys.length && column < cells.size(); column++) {
                set(cells.get(column), n(rows.get(index).get(keys[column]), ""));
            }
        }
        return true;
    }

    private void fillVendorRiskSummary(XSLFTextShape shape, Map<String, Object> project, String text) {
        Map<String, Object> sections = sectionMap(project);
        String[][] mappings = {
                {"中小企业保障：", "vendorSmeProtection"},
                {"资质能力情况：", "vendorQualificationSummary"},
                {"往期履约情况：", "vendorPerformanceSummary"},
                {"经营风险情况：", "vendorBusinessRiskSummary"}
        };
        for (String[] mapping : mappings) {
            if (text.contains(mapping[0])) {
                replaceParagraphUnicode(shape, mapping[0], n(sections.get(mapping[1]), ""));
                return;
            }
        }
    }

    private boolean fillCashFlowTable(XSLFTable table, Map<String, Object> project) {
        if (table.getRows().size() < 3 || table.getRows().get(0).getCells().size() != 9
                || !tableText(table).contains("项目现金流情况")) return false;
        double availableHeight = table.getAnchor().getHeight();
        List<Map<String, Object>> rows = sectionRows(project, "cashFlowRows");
        if (rows.isEmpty()) rows = List.of(Map.of("time", "第1年"));
        resizeBodyRows(table, 2, rows.size(), true);
        String[] keys = {"time", "revenueExTax", "revenueIncTax", "receiptIncTax", "expenseIncTax",
                "cashRealizationRate", "description", "investmentIncomeIncTax", "investmentConfirmation"};
        for (int index = 0; index < rows.size(); index++) {
            List<XSLFTableCell> cells = table.getRows().get(index + 2).getCells();
            for (int column = 0; column < keys.length && column < cells.size(); column++) {
                String value = n(rows.get(index).get(keys[column]), "");
                if (index == 0 && column == 0) value = "第1年";
                set(cells.get(column), value);
            }
        }
        List<XSLFTableCell> total = table.getRows().get(table.getRows().size() - 1).getCells();
        set(total.get(0), "合计");
        for (int column = 1; column <= 4; column++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (Map<String, Object> row : rows) sum = sum.add(decimalValue(row.get(keys[column])));
            set(total.get(column), sum.signum() == 0 ? "" : sum.stripTrailingZeros().toPlainString());
        }
        for (int column = 5; column < total.size(); column++) set(total.get(column), "");
        BigDecimal totalRevenueExTax = BigDecimal.ZERO;
        BigDecimal totalReceiptIncTax = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            totalRevenueExTax = totalRevenueExTax.add(decimalValue(row.get("revenueExTax")));
            totalReceiptIncTax = totalReceiptIncTax.add(decimalValue(row.get("receiptIncTax")));
        }
        if (totalRevenueExTax.signum() != 0) {
            set(total.get(5), totalReceiptIncTax.multiply(new BigDecimal("100"))
                    .divide(totalRevenueExTax, 2, RoundingMode.HALF_UP).toPlainString() + "%");
        }
        BigDecimal investmentTotal = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) investmentTotal = investmentTotal.add(decimalValue(row.get("investmentIncomeIncTax")));
        set(total.get(7), investmentTotal.toPlainString());
        if (!hasInvestmentCashFlow(rows)) {
            clearTableMerges(table);
            table.removeColumn(8);
            table.removeColumn(7);
            table.mergeCells(0, 1, 0, 0);
            table.mergeCells(0, 0, 1, 6);
        }
        fitTableRows(table, availableHeight);
        return true;
    }

    private boolean hasInvestmentCashFlow(List<Map<String, Object>> rows) {
        // Confirmation text can remain in legacy records after the investment
        // mode is cleared. Only a real non-zero investment amount keeps the
        // optional investment columns visible.
        return rows.stream().anyMatch(row -> decimalValue(row.get("investmentIncomeIncTax")).signum() != 0);
    }

    private boolean fillRiskAssessmentTable(XSLFTable table, Map<String, Object> project) {
        if (table.getRows().isEmpty() || table.getRows().get(0).getCells().size() != 5
                || !tableText(table).contains("风险类别")) return false;
        List<Map<String, Object>> rows = sectionRows(project, "riskAssessmentRows");
        if (rows.isEmpty()) return true;
        resizeBodyRows(table, 1, rows.size(), false);
        clearTableMerges(table);
        String[] keys = {"category", "riskPoint", "involvedDescription", "controlMeasures", "evaluationResult"};
        for (int index = 0; index < rows.size(); index++) {
            List<XSLFTableCell> cells = table.getRows().get(index + 1).getCells();
            for (int column = 0; column < keys.length; column++) set(cells.get(column), n(rows.get(index).get(keys[column]), ""));
        }
        int start = 0;
        while (start < rows.size()) {
            int end = start;
            String category = n(rows.get(start).get("category"), "");
            while (end + 1 < rows.size() && category.equals(n(rows.get(end + 1).get("category"), ""))) end++;
            if (end > start && !category.isBlank()) table.mergeCells(start + 1, end + 1, 0, 0);
            start = end + 1;
        }
        return true;
    }

    private boolean fillThreeLineTable(XSLFTable table, Map<String, Object> project) {
        if (table.getRows().size() != 4 || table.getRows().get(0).getCells().size() != 5
                || !tableText(table).contains("判定层级")) return false;
        List<Map<String, Object>> allRows = sectionRows(project, "threeLineRows");
        List<Map<String, Object>> rows = List.of("红线", "底线", "高线").stream()
                .map(level -> allRows.stream().filter(row -> level.equals(n(row.get("level"), "")))
                        .findFirst().orElse(Map.of("level", level))).toList();
        Map<String, Integer> templateIndex = Map.of("红线", 1, "底线", 2, "高线", 3);
        for (int index = 0; index < rows.size(); index++) {
            Map<String, Object> row = rows.get(index);
            int sourceIndex = templateIndex.getOrDefault(n(row.get("level"), ""), 1);
            if (index + 1 >= table.getRows().size()) insertStyledTableRow(table, index + 1, sourceIndex);
            List<XSLFTableCell> cells = table.getRows().get(index + 1).getCells();
            List<XSLFTableCell> source = table.getRows().get(sourceIndex).getCells();
            for (int column = 0; column < 3; column++) set(cells.get(column), source.get(column).getText(), source.get(column));
            set(cells.get(3), n(row.get("satisfaction"), ""));
            set(cells.get(4), n(row.get("remark"), ""));
        }
        while (table.getRows().size() > rows.size() + 1) table.removeRow(table.getRows().size() - 1);
        return true;
    }

    private boolean fillDeliveryTable(XSLFTable table, Map<String, Object> project) {
        if (table.getRows().size() < 2 || table.getRows().get(0).getCells().size() != 9
                || !tableText(table).contains("实际实施方")) return false;
        List<String> selectedModes = sectionStrings(project, "deliveryModes");
        List<Map<String, Object>> rows = sectionRows(project, "deliveryRows").stream()
                .filter(row -> selectedModes.isEmpty() || selectedModes.contains(n(row.get("mode"), ""))).toList();
        if (rows.isEmpty()) return true;
        resizeBodyRows(table, 1, rows.size(), true);
        clearTableMerges(table);
        String[] keys = {"no", "mode", "name", "unit", "priceIncTax", "ownCapability", "downstreamUnit", "implementer", "constructionMaintenance"};
        for (int index = 0; index < rows.size(); index++) {
            List<XSLFTableCell> cells = table.getRows().get(index + 1).getCells();
            for (int column = 0; column < keys.length; column++) set(cells.get(column), n(rows.get(index).get(keys[column]), ""));
        }
        int start = 0;
        while (start < rows.size()) {
            int end = start;
            String mode = n(rows.get(start).get("mode"), "");
            while (end + 1 < rows.size() && mode.equals(n(rows.get(end + 1).get("mode"), ""))) end++;
            if (end > start && !mode.isBlank()) table.mergeCells(start + 1, end + 1, 1, 1);
            start = end + 1;
        }
        List<XSLFTableCell> total = table.getRows().get(table.getRows().size() - 1).getCells();
        set(total.get(0), "总计");
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) sum = sum.add(decimalValue(row.get("priceIncTax")));
        set(total.get(4), sum.signum() == 0 ? "" : sum.stripTrailingZeros().toPlainString());
        table.mergeCells(table.getRows().size() - 1, table.getRows().size() - 1, 0, 3);
        return true;
    }

    private boolean fillMaintenanceTable(XSLFTable table, Map<String, Object> project) {
        if (table.getRows().size() != 24 || table.getRows().get(0).getCells().size() != 6 || !tableText(table).contains("业务信息表")) return false;
        Map<String,Object> s = sectionMap(project);
        set(table.getRows().get(1).getCells().get(1), n(s.get("maintenanceCustomerName"), n(project.get("customerName"), "")));
        set(table.getRows().get(1).getCells().get(4), n(s.get("maintenanceCustomerAddress"), ""));
        set(table.getRows().get(2).getCells().get(1), n(s.get("maintenanceContact"), "")); set(table.getRows().get(2).getCells().get(4), n(s.get("maintenancePhone"), ""));
        set(table.getRows().get(3).getCells().get(1), n(s.get("maintenanceCustomerLevel"), "")); set(table.getRows().get(3).getCells().get(4), n(s.get("maintenanceManager"), ""));
        List<Map<String,Object>> rows = sectionRows(project, "maintenanceItems");
        int current = 3;
        while (current < rows.size()) { insertStyledTableRow(table, 6 + current, 6); current++; }
        while (current > rows.size()) { table.removeRow(6 + current - 1); current--; }
        String[] keys={"businessMode","itemName","quantityUnit","agreementYears","managementDepartment","maintenanceUnit"};
        for(int i=0;i<rows.size();i++){var cells=table.getRows().get(6+i).getCells(); for(int c=0;c<6;c++) set(cells.get(c),n(rows.get(i).get(keys[c]),""));}
        int base=6+rows.size();
        String fullCoverage=n(s.get("maintenanceFullCoverage"),"");
        String coverageRemark="否".equals(fullCoverage)?n(s.get("maintenanceFullCoverageRemark"),""):"";
        set(table.getRows().get(base).getCells().get(2),fullCoverage+(coverageRemark.isBlank()?"":"："+coverageRemark));
        set(table.getRows().get(base+4).getCells().get(0), n(s.get("onsiteService"), "")); set(table.getRows().get(base+4).getCells().get(1), n(s.get("onsiteProvider"), "")); set(table.getRows().get(base+4).getCells().get(3), n(s.get("keySupportService"), "")); set(table.getRows().get(base+4).getCells().get(4), n(s.get("keySupportDescription"), ""));
        fillMaintenanceStandard(table,base+6,s,"line"); fillMaintenanceStandard(table,base+11,s,"fiveG");
        return true;
    }
    private void fillMaintenanceStandard(XSLFTable t,int r,Map<String,Object>s,String p){set(t.getRows().get(r).getCells().get(1),n(s.get(p+"ProtectionLevel"),""));set(t.getRows().get(r).getCells().get(4),n(s.get(p+"DualRoute"),""));set(t.getRows().get(r+1).getCells().get(1),n(s.get(p+"RecoveryHours"),""));set(t.getRows().get(r+1).getCells().get(4),n(s.get(p+"AllowedInterruption"),""));set(t.getRows().get(r+2).getCells().get(1),n(s.get(p+"DailyMaintenance"),""));set(t.getRows().get(r+3).getCells().get(1),n(s.get(p+"Supervisor"),""));set(t.getRows().get(r+3).getCells().get(4),n(s.get(p+"Manager"),""));}

    private boolean fillExistingBusinessTable(XSLFTable table, Map<String,Object> project){
        if(table.getRows().isEmpty()||table.getRows().get(0).getCells().size()!=7||!tableText(table).contains("业务提供方"))return false;
        List<String> providers=sectionStrings(project,"existingProviders");
        List<Map<String,Object>> rows=sectionRows(project,"existingBusinessRows").stream().filter(row->providers.isEmpty()||providers.contains(n(row.get("provider"),""))).toList(); if(rows.isEmpty())return true;
        resizeBodyRows(table,1,rows.size(),false); clearTableMerges(table); String[] keys={"provider","businessName","quantity","unit","monthlyFee","annualRevenue","remark"};
        for(int i=0;i<rows.size();i++){var cells=table.getRows().get(i+1).getCells();for(int c=0;c<7;c++)set(cells.get(c),n(rows.get(i).get(keys[c]),""));}
        int st=0;while(st<rows.size()){int en=st;String p=n(rows.get(st).get("provider"),"");while(en+1<rows.size()&&p.equals(n(rows.get(en+1).get("provider"),"")))en++;if(en>st&&!p.isBlank())table.mergeCells(st+1,en+1,0,0);st=en+1;}return true;
    }
    private boolean fillPartnerSelectionTable(XSLFTable table,Map<String,Object> project){
        if(table.getRows().size()<3||table.getRows().get(0).getCells().size()!=3||!tableText(table).contains("合作伙伴甄选结果"))return false;List<Map<String,Object>>rows=sectionRows(project,"partnerSelectionRows");resizeBodyRows(table,2,rows.size(),true);String[]keys={"packageNo","winner","amountExTax"};for(int i=0;i<rows.size();i++){var cells=table.getRows().get(i+2).getCells();for(int c=0;c<3;c++)set(cells.get(c),n(rows.get(i).get(keys[c]),""));}var total=table.getRows().get(table.getRows().size()-1).getCells();set(total.get(0),"合计");BigDecimal sum=BigDecimal.ZERO;for(var row:rows)sum=sum.add(embeddedNumberSum(row.get("amountExTax")));set(total.get(2),sum.signum()==0?"":sum.stripTrailingZeros().toPlainString());return true;
    }
    private boolean fillAppraisalTable(XSLFTable table,Map<String,Object> project){
        if(table.getRows().size()<3||table.getRows().get(0).getCells().size()!=6||!tableText(table).contains("生态报价"))return false;
        List<Map<String,Object>>rows=sectionRows(project,"appraisalRows");resizeBodyRows(table,2,rows.size(),true);clearTableMerges(table);table.mergeCells(0,0,3,4);
        String[]keys={"no","content","ecosystemQuote","reducibleAmount","reducibleRatio","remark"};
        BigDecimal quote=BigDecimal.ZERO,reducible=BigDecimal.ZERO;
        for(int i=0;i<rows.size();i++){var cells=table.getRows().get(i+2).getCells();for(int c=0;c<6;c++)set(cells.get(c),n(rows.get(i).get(keys[c]),""));quote=quote.add(decimalValue(rows.get(i).get("ecosystemQuote")));reducible=reducible.add(decimalValue(rows.get(i).get("reducibleAmount")));}
        int start=0;while(start<rows.size()){int end=start;String no=n(rows.get(start).get("no"),"");while(end+1<rows.size()&&no.equals(n(rows.get(end+1).get("no"),"")))end++;if(end>start&&!no.isBlank())table.mergeCells(start+2,end+2,0,0);start=end+1;}
        var total=table.getRows().get(table.getRows().size()-1).getCells();set(total.get(0),"");set(total.get(1),"合计");set(total.get(2),quote.signum()==0?"":quote.stripTrailingZeros().toPlainString());set(total.get(3),reducible.signum()==0?"":reducible.stripTrailingZeros().toPlainString());set(total.get(4),"");set(total.get(5),"");return true;
    }

    private void resizeBodyRows(XSLFTable table, int headerRows, int detailCount, boolean hasTotal) {
        int wanted = headerRows + detailCount + (hasTotal ? 1 : 0);
        int styleIndex = Math.min(headerRows, table.getRows().size() - 1);
        while (table.getRows().size() < wanted) {
            int insertAt = hasTotal ? table.getRows().size() - 1 : table.getRows().size();
            insertStyledTableRow(table, insertAt, styleIndex);
        }
        while (table.getRows().size() > wanted) {
            int removeAt = hasTotal ? table.getRows().size() - 2 : table.getRows().size() - 1;
            table.removeRow(removeAt);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> sectionRows(Map<String, Object> project, String key) {
        Object value = sectionMap(project).get(key);
        if (!(value instanceof List<?> list)) return new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : list) if (item instanceof Map<?, ?>) rows.add((Map<String, Object>) item);
        return rows;
    }

    private void fillFundAnalysis(XSLFTextShape shape, Map<String, Object> project, String text) {
        Map<String, Object> sections = sectionMap(project);
        String analysis = n(sections.get("fundSource"), "");
        String fundType = n(sections.get("fundType"), "");
        if (text.startsWith("资金类型：") && !fundType.isBlank()) {
            set(shape, "资金类型：" + fundType);
            return;
        }
        if (replaceFundEvidenceDescription(shape, text, sections)) return;
        if (text.startsWith("最终得分")) {
            FundScores scores=fundScores(project);
            set(shape,"最终得分="+scoreText(scores.history())+"×40%+"+scoreText(scores.current())+"×60%+"+scoreText(scores.bonus())+"="+scoreText(scores.finalScore())+"分");
            return;
        }
        if (analysis.isBlank()) return;
        if (text.contains("\u7ecf\u6838\u5b9e\uff0c\u9879\u76ee\u5c5e\u4e8e")
                || text.contains("\u7ecf\u8bc4\u4f30\uff0c\u9879\u76ee\u8d44\u91d1\u98ce\u9669\u7b49\u7ea7")) {
            String level = n(project.get("fundRiskLevel"), "");
            set(shape, analysis + (level.isBlank() ? "" :
                    "\n\u9879\u76ee\u8d44\u91d1\u98ce\u9669\u7b49\u7ea7\uff1a" + level));
        }
    }

    private boolean replaceFundEvidenceDescription(
            XSLFTextShape shape, String text, Map<String, Object> sections) {
        String[][] mappings = {
                {"政府部门预算公开文件", "fundGovernmentBudgetPublicDescription"},
                {"项目对应的支出科目", "fundGovernmentExpenditureSubjectDescription"},
                {"项目对应支出科目", "fundGovernmentExpenditureSubjectDescription"},
                {"对应项目的项目支出绩效目标表", "fundGovernmentPerformanceTargetDescription"},
                {"项目支出绩效目标表", "fundGovernmentPerformanceTargetDescription"},
                {"政采招标网站的政府采购单等", "fundGovernmentProcurementDescription"},
                {"政府采购单等", "fundGovernmentProcurementDescription"},
                {"客户内部立项文件", "fundEnterpriseInternalApprovalDescription"},
                {"客户资金实力研判", "fundEnterpriseFinancialStrengthDescription"}
        };
        for (String[] mapping : mappings) {
            if (!text.contains(mapping[0])) continue;
            String value = n(sections.get(mapping[1]), "");
            if (value.isBlank()) value = n(sections.get("fundProofMaterials"), "");
            if (!value.isBlank()) {
                replaceParagraphUnicode(shape, mapping[0], "：" + value);
            }
            return true;
        }
        return false;
    }

    private boolean fillFundScoreTable(XSLFTable table,Map<String,Object> project){
        String text=tableText(table);Map<String,Object>s=sectionMap(project);FundScores scores=fundScores(project);
        if(text.contains("历史项目欠收分析模型得分")){
            String[][]values={{"fundHistoryIndustryItem","fundHistoryIndustryDeduction"},{"fundHistoryCustomerItem","fundHistoryCustomerDeduction"},{"fundHistorySourceItem","fundHistorySourceDeduction"}};
            for(int i=0;i<values.length;i++){var cells=table.getRows().get(i+2).getCells();set(cells.get(1),n(s.get(values[i][0]),""),cells.get(0));set(cells.get(2),scoreText(s.get(values[i][1])),cells.get(0));}
            XSLFTableCell totalCell=table.getRows().get(5).getCells().get(0);set(totalCell,"本项得分：100-"+scoreText(scores.historyDeduction())+"="+scoreText(scores.history())+"分",table.getRows().get(4).getCells().get(0));setCellFontSize(totalCell,8d);return true;
        }
        if(text.contains("当前项目资金风险分析模型得分")){
            boolean enterprise=table.getRows().size()>6;String[]keys=enterprise?new String[]{"fundCurrentAuditDeduction","fundCurrentHistoryDeduction","fundCurrentSourceDeduction","fundCurrentCounterpartyDeduction"}:new String[]{"fundCurrentHistoryDeduction","fundCurrentSourceDeduction","fundCurrentDebtDeduction"};
            for(int i=0;i<keys.length;i++){var cells=table.getRows().get(i+2).getCells();set(cells.get(1),scoreText(s.get(keys[i])),cells.get(0));}
            set(table.getRows().get(keys.length+2).getCells().get(0),"本项得分：100-"+scoreText(scores.currentDeduction())+"="+scoreText(scores.current())+"分",table.getRows().get(keys.length+1).getCells().get(0));return true;
        }
        if(text.contains("特殊情况加分")){
            var detail=table.getRows().get(2).getCells();set(detail.get(0),n(s.get("fundSpecialBonusType"),""),table.getRows().get(1).getCells().get(0));set(detail.get(1),scoreText(s.get("fundSpecialBonusScore")),table.getRows().get(1).getCells().get(1));set(table.getRows().get(3).getCells().get(0),"本项得分："+scoreText(scores.bonus())+"分",detail.get(0));return true;
        }
        return false;
    }

    private record FundScores(BigDecimal historyDeduction,BigDecimal history,BigDecimal currentDeduction,BigDecimal current,BigDecimal bonus,BigDecimal finalScore){}
    private FundScores fundScores(Map<String,Object>project){
        Map<String,Object>s=sectionMap(project);
        BigDecimal historyDeduction=decimalValue(s.get("fundHistoryIndustryDeduction"))
                .add(decimalValue(s.get("fundHistoryCustomerDeduction")))
                .add(decimalValue(s.get("fundHistorySourceDeduction")));
        BigDecimal currentDeduction=decimalValue(s.get("fundCurrentHistoryDeduction"))
                .add(decimalValue(s.get("fundCurrentSourceDeduction")));
        currentDeduction=currentDeduction.add(isGovernmentFundProject(project)
                ? decimalValue(s.get("fundCurrentDebtDeduction"))
                : decimalValue(s.get("fundCurrentAuditDeduction")).add(decimalValue(s.get("fundCurrentCounterpartyDeduction"))));
        BigDecimal history=BigDecimal.valueOf(100).subtract(historyDeduction).max(BigDecimal.ZERO);
        BigDecimal current=BigDecimal.valueOf(100).subtract(currentDeduction).max(BigDecimal.ZERO);
        BigDecimal bonus=decimalValue(s.get("fundSpecialBonusScore"));
        BigDecimal total=history.multiply(new BigDecimal("0.4")).add(current.multiply(new BigDecimal("0.6"))).add(bonus).min(BigDecimal.valueOf(100));
        return new FundScores(historyDeduction,history,currentDeduction,current,bonus,total);
    }
    private String scoreText(Object value){BigDecimal score=decimalValue(value);return score.signum()==0?"0":score.stripTrailingZeros().toPlainString();}

    private boolean fillClientDueDiligenceTable(XSLFTable table,Map<String,Object> project){
        if(table.getRows().size()!=2||table.getRows().get(0).getCells().size()!=6||!tableText(table).contains("集团名称"))return false;
        Map<String,Object>s=sectionMap(project);String[]keys={"clientGroupName","clientEnterpriseNature","clientEstablishmentDate","clientRegisteredCapital","clientPaidInCapital","clientInsuredEmployees"};int[][]positions={{0,1},{0,3},{0,5},{1,1},{1,3},{1,5}};for(int i=0;i<keys.length;i++){String value=n(s.get(keys[i]),"");if(isGovernmentFundProject(project)&&(i==2||i==3||i==4))value="";set(table.getRows().get(positions[i][0]).getCells().get(positions[i][1]),value);}return true;
    }

    private boolean fillClientPerformanceTable(XSLFTable table,Map<String,Object> project){
        if(table.getRows().isEmpty()||table.getRows().get(0).getCells().size()!=13||!tableText(table).contains("商机编号"))return false;
        double availableHeight=table.getAnchor().getHeight();
        List<Map<String,Object>>rows=sectionRows(project,"clientPastPerformanceRows");
        resizeBodyRows(table,1,rows.size(),true);
        String[]keys={"no","projectName","opportunityNo","signingDate","contractAmount","payableAmount","paidAmount","paymentMethod","paymentRatio","arrearsAmount","overdueArrears","minimumArrearsPeriod","remark"};
        BigDecimal contract=BigDecimal.ZERO,payable=BigDecimal.ZERO,paid=BigDecimal.ZERO,arrears=BigDecimal.ZERO;
        for(int i=0;i<rows.size();i++){
            var cells=table.getRows().get(i+1).getCells();
            for(int c=0;c<keys.length;c++)set(cells.get(c),n(rows.get(i).get(keys[c]),""));
            contract=contract.add(decimalValue(rows.get(i).get("contractAmount")));
            payable=payable.add(decimalValue(rows.get(i).get("payableAmount")));
            paid=paid.add(decimalValue(rows.get(i).get("paidAmount")));
            arrears=arrears.add(decimalValue(rows.get(i).get("arrearsAmount")));
        }
        var total=table.getRows().get(table.getRows().size()-1).getCells();
        for(var cell:total)set(cell,"");
        set(total.get(0),"合计");set(total.get(4),plain(contract));set(total.get(5),plain(payable));set(total.get(6),plain(paid));set(total.get(7),"-");set(total.get(8),"-");set(total.get(9),plain(arrears));
        fitTableRows(table,availableHeight);
        return true;
    }

    private void addCapabilityLabeledParagraph(XSLFTextShape shape, String line) {
        XSLFTextParagraph paragraph = shape.addNewTextParagraph();
        paragraph.setBullet(false);
        paragraph.setLeftMargin(24d);
        paragraph.setIndent(0d);
        paragraph.setSpaceAfter(3d);
        int colon = line.indexOf('\uff1a');
        String label = colon >= 0 ? line.substring(0, colon + 1) : line;
        String answer = colon >= 0 ? line.substring(colon + 1).trim() : "";
        addCapabilityRun(paragraph, "\u2713 " + label, new Color(0x00, 0x70, 0xC0));
        if (!answer.isBlank()) addCapabilityRun(paragraph, answer, Color.BLACK);
    }

    private void addSevenFusionParagraph(XSLFTextShape shape, String summary) {
        XSLFTextParagraph paragraph = shape.addNewTextParagraph();
        paragraph.setBullet(false);
        paragraph.setLeftMargin(0d);
        paragraph.setIndent(0d);
        paragraph.setSpaceAfter(3d);
        addCapabilityRun(paragraph, "\u2713 \u7ecf\u201c\u4e03\u878d\u89e3\u6790\u201d\u540e\uff0c", new Color(0x00, 0x70, 0xC0));
        addCapabilityRun(paragraph, summary, Color.BLACK);
    }

    private void addCapabilityRun(XSLFTextParagraph paragraph, String text, Color color) {
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(text);
        run.setFontFamily("Microsoft YaHei");
        run.setFontSize(16d);
        run.setFontColor(color);
        run.setBold(false);
    }

    private void addDerivedOtherRevenueBenefitGroups(Map<String, Object> project,
            Map<String, String> benefitGroupByRowKey, Set<String> selectedModeKeys) {
        boolean hasRecognizedOtherIncome = false;
        boolean hasUnrecognizedOtherIncome = false;
        for (Object raw : project.get("incomeItems") instanceof List<?> list ? list : List.of()) {
            Map<String, Object> item = mapOf(raw);
            if (!"其他收入".equals(n(item.get("mode"), ""))) continue;
            String rowKey = benefitRowKeyForOtherRevenue(n(item.get("name"), ""));
            if (rowKey == null) hasUnrecognizedOtherIncome = true;
            else {
                hasRecognizedOtherIncome = true;
                selectedModeKeys.add(n(benefitGroupByRowKey.get(rowKey), ""));
            }
        }
        if (hasRecognizedOtherIncome && !hasUnrecognizedOtherIncome) {
            selectedModeKeys.remove(n(benefitGroupByRowKey.get("row23"), ""));
        }
    }

    private String derivedOtherRevenueProfitRate(Map<String, Object> project, String rowKey) {
        String keyword = switch (rowKey) {
            case "row12" -> "视频";
            case "row14" -> "专线";
            case "row15" -> "AI";
            case "row22" -> "自主集成";
            default -> null;
        };
        if (keyword == null) return null;
        BigDecimal revenue = sumFinanceDetailsByName(project.get("incomeItems"), "其他收入", keyword);
        if (revenue.signum() == 0) return null;
        BigDecimal expense = sumFinanceDetailsByName(project.get("costItems"), null, keyword);
        BigDecimal netProfit = revenue.subtract(expense).multiply(new BigDecimal("0.75"));
        return netProfit.multiply(new BigDecimal("100"))
                .divide(revenue, 2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private BigDecimal sumFinanceDetailsByName(Object rawItems, String requiredMode, String keyword) {
        BigDecimal total = BigDecimal.ZERO;
        for (Object raw : rawItems instanceof List<?> list ? list : List.of()) {
            Map<String, Object> item = mapOf(raw);
            if (requiredMode != null && !requiredMode.equals(n(item.get("mode"), ""))) continue;
            if (!n(item.get("name"), "").toLowerCase().contains(keyword.toLowerCase())) continue;
            total = total.add(decimalValue(item.get("amountExTax")));
        }
        return total;
    }

    private String benefitRowKeyForOtherRevenue(String name) {
        if (name.contains("自主集成")) return "row22";
        if (name.contains("专线")) return "row14";
        if (name.contains("云视讯") || name.contains("大视频") || name.contains("视频")) return "row12";
        if (name.toUpperCase().contains("AI")) return "row15";
        return null;
    }

    private void fitTableRows(XSLFTable table,double availableHeight){
        if(table.getRows().isEmpty()||availableHeight<=0)return;
        double rowHeight=availableHeight/table.getRows().size();
        for(XSLFTableRow row:table.getRows())row.setHeight(rowHeight);
    }

    private String plain(BigDecimal value){return value.signum()==0?"":value.stripTrailingZeros().toPlainString();}

    private void fillClientDueDiligenceText(XSLFTextShape shape, Map<String, Object> project, String text) {
        Map<String, Object> s = sectionMap(project);
        replaceParagraphUnicode(shape, "行业类型：", n(s.get("clientIndustryType"), ""));
        replaceParagraphUnicode(shape, "项目需求：", n(s.get("clientProjectDemand"), ""));
        replaceParagraphUnicode(shape, "客户实地拜访：", n(s.get("clientSiteVisit"), ""));
        replaceParagraphUnicode(shape, "项目交付详细地址：", n(s.get("clientDeliveryAddress"), ""));
        normalizeBodyTypography(shape);
    }

    private void normalizeBodyTypography(XSLFTextShape shape) {
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            XSLFTextRun reference = firstTextRun(paragraph.getTextRuns(), 0);
            if (reference == null) continue;
            for (XSLFTextRun run : paragraph.getTextRuns()) {
                if (run.getRawText() == null || run.getRawText().isBlank()) continue;
                if (reference.getFontFamily() != null) run.setFontFamily(reference.getFontFamily());
                if (reference.getFontSize() != null) run.setFontSize(reference.getFontSize());
                if (reference.getFontColor() != null) run.setFontColor(reference.getFontColor());
                run.setBold(true);
            }
        }
    }

    private void fillClientPerformanceText(XSLFTextShape shape, Map<String,Object> project, String text) {
        Map<String,Object> s = sectionMap(project);
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            String line = n(paragraph.getText(), "");
            if (line.contains("该单位前期采用以房抵债方式")) set(paragraph, "该单位前期采用以房抵债方式抵消欠费金额" + n(s.get("clientHousingDebtAmount"), "") + "万元。");
            else if (line.contains("该单位前期与移动合作过")) set(paragraph, n(s.get("clientMobilePerformance"), ""));
            else if (line.contains("与其他单位合作履约情况")) set(paragraph, "与其他单位合作履约情况：" + n(s.get("clientOtherPerformance"), ""));
            else if (line.contains("共计非对称支付后向")) set(paragraph, "前期与该单位合作项目中，共计非对称支付后向" + n(s.get("clientAsymmetricCount"), "") + "次，合计支付金额" + n(s.get("clientAsymmetricAmount"), "") + "万元，占当前全省非对称支付总额的" + n(s.get("clientAsymmetricRatio"), "") + "%。");
            else if (line.contains("拖欠税款被列入")) set(paragraph, riskChoiceSentence(s.get("clientTaxDebtRisk"), "拖欠税款被列入过欠税公告名单、因借款被起诉。"));
            else if (line.contains("企业公示信息弄虚作假")) set(paragraph, riskChoiceSentence(s.get("clientAbnormalOperationRisk"), "企业公示信息弄虚作假而被列入企业经营异常名录、因买卖合同纠纷被起诉。"));
            else if (line.contains("多起劳务纠纷")) set(paragraph, riskChoiceSentence(s.get("clientLaborDisputeRisk"), "多起劳务纠纷被起诉。"));
            else if (line.contains("股权、高管频繁变动")) set(paragraph, riskChoiceSentence(s.get("clientEquityManagementRisk"), "股权、高管频繁变动。"));
        }
    }

    private void clearGovernmentRiskParagraphs(XSLFTextShape shape) {
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            if (isGovernmentRiskAuditText(n(paragraph.getText(), ""))) set(paragraph, "");
        }
    }

    private String riskChoiceSentence(Object raw, String subject) {
        String choice = n(raw, "").trim();
        if (choice.isBlank()) return "";
        if (choice.startsWith("该单位") || choice.length() > 4) return choice;
        return "该单位" + choice + subject;
    }

    private void fillPaymentMethods(XSLFTextShape shape, Map<String, Object> project, String text) {
        boolean receiptShape = text.contains("项目收款方式");
        boolean paymentShape = text.contains("项目付款方式");
        if (!receiptShape && !paymentShape) return;
        Map<String, Object> sections = sectionMap(project);
        String receiptDetails = n(sections.get("projectReceiptMethod"), "").trim();
        String paymentDetails = n(sections.get("projectPaymentMethod"), "").trim();
        String receiptOptions = "";
        Map<String, Object> paymentDetailMap = mapOf(sections.get("projectPaymentDetails"));
        Object selectedMethods = sections.get("projectPaymentMethods");
        List<String> paymentLines = new ArrayList<>();
        if (selectedMethods instanceof List<?> methods) {
            for (Object method : methods) {
                String label = String.valueOf(method);
                String details = n(paymentDetailMap.get(label), "").trim();
                // The option label and its template example are editing aids,
                // not project data. Only text actually entered by the user is
                // allowed to reach the generated presentation.
                if (!details.isBlank()) paymentLines.add(details);
            }
        }
        String paymentOptions = String.join("\n", paymentLines);
        String receipt = String.join("\n", java.util.stream.Stream.of(receiptOptions, receiptDetails).filter(value -> !value.isBlank()).toList());
        // New forms store the actual clause under the selected payment type.
        // Keep the legacy free-text field only as a fallback for records that
        // have not been opened and saved since the form structure changed.
        String payment = paymentOptions.isBlank() ? paymentDetails : paymentOptions;
        if (receiptShape && paymentShape) {
            writePaymentSections(shape, receipt, payment);
        } else if (receiptShape) {
            writeSinglePaymentSection(shape, "项目收款方式", receipt);
        } else {
            writeSinglePaymentSection(shape, "项目付款方式", payment);
        }
    }

    private void writePaymentSections(XSLFTextShape shape, String receipt, String payment) {
        shape.clearText();
        addPaymentParagraph(shape, "□  项目收款方式", true);
        addCheckedPaymentLines(shape, receipt);
        addPaymentParagraph(shape, "□  项目付款方式", true);
        addCheckedPaymentLines(shape, payment);
        shape.setTextAutofit(TextShape.TextAutofit.NORMAL);
    }

    private void writeSinglePaymentSection(XSLFTextShape shape, String title, String body) {
        shape.clearText();
        addPaymentParagraph(shape, "□  " + title, true);
        addCheckedPaymentLines(shape, body);
        shape.setTextAutofit(TextShape.TextAutofit.NORMAL);
    }

    private void addCheckedPaymentLines(XSLFTextShape shape, String body) {
        if (body == null || body.isBlank()) return;
        for (String line : body.split("\\R")) {
            String clean = line.trim();
            if (!clean.isBlank()) addTemplateCheckParagraph(shape, clean);
        }
    }

    /**
     * Rebuild the template's real Wingdings check bullet instead of inserting
     * a Unicode check-mark character into the text. These values mirror the
     * payment-body paragraphs in the uploaded DICT template.
     */
    private void addTemplateCheckParagraph(XSLFTextShape shape, String value) {
        XSLFTextParagraph paragraph = shape.addNewTextParagraph();
        paragraph.setBullet(true);
        paragraph.setBulletFont("Wingdings");
        paragraph.setBulletCharacter("ü");
        paragraph.setBulletFontColor(new Color(0x00, 0x70, 0xC0));
        paragraph.setIndentLevel(1);
        paragraph.setLeftMargin(58.5d);
        paragraph.setIndent(-22.5d);
        paragraph.setLineSpacing(150d);
        paragraph.setSpaceBefore(0d);
        paragraph.setSpaceAfter(0d);
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(value);
        run.setFontFamily("Microsoft YaHei");
        run.setFontSize(14d);
        run.setBold(false);
        run.setFontColor(Color.BLACK);
    }

    private void addPaymentParagraph(XSLFTextShape shape, String value, boolean heading) {
        if (value == null || value.isBlank()) return;
        XSLFTextParagraph paragraph = shape.addNewTextParagraph();
        paragraph.setBullet(false);
        paragraph.setLeftMargin(0d);
        paragraph.setIndent(0d);
        paragraph.setSpaceBefore(heading ? 0d : 2d);
        paragraph.setSpaceAfter(heading ? 2d : 6d);
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(value);
        run.setFontFamily("Microsoft YaHei");
        run.setFontSize(heading ? 16d : 13d);
        run.setBold(heading);
        run.setFontColor(heading ? new Color(0x00, 0x70, 0xC0) : Color.BLACK);
    }

    private void reflowPaymentCashFlowPage(XSLFSlide slide, Map<String, Object> project) {
        XSLFTextShape methods = null;
        XSLFTextShape footnote = null;
        XSLFTable cashFlow = null;
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTable table && tableText(table).contains("项目现金流情况")) cashFlow = table;
            else if (shape instanceof XSLFTextShape textShape) {
                String value = n(textShape.getText(), "");
                if (value.contains("项目收款方式") && value.contains("项目付款方式")) methods = textShape;
                else if (value.contains("项目合作服务模式不含税收入")) footnote = textShape;
            }
        }
        boolean hasInvestment = hasInvestmentCashFlow(sectionRows(project, "cashFlowRows"));
        if (methods != null) methods.setAnchor(new Rectangle2D.Double(25.2, 64.0, 907.1, 185.0));
        if (cashFlow != null) {
            if (hasInvestment) {
                cashFlow.setAnchor(new Rectangle2D.Double(27.2, 280.0, 905.1, 170.0));
            } else {
                cashFlow.setAnchor(new Rectangle2D.Double(70.0, 278.0, 754.0, 176.0));
                double[] widths = {88, 106, 94, 95, 95, 187, 89};
                for (int column = 0; column < widths.length; column++) cashFlow.setColumnWidth(column, widths[column]);
            }
        }
        if (footnote != null) {
            if (hasInvestment) {
                footnote.setAnchor(new Rectangle2D.Double(21.6, 462.0, 915.0, 42.0));
            } else {
                footnote.clearText();
                XSLFTextParagraph paragraph = footnote.addNewTextParagraph();
                paragraph.setBullet(false);
                XSLFTextRun run = paragraph.addNewTextRun();
                run.setText("□  项目协议期内现金流");
                run.setFontFamily("Microsoft YaHei");
                run.setFontSize(16d);
                run.setBold(true);
                run.setFontColor(new Color(0x00, 0x70, 0xC0));
                footnote.setAnchor(new Rectangle2D.Double(25.2, 235.0, 400.0, 32.0));
            }
        }
    }

    private void reflowClientPerformancePage(XSLFSlide slide, Map<String, Object> project) {
        Map<String, Object> sections = sectionMap(project);
        boolean hasHousing = !n(sections.get("clientHousingDebtAmount"), "").isBlank();
        boolean hasAsymmetric = !n(sections.get("clientAsymmetricCount"), "").isBlank()
                || !n(sections.get("clientAsymmetricAmount"), "").isBlank()
                || !n(sections.get("clientAsymmetricRatio"), "").isBlank();
        XSLFTable performance = null;
        XSLFTextShape otherPerformance = null;
        XSLFTextShape riskHeading = null;
        XSLFTextShape riskBody = null;
        for (XSLFShape shape : new ArrayList<>(slide.getShapes())) {
            if (shape instanceof XSLFTable table && tableText(table).contains("商机编号")) {
                performance = table;
                continue;
            }
            if (!(shape instanceof XSLFTextShape textShape)) continue;
            String value = n(textShape.getText(), "");
            if (!hasHousing && (value.contains("以房抵债情况") || value.contains("采用以房抵债方式"))) {
                slide.removeShape(shape);
            } else if (!hasAsymmetric && (value.contains("非对称付款情况") || value.contains("共计非对称支付后向"))) {
                slide.removeShape(shape);
            } else if (value.contains("与其他单位合作履约情况")) otherPerformance = textShape;
            else if (value.contains("项目风险核查")) riskHeading = textShape;
            else if (value.contains("拖欠税款被列入") || value.contains("股权、高管频繁变动")) riskBody = textShape;
        }
        if (performance != null) {
            for (XSLFTableRow row : performance.getRows()) row.setHeight(17d);
            performance.setAnchor(new Rectangle2D.Double(59.2, 124.0, 864.0, 85.0));
        }
        if (otherPerformance != null) otherPerformance.setAnchor(new Rectangle2D.Double(59.2, 260.0, 864.0, 30.0));
        double riskTop = hasHousing || hasAsymmetric ? 330.0 : 295.0;
        if (riskHeading != null) riskHeading.setAnchor(new Rectangle2D.Double(25.8, riskTop, 893.0, 38.0));
        if (riskBody != null) riskBody.setAnchor(new Rectangle2D.Double(59.2, riskTop + 45.0, 864.0, 120.0));
    }

    private void writeRiskSummary(XSLFTextShape shape, String summary) {
        shape.clearText();
        XSLFTextParagraph paragraph = shape.addNewTextParagraph();
        paragraph.setBullet(false);
        paragraph.setLeftMargin(0d);
        paragraph.setIndent(0d);
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText("□  " + summary.trim());
        run.setFontFamily("Microsoft YaHei");
        run.setFontSize(14d);
        run.setFontColor(new Color(0x00, 0x70, 0xC0));
        run.setBold(true);
    }

    private void addTemplateBodyParagraph(XSLFTextShape shape, String value, boolean heading) {
        if (value == null || value.isBlank()) return;
        XSLFTextParagraph paragraph = shape.addNewTextParagraph();
        paragraph.setLeftMargin(heading ? 0d : 18d);
        paragraph.setIndent(0d);
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(value);
        run.setFontFamily("Microsoft YaHei");
        run.setFontSize(heading ? 18d : 13d);
        run.setBold(heading);
        run.setFontColor(heading ? new Color(0x00, 0x70, 0xC0) : Color.BLACK);
    }

    private String joinedSectionValues(Map<String, Object> project, String key, String delimiter) {
        Object value = sectionMap(project).get(key);
        if (!(value instanceof List<?> list)) return "";
        return list.stream().map(item -> n(item, "")).filter(item -> !item.isBlank()).collect(java.util.stream.Collectors.joining(delimiter));
    }

    private List<String> sectionStrings(Map<String, Object> project, String key) {
        Object value = sectionMap(project).get(key);
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().map(item -> n(item, "")).filter(item -> !item.isBlank()).toList();
    }

    private void fillDeliveryPlan(XSLFTextShape shape, Map<String, Object> project, String text) {
        Map<String, Object> sections = sectionMap(project);
        if (!text.contains("交付责任：") && !text.contains("工期情况：")) return;
        String responsibility = n(sections.get("deliveryResponsibility"), "");
        String partners = n(sections.get("deliveryPartners"), "");
        String schedule = n(sections.get("deliverySchedule"), "");
        if (responsibility.isBlank() && partners.isBlank() && schedule.isBlank()) {
            String legacy = n(sections.get("deliveryPlan"), "");
            if (!legacy.isBlank()) set(shape, legacy);
            return;
        }
        writeLabeledBodyLines(shape, List.of(
                new String[]{"交付责任：", responsibility},
                new String[]{"合作伙伴：", partners},
                new String[]{"工期情况：", schedule}));
    }

    private void writeLabeledBodyLines(XSLFTextShape shape, List<String[]> lines) {
        XSLFTextParagraph templateParagraph = shape.getTextParagraphs().stream()
                .filter(p -> p.getText() != null && !p.getText().isBlank()).findFirst().orElse(null);
        XSLFTextRun templateRun = templateParagraph == null ? null : firstTextRun(templateParagraph.getTextRuns(), 0);
        String templateFontFamily = templateRun == null ? null : templateRun.getFontFamily();
        Double templateFontSize = templateRun == null ? null : templateRun.getFontSize();
        PaintStyle templatePaint = templateRun == null ? null : templateRun.getFontColor();
        Color templateFontColor = templatePaint instanceof PaintStyle.SolidPaint solidPaint
                ? solidPaint.getSolidColor().getColor() : null;
        boolean templateBold = templateRun != null && templateRun.isBold();
        shape.clearText();
        for (String[] line : lines) {
            XSLFTextParagraph paragraph = shape.addNewTextParagraph();
            paragraph.setBullet(false);
            paragraph.setLeftMargin(0d);
            paragraph.setIndent(0d);
            XSLFTextRun label = paragraph.addNewTextRun();
            XSLFTextRun value = paragraph.addNewTextRun();
            label.setText("□  " + line[0]);
            value.setText(line[1]);
            for (XSLFTextRun run : List.of(label, value)) {
                if (templateFontFamily != null) run.setFontFamily(templateFontFamily);
                if (templateFontSize != null) run.setFontSize(templateFontSize);
            }
            label.setFontColor(new Color(0x00, 0x70, 0xC0));
            label.setBold(true);
            value.setFontColor(Color.BLACK);
            value.setBold(false);
        }
    }
    private void fillDecisionPage(XSLFTextShape shape, Map<String, Object> project, String text) {
        if (!text.contains("是否同意该项目方案") && !text.contains("是否同意通过") && !text.contains("是否通过投入成本")) return;
        Map<String, Object> s = sectionMap(project);
        String mode = n(s.get("decisionMode"), "").trim();
        if (mode.isBlank()) {
            List<String> deliveryModes = sectionStrings(project, "deliveryModes");
            if (!deliveryModes.isEmpty()) mode = deliveryModes.get(0);
        }
        if (mode.isBlank()) mode = "合作服务模式";
        String cost = amountWithoutWanUnit(s.get("decisionCostIncTax"));
        String total = amountWithoutWanUnit(s.get("decisionTotalRevenueIncTax"));
        String main = amountWithoutWanUnit(s.get("decisionMainRevenueIncTax"));
        String other = amountWithoutWanUnit(s.get("decisionOtherRevenueIncTax"));
        if (other.isBlank()) other = "0";
        String costBasis = mode.contains("受托代销") ? "受托代销应付账款"
                : mode + (mode.contains("投资") ? "投资" : "投入成本");
        String decision = "是否同意通过" + costBasis + cost
                + "万元（含税）建设该项目，项目总收入为" + total
                + "万元（含税），其中主营业务收入" + main + "万元（含税）"
                + "、其他业务收入" + other + "万元（含税）。";
        String includeSelection = normalizeYesNo(s.get("decisionIncludeSelection"));
        if (!s.containsKey("decisionIncludeSelection")) {
            includeSelection = "是".equals(normalizeYesNo(s.get("decisionApproveSelection"))) ? "是" : "";
        }
        writeDecisionItems(shape, decision, "是".equals(includeSelection));
    }
    private void writeDecisionItems(XSLFTextShape shape, String decision, boolean includeSelection) {
        shape.clearText();
        addDecisionParagraph(shape, decision);
        if (includeSelection) addDecisionParagraph(shape, "是否同意该项目标后甄选结果？");
    }
    private void addDecisionParagraph(XSLFTextShape shape, String value) {
        XSLFTextParagraph paragraph = shape.addNewTextParagraph();
        paragraph.setBullet(false);
        paragraph.setLeftMargin(0d);
        paragraph.setIndent(0d);
        paragraph.setSpaceAfter(8d);
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText("• " + value);
        run.setFontFamily("Microsoft YaHei");
        run.setFontSize(18d);
        run.setBold(false);
        run.setFontColor(Color.BLACK);
    }
    private String amountWithoutWanUnit(Object value){return n(value,"").trim().replaceFirst("万元$","").trim();}
    private void fillPreDecisionOpinions(XSLFTextShape shape,Map<String,Object> project,String text){Map<String,Object>s=sectionMap(project);if(text.contains("省公司预决策意见")||text.contains("省公司招投标意见"))setTitleAndBody(shape,"省公司招投标意见",n(s.get("provincialPreDecisionOpinion"),""),72d);else if(text.contains("市公司决策意见")||text.contains("市公司招投标意见"))setTitleAndBody(shape,"市公司招投标意见（签报或会议纪要需体现法务人员意见）",n(s.get("cityDecisionOpinion"),""),72d);}
    private void fillExistingBusinessText(XSLFTextShape shape,Map<String,Object> project,String text){Map<String,Object>s=sectionMap(project);if(text.startsWith("存量项目运营")){String driven=normalizeYesNo(s.get("existingProjectDriven"));String detail;if("是".equals(driven))detail="本次立项项目由存量项目带动，存量商机编号为"+n(s.get("existingMerchantNo"),"")+"，项目名称为"+n(s.get("existingProjectName"),"")+"。";else if("否".equals(driven))detail="本次立项项目非存量项目带动。";else detail="";String supplement=n(s.get("existingProjectOperation"),"");if(!supplement.isBlank())detail+=(detail.isBlank()?"":"\n")+supplement;setTitleAndBody(shape,"存量项目运营：",detail,12d);}else if(text.startsWith("存量CT业务情况"))setTitleAndBody(shape,"存量CT业务情况：",n(s.get("existingCtSummary"),""),12d);}
    private void fillSupplyChainFinance(XSLFTextShape shape, Map<String,Object> project, String text) {
        if (!text.startsWith("供应链金融合作")) return;
        Map<String,Object> s = sectionMap(project);
        String used = normalizeYesNo(s.get("supplyChainFinanceUsed"));
        String usedText = "是".equals(used) ? "该项目与" + n(s.get("supplyChainFinanceBank"), "")
                + "银行合作供应链金融业务，合作金额" + amountWithoutWanUnit(s.get("supplyChainFinanceAmount"))
                + "万元，降低我公司垫资压力与资金成本，助力中小企业发展。" : "";
        String unusedText = "否".equals(used)
                ? supplyChainUnusedSentence(n(s.get("supplyChainFinanceReason"), "")) : "";
        writeSupplyChainCategories(shape, usedText, unusedText, n(s.get("supplyChainFinanceDescription"), ""));
    }

    private String supplyChainUnusedSentence(String reason) {
        String clean = reason.trim();
        if (clean.isBlank()) return "该项目未与银行合作供应链金融业务。";
        if (clean.contains("未与银行合作供应链金融业务")) {
            return clean.matches(".*[。！？]$") ? clean : clean + "。";
        }
        clean = clean.replaceFirst("^该项目因", "").replaceFirst("原因[，,。；;]*$", "").trim();
        return "该项目因" + clean + "原因，未与银行合作供应链金融业务。";
    }

    private void writeSupplyChainCategories(XSLFTextShape shape, String usedText, String unusedText, String supplement) {
        shape.clearText();
        addTemplateBodyParagraph(shape, "□ 供应链金融合作：", true);
        if (!usedText.isBlank()) {
            addSupplyChainCategoryParagraph(shape, "若使用供应链金融：", usedText);
        }
        if (!unusedText.isBlank()) {
            addSupplyChainCategoryParagraph(shape, "若未使用供应链金融：", unusedText);
        }
        if (!supplement.isBlank()) addTemplateBodyParagraph(shape, supplement, false);
    }
    private void addSupplyChainCategoryParagraph(XSLFTextShape shape,String label,String value){
        XSLFTextParagraph paragraph=shape.addNewTextParagraph();paragraph.setBullet(false);paragraph.setLeftMargin(0d);paragraph.setIndent(0d);paragraph.setSpaceAfter(4d);
        XSLFTextRun labelRun=paragraph.addNewTextRun();labelRun.setText("➢ " + label);labelRun.setFontFamily("Microsoft YaHei");labelRun.setFontSize(16d);labelRun.setBold(true);labelRun.setFontColor(Color.BLACK);
        XSLFTextRun valueRun=paragraph.addNewTextRun();valueRun.setText(" " + value);valueRun.setFontFamily("Microsoft YaHei");valueRun.setFontSize(16d);valueRun.setBold(false);valueRun.setFontColor(Color.BLACK);
    }
    private String normalizeYesNo(Object value){String text=n(value,"");if("同意".equals(text))return "是";if("不同意".equals(text))return "否";return "不涉及".equals(text)?"":text;}
    private void fillAppraisalText(XSLFTextShape shape,Map<String,Object> project,String text){if(!text.startsWith("执行情况"))return;Map<String,Object>s=sectionMap(project);String executed=n(s.get("appraisalExecuted"),"");BigDecimal amount=BigDecimal.ZERO;for(var row:sectionRows(project,"appraisalRows"))amount=amount.add(decimalValue(row.get("reducibleAmount")));String enteredAmount=n(s.get("appraisalReductionAmount"),"");if(enteredAmount.isBlank())enteredAmount=amount.signum()==0?"":amount.stripTrailingZeros().toPlainString();String ratio=n(s.get("appraisalReductionRatio"),"");String result="是".equals(executed)?"按照审核结果与生态议价，相对生态报价压降"+enteredAmount+"元，压降比例"+ratio+"%；":"未按照审核结果与生态议价，未议价原因："+n(s.get("appraisalReason"),"")+"。";setTitleAndBody(shape,"执行情况：（下面情况二选一）",result,8d);}

    private void replaceParagraphUnicode(XSLFTextShape shape, String marker, String value) {
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            String text = paragraph.getText() == null ? "" : paragraph.getText();
            if (text.contains(marker)) {
                setLabelValue(paragraph, marker, value);
                return;
            }
        }
    }

    private void replaceParagraphUnicodeIndented(XSLFTextShape shape, String marker, String value) {
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            String text = paragraph.getText() == null ? "" : paragraph.getText();
            if (!text.contains(marker)) continue;
            setLabelValue(paragraph, marker, value);
            List<XSLFTextRun> runs = paragraph.getTextRuns();
            XSLFTextRun labelRun = firstTextRun(runs, 0);
            XSLFTextRun valueRun = firstTextRun(runs, labelRun == null ? 0 : runs.indexOf(labelRun) + 1);
            if (valueRun != null && valueRun.getRawText() != null && !valueRun.getRawText().isBlank()) {
                valueRun.setText("  " + valueRun.getRawText());
            }
            return;
        }
    }

    private String tableText(XSLFTable table) {
        StringBuilder builder = new StringBuilder();
        for (XSLFTableRow row : table.getRows()) {
            for (XSLFTableCell cell : row.getCells()) {
                builder.append(cell.getText()).append('\n');
            }
        }
        return builder.toString();
    }

    private int matchedRowIndex(String cellText, String[] rowKeys) {
        String normalizedCell = normalizeRowLabel(cellText);
        for (int i = 0; i < rowKeys.length; i++) {
            if (normalizedCell.equals(normalizeRowLabel(rowKeys[i]))) {
                return i;
            }
        }
        return -1;
    }

    /** Template revisions sometimes split or omit the unit in the first column. */
    private String normalizeRowLabel(String value) {
        return normalize(value)
                .replaceAll("[（(][^）)]*[）)]", "")
                .replace("其中：", "")
                .replace("其中:", "");
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("\u3000", "").replaceAll("\\s+", "");
    }

    private String valueFor(Map<String, Object> source, String rowKey, String suffix, int rowIndex) {
        Object direct = source.get(rowKey + suffix);
        if (direct != null) {
            return n(direct, "");
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapOf(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private void replaceParagraph(XSLFTextShape shape, String marker, String value) {
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            if (paragraph.getText().contains(marker)) {
                set(paragraph, marker + value);
                return;
            }
        }
    }

    private Map<String, String> flatten(Map<String, Object> source) {
        Map<String, String> out = new LinkedHashMap<>();
        flatten("", source, out);
        return out;
    }

    private void flatten(String prefix, Object value, Map<String, String> out) {
        if (value instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                flatten(prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey(), entry.getValue(), out);
            }
        } else if (value != null) {
            out.put(prefix, String.valueOf(value));
        }
    }

    private void fillSelectionPage(
            XSLFTextShape shape,
            Map<String, Object> project,
            String originalText) {
        Map<String, Object> sections = sectionMap(project);
        String company = n(sections.get("selectionCompany"),
                n(sections.get("branchCompany"), "XX\u516c\u53f8"));
        String planDate = n(sections.get("selectionPlanDecisionDate"), "");
        String resultDate = n(sections.get("selectionResultDecisionDate"), "");
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            String paragraphText = paragraph.getText() == null ? "" : paragraph.getText();
            if (paragraphText.contains("\u5bf9\u7504\u9009\u65b9\u6848\u8fdb\u884c\u51b3\u7b56")) {
                set(paragraph, company + planDate
                        + "\u901a\u8fc7\u603b\u7ecf\u7406\u529e\u516c\u4f1a\u5bf9\u7504\u9009\u65b9\u6848\u8fdb\u884c\u51b3\u7b56\uff0c\u76f8\u5173\u4fe1\u606f\u5982\u4e0b\uff1a");
            } else if (paragraphText.contains("\u5bf9\u7504\u9009\u7ed3\u679c\u8fdb\u884c\u51b3\u7b56")) {
                set(paragraph, company + resultDate
                        + "\u901a\u8fc7\u603b\u7ecf\u7406\u529e\u516c\u4f1a\u5bf9\u7504\u9009\u7ed3\u679c\u8fdb\u884c\u51b3\u7b56\uff0c\u76f8\u5173\u4fe1\u606f\u5982\u4e0b\uff1a");
            }
        }
        replaceParagraphUnicode(shape, "\u7504\u9009\u6a21\u5f0f\uff1a",
                n(sections.get("selectionMode"), ""));
        replaceParagraphUnicode(shape, "\u7504\u9009\u91d1\u989d\uff1a",
                n(sections.get("selectionAmountSummary"), ""));
        if (originalText.contains("\u7504\u9009\u6a21\u5f0f\uff1a")
                || originalText.contains("\u7504\u9009\u91d1\u989d\uff1a")) {
            for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
                for (XSLFTextRun run : paragraph.getTextRuns()) run.setBold(false);
            }
        }
        String afterBid = n(sections.get("selectionAfterBid"), "");
        String afterBidReason = n(sections.get("selectionAfterBidReason"), "");
        replaceParagraphUnicode(shape, "\u662f\u5426\u6807\u540e\u7504\u9009\uff1a",
                afterBid + (afterBidReason.isBlank() ? "" : "\uff08" + afterBidReason + "\uff09"));
        replaceParagraphUnicode(shape, "\u8bc4\u5ba1\u5c0f\u7ec4\uff1a",
                n(sections.get("selectionReviewPanel"), ""));
        replaceParagraphUnicode(shape, "\u8bc4\u5ba1\u6807\u51c6\uff1a",
                n(sections.get("selectionReviewStandard"), ""));
        replaceParagraphUnicode(shape, "\u7504\u9009\u8bc4\u5ba1\uff1a",
                n(sections.get("selectionReviewDate"), "")
                        + (n(sections.get("selectionReviewDate"), "").isBlank() ? "" : "\uff0c")
                        + n(sections.get("selectionReviewPanel"), ""));
        replaceParagraphUnicode(shape, "\u8bc4\u5ba1\u59d4\u5458\u4f1a\u63a8\u8350\u610f\u89c1\uff1a",
                "");
        replaceParagraphUnicodeIndented(shape, "\u4e2d\u9009\u4eba\uff1a",
                n(sections.get("selectionWinners"), ""));
        replaceParagraphUnicodeIndented(shape, "\u5019\u9009\u4eba\uff1a",
                n(sections.get("selectionCandidates"), ""));
        String publicationDate = n(sections.get("selectionPublicationDate"), "");
        replaceParagraphUnicode(shape, "\u7504\u9009\u7ed3\u679c\u516c\u793a\uff1a",
                publicationDate + (publicationDate.isBlank() ? ""
                        : "\u901a\u8fc7\u5c71\u4e1c\u79fb\u52a8\u751f\u6001\u5408\u4f5c\u7edf\u4e00\u95e8\u6237\u8fdb\u884c\u7ed3\u679c\u516c\u793a\u3002"));
    }

    private boolean isPreDecisionApproved(Map<String, Object> project) {
        Object value = sectionMap(project).get("preDecisionApproved");
        return Boolean.TRUE.equals(value)
                || "true".equalsIgnoreCase(String.valueOf(value))
                || "\u662f".equals(String.valueOf(value));
    }

    private boolean isProcurementComparisonEnabled(Map<String, Object> project) {
        Object value = sectionMap(project).get("procurementComparisonEnabled");
        return Boolean.TRUE.equals(value)
                || "true".equalsIgnoreCase(String.valueOf(value))
                || "\u662f".equals(String.valueOf(value));
    }

    private boolean isIdcEnabled(Map<String, Object> project) {
        Object value = sectionMap(project).get("idcEnabled");
        return Boolean.TRUE.equals(value)
                || "true".equalsIgnoreCase(String.valueOf(value))
                || "\u662f".equals(String.valueOf(value));
    }

    private boolean isInvestmentEconomicBenefitEnabled(Map<String, Object> project) {
        return sectionFlag(project, "investmentEconomicBenefitEnabled");
    }

    private boolean isCooperationEconomicBenefitEnabled(Map<String, Object> project) {
        return sectionFlag(project, "cooperationEconomicBenefitEnabled");
    }

    private boolean isAdvancePaymentReviewEnabled(Map<String, Object> project) {
        return sectionFlag(project, "advancePaymentReviewEnabled");
    }

    private boolean isGovernmentFundProject(Map<String, Object> project) {
        return !"ENTERPRISE".equalsIgnoreCase(n(sectionMap(project).get("fundProjectType"), "GOVERNMENT"));
    }

    private boolean isGovernmentRiskAuditText(String text) {
        return text.contains("项目风险核查")
                || text.contains("拖欠税款被列入")
                || text.contains("企业公示信息弄虚作假")
                || text.contains("多起劳务纠纷")
                || text.contains("股权、高管频繁变动");
    }

    private boolean sectionFlag(Map<String, Object> project, String key) {
        Object value = sectionMap(project).get(key);
        return Boolean.TRUE.equals(value)
                || "true".equalsIgnoreCase(String.valueOf(value))
                || "\u662f".equals(String.valueOf(value));
    }

    private Map<String, Object> sectionMap(Map<String, Object> project) {
        Object sections = project.get("sections");
        if (sections instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            return typed;
        }
        return Map.of();
    }

    private void set(XSLFTableCell cell, String value) {
        if (cell.getTextParagraphs().isEmpty()) {
            cell.addNewTextParagraph();
        }
        XSLFTextParagraph paragraph = cell.getTextParagraphs().get(0);
        set(paragraph, value);
        applyTemplateCellTypography(paragraph);
        for (XSLFTextRun run : paragraph.getTextRuns()) {
            if (run.getRawText() != null && !run.getRawText().isBlank()
                    && !"XSLFLineBreak".equals(run.getClass().getSimpleName())) {
                run.setFontColor(Color.BLACK);
            }
        }
        XmlObject[] paragraphs = cell.getXmlObject().selectPath(
                "declare namespace a='http://schemas.openxmlformats.org/drawingml/2006/main' "
                        + ".//a:txBody/a:p");
        for (int i = paragraphs.length - 1; i >= 1; i--) {
            try (XmlCursor cursor = paragraphs[i].newCursor()) {
                cursor.removeXml();
            }
        }
    }

    private void forceBlack(XSLFTextShape shape) {
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            for (XSLFTextRun run : paragraph.getTextRuns()) {
                run.setFontColor(Color.BLACK);
            }
        }
    }

    private void set(XSLFTableCell cell, String value, XSLFTableCell referenceCell) {
        set(cell, value);
        copyCellTypography(cell, referenceCell);
    }

    private void copyCellTypography(XSLFTableCell targetCell, XSLFTableCell referenceCell) {
        XSLFTextParagraph referenceParagraph = referenceCell.getTextParagraphs().stream()
                .filter(paragraph -> paragraph.getText() != null && !paragraph.getText().isBlank())
                .findFirst()
                .orElse(referenceCell.getTextParagraphs().isEmpty() ? null : referenceCell.getTextParagraphs().get(0));
        XSLFTextParagraph targetParagraph = targetCell.getTextParagraphs().isEmpty()
                ? null : targetCell.getTextParagraphs().get(0);
        if (referenceParagraph == null || targetParagraph == null) {
            return;
        }
        XSLFTextRun referenceRun = firstTextRun(referenceParagraph.getTextRuns(), 0);
        XSLFTextRun targetRun = firstTextRun(targetParagraph.getTextRuns(), 0);
        if (referenceRun == null || targetRun == null) {
            return;
        }
        targetParagraph.setTextAlign(referenceParagraph.getTextAlign());
        targetParagraph.setFontAlign(referenceParagraph.getFontAlign());
        targetParagraph.setLineSpacing(referenceParagraph.getLineSpacing());
        targetParagraph.setSpaceBefore(referenceParagraph.getSpaceBefore());
        targetParagraph.setSpaceAfter(referenceParagraph.getSpaceAfter());
        if (referenceRun.getFontSize() != null) {
            targetRun.setFontSize(referenceRun.getFontSize());
        }
        if (referenceRun.getFontFamily() != null) {
            targetRun.setFontFamily(referenceRun.getFontFamily());
        }
        targetRun.setBold(referenceRun.isBold());
        targetRun.setItalic(referenceRun.isItalic());
        if (referenceRun.getFontColor() != null) {
            targetRun.setFontColor(referenceRun.getFontColor());
        }
    }

    private void applyTemplateCellTypography(XSLFTextParagraph paragraph) {
        Double fontSize = paragraph.getDefaultFontSize();
        String fontFamily = paragraph.getDefaultFontFamily();
        for (XSLFTextRun run : paragraph.getTextRuns()) {
            if (run.getRawText() == null || run.getRawText().isBlank()
                    || "XSLFLineBreak".equals(run.getClass().getSimpleName())) {
                continue;
            }
            if (fontSize != null && fontSize > 0) {
                run.setFontSize(fontSize);
            }
            if (fontFamily != null && !fontFamily.isBlank()) {
                run.setFontFamily(fontFamily);
            }
        }
    }

    private void setCellFontSize(XSLFTableCell cell, double size) {
        for (XSLFTextParagraph paragraph : cell.getTextParagraphs()) {
            for (XSLFTextRun run : paragraph.getTextRuns()) {
                if (run.getRawText() != null && !run.getRawText().isBlank()) run.setFontSize(size);
            }
        }
    }

    private void set(XSLFTextShape shape, String value) {
        if (shape.getTextParagraphs().isEmpty()) {
            return;
        }
        XSLFTextParagraph target = shape.getTextParagraphs().stream()
                .filter(p -> p.getText() != null && !p.getText().isBlank())
                .findFirst()
                .orElse(shape.getTextParagraphs().get(0));
        set(target, value);
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            if (paragraph != target) {
                set(paragraph, "");
            }
        }
    }

    private void setTitleAndBody(XSLFTextShape shape, String title, String body, double bodySpaceBefore) {
        if (shape.getTextParagraphs().isEmpty()) {
            return;
        }
        XSLFTextParagraph titleParagraph = shape.getTextParagraphs().stream()
                .filter(p -> p.getText() != null && !p.getText().isBlank())
                .findFirst()
                .orElse(shape.getTextParagraphs().get(0));
        XSLFTextRun referenceRun = firstTextRun(titleParagraph.getTextRuns(), 0);
        set(titleParagraph, title);
        XSLFTextParagraph bodyParagraph = shape.getTextParagraphs().stream()
                .filter(p -> p != titleParagraph)
                .findFirst()
                .orElseGet(shape::addNewTextParagraph);
        set(bodyParagraph, body);
        bodyParagraph.setBullet(false);
        bodyParagraph.setIndent(0d);
        bodyParagraph.setLeftMargin(0d);
        bodyParagraph.setTextAlign(titleParagraph.getTextAlign());
        bodyParagraph.setFontAlign(titleParagraph.getFontAlign());
        bodyParagraph.setLineSpacing(titleParagraph.getLineSpacing());
        bodyParagraph.setSpaceAfter(titleParagraph.getSpaceAfter());
        bodyParagraph.setSpaceBefore(bodySpaceBefore);
        XSLFTextRun bodyRun = firstTextRun(bodyParagraph.getTextRuns(), 0);
        if (referenceRun != null && bodyRun != null) {
            if (referenceRun.getFontSize() != null) bodyRun.setFontSize(referenceRun.getFontSize());
            if (referenceRun.getFontFamily() != null) bodyRun.setFontFamily(referenceRun.getFontFamily());
            bodyRun.setBold(referenceRun.isBold());
            bodyRun.setItalic(referenceRun.isItalic());
            if (referenceRun.getFontColor() != null) bodyRun.setFontColor(referenceRun.getFontColor());
        }
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            if (paragraph != titleParagraph && paragraph != bodyParagraph) {
                set(paragraph, "");
            }
        }
    }

    private void setWithoutBullet(XSLFTextShape shape,String value){set(shape,value);for(XSLFTextParagraph paragraph:shape.getTextParagraphs()){paragraph.setBullet(false);paragraph.setIndent(0.0);paragraph.setLeftMargin(0.0);}}

    private void set(XSLFTextParagraph paragraph, String value) {
        List<XSLFTextRun> runs = paragraph.getTextRuns();
        XSLFTextRun target = null;
        for (XSLFTextRun run : runs) {
            if (!"XSLFLineBreak".equals(run.getClass().getSimpleName())) {
                target = run;
                break;
            }
        }
        if (target == null) {
            target = paragraph.addNewTextRun();
        }
        target.setText(value);
        for (XSLFTextRun run : runs) {
            if (run != target && !"XSLFLineBreak".equals(run.getClass().getSimpleName())) {
                run.setText("");
            }
        }
    }

    private String n(Object value, String fallback) {
        if (value instanceof List<?> values) {
            String joined=values.stream().map(String::valueOf).filter(text -> !text.isBlank()).collect(java.util.stream.Collectors.joining("、"));
            return joined.isBlank()?fallback:joined;
        }
        return value == null || String.valueOf(value).isBlank()
                ? fallback
                : String.valueOf(value).strip();
    }

    private String money(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return "";
        }
        try {
            return new BigDecimal(String.valueOf(value)).stripTrailingZeros().toPlainString();
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String money2(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return "";
        try {
            return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
