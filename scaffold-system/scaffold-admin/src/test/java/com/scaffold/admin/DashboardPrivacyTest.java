package com.scaffold.admin;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.framework.security.PermissionResolver;
import com.scaffold.system.mapper.DashboardMapper;
import com.scaffold.system.service.DashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DashboardPrivacyTest {
    private final DashboardMapper mapper = mock(DashboardMapper.class);
    private final PermissionResolver permissions = mock(PermissionResolver.class);
    private final DashboardService service = new DashboardService(mapper, permissions);

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void employeesOnlyQueryTheirOwnDetails() {
        login("employee");
        service.summary("24h");
        verify(mapper).selectTodos("employee");
        verify(mapper).selectActivities("employee");
        verify(mapper, never()).selectTodos(null);
        verify(mapper, never()).selectActivities(null);
    }

    @Test void approvalPermissionDoesNotExposeLoginAudit() {
        login("buyer");
        when(permissions.hasPermission("buyer", "workflow:todo:list")).thenReturn(true);
        service.summary("7d");
        verify(mapper).selectTodos(null);
        verify(mapper).selectActivities("buyer");
    }

    @Test void auditPermissionDoesNotExposeOtherPeoplesApprovals() {
        login("auditor");
        when(permissions.hasPermission("auditor", "monitor:loginlog:list")).thenReturn(true);
        service.summary("30d");
        verify(mapper).selectTodos("auditor");
        verify(mapper).selectActivities(null);
    }

    @Test void unauthenticatedRequestsNeverQueryDashboardData() {
        assertThatThrownBy(() -> service.summary("24h")).isInstanceOf(BusinessException.class);
        verifyNoInteractions(mapper, permissions);
    }

    private void login(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }
}
