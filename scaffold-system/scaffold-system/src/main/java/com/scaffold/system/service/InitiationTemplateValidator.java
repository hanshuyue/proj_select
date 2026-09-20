package com.scaffold.system.service;

import com.scaffold.common.exception.BusinessException;
import org.apache.poi.xslf.usermodel.*;
import java.util.ArrayList;
import java.util.List;

/** Guard the positional contract used by the standard DICT renderer. */
public final class InitiationTemplateValidator {
    private InitiationTemplateValidator() {}

    public static void validate(XMLSlideShow ppt) {
        if (ppt.getSlides().size() != 31) {
            throw new BusinessException("模板必须保留标准DICT模板的31页完整页序，当前为"
                    + ppt.getSlides().size() + "页。请下载当前模板修改，不要上传已经生成的项目PPT。");
        }
        check(ppt, 6, "投入情况", 2, 7);
        check(ppt, 9, "收益情况", 2, 6);
        check(ppt, 10, "收益分析", 24, 6);
        check(ppt, 11, "对比", 13, 7);
        for (int page = 12; page <= 14; page++) check(ppt, page, "经济效益评估", 20, 12);
    }

    private static void check(XMLSlideShow ppt, int page, String title, int minRows, int columns) {
        List<XSLFShape> shapes = new ArrayList<>();
        collect(ppt.getSlides().get(page - 1).getShapes(), shapes);
        boolean titleMatches = shapes.stream().filter(s -> s instanceof XSLFTextShape)
                .map(s -> ((XSLFTextShape) s).getText().replaceAll("\\s+", ""))
                .anyMatch(text -> text.contains(title));
        boolean tableMatches = shapes.stream().filter(s -> s instanceof XSLFTable)
                .map(s -> (XSLFTable) s).anyMatch(t -> t.getRows().size() >= minRows
                        && t.getRows().get(0).getCells().size() == columns);
        if (!titleMatches || !tableMatches) {
            throw new BusinessException("模板第" + page + "页（" + title
                    + "）结构不兼容：请保留原标题及至少" + minRows + "行、" + columns + "列表格，勿调整页序。");
        }
    }

    private static void collect(List<XSLFShape> source, List<XSLFShape> target) {
        for (XSLFShape shape : source) {
            target.add(shape);
            if (shape instanceof XSLFGroupShape group) collect(group.getShapes(), target);
        }
    }
}
