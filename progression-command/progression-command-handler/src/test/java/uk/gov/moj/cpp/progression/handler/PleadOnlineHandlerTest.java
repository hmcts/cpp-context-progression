package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
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
import uk.gov.moj.cpp.progression.events.OnlinePleaPcqVisitedRecorded;
import uk.gov.moj.cpp.progression.events.OnlinePleaRecorded;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnlinePcqVisited;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleadOnlineHandlerTest {

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(OnlinePleaRecorded.class,
            NotificationSentForPleaDocument.class,
            OnlinePleaPcqVisitedRecorded.class);

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

    @BeforeEach
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

    @Test
    public void shouldHandlePleadOnlinePcqVisitedRequest() throws EventStreamException {
        final PleadOnlinePcqVisited pleadOnlinePcqVisited = PleadOnlinePcqVisited.pleadOnlinePcqVisited()
                .withCaseId(randomUUID())
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.update-hearing-defence-counsel")
                .withId(randomUUID())
                .build();
        final Envelope<PleadOnlinePcqVisited> envelope = envelopeFrom(metadata, pleadOnlinePcqVisited);

        when(caseAggregate.createOnlinePleaPcqVisited(any())).thenReturn(Stream.of(
                OnlinePleaPcqVisitedRecorded.onlinePleaPcqVisitedRecorded()
                        .withCaseId(pleadOnlinePcqVisited.getCaseId())
                        .withPleadOnlinePcqVisited(pleadOnlinePcqVisited)
                        .build()
        ));

        pleadOnlineHandler.handlePleadOnlinePcqVisitedRequest(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());
        final JsonEnvelope  resultEnvelope = (JsonEnvelope)envelopes.stream().filter(
                env -> env.metadata().name().equals("progression.event.online-plea-pcq-visited-recorded")).findFirst().get();

        MatcherAssert.assertThat(resultEnvelope.payloadAsJsonObject()
                , notNullValue());
    }
}