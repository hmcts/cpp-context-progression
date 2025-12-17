package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.core.courts.DefendantsToRemove;
import uk.gov.justice.core.courts.HearingUpdatedForPartialAllocation;
import uk.gov.justice.core.courts.OffencesToRemove;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingUpdatedForPartialAllocationEventProcessorTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private HearingUpdatedForPartialAllocationEventProcessor hearingUpdatedForPartialAllocationEventProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @Test
    public void shouldRaisePublicEvent(){

        HearingUpdatedForPartialAllocation hearingUpdatedForPartialAllocation = HearingUpdatedForPartialAllocation.hearingUpdatedForPartialAllocation()
                .withHearingId(UUID.randomUUID())
                .withProsecutionCasesToRemove(Collections.singletonList(ProsecutionCasesToRemove.prosecutionCasesToRemove()
                        .withCaseId(UUID.randomUUID())
                        .withDefendantsToRemove(Collections.singletonList(DefendantsToRemove.defendantsToRemove()
                                .withDefendantId(UUID.randomUUID())
                                .withOffencesToRemove(Arrays.asList(OffencesToRemove.offencesToRemove()
                                        .withOffenceId(UUID.randomUUID())
                                        .build(), OffencesToRemove.offencesToRemove()
                                        .withOffenceId(UUID.randomUUID())
                                        .build()))
                                .build()))
                        .build()))
                .build();

        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("progression.event.hearing-updated-for-partial-allocation")
                .withId(randomUUID())
                .build();
        final Envelope<HearingUpdatedForPartialAllocation> event = Envelope.envelopeFrom(eventEnvelopeMetadata, hearingUpdatedForPartialAllocation);

        hearingUpdatedForPartialAllocationEventProcessor.handle(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());
        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is("public.progression.offences-removed-from-existing-allocated-hearing"));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingUpdatedForPartialAllocation.getHearingId().toString())),
                withJsonPath("$.offenceIds[0]", equalTo(hearingUpdatedForPartialAllocation.getProsecutionCasesToRemove().get(0).getDefendantsToRemove().get(0).getOffencesToRemove().get(0).getOffenceId().toString())),
                withJsonPath("$.offenceIds[1]", equalTo(hearingUpdatedForPartialAllocation.getProsecutionCasesToRemove().get(0).getDefendantsToRemove().get(0).getOffencesToRemove().get(1).getOffenceId().toString()))
        )));
    }


}
