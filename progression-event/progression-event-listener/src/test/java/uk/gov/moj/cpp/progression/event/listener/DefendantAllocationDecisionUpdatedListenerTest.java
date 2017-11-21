package uk.gov.moj.cpp.progression.event.listener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import java.util.UUID;

import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.moj.cpp.progression.event.utils.DefaultTestData.ALLOCATION_DECISION;
import static uk.gov.moj.cpp.progression.event.utils.DefaultTestData.CASE_ID;
import static uk.gov.moj.cpp.progression.event.utils.DefaultTestData.DEFENDANT_ID;

@RunWith(MockitoJUnitRunner.class)
public class DefendantAllocationDecisionUpdatedListenerTest {

    @Mock
    private CaseProgressionDetailRepository caseRepository;

    @InjectMocks
    private DefendantAllocationDecisionUpdatedListener listener;


    @Test
    public void shouldHandleAllocationDecisionUpdatedEvent() throws Exception {
        // given
        JsonEnvelope event = envelopeFrom(
                metadataOf(UUID.randomUUID(), "progression.events.defendant-allocation-decision-updated"),
                createObjectBuilder()
                        .add("caseId", CASE_ID.toString())
                        .add("defendantId", DEFENDANT_ID.toString())
                        .add("allocationDecision", ALLOCATION_DECISION)
                        .build()
        );

        CaseProgressionDetail caseDetail = mock(CaseProgressionDetail.class);
        // and
        Defendant defendantDetail = mock(Defendant.class);
        // and
        when(caseRepository.findBy(eq(CASE_ID))).thenReturn(caseDetail);
        // and
        when(caseDetail.getDefendant(DEFENDANT_ID)).thenReturn(defendantDetail);

        listener.allocationDecisionUpdated(event);

        verify(defendantDetail).setAllocationDecision(ALLOCATION_DECISION);
    }
}