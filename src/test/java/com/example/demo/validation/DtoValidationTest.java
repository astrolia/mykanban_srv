package com.example.demo.validation;

import com.example.demo.dto.CardCreateWrapperDTO;
import com.example.demo.dto.CardRequestDTO;
import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.ColumnCreateWrapperDTO;
import com.example.demo.dto.EnvironmentArchiveRequestDTO;
import com.example.demo.dto.EnvironmentRequestDTO;
import com.example.demo.dto.EnvironmentUserRequestDTO;
import com.example.demo.dto.PriorityCreateWrapperDTO;
import com.example.demo.dto.UserDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    @DisplayName("UserDTO - análise de valor limite para senha (5 vs 6 caracteres) e nome (100 vs 101)")
    void shouldValidateUserDtoBoundaryValues() {
        UserDTO shortPassword = new UserDTO("Aline", "aline@example.com", "12345");
        assertThat(validator.validate(shortPassword)).isNotEmpty();

        UserDTO minValidPassword = new UserDTO("Aline", "aline@example.com", "123456");
        assertThat(validator.validate(minValidPassword)).isEmpty();

        UserDTO maxValidName = new UserDTO("A".repeat(100), "aline@example.com", "123456");
        assertThat(validator.validate(maxValidName)).isEmpty();

        UserDTO overMaxName = new UserDTO("A".repeat(101), "aline@example.com", "123456");
        assertThat(validator.validate(overMaxName)).isNotEmpty();

        UserDTO invalidEmail = new UserDTO("Aline", "email-sem-arroba", "123456");
        assertThat(validator.validate(invalidEmail)).isNotEmpty();
    }

    @Test
    @DisplayName("ChangePasswordRequest - particionamento de equivalência e limite de 6 caracteres")
    void shouldValidateChangePasswordBoundaries() {
        assertThat(validator.validate(new ChangePasswordRequest("atual123", "12345"))).isNotEmpty();
        assertThat(validator.validate(new ChangePasswordRequest("atual123", "123456"))).isEmpty();
        assertThat(validator.validate(new ChangePasswordRequest(" ", "123456"))).isNotEmpty();
    }

    @Test
    @DisplayName("CardRequestDTO - análise de valor limite para progresso (0..100) e posição (>= 0)")
    void shouldValidateCardProgressAndPositionBoundaries() {
        CardRequestDTO belowMin = new CardRequestDTO("Card", null, 1, null, null, -0.1f, null, null, null, null, null, 0);
        CardRequestDTO atMin = new CardRequestDTO("Card", null, 1, null, null, 0.0f, null, null, null, null, null, 0);
        CardRequestDTO atMax = new CardRequestDTO("Card", null, 1, null, null, 100.0f, null, null, null, null, null, 0);
        CardRequestDTO aboveMax = new CardRequestDTO("Card", null, 1, null, null, 100.1f, null, null, null, null, null, 0);
        CardRequestDTO negativePos = new CardRequestDTO("Card", null, 1, null, null, 50.0f, null, null, null, null, null, -1);

        assertThat(validator.validate(belowMin)).isNotEmpty();
        assertThat(validator.validate(atMin)).isEmpty();
        assertThat(validator.validate(atMax)).isEmpty();
        assertThat(validator.validate(aboveMax)).isNotEmpty();
        assertThat(validator.validate(negativePos)).isNotEmpty();
    }

    @Test
    @DisplayName("CardRequestDTO - conversão de deadline nos formatos yyyy-MM-dd e ISO-8601")
    void shouldParseSupportedDeadlineFormats() {
        CardRequestDTO dateOnly = new CardRequestDTO("Card", null, null, null, null, null, "2026-11-30", null, null, null, null, null);
        CardRequestDTO isoUtc = new CardRequestDTO("Card", null, null, null, null, null, "2026-12-15T12:00:00.000Z", null, null, null, null, null);
        CardRequestDTO blank = new CardRequestDTO("Card", null, null, null, null, null, "   ", null, null, null, null, null);

        assertThat(dateOnly.resolvedDeadline()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(isoUtc.resolvedDeadline()).isEqualTo(LocalDate.of(2026, 12, 15));
        assertThat(blank.resolvedDeadline()).isNull();
    }

    @Test
    @DisplayName("DTOs de Ambiente, Coluna, Card e Prioridade - campos obrigatórios e limites")
    void shouldValidateEnvironmentColumnCardAndPriorityWrappers() {
        assertThat(validator.validate(new EnvironmentRequestDTO("   ", "", 0, false, null))).isNotEmpty();
        assertThat(validator.validate(new EnvironmentRequestDTO("Ambiente", "", -1, false, null))).isNotEmpty();
        assertThat(validator.validate(new EnvironmentRequestDTO("Ambiente", "", 0, false, null))).isEmpty();

        assertThat(validator.validate(new EnvironmentArchiveRequestDTO(null))).isNotEmpty();
        assertThat(validator.validate(new EnvironmentArchiveRequestDTO(true))).isEmpty();

        assertThat(validator.validate(new EnvironmentUserRequestDTO(null))).isNotEmpty();
        assertThat(validator.validate(new EnvironmentUserRequestDTO(0))).isNotEmpty();
        assertThat(validator.validate(new EnvironmentUserRequestDTO(1))).isEmpty();

        assertThat(validator.validate(new ColumnCreateWrapperDTO(null, "   ", null, 0))).isNotEmpty();
        assertThat(validator.validate(new ColumnCreateWrapperDTO(null, "Backlog", null, 0))).isEmpty();

        assertThat(validator.validate(new PriorityCreateWrapperDTO(null, "", null, "priority-red", null))).isNotEmpty();
        assertThat(validator.validate(new PriorityCreateWrapperDTO(null, "Alta", null, "priority-red", null))).isEmpty();

        assertThat(validator.validate(new CardCreateWrapperDTO(
                null, "  ", null, 1, null, null, 0f, null, null, null, null, null, 0
        ))).isNotEmpty();
    }
}
