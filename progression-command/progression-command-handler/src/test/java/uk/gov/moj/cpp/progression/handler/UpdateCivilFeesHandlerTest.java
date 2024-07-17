package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.CivilFeesUpdated;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.UpdateCivilFees;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateCivilFeesHandlerTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    private static final String PROGRESSION_COMMAND_EVENT_CIVIL_FEE = "progression.event.civil-fees-updated";

    @Mock
    private EventSource eventSource;

    @Mock
    protected Stream<Object> events;

    @Mock
    protected Stream<JsonEnvelope> jsonEvents;

    @Mock
    protected Function function;

    @Mock
    private EventStream eventStream;

    @Mock
    private CaseAggregate caseAggregate;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private UpdateCivilFeesHandler updateCivilFeeHandler;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CivilFeesUpdated.class);

    @Before
    public void setup() {
        caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(events.map(function)).thenReturn(jsonEvents);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(updateCivilFeeHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleCivilFee")
                        .thatHandles("progression.command.update-civil-fees")));
    }

    @Test
    public void shouldProcessAddCivilFees() throws Exception {

        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        List<CivilFees> civilFeesList = new ArrayList<CivilFees>();
        civilFeesList.add(CivilFees.civilFees()
                .withFeeId(UUID.randomUUID())
                .withFeeType(FeeType.INITIAL)
                .withFeeStatus(FeeStatus.OUTSTANDING)
                .withPaymentReference("paymentRef001")
                .build());

        UpdateCivilFees updateCivilFees = UpdateCivilFees.updateCivilFees()
                .withCaseId(CASE_ID)
                .withCivilFees(civilFeesList)
                .build();

        updateCivilFeeHandler.handleCivilFee(Envelope.envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_COMMAND_EVENT_CIVIL_FEE),updateCivilFees));

        verifyResults();
    }

    private void verifyResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        Assert.assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(PROGRESSION_COMMAND_EVENT_CIVIL_FEE),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(CASE_ID.toString()))
                                )
                        ))

                )
        );
    }
}
