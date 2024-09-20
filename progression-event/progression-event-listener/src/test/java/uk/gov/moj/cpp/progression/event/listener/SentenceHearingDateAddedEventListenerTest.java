package uk.gov.moj.cpp.progression.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.event.service.CaseService;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ExtendWith(MockitoExtension.class)
public class SentenceHearingDateAddedEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private SentenceHearingDateAdded sentenceHearingDateAdded;

    @Mock
    private CaseProgressionDetail caseProgressionDetail;

    @Mock
    CaseService caseService;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private SentenceHearingDateAddedEventListener eventListener;

    @Test
    public void shouldHandleSentenceHearingDateAddedEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, SentenceHearingDateAdded.class))
                        .thenReturn(sentenceHearingDateAdded);

        eventListener.processEvent(envelope);

        verify(caseService).addSentenceHearingDate(sentenceHearingDateAdded);
    }
}
