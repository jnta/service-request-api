package com.jnta.solicitation.domain.address;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AddressTest {

    @Test
    @DisplayName("given a formatted CEP, when created, then it is normalized to digits")
    void shouldNormalizeCep() {
        assertThat(new Cep("70040-902").value()).isEqualTo("70040902");
    }

    @Test
    @DisplayName("given an invalid CEP, when created, then it is rejected")
    void shouldRejectInvalidCep() {
        assertThatThrownBy(() -> new Cep("70040-9021"))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    @Test
    @DisplayName("given valid address fields, when created, then required fields are normalized")
    void shouldNormalizeAddressFields() {
        var address = new Address(new Cep("70040902"), " Main Street ", " Central ", " Brasilia ",
                State.DF, " 123 ", "   ");

        assertThat(address.street()).isEqualTo("Main Street");
        assertThat(address.number()).isEqualTo("123");
        assertThat(address.complement()).isNull();
    }

    @Test
    @DisplayName("given an address number longer than 20 characters, when created, then it is rejected")
    void shouldRejectAddressNumberLongerThanMaximum() {
        assertThatThrownBy(() -> new Address(new Cep("70040902"), "Main Street", "Central", "Brasilia",
                State.DF, "123456789012345678901", null))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    @Test
    @DisplayName("given a missing locality field, when the address is created, then it is rejected")
    void shouldRejectAddressWithMissingCity() {
        assertThatThrownBy(() -> new Address(new Cep("70040902"), "Main Street", "Central", " ",
                State.DF, "123", null))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }
}
