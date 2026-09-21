package com.scaffold.admin;

import com.scaffold.system.domain.dto.RegisterRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {
    @Test void registrationAcceptsSixDigitsAndReportsInvalidFields() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new RegisterRequest();
            request.setRealName("张三丰"); request.setPhone("13800000000");
            request.setDeptId(1L); request.setCaptcha("eyvx"); request.setCaptchaUuid("test");
            for (int length : new int[]{6, 7, 64}) {
                request.setPassword("1".repeat(length));
                assertThat(validator.validate(request)).isEmpty();
            }
            for (int length : new int[]{5, 65}) {
                request.setPassword("1".repeat(length));
                assertThat(validator.validate(request)).anyMatch(v -> v.getMessage().contains("6-64"));
            }
            request.setPassword("123456"); request.setPhone("11111111111");
            assertThat(validator.validate(request)).anyMatch(v -> v.getMessage().equals("手机号格式不正确"));
        }
    }
}
