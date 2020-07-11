package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.ConfirmedProsecutionCaseId;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.PrepareSummonsData;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
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
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PrepareSummonsDataHandlerTest {

    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID COURT_CENTRE_ID = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            SummonsDataPrepared.class);

    @InjectMocks
    private PrepareSummonsDataHandler handler;

    private HearingAggregate hearingAggregate;

    private UtcClock clock = new UtcClock();

    @Before
    public void setup() {
        hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        ReflectionUtil.setField(hearingAggregate, "listDefendantRequests", Arrays.asList(ListDefendantRequest.listDefendantRequest()
                .withDefendantId(DEFENDANT_ID)
                .build()));
    }

    @Test
    public void shouldPrepareSummonsData() throws EventStreamException {
        final ZonedDateTime hearingDate = clock.now();
        final PrepareSummonsData payload = PrepareSummonsData.prepareSummonsData()
                .withConfirmedProsecutionCaseIds(Arrays.asList(ConfirmedProsecutionCaseId.confirmedProsecutionCaseId()
                        .withConfirmedDefendantIds(Arrays.asList(DEFENDANT_ID))
                        .withId(CASE_ID)
                        .build()))
                .withHearingId(HEARING_ID)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withCode("courtCentreCode")
                        .withId(COURT_CENTRE_ID)
                        .build())
                .withHearingDateTime(hearingDate)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.prepare-summons-data")
                .withId(randomUUID())
                .build();

        final Envelope<PrepareSummonsData> envelope = envelopeFrom(metadata, payload);

        handler.prepareSummonsData(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.summons-data-prepared"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.summonsData.confirmedProsecutionCaseIds[0].id", is(CASE_ID.toString())),
                                withJsonPath("$.summonsData.confirmedProsecutionCaseIds[0].confirmedDefendantIds[0]", is(DEFENDANT_ID.toString())),
                                withJsonPath("$.summonsData.listDefendantRequests[0].defendantId", is(DEFENDANT_ID.toString())),
                                withJsonPath("$.summonsData.courtCentre.code", is("courtCentreCode")),
                                withJsonPath("$.summonsData.courtCentre.id", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.summonsData.hearingDateTime", is(ZonedDateTimes.toString(hearingDate)))
                                )
                        ))

                )
        );
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new PrepareSummonsDataHandler(), isHandler(COMMAND_HANDLER)
                .with(method("prepareSummonsData")
                        .thatHandles("progression.command.prepare-summons-data")
                ));
    }
}