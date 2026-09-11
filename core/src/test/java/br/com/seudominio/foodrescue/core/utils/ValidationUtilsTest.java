package br.com.seudominio.foodrescue.core.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValidationUtilsTest {

    @Test
    void acceptsValidEmail() {
        assertThat(ValidationUtils.isValidEmail("maria@example.com")).isTrue();
    }

    @Test
    void rejectsMissingAtSign() {
        assertThat(ValidationUtils.isValidEmail("maria.example.com")).isFalse();
    }

    @Test
    void rejectsNull() {
        assertThat(ValidationUtils.isValidEmail(null)).isFalse();
    }
}
