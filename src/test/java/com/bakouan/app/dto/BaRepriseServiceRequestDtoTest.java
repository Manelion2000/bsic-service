package com.bakouan.app.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BaRepriseServiceRequestDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsIncoherentLeaveDates() {
        BaRepriseServiceRequestDto request = request(LocalDate.of(2026, 8, 19));

        assertThat(validator.validate(request))
                .anyMatch(violation -> "chronologieValide".equals(violation.getPropertyPath().toString()));
    }

    @Test
    void acceptsChronologicalLeaveDates() {
        assertThat(validator.validate(request(LocalDate.of(2026, 8, 21)))).isEmpty();
    }

    private BaRepriseServiceRequestDto request(final LocalDate reprise) {
        BaRepriseServiceRequestDto request = new BaRepriseServiceRequestDto();
        request.setMotifAbsence("Conge annuel");
        request.setDateDebutConge(LocalDate.of(2026, 8, 10));
        request.setDateFinConge(LocalDate.of(2026, 8, 20));
        request.setDateReprise(reprise);
        return request;
    }
}
