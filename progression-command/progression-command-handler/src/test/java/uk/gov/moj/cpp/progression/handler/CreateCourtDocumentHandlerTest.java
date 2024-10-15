package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
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

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentCreated;
import uk.gov.justice.core.courts.CreateCourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
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
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateCourtDocumentHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private RefDataService refDataService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CourtsDocumentCreated.class);

    @InjectMocks
    private CreateCourtDocumentHandler createCourtDocumentHandler;


    @Test
    public void shouldHandleCommand() {
        assertThat(new CreateCourtDocumentHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.create-court-document")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        final CreateCourtDocument createCourtDocument =
                CreateCourtDocument.createCourtDocument().withCourtDocument(buildCourtDocument()).build();

        final CourtDocumentAggregate courtDocumentAggregate = new CourtDocumentAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtDocumentAggregate.class)).thenReturn(courtDocumentAggregate);

        courtDocumentAggregate.apply(createCourtDocument.getCourtDocument());


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.create-court-document")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<CreateCourtDocument> envelope = envelopeFrom(metadata, createCourtDocument);

        when(refDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(buildDocumentTypeDataWithRBAC()));

        createCourtDocumentHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-document-created"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtDocument", notNullValue()),
                                withJsonPath("$.courtDocument.containsFinancialMeans", notNullValue()),
                                withJsonPath("$.courtDocument.documentTypeRBAC", notNullValue())
                                )
                        ))

                )
        );
    }

    private CourtDocument buildCourtDocument() {

        return CourtDocument.courtDocument().withName("SJP notice")
                .withDocumentTypeId(randomUUID()).withCourtDocumentId(randomUUID())
                .withMaterials(Collections.singletonList(Material.material().withId(randomUUID())
                        .withUploadDateTime(ZonedDateTime.now(ZoneOffset.UTC)).build()))
                .withContainsFinancialMeans(true)
                .withDocumentCategory(DocumentCategory.documentCategory().build())
                .withSeqNum(10)
                .withSendToCps(false)
                .build();

    }

    private static JsonObject buildDocumentTypeDataWithRBAC() {
        return Json.createObjectBuilder()
                .add("courtDocumentTypeRBAC",
                        Json.createObjectBuilder()
                                .add("uploadUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer").build()).build())
                                .add("readUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer")).add(buildUserGroup("Magistrates")).build())
                                .add("downloadUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer")).add(buildUserGroup("Magistrates")).build()).build())
                .add("seqNum",10)
                .build();
    }

    private static JsonObjectBuilder buildUserGroup(final String userGroupName) {
        return Json.createObjectBuilder().add("cppGroup", Json.createObjectBuilder().add("id", randomUUID().toString()).add("groupName", userGroupName));
    }


}
