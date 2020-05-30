package uk.gov.moj.cpp.progression.processor.document;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.moj.cpp.progression.processor.document.CourtDocumentAddedProcessor.PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT;
import static uk.gov.moj.cpp.progression.processor.document.CourtDocumentAddedProcessor.PUBLIC_IDPC_COURT_DOCUMENT_RECEIVED;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.DefenceNotificationService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;
import uk.gov.moj.cpp.progression.service.payloads.UserGroupDetails;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentAddedProcessorTest {

    @InjectMocks
    private CourtDocumentAddedProcessor eventProcessor;


    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Mock
    private Sender sender;

    @Mock
    private UsersGroupService usersGroupService;

    @Mock
    private DefenceNotificationService defenceNotificationService;



    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private static JsonObject buildDocumentCategoryJsonObject(String documentTypeId) {

        final JsonObject documentCategory =
                createObjectBuilder().add("defendantDocument",
                        createObjectBuilder()
                                .add("prosecutionCaseId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                                .add("defendants", createArrayBuilder().add("e1d32d9d-29ec-4934-a932-22a50f223966"))).build();

        final JsonObject courtDocument =
                createObjectBuilder().add("courtDocument",
                        createObjectBuilder()
                                .add("courtDocumentId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                                .add("documentCategory", documentCategory)
                                .add("name", "SJP Notice")
                                .add("documentTypeId", documentTypeId)
                                .add("documentTypeDescription", "SJP Notice")
                                .add("mimeType", "pdf")
                                .add("materials", createArrayBuilder().add(buildMaterial()))
                                .add("containsFinancialMeans", false)
                                .add("documentTypeRBAC", buildDocumentTypeDataWithRBAC()))
                        .build();

        return courtDocument;
    }

    private static JsonObject buildMaterial() {
        return  createObjectBuilder().add("id", "5e1cc18c-76dc-47dd-99c1-d6f87385edf1").add("receivedDateTime", ZonedDateTime.now().toString()).build();
    }

    private static JsonObject buildDocumentTypeDataWithRBAC() {
        return Json.createObjectBuilder()
                .add("documentAccess", Json.createArrayBuilder().add("Listing Officer"))
                .add("canCreateUserGroups", Json.createArrayBuilder().add("Listing Officer"))
                .add("canReadUserGroups", Json.createArrayBuilder().add("Listing Officer").add("Magistrates"))
                .add("canDownloadUserGroups", Json.createArrayBuilder().add("Listing Officer").add("Magistrates"))
                .build();
    }

    @Test
    public void shouldProcessUploadCourtDocumentMessage() {

        final JsonObject courtDocumentPayload = buildDocumentCategoryJsonObject("0bb7b276-9dc0-4af2-83b9-f4acef0c7898");

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);

        List<UserGroupDetails> userGroupDetails  = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(UUID.randomUUID(),"Chambers Admin"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);
        when(usersGroupService.getGroupIdForDefenceLawyers()).thenReturn(UUID.randomUUID().toString());

        eventProcessor.handleCourtDocumentAddEvent(requestMessage);
        verify(sender, times(2)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata(), withMetadataEnvelopedFrom(requestMessage).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT));
        final JsonObject commandCreateCourtDocumentPayload = commands.get(0).payload();
        //This is an Error Payload Structure that is actually returned....
        assertThat(commandCreateCourtDocumentPayload.getJsonObject("courtDocument").getBoolean("containsFinancialMeans"),is(false));
        final JsonObject documentTypeRBACObject = commandCreateCourtDocumentPayload.getJsonObject("courtDocument").getJsonObject("documentTypeRBAC");
        assertThat(documentTypeRBACObject, is(buildDocumentTypeDataWithRBAC()));
    }


    @Test
    public void shouldProcessUploadIDPCCourtDocumentMessage() {

        final JsonObject courtDocumentPayload = buildDocumentCategoryJsonObject("41be14e8-9df5-4b08-80b0-1e670bc80a5b");

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);

        List<UserGroupDetails> userGroupDetails  = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(UUID.randomUUID(),"Chambers Admin"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);

        eventProcessor.handleCourtDocumentAddEvent(requestMessage);
        verify(sender, times(3)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata(), withMetadataEnvelopedFrom(requestMessage).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT));
        assertThat(commands.get(2).metadata().name(), is(PUBLIC_IDPC_COURT_DOCUMENT_RECEIVED));
        final JsonObject commandCreateCourtDocumentPayload = commands.get(0).payload();
        final JsonObject commandIDPCPayload = commands.get(2).payload();
        //This is an Error Payload Structure that is actually returned....
        assertThat(commandCreateCourtDocumentPayload.getJsonObject("courtDocument").getBoolean("containsFinancialMeans"),is(false));
        final JsonObject documentTypeRBACObject = commandCreateCourtDocumentPayload.getJsonObject("courtDocument").getJsonObject("documentTypeRBAC");
        assertThat(documentTypeRBACObject, is(buildDocumentTypeDataWithRBAC()));
        assertThat(commandIDPCPayload.getString("caseId"),is("2279b2c3-b0d3-4889-ae8e-1ecc20c39e27"));
        assertThat(commandIDPCPayload.getString("materialId"),is("5e1cc18c-76dc-47dd-99c1-d6f87385edf1"));
        assertThat(commandIDPCPayload.getString("defendantId"),is("e1d32d9d-29ec-4934-a932-22a50f223966"));


    }

}
