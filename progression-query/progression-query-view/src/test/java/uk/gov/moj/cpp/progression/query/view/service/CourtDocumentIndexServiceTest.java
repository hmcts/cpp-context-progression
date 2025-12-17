package uk.gov.moj.cpp.progression.query.view.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.progression.domain.pojo.SearchCriteria;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentIndexCriteriaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtDocumentIndexServiceTest {

    @InjectMocks
    private CourtDocumentIndexService courtDocumentIndexService;

    @Mock
    private CourtDocumentIndexCriteriaRepository courtDocumentIndexCriteriaRepository;

    @Test
    public void shouldGetCountByCriteria(){
        final SearchCriteria searchCriteria = SearchCriteria.searchCriteria()
                .withCaseId(UUID.randomUUID())
                .build();
        when(courtDocumentIndexCriteriaRepository.countByCriteria(any())).thenReturn(Long.valueOf(5));

        final Long value = courtDocumentIndexService.countByCriteria(searchCriteria);
        assertThat(value, is(5L));
    }

    @Test
    public void shouldGetCourtDocumentIndexByCriteria(){
        final SearchCriteria searchCriteria = SearchCriteria.searchCriteria()
                .withCaseId(UUID.randomUUID())
                .build();
        final List<CourtDocumentIndexEntity> list = new ArrayList<>();
        list.add(new CourtDocumentIndexEntity());
        when(courtDocumentIndexCriteriaRepository.getCourtDocumentIndexByCriteria(any())).thenReturn(list);

        final List<CourtDocumentIndexEntity> courtDocumentIndexEntityList = courtDocumentIndexService.getCourtDocumentIndexByCriteria(searchCriteria);
        assertThat(courtDocumentIndexEntityList.size(), is(1));
    }
}
