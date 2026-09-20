package com.scaffold.workflow.controller;

import com.scaffold.common.domain.PageQuery;
import com.scaffold.common.domain.PageResult;
import com.scaffold.common.domain.R;
import com.scaffold.framework.audit.OperLog;
import com.scaffold.framework.security.RequiresPermission;
import com.scaffold.workflow.domain.WorkflowRequests.ApproveRequest;
import com.scaffold.workflow.domain.WorkflowRequests.ModelWriteRequest;
import com.scaffold.workflow.domain.WorkflowRequests.StartInstanceRequest;
import com.scaffold.workflow.service.WorkflowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final WorkflowService service;

    public WorkflowController(WorkflowService service) {
        this.service = service;
    }

    @GetMapping("/models")
    @RequiresPermission("workflow:model:list")
    public R<PageResult<Map<String, Object>>> models(PageQuery query, @RequestParam(required = false) String keyword) {
        return R.success(service.models(query, keyword));
    }

    @PostMapping("/models")
    @RequiresPermission("workflow:model:list")
    @OperLog(module = "流程模型", action = "新增")
    public R<Map<String, Object>> createModel(@RequestBody ModelWriteRequest request) {
        return R.success(service.createModel(request));
    }

    @PutMapping("/models/{id}")
    @RequiresPermission("workflow:model:list")
    @OperLog(module = "流程模型", action = "修改")
    public R<Void> updateModel(@PathVariable Long id, @RequestBody ModelWriteRequest request) {
        service.updateModel(id, request);
        return R.success(null);
    }

    @DeleteMapping("/models/{id}")
    @RequiresPermission("workflow:model:list")
    @OperLog(module = "流程模型", action = "删除")
    public R<Void> deleteModel(@PathVariable Long id) {
        service.deleteModel(id);
        return R.success(null);
    }

    @PostMapping("/models/{id}/deploy")
    @RequiresPermission("workflow:model:list")
    @OperLog(module = "流程模型", action = "部署")
    public R<Void> deployModel(@PathVariable Long id) {
        service.deployModel(id);
        return R.success(null);
    }

    @GetMapping("/definitions")
    @RequiresPermission("workflow:def:list")
    public R<PageResult<Map<String, Object>>> definitions(PageQuery query, @RequestParam(required = false) String keyword) {
        return R.success(service.definitions(query, keyword));
    }

    @PutMapping("/definitions/{id}/suspend")
    @RequiresPermission("workflow:def:list")
    @OperLog(module = "流程定义", action = "状态变更")
    public R<Void> suspendDefinition(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        service.suspendDefinition(id, Boolean.TRUE.equals(request.get("suspended")));
        return R.success(null);
    }

    @PostMapping("/instances/start")
    @RequiresPermission("workflow:def:list")
    @OperLog(module = "流程定义", action = "发起流程")
    public R<Map<String, Object>> startInstance(@RequestBody StartInstanceRequest request) {
        return R.success(service.startInstance(request));
    }

    @GetMapping("/tasks/todo")
    @RequiresPermission("workflow:todo:list")
    public R<PageResult<Map<String, Object>>> todoTasks(PageQuery query, @RequestParam(required = false) String keyword) {
        return R.success(service.todoTasks(query, keyword));
    }

    @PostMapping("/tasks/{id}/approve")
    @RequiresPermission("workflow:todo:list")
    @OperLog(module = "我的待办", action = "审批")
    public R<Void> approveTask(@PathVariable String id, @RequestBody(required = false) ApproveRequest request) {
        service.approveTask(id, request);
        return R.success(null);
    }

    @GetMapping("/tasks/done")
    @RequiresPermission("workflow:done:list")
    public R<PageResult<Map<String, Object>>> doneTasks(PageQuery query, @RequestParam(required = false) String keyword) {
        return R.success(service.doneTasks(query, keyword));
    }

    @GetMapping("/tasks/{id}/trace")
    @RequiresPermission("workflow:todo:list")
    public R<List<Map<String, Object>>> trace(@PathVariable String id) {
        return R.success(service.traceByTask(id));
    }

    @GetMapping("/instances/{processInstanceId}/trace")
    @RequiresPermission("workflow:todo:list")
    public R<List<Map<String, Object>>> instanceTrace(@PathVariable String processInstanceId) {
        return R.success(service.traceByInstance(processInstanceId));
    }
}
