package uk.gov.moj.cpp.progression.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateAdded;
import uk.gov.moj.cpp.progression.event.service.CaseService;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ExtendWith(MockitoExtension.class)
public class ConvictionDateAddedEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private ConvictionDateAdded convictionDateAdded;

    @Mock
    private CaseProgressionDetail caseProgressionDetail;

    @Mock
    CaseService caseService;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private ConvictionDateAddedEventListener eventListener;

    @Test
    public void shouldHandleConvictionDateAddedEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ConvictionDateAdded.class)).thenReturn(convictionDateAdded);

        eventListener.processEvent(envelope);

        verify(caseService).setConvictionDate(convictionDateAdded.getOffenceId(), convictionDateAdded.getConvictionDate());
    }
}
