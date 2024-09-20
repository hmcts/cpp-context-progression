package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.progression.courts.RelatedHearingUpdated;
import uk.gov.justice.progression.courts.UpdateRelatedHearingCommand;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RelatedHearingCommandHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(RelatedHearingUpdated.class);

    @InjectMocks
    private RelatedHearingCommandHandler relatedHearingCommandHandler;

    @Test
    public void shouldHandleCommand() {
        assertThat(new RelatedHearingCommandHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleUpdateRelatedHearingCommand")
                        .thatHandles("progression.command.update-related-hearing")
                ));
    }

    @Test
    public void shouldProcessCommandForProsecutionCase() throws Exception {

        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID offenceId = randomUUID();

        final UpdateRelatedHearingCommand command = createUpdateRelatedHearingCommand(hearingId, seedingHearingId, prosecutionCaseId, offenceId);

        final HearingAggregate hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        hearingAggregate.updateRelatedHearing(command.getHearingRequest(), command.getIsAdjourned(), command.getExtendedHearingFrom(), command.getIsPartiallyAllocated(), command.getSeedingHearing(), command.getShadowListedOffences());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-related-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateRelatedHearingCommand> envelope = envelopeFrom(metadata, command);
        relatedHearingCommandHandler.handleUpdateRelatedHearingCommand(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.related-hearing-updated"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                        withJsonPath("$.hearingRequest", notNullValue()),
                                        withJsonPath("$.hearingRequest.id", is(hearingId.toString())),
                                        withJsonPath("$.hearingRequest.prosecutionCases", notNullValue()),
                                        withJsonPath("$.hearingRequest.prosecutionCases[0].id", is(prosecutionCaseId.toString())),
                                        withJsonPath("$.seedingHearing", notNullValue()),
                                        withJsonPath("$.seedingHearing.seedingHearingId", is(seedingHearingId.toString())),
                                        withJsonPath("$.shadowListedOffences", notNullValue()),
                                        withJsonPath("$.shadowListedOffences[0]", is(offenceId.toString())))
                                ))
                )
        );
    }

    private static UpdateRelatedHearingCommand createUpdateRelatedHearingCommand(final UUID hearingId, final UUID seedingHearingId, final UUID prosecutionCaseId, final UUID offenceId) {

        return UpdateRelatedHearingCommand.updateRelatedHearingCommand()
                .withExtendedHearingFrom(randomUUID())
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSeedingHearingId(seedingHearingId)
                        .build())
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withId(hearingId)
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(prosecutionCaseId)
                                .build()))
                        .build())
                .withShadowListedOffences(Arrays.asList(offenceId))
                .build();

    }
}

