package uk.gov.moj.cpp.progression.command.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.LinkType.LINKED;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingQueryServiceTest {

    @Mock
    private Requester requester;

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    private HearingQueryService hearingQueryService;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private final Envelope<JsonObject> command = envelopeFrom(metadataWithRandomUUID("progression.initiate-court-proceedings-for-application"), createObjectBuilder().build());

    @Test
    public void shouldReturnCourtApplicationsOfHearing() {
        final UUID hearingId = randomUUID();
        final UUID parentApplicationId = randomUUID();
        final UUID childApplicationId = randomUUID();
        givenHearingResponse(createObjectBuilder().add("hearing", createObjectBuilder()
                .add("id", hearingId.toString())
                .add("courtApplications", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", parentApplicationId.toString())
                                .add("type", createObjectBuilder().add("linkType", "LINKED")))
                        .add(createObjectBuilder()
                                .add("id", childApplicationId.toString())
                                .add("parentApplicationId", parentApplicationId.toString()))))
                .build());

        final List<CourtApplication> result = hearingQueryService.getCourtApplications(hearingId, command);

        assertThat(result.size(), is(2));
        assertThat(result.get(0).getId(), is(parentApplicationId));
        assertThat(result.get(0).getType().getLinkType(), is(LINKED));
        assertThat(result.get(1).getId(), is(childApplicationId));
        assertThat(result.get(1).getParentApplicationId(), is(parentApplicationId));
        verify(requester).requestAsAdmin(envelopeCaptor.capture(), eq(JsonObject.class));
        assertThat(envelopeCaptor.getValue().metadata().name(), is("hearing.get.hearing"));
        assertThat(envelopeCaptor.getValue().payload().getString("hearingId"), is(hearingId.toString()));
    }

    @Test
    public void shouldReturnEmptyWhenHearingHasNoCourtApplications() {
        givenHearingResponse(createObjectBuilder().add("hearing", createObjectBuilder().add("id", randomUUID().toString())).build());

        assertThat(hearingQueryService.getCourtApplications(randomUUID(), command), is(empty()));
    }

    @Test
    public void shouldReturnEmptyWhenHearingNotFound() {
        givenHearingResponse(createObjectBuilder().build());

        assertThat(hearingQueryService.getCourtApplications(randomUUID(), command), is(empty()));
    }

    private void givenHearingResponse(final JsonObject payload) {
        when(requester.requestAsAdmin(any(Envelope.class), eq(JsonObject.class)))
                .thenReturn(envelopeFrom(metadataWithRandomUUID("hearing.get.hearing"), payload));
    }
}
