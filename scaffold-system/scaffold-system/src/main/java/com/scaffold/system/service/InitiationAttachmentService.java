package com.scaffold.system.service;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.domain.vo.IdResponse;
import com.scaffold.system.mapper.InitiationMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class InitiationAttachmentService {
  private final InitiationMapper mapper; private final Path root;
  public InitiationAttachmentService(InitiationMapper mapper,@Value("${scaffold.initiation.storage-path:./data/initiation}") String path){this.mapper=mapper;this.root=Path.of(path).toAbsolutePath().normalize().resolve("attachments");}
  public List<Map<String,Object>> list(Long projectId){return mapper.attachments(projectId);}
  public IdResponse upload(Long projectId,String type,MultipartFile file){
    if(mapper.get(projectId)==null)throw new BusinessException(404,"立项项目不存在"); if(file==null||file.isEmpty())throw new BusinessException("请选择文件");
    String name=Optional.ofNullable(file.getOriginalFilename()).orElse("附件");
    try{Files.createDirectories(root);String suffix=name.contains(".")?name.substring(name.lastIndexOf('.')):"";Path target=root.resolve(projectId+"-"+System.currentTimeMillis()+suffix).normalize();if(!target.startsWith(root))throw new BusinessException("附件路径无效");file.transferTo(target);Map<String,Object> row=new HashMap<>();mapper.insertAttachment(row,projectId,type==null?"OTHER":type,name,target.toString(),file.getSize(),operator());return new IdResponse(((Number)row.get("id")).longValue());}catch(IOException e){throw new BusinessException("附件上传失败："+e.getMessage());}
  }
  public Map.Entry<String,byte[]> download(Long id){Map<String,Object> a=mapper.attachment(id);if(a==null)throw new BusinessException(404,"附件不存在");try{return Map.entry(String.valueOf(a.get("originalFilename")),Files.readAllBytes(Path.of(String.valueOf(a.get("storagePath")))));}catch(IOException e){throw new BusinessException(404,"附件文件不存在");}}
  public void delete(Long id){Map<String,Object>a=mapper.attachment(id);if(a==null)return;mapper.deleteAttachment(id);try{Files.deleteIfExists(Path.of(String.valueOf(a.get("storagePath"))));}catch(IOException ignored){}}
  private String operator(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":String.valueOf(a.getPrincipal());}
}
