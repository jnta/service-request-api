package com.jnta.solicitation.adapter.out.persistence;

import com.jnta.solicitation.application.exception.ConcurrentSolicitationModificationException;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.domain.SolicitationId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JpaSolicitationRepositoryAdapter implements SolicitationRepository {

    private final SolicitationJpaRepository repository;
    private final SolicitationJpaMapper mapper = new SolicitationJpaMapper();

    @Override
    @Transactional
    public VersionedSolicitation save(VersionedSolicitation solicitation) {
        try {
            var entity = repository.saveAndFlush(mapper.toEntity(solicitation));
            return mapper.toVersionedSolicitation(entity);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConcurrentSolicitationModificationException(solicitation.solicitation().id());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VersionedSolicitation> findById(SolicitationId id) {
        return repository.findById(id.value()).map(mapper::toVersionedSolicitation);
    }
}
