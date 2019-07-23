package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@Repository(forEntity = SearchProsecutionCaseEntity.class)
public abstract class SearchProsecutionCaseRepository extends AbstractEntityRepository<SearchProsecutionCaseEntity, UUID> {

    @Query("from SearchProsecutionCaseEntity sc where LOWER(sc.searchTarget) LIKE LOWER(:searchCriteria) order by isStandaloneApplication, (defendantFirstName || defendantLastName) NULLS LAST, (defendantFirstName || defendantLastName), prosecutor")
    public abstract List<SearchProsecutionCaseEntity> findBySearchCriteria(@QueryParam("searchCriteria") final String searchCriteria);
}


