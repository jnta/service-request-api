package com.jnta.solicitation.domain;

import com.jnta.solicitation.domain.exception.IncompleteSolicitationException;
import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import com.jnta.solicitation.domain.exception.InvalidSolicitationStateException;
import com.jnta.user.domain.UserId;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Solicitation {

    private static final BigDecimal HIGH_PRIORITY_MINIMUM_VALUE = new BigDecimal("100");

    private final SolicitationId id;
    private final UserId owner;
    private SolicitationStatus status;
    private SolicitationStep currentStep;
    private ServiceType serviceType;
    private String title;
    private String description;
    private Address address;
    private Priority priority;
    private LocalDate preferredDate;
    private BigDecimal estimatedValue;
    private boolean termsAccepted;
    private Decision decision;
    private String decisionComment;

    private Solicitation(SolicitationId id, UserId owner) {
        if (id == null || owner == null) {
            throw new InvalidSolicitationDataException("Solicitation id and owner are required");
        }
        this.id = id;
        this.owner = owner;
        this.status = SolicitationStatus.DRAFT;
        this.currentStep = SolicitationStep.BASIC_DATA;
    }

    public static Solicitation create(SolicitationId id, UserId owner) {
        return new Solicitation(id, owner);
    }

    public void updateBasicData(ServiceType serviceType, String title, String description) {
        ensureDraft();
        if (serviceType == null) {
            throw new InvalidSolicitationDataException("Service type is required");
        }
        var normalizedTitle = required(title, "Title");
        var normalizedDescription = required(description, "Description");
        if (normalizedTitle.length() < 3 || normalizedTitle.length() > 80) {
            throw new InvalidSolicitationDataException("Title must contain between 3 and 80 characters");
        }
        if (normalizedDescription.length() < 20 || normalizedDescription.length() > 1000) {
            throw new InvalidSolicitationDataException("Description must contain between 20 and 1000 characters");
        }

        this.serviceType = serviceType;
        this.title = normalizedTitle;
        this.description = normalizedDescription;
        if (currentStep == SolicitationStep.BASIC_DATA) {
            currentStep = SolicitationStep.ADDRESS;
        }
    }

    public void updateAddress(Address address) {
        ensureDraft();
        if (serviceType == null) {
            throw new InvalidSolicitationStateException("Basic data must be completed before the address");
        }
        if (address == null) {
            throw new InvalidSolicitationDataException("Address is required");
        }

        this.address = address;
        if (currentStep == SolicitationStep.ADDRESS) {
            currentStep = SolicitationStep.CONFIRMATION;
        }
    }

    public void updateConfirmation(
            Priority priority,
            LocalDate preferredDate,
            BigDecimal estimatedValue,
            boolean termsAccepted,
            Clock clock) {
        ensureDraft();
        if (address == null) {
            throw new InvalidSolicitationStateException("Address must be completed before confirmation");
        }
        if (priority == null) {
            throw new InvalidSolicitationDataException("Priority is required");
        }
        if (preferredDate == null) {
            throw new InvalidSolicitationDataException("Preferred date is required");
        }
        if (clock == null) {
            throw new InvalidSolicitationDataException("Clock is required");
        }
        if (preferredDate.isBefore(LocalDate.now(clock))) {
            throw new InvalidSolicitationDataException("Preferred date cannot be in the past");
        }
        if (estimatedValue == null || estimatedValue.signum() < 0) {
            throw new InvalidSolicitationDataException("Estimated value must not be negative");
        }
        if (priority == Priority.HIGH && estimatedValue.compareTo(HIGH_PRIORITY_MINIMUM_VALUE) < 0) {
            throw new InvalidSolicitationDataException("High priority requires an estimated value of at least 100");
        }

        this.priority = priority;
        this.preferredDate = preferredDate;
        this.estimatedValue = estimatedValue;
        this.termsAccepted = termsAccepted;
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
        status = SolicitationStatus.SUBMITTED;
    }

    public void startReview() {
        ensureStatus(SolicitationStatus.SUBMITTED, "Only submitted solicitations can start review");
        status = SolicitationStatus.IN_REVIEW;
    }

    public void approve(String comment) {
        decide(Decision.APPROVE, comment);
    }

    public void reject(String comment) {
        decide(Decision.REJECT, comment);
    }

    private void decide(Decision decision, String comment) {
        ensureStatus(SolicitationStatus.IN_REVIEW, "Only solicitations in review can be decided");
        this.decision = decision;
        this.decisionComment = comment;
        this.status = decision == Decision.APPROVE ? SolicitationStatus.APPROVED : SolicitationStatus.REJECTED;
    }

    private List<String> submissionViolations(LocalDate today) {
        var violations = new ArrayList<String>();
        if (serviceType == null) {
            violations.add("serviceType is required");
        }
        if (title == null || description == null) {
            violations.add("basicData is required");
        }
        if (address == null) {
            violations.add("address is required");
        }
        if (priority == null) {
            violations.add("priority is required");
        }
        if (preferredDate == null) {
            violations.add("preferredDate is required");
        } else if (preferredDate.isBefore(today)) {
            violations.add("preferredDate cannot be in the past");
        }
        if (estimatedValue == null) {
            violations.add("estimatedValue is required");
        } else if (priority == Priority.HIGH && estimatedValue.compareTo(HIGH_PRIORITY_MINIMUM_VALUE) < 0) {
            violations.add("estimatedValue must be at least 100 for high priority");
        }
        if (!termsAccepted) {
            violations.add("termsAccepted must be true");
        }
        return violations;
    }

    private void ensureDraft() {
        ensureStatus(SolicitationStatus.DRAFT, "Only draft solicitations can be edited or submitted");
    }

    private void ensureStatus(SolicitationStatus expected, String message) {
        if (status != expected) {
            throw new InvalidSolicitationStateException(message);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidSolicitationDataException(field + " is required");
        }
        return value.trim();
    }

    public SolicitationId id() {
        return id;
    }

    public UserId owner() {
        return owner;
    }

    public SolicitationStatus status() {
        return status;
    }

    public SolicitationStep currentStep() {
        return currentStep;
    }

    public ServiceType serviceType() {
        return serviceType;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public Address address() {
        return address;
    }

    public Priority priority() {
        return priority;
    }

    public LocalDate preferredDate() {
        return preferredDate;
    }

    public BigDecimal estimatedValue() {
        return estimatedValue;
    }

    public boolean termsAccepted() {
        return termsAccepted;
    }

    public Decision decision() {
        return decision;
    }

    public String decisionComment() {
        return decisionComment;
    }

    public boolean isReadyForSubmission() {
        return serviceType != null && title != null && description != null && address != null
                && priority != null && preferredDate != null && estimatedValue != null;
    }
}
