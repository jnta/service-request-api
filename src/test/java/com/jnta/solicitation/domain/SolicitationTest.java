package com.jnta.solicitation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.domain.address.Address;
import com.jnta.solicitation.domain.address.Cep;
import com.jnta.solicitation.domain.address.State;
import com.jnta.solicitation.domain.basicdata.BasicData;
import com.jnta.solicitation.domain.basicdata.ServiceType;
import com.jnta.solicitation.domain.confirmation.Confirmation;
import com.jnta.solicitation.domain.confirmation.Priority;
import com.jnta.solicitation.domain.exception.IncompleteSolicitationException;
import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import com.jnta.solicitation.domain.exception.InvalidSolicitationStateException;
import com.jnta.solicitation.domain.review.Decision;
import com.jnta.solicitation.domain.validation.Violation;
import com.jnta.solicitation.domain.workflow.SolicitationStatus;
import com.jnta.solicitation.domain.workflow.SolicitationStep;
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
    @DisplayName("Completing solicitation steps")
    class CompletingSteps {

        @Test
        @DisplayName("given a draft, when basic data is provided, then the current step becomes ADDRESS")
        void shouldAdvanceToAddressWhenBasicDataIsProvided() {
            var solicitation = newSolicitation();

            solicitation.updateBasicData(basicData());

            assertThat(solicitation.currentStep()).isEqualTo(SolicitationStep.ADDRESS);
            assertThat(solicitation.basicData()).isEqualTo(basicData());
        }

        @Test
        @DisplayName("given a draft at BASIC_DATA, when address is provided, then the operation is rejected")
        void shouldRejectAddressBeforeBasicData() {
            var solicitation = newSolicitation();

            assertThatThrownBy(() -> solicitation.updateAddress(address()))
                    .isInstanceOf(InvalidSolicitationStateException.class);

            assertThat(solicitation.address()).isNull();
            assertThat(solicitation.currentStep()).isEqualTo(SolicitationStep.BASIC_DATA);
        }

        @Test
        @DisplayName("given completed basic data, when an address is provided, then the current step becomes CONFIRMATION")
        void shouldAdvanceToConfirmationWhenAddressIsProvided() {
            var solicitation = solicitationWithBasicData();

            solicitation.updateAddress(address());

            assertThat(solicitation.currentStep()).isEqualTo(SolicitationStep.CONFIRMATION);
            assertThat(solicitation.address()).isEqualTo(address());
        }

        @Test
        @DisplayName("given a draft with an address, when confirmation data is provided, then all steps are complete")
        void shouldCompleteAllStepsWhenConfirmationIsProvided() {
            var solicitation = solicitationWithAddress();
            var confirmation = confirmation(Priority.MEDIUM, "99.99", true, TODAY);

            solicitation.updateConfirmation(confirmation, CLOCK);

            assertThat(solicitation.hasCompletedSteps()).isTrue();
            assertThat(solicitation.isReadyForSubmission(CLOCK)).isTrue();
            assertThat(solicitation.confirmation()).isEqualTo(confirmation);
            assertThat(solicitation.currentStep()).isEqualTo(SolicitationStep.CONFIRMATION);
        }

        @Test
        @DisplayName("given a draft, when basic data is replaced after completing later steps, then completed steps remain complete")
        void shouldDeriveCurrentStepFromCompletedData() {
            var solicitation = solicitationWithAddress();
            solicitation.updateConfirmation(confirmation(Priority.LOW, "0", true, TODAY), CLOCK);

            solicitation.updateBasicData(new BasicData(serviceType(), "Updated title", "Updated description for the request."));

            assertThat(solicitation.currentStep()).isEqualTo(SolicitationStep.CONFIRMATION);
            assertThat(solicitation.address()).isNotNull();
            assertThat(solicitation.confirmation()).isNotNull();
        }

        @Test
        @DisplayName("given a preferred date in the past, when confirmation is updated, then the draft remains unchanged")
        void shouldRejectPastPreferredDateWithoutChangingConfirmation() {
            var solicitation = solicitationWithAddress();
            var pastConfirmation = confirmation(Priority.LOW, "0", true, TODAY.minusDays(1));

            assertThatThrownBy(() -> solicitation.updateConfirmation(pastConfirmation, CLOCK))
                    .isInstanceOf(InvalidSolicitationDataException.class);

            assertThat(solicitation.confirmation()).isNull();
            assertThat(solicitation.currentStep()).isEqualTo(SolicitationStep.CONFIRMATION);
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
        @DisplayName("given an incomplete draft, when submitted, then every missing step is reported and status remains DRAFT")
        void shouldReportAllMissingStepsWhenSubmissionIsIncomplete() {
            var solicitation = newSolicitation();

            assertThatThrownBy(() -> solicitation.submit(CLOCK))
                    .isInstanceOf(IncompleteSolicitationException.class)
                    .satisfies(exception -> assertThat(((IncompleteSolicitationException) exception).violations())
                            .containsExactly(
                                    new Violation("basicData", "Basic data is required"),
                                    new Violation("address", "Address is required"),
                                    new Violation("confirmation", "Confirmation is required")));

            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.DRAFT);
        }

        @Test
        @DisplayName("given a draft without accepted terms, when submitted, then submission is rejected")
        void shouldRejectSubmissionWhenTermsAreNotAccepted() {
            var solicitation = solicitationWithAddress();
            solicitation.updateConfirmation(confirmation(Priority.LOW, "0", false, TODAY), CLOCK);

            assertThat(solicitation.hasCompletedSteps()).isTrue();
            assertThat(solicitation.isReadyForSubmission(CLOCK)).isFalse();

            assertThatThrownBy(() -> solicitation.submit(CLOCK))
                    .isInstanceOf(IncompleteSolicitationException.class)
                    .satisfies(exception -> assertThat(((IncompleteSolicitationException) exception).violations())
                            .contains(new Violation("termsAccepted", "Terms must be accepted")));

            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.DRAFT);
        }

        @Test
        @DisplayName("given a preferred date that has passed, when submitted, then submission is rejected")
        void shouldRevalidatePreferredDateAtSubmission() {
            var solicitation = solicitationWithAddress();
            var initialClock = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.UTC);
            var laterClock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC);
            solicitation.updateConfirmation(confirmation(Priority.LOW, "0", true, TODAY.plusDays(1)), initialClock);

            assertThatThrownBy(() -> solicitation.submit(laterClock))
                    .isInstanceOf(IncompleteSolicitationException.class)
                    .satisfies(exception -> assertThat(((IncompleteSolicitationException) exception).violations())
                            .contains(new Violation("preferredDate", "Preferred date cannot be in the past")));

            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.DRAFT);
        }

        @Test
        @DisplayName("given a submitted solicitation, when basic data is updated, then the update is rejected")
        void shouldPreventModificationsAfterSubmission() {
            var solicitation = completeSolicitation();
            solicitation.submit(CLOCK);

            assertThatThrownBy(() -> solicitation.updateBasicData(basicData()))
                    .isInstanceOf(InvalidSolicitationStateException.class);
        }
    }

    @Nested
    @DisplayName("Reviewing a solicitation")
    class ReviewingSolicitation {

        @Test
        @DisplayName("given a submitted solicitation, when review starts, then status becomes IN_REVIEW")
        void shouldStartReview() {
            var solicitation = submittedSolicitation();

            solicitation.startReview();

            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.IN_REVIEW);
        }

        @Test
        @DisplayName("given a solicitation in review, when approved, then it stores the approval decision")
        void shouldApproveSolicitation() {
            var solicitation = solicitationInReview();

            solicitation.approve("Validated");

            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.APPROVED);
            assertThat(solicitation.decision()).isEqualTo(new Decision(Decision.Outcome.APPROVE, "Validated"));
        }

        @Test
        @DisplayName("given a solicitation in review, when rejected, then it stores the rejection decision")
        void shouldRejectSolicitation() {
            var solicitation = solicitationInReview();

            solicitation.reject("Insufficient information");

            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.REJECTED);
            assertThat(solicitation.decision())
                    .isEqualTo(new Decision(Decision.Outcome.REJECT, "Insufficient information"));
        }

        @Test
        @DisplayName("given a draft, when approval is attempted, then the operation is rejected")
        void shouldPreventApprovalOfDraft() {
            var solicitation = newSolicitation();

            assertThatThrownBy(() -> solicitation.approve("Validated"))
                    .isInstanceOf(InvalidSolicitationStateException.class);

            assertThat(solicitation.status()).isEqualTo(SolicitationStatus.DRAFT);
            assertThat(solicitation.decision()).isNull();
        }

        @Test
        @DisplayName("given a draft, when review starts, then the operation is rejected")
        void shouldPreventStartingReviewOfDraft() {
            var solicitation = newSolicitation();

            assertThatThrownBy(solicitation::startReview)
                    .isInstanceOf(InvalidSolicitationStateException.class);
        }
    }

    private static Solicitation newSolicitation() {
        return Solicitation.create(SolicitationId.generate(), new UserId(UUID.randomUUID()));
    }

    private static BasicData basicData() {
        return new BasicData(serviceType(), "Equipment installation", "Install equipment at the main office.");
    }

    private static Solicitation solicitationWithBasicData() {
        var solicitation = newSolicitation();
        solicitation.updateBasicData(basicData());
        return solicitation;
    }

    private static Solicitation solicitationWithAddress() {
        var solicitation = solicitationWithBasicData();
        solicitation.updateAddress(address());
        return solicitation;
    }

    private static Solicitation completeSolicitation() {
        var solicitation = solicitationWithAddress();
        solicitation.updateConfirmation(confirmation(Priority.HIGH, "100.00", true, TODAY), CLOCK);
        return solicitation;
    }

    private static Solicitation submittedSolicitation() {
        var solicitation = completeSolicitation();
        solicitation.submit(CLOCK);
        return solicitation;
    }

    private static Solicitation solicitationInReview() {
        var solicitation = submittedSolicitation();
        solicitation.startReview();
        return solicitation;
    }

    private static ServiceType serviceType() {
        return new ServiceType("INSTALLATION");
    }

    private static Address address() {
        return new Address(new Cep("70040-902"), "Main Street", "Central", "Brasilia", State.DF, "123", null);
    }

    private static Confirmation confirmation(Priority priority, String value, boolean termsAccepted, LocalDate date) {
        return new Confirmation(priority, date, new BigDecimal(value), termsAccepted);
    }
}
