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
import static uk.gov.moj.cpp.progression.event.utils.DefaultTestData.CASE_ID;
import static uk.gov.moj.cpp.progression.event.utils.DefaultTestData.DEFENDANT_ID;

@RunWith(MockitoJUnitRunner.class)
public class DefendantAllocationDecisionRemovedListenerTest {
    @Mock
    private CaseProgressionDetailRepository caseRepository;

    @InjectMocks
    private DefendantAllocationDecisionRemovedListener listener;


    @Test
    public void shouldHandleAllocationDecisionRemoved() throws Exception {
        // given
        JsonEnvelope event = envelopeFrom(
                metadataOf(UUID.randomUUID(), "structure.events.defendant-allocation-decision-removed"),
                createObjectBuilder()
                        .add("caseId", CASE_ID.toString())
                        .add("defendantId", DEFENDANT_ID.toString())
                        .build()
        );

        CaseProgressionDetail caseDetail = mock(CaseProgressionDetail.class);
        // and
        Defendant defendantDetail = mock(Defendant.class);
        // and
        when(caseRepository.findBy(eq(CASE_ID))).thenReturn(caseDetail);
        // and
        when(caseDetail.getDefendant(DEFENDANT_ID)).thenReturn(defendantDetail);

        listener.allocationDecisionRemoved(event);

        verify(defendantDetail).setAllocationDecision(null);
    }
}