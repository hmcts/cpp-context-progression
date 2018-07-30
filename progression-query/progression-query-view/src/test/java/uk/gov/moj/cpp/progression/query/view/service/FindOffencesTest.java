package uk.gov.moj.cpp.progression.query.view.service;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.OffenceRepository;
import uk.gov.moj.cpp.progression.query.view.AbstractProgressionQueryBaseTest;
import uk.gov.moj.cpp.progression.query.view.response.OffenceView;
import uk.gov.moj.cpp.progression.query.view.response.OffencesView;

@RunWith(MockitoJUnitRunner.class)
public class FindOffencesTest extends AbstractProgressionQueryBaseTest {

    @Mock
    private CaseProgressionDetailRepository caseRepository;

    @Mock
    private OffenceRepository offenceRepository;

    @InjectMocks
    private OffencesService offencesService;

    @Test
    public void canFindOffencesForCaseIdAndDefendantId() throws Exception {
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final Defendant defendant = getDefendants(defendantId, caseId).get(0);
        defendant.setDefendantId(defendantId);
        when(caseRepository.findCaseDefendants(anyObject())).thenReturn(Arrays.asList(defendant));
        when(offenceRepository.findByDefendantOrderByOrderIndex(anyObject()))
                        .thenReturn(Arrays.asList(getOffenceDetail(offenceId)));

        final OffencesView view = new OffencesView(getOffencesAsList(Arrays.asList(getOffenceDetail(offenceId))));

        final OffencesView result = offencesService.findOffences(caseId.toString(), defendantId.toString());

        verify(caseRepository, atMost(1)).findCaseDefendants(anyObject());
        verify(offenceRepository, atMost(1)).findByDefendantOrderByOrderIndex(anyObject());
        assertNotNull(result);
        assertEquals(result.getOffences().get(0).getId(), view.getOffences().get(0).getId());

    }

    private List<OffenceView> getOffencesAsList(final List<OffenceDetail> offenceDetails) {
        final OffenceView view = new OffenceView(offenceDetails.get(0));
        final List<OffenceView> offenceViews = new ArrayList<>();
        offenceViews.add(view);
        return offenceViews;
    }

}
