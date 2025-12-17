package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import uk.gov.justice.core.courts.AuditCourtDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentAudit;
import uk.gov.justice.core.courts.Material;
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
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuditCourtDocumentHandlerTest {


    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CourtDocumentAudit.class);


    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private AuditCourtDocumentHandler auditCourtDocumentHandler;

    @Test
    public void shouldHandleCommand() {
        assertThat(new AuditCourtDocumentHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.audit-court-document")
                ));
    }


    @Test
    public void shouldThrowExceptionCommandWithOutUserId() throws Exception {

        AuditCourtDocument auditCourtDocument = AuditCourtDocument.auditCourtDocument().withAction("View").build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.audit-court-document")
                .withId(randomUUID())
                .build();

        final Envelope<AuditCourtDocument> envelope = envelopeFrom(metadata, auditCourtDocument);

        assertThrows(IllegalArgumentException.class, () -> auditCourtDocumentHandler.handle(envelope));
    }


    @Test()
    public void shouldProcessCommand() throws Exception {

        final UUID courtDocumentId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID userId = randomUUID();

        final CourtDocumentAggregate courtDocumentAggregate = new CourtDocumentAggregate();
        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtDocumentAggregate.class)).thenReturn(courtDocumentAggregate);


        AuditCourtDocument auditCourtDocument = AuditCourtDocument.auditCourtDocument().withAction("View")
                .withCourtDocumentId(courtDocumentId)
                .withMaterialId(materialId)
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.audit-court-document")
                .withId(randomUUID())
                .withUserId(userId.toString())
                .build();

        final Envelope<AuditCourtDocument> envelope = envelopeFrom(metadata, auditCourtDocument);

        Material material = Material.material().withId(materialId).withName("Test").build();
        CourtDocument courtDocument = CourtDocument.courtDocument().withCourtDocumentId(courtDocumentId).withMaterials(asList(material)).build();
        courtDocumentAggregate.apply(courtDocumentAggregate.addCourtDocument(courtDocument));
        auditCourtDocumentHandler.handle(envelope);


        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.court-document-audit"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                                withJsonPath("$.action", is("View")),
                                                withJsonPath("$.materialId", is(materialId.toString())),
                                                withJsonPath("$.userId", is(userId.toString())),
                                                withJsonPath("$.courtDocumentId", is(courtDocumentId.toString())),
                                                withJsonPath("$.materialName", is("Test"))
                                        )
                                ))
                )
        );
    }
}