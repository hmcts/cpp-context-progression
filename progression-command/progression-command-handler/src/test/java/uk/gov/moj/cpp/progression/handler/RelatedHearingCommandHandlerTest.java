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


import java.util.ArrayList;
import java.util.Collections;

import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingUpdatedProcessed;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.progression.courts.HearingForApplicationCreated;
import uk.gov.justice.progression.courts.HearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.RelatedHearingUpdated;
import uk.gov.justice.progression.courts.RelatedHearingUpdatedForAdhocHearing;
import uk.gov.justice.progression.courts.UpdateRelatedHearingCommand;
import uk.gov.justice.core.courts.UpdateRelatedHearingCommandForAdhocHearing;
import uk.gov.justice.progression.courts.VejHearingPopulatedToProbationCaseworker;
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
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(RelatedHearingUpdated.class, ProsecutionCaseDefendantListingStatusChangedV2.class,
            RelatedHearingUpdatedForAdhocHearing.class, VejHearingPopulatedToProbationCaseworker.class, HearingPopulatedToProbationCaseworker.class);

    @InjectMocks
    private RelatedHearingCommandHandler relatedHearingCommandHandler;

    private HearingAggregate hearingAggregate;

    @BeforeEach
    public void setup() {
        hearingAggregate = new HearingAggregate();
    }

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
        hearingAggregate.apply(HearingForApplicationCreated.hearingForApplicationCreated().withHearingListingStatus(HearingListingStatus.HEARING_RESULTED).build());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-related-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateRelatedHearingCommand> envelope = envelopeFrom(metadata, command);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
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

    @Test
    public void shouldProcessAdhocHearingsCommandForProsecutionCase() throws Exception {

        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID oldProsecutionCaseId = randomUUID();
        final UUID offenceId = randomUUID();

        hearingAggregate.apply(HearingUpdatedProcessed.hearingUpdatedProcessed()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Collections.singletonList(ProsecutionCase.prosecutionCase()
                                .withId(oldProsecutionCaseId)
                                .withDefendants(Collections.singletonList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Collections.singletonList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .withDefenceCounsels(Collections.singletonList(DefenceCounsel.defenceCounsel()
                                .withId(randomUUID())
                                .build()))
                        .build()).build());
        final UpdateRelatedHearingCommandForAdhocHearing command = createUpdateRelatedHearingCommandForAdhocHearing(hearingId, prosecutionCaseId, offenceId);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-related-hearing-for-adhoc-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateRelatedHearingCommandForAdhocHearing> envelope = envelopeFrom(metadata, command);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        relatedHearingCommandHandler.handleUpdateRelatedHearingCommandForAdhocHearing(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecutionCase-defendant-listing-status-changed-v2"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing.id", is(hearingId.toString())),
                                withJsonPath("$.hearing.prosecutionCases.length()", is(2)),
                                withJsonPath("$.hearing.defenceCounsels", notNullValue())
                        ))),
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.related-hearing-updated-for-adhoc-hearing"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingRequest", notNullValue()),
                                withJsonPath("$.hearingRequest.id", is(hearingId.toString())),
                                withJsonPath("$.hearingRequest.prosecutionCases", notNullValue()),
                                withJsonPath("$.hearingRequest.prosecutionCases.length()", is(2)),
                                withJsonPath("$.sendNotificationToParties", is(true)))
                        )),
                jsonEnvelope(
                        metadata()
                                .withName("progression.events.hearing-populated-to-probation-caseworker"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing.id", is(hearingId.toString())),
                                withJsonPath("$.hearing.prosecutionCases", notNullValue()),
                                withJsonPath("$.hearing.prosecutionCases.length()", is(2)),
                                withJsonPath("$.hearing.defenceCounsels", notNullValue())
                        ))),
                jsonEnvelope(
                        metadata()
                                .withName("progression.events.vej-hearing-populated-to-probation-caseworker"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing.id", is(hearingId.toString())),
                                withJsonPath("$.hearing.prosecutionCases", notNullValue()),
                                withJsonPath("$.hearing.prosecutionCases.length()", is(2)),
                                withJsonPath("$.hearing.defenceCounsels", notNullValue())
                        )))
        ));
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

    private static UpdateRelatedHearingCommandForAdhocHearing createUpdateRelatedHearingCommandForAdhocHearing(final UUID hearingId, final UUID prosecutionCaseId, final UUID offenceId) {

        final ArrayList cases = new ArrayList();
        cases.add(ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(Collections.singletonList(Defendant.defendant()
                        .withId(randomUUID())
                        .withOffences(Collections.singletonList(Offence.offence()
                                .withId(randomUUID())
                                .build()))
                        .build()))
                .build());
        return UpdateRelatedHearingCommandForAdhocHearing.updateRelatedHearingCommandForAdhocHearing()
                .withExtendedHearingFrom(randomUUID())
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withId(hearingId)
                        .withProsecutionCases(cases)
                        .build())
                .withShadowListedOffences(Arrays.asList(offenceId))
                .withSendNotificationToParties(true)
                .build();

    }
}

