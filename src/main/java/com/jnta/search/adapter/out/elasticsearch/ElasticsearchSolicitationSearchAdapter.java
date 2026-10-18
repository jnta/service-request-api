package com.jnta.search.adapter.out.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import com.jnta.search.application.SolicitationSearchCriteria;
import com.jnta.search.application.SolicitationSearchPage;
import com.jnta.search.application.SolicitationSearchResult;
import com.jnta.search.application.port.out.SolicitationSearchRepository;
import com.jnta.shared.geography.BrazilianState;
import com.jnta.solicitation.application.port.out.SolicitationSearchProjection;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.domain.address.Address;
import com.jnta.solicitation.domain.basicdata.BasicData;
import com.jnta.solicitation.domain.confirmation.Confirmation;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ElasticsearchSolicitationSearchAdapter
        implements SolicitationSearchProjection, SolicitationSearchRepository {

    private static final String INDEX_READY_TIMEOUT = "5s";

    private final ElasticsearchOperations operations;
    private final ElasticsearchClient client;
    private boolean indexReady;

    @Override
    public void index(VersionedSolicitation entry) {
        ensureIndex();
        operations.save(toDocument(entry));
    }

    @Override
    public SolicitationSearchPage search(SolicitationSearchCriteria criteria) {
        ensureIndex();
        var pageable = PageRequest.of(
                criteria.page(),
                criteria.size(),
                Sort.by(Sort.Direction.valueOf(criteria.sortDirection().name()), criteria.sortField().property()));
        var hits = operations.search(new CriteriaQuery(toCriteria(criteria), pageable), SolicitationSearchDocument.class);
        var items = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(ElasticsearchSolicitationSearchAdapter::toResult)
                .toList();
        return new SolicitationSearchPage(items, hits.getTotalHits(), criteria.page(), criteria.size());
    }

    private synchronized void ensureIndex() {
        var index = operations.indexOps(SolicitationSearchDocument.class);
        boolean exists = index.exists();
        if (indexReady && exists) {
            return;
        }
        if (!exists) {
            try {
                index.createWithMapping();
            } catch (DataAccessException exception) {
                if (!index.exists()) {
                    throw exception;
                }
            }
        }
        try {
            var health = client.cluster().health(request -> request
                    .index(index.getIndexCoordinates().getIndexName())
                    .waitForStatus(HealthStatus.Yellow)
                    .timeout(timeout -> timeout.time(INDEX_READY_TIMEOUT)));
            if (health.timedOut() || health.status() == HealthStatus.Red) {
                throw new DataAccessResourceFailureException("Elasticsearch search index is not ready");
            }
        } catch (java.io.IOException exception) {
            throw new DataAccessResourceFailureException("Failed to check Elasticsearch search index health", exception);
        }
        indexReady = true;
    }

    private Criteria toCriteria(SolicitationSearchCriteria filters) {
        var criteria = new Criteria("id").exists();
        if (filters.q() != null) {
            var textCriteria = new Criteria("title").matches(filters.q())
                    .or(new Criteria("description").matches(filters.q()));
            criteria = criteria.and(textCriteria);
        }
        if (filters.status() != null) {
            criteria = criteria.and("status").is(filters.status().name());
        }
        if (filters.serviceType() != null) {
            criteria = criteria.and("serviceType").is(filters.serviceType());
        }
        if (filters.priority() != null) {
            criteria = criteria.and("priority").is(filters.priority().name());
        }
        if (!filters.allowedStates().isEmpty()) {
            criteria = criteria.and("state").in(filters.allowedStates().stream().map(BrazilianState::name).toList());
        }
        if (filters.fromDate() != null) {
            criteria = criteria.and("preferredDate").greaterThanEqual(filters.fromDate());
        }
        if (filters.toDate() != null) {
            criteria = criteria.and("preferredDate").lessThanEqual(filters.toDate());
        }
        return criteria;
    }

    private SolicitationSearchDocument toDocument(VersionedSolicitation entry) {
        var solicitation = entry.solicitation();
        var document = new SolicitationSearchDocument();
        document.setId(solicitation.id().value().toString());
        document.setClientId(solicitation.owner().value().toString());
        document.setStatus(solicitation.status().name());
        document.setCreatedAt(entry.createdAt().orElseThrow());
        document.setUpdatedAt(entry.updatedAt().orElseThrow());
        setBasicData(document, solicitation.basicData());
        setAddress(document, solicitation.address());
        setConfirmation(document, solicitation.confirmation());
        return document;
    }

    private static void setBasicData(SolicitationSearchDocument document, BasicData basicData) {
        if (basicData != null) {
            document.setServiceType(basicData.serviceType().name());
            document.setTitle(basicData.title());
            document.setDescription(basicData.description());
        }
    }

    private static void setAddress(SolicitationSearchDocument document, Address address) {
        if (address != null) {
            document.setState(address.state().name());
        }
    }

    private static void setConfirmation(SolicitationSearchDocument document, Confirmation confirmation) {
        if (confirmation != null) {
            document.setPriority(confirmation.priority().name());
            document.setPreferredDate(confirmation.preferredDate());
        }
    }

    private static SolicitationSearchResult toResult(SolicitationSearchDocument document) {
        return new SolicitationSearchResult(
                UUID.fromString(document.getId()),
                UUID.fromString(document.getClientId()),
                document.getServiceType(),
                document.getTitle(),
                com.jnta.solicitation.domain.workflow.SolicitationStatus.valueOf(document.getStatus()),
                document.getPriority() == null
                        ? null
                        : com.jnta.solicitation.domain.confirmation.Priority.valueOf(document.getPriority()),
                document.getState() == null ? null : BrazilianState.valueOf(document.getState()),
                document.getPreferredDate(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}
