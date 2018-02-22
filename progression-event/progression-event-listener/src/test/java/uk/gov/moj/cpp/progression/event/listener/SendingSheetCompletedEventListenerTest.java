package uk.gov.moj.cpp.progression.event.listener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Hearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.event.service.CaseService;

import javax.json.JsonObject;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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

        SendingSheetCompleted sendingSheetCompleted = new SendingSheetCompleted();
        sendingSheetCompleted.setHearing(new Hearing());
        sendingSheetCompleted.getHearing().setCaseId( UUID.randomUUID());

        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);
        when(jsonObjectToObjectConverter.convert(payload, SendingSheetCompleted.class))
                        .thenReturn(sendingSheetCompleted);
        //When
        listener.processEvent(envelope);
        //Then
        verify(service).caseAssignedForReview(sendingSheetCompleted.getHearing().getCaseId(), CaseStatusEnum.READY_FOR_REVIEW);
    }

}
