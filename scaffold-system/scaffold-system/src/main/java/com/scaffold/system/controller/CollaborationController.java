package com.scaffold.system.controller;

import com.scaffold.common.domain.R;
import com.scaffold.framework.audit.OperLog;
import com.scaffold.framework.security.CaptchaService;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.domain.dto.*;
import com.scaffold.system.domain.vo.IdResponse;
import com.scaffold.system.service.CollaborationService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController @RequestMapping("/api")
public class CollaborationController {
 private final CollaborationService service; private final CaptchaService captchaService;
 public CollaborationController(CollaborationService s,CaptchaService captchaService){service=s;this.captchaService=captchaService;}
 @GetMapping("/public/departments") public R<List<Map<String,Object>>> departments(){return R.success(service.departments());}
 @PostMapping("/auth/register") public R<Void> register(@Valid @RequestBody RegisterRequest r){if(!captchaService.verify(r.getCaptchaUuid(),r.getCaptcha()))throw new BusinessException("验证码错误或已过期");service.register(r);return R.success(null);}
 @GetMapping("/admin/registrations") public R<List<Map<String,Object>>> registrations(){return R.success(service.registrations());}
 @PutMapping("/admin/registrations/{id}/administrator") @OperLog(module="注册用户",action="设置管理员") public R<Void> administrator(@PathVariable Long id,@RequestBody AdministratorAssignmentRequest r){service.setAdministrator(id,r.isAdministrator());return R.success(null);}
 @GetMapping("/documents") public R<List<Map<String,Object>>> documents(){return R.success(service.documents());}
 @PostMapping(value="/documents",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @OperLog(module="文档中心",action="提交") public R<IdResponse> upload(@RequestParam String title,@RequestParam(required=false)String description,@RequestParam(required=false)String businessType,@RequestParam(required=false)Long projectId,@RequestParam(required=false)String projectName,@RequestPart MultipartFile file){return R.success(service.upload(title,description,businessType,projectId,projectName,file));}
 @GetMapping("/documents/{id}/file") public ResponseEntity<byte[]> file(@PathVariable Long id){var f=service.download(id);return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(f.getKey(),StandardCharsets.UTF_8).build().toString()).body(f.getValue());}
 @PutMapping("/documents/{id}/feedback") @OperLog(module="文档中心",action="点评") public R<Void> feedback(@PathVariable Long id,@Valid @RequestBody DocumentFeedbackRequest r){service.feedback(id,r);return R.success(null);}
}
