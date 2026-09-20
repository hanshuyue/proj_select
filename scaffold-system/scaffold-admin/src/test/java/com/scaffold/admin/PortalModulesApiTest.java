package com.scaffold.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ScaffoldAdminApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class PortalModulesApiTest {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedPortalRows() {
        jdbcTemplate.update("""
                INSERT INTO wf_model_meta (model_key, model_name, category, deployed, description, create_by, update_by)
                VALUES ('it_purchase', '集成测试采购审批', '财务审批', 1, '采购申请与预算校验', 'test', 'test')
                ON DUPLICATE KEY UPDATE model_name = VALUES(model_name), category = VALUES(category), deployed = 1,
                    description = VALUES(description), update_by = 'test'
                """);
        jdbcTemplate.update("""
                INSERT INTO wf_business (business_key, business_type, business_title, process_definition_id,
                    process_instance_id, business_status, starter, create_by, update_by)
                VALUES ('IT-PO-001', 'purchase', '集成测试采购单', 'it_purchase:1', 'it-pi-001', 'TODO', '陈思远', 'test', 'test')
                ON DUPLICATE KEY UPDATE business_title = VALUES(business_title), business_status = 'TODO',
                    starter = VALUES(starter), update_by = 'test'
                """);
        jdbcTemplate.update("""
                INSERT INTO wf_business (business_key, business_type, business_title, process_definition_id,
                    process_instance_id, business_status, starter, create_by, update_by)
                VALUES ('IT-PO-002', 'purchase', '集成测试已办采购单', 'it_purchase:1', 'it-pi-002', 'PASS', '吴敏', 'test', 'test')
                ON DUPLICATE KEY UPDATE business_title = VALUES(business_title), business_status = 'PASS',
                    starter = VALUES(starter), update_by = 'test'
                """);
        jdbcTemplate.update("""
                INSERT INTO gen_table (table_name, table_comment, class_name, module_name, business_name, synced, create_by, update_by)
                VALUES ('it_purchase_order', '集成测试采购订单', 'PurchaseOrder', 'workflow', 'purchaseOrder', 1, 'test', 'test')
                ON DUPLICATE KEY UPDATE table_comment = VALUES(table_comment), class_name = VALUES(class_name),
                    module_name = VALUES(module_name), business_name = VALUES(business_name), synced = 1, update_by = 'test'
                """);
        Long tableId = jdbcTemplate.queryForObject("SELECT id FROM gen_table WHERE table_name = 'it_purchase_order'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO gen_table_column (table_id, column_name, java_field, java_type, jdbc_type, column_comment,
                    is_insert, is_edit, is_list, is_query, query_type, form_type, required, sort)
                SELECT ?, 'order_no', 'orderNo', 'String', 'varchar', '订单编号', 1, 1, 1, 1, 'LIKE', '文本框', 1, 1
                WHERE NOT EXISTS (
                    SELECT 1 FROM gen_table_column WHERE table_id = ? AND column_name = 'order_no'
                )
                """, tableId, tableId);
        jdbcTemplate.update("""
                INSERT INTO gen_table_column (table_id, column_name, java_field, java_type, jdbc_type, column_comment,
                    is_insert, is_edit, is_list, is_query, query_type, form_type, required, sort)
                SELECT ?, 'amount', 'amount', 'BigDecimal', 'decimal', '采购金额', 1, 1, 1, 0, '=', '数字输入框', 1, 2
                WHERE NOT EXISTS (
                    SELECT 1 FROM gen_table_column WHERE table_id = ? AND column_name = 'amount'
                )
                """, tableId, tableId);
    }

    @Test
    void dashboardSummaryReturnsRealCountsTodosAndActivities() {
        HttpEntity<Void> entity = authorizedEntity();

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/dashboard/summary",
                HttpMethod.GET,
                entity,
                MAP_TYPE
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> summary = dataOf(response.getBody());
        assertThat(listOf(summary.get("stats"))).extracting(row -> row.get("key"))
                .contains("users", "online", "todo", "api");
        assertThat(listOf(summary.get("trend"))).hasSize(24);
        assertThat(listOf(summary.get("todos"))).extracting(row -> row.get("bizTitle"))
                .contains("集成测试采购单");
        assertThat(listOf(summary.get("activities")).get(0)).containsKeys("who", "action", "target", "time", "tint");
    }

    @Test
    void workflowApisExposeModelsDefinitionsAndTasks() {
        HttpHeaders headers = bearerHeaders(login("admin", "admin123"));

        ResponseEntity<Map<String, Object>> models = restTemplate.exchange(
                "/api/workflow/models?pageNum=1&pageSize=10&keyword=集成测试",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                MAP_TYPE
        );
        assertThat(models.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> model = rowsOf(dataOf(models.getBody())).get(0);
        assertThat(model).containsEntry("modelKey", "it_purchase");
        assertThat(model).containsKeys("modelName", "category", "version", "deployed", "updateTime", "desc");

        ResponseEntity<Map<String, Object>> deploy = restTemplate.exchange(
                "/api/workflow/models/" + model.get("id") + "/deploy",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                MAP_TYPE
        );
        assertThat(deploy.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map<String, Object>> definitions = restTemplate.exchange(
                "/api/workflow/definitions?pageNum=1&pageSize=10&keyword=集成测试",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                MAP_TYPE
        );
        assertThat(definitions.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> definition = rowsOf(dataOf(definitions.getBody())).get(0);
        assertThat(definition).containsEntry("processKey", "it_purchase");
        assertThat(definition).containsKeys("processName", "deployTime", "suspended", "nodes");

        ResponseEntity<Map<String, Object>> todos = restTemplate.exchange(
                "/api/workflow/tasks/todo?pageNum=1&pageSize=10&keyword=集成测试采购单",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                MAP_TYPE
        );
        assertThat(todos.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> todo = rowsOf(dataOf(todos.getBody())).get(0);
        assertThat(todo).containsEntry("bizTitle", "集成测试采购单");
        assertThat(todo).containsKeys("taskName", "procName", "bizType", "starter", "due", "priority", "node", "total");

        ResponseEntity<Map<String, Object>> approve = restTemplate.exchange(
                "/api/workflow/tasks/" + todo.get("id") + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("result", "pass", "comment", "同意"), headers),
                MAP_TYPE
        );
        assertThat(approve.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map<String, Object>> done = restTemplate.exchange(
                "/api/workflow/tasks/done?pageNum=1&pageSize=10&keyword=集成测试采购单",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                MAP_TYPE
        );
        assertThat(done.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rowsOf(dataOf(done.getBody()))).extracting(row -> row.get("result")).contains("通过");

        ResponseEntity<Map<String, Object>> trace = restTemplate.exchange(
                "/api/workflow/tasks/" + todo.get("id") + "/trace",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                MAP_TYPE
        );
        assertThat(trace.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listDataOf(trace.getBody())).extracting(row -> row.get("name"))
                .contains("提交申请", "部门负责人审批", "财务复核");
    }

    @Test
    void generatorApisExposeTableColumnsPreviewAndSave() {
        HttpHeaders headers = bearerHeaders(login("admin", "admin123"));

        ResponseEntity<Map<String, Object>> tables = restTemplate.exchange(
                "/api/tool/generator/tables?pageNum=1&pageSize=10&keyword=it_purchase_order",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                MAP_TYPE
        );
        assertThat(tables.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> table = rowsOf(dataOf(tables.getBody())).get(0);
        assertThat(table).containsEntry("tableName", "it_purchase_order");
        assertThat(table).containsKeys("tableComment", "className", "module", "synced", "createTime");

        ResponseEntity<Map<String, Object>> columns = restTemplate.exchange(
                "/api/tool/generator/tables/" + table.get("id") + "/columns",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                MAP_TYPE
        );
        assertThat(columns.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listDataOf(columns.getBody())).extracting(row -> row.get("javaField"))
                .contains("orderNo", "amount");

        ResponseEntity<Map<String, Object>> preview = restTemplate.exchange(
                "/api/tool/generator/tables/" + table.get("id") + "/preview",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                MAP_TYPE
        );
        assertThat(preview.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> previewData = dataOf(preview.getBody());
        assertThat(listOf(previewData.get("files"))).extracting(row -> row.get("name"))
                .contains("PurchaseOrderController.java", "PurchaseOrderMapper.java", "index.vue");
        assertThat((String) previewData.get("code")).contains("class PurchaseOrderController");

        ResponseEntity<Map<String, Object>> save = restTemplate.exchange(
                "/api/tool/generator/tables/" + table.get("id"),
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("tableComment", "集成测试采购订单-已保存", "synced", false), headers),
                MAP_TYPE
        );
        assertThat(save.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private HttpEntity<Void> authorizedEntity() {
        String token = login("admin", "admin123");
        return new HttpEntity<>(bearerHeaders(token));
    }

    private String login(String username, String password) {
        return TestAuthClient.login(restTemplate, username, password);
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(Map<String, Object> body) {
        assertThat(body).containsEntry("code", 200);
        return (Map<String, Object>) body.get("data");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listDataOf(Map<String, Object> body) {
        assertThat(body).containsEntry("code", 200);
        return (List<Map<String, Object>>) body.get("data");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rowsOf(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("rows");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOf(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
