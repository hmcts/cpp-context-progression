package uk.gov.moj.cpp.progression.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.event.service.CaseService;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CasePendingForScentenceHearingEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CasePendingForSentenceHearing casePendingForSentenceHearing;

    @Mock
    private CaseProgressionDetailRepository repository;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private CaseService service;

    @InjectMocks
    private CasePendingForScentenceHearingEventListener listener;

    @Test
    public void testProcessEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);

        when(jsonObjectToObjectConverter.convert(payload, CasePendingForSentenceHearing.class))
                        .thenReturn(casePendingForSentenceHearing);

        listener.processEvent(envelope);

        verify(service).casePendingForSentenceHearing(casePendingForSentenceHearing);
    }

}
