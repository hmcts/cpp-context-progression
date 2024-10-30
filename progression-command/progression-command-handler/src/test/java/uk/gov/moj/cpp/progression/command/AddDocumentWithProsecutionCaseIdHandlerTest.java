package uk.gov.moj.cpp.progression.command;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentWithProsecutionCaseIdAdded;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.handler.AddDocumentWithProsecutionCaseIdHandler;
import uk.gov.moj.cpp.progression.helper.EnvelopeHelper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class AddDocumentWithProsecutionCaseIdHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EnvelopeHelper envelopeHelper;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Logger logger;

    @InjectMocks
    private AddDocumentWithProsecutionCaseIdHandler addDocumentWithProsecutionCaseIdHandler;

    @BeforeEach
    public void setup() {
        createEnveloperWithEvents(DocumentWithProsecutionCaseIdAdded.class);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new AddDocumentWithProsecutionCaseIdHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.add-document-with-prosecution-case-id")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        final UUID caseId = randomUUID();
        final AddDocumentWithProsecutionCaseId addDocumentWithProsecutionCaseId = AddDocumentWithProsecutionCaseId.addDocumentWithProsecutionCaseId()
                .withCourtDocument(buildCourtDocument())
                .withCaseId(caseId)
                .build();
        final CourtDocument courtDocument = addDocumentWithProsecutionCaseId.getCourtDocument();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.add-document-with-prosecution-case-id"),
                buildCourtDocumentDocumentType());
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final EventStream eventStream = mock(EventStream.class);
        final CaseAggregate caseAggregate = mock(CaseAggregate.class);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.addDocument(any())).thenReturn(Stream.of(DocumentWithProsecutionCaseIdAdded.documentWithProsecutionCaseIdAdded().withCourtDocument(courtDocument).withProsecutionCase(ProsecutionCase.prosecutionCase().withId(caseId).build()).build()));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-document-with-prosecution-case-id")
                .withId(randomUUID())
                .build();

        final Envelope<AddDocumentWithProsecutionCaseId> envelope = envelopeFrom(metadata, addDocumentWithProsecutionCaseId);

        addDocumentWithProsecutionCaseIdHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.document-with-prosecution-case-id-added"),
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

    private JsonObject buildCourtDocumentDocumentType() {

        final JsonObject documentCategory =
                createObjectBuilder().add("defendantDocument",
                        createObjectBuilder()
                                .add("prosecutionCaseId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                                .add("defendants", createArrayBuilder().add("e1d32d9d-29ec-4934-a932-22a50f223966"))).build();

        return createObjectBuilder().add("courtDocument",
                        createObjectBuilder()
                                .add("courtDocumentId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                                .add("documentCategory", documentCategory)
                                .add("name", "SJP Notice")
                                .add("documentTypeId", "0bb7b276-9dc0-4af2-83b9-f4acef0c7898")
                                .add("documentTypeDescription", "SJP Notice")
                                .add("mimeType", "pdf")
                                .add("materials", createObjectBuilder().add("id", "5e1cc18c-76dc-47dd-99c1-d6f87385edf1"))
                                .add("containsFinancialMeans", false)
                                .add("seqNum", 10))
                .build();
    }

    private static JsonObjectBuilder buildUserGroup(final String userGroupName) {
        return Json.createObjectBuilder().add("cppGroup", Json.createObjectBuilder().add("id", randomUUID().toString()).add("groupName", userGroupName));
    }

    private CourtDocument buildCourtDocument() {
        final List<UUID> defendantIdList = new ArrayList<>();
        final UUID defendantId = UUID.randomUUID();
        defendantIdList.add(defendantId);
        final DefendantDocument defendantDocument = DefendantDocument.defendantDocument()
                .withProsecutionCaseId(UUID.randomUUID())
                .withDefendants(defendantIdList)
                .build();
        final DocumentCategory documentCategory = DocumentCategory.documentCategory().withDefendantDocument(defendantDocument).build();

        return CourtDocument.courtDocument().withName("SJP notice")
                .withDocumentTypeId(randomUUID()).withCourtDocumentId(randomUUID())
                .withMaterials(Collections.singletonList(Material.material().withId(randomUUID())
                        .withUploadDateTime(ZonedDateTime.now(ZoneOffset.UTC)).build()))
                .withContainsFinancialMeans(false)
                .withDocumentCategory(documentCategory)
                .withSeqNum(10)
                .build();
    }
}