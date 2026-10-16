package com.jnta.solicitation.adapter.out.persistence;

import com.jnta.solicitation.domain.address.State;
import com.jnta.solicitation.domain.confirmation.Priority;
import com.jnta.solicitation.domain.review.Decision;
import com.jnta.solicitation.domain.workflow.SolicitationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "solicitations")
public class SolicitationJpaEntity {

    @Id
    UUID id;

    @Column(name = "client_id", nullable = false)
    UUID clientId;

    @Column(name = "service_type", length = 40)
    String serviceType;

    @Column(length = 80)
    String title;

    @Column(length = 1000)
    String description;

    @Column(length = 8)
    String cep;

    @Column(columnDefinition = "text")
    String street;

    @Column(columnDefinition = "text")
    String neighborhood;

    @Column(columnDefinition = "text")
    String city;

    @Enumerated(EnumType.STRING)
    @Column(length = 2)
    State state;

    @Column(name = "address_number", length = 20)
    String addressNumber;

    @Column(length = 120)
    String complement;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    Priority priority;

    @Column(name = "preferred_date")
    LocalDate preferredDate;

    @Column(name = "estimated_value", columnDefinition = "numeric")
    BigDecimal estimatedValue;

    @Column(name = "terms_accepted", nullable = false)
    boolean termsAccepted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    SolicitationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    Decision.Outcome decision;

    @Column(name = "decision_comment", columnDefinition = "text")
    String decisionComment;

    @Version
    @Column(nullable = false)
    Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected SolicitationJpaEntity() {
    }
}
