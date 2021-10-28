package uk.gov.moj.cpp.progression.command;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.hearing.courts.HearingResult.hearingResult;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ProsecutionCasesResulted;
import uk.gov.justice.hearing.courts.HearingResult;
import uk.gov.justice.hearing.courts.ProgressionHearingTrialVacated;
import uk.gov.justice.progression.courts.ApplicationsResulted;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.progression.courts.HearingTrialVacated;
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
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.handler.HearingResultHandler;
import uk.gov.moj.cpp.progression.handler.HearingTrialVacatedHandler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingTrialVacatedHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingTrialVacated.class);

    @InjectMocks
    private HearingTrialVacatedHandler handler;


    private HearingAggregate aggregate;

    @Before
    public void setup() {
        aggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new HearingTrialVacatedHandler(), isHandler(COMMAND_HANDLER)
                .with(method("hearingTrialVacated")
                        .thatHandles("progression.command.hearing-trial-vacated")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        final ProgressionHearingTrialVacated hearingTrialVacated = ProgressionHearingTrialVacated.progressionHearingTrialVacated()
                .withHearingId(UUID.randomUUID())
                .withVacatedTrialReasonId(UUID.randomUUID())
                .build();
        aggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                                        .withId(randomUUID())
                                        .withDefendants(asList(Defendant.defendant()
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                                .withId(randomUUID())
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(randomUUID())
                                                                .build())))
                                                .build()))
                                        .build(),
                                ProsecutionCase.prosecutionCase()
                                        .withId(randomUUID())
                                        .withDefendants(asList(Defendant.defendant()
                                                .withOffences(asList(Offence.offence()
                                                        .withId(randomUUID())
                                                        .build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        aggregate.apply(hearingTrialVacated.getHearingId());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-trial-vacated")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<ProgressionHearingTrialVacated> envelope = envelopeFrom(metadata, hearingTrialVacated);

        handler.hearingTrialVacated(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);


        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope  hearingTrialVacatedEvent = (JsonEnvelope)envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-trial-vacated")).findFirst().get();

        assertThat(hearingTrialVacatedEvent.payloadAsJsonObject()
                , notNullValue());
        assertThat(hearingTrialVacatedEvent.metadata().name(), is("progression.event.hearing-trial-vacated"));

    }
}
