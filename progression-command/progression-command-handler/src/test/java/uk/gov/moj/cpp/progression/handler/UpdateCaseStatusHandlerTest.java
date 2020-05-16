package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.HearingConfirmedCaseStatusUpdated;
import uk.gov.justice.core.courts.HearingConfirmedUpdateCaseStatus;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.helper.FileResourceObjectMapper;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateCaseStatusHandlerTest {
    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingConfirmedCaseStatusUpdated.class);


    @InjectMocks
    private UpdateCaseStatusHandler updateCaseStatusHandler;

    private CaseAggregate aggregate;

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    private static final UUID USER_ID = UUID.randomUUID();

    @Before
    public void setup() {
        aggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleUpdateCaseStatusCommand() {
        assertThat(new UpdateCaseStatusHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleUpdateCaseStatus")
                        .thatHandles("progression.command.hearing-confirmed-update-case-status")
                ));
    }

    @Test
    public void shouldUpdateCaseStatus() throws Exception {
        final HearingConfirmedUpdateCaseStatus hearingConfirmedUpdateCaseStatus = handlerTestHelper.convertFromFile("json/hearing-confirmed-update-case-status.json", HearingConfirmedUpdateCaseStatus.class);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-confirmed-update-case-status")
                .withId(randomUUID())
                .build();

        final Envelope<HearingConfirmedUpdateCaseStatus> envelope = envelopeFrom(metadata, hearingConfirmedUpdateCaseStatus);

        updateCaseStatusHandler.handleUpdateCaseStatus(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.hearing-confirmed-case-status-updated"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.caseStatus", is("sjp_referral"))
                                        )
                                ))
                )));
    }
}