package uk.gov.moj.cpp.progression.handler;

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

import uk.gov.justice.core.courts.CreateHearingForApplicationV2;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateHearingForApplicationHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingForApplicationCreated.class);

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
    private CreateHearingForApplicationHandler createHearingForApplicationHandler;

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }


    @Test
    public void shouldCreateHearingForApplication() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.create-hearing-for-application")
                .withId(randomUUID())
                .build();
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();


        final Envelope<CreateHearingForApplicationV2> envelope = envelopeFrom(metadata, CreateHearingForApplicationV2.createHearingForApplicationV2()
                .withHearing(hearing)
                .withHearingListingStatus(HearingListingStatus.SENT_FOR_LISTING)
                .build());

        when(hearingAggregate.createHearingForApplication(hearing,HearingListingStatus.SENT_FOR_LISTING, null))
                .thenReturn(Stream.of(HearingForApplicationCreated.hearingForApplicationCreated()
                        .withHearing(hearing)
                        .withHearingListingStatus(HearingListingStatus.SENT_FOR_LISTING)
                        .build()));


        createHearingForApplicationHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-for-application-created"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing.id", is(hearingId.toString())),
                                withJsonPath("$.hearingListingStatus", is(HearingListingStatus.SENT_FOR_LISTING.toString()))
                        ))
                ))
        );
    }



}
