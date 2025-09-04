package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
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

import uk.gov.justice.progression.courts.CaseStatusUpdatedBdf;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.UpdateCaseStatusBdf;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseStatusHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CaseStatusUpdatedBdf.class);

    @InjectMocks
    private CaseStatusHandler caseStatusHandler;

    private final UUID CASE_ID = randomUUID();

    @Test
    void shouldHandleCommand() {
        assertThat(new CaseStatusHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleUpdateCaseStatusBdf")
                        .thatHandles("progression.command.update-case-status-bdf")
                ));
    }

    @Test
    void shouldProcessCommandCasInactiveWithProsecutionCaseId() throws Exception {

        final CaseAggregate caseAggregate = new CaseAggregate();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        UpdateCaseStatusBdf inactive = UpdateCaseStatusBdf.updateCaseStatusBdf()
                .withProsecutionCaseId(CASE_ID)
                .withCaseStatus(CaseStatusEnum.INACTIVE.name())
                .withNotes("Technical error")
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-case-status-bdf")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateCaseStatusBdf> envelope = envelopeFrom(metadata, inactive);

        caseStatusHandler.handleUpdateCaseStatusBdf(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.case-status-updated-bdf"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.prosecutionCaseId", is(CASE_ID.toString())),
                                withJsonPath("$.caseStatus", is(CaseStatusEnum.INACTIVE.name())),
                                withJsonPath("$.notes", is("Technical error")))))));
    }
}
