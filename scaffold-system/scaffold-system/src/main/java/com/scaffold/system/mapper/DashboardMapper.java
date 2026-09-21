package com.scaffold.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0")
    long countUsers();

    @Select("""
            SELECT COUNT(*)
            FROM sys_login_log
            WHERE status = 1 AND login_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
            """)
    long countOnlineUsers();

    @Select("""
            SELECT COUNT(*)
            FROM wf_business
            WHERE business_status IN ('TODO', 'PENDING', 'APPROVING')
            """)
    long countTodoTasks();

    @Select("""
            SELECT COUNT(*)
            FROM sys_oper_log
            WHERE oper_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
            """)
    long countApiCalls();

    @Select("""
            SELECT HOUR(login_time) AS hourValue, COUNT(*) AS total
            FROM sys_login_log
            WHERE login_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
            GROUP BY HOUR(login_time)
            """)
    List<Map<String, Object>> selectLoginTrend();

    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND create_time >= DATE_SUB(NOW(), INTERVAL 1 DAY)")
    long countUsersCreatedSince1d();

    @Select("SELECT COUNT(*) FROM sys_login_log WHERE status = 1 AND login_time >= DATE_SUB(NOW(), INTERVAL 2 HOUR)")
    long countOnlineUsers2h();

    @Select("""
            SELECT COUNT(*)
            FROM wf_business
            WHERE business_status IN ('TODO', 'PENDING', 'APPROVING')
              AND create_time >= DATE_SUB(NOW(), INTERVAL 1 DAY)
            """)
    long countTodoTasksSince1d();

    @Select("SELECT COUNT(*) FROM sys_oper_log WHERE oper_time >= DATE_SUB(NOW(), INTERVAL 48 HOUR) AND oper_time < DATE_SUB(NOW(), INTERVAL 24 HOUR)")
    long countApiCallsPrev24h();

    @Select("""
            SELECT r.data_scope AS label, COUNT(ur.user_id) AS value
            FROM sys_role r
            LEFT JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE r.deleted = 0
            GROUP BY r.data_scope
            ORDER BY value DESC
            """)
    List<Map<String, Object>> selectRoleDistribution();

    @Select("SELECT COUNT(*) FROM sys_oper_log WHERE oper_time >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR)")
    long countApiCallsInHours(int hours);

    @Select("SELECT COUNT(*) FROM sys_oper_log WHERE oper_time >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND result = 1")
    long countApiCallsSuccessInHours(int hours);

    @Select("""
            SELECT DATEDIFF(DATE(login_time), CURDATE() - INTERVAL #{days} DAY) AS dayIndex, COUNT(*) AS total
            FROM sys_login_log
            WHERE login_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            GROUP BY dayIndex
            ORDER BY dayIndex
            """)
    List<Map<String, Object>> selectLoginTrendByDay(int days);

    @Select("""
            SELECT b.id,
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
                   3 AS total
            FROM wf_business b
            LEFT JOIN wf_model_meta m
              ON b.process_definition_id LIKE CONCAT(m.model_key, ':%')
            WHERE b.business_status IN ('TODO', 'PENDING', 'APPROVING')
              AND (#{username} IS NULL OR b.starter = #{username})
            ORDER BY b.create_time DESC, b.id DESC
            LIMIT 5
            """)
    List<Map<String, Object>> selectTodos(@Param("username") String username);

    @Select("""
            SELECT username AS who,
                   CASE status WHEN 1 THEN '登录系统' ELSE '登录失败' END AS action,
                   COALESCE(msg, '登录') AS target,
                   DATE_FORMAT(login_time, '%Y-%m-%d %H:%i') AS time,
                   CASE status WHEN 1 THEN 'ok' ELSE 'danger' END AS tint
            FROM sys_login_log
            WHERE (#{username} IS NULL OR username = #{username})
            ORDER BY login_time DESC, id DESC
            LIMIT 8
            """)
    List<Map<String, Object>> selectActivities(@Param("username") String username);
}
