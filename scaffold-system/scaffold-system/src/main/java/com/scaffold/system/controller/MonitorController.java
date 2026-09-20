package com.scaffold.system.controller;

import com.scaffold.common.domain.PageQuery;
import com.scaffold.common.domain.PageResult;
import com.scaffold.common.domain.R;
import com.scaffold.framework.security.RequiresPermission;
import com.scaffold.system.service.SystemManagementService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final SystemManagementService service;

    public MonitorController(SystemManagementService service) {
        this.service = service;
    }

    @GetMapping("/oper-logs")
    @RequiresPermission("monitor:operlog:list")
    public R<PageResult<Map<String, Object>>> operLogs(
            PageQuery query,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer result
    ) {
        return R.success(service.operLogs(query, keyword, result));
    }

    @GetMapping("/oper-logs/export")
    @RequiresPermission("monitor:operlog:list")
    public ResponseEntity<String> exportOperLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer result
    ) {
        return csv("oper-logs.csv", service.exportOperLogs(keyword, result));
    }

    @DeleteMapping("/oper-logs/{id}")
    @RequiresPermission("monitor:operlog:remove")
    public R<Void> deleteOperLog(@PathVariable Long id) {
        service.deleteOperLog(id);
        return R.success(null);
    }

    @DeleteMapping("/oper-logs")
    @RequiresPermission("monitor:operlog:remove")
    public R<Void> clearOperLogs() {
        service.clearOperLogs();
        return R.success(null);
    }

    @GetMapping("/login-logs")
    @RequiresPermission("monitor:loginlog:list")
    public R<PageResult<Map<String, Object>>> loginLogs(
            PageQuery query,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        return R.success(service.loginLogs(query, keyword, status));
    }

    @GetMapping("/login-logs/export")
    @RequiresPermission("monitor:loginlog:list")
    public ResponseEntity<String> exportLoginLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        return csv("login-logs.csv", service.exportLoginLogs(keyword, status));
    }

    @DeleteMapping("/login-logs/{id}")
    @RequiresPermission("monitor:loginlog:remove")
    public R<Void> deleteLoginLog(@PathVariable Long id) {
        service.deleteLoginLog(id);
        return R.success(null);
    }

    @DeleteMapping("/login-logs")
    @RequiresPermission("monitor:loginlog:remove")
    public R<Void> clearLoginLogs() {
        service.clearLoginLogs();
        return R.success(null);
    }

    private ResponseEntity<String> csv(String filename, String content) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content);
    }
}
