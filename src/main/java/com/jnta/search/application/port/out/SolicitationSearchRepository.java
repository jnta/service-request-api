package com.jnta.search.application.port.out;

import com.jnta.search.application.SolicitationSearchCriteria;
import com.jnta.search.application.SolicitationSearchPage;

public interface SolicitationSearchRepository {

    SolicitationSearchPage search(SolicitationSearchCriteria criteria);
}
