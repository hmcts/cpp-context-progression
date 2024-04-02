package uk.gov.moj.cpp.progression.event;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.progression.courts.OffencesRemovedFromHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingSelectedOffenceRemovedEventProcessorTest {
    private static final String PROGRESSION_COMMAND_REMOVE_OFFENCE_FROM_EXISTING_HEARING = "progression.command.remove-offences-from-existing-hearing";
    private static final String PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING = "public.events.listing.offences-removed-from-allocated-hearing";

    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @InjectMocks
    private HearingSelectedOffenceRemovedEventProcessor hearingSelectedOffenceRemovedEventProcessor;

    @Test
    public void shouldHandleHearingTrialVacatedCommand() {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final JsonObject hearingSelectedOffenceRemoved = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("offenceIds", createArrayBuilder().add(offenceId1.toString()).add(offenceId2.toString()))
                .build();

        OffencesRemovedFromHearing offencesRemovedFromHearing = OffencesRemovedFromHearing.offencesRemovedFromHearing().withHearingId(hearingId).withOffenceIds(Stream.of(offenceId1,offenceId2).collect(Collectors.toList())).build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING),
                hearingSelectedOffenceRemoved);
        when(jsonObjectConverter.convert(hearingSelectedOffenceRemoved, OffencesRemovedFromHearing.class)).thenReturn(offencesRemovedFromHearing);

        hearingSelectedOffenceRemovedEventProcessor.handleHearingSelectedOffenceRemovedFromExistingHearingPublicEvent(envelope);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is(PROGRESSION_COMMAND_REMOVE_OFFENCE_FROM_EXISTING_HEARING));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.offenceIds", hasSize(2)))));
    }


}