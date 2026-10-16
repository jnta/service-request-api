package com.jnta.solicitation.application.port.out;

import com.jnta.solicitation.domain.Solicitation;
import java.util.OptionalLong;

public record VersionedSolicitation(Solicitation solicitation, OptionalLong version) {

    public VersionedSolicitation {
        if (solicitation == null || version == null || (version.isPresent() && version.getAsLong() < 0)) {
            throw new IllegalArgumentException("Solicitation and a valid version are required");
        }
    }

    public static VersionedSolicitation newEntry(Solicitation solicitation) {
        return new VersionedSolicitation(solicitation, OptionalLong.empty());
    }

    public static VersionedSolicitation persisted(Solicitation solicitation, long version) {
        return new VersionedSolicitation(solicitation, OptionalLong.of(version));
    }
}
