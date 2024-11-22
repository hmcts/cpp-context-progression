package uk.gov.moj.cpp.progression.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Hearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.event.service.CaseService;

import java.util.UUID;

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
public class SendingSheetCompletedEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private CaseService service;

    @InjectMocks
    private SendingSheetCompletedEventListener listener;

    @Test
    public void testProcessEvent() throws Exception {

        final SendingSheetCompleted sendingSheetCompleted = new SendingSheetCompleted();
        sendingSheetCompleted.setHearing(new Hearing());
        sendingSheetCompleted.getHearing().setCaseId( UUID.randomUUID());

        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, SendingSheetCompleted.class))
                        .thenReturn(sendingSheetCompleted);
        //When
        listener.processEvent(envelope);
        //Then
        verify(service).caseAssignedForReview(sendingSheetCompleted.getHearing().getCaseId(), CaseStatusEnum.READY_FOR_REVIEW);
    }

}
