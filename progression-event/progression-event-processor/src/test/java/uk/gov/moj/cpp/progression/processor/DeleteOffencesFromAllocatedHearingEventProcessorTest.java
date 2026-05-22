package uk.gov.moj.cpp.progression.processor;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class DeleteOffencesFromAllocatedHearingEventProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @InjectMocks
    private DeleteOffencesFromAllocatedHearingEventProcessor deleteOffencesFromAllocatedHearingEventProcessor;


    @Test
    public void shouldHandleOffencesRemovedFromExistingAllocatedHearing() {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final JsonObject offenceRemovedFromExistingUnallocatedHearing = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("offenceIds", JsonObjects.createArrayBuilder()
                        .add(offenceId1.toString())
                        .add(offenceId2.toString())
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.events.listing.offences-removed-from-existing-allocated-hearing"),
                offenceRemovedFromExistingUnallocatedHearing);


        deleteOffencesFromAllocatedHearingEventProcessor.handleOffencesRemovedFromExistingAllocatedHearingPublicEvent(event);

        verify(this.sender, times(1)).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEventResponse = this.senderJsonEnvelopeCaptor.getAllValues().get(0);

        assertThat(commandEventResponse.metadata().name(), is("progression.command.remove-offences-from-existing-hearing"));
        assertThat(commandEventResponse.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.offenceIds[0:2]", containsInAnyOrder(offenceId1.toString(), offenceId2.toString())))));
    }
}
