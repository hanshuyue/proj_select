package com.scaffold.system.service;

import com.scaffold.system.mapper.SelectionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class SelectionTemplateBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SelectionTemplateBootstrap.class);
    private final SelectionMapper mapper;
    private final SelectionTemplateService service;
    private final String sourcePath;

    public SelectionTemplateBootstrap(SelectionMapper mapper, SelectionTemplateService service,
                                      @Value("${scaffold.selection.bootstrap-template-path:}") String sourcePath) {
        this.mapper = mapper;
        this.service = service;
        this.sourcePath = sourcePath;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (sourcePath == null || sourcePath.isBlank()) return;
        try {
            if (mapper.selectDefaultTemplate() != null) return;
            Path source = Path.of(sourcePath).toAbsolutePath().normalize();
            if (Files.isRegularFile(source)) service.importLocalTemplate("甄选结果标准模板", source);
        } catch (Exception e) {
            log.warn("甄选PPT默认模板初始化跳过，请确认selection_module.sql已执行：{}", e.getMessage());
        }
    }
}
