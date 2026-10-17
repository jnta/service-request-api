package com.jnta.search.adapter.out.elasticsearch;

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

    private final ElasticsearchOperations operations;

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

    private void ensureIndex() {
        var index = operations.indexOps(SolicitationSearchDocument.class);
        if (!index.exists()) {
            index.createWithMapping();
        }
    }

    private Criteria toCriteria(SolicitationSearchCriteria filters) {
        var criteria = new Criteria("id").exists();
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
            document.setServiceType(basicData.serviceType().value());
            document.setTitle(basicData.title());
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
