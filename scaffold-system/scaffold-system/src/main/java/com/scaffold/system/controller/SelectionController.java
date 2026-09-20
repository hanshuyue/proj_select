package com.scaffold.system.controller;

import com.scaffold.common.domain.PageQuery;
import com.scaffold.common.domain.PageResult;
import com.scaffold.common.domain.R;
import com.scaffold.framework.audit.OperLog;
import com.scaffold.framework.security.RequiresPermission;
import com.scaffold.system.domain.dto.SelectionProjectRequest;
import com.scaffold.system.domain.vo.IdResponse;
import com.scaffold.system.service.PptTemplateService;
import com.scaffold.system.service.SelectionProjectService;
import com.scaffold.system.service.SelectionTemplateService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/selection")
public class SelectionController {
    private final SelectionProjectService projectService;
    private final SelectionTemplateService templateService;
    private final PptTemplateService pptTemplateService;

    public SelectionController(SelectionProjectService projectService, SelectionTemplateService templateService,
                               PptTemplateService pptTemplateService) {
        this.projectService = projectService;
        this.templateService = templateService;
        this.pptTemplateService = pptTemplateService;
    }

    @GetMapping("/projects")
    @RequiresPermission("selection:project:list")
    public R<PageResult<Map<String, Object>>> list(PageQuery query, @RequestParam(required = false) String keyword) {
        return R.success(projectService.list(query, keyword));
    }

    @GetMapping("/projects/{id}")
    @RequiresPermission("selection:project:list")
    public R<Map<String, Object>> detail(@PathVariable Long id) { return R.success(projectService.detail(id)); }

    @PostMapping("/projects")
    @RequiresPermission("selection:project:add")
    @OperLog(module = "甄选结果", action = "新增")
    public R<IdResponse> create(@RequestBody SelectionProjectRequest request) {
        return R.success(projectService.create(request));
    }

    @PutMapping("/projects/{id}")
    @RequiresPermission("selection:project:edit")
    @OperLog(module = "甄选结果", action = "修改")
    public R<Void> update(@PathVariable Long id, @RequestBody SelectionProjectRequest request) {
        projectService.update(id, request);
        return R.success(null);
    }

    @DeleteMapping("/projects/{id}")
    @RequiresPermission("selection:project:remove")
    @OperLog(module = "甄选结果", action = "删除")
    public R<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return R.success(null);
    }

    @PostMapping(value = "/projects/{id}/review-report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission("selection:project:edit")
    @OperLog(module = "甄选结果", action = "上传评审报告")
    public R<Map<String, String>> uploadReviewReport(@PathVariable Long id,
                                                     @RequestPart("file") MultipartFile file) {
        return R.success(projectService.uploadReviewReport(id, file));
    }

    @GetMapping("/projects/{id}/review-report")
    @RequiresPermission("selection:project:list")
    public ResponseEntity<byte[]> downloadReviewReport(@PathVariable Long id) {
        Map.Entry<String, byte[]> report = projectService.downloadReviewReport(id);
        return binary(report.getKey(), MediaType.APPLICATION_OCTET_STREAM_VALUE, report.getValue());
    }

    @GetMapping("/projects/{id}/ppt")
    @RequiresPermission("selection:project:export")
    @OperLog(module = "甄选结果", action = "导出PPT")
    public ResponseEntity<byte[]> export(@PathVariable Long id, @RequestParam(required = false) Long templateId) {
        Map<String, Object> project = projectService.detail(id);
        String filename = "甄选结果_" + String.valueOf(project.get("projectName")) + ".pptx";
        return binary(filename, "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                pptTemplateService.generatePptFromTemplate(id, templateId));
    }

    @GetMapping("/templates")
    @RequiresPermission("selection:template:list")
    public R<List<Map<String, Object>>> templates() { return R.success(templateService.list()); }

    @PostMapping(value = "/templates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission("selection:template:upload")
    @OperLog(module = "PPT模板", action = "上传")
    public R<IdResponse> upload(@RequestParam(required = false) String name,
                                @RequestPart("file") MultipartFile file) {
        return R.success(templateService.upload(name, file));
    }

    @PutMapping("/templates/{id}/default")
    @RequiresPermission("selection:template:edit")
    public R<Void> setDefault(@PathVariable Long id) {
        templateService.setDefault(id);
        return R.success(null);
    }

    @GetMapping("/templates/{id}/file")
    @RequiresPermission("selection:template:list")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable Long id) {
        Map<String, Object> row = templateService.get(id);
        return binary(String.valueOf(row.get("originalFilename")),
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                templateService.download(id));
    }

    @DeleteMapping("/templates/{id}")
    @RequiresPermission("selection:template:remove")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        templateService.delete(id);
        return R.success(null);
    }

    private ResponseEntity<byte[]> binary(String filename, String contentType, byte[] bytes) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(bytes.length)
                .body(bytes);
    }
}
