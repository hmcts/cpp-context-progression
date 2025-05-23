package uk.gov.moj.cpp.progression.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.*;
import uk.gov.justice.core.progression.courts.HearingForApplicationCreated;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

@ExtendWith(MockitoExtension.class)
public class CreateNextHearingCommandHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            NextHearingsRequested.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private HearingAggregate hearingAggregate;

    @InjectMocks
    @Spy
    private CreateNextHearingCommandHandler createNextHearingCommandHandler;

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }

    @Test
    public void shouldCreateNextHearing() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.create-next-hearing")
                .withId(randomUUID())
                .build();
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();

        final CreateNextHearing createNextHearing = CreateNextHearing.createNextHearing()
                .withHearing(hearing)
                .withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(randomUUID()).build())
                .withCommittingCourt(CommittingCourt.committingCourt()
                        .withCourtCentreId(randomUUID())
                        .withCourtHouseCode("B10JQ")
                        .withCourtHouseType(JurisdictionType.MAGISTRATES)
                        .build())
                .build();


        final Envelope<CreateNextHearing> envelope = envelopeFrom(metadata, createNextHearing);

        when(hearingAggregate.processCreateNextHearing(createNextHearing))
                .thenReturn(Stream.of(NextHearingsRequested.nextHearingsRequested()
                                .withHearing(createNextHearing.getHearing())
                                .withCommittingCourt(createNextHearing.getCommittingCourt())
                                .withSeedingHearing(createNextHearing.getSeedingHearing())
                                .withShadowListedOffences(createNextHearing.getShadowListedOffences())
                                .build()));


        createNextHearingCommandHandler.processCreateNextHearing(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.next-hearings-requested"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing.id", is(hearingId.toString())))
                        ))
                ));
    }




}
