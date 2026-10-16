package com.jnta.solicitation.domain.basicdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BasicDataTest {

    @Test
    @DisplayName("given valid basic data, when created, then text fields are normalized")
    void shouldNormalizeTextFields() {
        var data = new BasicData(new ServiceType("installation"), " Equipment ", " Install equipment at the office ");

        assertThat(data.serviceType().value()).isEqualTo("INSTALLATION");
        assertThat(data.title()).isEqualTo("Equipment");
        assertThat(data.description()).isEqualTo("Install equipment at the office");
    }

    @Test
    @DisplayName("given a title shorter than 3 characters, when basic data is created, then it is rejected")
    void shouldRejectShortTitle() {
        assertThatThrownBy(() -> new BasicData(new ServiceType("INSTALLATION"), "AB",
                "Install equipment at the main office."))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    @Test
    @DisplayName("given a description shorter than 20 characters, when basic data is created, then it is rejected")
    void shouldRejectShortDescription() {
        assertThatThrownBy(() -> new BasicData(new ServiceType("INSTALLATION"), "Equipment", "Too short"))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    @Test
    @DisplayName("given an invalid service type code, when created, then it is rejected")
    void shouldRejectInvalidServiceType() {
        assertThatThrownBy(() -> new ServiceType("not a code"))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }
}
