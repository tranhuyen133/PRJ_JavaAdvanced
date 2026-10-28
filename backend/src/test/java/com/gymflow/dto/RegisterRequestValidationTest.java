package com.gymflow.dto;

import com.gymflow.dto.Dtos.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Kiểm tra ràng buộc @Valid của form đăng ký ở phía server (không cần Spring context). */
class RegisterRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private static Set<String> invalidFields(RegisterRequest r) {
        return validator.validate(r).stream().map(ConstraintViolation::getPropertyPath).map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void validRequestPasses() {
        assertThat(invalidFields(new RegisterRequest("Lê Thu Hà", "ha@example.com", "0912345678", "Pass1234", "Pass1234", true))).isEmpty();
        assertThat(invalidFields(new RegisterRequest("Lê Thu Hà", "ha@example.com", "", "Pass1234", "Pass1234", true))).isEmpty();
    }

    @Test
    void emptyFormReportsEveryRequiredField() {
        assertThat(invalidFields(new RegisterRequest("", "", null, "", "", false)))
                .contains("fullName", "email", "password", "confirmPassword", "acceptTerms");
    }

    @Test
    void rejectsBadEmailPhoneAndWeakPassword() {
        assertThat(invalidFields(new RegisterRequest("Lê Thu Hà", "khong-phai-email", "12345", "abcdefgh", "abcdefgh", true)))
                .containsExactlyInAnyOrder("email", "phone", "password");
        assertThat(invalidFields(new RegisterRequest("Lê Thu Hà", "ha@example.com", null, "Ab1", "Ab1", true)))
                .containsExactly("password");
    }
}
