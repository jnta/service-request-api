package com.jnta.solicitation.application.port.out;

import com.jnta.solicitation.domain.SolicitationId;
import java.util.Optional;

public interface SolicitationRepository {

    VersionedSolicitation save(VersionedSolicitation solicitation);

    Optional<VersionedSolicitation> findById(SolicitationId id);
}
