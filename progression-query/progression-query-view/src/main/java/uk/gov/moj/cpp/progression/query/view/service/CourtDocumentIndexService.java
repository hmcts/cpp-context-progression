package uk.gov.moj.cpp.progression.query.view.service;

import uk.gov.moj.cpp.progression.domain.pojo.SearchCriteria;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentIndexCriteriaRepository;

import java.util.List;

import javax.inject.Inject;

public class CourtDocumentIndexService {

    @Inject
    private CourtDocumentIndexCriteriaRepository courtDocumentIndexCriteriaRepository;

    public Long countByCriteria(final SearchCriteria searchCriteria) {
        return courtDocumentIndexCriteriaRepository.countByCriteria(searchCriteria);
    }

    public List<CourtDocumentIndexEntity> getCourtDocumentIndexByCriteria(final SearchCriteria searchCriteria) {
        return courtDocumentIndexCriteriaRepository.getCourtDocumentIndexByCriteria(searchCriteria);
    }

}
