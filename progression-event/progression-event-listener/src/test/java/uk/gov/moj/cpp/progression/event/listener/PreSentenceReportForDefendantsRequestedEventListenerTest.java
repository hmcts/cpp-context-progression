package uk.gov.moj.cpp.progression.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsRequested;
import uk.gov.moj.cpp.progression.event.service.CaseService;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;

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
        when(jsonObjectConverter.convert(payload, PreSentenceReportForDefendantsRequested.class))
                .thenReturn(noMoreInformationRequiredEvent);

        listener.processEvent(envelope);

        verify(service).preSentenceReportForDefendantsRequested(
                noMoreInformationRequiredEvent);
    }


}
