package com.scaffold.system.service;

import com.scaffold.system.mapper.InitiationMapper;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;

import java.nio.file.*;

@Component
public class InitiationTemplateBootstrap implements ApplicationRunner {
    private static final Logger log=LoggerFactory.getLogger(InitiationTemplateBootstrap.class);
    private final InitiationMapper mapper; private final InitiationTemplateService service; private final String sourcePath;
    public InitiationTemplateBootstrap(InitiationMapper mapper,InitiationTemplateService service,
      @Value("${scaffold.initiation.bootstrap-template-path:}")String sourcePath){this.mapper=mapper;this.service=service;this.sourcePath=sourcePath;}
    @Override public void run(ApplicationArguments args){
        if(sourcePath==null||sourcePath.isBlank())return;
        try{if(mapper.defaultTemplate()!=null)return;Path p=Path.of(sourcePath).toAbsolutePath().normalize();if(Files.isRegularFile(p))service.importLocal("DICT项目立项标准模板",p);}
        catch(Exception e){log.warn("立项PPT默认模板初始化跳过，请确认initiation_module.sql已执行：{}",e.getMessage());}
    }
}
