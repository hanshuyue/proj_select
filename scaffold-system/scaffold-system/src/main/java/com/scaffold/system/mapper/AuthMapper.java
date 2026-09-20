package com.scaffold.system.mapper;

import com.scaffold.system.domain.AuthUserRow;
import com.scaffold.system.domain.MenuRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AuthMapper {

    @Select("""
            SELECT u.id, u.username, u.password, u.nickname, u.status, u.dept_id, d.dept_name,
                   u.must_change_password
            FROM sys_user u
            LEFT JOIN sys_dept d ON d.id = u.dept_id
            WHERE (u.phone = #{username} OR u.username = #{username}) AND u.deleted = 0
            LIMIT 1
            """)
    AuthUserRow selectUserByUsername(@Param("username") String username);

    @Select("""
            SELECT r.role_code
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0
            ORDER BY r.sort, r.id
            """)
    List<String> selectRoleCodes(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT m.permission
            FROM sys_user_role ur
            JOIN sys_role_menu rm ON rm.role_id = ur.role_id
            JOIN sys_menu m ON m.id = rm.menu_id
            WHERE ur.user_id = #{userId}
              AND m.status = 1
              AND m.permission IS NOT NULL
              AND m.permission <> ''
            ORDER BY m.permission
            """)
    List<String> selectPermissions(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT m.id, m.parent_id, m.menu_name, m.menu_type, m.icon, m.path,
                   m.component, m.permission, m.sort
            FROM sys_user_role ur
            JOIN sys_role_menu rm ON rm.role_id = ur.role_id
            JOIN sys_menu m ON m.id = rm.menu_id
            WHERE ur.user_id = #{userId}
              AND m.status = 1
              AND m.visible = 1
              AND m.menu_type IN ('dir', 'menu')
            ORDER BY m.parent_id, m.sort, m.id
            """)
    List<MenuRow> selectRouteMenus(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO sys_login_log (username, status, msg, login_time)
            VALUES (#{username}, #{status}, #{msg}, NOW())
            """)
    void insertLoginLog(@Param("username") String username, @Param("status") int status, @Param("msg") String msg);

    @Update("""
            UPDATE sys_user
            SET password = #{password}, must_change_password = 0, update_by = #{username}, update_time = NOW()
            WHERE id = #{userId} AND deleted = 0
            """)
    int updatePassword(@Param("userId") Long userId, @Param("username") String username, @Param("password") String password);
}
