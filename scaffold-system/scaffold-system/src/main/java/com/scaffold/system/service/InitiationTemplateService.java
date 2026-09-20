package com.scaffold.system.service;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.domain.vo.IdResponse;
import com.scaffold.system.mapper.InitiationMapper;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;

@Service
public class InitiationTemplateService {
    private static final Pattern TOKEN=Pattern.compile("\\{\\{[#/@?]?([\\w.:]+)}}");
    private final InitiationMapper mapper;
    private final Path directory;

    public InitiationTemplateService(InitiationMapper mapper,
      @Value("${scaffold.initiation.storage-path:./data/initiation}") String storagePath) {
        this.mapper=mapper; this.directory=Path.of(storagePath).toAbsolutePath().normalize().resolve("templates");
    }
    public List<Map<String,Object>> list(){return mapper.templates();}

    @Transactional
    public IdResponse upload(String name,MultipartFile file) {
        if(file==null||file.isEmpty())throw new BusinessException("请选择PPTX模板");
        String original=Optional.ofNullable(file.getOriginalFilename()).orElse("template.pptx");
        if(!original.toLowerCase(Locale.ROOT).endsWith(".pptx"))throw new BusinessException("只支持.pptx模板");
        try{return save(name,original,file.getBytes());}catch(Exception e){
            if(e instanceof BusinessException b)throw b;
            throw new BusinessException("模板上传失败："+e.getMessage());
        }
    }
    public IdResponse importLocal(String name,Path source) {
        try{return save(name,source.getFileName().toString(),Files.readAllBytes(source));}
        catch(Exception e){throw new BusinessException("默认模板导入失败："+e.getMessage());}
    }
    private IdResponse save(String name,String original,byte[] bytes)throws Exception{
        if(bytes.length>80L*1024*1024)throw new BusinessException("模板不能超过80MB");
        Set<String> keys=new TreeSet<>();
        try(XMLSlideShow ppt=new XMLSlideShow(new ByteArrayInputStream(bytes))){
            InitiationTemplateValidator.validate(ppt);
            for(XSLFSlide slide:ppt.getSlides())for(XSLFShape shape:slide.getShapes()){
                if(shape instanceof XSLFTextShape text)collect(text.getText(),keys);
                if(shape instanceof XSLFTable table)for(XSLFTableRow row:table.getRows())
                    for(XSLFTableCell cell:row.getCells())collect(cell.getText(),keys);
            }
        }
        Files.createDirectories(directory);
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        Path target=directory.resolve(System.currentTimeMillis()+"-"+hash.substring(0,12)+".pptx").normalize();
        if(!target.startsWith(directory))throw new BusinessException("模板路径无效");
        Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
        Map<String,Object> row=new LinkedHashMap<>();
        row.put("templateName",name==null||name.isBlank()?original.replaceFirst("(?i)\\.pptx$",""):name.trim());
        row.put("originalFilename",original);row.put("storagePath",target.toString());row.put("fileSize",bytes.length);
        row.put("sha256",hash);row.put("placeholderKeys",String.join(",",keys));
        row.put("defaultFlag",mapper.defaultTemplate()==null?1:0);
        mapper.insertTemplate(row,operator());return new IdResponse(((Number)row.get("id")).longValue());
    }
    private void collect(String text,Set<String> keys){if(text==null)return;Matcher m=TOKEN.matcher(text);while(m.find())keys.add(m.group(1));}
    @Transactional public void setDefault(Long id){
        Map<String,Object> template=get(id);
        try(XMLSlideShow ppt=new XMLSlideShow(Files.newInputStream(path(template)))){
            InitiationTemplateValidator.validate(ppt);
        }catch(IOException e){throw new BusinessException("模板读取失败，默认模板未变更");}
        mapper.clearDefault();mapper.setDefault(id);
    }
    public byte[] download(Long id){Map<String,Object> t=get(id);try{return Files.readAllBytes(path(t));}catch(IOException e){throw new BusinessException("模板读取失败");}}
    public Map<String,Object> get(Long id){Map<String,Object> t=mapper.template(id);if(t==null)throw new BusinessException(404,"模板不存在");return t;}
    public Path path(Map<String,Object> t){Path p=Path.of(String.valueOf(t.getOrDefault("storagePath",t.get("storage_path")))).toAbsolutePath().normalize();if(!p.startsWith(directory)||!Files.isRegularFile(p))throw new BusinessException(404,"模板文件不存在");return p;}
    public Map<String,Object> resolve(Long id){Map<String,Object> t=id==null?mapper.defaultTemplate():mapper.template(id);if(t==null)throw new BusinessException("请先上传并启用立项PPT模板");return t;}
    @Transactional public void delete(Long id){Map<String,Object> t=get(id);if(Boolean.TRUE.equals(t.get("defaultFlag"))||"1".equals(String.valueOf(t.get("default_flag"))))throw new BusinessException("默认模板不能删除");if(mapper.deleteTemplate(id)==0)throw new BusinessException("模板删除失败");try{Files.deleteIfExists(path(t));}catch(Exception ignored){}}
    private String operator(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":String.valueOf(a.getPrincipal());}
}
