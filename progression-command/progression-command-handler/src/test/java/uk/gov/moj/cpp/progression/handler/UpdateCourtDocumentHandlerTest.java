package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.UpdateCourtDocumentPrintTime.updateCourtDocumentPrintTime;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.progression.command.helper.HandlerTestHelper.metadataFor;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentPrintTimeUpdated;
import uk.gov.justice.core.courts.CourtDocumentUpdated;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.UpdateCourtDocument;
import uk.gov.justice.core.courts.UpdateCourtDocumentPrintTime;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;
import uk.gov.moj.cpp.progression.helper.EnvelopeHelper;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateCourtDocumentHandlerTest {

    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CourtDocumentUpdated.class, CourtDocumentPrintTimeUpdated.class);
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Mock
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private EnvelopeHelper envelopeHelper;
    @Mock
    private ReferenceDataService refDataService;
    @Mock
    private JsonEnvelope envelope;
    @InjectMocks
    private UpdateCourtDocumentHandler target;
    private CourtDocumentAggregate aggregate;

    private static JsonObject buildDocumentTypeDataWithRBAC(final String documentCategory) {
        return Json.createObjectBuilder().add("section", "charges")
                .add("documentCategory", documentCategory)
                .add("courtDocumentTypeRBAC",
                        Json.createObjectBuilder()
                                .add("uploadUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer").build()).build())
                                .add("readUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer")).add(buildUserGroup("Magistrates")).build())
                                .add("downloadUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer")).add(buildUserGroup("Magistrates")).build()).build())
                .add("seqNum", 10)
                .build();
    }

    private static JsonObjectBuilder buildUserGroup(final String userGroupName) {
        return Json.createObjectBuilder().add("cppGroup", Json.createObjectBuilder().add("id", randomUUID().toString()).add("groupName", userGroupName));
    }

    @Before
    public void setup() {
        aggregate = new CourtDocumentAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtDocumentAggregate.class)).thenReturn(aggregate);
        final UUID caseId = randomUUID();

        CourtDocument courtDocument = CourtDocument.courtDocument()
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(caseId).build()).build())
                .withMaterials(new ArrayList<>())
                .withSendToCps(false)
                .build();
        this.aggregate.createCourtDocument(courtDocument, true);

        ReflectionUtil.setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        ReflectionUtil.setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateCourtDocumentHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleUpdateCourtDocument")
                        .thatHandles("progression.command.update-court-document")
                ));
    }

    @Test
    public void shouldProcessShareCourtDocumentCommandForDefendantLevel() throws Exception {

        final UpdateCourtDocument updateCourtDocument = buildUpdateCourtDocument();

        when(refDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(buildDocumentTypeDataWithRBAC("Defendant level")));


        final Envelope<UpdateCourtDocument> envelope =
                envelopeFrom(metadataFor("progression.command.update-court-document", randomUUID()),
                        updateCourtDocument);

        target.handleUpdateCourtDocument(envelope);

        verifyAppendAndGetArgumentFrom(eventStream);
    }

    @Test
    public void shouldProcessShareCourtDocumentCommandForCaseLevel() throws Exception {

        final UpdateCourtDocument updateCourtDocument = buildUpdateCourtDocumentWithProsecutionCase();

        when(refDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(buildDocumentTypeDataWithRBAC("Case level")));


        final Envelope<UpdateCourtDocument> envelope =
                envelopeFrom(metadataFor("progression.command.update-court-document", randomUUID()),
                        updateCourtDocument);

        target.handleUpdateCourtDocument(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-document-updated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtDocument", notNullValue()),
                                withJsonPath("$.courtDocument.documentCategory.caseDocument.prosecutionCaseId", notNullValue()),
                                withJsonPath("$.courtDocument.documentTypeId", notNullValue()),
                                withJsonPath("$.courtDocument.seqNum", is(10))
                                )
                        ))

                )
        );
    }

    @Test
    public void shouldProcessShareCourtDocumentCommandForApplicationLevel() throws Exception {

        final UpdateCourtDocument updateCourtDocument = buildUpdateCourtDocumentWithApplication();

        when(refDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(buildDocumentTypeDataWithRBAC("Applications")));


        final Envelope<UpdateCourtDocument> envelope =
                envelopeFrom(metadataFor("progression.command.update-court-document", randomUUID()),
                        updateCourtDocument);

        target.handleUpdateCourtDocument(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-document-updated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtDocument", notNullValue()),
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", notNullValue()),
                                withJsonPath("$.courtDocument.documentTypeId", notNullValue()),
                                withJsonPath("$.courtDocument.seqNum", is(10))
                                )
                        ))

                )
        );
    }

    @Test
    public void shouldHandleUpdateCourtDocumentPrintTime() throws EventStreamException {
        final UpdateCourtDocumentPrintTime courtDocumentPrintTime = buildUpdateCourtDocumentPrintTime();
        final Envelope<UpdateCourtDocumentPrintTime> envelope =
                envelopeFrom(metadataFor("progression.command.update-court-document-print-time", randomUUID()),
                        courtDocumentPrintTime);

        target.handleUpdateCourtDocumentPrintTime(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        MatcherAssert.assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-document-print-time-updated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtDocumentId", is(courtDocumentPrintTime.getCourtDocumentId().toString())),
                                withJsonPath("$.materialId", is(courtDocumentPrintTime.getMaterialId().toString())),
                                withJsonPath("$.printedAt", is(courtDocumentPrintTime.getPrintedAt().format(ISO_8601_FORMATTER)))
                                )
                        ))
                )
        );
    }

    private UpdateCourtDocument buildUpdateCourtDocument() {

        return UpdateCourtDocument.updateCourtDocument().withCourtDocumentId(randomUUID())
                .withDocumentTypeId(randomUUID()).withContainsFinancialMeans(true).withDefendants(Arrays.asList(randomUUID())).withName("SJP Notice").withSendToCps(false).build();
    }

    private UpdateCourtDocument buildUpdateCourtDocumentWithProsecutionCase() {

        return UpdateCourtDocument.updateCourtDocument().withCourtDocumentId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .withDocumentTypeId(randomUUID()).withContainsFinancialMeans(true).withDefendants(Arrays.asList(randomUUID())).withName("SJP Notice").withSendToCps(false).build();
    }

    private UpdateCourtDocument buildUpdateCourtDocumentWithApplication() {

        return UpdateCourtDocument.updateCourtDocument().withCourtDocumentId(randomUUID())
                .withApplicationId(randomUUID())
                .withDocumentTypeId(randomUUID()).withContainsFinancialMeans(true).withDefendants(Arrays.asList(randomUUID())).withName("SJP Notice").withSendToCps(false).build();
    }

    private UpdateCourtDocumentPrintTime buildUpdateCourtDocumentPrintTime() {
        return updateCourtDocumentPrintTime()
                .withPrintedAt(now())
                .withMaterialId(randomUUID())
                .withCourtDocumentId(randomUUID())
                .build();
    }

}
