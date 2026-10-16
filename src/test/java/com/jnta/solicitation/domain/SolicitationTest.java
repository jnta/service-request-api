package com.jnta.solicitation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnta.solicitation.domain.exception.IncompleteSolicitationException;
import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import com.jnta.solicitation.domain.exception.InvalidSolicitationStateException;
import com.jnta.user.domain.UserId;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SolicitationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 16);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("given a valid client, when a solicitation is created, then it starts as DRAFT at BASIC_DATA")
    void shouldCreateSolicitationAsDraft() {
        var solicitation = newSolicitation();

        assertThat(solicitation.status()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(solicitation.currentStep()).isEqualTo(SolicitationStep.BASIC_DATA);
    }

    @Nested
    @DisplayName("Completing basic data")
    class CompletingBasicData {

        @Test
        @DisplayName("given a solicitation in DRAFT, when valid basic data is provided, then the current step becomes ADDRESS")
        void shouldAdvanceToAddressWhenBasicDataIsValid() {
            var solicitation = newSolicitation();

            solicitation.updateBasicData(serviceType(), "Equipment installation", "Install equipment at the main office.");

            assertThat(solicitation.currentStep()).isEqualTo(SolicitationStep.ADDRESS);
            assertThat(solicitation.title()).isEqualTo("Equipment installation");
        }

        @Test
        @DisplayName("given a solicitation in DRAFT, when the title is shorter than 3 characters, then basic data is rejected and the step remains BASIC_DATA")
        void shouldRejectTitleShorterThanMinimumWithoutChangingStep() {
            var solicitation = newSolicitation();

            assertThatThrownBy(() -> solicitation.updateBasicData(serviceType(), "AB", "Install equipment at the main office."))
                    .isInstanceOf(InvalidSolicitationDataException.class);

            assertThat(solicitation.currentStep()).isEqualTo(SolicitationStep.BASIC_DATA);
            assertThat(solicitation.title()).isNull();
        }

        @Test
        @DisplayName("given a solicitation in DRAFT, when the description is shorter than 20 characters, then basic data is rejected")
        void shouldRejectDescriptionShorterThanMinimum() {
            var solicitation = newSolicitation();

            assertThatThrownBy(() -> solicitation.updateBasicData(serviceType(), "Valid title", "Too short"))
                    .isInstanceOf(InvalidSolicitationDataException.class);

            assertThat(solicitation.currentStep()).isEqualTo(SolicitationStep.BASIC_DATA);
        }
    }

    @Nested
    @DisplayName("Completing address")
    class CompletingAddress {

        @Test
        @DisplayName("given completed basic data, when a valid address is provided, then the current step becomes CONFIRMATION")
        void shouldAdvanceToConfirmationWhenAddressIsValid() {
            var solicitation = solicitationWithBasicData();

            solicitation.updateAddress(address());

            assertThat(solicitation.currentStep()).isEqualTo(SolicitationStep.CONFIRMATION);
            assertThat(solicitation.address().cep().value()).isEqualTo("70040902");
        }

        @Test
        @DisplayName("given a solicitation at BASIC_DATA, when address data is provided, then the operation is rejected")
        void shouldRejectAddressBeforeBasicData() {
            var solicitation = newSolicitation();

            assertThatThrownBy(() -> solicitation.updateAddress(address()))
                    .isInstanceOf(InvalidSolicitationStateException.class);

            assertThat(solicitation.address()).isNull();
            assertThat(solicitation.currentStep()).isEqualTo(SolicitationStep.BASIC_DATA);
        }

        @Test
        @DisplayName("given an invalid CEP, when an address is created, then the address is rejected")
        void shouldRejectInvalidCep() {
            assertThatThrownBy(() -> new Cep("70040-9021"))
                    .isInstanceOf(InvalidSolicitationDataException.class);
        }

        @Test
        @DisplayName("given an address number longer than 20 characters, when an address is created, then the address is rejected")
        void shouldRejectAddressNumberLongerThanMaximum() {
            assertThatThrownBy(() -> new Address(
                            new Cep("70040902"),
                            "Main Street",
                            "Central",
                            "Brasilia",
                            State.DF,
                            "123456789012345678901",
                            null))
                    .isInstanceOf(InvalidSolicitationDataException.class);
        }
    }

    @Nested
    @DisplayName("Completing confirmation")
    class CompletingConfirmation {

        @Test
        @DisplayName("given a solicitation whose address is complete, when valid confirmation data is provided, then it is ready for submission")
        void shouldBeReadyForSubmissionWhenConfirmationIsValid() {
            var solicitation = solicitationWithAddress();

            solicitation.updateConfirmation(Priority.MEDIUM, TODAY, new BigDecimal("99.99"), true, CLOCK);

            assertThat(solicitation.isReadyForSubmission()).isTrue();
            assertThat(solicitation.termsAccepted()).isTrue();
        }

        @Test
        @DisplayName("given a preferred date in the past, when confirmation data is provided, then confirmation is rejected")
        void shouldRejectPreferredDateInThePast() {
            var solicitation = solicitationWithAddress();

            assertThatThrownBy(() -> solicitation.updateConfirmation(
                            Priority.MEDIUM, TODAY.minusDays(1), new BigDecimal("100"), true, CLOCK))
                    .isInstanceOf(InvalidSolicitationDataException.class);

            assertThat(solicitation.priority()).isNull();
        }

        @Test
        @DisplayName("given HIGH priority and an estimated value below 100, when confirmation data is provided, then confirmation is rejected")
        void shouldRejectLowEstimatedValueForHighPriority() {
            var solicitation = solicitationWithAddress();

            assertThatThrownBy(() -> solicitation.updateConfirmation(
                            Priority.HIGH, TODAY, new BigDecimal("99.99"), true, CLOCK))
                    .isInstanceOf(InvalidSolicitationDataException.class);

            assertThat(solicitation.priority()).isNull();
        }

        @Test
        @DisplayName("given valid confirmation data and terms not accepted, when confirmation is saved, then the draft remains ready but cannot be submitted")
        void shouldAllowUnacceptedTermsOnDraftButRejectSubmission() {
            var solicitation = solicitationWithAddress();
            solicitation.updateConfirmation(Priority.LOW, TODAY, BigDecimal.ZERO, false, CLOCK);

            assertThat(solicitation.isReadyForSubmission()).isTrue();
            assertThatThrownBy(() -> solicitation.submit(CLOCK))
                    .isInstanceOf(IncompleteSolicitationException.class)
                    .satisfies(exception -> assertThat(((IncompleteSolicitationException) exception).violations())
                            .contains("termsAccepted must be true"));
            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.DRAFT);
        }

        @Test
        @DisplayName("given a valid confirmation, when the preferred date has passed before submission, then submission is rejected")
        void shouldRevalidatePreferredDateWhenSubmitting() {
            var solicitation = solicitationWithAddress();
            var initialClock = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.UTC);
            var laterClock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC);
            solicitation.updateConfirmation(Priority.LOW, TODAY.plusDays(1), BigDecimal.ZERO, true, initialClock);

            assertThatThrownBy(() -> solicitation.submit(laterClock))
                    .isInstanceOf(IncompleteSolicitationException.class);
            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("Submitting a solicitation")
    class SubmittingSolicitation {

        @Test
        @DisplayName("given all steps are complete and terms accepted, when submitted, then status becomes SUBMITTED")
        void shouldSubmitCompleteSolicitation() {
            var solicitation = completeSolicitation();

            solicitation.submit(CLOCK);

            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.SUBMITTED);
        }

        @Test
        @DisplayName("given a solicitation missing address information, when submitted, then submission fails and status remains DRAFT")
        void shouldRejectIncompleteSubmission() {
            var solicitation = solicitationWithBasicData();

            assertThatThrownBy(() -> solicitation.submit(CLOCK))
                    .isInstanceOf(IncompleteSolicitationException.class)
                    .satisfies(exception -> assertThat(((IncompleteSolicitationException) exception).violations())
                            .contains("address is required"));

            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.DRAFT);
        }

        @Test
        @DisplayName("given a solicitation with status SUBMITTED, when basic data is updated, then the update is rejected")
        void shouldPreventModificationsAfterSubmission() {
            var solicitation = completeSolicitation();
            solicitation.submit(CLOCK);

            assertThatThrownBy(() -> solicitation.updateBasicData(
                            serviceType(), "Changed title", "Install equipment at the main office."))
                    .isInstanceOf(InvalidSolicitationStateException.class);
        }
    }

    @Nested
    @DisplayName("Reviewing a solicitation")
    class ReviewingSolicitation {

        @Test
        @DisplayName("given a solicitation with status SUBMITTED, when review starts, then status becomes IN_REVIEW")
        void shouldStartReview() {
            var solicitation = completeSolicitation();
            solicitation.submit(CLOCK);

            solicitation.startReview();

            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.IN_REVIEW);
        }

        @Test
        @DisplayName("given a solicitation with status IN_REVIEW, when approved, then status becomes APPROVED")
        void shouldApproveSolicitation() {
            var solicitation = solicitationInReview();

            solicitation.approve("Validated");

            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.APPROVED);
            assertThat(solicitation.decision()).isEqualTo(Decision.APPROVE);
            assertThat(solicitation.decisionComment()).isEqualTo("Validated");
        }

        @Test
        @DisplayName("given a solicitation with status IN_REVIEW, when rejected, then status becomes REJECTED")
        void shouldRejectSolicitation() {
            var solicitation = solicitationInReview();

            solicitation.reject("Insufficient information");

            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.REJECTED);
            assertThat(solicitation.decision()).isEqualTo(Decision.REJECT);
        }

        @Test
        @DisplayName("given a solicitation with status DRAFT, when approval is attempted, then the operation is rejected")
        void shouldPreventApprovalOfDraft() {
            var solicitation = newSolicitation();

            assertThatThrownBy(() -> solicitation.approve("Validated"))
                    .isInstanceOf(InvalidSolicitationStateException.class);

            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.DRAFT);
        }

        @Test
        @DisplayName("given a solicitation with status DRAFT, when review is started, then the operation is rejected")
        void shouldPreventStartingReviewOfDraft() {
            var solicitation = newSolicitation();

            assertThatThrownBy(solicitation::startReview)
                    .isInstanceOf(InvalidSolicitationStateException.class);
        }
    }

    private static Solicitation newSolicitation() {
        return Solicitation.create(SolicitationId.generate(), new UserId(UUID.randomUUID()));
    }

    private static Solicitation solicitationWithBasicData() {
        var solicitation = newSolicitation();
        solicitation.updateBasicData(serviceType(), "Equipment installation", "Install equipment at the main office.");
        return solicitation;
    }

    private static Solicitation solicitationWithAddress() {
        var solicitation = solicitationWithBasicData();
        solicitation.updateAddress(address());
        return solicitation;
    }

    private static Solicitation completeSolicitation() {
        var solicitation = solicitationWithAddress();
        solicitation.updateConfirmation(Priority.HIGH, TODAY, new BigDecimal("100.00"), true, CLOCK);
        return solicitation;
    }

    private static Solicitation solicitationInReview() {
        var solicitation = completeSolicitation();
        solicitation.submit(CLOCK);
        solicitation.startReview();
        return solicitation;
    }

    private static ServiceType serviceType() {
        return new ServiceType("INSTALLATION");
    }

    private static Address address() {
        return new Address(new Cep("70040-902"), "Main Street", "Central", "Brasilia", State.DF, "123", null);
    }
}
