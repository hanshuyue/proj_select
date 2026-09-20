package com.scaffold.system.controller;

import com.scaffold.common.domain.*;
import com.scaffold.framework.audit.OperLog;
import com.scaffold.framework.security.RequiresPermission;
import com.scaffold.system.domain.dto.InitiationProjectRequest;
import com.scaffold.system.domain.vo.IdResponse;
import com.scaffold.system.service.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/initiation")
public class InitiationController {
    private final InitiationProjectService projects; private final InitiationTemplateService templates; private final InitiationPptService ppt; private final InitiationAttachmentService attachments;
    public InitiationController(InitiationProjectService projects,InitiationTemplateService templates,InitiationPptService ppt,InitiationAttachmentService attachments){this.projects=projects;this.templates=templates;this.ppt=ppt;this.attachments=attachments;}
    @GetMapping("/projects") @RequiresPermission("initiation:project:list")
    public R<PageResult<Map<String,Object>>> list(PageQuery q,@RequestParam(required=false)String keyword){return R.success(projects.list(q,keyword));}
    @GetMapping("/projects/{id}") @RequiresPermission("initiation:project:list")
    public R<Map<String,Object>> detail(@PathVariable Long id){return R.success(projects.detail(id));}
    @PostMapping("/projects") @RequiresPermission("initiation:project:add") @OperLog(module="项目立项",action="新增")
    public R<IdResponse> create(@RequestBody InitiationProjectRequest r){return R.success(projects.create(r));}
    @PutMapping("/projects/{id}") @RequiresPermission("initiation:project:edit") @OperLog(module="项目立项",action="修改")
    public R<Void> update(@PathVariable Long id,@RequestBody InitiationProjectRequest r){projects.update(id,r);return R.success(null);}
    @DeleteMapping("/projects/{id}") @RequiresPermission("initiation:project:remove")
    public R<Void> delete(@PathVariable Long id){projects.delete(id);return R.success(null);}
    @GetMapping("/projects/{id}/attachments") @RequiresPermission("initiation:project:list")
    public R<List<Map<String,Object>>> attachments(@PathVariable Long id){return R.success(attachments.list(id));}
    @PostMapping(value="/projects/{id}/attachments",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @RequiresPermission("initiation:project:edit")
    public R<IdResponse> uploadAttachment(@PathVariable Long id,@RequestParam(required=false) String type,@RequestPart("file") MultipartFile file){return R.success(attachments.upload(id,type,file));}
    @GetMapping("/attachments/{id}/file") @RequiresPermission("initiation:project:list")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id){Map.Entry<String,byte[]> a=attachments.download(id);return binary(a.getKey(),MediaType.APPLICATION_OCTET_STREAM_VALUE,a.getValue());}
    @DeleteMapping("/attachments/{id}") @RequiresPermission("initiation:project:edit")
    public R<Void> deleteAttachment(@PathVariable Long id){attachments.delete(id);return R.success(null);}
    @PostMapping("/projects/calculate")
    public R<Map<String,Object>> calculate(@RequestBody InitiationProjectRequest r){return R.success(projects.calculate(r));}
    @GetMapping("/projects/{id}/validate")
    public R<List<String>> validate(@PathVariable Long id){return R.success(projects.validate(id));}
    @GetMapping("/projects/{id}/ppt") @RequiresPermission("initiation:project:export") @OperLog(module="项目立项",action="导出PPT")
    public ResponseEntity<byte[]> export(@PathVariable Long id,@RequestParam(required=false)Long templateId){
        String name=String.valueOf(projects.detail(id).get("projectName"));
        return binary("项目立项-"+name+".pptx","application/vnd.openxmlformats-officedocument.presentationml.presentation",ppt.generate(id,templateId));
    }
    @GetMapping("/templates") @RequiresPermission("initiation:template:list")
    public R<List<Map<String,Object>>> templates(){return R.success(templates.list());}
    @PostMapping(value="/templates",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @RequiresPermission("initiation:template:upload")
    public R<IdResponse> upload(@RequestParam(required=false)String name,@RequestPart("file")MultipartFile file){return R.success(templates.upload(name,file));}
    @PutMapping("/templates/{id}/default") @RequiresPermission("initiation:template:edit")
    public R<Void> setDefault(@PathVariable Long id){templates.setDefault(id);return R.success(null);}
    @GetMapping("/templates/{id}/file") @RequiresPermission("initiation:template:list")
    public ResponseEntity<byte[]> download(@PathVariable Long id){Map<String,Object> t=templates.get(id);return binary(String.valueOf(t.getOrDefault("originalFilename",t.get("original_filename"))),"application/vnd.openxmlformats-officedocument.presentationml.presentation",templates.download(id));}
    @DeleteMapping("/templates/{id}") @RequiresPermission("initiation:template:remove")
    public R<Void> deleteTemplate(@PathVariable Long id){templates.delete(id);return R.success(null);}
    private ResponseEntity<byte[]> binary(String filename,String type,byte[] bytes){return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(filename,StandardCharsets.UTF_8).build().toString()).contentLength(bytes.length).body(bytes);}
}
