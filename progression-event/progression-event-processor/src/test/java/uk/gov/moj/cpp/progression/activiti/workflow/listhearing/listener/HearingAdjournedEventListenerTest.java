package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.listener;

import static org.mockito.Mockito.verify;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;
import uk.gov.moj.cpp.progression.activiti.workflow.listhearing.ListHearingService;
import uk.gov.moj.cpp.progression.helper.JsonHelper;

import java.util.HashMap;
import java.util.UUID;

import javax.json.Json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingAdjournedEventListenerTest {

    private static final String PUBLIC_HEARING_ADJOURNED = "public.hearing.adjourned";
    final ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Mock
    private ListHearingService listHearingService;

    @InjectMocks
    private HearingAdjournedEventListener testObj;

    @Test
    public void processEvent() throws Exception {
        //given
        final String userId = UUID.randomUUID().toString();
        final JsonEnvelope jsonEnvelope = JsonHelper.createJsonEnvelope(
                JsonHelper.createMetadataWithProcessIdAndUserId(UUID.randomUUID().toString(), PUBLIC_HEARING_ADJOURNED, UUID.randomUUID().toString(), userId),
                Json.createObjectBuilder().add("caseId", UUID.randomUUID().toString()).build());
        //when
        testObj.processEvent(jsonEnvelope);

        //then
        verify(listHearingService).startProcess(captor.capture());
        ListingCase listingCase = (ListingCase) captor.getValue().get("sendCaseForlistingPayload");
        Assert.assertThat(captor.getValue().get("caseId"), IsEqual.equalTo(listingCase.getCaseId()));
        Assert.assertThat(captor.getValue().get("userId"), IsEqual.equalTo(userId));
        Assert.assertThat(captor.getValue().get("WHEN"), IsEqual.equalTo("Schedule Adjourment"));

    }

}