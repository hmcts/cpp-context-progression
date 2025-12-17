package uk.gov.moj.cpp.progression.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.event.service.CaseService;

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
public class SendingCommittalHearingInformationAddedEventListenerTest {

    @Mock
    private SendingCommittalHearingInformationAdded sendingCommittalHearingInformationAdded;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private CaseService service;

    @InjectMocks
    private SendingCommittalHearingInformationAddedEventListener listener;

    @Test
    public void testProcessEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, SendingCommittalHearingInformationAdded.class))
                        .thenReturn(sendingCommittalHearingInformationAdded);

        listener.processEvent(envelope);

        verify(service).addSendingCommittalHearingInformation(
                        sendingCommittalHearingInformationAdded);
    }

}
