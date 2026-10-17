package com.jnta.solicitation.adapter.out.persistence;

import com.jnta.solicitation.application.port.out.SolicitationSearchProjection;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.domain.SolicitationId;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class SearchIndexingSolicitationRepository implements SolicitationRepository {

    private final SolicitationRepository persistenceRepository;
    private final SolicitationSearchProjection searchProjection;

    public SearchIndexingSolicitationRepository(
            @Qualifier("jpaSolicitationRepositoryAdapter") SolicitationRepository persistenceRepository,
            SolicitationSearchProjection searchProjection) {
        this.persistenceRepository = persistenceRepository;
        this.searchProjection = searchProjection;
    }

    @Override
    public VersionedSolicitation save(VersionedSolicitation solicitation) {
        var stored = persistenceRepository.save(solicitation);
        searchProjection.index(stored);
        return stored;
    }

    @Override
    public Optional<VersionedSolicitation> findById(SolicitationId id) {
        return persistenceRepository.findById(id);
    }
}
