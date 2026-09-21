package com.scaffold.admin;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.domain.AuthUserRow;
import com.scaffold.system.mapper.AuthMapper;
import com.scaffold.system.mapper.ProjectAccessMapper;
import com.scaffold.system.service.ProjectAccessService;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectAccessSecurityTest {
    private final AuthMapper auth = mock(AuthMapper.class);
    private final ProjectAccessMapper mapper = mock(ProjectAccessMapper.class);
    private final ProjectAccessService access = new ProjectAccessService(auth, mapper);
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    private void login(String role) {
        AuthUserRow user = new AuthUserRow(); user.setId(1L); user.setUsername("alice"); user.setStatus(1);
        when(auth.selectUserByUsername("alice")).thenReturn(user);
        when(auth.selectRoleCodes(1L)).thenReturn(List.of(role));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alice", "token", List.of()));
    }
    @Test void anonymousCannotAccessProjects() {
        assertThatThrownBy(access::ownerFilter).isInstanceOf(BusinessException.class);
    }
    @Test void employeeListIsScopedToOwner() {
        login("external_user"); assertThat(access.ownerFilter()).isEqualTo("alice");
    }
    @Test void employeeCanAccessOwnProject() {
        login("external_user"); when(mapper.selectionOwner(7L)).thenReturn("alice");
        assertThatCode(() -> access.selection(7L)).doesNotThrowAnyException();
    }
    @Test void employeeCannotAccessAnotherUsersProjectInEitherModule() {
        login("external_user"); when(mapper.selectionOwner(7L)).thenReturn("bob"); when(mapper.initiationOwner(8L)).thenReturn("bob");
        assertThatThrownBy(() -> access.selection(7L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> access.initiation(8L)).isInstanceOf(BusinessException.class);
    }
    @Test void attachmentChecksParentProjectOwnership() {
        login("external_user"); when(mapper.attachmentProject(9L)).thenReturn(8L); when(mapper.initiationOwner(8L)).thenReturn("bob");
        assertThatThrownBy(() -> access.attachment(9L)).isInstanceOf(BusinessException.class);
    }
    @Test void purchaserCanAccessEmployeeProjects() {
        login("biz_admin"); when(mapper.selectionOwner(7L)).thenReturn("bob");
        assertThat(access.ownerFilter()).isNull();
        assertThatCode(() -> access.selection(7L)).doesNotThrowAnyException();
    }
    @Test void legacyActiveSystemAdministratorRetainsProjectAccess() {
        login("sys_admin"); when(mapper.initiationOwner(8L)).thenReturn("bob");
        assertThat(access.ownerFilter()).isNull();
        assertThatCode(() -> access.initiation(8L)).doesNotThrowAnyException();
    }
    @Test void disabledPurchaserCannotAccessProjects() {
        login("biz_admin"); auth.selectUserByUsername("alice").setStatus(0);
        assertThatThrownBy(access::ownerFilter).isInstanceOf(BusinessException.class);
    }
}
