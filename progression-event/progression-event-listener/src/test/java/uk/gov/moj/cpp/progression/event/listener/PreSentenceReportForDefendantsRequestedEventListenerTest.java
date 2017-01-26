package uk.gov.moj.cpp.progression.event.listener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsRequested;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.NoMoreInformationRequiredEvent;
import uk.gov.moj.cpp.progression.event.service.CaseService;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.progression.persistence.repository.DefendantRepository;

import javax.json.JsonObject;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PreSentenceReportForDefendantsRequestedEventListenerTest {

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private PreSentenceReportForDefendantsRequested noMoreInformationRequiredEvent;

    @Mock
    private Defendant defendant;

    @Mock
    private CaseService service;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private PreSentenceReportForDefendantsRequestedEventListener listener;


    @Test
    public void shouldPassiPSRRequested() throws Exception {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);
        when(jsonObjectConverter.convert(payload, PreSentenceReportForDefendantsRequested.class))
                .thenReturn(noMoreInformationRequiredEvent);

        listener.processEvent(envelope);

        verify(service).preSentenceReportForDefendantsRequested(
                noMoreInformationRequiredEvent);
    }


}
