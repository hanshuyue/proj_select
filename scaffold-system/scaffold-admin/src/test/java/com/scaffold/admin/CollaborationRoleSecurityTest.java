package com.scaffold.admin;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.domain.AuthUserRow;
import com.scaffold.system.domain.dto.RegisterRequest;
import com.scaffold.system.domain.dto.DocumentFeedbackRequest;
import com.scaffold.system.mapper.AuthMapper;
import com.scaffold.system.mapper.CollaborationMapper;
import com.scaffold.system.service.CollaborationService;
import com.scaffold.system.service.SystemPermissionResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollaborationRoleSecurityTest {
    private final CollaborationMapper mapper = mock(CollaborationMapper.class);
    private final AuthMapper auth = mock(AuthMapper.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final CollaborationService service = new CollaborationService(mapper, auth, encoder, "./target/test-documents");

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    private void authenticate(String role) {
        AuthUserRow user = new AuthUserRow();
        user.setId(1L); user.setUsername("actor"); user.setStatus(1);
        when(auth.selectUserByUsername("actor")).thenReturn(user);
        when(auth.selectRoleCodes(1L)).thenReturn(List.of(role));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("actor", "token"));
    }

    @Test void registrationBindsOnlyEmployeeRoleAndEncodesPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800000000"); request.setRealName(" Employee ");
        request.setPassword("password123"); request.setDeptId(2L);
        when(mapper.validRegistrationDept(2L)).thenReturn(1);
        when(encoder.encode("password123")).thenReturn("encoded");
        doAnswer(invocation -> { Map<String,Object> row = invocation.getArgument(0); row.put("id", 8L); return null; }).when(mapper).register(anyMap());
        when(mapper.bindExternalRole(8L)).thenReturn(1);
        service.register(request);
        verify(mapper).register(argThat(row -> "encoded".equals(row.get("password")) && "Employee".equals(row.get("realName"))));
        verify(mapper).bindExternalRole(8L);
        verify(mapper, never()).addBusinessAdmin(anyLong());
    }

    @Test void promotionIncludesEmployeeCapabilities() {
        authenticate("super_admin");
        when(mapper.registrationExists(8L)).thenReturn(1);
        when(mapper.bindExternalRole(8L)).thenReturn(1);
        when(mapper.addBusinessAdmin(8L)).thenReturn(1);
        service.setAdministrator(8L, true);
        verify(mapper).bindExternalRole(8L);
        verify(mapper).addBusinessAdmin(8L);
    }

    @Test void promotionFailsWhenPurchaserRoleIsMissing() {
        authenticate("super_admin");
        when(mapper.registrationExists(8L)).thenReturn(1);
        when(mapper.bindExternalRole(8L)).thenReturn(1);
        assertThatThrownBy(() -> service.setAdministrator(8L, true)).isInstanceOf(BusinessException.class);
    }

    @Test void demotionRetainsEmployeeCapabilities() {
        authenticate("super_admin");
        when(mapper.registrationExists(8L)).thenReturn(1);
        when(mapper.bindExternalRole(8L)).thenReturn(1);
        service.setAdministrator(8L, false);
        verify(mapper).removeBusinessAdmin(8L);
        verify(mapper).bindExternalRole(8L);
        verify(mapper, never()).addBusinessAdmin(anyLong());
    }

    @Test void purchaserCannotPromoteUsers() {
        authenticate("biz_admin");
        assertThatThrownBy(() -> service.setAdministrator(8L, true)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(mapper);
    }

    @Test void employeesCannotReviewDocuments() {
        authenticate("external_user");
        DocumentFeedbackRequest request = new DocumentFeedbackRequest(); request.setStatus("APPROVED");
        assertThatThrownBy(() -> service.feedback(8L, request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(mapper);
    }

    @Test void purchaserCanReviewDocuments() {
        authenticate("biz_admin");
        DocumentFeedbackRequest request = new DocumentFeedbackRequest(); request.setStatus("APPROVED");
        when(mapper.reviewDocument(eq(8L), eq("APPROVED"), eq(""), eq(1L), isNull())).thenReturn(1);
        service.feedback(8L, request);
        verify(mapper).reviewDocument(8L, "APPROVED", "", 1L, null);
    }

    @Test void disabledAccountHasNoPermissionsEvenWithSuperAdminRole() {
        authenticate("super_admin");
        auth.selectUserByUsername("actor").setStatus(0);
        assertThat(new SystemPermissionResolver(auth).hasPermission("actor", "selection:project:list")).isFalse();
    }
}
