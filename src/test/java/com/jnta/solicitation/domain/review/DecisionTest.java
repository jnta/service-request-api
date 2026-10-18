package com.jnta.solicitation.domain.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DecisionTest {

    @Test
    @DisplayName("given a comment shorter than 10 characters after trimming, when a decision is created, then it is rejected")
    void shouldRejectShortCommentAfterTrimming() {
        assertThatThrownBy(() -> new Decision(Decision.Outcome.APPROVE, " 123456789 "))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    @Test
    @DisplayName("given a 10-character comment after trimming, when a decision is created, then it is accepted")
    void shouldAcceptMinimumLengthCommentAfterTrimming() {
        var decision = new Decision(Decision.Outcome.APPROVE, " 1234567890 ");

        assertThat(decision.comment()).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("given a comment longer than 1000 characters after trimming, when a decision is created, then it is rejected")
    void shouldRejectCommentAboveMaximumLength() {
        assertThatThrownBy(() -> new Decision(Decision.Outcome.REJECT, "C".repeat(1001)))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }
}
