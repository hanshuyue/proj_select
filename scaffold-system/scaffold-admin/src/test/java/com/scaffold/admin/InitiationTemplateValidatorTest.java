package com.scaffold.admin;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.service.InitiationTemplateValidator;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class InitiationTemplateValidatorTest {
    @Test void rejectsProjectExportOrWrongPageCount() throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.createSlide();
            assertTrue(assertThrows(BusinessException.class, () -> InitiationTemplateValidator.validate(ppt))
                    .getMessage().contains("31页"));
        }
    }
    @Test void acceptsStandardTemplateAndRejectsRemovedFinancialTable() throws Exception {
        String file = System.getProperty("dict.template.path", "");
        assumeTrue(!file.isBlank() && Files.isRegularFile(Path.of(file)));
        try (XMLSlideShow ppt = new XMLSlideShow(Files.newInputStream(Path.of(file)))) {
            assertDoesNotThrow(() -> InitiationTemplateValidator.validate(ppt));
            var slide = ppt.getSlides().get(11);
            for (var shape : new java.util.ArrayList<>(slide.getShapes())) {
                if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTable) slide.removeShape(shape);
            }
            assertTrue(assertThrows(BusinessException.class, () -> InitiationTemplateValidator.validate(ppt))
                    .getMessage().contains("第12页"));
        }
    }
}
