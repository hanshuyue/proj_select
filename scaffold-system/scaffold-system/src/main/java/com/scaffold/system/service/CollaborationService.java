package com.scaffold.system.service;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.domain.AuthUserRow;
import com.scaffold.system.domain.dto.*;
import com.scaffold.system.domain.vo.IdResponse;
import com.scaffold.system.mapper.AuthMapper;
import com.scaffold.system.mapper.CollaborationMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.*;

@Service
public class CollaborationService {
 private final CollaborationMapper mapper; private final AuthMapper auth; private final PasswordEncoder encoder; private final Path root;
 public CollaborationService(CollaborationMapper mapper,AuthMapper auth,PasswordEncoder encoder,@Value("${scaffold.documents.storage-path:./data/documents}")String path){this.mapper=mapper;this.auth=auth;this.encoder=encoder;this.root=Path.of(path).toAbsolutePath().normalize();}
 public List<Map<String,Object>> departments(){return mapper.publicDepartments();}
 @Transactional public void register(RegisterRequest r){if(mapper.phoneExists(r.getPhone())>0)throw new BusinessException("该手机号已注册");if(mapper.validRegistrationDept(r.getDeptId())==0)throw new BusinessException("请选择有效的德州市或区县");Map<String,Object> row=new HashMap<>();row.put("phone",r.getPhone());row.put("password",encoder.encode(r.getPassword()));row.put("realName",r.getRealName().trim());row.put("deptId",r.getDeptId());mapper.register(row);mapper.bindExternalRole(((Number)row.get("id")).longValue());}
 public List<Map<String,Object>> registrations(){AuthUserRow u=requireAdmin();return mapper.registrations(u.getDeptId(),isSuperAdmin(u.getId()));}
 @Transactional public void setAdministrator(Long id,boolean administrator){requireSuperAdmin();if(mapper.registrationExists(id)==0)throw new BusinessException("用户不存在或不能变更该身份");mapper.removeBusinessAdmin(id);if(administrator)mapper.addBusinessAdmin(id);else mapper.bindExternalRole(id);}
 public List<Map<String,Object>> documents(){AuthUserRow u=current();return mapper.documents(u.getId(),isAdmin(u.getId()));}
 @Transactional public IdResponse upload(String title,String description,String businessType,Long projectId,String projectName,MultipartFile file){AuthUserRow u=current();if(title==null||title.isBlank())throw new BusinessException("请输入文档标题");if(file==null||file.isEmpty())throw new BusinessException("请选择PPT文件");if(file.getSize()>100L*1024*1024)throw new BusinessException("PPT不能超过 100MB");String original=Optional.ofNullable(file.getOriginalFilename()).orElse("document.pptx");String normalizedType=businessType==null?"OTHER":businessType.trim().toUpperCase(Locale.ROOT);if(!original.toLowerCase(Locale.ROOT).endsWith(".pptx"))throw new BusinessException("仅支持上传 .pptx 成品文件");Map<String,Object> previous=projectId==null?null:mapper.latestDocument(normalizedType,projectId,u.getId());if(previous!=null&&"SUBMITTED".equals(String.valueOf(previous.get("status"))))throw new BusinessException("上一轮PPT仍在审核中，请等待审核后再提交新一轮");int revision=previous==null?1:((Number)previous.get("revision_no")).intValue()+1;if(revision>1&&(description==null||description.isBlank()))throw new BusinessException("请填写本轮修改说明");String safe=original.replaceAll("[^\\p{L}\\p{N}._-]","_");try{Files.createDirectories(root);Path target=root.resolve(u.getId()+"-"+System.currentTimeMillis()+"-"+safe).normalize();if(!target.startsWith(root))throw new BusinessException("文档路径无效");file.transferTo(target);Map<String,Object> row=new HashMap<>();row.put("title",title.trim());row.put("description",description==null?null:description.trim());row.put("businessType",normalizedType);row.put("projectId",projectId);row.put("projectName",projectName);row.put("revisionNo",revision);row.put("previousDocumentId",previous==null?null:previous.get("id"));row.put("originalName",original);row.put("storagePath",target.toString());row.put("contentType",file.getContentType());row.put("fileSize",file.getSize());row.put("submitterId",u.getId());row.put("submitterName",u.getNickname());row.put("deptId",u.getDeptId());mapper.insertDocument(row);return new IdResponse(((Number)row.get("id")).longValue());}catch(Exception e){if(e instanceof BusinessException b)throw b;throw new BusinessException("PPT提交失败："+e.getMessage());}}
 public Map.Entry<String,byte[]> download(Long id){AuthUserRow u=current();Map<String,Object>d=mapper.document(id);if(d==null)throw new BusinessException(404,"文档不存在");long owner=((Number)d.get("submitter_id")).longValue();if(owner!=u.getId()&&!isAdmin(u.getId()))throw new BusinessException(403,"无权访问该文档");try{return Map.entry(String.valueOf(d.get("original_name")),Files.readAllBytes(Path.of(String.valueOf(d.get("storage_path")))));}catch(Exception e){throw new BusinessException(404,"文档文件不存在");}}
 public void feedback(Long id,DocumentFeedbackRequest r){AuthUserRow u=current();requireAdmin();String status=r.getStatus();if(!Set.of("APPROVED","REVISION_REQUIRED").contains(status))throw new BusinessException("审核结果无效");String note=Optional.ofNullable(r.getFeedback()).orElse("").trim();if("REVISION_REQUIRED".equals(status)&&note.isBlank())throw new BusinessException("退回修改时必须填写原因");if(mapper.reviewDocument(id,status,note,u.getId(),u.getNickname())==0)throw new BusinessException("该PPT已由其他审批专员处理，请刷新列表");}
 private AuthUserRow current(){AuthUserRow u=auth.selectUserByUsername(username());if(u==null)throw new BusinessException(401,"登录已过期");return u;}
 private String username(){return String.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal());}
 private boolean isAdmin(Long id){List<String> roles=auth.selectRoleCodes(id);return roles.stream().anyMatch(x->x.equals("super_admin")||x.equals("sys_admin")||x.equals("biz_admin"));}
 private AuthUserRow requireAdmin(){AuthUserRow u=current();if(!isAdmin(u.getId()))throw new BusinessException(403,"仅管理员可执行此操作");return u;}
 private boolean isSuperAdmin(Long id){return auth.selectRoleCodes(id).contains("super_admin");}
 private void requireSuperAdmin(){AuthUserRow u=current();if(!isSuperAdmin(u.getId()))throw new BusinessException(403,"仅超级管理员可设置管理员");}
}
