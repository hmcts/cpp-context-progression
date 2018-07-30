package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.task;

import static java.util.Collections.singletonList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.hearing.Defendant;
import uk.gov.moj.cpp.external.domain.hearing.Hearing;
import uk.gov.moj.cpp.external.domain.hearing.InitiateHearing;
import uk.gov.moj.cpp.external.domain.hearing.Judge;
import uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant;

import java.util.ArrayList;
import java.util.UUID;

import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class InitiateHearingTaskTest {

    @InjectMocks
    private InitiateHearingTask initiateHearingTask;

    @Mock
    private Sender sender;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private Hearing hearing;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private JsonObject jsonObject;

    @Test
    public void shouldInitiateHearingForCaseRequestUsingSender() throws Exception {
        //Given
        final String userId = UUID.randomUUID().toString();
        final UUID hearingId = UUID.randomUUID();
        Hearing hearing = new Hearing(hearingId, UUID.randomUUID(), UUID.randomUUID(),new Judge(UUID.randomUUID()), "", singletonList(""), new ArrayList<Defendant>());
        final InitiateHearing inititateHearing = new InitiateHearing( null,hearing);

        when(delegateExecution.getVariable(ProcessMapConstant.INITIATE_HEARING_PAYLOAD)).thenReturn(inititateHearing);
        when(delegateExecution.getVariable(ProcessMapConstant.USER_ID)).thenReturn(userId);
        when(objectToJsonObjectConverter.convert(inititateHearing)).thenReturn(jsonObject);

        //when
        initiateHearingTask.execute(delegateExecution);


        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope envelope = senderJsonEnvelopeCaptor.getValue();
        assertThat(envelope.metadata().userId().get(), equalTo(userId));
        assertThat(envelope.metadata().name(),
                equalTo("hearing.initiate"));

    }
}
