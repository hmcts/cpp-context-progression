package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
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
import uk.gov.justice.progression.event.SendToCpsFlagUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;
import uk.gov.moj.cpp.progression.command.UpdateSendToCpsFlag;
import uk.gov.moj.cpp.progression.helper.EnvelopeHelper;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateCourtDocumentHandlerTest {

    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CourtDocumentUpdated.class, CourtDocumentPrintTimeUpdated.class, SendToCpsFlagUpdated.class);
    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();
    @Mock
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private EnvelopeHelper envelopeHelper;
    @Mock
    private RefDataService refDataService;
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

        final CourtDocumentAggregate courtDocumentAggregate = new CourtDocumentAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtDocumentAggregate.class)).thenReturn(courtDocumentAggregate);
        final UUID caseId = randomUUID();

        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(caseId).build()).build())
                .withMaterials(new ArrayList<>())
                .withSendToCps(false)
                .build();
        courtDocumentAggregate.createCourtDocument(courtDocument, true, null);

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

        final CourtDocumentAggregate courtDocumentAggregate = new CourtDocumentAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtDocumentAggregate.class)).thenReturn(courtDocumentAggregate);
        final UUID caseId = randomUUID();

        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(caseId).build()).build())
                .withMaterials(new ArrayList<>())
                .withSendToCps(false)
                .build();
        courtDocumentAggregate.createCourtDocument(courtDocument, true, null);

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

        final CourtDocumentAggregate courtDocumentAggregate = new CourtDocumentAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtDocumentAggregate.class)).thenReturn(courtDocumentAggregate);
        final UUID caseId = randomUUID();

        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(caseId).build()).build())
                .withMaterials(new ArrayList<>())
                .withSendToCps(false)
                .build();
        courtDocumentAggregate.createCourtDocument(courtDocument, true, null);

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
        final CourtDocumentAggregate courtDocumentAggregate = new CourtDocumentAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtDocumentAggregate.class)).thenReturn(courtDocumentAggregate);
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

    @Test
    public void shouldHandleUpdateSendToCpsFlag() throws EventStreamException {
        final CourtDocument courtDocument = CourtDocument.courtDocument().withCourtDocumentId(randomUUID()).build();
        final UpdateSendToCpsFlag updateSendToCpsFlag = UpdateSendToCpsFlag.updateSendToCpsFlag()
                .withCourtDocumentId(randomUUID())
                .withSendToCps(true)
                .withCourtDocument(courtDocument)
                .build();
        final CourtDocumentAggregate courtDocumentAggregate = new CourtDocumentAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtDocumentAggregate.class)).thenReturn(courtDocumentAggregate);

        final Envelope<UpdateSendToCpsFlag> envelope =
                envelopeFrom(metadataFor("progression.command.update-send-to-cps-flag", randomUUID()),
                        updateSendToCpsFlag);

        target.handleUpdateSendToCpsFlag(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        MatcherAssert.assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.send-to-cps-flag-updated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtDocumentId", is(updateSendToCpsFlag.getCourtDocumentId().toString())),
                                withJsonPath("$.courtDocument", notNullValue()),
                                withJsonPath("$.sendToCps", is(updateSendToCpsFlag.getSendToCps()))
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
                .withPrintedAt(new UtcClock().now())
                .withMaterialId(randomUUID())
                .withCourtDocumentId(randomUUID())
                .build();
    }

}
