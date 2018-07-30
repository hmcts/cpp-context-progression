package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.task;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;
import uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant;

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
public class SendCaseForListingTest {

    @InjectMocks
    private SendCaseForListing sendCaseForListing;

    @Mock
    private Sender sender;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Mock
    private JsonObject jsonObject;

    @Test
    public void shouldBookHearingForCaseRequestUsingSender() throws Exception {
        //Given
        final ListingCase listingCase = new ListingCase(UUID.randomUUID(), null, null);
        final String userId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();

        when(delegateExecution.getVariable(ProcessMapConstant.SEND_CASE_FOR_LISTING_PAYLOAD)).thenReturn(listingCase);
        when(delegateExecution.getVariable(ProcessMapConstant.USER_ID)).thenReturn(userId);
        when(delegateExecution.getVariable(ProcessMapConstant.CASE_ID)).thenReturn(caseId);
        when(objectToJsonObjectConverter.convert(listingCase)).thenReturn(jsonObject);

        //when
        sendCaseForListing.execute(delegateExecution);


        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope envelope = senderJsonEnvelopeCaptor.getValue();
        assertThat(envelope.metadata().userId().get(), equalTo(userId));
        assertThat(envelope.metadata().name(),
                equalTo("listing.command.send-case-for-listing"));

    }
}
