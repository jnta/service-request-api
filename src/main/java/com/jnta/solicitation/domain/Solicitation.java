package com.jnta.solicitation.domain;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.domain.address.Address;
import com.jnta.solicitation.domain.basicdata.BasicData;
import com.jnta.solicitation.domain.confirmation.Confirmation;
import com.jnta.solicitation.domain.exception.IncompleteSolicitationException;
import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import com.jnta.solicitation.domain.exception.InvalidSolicitationStateException;
import com.jnta.solicitation.domain.review.Decision;
import com.jnta.solicitation.domain.validation.Violation;
import com.jnta.solicitation.domain.workflow.SolicitationStatus;
import com.jnta.solicitation.domain.workflow.SolicitationStep;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class Solicitation {

    @Getter
    private final SolicitationId id;
    @Getter
    private final UserId owner;
    private SolicitationStatus workflowStatus;
    @Getter
    private BasicData basicData;
    @Getter
    private Address address;
    @Getter
    private Confirmation confirmation;
    @Getter
    private Decision decision;

    private Solicitation(SolicitationId id, UserId owner) {
        if (id == null || owner == null) {
            throw new InvalidSolicitationDataException("Solicitation id and owner are required");
        }
        this.id = id;
        this.owner = owner;
        workflowStatus = SolicitationStatus.DRAFT;
    }

    public static Solicitation create(SolicitationId id, UserId owner) {
        return new Solicitation(id, owner);
    }

    public static Solicitation reconstitute(
            SolicitationId id,
            UserId owner,
            SolicitationStatus status,
            BasicData basicData,
            Address address,
            Confirmation confirmation,
            Decision decision) {
        if (status == null) {
            throw new InvalidSolicitationDataException("Solicitation status is required");
        }

        var solicitation = new Solicitation(id, owner);
        solicitation.basicData = basicData;
        solicitation.address = address;
        solicitation.confirmation = confirmation;

        if (status == SolicitationStatus.APPROVED || status == SolicitationStatus.REJECTED) {
            solicitation.restoreDecision(status, decision);
        } else {
            if (decision != null) {
                throw new InvalidSolicitationDataException("A decision requires a terminal status");
            }
            solicitation.workflowStatus = status;
        }

        if (status != SolicitationStatus.DRAFT && !solicitation.hasCompletedSteps()) {
            throw new InvalidSolicitationDataException("A submitted solicitation must have every step completed");
        }
        if (status != SolicitationStatus.DRAFT && !confirmation.termsAccepted()) {
            throw new InvalidSolicitationDataException("A submitted solicitation must have accepted terms");
        }
        return solicitation;
    }

    public void updateBasicData(BasicData basicData) {
        ensureDraft();
        if (basicData == null) {
            throw new InvalidSolicitationDataException("Basic data is required");
        }
        this.basicData = basicData;
    }

    public void updateAddress(Address address) {
        ensureDraft();
        if (basicData == null) {
            throw new InvalidSolicitationStateException("Basic data must be completed before the address");
        }
        if (address == null) {
            throw new InvalidSolicitationDataException("Address is required");
        }
        this.address = address;
    }

    public void updateConfirmation(Confirmation confirmation, Clock clock) {
        ensureDraft();
        if (address == null) {
            throw new InvalidSolicitationStateException("Address must be completed before confirmation");
        }
        if (confirmation == null) {
            throw new InvalidSolicitationDataException("Confirmation is required");
        }
        if (clock == null) {
            throw new InvalidSolicitationDataException("Clock is required");
        }
        ensurePreferredDateIsValid(confirmation, LocalDate.now(clock));
        this.confirmation = confirmation;
    }

    public void submit(Clock clock) {
        ensureDraft();
        if (clock == null) {
            throw new InvalidSolicitationDataException("Clock is required");
        }

        var violations = submissionViolations(LocalDate.now(clock));
        if (!violations.isEmpty()) {
            throw new IncompleteSolicitationException(violations);
        }
        workflowStatus = SolicitationStatus.SUBMITTED;
    }

    public void startReview() {
        ensureStatus(SolicitationStatus.SUBMITTED, "Only submitted solicitations can start review");
        workflowStatus = SolicitationStatus.IN_REVIEW;
    }

    public void approve(String comment) {
        decide(Decision.Outcome.APPROVE, comment);
    }

    public void reject(String comment) {
        decide(Decision.Outcome.REJECT, comment);
    }

    public SolicitationStatus status() {
        if (decision == null) {
            return workflowStatus;
        }
        return decision.outcome() == Decision.Outcome.APPROVE
                ? SolicitationStatus.APPROVED
                : SolicitationStatus.REJECTED;
    }

    public SolicitationStep currentStep() {
        if (basicData == null) {
            return SolicitationStep.BASIC_DATA;
        }
        if (address == null) {
            return SolicitationStep.ADDRESS;
        }
        return SolicitationStep.CONFIRMATION;
    }

    public boolean hasCompletedSteps() {
        return basicData != null && address != null && confirmation != null;
    }

    public boolean isReadyForSubmission(Clock clock) {
        if (status() != SolicitationStatus.DRAFT || !hasCompletedSteps()) {
            return false;
        }
        if (clock == null) {
            throw new InvalidSolicitationDataException("Clock is required");
        }
        return submissionViolations(LocalDate.now(clock)).isEmpty();
    }

    private void decide(Decision.Outcome outcome, String comment) {
        ensureStatus(SolicitationStatus.IN_REVIEW, "Only solicitations in review can be decided");
        decision = new Decision(outcome, comment);
    }

    private void restoreDecision(SolicitationStatus status, Decision decision) {
        if (decision == null) {
            throw new InvalidSolicitationDataException("A terminal status requires a decision");
        }
        var expectedStatus = decision.outcome() == Decision.Outcome.APPROVE
                ? SolicitationStatus.APPROVED
                : SolicitationStatus.REJECTED;
        if (status != expectedStatus) {
            throw new InvalidSolicitationDataException("Decision does not match solicitation status");
        }
        workflowStatus = SolicitationStatus.IN_REVIEW;
        this.decision = decision;
    }

    private List<Violation> submissionViolations(LocalDate today) {
        var violations = new ArrayList<Violation>();
        if (basicData == null) {
            violations.add(new Violation("basicData", "Basic data is required"));
        }
        if (address == null) {
            violations.add(new Violation("address", "Address is required"));
        }
        if (confirmation == null) {
            violations.add(new Violation("confirmation", "Confirmation is required"));
        } else {
            violations.addAll(confirmation.submissionViolations(today));
        }
        return List.copyOf(violations);
    }

    private void ensurePreferredDateIsValid(Confirmation confirmation, LocalDate today) {
        if (confirmation.preferredDate().isBefore(today)) {
            throw new InvalidSolicitationDataException("Preferred date cannot be in the past");
        }
    }

    private void ensureDraft() {
        ensureStatus(SolicitationStatus.DRAFT, "Only draft solicitations can be edited or submitted");
    }

    private void ensureStatus(SolicitationStatus expected, String message) {
        if (status() != expected) {
            throw new InvalidSolicitationStateException(message);
        }
    }
}
