package uk.gov.moj.cpp.progression.processor;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.progression.courts.HearingMarkedAsDuplicate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

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
public class HearingMarkedAsDuplicateEventProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private ProgressionService progressionService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @InjectMocks
    private HearingMarkedAsDuplicateEventProcessor hearingMarkedAsDuplicateEventProcessor;

    @Test
    public void shouldHandleHearingMarkedAsDuplicate() {
        final String hearingId = randomUUID().toString();
        final String case1Id = randomUUID().toString();
        final String case2Id = randomUUID().toString();
        final String defendant1Id = randomUUID().toString();
        final String defendant2Id = randomUUID().toString();
        final String offence1Id = randomUUID().toString();
        final String offence2Id = randomUUID().toString();
        final JsonObject hearingMarkedAsDuplicate = createObjectBuilder()
                .add("hearingId", hearingId)
                .add("prosecutionCaseIds", Json.createArrayBuilder()
                        .add(case1Id)
                        .add(case2Id)
                        .build())
                .add("defendantIds", Json.createArrayBuilder()
                        .add(defendant1Id)
                        .add(defendant2Id)
                        .build())
                .add("offenceIds", Json.createArrayBuilder()
                        .add(offence1Id)
                        .add(offence2Id)
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.events.hearing.marked-as-duplicate"),
                hearingMarkedAsDuplicate);

        hearingMarkedAsDuplicateEventProcessor.handleHearingMarkedAsDuplicatePublicEvent(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is("progression.command.mark-hearing-as-duplicate"));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId)),
                withJsonPath("$.prosecutionCaseIds[0]", equalTo(case1Id)),
                withJsonPath("$.prosecutionCaseIds[1]", equalTo(case2Id)))));

        verify(progressionService).populateHearingToProbationCaseworker(eq(event), eq(fromString(hearingId)));
    }

    @Test
    public void shouldHandleHearingMarkedAsDuplicatePrivateEvent() {
        final UUID hearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final JsonObject hearingMarkedAsDuplicate = createObjectBuilder()
                .add("hearingIdToBeDeleted", hearingId.toString())
                .add("caseIds", Json.createArrayBuilder()
                        .add(case1Id.toString())
                        .add(case2Id.toString())
                        .build())
                .build();


        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.events.hearing.marked-as-duplicate"),
                hearingMarkedAsDuplicate);

        when(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingMarkedAsDuplicate.class))
                .thenReturn(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                        .withHearingId(hearingId)
                        .withCaseIds(Arrays.asList(case1Id, case2Id))
                        .build());

        hearingMarkedAsDuplicateEventProcessor.handleHearingMarkedAsDuplicatePrivateEvent(event);

        verify(this.sender, times(2)).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope firstCommandEvent = this.senderJsonEnvelopeCaptor.getAllValues().get(0);
        final JsonEnvelope secondCommandEvent = this.senderJsonEnvelopeCaptor.getAllValues().get(1);

        assertThat(firstCommandEvent.metadata().name(), is("progression.command.mark-hearing-as-duplicate-for-case"));
        assertThat(firstCommandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.caseId", equalTo(case1Id.toString())))));

        assertThat(secondCommandEvent.metadata().name(), is("progression.command.mark-hearing-as-duplicate-for-case"));
        assertThat(secondCommandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.caseId", equalTo(case2Id.toString())))));
    }
}
