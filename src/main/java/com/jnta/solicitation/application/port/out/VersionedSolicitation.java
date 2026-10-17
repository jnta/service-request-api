package com.jnta.solicitation.application.port.out;

import com.jnta.solicitation.domain.Solicitation;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

public record VersionedSolicitation(
        Solicitation solicitation,
        OptionalLong version,
        Optional<Instant> createdAt,
        Optional<Instant> updatedAt) {

    public VersionedSolicitation {
        if (solicitation == null || version == null || createdAt == null || updatedAt == null
                || (version.isPresent() && version.getAsLong() < 0)
                || version.isPresent() != createdAt.isPresent()
                || version.isPresent() != updatedAt.isPresent()) {
            throw new IllegalArgumentException("Solicitation and a valid version are required");
        }
    }

    public static VersionedSolicitation newEntry(Solicitation solicitation) {
        return new VersionedSolicitation(solicitation, OptionalLong.empty(), Optional.empty(), Optional.empty());
    }

    public static VersionedSolicitation persisted(
            Solicitation solicitation, long version, Instant createdAt, Instant updatedAt) {
        return new VersionedSolicitation(
                solicitation, OptionalLong.of(version), Optional.of(createdAt), Optional.of(updatedAt));
    }

    public VersionedSolicitation withSolicitation(Solicitation updated) {
        return new VersionedSolicitation(updated, version, createdAt, updatedAt);
    }
}
