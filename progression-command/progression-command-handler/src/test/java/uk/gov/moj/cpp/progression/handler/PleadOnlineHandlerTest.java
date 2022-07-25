package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.progression.events.OnlinePleaRecorded.onlinePleaRecorded;
import static uk.gov.moj.cpp.progression.plea.json.schemas.PleaNotificationType.COMPANYONLINEPLEA;
import static uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnline.pleadOnline;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.handler.HandleOnlinePleaDocumentCreation;
import uk.gov.moj.cpp.progression.events.NotificationSentForPleaDocument;
import uk.gov.moj.cpp.progression.events.OnlinePleaRecorded;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleaNotificationType;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnline;

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
public class PleadOnlineHandlerTest {

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(OnlinePleaRecorded.class,
            NotificationSentForPleaDocument.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private CaseAggregate caseAggregate;

    @InjectMocks
    private PleadOnlineHandler pleadOnlineHandler;

    @Before
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
    }

    @Test
    public void shouldHandleCreatePetForm() throws EventStreamException {

        final PleadOnline onlinePleaRequest = pleadOnline()
                .withCaseId(randomUUID())
                .withDefendantId(randomUUID())
                .build();

        final Metadata metadata = getMetadata("progression.command.plead-online");

        final Envelope<PleadOnline> envelope = envelopeFrom(metadata, onlinePleaRequest);

        when(caseAggregate.recordOnlinePlea(any())).thenReturn(Stream.of(onlinePleaRecorded()
                .withCaseId(onlinePleaRequest.getCaseId()).withPleadOnline(onlinePleaRequest).build()));

        pleadOnlineHandler.handlePleadOnlineRequest(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.online-plea-recorded"),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", is(onlinePleaRequest.getCaseId().toString())),
                                withJsonPath("$.pleadOnline.defendantId", is(onlinePleaRequest.getDefendantId().toString())))
                        ))
        ));

    }

    private Metadata getMetadata(final String name) {
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(name)
                .withId(randomUUID())
                .build();
        return metadata;
    }

    @Test
    public void shouldHandleNotifyForOnlinePlea() throws EventStreamException {

        final UUID caseId = randomUUID();

        final HandleOnlinePleaDocumentCreation notifyForOnlinePlea = HandleOnlinePleaDocumentCreation.handleOnlinePleaDocumentCreation()
                .withCaseId(randomUUID())
                .withPleaNotificationType(COMPANYONLINEPLEA)
                .withSystemDocGeneratorId(randomUUID())
                .build();

        final Metadata metadata = getMetadata("progression.command.handle-online-plea-document-creation");

        final Envelope<HandleOnlinePleaDocumentCreation> envelope = envelopeFrom(metadata, notifyForOnlinePlea);

        when(caseAggregate.handleOnlinePleaDocumentCreation(any())).thenReturn(Stream.of(
                NotificationSentForPleaDocument.notificationSentForPleaDocument()
                        .withEmail("email@hmcts.net")
                        .withSystemDocGeneratorId(randomUUID())
                        .withCaseId(caseId)
                        .withPleaNotificationType(COMPANYONLINEPLEA)
                        .build()
        ));

        pleadOnlineHandler.handleNotifyForOnlinePlea(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.notification-sent-for-plea-document"),
                        payload().isJson(allOf(
                                        withJsonPath("$.caseId", is(caseId.toString()))
                                )
                        ))
        ));

    }
}