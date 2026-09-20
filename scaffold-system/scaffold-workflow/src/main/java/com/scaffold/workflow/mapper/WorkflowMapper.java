package com.scaffold.workflow.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface WorkflowMapper {

    @Select("""
            SELECT COUNT(*)
            FROM wf_model_meta
            WHERE #{keyword} IS NULL OR #{keyword} = ''
               OR model_key LIKE CONCAT('%', #{keyword}, '%')
               OR model_name LIKE CONCAT('%', #{keyword}, '%')
               OR category LIKE CONCAT('%', #{keyword}, '%')
            """)
    long countModels(@Param("keyword") String keyword);

    @Select("""
            SELECT id,
                   model_key AS modelKey,
                   model_name AS modelName,
                   category,
                   version,
                   deployed,
                   deployment_id AS deploymentId,
                   process_definition_id AS processDefinitionId,
                   bpmn_xml AS bpmnXml,
                   DATE_FORMAT(create_time, '%Y-%m-%d %H:%i') AS createTime,
                   DATE_FORMAT(update_time, '%Y-%m-%d %H:%i') AS updateTime,
                   description AS `desc`
            FROM wf_model_meta
            WHERE #{keyword} IS NULL OR #{keyword} = ''
               OR model_key LIKE CONCAT('%', #{keyword}, '%')
               OR model_name LIKE CONCAT('%', #{keyword}, '%')
               OR category LIKE CONCAT('%', #{keyword}, '%')
            ORDER BY update_time DESC, id DESC
            LIMIT #{offset}, #{pageSize}
            """)
    List<Map<String, Object>> selectModels(@Param("keyword") String keyword, @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("""
            SELECT id,
                   model_key AS modelKey,
                   model_name AS modelName,
                   category,
                   description AS `desc`,
                   bpmn_xml AS bpmnXml,
                   process_definition_id AS processDefinitionId,
                   deployment_id AS deploymentId,
                   version,
                   deployed
            FROM wf_model_meta
            WHERE id = #{id}
            """)
    Map<String, Object> selectModelById(@Param("id") Long id);

    @Select("""
            SELECT COUNT(*)
            FROM wf_model_meta
            WHERE model_key = #{modelKey}
              AND (#{excludeId} IS NULL OR id != #{excludeId})
            """)
    long countModelKey(@Param("modelKey") String modelKey, @Param("excludeId") Long excludeId);

    @Insert("""
            INSERT INTO wf_model_meta (model_key, model_name, category, deployed, description, bpmn_xml, create_by, update_by)
            VALUES (#{row.modelKey}, #{row.modelName}, #{row.category}, 0, #{row.desc}, #{row.bpmnXml}, #{username}, #{username})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "row.id")
    int insertModel(@Param("row") Map<String, Object> row, @Param("username") String username);

    @Update("""
            UPDATE wf_model_meta
            SET model_name = COALESCE(#{row.modelName}, model_name),
                category = COALESCE(#{row.category}, category),
                description = COALESCE(#{row.desc}, description),
                bpmn_xml = COALESCE(#{row.bpmnXml}, bpmn_xml),
                deployed = CASE WHEN #{row.bpmnXml} IS NULL THEN deployed ELSE 0 END,
                update_by = #{username}
            WHERE id = #{id}
            """)
    int updateModel(@Param("id") Long id, @Param("row") Map<String, Object> row, @Param("username") String username);

    @Delete("DELETE FROM wf_model_meta WHERE id = #{id}")
    int deleteModel(@Param("id") Long id);

    @Update("""
            UPDATE wf_model_meta
            SET deployed = 1,
                deployment_id = #{deploymentId},
                process_definition_id = #{processDefinitionId},
                version = #{version},
                bpmn_xml = #{bpmnXml},
                update_by = #{username}
            WHERE id = #{id}
            """)
    int updateModelDeployment(@Param("id") Long id,
                              @Param("deploymentId") String deploymentId,
                              @Param("processDefinitionId") String processDefinitionId,
                              @Param("version") Integer version,
                              @Param("bpmnXml") String bpmnXml,
                              @Param("username") String username);

    @Select("""
            SELECT COUNT(*)
            FROM wf_model_meta
            WHERE deployed = 1
              AND process_definition_id IS NOT NULL
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR model_key LIKE CONCAT('%', #{keyword}, '%')
                   OR model_name LIKE CONCAT('%', #{keyword}, '%')
                   OR category LIKE CONCAT('%', #{keyword}, '%'))
            """)
    long countDefinitions(@Param("keyword") String keyword);

    @Select("""
            SELECT id,
                   model_key AS modelKey,
                   model_name AS modelName,
                   bpmn_xml AS bpmnXml
            FROM wf_model_meta
            WHERE deployed = 1
              AND process_definition_id IS NULL
            ORDER BY id
            """)
    List<Map<String, Object>> selectLegacyDeployedModelsWithoutDefinition();

    @Select("""
            SELECT id,
                   model_key AS processKey,
                   model_name AS processName,
                   category,
                   version,
                   process_definition_id AS processDefinitionId,
                   bpmn_xml AS bpmnXml,
                   DATE_FORMAT(update_time, '%Y-%m-%d %H:%i') AS deployTime,
                   CASE deployed WHEN 1 THEN 0 ELSE 1 END AS suspended
            FROM wf_model_meta
            WHERE deployed = 1
              AND process_definition_id IS NOT NULL
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR model_key LIKE CONCAT('%', #{keyword}, '%')
                   OR model_name LIKE CONCAT('%', #{keyword}, '%')
                   OR category LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY update_time DESC, id DESC
            LIMIT #{offset}, #{pageSize}
            """)
    List<Map<String, Object>> selectDefinitions(@Param("keyword") String keyword, @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("""
            SELECT bpmn_xml
            FROM wf_model_meta
            WHERE process_definition_id = #{processDefinitionId}
            LIMIT 1
            """)
    String selectBpmnXmlByProcessDefinitionId(@Param("processDefinitionId") String processDefinitionId);

    @Update("""
            UPDATE wf_model_meta
            SET deployed = CASE WHEN #{suspended} THEN 0 ELSE 1 END,
                update_by = #{username}
            WHERE id = #{id}
            """)
    int updateDefinitionStatus(@Param("id") Long id, @Param("suspended") boolean suspended, @Param("username") String username);

    @Insert("""
            INSERT INTO wf_business (business_key, business_type, business_title, process_definition_id,
                process_instance_id, business_status, starter, create_by, update_by)
            VALUES (#{row.businessKey}, #{row.businessType}, #{row.businessTitle}, #{row.processDefinitionId},
                #{row.processInstanceId}, #{row.businessStatus}, #{row.starter}, #{username}, #{username})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "row.id")
    int insertBusiness(@Param("row") Map<String, Object> row, @Param("username") String username);

    @Select("""
            SELECT id,
                   business_key AS businessKey,
                   business_type AS businessType,
                   business_title AS businessTitle,
                   process_definition_id AS processDefinitionId,
                   process_instance_id AS processInstanceId,
                   business_status AS businessStatus,
                   starter,
                   DATE_FORMAT(create_time, '%Y-%m-%d %H:%i') AS createTime
            FROM wf_business
            WHERE process_instance_id = #{processInstanceId}
            LIMIT 1
            """)
    Map<String, Object> selectBusinessByProcessInstanceId(@Param("processInstanceId") String processInstanceId);

    @Select("""
            SELECT id,
                   business_key AS businessKey,
                   business_type AS businessType,
                   business_title AS businessTitle,
                   process_definition_id AS processDefinitionId,
                   process_instance_id AS processInstanceId,
                   business_status AS businessStatus,
                   starter,
                   DATE_FORMAT(create_time, '%Y-%m-%d %H:%i') AS createTime
            FROM wf_business
            WHERE id = #{id}
            """)
    Map<String, Object> selectBusinessById(@Param("id") Long id);

    @Update("""
            UPDATE wf_business
            SET business_status = #{status},
                update_by = #{username}
            WHERE process_instance_id = #{processInstanceId}
            """)
    int updateBusinessStatusByInstance(@Param("processInstanceId") String processInstanceId,
                                       @Param("status") String status,
                                       @Param("username") String username);

    @Update("""
            UPDATE wf_business
            SET business_status = #{status},
                update_by = #{username}
            WHERE id = #{id}
            """)
    int updateBusinessStatusById(@Param("id") Long id, @Param("status") String status, @Param("username") String username);

    @Select("""
            SELECT r.role_code
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE u.username = #{username}
              AND u.deleted = 0
              AND r.deleted = 0
              AND r.status = 1
            ORDER BY r.sort, r.id
            """)
    List<String> selectRoleCodesByUsername(@Param("username") String username);

    @Insert("""
            INSERT INTO wf_approval_record (business_id, process_instance_id, task_id, task_name,
                action, comment, assignee, result_status, create_by, update_by)
            VALUES (#{row.businessId}, #{row.processInstanceId}, #{row.taskId}, #{row.taskName},
                #{row.action}, #{row.comment}, #{row.assignee}, #{row.resultStatus}, #{username}, #{username})
            """)
    int insertApprovalRecord(@Param("row") Map<String, Object> row, @Param("username") String username);

    @Select("""
            SELECT COUNT(*)
            FROM wf_business
            WHERE business_status IN ('TODO', 'PENDING', 'APPROVING')
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR business_title LIKE CONCAT('%', #{keyword}, '%')
                   OR business_key LIKE CONCAT('%', #{keyword}, '%')
                   OR starter LIKE CONCAT('%', #{keyword}, '%'))
            """)
    long countLegacyTodoTasks(@Param("keyword") String keyword);

    @Select("""
            SELECT b.id,
                   b.business_key AS businessKey,
                   b.process_instance_id AS processInstanceId,
                   CASE b.business_type
                       WHEN 'purchase' THEN '采购审批'
                       WHEN 'expense' THEN '费用报销'
                       WHEN 'leave' THEN '请假审批'
                       WHEN 'contract' THEN '合同审批'
                       ELSE '流程审批'
                   END AS taskName,
                   COALESCE(m.model_name, b.business_type) AS procName,
                   b.business_title AS bizTitle,
                   b.business_type AS bizType,
                   b.starter,
                   DATE_FORMAT(b.create_time, '%Y-%m-%d %H:%i') AS createTime,
                   DATE_FORMAT(DATE_ADD(b.create_time, INTERVAL 2 DAY), '%Y-%m-%d %H:%i') AS due,
                   'high' AS priority,
                   '部门负责人审批' AS node,
                   3 AS total,
                   m.bpmn_xml AS bpmnXml
            FROM wf_business b
            LEFT JOIN wf_model_meta m
              ON b.process_definition_id = m.process_definition_id
                 OR b.process_definition_id LIKE CONCAT(m.model_key, ':%')
            WHERE b.business_status IN ('TODO', 'PENDING', 'APPROVING')
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR b.business_title LIKE CONCAT('%', #{keyword}, '%')
                   OR b.business_key LIKE CONCAT('%', #{keyword}, '%')
                   OR b.starter LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY b.create_time DESC, b.id DESC
            LIMIT #{offset}, #{pageSize}
            """)
    List<Map<String, Object>> selectLegacyTodoTasks(@Param("keyword") String keyword,
                                                    @Param("offset") int offset,
                                                    @Param("pageSize") int pageSize);

    @Select("""
            SELECT COUNT(*)
            FROM wf_approval_record r
            JOIN wf_business b ON b.id = r.business_id
            WHERE r.action != 'start'
              AND r.assignee = #{username}
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR b.business_title LIKE CONCAT('%', #{keyword}, '%')
                   OR b.business_key LIKE CONCAT('%', #{keyword}, '%')
                   OR b.starter LIKE CONCAT('%', #{keyword}, '%')
                   OR r.task_name LIKE CONCAT('%', #{keyword}, '%'))
            """)
    long countDoneTasks(@Param("keyword") String keyword, @Param("username") String username);

    @Select("""
            SELECT r.id,
                   r.task_id AS taskId,
                   r.process_instance_id AS processInstanceId,
                   r.task_name AS taskName,
                   COALESCE(m.model_name, b.business_type) AS procName,
                   b.business_title AS bizTitle,
                   b.starter,
                   CASE r.action
                       WHEN 'reject' THEN '驳回'
                       WHEN 'return' THEN '退回'
                       WHEN 'revoke' THEN '撤回'
                       ELSE '通过'
                   END AS result,
                   CASE r.action
                       WHEN 'reject' THEN 'danger'
                       WHEN 'return' THEN 'warn'
                       WHEN 'revoke' THEN 'neutral'
                       ELSE 'ok'
                   END AS tone,
                   r.comment,
                   DATE_FORMAT(r.handle_time, '%Y-%m-%d %H:%i') AS approveTime
            FROM wf_approval_record r
            JOIN wf_business b ON b.id = r.business_id
            LEFT JOIN wf_model_meta m
              ON b.process_definition_id = m.process_definition_id
                 OR b.process_definition_id LIKE CONCAT(m.model_key, ':%')
            WHERE r.action != 'start'
              AND r.assignee = #{username}
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR b.business_title LIKE CONCAT('%', #{keyword}, '%')
                   OR b.business_key LIKE CONCAT('%', #{keyword}, '%')
                   OR b.starter LIKE CONCAT('%', #{keyword}, '%')
                   OR r.task_name LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY r.handle_time DESC, r.id DESC
            LIMIT #{offset}, #{pageSize}
            """)
    List<Map<String, Object>> selectDoneTasks(@Param("keyword") String keyword,
                                              @Param("username") String username,
                                              @Param("offset") int offset,
                                              @Param("pageSize") int pageSize);

    @Select("""
            SELECT r.id,
                   r.task_id AS taskId,
                   r.task_name AS taskName,
                   r.action,
                   r.comment,
                   r.assignee,
                   r.result_status AS resultStatus,
                   DATE_FORMAT(r.handle_time, '%Y-%m-%d %H:%i') AS handleTime
            FROM wf_approval_record r
            WHERE r.process_instance_id = #{processInstanceId}
            ORDER BY r.handle_time, r.id
            """)
    List<Map<String, Object>> selectTraceRecords(@Param("processInstanceId") String processInstanceId);
}
