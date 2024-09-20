package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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

import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CreateHearingApplicationLink;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationLinkCreated;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUpdatedForAllocationFields;
import uk.gov.justice.core.courts.UpdateHearingForAllocationFields;
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
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
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
public class CreateHearingApplicationLinkHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CourtApplicationCreated.class, HearingApplicationLinkCreated.class,
            HearingUpdatedForAllocationFields.class);

    @InjectMocks
    private CreateHearingApplicationLinkHandler createHearingApplicationLinkHandler;

    private HearingAggregate hearingAggregate;
    private ApplicationAggregate aggregate;

    private static final String PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK
            = "progression.command.create-hearing-application-link";

    @BeforeEach
    public void setup() {
        aggregate = new ApplicationAggregate();
        hearingAggregate = new HearingAggregate();
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new CreateHearingApplicationLinkHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles(PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK)
                ));
    }

    @Test
    public void shouldhandleForAllocationFieldsCommand() {
        assertThat(new CreateHearingApplicationLinkHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleForAllocationFields")
                        .thatHandles("progression.command.update-hearing-for-allocation-fields")
                ));
    }

    @Test
    public void shouldCreateHearingApplicationLink() throws EventStreamException {
        final UUID applicationId = randomUUID();
        final CreateHearingApplicationLink createHearingApplicationLink = CreateHearingApplicationLink.createHearingApplicationLink()
                .withApplicationId(applicationId)
                .withHearing(Hearing.hearing().build())
                .withHearingListingStatus(HearingListingStatus.HEARING_INITIALISED)
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK)
                .withId(randomUUID())
                .build();

        final Envelope<CreateHearingApplicationLink> envelope = envelopeFrom(metadata, createHearingApplicationLink);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
        createHearingApplicationLinkHandler.handle(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-application-link-created"),
                        JsonEnvelopePayloadMatcher.payload().isJson(anyOf(
                                withJsonPath("$.applicationId", is(applicationId.toString()))
                                )
                        ))

                )
        );
    }

    @Test
    public void shouldHandleForAllocationFields() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();
        hearingAggregate.apply(HearingForApplicationCreated.hearingForApplicationCreated()
                .withHearing(hearing)
                .withHearingListingStatus(HearingListingStatus.SENT_FOR_LISTING)
                .build());

        final UUID id = randomUUID();
        final UpdateHearingForAllocationFields updateHearingForAllocationFields = UpdateHearingForAllocationFields.updateHearingForAllocationFields()
                .withId(randomUUID())
                .withCourtCentre(CourtCentre.courtCentre().build())
                .withType(HearingType.hearingType()
                        .withId(id)
                        .build())
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-hearing-for-allocation-fields")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateHearingForAllocationFields> envelope = envelopeFrom(metadata, updateHearingForAllocationFields);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        createHearingApplicationLinkHandler.handleForAllocationFields(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-updated-for-allocation-fields"),
                        JsonEnvelopePayloadMatcher.payload().isJson(anyOf(
                                withJsonPath("$.type.id", is(id.toString()))
                                )
                        ))

                )
        );
    }
}
