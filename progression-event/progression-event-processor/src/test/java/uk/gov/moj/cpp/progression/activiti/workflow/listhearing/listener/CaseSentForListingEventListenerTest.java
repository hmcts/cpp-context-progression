package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.listing.event.CaseSentForListing;
import uk.gov.moj.cpp.progression.activiti.service.ActivitiService;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CaseSentForListingEventListenerTest {

    @Mock
    private ActivitiService activitiService;


    @InjectMocks
    private CaseSentForListingEventListener caseSentForListingEventListener;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private JsonEnvelope event;

    @Mock
    private JsonObject jsonObject;

    private static final String USER_ID = randomUUID().toString();

    @Test
    public void shouldSignalProcessWithActivitiIdIfCaseIdPresent() {
        //Given
        final UUID caseId = UUID.randomUUID();
        final CaseSentForListing caseSentForListing = new CaseSentForListing(caseId);
        when(event.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObject.containsKey("caseId")).thenReturn(true);
        when(jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseSentForListing.class)).thenReturn(caseSentForListing);


        //when
        caseSentForListingEventListener.processEvent(event);
        //then
        verify(activitiService).signalProcessByActivitiIdAndFieldName(eq("recieveListingCreatedConfirmation"), eq("caseId"), eq(caseId));
    }

    @Test
    public void shouldNotSignalProcessWithActivitiIdIfCaseIdNotPresent() {
        //Given
        final String fakeId = UUID.randomUUID().toString();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("fakeId", fakeId)
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("public.progression.defendant-added")
                .withUserId(USER_ID), jsonObject);

        //when
        caseSentForListingEventListener.processEvent(jsonEnvelope);
        //then
        verify(activitiService, never()).signalProcessByActivitiIdAndFieldName(any(), any(), any());
    }

}
