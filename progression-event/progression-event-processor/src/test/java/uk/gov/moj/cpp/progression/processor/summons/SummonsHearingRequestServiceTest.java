package uk.gov.moj.cpp.progression.processor.summons;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds;
import static uk.gov.justice.core.courts.CreateHearingApplicationRequest.createHearingApplicationRequest;
import static uk.gov.justice.core.courts.ListDefendantRequest.listDefendantRequest;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CreateHearingApplicationRequest;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SummonsHearingRequestServiceTest {

    private static final UUID DEFENDANT_ID = randomUUID();

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @InjectMocks
    private SummonsHearingRequestService summonsHearingRequestService;

    @BeforeEach
    public void setup() {
        setField(summonsHearingRequestService, "objectToJsonObjectConverter", new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper()));
    }

    @Test
    public void addDefendantRequestToHearing() {

        final UUID hearingId = randomUUID();

        summonsHearingRequestService.addDefendantRequestToHearing(jsonEnvelope(), getDefendantRequests(), hearingId);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope<JsonObject> commandEnveloper = envelopeCaptor.getValue();
        assertThat(commandEnveloper.metadata().name(), is("progression.command.create-hearing-defendant-request"));
        assertThat(commandEnveloper.payload().getString("hearingId"), is(hearingId.toString()));
        assertThat(commandEnveloper.payload().getJsonArray("defendantRequests").getJsonObject(0).getString("defendantId"), is(DEFENDANT_ID.toString()));

    }

    @Test
    public void addApplicationRequestToHearing() {

        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        summonsHearingRequestService.addApplicationRequestToHearing(jsonEnvelope(), getHearingApplicationRequest(hearingId, applicationId));

        verify(sender).send(envelopeCaptor.capture());

        final Envelope<JsonObject> commandEnveloper = envelopeCaptor.getValue();
        assertThat(commandEnveloper.metadata().name(), is("progression.command.create-hearing-application-request"));
        assertThat(commandEnveloper.payload().getString("hearingId"), is(hearingId.toString()));
        assertThat(commandEnveloper.payload().getJsonArray("applicationRequests").getJsonObject(0).getString("courtApplicationId"), is(applicationId.toString()));

    }

    private CreateHearingApplicationRequest getHearingApplicationRequest(final UUID hearingId, final UUID applicationId) {
        return createHearingApplicationRequest().withHearingId(hearingId)
                .withApplicationRequests(singletonList(courtApplicationPartyListingNeeds().withCourtApplicationId(applicationId).build()))
                .build();
    }

    private List<ListDefendantRequest> getDefendantRequests() {
        return Lists.newArrayList(listDefendantRequest().withDefendantId(DEFENDANT_ID).build());
    }

    private JsonEnvelope jsonEnvelope() {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName("something").build(), createObjectBuilder().build());
    }
}