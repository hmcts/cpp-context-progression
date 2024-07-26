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

import uk.gov.justice.core.courts.CourtFeeForCivilApplicationUpdated;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.command.CourtApplicationPayment;
import uk.gov.moj.cpp.progression.command.EditCourtFeeForCivilApplication;

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
public class EditCourtFeeForCivilApplicationHandlerTest {

    private static final UUID APPLICATION_ID = UUID.randomUUID();

    private static final String PROGRESSION_COMMAND_EVENT_CIVIL_APPLICATION_FEE = "progression.event.court-fee-for-civil-application-updated";

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
    private ApplicationAggregate applicationAggregate;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private EditCourtFeeForCivilApplicationHandler editCourtFeeForCivilApplicationHandler;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CourtFeeForCivilApplicationUpdated.class);

    @Before
    public void setup() {
        applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(events.map(function)).thenReturn(jsonEvents);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(editCourtFeeForCivilApplicationHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleCivilFee")
                        .thatHandles("progression.command.edit-court-fee-for-civil-application")));
    }

    @Test
    public void shouldProcessEditCivilApplicationFees() throws Exception {

        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        EditCourtFeeForCivilApplication editCourtFeeForCivilApplication = EditCourtFeeForCivilApplication.editCourtFeeForCivilApplication()
                .withApplicationId(APPLICATION_ID)
                .withCourtApplicationPayment(CourtApplicationPayment.courtApplicationPayment()
                        .withFeeStatus(FeeStatus.OUTSTANDING)
                        .withPaymentReference("TestReference001")
                        .build())
                .build();

        editCourtFeeForCivilApplicationHandler.handleCivilFee(Envelope.envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_COMMAND_EVENT_CIVIL_APPLICATION_FEE),editCourtFeeForCivilApplication));

        verifyResults();
    }

    private void verifyResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        Assert.assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(PROGRESSION_COMMAND_EVENT_CIVIL_APPLICATION_FEE),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.applicationId", is(APPLICATION_ID.toString()))
                                )
                        ))

                )
        );
    }
}
