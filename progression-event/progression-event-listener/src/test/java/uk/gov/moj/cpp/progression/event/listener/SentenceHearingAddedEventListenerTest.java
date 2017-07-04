package uk.gov.moj.cpp.progression.event.listener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingAdded;
import uk.gov.moj.cpp.progression.event.service.CaseService;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;

import javax.json.JsonObject;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SentenceHearingAddedEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private SentenceHearingAdded sentenceHearingAdded;

    @Mock
    private CaseProgressionDetail caseProgressionDetail;

    @Mock
    CaseService caseService;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private SentenceHearingAddedEventListener eventListener;

    @Test
    public void shouldHandleSentenceHearingAddedEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, SentenceHearingAdded.class))
                        .thenReturn(sentenceHearingAdded);
        when(envelope.metadata()).thenReturn(metadata);

        eventListener.processEvent(envelope);
        verify(caseService).addSentenceHearing(sentenceHearingAdded);
    }
}
