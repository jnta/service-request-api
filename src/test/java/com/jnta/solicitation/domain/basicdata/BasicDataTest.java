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
        var data = new BasicData(ServiceType.from("installation"), " Equipment ", " Install equipment at the office ");

        assertThat(data.serviceType()).isEqualTo(ServiceType.INSTALLATION);
        assertThat(data.title()).isEqualTo("Equipment");
        assertThat(data.description()).isEqualTo("Install equipment at the office");
    }

    @Test
    @DisplayName("given a title shorter than 3 characters, when basic data is created, then it is rejected")
    void shouldRejectShortTitle() {
        assertThatThrownBy(() -> new BasicData(ServiceType.INSTALLATION, "AB",
                "Install equipment at the main office."))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    @Test
    @DisplayName("given a description shorter than 20 characters, when basic data is created, then it is rejected")
    void shouldRejectShortDescription() {
        assertThatThrownBy(() -> new BasicData(ServiceType.INSTALLATION, "Equipment", "Too short"))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    @Test
    @DisplayName("given an unsupported service type, when parsed, then it is rejected")
    void shouldRejectUnsupportedServiceType() {
        assertThatThrownBy(() -> ServiceType.from("CUSTOM"))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    @Test
    @DisplayName("given a lowercase service type, when parsed, then it is normalized to the enum value")
    void shouldParseCaseInsensitively() {
        assertThat(ServiceType.from("installation")).isEqualTo(ServiceType.INSTALLATION);
    }

    @Test
    @DisplayName("given title and description at their normalized maximum lengths, when created, then they are accepted")
    void shouldValidateMaximumLengthsAfterTrimming() {
        String title = "T".repeat(80);
        String description = "D".repeat(1000);

        var data = new BasicData(ServiceType.INSTALLATION, " " + title + " ", " " + description + " ");

        assertThat(data.title()).hasSize(80);
        assertThat(data.description()).hasSize(1000);
    }
}
