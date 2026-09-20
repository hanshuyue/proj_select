package com.scaffold.workflow.service;

import com.scaffold.common.domain.PageQuery;
import com.scaffold.common.domain.PageResult;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.workflow.domain.WorkflowRequests.ApproveRequest;
import com.scaffold.workflow.domain.WorkflowRequests.ModelWriteRequest;
import com.scaffold.workflow.domain.WorkflowRequests.StartInstanceRequest;
import com.scaffold.workflow.domain.WorkflowStatus;
import com.scaffold.workflow.mapper.WorkflowMapper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class WorkflowService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WorkflowMapper mapper;
    private final BpmnTemplateService bpmnTemplateService;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    public WorkflowService(WorkflowMapper mapper,
                           BpmnTemplateService bpmnTemplateService,
                           RepositoryService repositoryService,
                           RuntimeService runtimeService,
                           TaskService taskService,
                           HistoryService historyService) {
        this.mapper = mapper;
        this.bpmnTemplateService = bpmnTemplateService;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
    }

    public PageResult<Map<String, Object>> models(PageQuery query, String keyword) {
        Page page = pageOf(query);
        return PageResult.of(booleanRows(mapper.selectModels(keyword, page.offset(), page.pageSize()), "deployed"),
                mapper.countModels(keyword), page.pageNum(), page.pageSize());
    }

    @Transactional
    public Map<String, Object> createModel(ModelWriteRequest request) {
        if (request == null) {
            throw new BusinessException("流程模型参数不能为空");
        }
        String modelKey = bpmnTemplateService.safeKey(required(request == null ? null : request.modelKey(), "流程模型标识不能为空"));
        String modelName = required(request.modelName(), "流程模型名称不能为空");
        if (mapper.countModelKey(modelKey, null) > 0) {
            throw new BusinessException("流程模型标识已存在");
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("modelKey", modelKey);
        row.put("modelName", modelName);
        row.put("category", trimToNull(request.category()));
        row.put("desc", trimToNull(request.desc()));
        row.put("bpmnXml", bpmnTemplateService.normalizeXml(request.bpmnXml(), modelKey, modelName));
        mapper.insertModel(row, currentUsername());
        return Map.of("id", numberAsLong(row.get("id")));
    }

    @Transactional
    public void updateModel(Long id, ModelWriteRequest request) {
        if (request == null) {
            throw new BusinessException("流程模型参数不能为空");
        }
        Map<String, Object> existing = modelOrThrow(id);
        String modelKey = asString(existing.get("modelKey"));
        String modelName = trimToNull(request.modelName());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("modelName", modelName);
        row.put("category", trimToNull(request.category()));
        row.put("desc", trimToNull(request.desc()));
        row.put("bpmnXml", request.bpmnXml() == null ? null :
                bpmnTemplateService.normalizeXml(request.bpmnXml(), modelKey, modelName == null ? asString(existing.get("modelName")) : modelName));
        if (mapper.updateModel(id, row, currentUsername()) == 0) {
            throw new BusinessException("流程模型不存在");
        }
    }

    @Transactional
    public void deleteModel(Long id) {
        if (mapper.deleteModel(id) == 0) {
            throw new BusinessException("流程模型不存在");
        }
    }

    @Transactional
    public void deployModel(Long id) {
        Map<String, Object> model = modelOrThrow(id);
        String modelKey = asString(model.get("modelKey"));
        String modelName = asString(model.get("modelName"));
        String bpmnXml = bpmnTemplateService.normalizeXml(asString(model.get("bpmnXml")), modelKey, modelName);
        Deployment deployment = repositoryService.createDeployment()
                .name(modelName)
                .key(modelKey)
                .addString(modelKey + ".bpmn20.xml", bpmnXml)
                .deploy();
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        if (definition == null) {
            throw new BusinessException("流程部署失败，未生成流程定义");
        }
        mapper.updateModelDeployment(id, deployment.getId(), definition.getId(), definition.getVersion(), bpmnXml, currentUsername());
    }

    public PageResult<Map<String, Object>> definitions(PageQuery query, String keyword) {
        deployMissingDefinitions();
        Page page = pageOf(query);
        List<Map<String, Object>> rows = booleanRows(mapper.selectDefinitions(keyword, page.offset(), page.pageSize()), "suspended");
        for (Map<String, Object> row : rows) {
            row.put("nodes", bpmnTemplateService.nodeNames(asString(row.remove("bpmnXml"))));
            String processDefinitionId = asString(row.get("processDefinitionId"));
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            if (definition != null) {
                row.put("suspended", definition.isSuspended());
            }
        }
        return PageResult.of(rows, mapper.countDefinitions(keyword), page.pageNum(), page.pageSize());
    }

    private void deployMissingDefinitions() {
        for (Map<String, Object> model : mapper.selectLegacyDeployedModelsWithoutDefinition()) {
            Long id = numberAsLong(model.get("id"));
            String modelKey = asString(model.get("modelKey"));
            String modelName = asString(model.get("modelName"));
            String bpmnXml = bpmnTemplateService.normalizeXml(asString(model.get("bpmnXml")), modelKey, modelName);
            Deployment deployment = repositoryService.createDeployment()
                    .name(modelName)
                    .key(modelKey)
                    .addString(modelKey + ".bpmn20.xml", bpmnXml)
                    .deploy();
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();
            if (definition != null) {
                mapper.updateModelDeployment(id, deployment.getId(), definition.getId(), definition.getVersion(), bpmnXml, "system");
            }
        }
    }

    @Transactional
    public void suspendDefinition(Long id, boolean suspended) {
        Map<String, Object> model = modelOrThrow(id);
        String processDefinitionId = asString(model.get("processDefinitionId"));
        if (processDefinitionId == null) {
            throw new BusinessException("流程定义不存在");
        }
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        if (definition == null) {
            throw new BusinessException("流程定义不存在");
        }
        if (suspended && !definition.isSuspended()) {
            repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);
        }
        if (!suspended && definition.isSuspended()) {
            repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);
        }
        mapper.updateDefinitionStatus(id, suspended, currentUsername());
    }

    @Transactional
    public Map<String, Object> startInstance(StartInstanceRequest request) {
        if (request == null) {
            throw new BusinessException("启动流程参数不能为空");
        }
        String processDefinitionId = required(request.processDefinitionId(), "流程定义ID不能为空");
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        if (definition == null) {
            throw new BusinessException("流程定义不存在");
        }
        if (definition.isSuspended()) {
            throw new BusinessException("流程定义已挂起，不能发起流程");
        }
        String username = currentUsername();
        String businessType = required(request.businessType(), "业务类型不能为空");
        String businessKey = trimToNull(request.businessKey());
        if (businessKey == null) {
            businessKey = businessType + "-" + System.currentTimeMillis();
        }
        String businessTitle = trimToNull(request.businessTitle());
        if (businessTitle == null) {
            businessTitle = definition.getName() + "-" + businessKey;
        }
        Map<String, Object> variables = new HashMap<>();
        if (request.variables() != null) {
            variables.putAll(request.variables());
        }
        variables.put("starter", username);
        variables.put("businessTitle", businessTitle);
        ProcessInstance instance = runtimeService.startProcessInstanceById(processDefinitionId, businessKey, variables);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("businessKey", businessKey);
        row.put("businessType", businessType);
        row.put("businessTitle", businessTitle);
        row.put("processDefinitionId", processDefinitionId);
        row.put("processInstanceId", instance.getId());
        row.put("businessStatus", WorkflowStatus.TODO);
        row.put("starter", username);
        mapper.insertBusiness(row, username);
        insertRecord(numberAsLong(row.get("id")), instance.getId(), null, "提交申请",
                "start", "发起流程", username, WorkflowStatus.TODO);
        return Map.of(
                "businessId", numberAsLong(row.get("id")),
                "processInstanceId", instance.getId(),
                "businessKey", businessKey
        );
    }

    public PageResult<Map<String, Object>> todoTasks(PageQuery query, String keyword) {
        Page page = pageOf(query);
        String username = currentUsername();
        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> flowableRows = flowableTodoRows(username, keyword);
        rows.addAll(flowableRows);
        Set<String> activeInstances = new LinkedHashSet<>();
        for (Map<String, Object> row : flowableRows) {
            String processInstanceId = asString(row.get("processInstanceId"));
            if (processInstanceId != null) {
                activeInstances.add(processInstanceId);
            }
        }
        for (Map<String, Object> row : mapper.selectLegacyTodoTasks(keyword, 0, Math.max(page.pageSize(), 50))) {
            if (!activeInstances.contains(asString(row.get("processInstanceId")))) {
                List<String> nodes = bpmnTemplateService.nodeNames(asString(row.remove("bpmnXml")));
                row.put("nodes", nodes);
                row.put("total", nodes.size());
                rows.add(row);
            }
        }
        rows.sort(Comparator.comparing(row -> asString(row.get("createTime")), Comparator.nullsLast(Comparator.reverseOrder())));
        return PageResult.of(pageRows(rows, page), rows.size(), page.pageNum(), page.pageSize());
    }

    @Transactional
    public void approveTask(String id, ApproveRequest request) {
        String action = WorkflowStatus.normalizeAction(request == null ? null : request.result());
        String comment = trimToNull(request == null ? null : request.comment());
        if (comment == null) {
            comment = WorkflowStatus.labelOfAction(action);
        }
        Task task = taskService.createTaskQuery().taskId(id).singleResult();
        if (task == null) {
            approveLegacyTask(id, action, comment);
            return;
        }
        String username = currentUsername();
        Map<String, Object> business = mapper.selectBusinessByProcessInstanceId(task.getProcessInstanceId());
        if (business == null) {
            throw new BusinessException("流程业务绑定不存在");
        }
        Long businessId = numberAsLong(business.get("id"));
        assertTaskHandler(task, username);
        if ("revoke".equals(action) && !username.equals(asString(business.get("starter")))) {
            throw new BusinessException("只有流程发起人可以撤回");
        }
        if ("pass".equals(action)) {
            if (task.getAssignee() == null || task.getAssignee().isBlank()) {
                taskService.setAssignee(task.getId(), username);
            }
            taskService.complete(task.getId());
            long activeTasks = taskService.createTaskQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .count();
            String status = activeTasks == 0 ? WorkflowStatus.PASS : WorkflowStatus.TODO;
            mapper.updateBusinessStatusByInstance(task.getProcessInstanceId(), status, username);
            insertRecord(businessId, task.getProcessInstanceId(), task.getId(), task.getName(),
                    action, comment, username, status);
            return;
        }
        String terminalStatus = WorkflowStatus.terminalStatusOfAction(action);
        if (runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult() != null) {
            runtimeService.deleteProcessInstance(task.getProcessInstanceId(), WorkflowStatus.labelOfAction(action));
        }
        mapper.updateBusinessStatusByInstance(task.getProcessInstanceId(), terminalStatus, username);
        insertRecord(businessId, task.getProcessInstanceId(), task.getId(), task.getName(),
                action, comment, username, terminalStatus);
    }

    public PageResult<Map<String, Object>> doneTasks(PageQuery query, String keyword) {
        Page page = pageOf(query);
        String username = currentUsername();
        return PageResult.of(mapper.selectDoneTasks(keyword, username, page.offset(), page.pageSize()),
                mapper.countDoneTasks(keyword, username), page.pageNum(), page.pageSize());
    }

    public List<Map<String, Object>> traceByTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task != null) {
            return traceByInstance(task.getProcessInstanceId());
        }
        try {
            Map<String, Object> business = mapper.selectBusinessById(Long.valueOf(taskId));
            if (business != null) {
                return traceByInstance(asString(business.get("processInstanceId")));
            }
        } catch (NumberFormatException ignored) {
            // Flowable task ids are strings; non-numeric missing ids fall back to the static trace below.
        }
        return legacyTrace();
    }

    public List<Map<String, Object>> traceByInstance(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return legacyTrace();
        }
        boolean knownFlowableInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .count() > 0
                || historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .count() > 0;
        if (!knownFlowableInstance) {
            return legacyTrace();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> record : mapper.selectTraceRecords(processInstanceId)) {
            String action = asString(record.get("action"));
            rows.add(traceItem(
                    asString(record.get("taskName")),
                    asString(record.get("taskName")),
                    asString(record.get("assignee")),
                    asString(record.get("handleTime")),
                    WorkflowStatus.labelOfAction(action),
                    WorkflowStatus.toneOfAction(action),
                    asString(record.get("comment")),
                    true,
                    false
            ));
        }
        List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .asc()
                .list();
        for (Task task : activeTasks) {
            rows.add(traceItem(task.getName(), task.getName(), "当前处理人", "处理中",
                    "待处理", "warn", "等待审批", false, true));
        }
        boolean finished = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .count() > 0;
        if (finished && activeTasks.isEmpty() && rows.stream().noneMatch(row -> "归档".equals(row.get("node")))) {
            rows.add(traceItem("归档", "流程结束归档", "系统", "已完成", "已结束", "ok", "流程已结束", true, false));
        }
        if (rows.isEmpty()) {
            if (finished) {
                rows.add(traceItem("归档", "归档", "系统", "已完成", "已结束", "ok", "流程已结束", true, false));
            }
        }
        return rows.isEmpty() ? legacyTrace() : rows;
    }

    private List<Map<String, Object>> flowableTodoRows(String username, String keyword) {
        List<String> groups = mapper.selectRoleCodesByUsername(username);
        Set<String> taskIds = new LinkedHashSet<>();
        List<Task> tasks = new ArrayList<>();
        for (Task task : taskService.createTaskQuery().taskAssignee(username).list()) {
            if (taskIds.add(task.getId())) {
                tasks.add(task);
            }
        }
        if (!groups.isEmpty()) {
            for (Task task : taskService.createTaskQuery().taskCandidateGroupIn(groups).list()) {
                if (taskIds.add(task.getId())) {
                    tasks.add(task);
                }
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Task task : tasks) {
            Map<String, Object> business = mapper.selectBusinessByProcessInstanceId(task.getProcessInstanceId());
            if (business == null || !matchesKeyword(business, task, keyword)) {
                continue;
            }
            rows.add(todoRow(task, business));
        }
        return rows;
    }

    private void assertTaskHandler(Task task, String username) {
        if (username.equals(task.getAssignee())) {
            return;
        }
        if (task.getAssignee() != null && !task.getAssignee().isBlank()) {
            throw new BusinessException("无权处理当前待办任务");
        }
        List<String> groups = mapper.selectRoleCodesByUsername(username);
        for (IdentityLink link : taskService.getIdentityLinksForTask(task.getId())) {
            if (username.equals(link.getUserId())) {
                return;
            }
            String groupId = link.getGroupId();
            if (groupId != null && groups.contains(groupId)) {
                return;
            }
        }
        throw new BusinessException("无权处理当前待办任务");
    }

    private Map<String, Object> todoRow(Task task, Map<String, Object> business) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", task.getId());
        row.put("businessId", business.get("id"));
        row.put("businessKey", business.get("businessKey"));
        row.put("processInstanceId", task.getProcessInstanceId());
        row.put("taskName", task.getName());
        row.put("procName", processDefinitionName(asString(business.get("processDefinitionId"))));
        row.put("bizTitle", business.get("businessTitle"));
        row.put("bizType", business.get("businessType"));
        row.put("starter", business.get("starter"));
        row.put("createTime", format(task.getCreateTime()));
        row.put("due", task.getDueDate() == null ? "未设置" : format(task.getDueDate()));
        row.put("priority", task.getPriority() >= 80 ? "high" : "mid");
        row.put("node", task.getName());
        List<String> nodes = bpmnTemplateService.nodeNames(mapper.selectBpmnXmlByProcessDefinitionId(asString(business.get("processDefinitionId"))));
        row.put("nodes", nodes);
        row.put("total", nodes.size());
        return row;
    }

    private void approveLegacyTask(String id, String action, String comment) {
        Long businessId;
        try {
            businessId = Long.valueOf(id);
        } catch (NumberFormatException exception) {
            throw new BusinessException("待办任务不存在");
        }
        Map<String, Object> business = mapper.selectBusinessById(businessId);
        if (business == null) {
            throw new BusinessException("待办任务不存在");
        }
        String username = currentUsername();
        if ("revoke".equals(action) && !username.equals(asString(business.get("starter")))) {
            throw new BusinessException("只有流程发起人可以撤回");
        }
        String status = WorkflowStatus.terminalStatusOfAction(action);
        mapper.updateBusinessStatusById(businessId, status, username);
        insertRecord(businessId, asString(business.get("processInstanceId")), id, "部门负责人审批",
                action, comment, username, status);
    }

    private void insertRecord(Long businessId,
                              String processInstanceId,
                              String taskId,
                              String taskName,
                              String action,
                              String comment,
                              String assignee,
                              String resultStatus) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("businessId", businessId);
        row.put("processInstanceId", processInstanceId);
        row.put("taskId", taskId);
        row.put("taskName", taskName);
        row.put("action", WorkflowStatus.normalizeAction(action));
        row.put("comment", comment);
        row.put("assignee", assignee);
        row.put("resultStatus", resultStatus);
        mapper.insertApprovalRecord(row, assignee);
    }

    private Map<String, Object> modelOrThrow(Long id) {
        Map<String, Object> model = mapper.selectModelById(id);
        if (model == null) {
            throw new BusinessException("流程模型不存在");
        }
        return model;
    }

    private boolean matchesKeyword(Map<String, Object> business, Task task, String keyword) {
        String text = trimToNull(keyword);
        if (text == null) {
            return true;
        }
        return List.of(business.get("businessTitle"), business.get("businessKey"), business.get("starter"), task.getName()).stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .anyMatch(value -> value.contains(text));
    }

    private String processDefinitionName(String processDefinitionId) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        return definition == null ? "流程审批" : definition.getName();
    }

    private List<Map<String, Object>> pageRows(List<Map<String, Object>> rows, Page page) {
        int from = Math.min(page.offset(), rows.size());
        int to = Math.min(from + page.pageSize(), rows.size());
        return rows.subList(from, to);
    }

    private List<Map<String, Object>> legacyTrace() {
        return List.of(
                traceItem("提交申请", "发起人提交业务单据", "系统", "已完成", "通过", "ok", "发起人提交业务单据", true, false),
                traceItem("部门负责人审批", "校验业务合理性与预算归属", "系统", "已完成", "通过", "ok", "校验业务合理性与预算归属", true, false),
                traceItem("财务复核", "复核金额、科目和付款计划", "当前处理人", "处理中", "待处理", "warn", "复核金额、科目和付款计划", false, true)
        );
    }

    private Map<String, Object> traceItem(String name,
                                          String desc,
                                          String who,
                                          String time,
                                          String result,
                                          String tone,
                                          String comment,
                                          boolean done,
                                          boolean current) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("node", name);
        row.put("desc", desc);
        row.put("who", who);
        row.put("time", time);
        row.put("result", result);
        row.put("tone", tone);
        row.put("comment", comment);
        row.put("done", done);
        row.put("status", current ? "current" : done ? "done" : "wait");
        row.put("current", current);
        return row;
    }

    private List<Map<String, Object>> booleanRows(List<Map<String, Object>> rows, String key) {
        for (Map<String, Object> row : rows) {
            Object value = row.get(key);
            if (value instanceof Number number) {
                row.put(key, number.intValue() == 1);
            }
        }
        return rows;
    }

    private Page pageOf(PageQuery query) {
        int pageNum = query == null ? PageQuery.DEFAULT_PAGE_NUM : query.normalizedPageNum();
        int pageSize = query == null ? PageQuery.DEFAULT_PAGE_SIZE : query.normalizedPageSize();
        return new Page(pageNum, pageSize);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : String.valueOf(authentication.getPrincipal());
    }

    private String required(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException(message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long numberAsLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private String format(java.util.Date date) {
        return DATE_TIME.format(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    private record Page(int pageNum, int pageSize) {
        int offset() {
            return (pageNum - 1) * pageSize;
        }
    }
}
