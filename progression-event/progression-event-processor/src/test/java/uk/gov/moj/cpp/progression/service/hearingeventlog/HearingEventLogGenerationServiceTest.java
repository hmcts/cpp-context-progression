package uk.gov.moj.cpp.progression.service.hearingeventlog;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.utils.PayloadUtil.getPayloadAsJsonObject;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)

public class HearingEventLogGenerationServiceTest {

    private final String hearingEventLogResponse = "hearing.get-hearing-event-log-document.json";
    public static final UUID APPLICATION_DOCUMENT_TYPE_ID = UUID.fromString("460fae22-c002-11e8-a355-529269fb1459");

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Mock
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    @Captor
    ArgumentCaptor<JsonObject> hearingEventLogArgumentCaptor;
    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;
    @Mock
    private SystemUserProvider systemUserProvider;
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();
    @Mock
    private Sender sender;

    @Mock
    private DocumentGeneratorService documentGeneratorService;
    @InjectMocks
    private HearingEventLogGenerationService hearingEventLogGenerationService;
    @Mock
    private RefDataService referenceDataService;
    @Mock
    private DocumentGeneratorClient documentGeneratorClient;
    @Mock
    private JsonEnvelope originatingEnvelope;
    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Test
    public void shouldGenerateCaseHearingEventLog() throws Exception {
        final byte[] documentData = {34, 56, 78, 90};
        final UUID systemUserId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final JsonObject responsePayload = getPayloadAsJsonObject(hearingEventLogResponse);
        final Optional<JsonObject> documentTypeData = buildDocumentTypeDataWithRBAC("documentCategory");


        when(originatingEnvelope.metadata()).thenReturn(metadataBuilder().withId(randomUUID()).withName("progression.event.hearing-event-logs-document-created").withUserId(randomUUID().toString()).build());
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(documentData);
        when(documentGeneratorService.generatePdfDocument(any(), any(), any())).thenReturn(materialId);
        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(documentTypeData);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(documentTypeData.get());

        hearingEventLogGenerationService.generateHearingLogEvent(originatingEnvelope, caseId, responsePayload,Optional.empty());

        verify(documentGeneratorClient, times(1)).generatePdfDocument(hearingEventLogArgumentCaptor.capture(), anyString(), any(UUID.class));

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.create-court-document"));
        assertThat(envelopeArgumentCaptor.getValue().payload(), notNullValue());
        assertThat(envelopeArgumentCaptor.getValue().payload().toString(), isJson(anyOf( withJsonPath("$.courtDocument.documentCategory.caseDocument.prosecutionCaseId", equalTo(caseId)),
                withJsonPath("$.courtDocument.section", equalTo("orders & notices")),
                withJsonPath("$.documentCategory", equalTo("documentCategory")),
                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf")))));

    }

    @Test
    public void shouldGenerateApplicationHearingEventLog() throws Exception {
        final byte[] documentData = {34, 56, 78, 90};
        final UUID systemUserId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID applicationId = randomUUID();
        final JsonObject responsePayload = getPayloadAsJsonObject(hearingEventLogResponse);
        final Optional<JsonObject> documentTypeData = buildDocumentTypeDataWithRBAC("documentCategory");


        when(originatingEnvelope.metadata()).thenReturn(metadataBuilder().withId(randomUUID()).withName("progression.event.hearing-event-logs-document-created").withUserId(randomUUID().toString()).build());
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(documentData);
        when(documentGeneratorService.generatePdfDocument(any(), any(), any())).thenReturn(materialId);
        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(documentTypeData);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(documentTypeData.get());

        hearingEventLogGenerationService.generateHearingLogEvent(originatingEnvelope, caseId, responsePayload,Optional.of(applicationId.toString()));

        verify(documentGeneratorClient, times(1)).generatePdfDocument(hearingEventLogArgumentCaptor.capture(), anyString(), any(UUID.class));

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.create-court-document"));
        assertThat(envelopeArgumentCaptor.getValue().payload(), notNullValue());
        assertThat(envelopeArgumentCaptor.getValue().payload().toString(), isJson(anyOf( withJsonPath("$.courtDocument.documentCategory.caseDocument.prosecutionCaseId", equalTo(caseId)),
                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(caseId)),
                withJsonPath("$.courtDocument.section", equalTo("orders & notices")),
                withJsonPath("$.documentCategory", equalTo("documentCategory")),
                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf")))));

    }

    private static Optional<JsonObject> buildDocumentTypeDataWithRBAC(final String documentCategory) {
        return Optional.ofNullable(Json.createObjectBuilder().add("section", "orders & notices")
                .add("documentCategory", "documentCategory")
                .add("documentTypeDescription", "Applications")
                .add("documentTypeId", documentCategory)
                .add("", "")
                .add("mimeType", "application/pdf")
                .add("courtDocumentTypeRBAC",
                        Json.createObjectBuilder()
                                .add("uploadUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer").build()).build())
                                .add("readUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer")).add(buildUserGroup("Magistrates")).build())
                                .add("downloadUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer")).add(buildUserGroup("Magistrates")).build()).build())
                .add("seqNum", 10)
                .build());
    }

    private static JsonObjectBuilder buildUserGroup(final String userGroupName) {
        return Json.createObjectBuilder().add("cppGroup", Json.createObjectBuilder().add("id", randomUUID().toString()).add("groupName", userGroupName));
    }


}