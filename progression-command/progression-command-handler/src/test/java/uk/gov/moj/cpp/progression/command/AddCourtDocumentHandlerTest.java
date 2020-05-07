package uk.gov.moj.cpp.progression.command;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.AddCourtDocument.addCourtDocument;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.AddCourtDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;
import uk.gov.moj.cpp.progression.handler.AddCourtDocumentHandler;
import uk.gov.moj.cpp.progression.handler.courts.document.CourtDocumentEnricher;
import uk.gov.moj.cpp.progression.handler.courts.document.DefaultCourtDocumentFactory;
import uk.gov.moj.cpp.progression.handler.courts.document.DocumentTypeAccessProvider;
import uk.gov.moj.cpp.progression.helper.EnvelopeHelper;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class AddCourtDocumentHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private CourtDocumentEnricher courtDocumentEnricher;

    @Mock
    private DocumentTypeAccessProvider documentTypeAccessProvider;

    @Mock
    private EnvelopeHelper envelopeHelper;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private DefaultCourtDocumentFactory defaultCourtDocumentFactory;

    @Mock
    private Logger logger;

    @InjectMocks
    private AddCourtDocumentHandler addCourtDocumentHandler;

    private CourtDocument buildCourtDocument() {

        return CourtDocument.courtDocument().withName("SJP notice")
                .withDocumentTypeId(randomUUID()).withCourtDocumentId(randomUUID())
                .withMaterials(Collections.singletonList(Material.material().withId(randomUUID())
                        .withUploadDateTime(ZonedDateTime.now(ZoneOffset.UTC)).build()))
                .withContainsFinancialMeans(false)
                .withSeqNum(10)
                .build();

    }

    @Before
    public void setup() {
        createEnveloperWithEvents(CourtsDocumentAdded.class);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new AddCourtDocumentHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.add-court-document")
                ));
    }

    @Ignore("will restore later")
    @Test
    public void shouldProcessCommand() throws Exception {

        final AddCourtDocument addCourtDocument = addCourtDocument()
                .withCourtDocument(buildCourtDocument())
                .build();
        final CourtDocument courtDocument = addCourtDocument.getCourtDocument();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                buildCourtDocumentWithoutDocumentType());
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final DocumentTypeAccess documentTypeData = mock(DocumentTypeAccess.class);
        final CourtDocument enrichedCourtDocument = mock(CourtDocument.class);
        final EventStream eventStream = mock(EventStream.class);
        final CourtDocumentAggregate courtDocumentAggregate = mock(CourtDocumentAggregate.class);

        when(defaultCourtDocumentFactory.createDefaultCourtDocument(courtDocument)).thenReturn(courtDocument);
        when(objectToJsonObjectConverter.convert(courtDocument)).thenReturn(payload);
        when(envelopeHelper.withMetadataInPayload(any())).thenReturn(jsonEnvelope);
        when(documentTypeAccessProvider.getDocumentTypeAccess(courtDocument, jsonEnvelope)).thenReturn(documentTypeData);
        when(courtDocumentEnricher.enrichWithMaterialUserGroups(courtDocument, documentTypeData)).thenReturn(enrichedCourtDocument);
        when(eventSource.getStreamById(enrichedCourtDocument.getCourtDocumentId())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtDocumentAggregate.class)).thenReturn(courtDocumentAggregate);
        when(courtDocumentAggregate.addCourtDocument(enrichedCourtDocument)).thenReturn(Stream.of(CourtsDocumentAdded.courtsDocumentAdded().withCourtDocument(courtDocument).build()));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-court-document")
                .withId(randomUUID())
                .build();

        final Envelope<AddCourtDocument> envelope = envelopeFrom(metadata, addCourtDocument);

        addCourtDocumentHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-document-added"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtDocument", notNullValue()),
                                withJsonPath("$.courtDocument.containsFinancialMeans", is(false)),
                                withJsonPath("$.courtDocument.documentTypeId", notNullValue()),
                                withJsonPath("$.courtDocument.seqNum", is(10))
                                )
                        ))

                )
        );
    }

    private JsonObject buildCourtDocumentWithoutDocumentType() {

        final JsonObject documentCategory =
                Json.createObjectBuilder().add("defendantDocument",
                        Json.createObjectBuilder()
                                .add("prosecutionCaseId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                                .add("defendants", createArrayBuilder().add("e1d32d9d-29ec-4934-a932-22a50f223966"))).build();

        return Json.createObjectBuilder().add("courtDocument",
                Json.createObjectBuilder()
                        .add("courtDocumentId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                        .add("documentCategory", documentCategory)
                        .add("name", "SJP Notice")
                        .add("documentTypeId", "0bb7b276-9dc0-4af2-83b9-f4acef0c7898")
                        .add("documentTypeDescription", "SJP Notice")
                        .add("mimeType", "pdf")
                        .add("materials", Json.createObjectBuilder().add("id", "5e1cc18c-76dc-47dd-99c1-d6f87385edf1"))
                        .add("containsFinancialMeans", false)
                        .add("seqNum", 10))
                .build();
    }
}
