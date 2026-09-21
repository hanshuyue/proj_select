package com.scaffold.system.service;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.mapper.AuthMapper;
import com.scaffold.system.mapper.ProjectAccessMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ProjectAccessService {
    private final AuthMapper auth;
    private final ProjectAccessMapper projects;
    public ProjectAccessService(AuthMapper auth, ProjectAccessMapper projects) {
        this.auth = auth; this.projects = projects;
    }
    /** Null means unrestricted for active administrators; employees are scoped to their username. */
    public String ownerFilter() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) throw new BusinessException(401, "请先登录");
        var user = auth.selectUserByUsername(String.valueOf(authentication.getPrincipal()));
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) throw new BusinessException(401, "登录已失效");
        var roles = auth.selectRoleCodes(user.getId());
        return roles.contains("super_admin") || roles.contains("biz_admin") || roles.contains("sys_admin") ? null : user.getUsername();
    }
    public void selection(Long id) { check(projects.selectionOwner(id)); }
    public void initiation(Long id) { check(projects.initiationOwner(id)); }
    public void attachment(Long id) {
        Long projectId = projects.attachmentProject(id);
        if (projectId == null) throw new BusinessException(404, "附件不存在");
        initiation(projectId);
    }
    private void check(String owner) {
        String viewer = ownerFilter();
        if (owner == null) throw new BusinessException(404, "项目不存在");
        if (viewer != null && !viewer.equals(owner)) throw new BusinessException(403, "无权访问他人的项目");
    }
}
