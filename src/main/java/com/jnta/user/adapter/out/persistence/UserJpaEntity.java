package com.jnta.user.adapter.out.persistence;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.user.domain.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    UUID id;

    @Column(nullable = false, length = 120)
    String name;

    @Column(nullable = false, unique = true, length = 320)
    String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 20)
    Role role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analyst_states", joinColumns = @JoinColumn(name = "analyst_id"))
    @Column(name = "state", nullable = false, length = 2)
    @Enumerated(EnumType.STRING)
    Set<BrazilianState> coverage = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    protected UserJpaEntity() {
    }
}
