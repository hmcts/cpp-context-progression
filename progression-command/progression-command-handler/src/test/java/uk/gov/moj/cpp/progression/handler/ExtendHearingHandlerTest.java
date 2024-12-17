package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
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

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.ExtendHearing;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingExtendedProcessed;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ProcessHearingExtended;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExtendHearingHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingExtended.class,
            HearingExtendedProcessed.class
    );

    @InjectMocks
    private ExtendHearingHandler extendHearingHandler;

    @Test
    public void shouldHandleCommand() {
        assertThat(new ExtendHearingHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.extend-hearing")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        final ExtendHearing extendHearing = createExtendHearingForApplication();
        applicationAggregate.extendHearing(extendHearing.getHearingRequest());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.extend-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<ExtendHearing> envelope = envelopeFrom(metadata, extendHearing);
        extendHearingHandler.handle(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.hearing-extended"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                        withJsonPath("$.hearingRequest", notNullValue()),
                                        withJsonPath("$.hearingRequest.courtApplications", notNullValue()))
                                ))

                )
        );
    }

    @Test
    public void shouldProcessCommandForProsecutionCase() throws Exception {

        final CaseAggregate caseAggregate = new CaseAggregate();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        final ExtendHearing extendHearing = createExtendHearingForProsecutionCase();
        caseAggregate.extendHearing(extendHearing.getHearingRequest(), extendHearing);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.extend-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<ExtendHearing> envelope = envelopeFrom(metadata, extendHearing);
        extendHearingHandler.handle(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.hearing-extended"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                        withJsonPath("$.hearingRequest", notNullValue()),
                                        withJsonPath("$.hearingRequest.prosecutionCases", notNullValue()))
                                ))
                )
        );
    }

    @Test
    public void shouldHandleProcessHearingExtended() throws Exception {

        final UUID hearingId = randomUUID();
        final ProcessHearingExtended processHearingExtended = ProcessHearingExtended.processHearingExtended()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds().withId(hearingId).build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.process-hearing-extended")
                .withId(randomUUID())
                .build();

        final HearingAggregate hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        hearingAggregate.apply(HearingResulted.hearingResulted().withHearing(Hearing.hearing().withId(hearingId).build()).build());

        final Envelope<ProcessHearingExtended> envelope = envelopeFrom(metadata, processHearingExtended);

        extendHearingHandler.handleProcessHearingExtended(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.hearing-extended-processed"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                        withJsonPath("$.hearingRequest.id", is(hearingId.toString())),
                                        withJsonPath("$.hearing.id", is(hearingId.toString())))
                                ))
                )
        );
    }

    private static ExtendHearing createExtendHearingForApplication() {

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(randomUUID())
                .build();

        final List<CourtApplication> courtApplicationList = new ArrayList<>();
        courtApplicationList.add(courtApplication);

        return ExtendHearing.extendHearing()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withCourtApplications(courtApplicationList)
                        .withId(randomUUID())
                        .build())
                .build();
    }

    private static ExtendHearing createExtendHearingForProsecutionCase() {

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .build();
        final List<ProsecutionCase> prosecutionCaseList = new ArrayList<>();
        prosecutionCaseList.add(prosecutionCase);

        return ExtendHearing.extendHearing()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withProsecutionCases(prosecutionCaseList)
                        .withId(randomUUID())
                        .build())
                .build();
    }
}

