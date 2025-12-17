package uk.gov.moj.cpp.progression.processor;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.progression.courts.HearingDeleted;
import uk.gov.justice.progression.courts.OffencesRemovedFromHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Arrays;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class HearingDeletedEventProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @InjectMocks
    private HearingDeletedEventProcessor hearingDeletedEventProcessor;

    @Test
    public void shouldIssueDeleteHearingCommandWhenHandlingAllocatedHearingDeleted() {
        final String hearingId = randomUUID().toString();
        final JsonObject hearingDeleted = createObjectBuilder()
                .add("hearingId", hearingId)
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.events.listing.allocated-hearing-deleted"),
                hearingDeleted);

        hearingDeletedEventProcessor.handleAllocatedHearingDeletedPublicEvent(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is("progression.command.delete-hearing"));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId))
                )));
    }

    @Test
    public void shouldIssueDeleteHearingCommandWhenHandlingUnallocatedHearingDeleted() {
        final String hearingId = randomUUID().toString();
        final JsonObject hearingDeleted = createObjectBuilder()
                .add("hearingId", hearingId)
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.events.listing.unallocated-hearing-deleted"),
                hearingDeleted);

        hearingDeletedEventProcessor.handleUnallocatedHearingDeletedPublicEvent(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is("progression.command.delete-hearing"));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId))
        )));
    }

    @Test
    public void shouldIssueDeleteHearingForProsecutionCaseCommand() {
        final UUID hearingId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID prosecutionCaseId1 = randomUUID();
        final UUID prosecutionCaseId2 = randomUUID();
        final JsonObject hearingDeletedForProsecutionCase = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("courtApplicationIds", Json.createArrayBuilder()
                        .add(courtApplicationId.toString())
                        .build())
                .add("prosecutionCaseIds", Json.createArrayBuilder()
                        .add(prosecutionCaseId1.toString())
                        .add(prosecutionCaseId2.toString())
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.event.hearing.deleted"),
                hearingDeletedForProsecutionCase);

        when(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingDeleted.class))
                .thenReturn(HearingDeleted.hearingDeleted()
                        .withHearingId(hearingId)
                        .withCourtApplicationIds(Arrays.asList(courtApplicationId))
                        .withProsecutionCaseIds(Arrays.asList(prosecutionCaseId1, prosecutionCaseId2))
                        .build());

        hearingDeletedEventProcessor.handleHearingDeletedPrivateEvent(event);

        verify(this.sender, times(3)).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope firstCommandEvent = this.senderJsonEnvelopeCaptor.getAllValues().get(0);
        final JsonEnvelope secondCommandEvent = this.senderJsonEnvelopeCaptor.getAllValues().get(1);
        final JsonEnvelope thirdCommandEvent = this.senderJsonEnvelopeCaptor.getAllValues().get(2);

        assertThat(firstCommandEvent.metadata().name(), is("progression.command.delete-hearing-for-prosecution-case"));
        assertThat(firstCommandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.prosecutionCaseId", equalTo(prosecutionCaseId1.toString())))));

        assertThat(secondCommandEvent.metadata().name(), is("progression.command.delete-hearing-for-prosecution-case"));
        assertThat(secondCommandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.prosecutionCaseId", equalTo(prosecutionCaseId2.toString())))));

        assertThat(thirdCommandEvent.metadata().name(), is("progression.command.delete-hearing-for-court-application"));
        assertThat(thirdCommandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.courtApplicationId", equalTo(courtApplicationId.toString())))));
    }

    @Test
    public void shouldCallDeleteCaseCommandFroDeletedCases(){
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId1 = randomUUID();
        final UUID prosecutionCaseId2 = randomUUID();

        final JsonObject offencesRemovedFromHearing = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("prosecutionCaseIds", Json.createArrayBuilder()
                        .add(prosecutionCaseId1.toString())
                        .add(prosecutionCaseId2.toString())
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.events.offences-removed-from-hearing"),
                offencesRemovedFromHearing);


        when(jsonObjectConverter.convert(event.payloadAsJsonObject(), OffencesRemovedFromHearing.class))
                .thenReturn(OffencesRemovedFromHearing.offencesRemovedFromHearing()
                        .withHearingId(hearingId)
                        .withProsecutionCaseIds(Arrays.asList(prosecutionCaseId1, prosecutionCaseId2))
                        .build());

        hearingDeletedEventProcessor.handleOffenceInHearingRemoved(event);

        verify(this.sender, times(2)).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope firstCommandEvent = this.senderJsonEnvelopeCaptor.getAllValues().get(0);
        final JsonEnvelope secondCommandEvent = this.senderJsonEnvelopeCaptor.getAllValues().get(1);

        assertThat(firstCommandEvent.metadata().name(), is("progression.command.delete-hearing-for-prosecution-case"));
        assertThat(firstCommandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.prosecutionCaseId", equalTo(prosecutionCaseId1.toString())))));

        assertThat(secondCommandEvent.metadata().name(), is("progression.command.delete-hearing-for-prosecution-case"));
        assertThat(secondCommandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.prosecutionCaseId", equalTo(prosecutionCaseId2.toString())))));

    }
}
