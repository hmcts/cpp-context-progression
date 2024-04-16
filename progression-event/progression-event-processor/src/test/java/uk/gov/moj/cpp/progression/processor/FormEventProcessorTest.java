package uk.gov.moj.cpp.progression.processor;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.BCM;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.CASE_DOCUMENT_TYPE_ID;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.CASE_ID;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.COURT_DOCUMENT;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.COURT_FORM_ID;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.DOCUMENT_META_DATA;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.EMAIL;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.EXPIRY_TIME;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.FINALISED_FORM_DATA;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.FIRST_NAME;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.FORM_DATA;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.FORM_DEFENDANTS;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.FORM_ID;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.FORM_TYPE;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.IS_LOCKED;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.IS_WELSH;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.LAST_NAME;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.LOCKED_BY;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.LOCK_REQUESTED_BY;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.LOCK_STATUS;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.MATERIAL_ID;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.PROGRESSION_COMMAND_ADD_COURT_DOCUMENT;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.PUBLIC_PROGRESSION_BCM_DEFENDANTS_UPDATED;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.PUBLIC_PROGRESSION_EDIT_FORM_REQUESTED;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.PUBLIC_PROGRESSION_FORM_CREATED;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.PUBLIC_PROGRESSION_FORM_FINALISED;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.PUBLIC_PROGRESSION_FORM_OPERATION_FAILED;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.PUBLIC_PROGRESSION_FORM_UPDATED;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.UPDATED_BY;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.USER_ID;
import static uk.gov.moj.cpp.progression.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.utils.FileUtil.jsonFromString;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.UpdateCpsDefendantId;
import uk.gov.moj.cpp.progression.service.CpsApiService;
import uk.gov.moj.cpp.progression.service.DefenceService;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.google.common.io.Resources;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class FormEventProcessorTest {

    public static final String userId = randomUUID().toString();
    public static final String COURT_DOCUMENT_ID = "courtDocumentId";
    public static final String DOCUMENT_CATEGORY = "documentCategory";
    public static final String DOCUMENT_TYPE_ID = "documentTypeId";
    public static final String CASE_DOCUMENT = "caseDocument";
    public static final String DEFENDANT_DOCUMENT = "defendantDocument";
    public static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    public static final String USER_NAME = "userName";

    public static final String PROGRESSION_COMMAND_UPDATE_CPS_DEFENDANT_ID = "progression.command.update-cps-defendant-id";
    public static final String PROGRESSION_EVENT_FORM_OPERATION_FAILED = "progression.event.form-operation-failed";
    public static final String PROGRESSION_EVENT_FORM_FORM_CREATED = "progression.event.form-created";
    public static final String PROGRESSION_EVENT_FORM_UPDATED = "progression.event.form-updated";
    public static final String PROGRESSION_EVENT_FORM_FINALISED = "progression.event.form-finalised";
    public static final String PROGRESSION_BCM_DEFENDANTS_UPDATED = "progression.event.form-defendants-updated";
    public static final String PROGRESSION_EVENT_EDIT_FORM_REQUESTED = "progression.event.edit-form-requested";

    private static final JsonObject USER_DETAILS = createObjectBuilder().add(FIRST_NAME, "firstName").add(LAST_NAME, "lastName").build();
    private static final String GROUP_ADVOCATES_USER = "Advocates";

    public static final String BCM_URL="https://spnl-apim-int-gw.cpp.nonlive/CPS/v1/notification/bcm-notification";

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @InjectMocks
    private FormEventProcessor formEventProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private UsersGroupService usersGroupService;

    @Mock
    private DefenceService defenceService;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ProsecutionCase prosecutionCase;

    @Mock
    private ProsecutionCaseIdentifier prosecutionCaseIdentifier;

    @Mock
    private Response response;

    @Mock
    private CpsApiService cpsApiService;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter ;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldRaiseFormCreatedPublicEvent() {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_FORM_CREATED).withUserId(FormEventProcessorTest.userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, courtFormId)
                        .add(CASE_ID, caseId)
                        .add(FORM_TYPE, "BCM")
                        .add(FORM_DEFENDANTS, createArrayBuilder().build())
                        .add(FORM_DATA, "formData")
                        .add(FORM_ID, UUID.randomUUID().toString())
                        .add(USER_ID, userId)
        );
        when(usersGroupService.getUserById(any(), any())).thenReturn(USER_DETAILS);
        when(usersGroupService.isUserPartOfGroup(any(), any())).thenReturn(true);

        formEventProcessor.formCreated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_FORM_CREATED));
        verify(usersGroupService).getUserById(any(), eq(userId));
        verify(usersGroupService).isUserPartOfGroup(any(), eq(GROUP_ADVOCATES_USER));
    }

    @Test
    public void shouldRaiseCpsFormCreatedPublicEvent() {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_FORM_CREATED).withUserId(FormEventProcessorTest.userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, courtFormId)
                        .add(CASE_ID, caseId)
                        .add(FORM_TYPE, "BCM")
                        .add(FORM_DEFENDANTS, createArrayBuilder().build())
                        .add(FORM_DATA, "formData")
                        .add(FORM_ID, UUID.randomUUID().toString())
                        .add(USER_NAME, "CMS User")
        );

        formEventProcessor.formCreated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_FORM_CREATED));
    }

    @Test
    public void shouldNotFetchUserDetailsForCPSUserFormCreatedPublicEvent() {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_FORM_CREATED).withUserId(userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, courtFormId)
                        .add(CASE_ID, caseId)
                        .add(FORM_TYPE, BCM)
                        .add(FORM_DEFENDANTS, createArrayBuilder().build())
                        .add(FORM_DATA, createObjectBuilder().build().toString())
                        .add(FORM_ID, UUID.randomUUID().toString())
                        .add(USER_NAME, "userName")
        );

        when(usersGroupService.getUserById(any(), any())).thenReturn(USER_DETAILS);
        formEventProcessor.formCreated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_FORM_CREATED));
    }

    @Test
    public void shouldRaiseFormOperationFailedEvent() {
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_OPERATION_FAILED).withUserId(userId),
                createObjectBuilder()
        );

        formEventProcessor.formOperationFailed(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_FORM_OPERATION_FAILED));
    }


    @Test
    public void shouldRaiseFormFinalisedPublicEvent() throws IOException {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final String fileName = "Jane JOHNSON, 22 May 1976, created on 22 December 10:45 2022.pdf";

        final String inputEvent = Resources.toString(getResource("finalised-form-data.json"), defaultCharset())
                .replaceAll("%caseId%", caseId)
                .replaceAll("%courtFormId%", courtFormId)
                .replaceAll("%formType%", FormType.BCM.name())
                .replaceAll("%userId%", userId);
        final JsonObject readData = stringToJsonObjectConverter.convert(inputEvent);
        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        readData.getJsonArray("finalisedFormData").forEach(x -> arrayBuilder.add(x.toString()));
        payloadBuilder.add("finalisedFormData", arrayBuilder.build())
                .add("caseId", caseId)
                .add("courtFormId", courtFormId)
                .add("formType", FormType.BCM.name())
                .add("userId", userId)
                .add(MATERIAL_ID, UUID.randomUUID().toString());

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_FINALISED)
                        .withUserId(FormEventProcessorTest.userId)
                , payloadBuilder.build());

        when(documentGeneratorService.generateFormDocument(any(), any(), any(), any())).thenReturn(fileName);
        when(usersGroupService.getUserById(any(), any())).thenReturn(USER_DETAILS);
        formEventProcessor.formFinalised(requestEnvelope);
        verify(sender, times(4)).send(envelopeArgumentCaptor.capture());

        final List<JsonEnvelope> envelopes = envelopeArgumentCaptor.getAllValues();

        final JsonEnvelope envelope1 = envelopes.get(0);
        assertAddCourtOrderCommandFromEnvelope(caseId, envelope1);

        final JsonEnvelope envelope2 = envelopes.get(1);
        assertAddCourtOrderCommandFromEnvelope(caseId, envelope2);

        final JsonEnvelope envelope3 = envelopes.get(2);
        assertAddCourtOrderCommandFromEnvelope(caseId, envelope3);

        final JsonEnvelope envelope4 = envelopes.get(3);
        assertPublicEventCreationFromCapturedEnvelope(courtFormId, caseId, userId, FormType.BCM, 3, false, envelope4, USER_DETAILS);
    }


    @Test
    public void shouldRaiseFormFinalisedPublicEvent_PTPH() throws IOException {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final String fileName = "Jane JOHNSON, 22 May 1976, created on 22 December 10:45 2022.pdf";

        final String inputEvent = Resources.toString(getResource("ptph-finalised-form-data.json"), defaultCharset())
                .replaceAll("%caseId%", caseId)
                .replaceAll("%courtFormId%", courtFormId)
                .replaceAll("%formType%", FormType.PTPH.name())
                .replaceAll("%userId%", userId);
        final JsonObject readData = stringToJsonObjectConverter.convert(inputEvent);
        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        readData.getJsonArray("finalisedFormData").forEach(x -> arrayBuilder.add(x.toString()));
        payloadBuilder.add("finalisedFormData", arrayBuilder.build())
                .add("caseId", caseId)
                .add("courtFormId", courtFormId)
                .add("formType", FormType.PTPH.name())
                .add("userId", userId)
                .add(MATERIAL_ID, UUID.randomUUID().toString());

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_FINALISED)
                        .withUserId(FormEventProcessorTest.userId)
                , payloadBuilder.build());

        when(documentGeneratorService.generateFormDocument(any(), any(), any(), any())).thenReturn(fileName);
        when(usersGroupService.getUserById(any(), any())).thenReturn(USER_DETAILS);
        formEventProcessor.formFinalised(requestEnvelope);
        verify(sender, times(2)).send(envelopeArgumentCaptor.capture());

        final List<JsonEnvelope> envelopes = envelopeArgumentCaptor.getAllValues();

        final JsonEnvelope envelope1 = envelopes.get(0);
        assertAddCourtOrderCommandFromEnvelope(caseId, envelope1);

        final JsonEnvelope envelope4 = envelopes.get(1);
        assertPublicEventCreationFromCapturedEnvelope(courtFormId, caseId, userId, FormType.PTPH, 1, false, envelope4, USER_DETAILS);
    }


    @Test
    public void shouldRaiseFormFinalisedPublicEvent_WhenWelshLanguageIsPresent() throws IOException {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final String fileName = "Jane JOHNSON, 22 May 1976, created on 22 December 10:45 2022.pdf";

        final String inputEvent = Resources.toString(getResource("finalised-form-data-with-welsh-data.json"), defaultCharset())
                .replaceAll("%caseId%", caseId)
                .replaceAll("%courtFormId%", courtFormId)
                .replaceAll("%formType%", FormType.BCM.name())
                .replaceAll("%userId%", userId);
        final JsonObject readData = stringToJsonObjectConverter.convert(inputEvent);
        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        readData.getJsonArray("finalisedFormData").forEach(x -> arrayBuilder.add(x.toString()));
        payloadBuilder.add("finalisedFormData", arrayBuilder.build())
                .add("caseId", caseId)
                .add("courtFormId", courtFormId)
                .add("formType", FormType.BCM.name())
                .add("userId", userId)
                .add(MATERIAL_ID, UUID.randomUUID().toString())
        ;

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_FINALISED)
                        .withUserId(FormEventProcessorTest.userId)
                , payloadBuilder.build());

        when(documentGeneratorService.generateFormDocument(any(), any(), any(), any())).thenReturn(fileName);
        when(usersGroupService.getUserById(any(), any())).thenReturn(USER_DETAILS);
        formEventProcessor.formFinalised(requestEnvelope);
        verify(sender, times(4)).send(envelopeArgumentCaptor.capture());

        final List<JsonEnvelope> envelopes = envelopeArgumentCaptor.getAllValues();

        //first document for defendant 1
        final JsonEnvelope envelope1 = envelopes.get(0);
        assertAddCourtOrderCommandFromEnvelope(caseId, envelope1);

        //second document for defendant 2
        final JsonEnvelope envelope2 = envelopes.get(1);
        assertAddCourtOrderCommandFromEnvelope(caseId, envelope2);

        //third document for defendant 3
        final JsonEnvelope envelope3 = envelopes.get(2);
        assertAddCourtOrderCommandFromEnvelope(caseId, envelope3);

        //single event creation for all documents
        final JsonEnvelope envelope4 = envelopes.get(3);
        assertPublicEventCreationFromCapturedEnvelope(courtFormId, caseId, userId, FormType.BCM, 3, true, envelope4, USER_DETAILS);
    }


    @Test
    public void shouldNotRaiseFormFinalisedPublicEvent_WhenFormIsNotPresentInApi() {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_FINALISED).withUserId(FormEventProcessorTest.userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, courtFormId)
                        .add(FORM_TYPE, FormType.BCM.name())
                        .add(CASE_ID, caseId)
                        .add(USER_ID, userId)
                        .add(MATERIAL_ID,UUID.randomUUID().toString())
        );

        formEventProcessor.formFinalised(requestEnvelope);
        verify(sender, times(0)).send(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldNotRaiseFormFinalisedPublicEvent_WhenFormDataIsEmptyInApi() {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_FINALISED).withUserId(FormEventProcessorTest.userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, courtFormId)
                        .add(FORM_TYPE, FormType.BCM.name())
                        .add(CASE_ID, caseId)
                        .add(FINALISED_FORM_DATA, createArrayBuilder().build())
                        .add(USER_ID, userId)
                        .add(MATERIAL_ID,UUID.randomUUID().toString())
        );

        formEventProcessor.formFinalised(requestEnvelope);
        verify(sender, times(0)).send(envelopeArgumentCaptor.capture());
    }


    private void assertAddCourtOrderCommandFromEnvelope(final String caseId, final JsonEnvelope envelope) {
        assertThat(envelope.metadata().name(), is(PROGRESSION_COMMAND_ADD_COURT_DOCUMENT));
        final JsonObject payload2 = envelope.payloadAsJsonObject();
        assertThat(payload2.getString(MATERIAL_ID), is(notNullValue()));
        final JsonObject courtDocument1 = payload2.getJsonObject(COURT_DOCUMENT);
        assertThat(courtDocument1.getString(COURT_DOCUMENT_ID), is(notNullValue()));
        assertThat(courtDocument1.getJsonObject(DOCUMENT_CATEGORY).getJsonObject(DEFENDANT_DOCUMENT).getString(PROSECUTION_CASE_ID), is(caseId));
        assertThat(courtDocument1.getString(DOCUMENT_TYPE_ID), is(CASE_DOCUMENT_TYPE_ID.toString()));

        if(courtDocument1.containsKey("notificationType")) {
            assertThat(courtDocument1.getString("notificationType"), is("bcm-form-finalised"));
            assertThat(courtDocument1.getBoolean("sendToCps"), is(true));
        }
    }

    private void assertPublicEventCreationFromCapturedEnvelope(final String courtFormId, final String caseId, final String userId, final FormType formType, final int documentListSize, final boolean isWelsh, final JsonEnvelope envelope, final JsonObject userDetails) {
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_FORM_FINALISED));
        final JsonObject payload1 = envelope.payloadAsJsonObject();
        assertThat(payload1.getString(COURT_FORM_ID), is(courtFormId));
        assertThat(payload1.getString(FORM_TYPE), is(formType.name()));
        assertThat(payload1.getString(CASE_ID), is(caseId));
        assertThat(payload1.getJsonArray(DOCUMENT_META_DATA), is(notNullValue()));
        assertThat(payload1.getJsonArray(DOCUMENT_META_DATA), hasSize(documentListSize));
        final List<Boolean> isWelshList = payload1.getJsonArray(DOCUMENT_META_DATA).getValuesAs(JsonObject.class).stream().map(item -> item.getBoolean(IS_WELSH)).collect(Collectors.toList());
        assertThat(isWelshList.contains(isWelsh), is(true));
        final JsonObject updatedBy = payload1.getJsonObject(UPDATED_BY);
        assertThat(updatedBy.getString(FIRST_NAME), is(userDetails.getString(FIRST_NAME)));
        assertThat(updatedBy.getString(LAST_NAME), is(userDetails.getString(LAST_NAME)));
    }

    @Test
    public void shouldRaiseFormUpdatedPublicEvent() {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_UPDATED).withUserId(userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, courtFormId)
                        .add(CASE_ID, caseId)
                        .add(USER_NAME, "userName")
                        .add(FORM_DATA, "formData")
                        .add("formType", FormType.PTPH.toString())
        );

        formEventProcessor.formUpdated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_FORM_UPDATED));
        verifyZeroInteractions(usersGroupService);
    }

    @Test
    public void shouldFetchUserDetailsForCPUserFormUpdatedPublicEvent() {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_UPDATED).withUserId(userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, courtFormId)
                        .add(CASE_ID, caseId)
                        .add(USER_ID, "userId")
                        .add(FORM_DATA, "formData")
                        .add("formType", FormType.PTPH.toString())
        );
        when(usersGroupService.getUserById(any(), any())).thenReturn(USER_DETAILS);

        formEventProcessor.formUpdated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_FORM_UPDATED));
        verify(usersGroupService).getUserById(any(), eq("userId"));
    }

    @Test
    public void shouldRaisePublicBcmDefendantsUpdated() {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_BCM_DEFENDANTS_UPDATED).withUserId(userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, courtFormId)
                        .add(CASE_ID, caseId)
        );

        formEventProcessor.bcmDefendantsUpdated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_BCM_DEFENDANTS_UPDATED));
    }


    @Test
    public void shouldRaiseEditFormRequestedPublicEvent_whenFormIsLocked() {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String lockedByUserId = randomUUID().toString();
        final String lockRequestedByUserId = randomUUID().toString();

        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenAnswer(invocationOnMock -> {
            final Envelope envelope = (Envelope) invocationOnMock.getArguments()[0];
            final JsonObject responsePayload = createObjectBuilder()
                    .add(FIRST_NAME, "firstName")
                    .add(LAST_NAME, "lastName")
                    .add(EMAIL, "email@email.com")
                    .build();

            return envelopeFrom(envelope.metadata(), responsePayload);
        });

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_EDIT_FORM_REQUESTED).withUserId(userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, courtFormId)
                        .add(CASE_ID, caseId)
                        .add(LOCK_STATUS, createObjectBuilder()
                                .add(EXPIRY_TIME, ZonedDateTime.now().toInstant().toString())
                                .add(IS_LOCKED, true)
                                .add(LOCKED_BY, lockedByUserId)
                                .add(LOCK_REQUESTED_BY, lockRequestedByUserId)
                                .build())
        );

        formEventProcessor.formEditRequested(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_EDIT_FORM_REQUESTED));
        final JsonObject publicEventPayload = envelope.payloadAsJsonObject();
        assertThat(publicEventPayload, notNullValue());
        assertThat(publicEventPayload.getString(CASE_ID), is(caseId));
        assertThat(publicEventPayload.getString(COURT_FORM_ID), is(courtFormId));
        final JsonObject lockedStatusObject = publicEventPayload.getJsonObject(LOCK_STATUS);
        assertThat(lockedStatusObject, notNullValue());
        assertThat(lockedStatusObject.getBoolean(IS_LOCKED), is(true));
        final JsonObject lockRequestedByObject = lockedStatusObject.getJsonObject(LOCK_REQUESTED_BY);
        assertThat(lockRequestedByObject, notNullValue());
        assertThat(lockRequestedByObject.getString(USER_ID), is(lockRequestedByUserId));
        final JsonObject lockedByObject = lockedStatusObject.getJsonObject(LOCKED_BY);
        assertThat(lockedByObject, notNullValue());
        assertThat(lockedByObject.getString(USER_ID), is(lockedByUserId));
        assertThat(lockedByObject.getString(FIRST_NAME), is("firstName"));
        assertThat(lockedByObject.getString(LAST_NAME), is("lastName"));
        assertThat(lockedByObject.getString(EMAIL), is("email@email.com"));
    }


    @Test
    public void shouldRaiseEditFormRequestedPublicEvent_whenFormIsNotLocked() {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_EDIT_FORM_REQUESTED).withUserId(userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, courtFormId)
                        .add(CASE_ID, caseId)
                        .add(LOCK_STATUS, createObjectBuilder()
                                .add(IS_LOCKED, false)
                                .build())
        );

        formEventProcessor.formEditRequested(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_EDIT_FORM_REQUESTED));
        final JsonObject publicEventPayload = envelope.payloadAsJsonObject();
        assertThat(publicEventPayload, notNullValue());
        assertThat(publicEventPayload.getString(CASE_ID), is(caseId));
        assertThat(publicEventPayload.getString(COURT_FORM_ID), is(courtFormId));
        final JsonObject lockedStatusObject = publicEventPayload.getJsonObject(LOCK_STATUS);
        assertThat(lockedStatusObject, notNullValue());
        assertThat(lockedStatusObject.getBoolean(IS_LOCKED), is(false));
        assertThat(lockedStatusObject.containsKey(LOCKED_BY), is(true));
        assertThat(lockedStatusObject.containsKey(LOCK_REQUESTED_BY), is(false));
    }


    public void shouldSendBcmNotificationOnFormCreation() throws IOException {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String caseUrn = "caseUrn123";
        final String prosecutionAuthorityReference = "paf123";
        final String prosecutionAuthorityOUCode = "ouCode123";
        final UUID DEF_ID1 = randomUUID();
        final UUID DEF_ID2 = randomUUID();
        final UUID cpsDefendantId1 = randomUUID();
        final UUID cpsDefendantId2 = randomUUID();

        final String formData = Resources.toString(getResource("formData.json"), defaultCharset())
                .replace("DEF_ID1",DEF_ID1.toString())
                .replace("DEF_ID2",DEF_ID2.toString());

        when(usersGroupService.getUserById(any(), any())).thenReturn(USER_DETAILS);

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_FORM_CREATED).withUserId(FormEventProcessorTest.userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, courtFormId)
                        .add(CASE_ID, caseId)
                        .add(FORM_TYPE, "BCM")
                        .add(FORM_DEFENDANTS, createArrayBuilder().add(
                                createObjectBuilder().add("defendantId", DEF_ID1.toString())
                        ))
                        .add(FORM_DATA,formData)
                        .add(USER_ID, userId)
        );

        final JsonObject defenceJsonObject = createObjectBuilder().add("caseId",randomUUID().toString())
                .add("assigneeId", randomUUID().toString())
                .add("isAdvocateDefendingOrProsecuting", "defending")
                .build();

        when(defenceService.getRoleInCaseByCaseId(any(),any())).thenReturn(defenceJsonObject);
        when(response.getStatus()).thenReturn(200);

        final JsonObject prosecutionCaseJsonObject = createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                .add("id", caseId)
                .add("prosecutionCaseIdentifier", createObjectBuilder()
                        .add("caseURN" , caseUrn)
                        .add("prosecutionAuthorityReference", prosecutionAuthorityReference)
                        .add("prosecutionAuthorityOUCode", prosecutionAuthorityOUCode)
                        .build())
                .build())
                .build();
        when(progressionService.getProsecutionCaseDetailById(any(),any())).thenReturn(java.util.Optional.ofNullable(prosecutionCaseJsonObject));
        when(payload.getJsonObject(any())).thenReturn(prosecutionCaseJsonObject);

        final List<Offence> offences = new ArrayList<>();
        final Offence offence1 = Offence.offence()
                .withOffenceCode("offenceCode1")
                .withOffenceTitle("OffenceTitle1")
                .withPlea(Plea.plea()
                        .withPleaValue("PleaValue1")
                        .build())
                .build();
        offences.add(offence1);

        final Offence offence2 = Offence.offence()
                .withOffenceCode("offenceCode2")
                .withOffenceTitle("OffenceTitle2")
                .withPlea(Plea.plea()
                        .withPleaValue("PleaValue2")
                        .build())
                .build();
        offences.add(offence2);

        final List<Defendant> defendants = new ArrayList<>();
        final Defendant def1 = Defendant.defendant()
                .withId(DEF_ID1)
                .withCpsDefendantId(cpsDefendantId1.toString())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withArrestSummonsNumber("arrestSummonsNo1")
                        .build())
                .withOffences(offences)
                .build();
        defendants.add(def1);

        final ProsecutionCase prosecutionCase1 = ProsecutionCase.prosecutionCase()
                .withId(UUID.fromString(caseId))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN(caseUrn)
                        .withProsecutionAuthorityReference(prosecutionAuthorityReference)
                        .withProsecutionAuthorityOUCode(prosecutionAuthorityOUCode)
                        .build())
                .withDefendants(defendants)
                .build();
        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(prosecutionCase1);

        formEventProcessor.formCreated(requestEnvelope);
        verify(cpsApiService, times(1)).sendNotification(any());
    }

    @Test
    public void shouldNotSendBcmNotificationOnFormCreationIfMandatoryParamsNotPresent() throws IOException {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String caseUrn = "caseUrn123";
        final String prosecutionAuthorityReference = "paf123";
        final String prosecutionAuthorityOUCode = "ouCode123";
        final UUID DEF_ID1 = randomUUID();
        final UUID DEF_ID2 = randomUUID();

        final String formData = Resources.toString(getResource("formData.json"), defaultCharset())
                .replace("DEF_ID1",DEF_ID1.toString())
                .replace("DEF_ID2",DEF_ID2.toString());

        when(usersGroupService.getUserById(any(), any())).thenReturn(USER_DETAILS);

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_FORM_CREATED).withUserId(FormEventProcessorTest.userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, courtFormId)
                        .add(CASE_ID, caseId)
                        .add(FORM_TYPE, "BCM")
                        .add(FORM_DEFENDANTS, createArrayBuilder().build())
                        .add(FORM_DATA,formData)
                        .add(USER_ID, userId)
        );

        final JsonObject defenceJsonObject = createObjectBuilder().add("caseId",randomUUID().toString())
                .add("assigneeId", randomUUID().toString())
                .add("isAdvocateDefendingOrProsecuting", "defending")
                .build();

        when(defenceService.getRoleInCaseByCaseId(any(),any())).thenReturn(defenceJsonObject);
        when(response.getStatus()).thenReturn(200);

        final JsonObject prosecutionCaseJsonObject = createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                .add("id", caseId)
                .add("prosecutionCaseIdentifier", createObjectBuilder()
                        .add("caseURN" , caseUrn)
                        .add("prosecutionAuthorityReference", prosecutionAuthorityReference)
                        .add("prosecutionAuthorityOUCode", prosecutionAuthorityOUCode)
                        .build())
                .build())
                .build();
        when(progressionService.getProsecutionCaseDetailById(any(),any())).thenReturn(java.util.Optional.ofNullable(prosecutionCaseJsonObject));
        when(payload.getJsonObject(any())).thenReturn(prosecutionCaseJsonObject);

        final List<Offence> offences = new ArrayList<>();
        final Offence offence1 = Offence.offence()
                .withOffenceCode("offenceCode1")
                .withOffenceTitle("OffenceTitle1")
                .withPlea(Plea.plea()
                        .withPleaValue("PleaValue1")
                        .build())
                .build();
        offences.add(offence1);

        final List<Defendant> defendants = new ArrayList<>();
        final Defendant def1 = Defendant.defendant()
                .withOffences(offences)
                .build();
        defendants.add(def1);

        final ProsecutionCase prosecutionCase1 = ProsecutionCase.prosecutionCase()
                .withId(UUID.fromString(caseId))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN(caseUrn)
                        .withProsecutionAuthorityReference(prosecutionAuthorityReference)
                        .withProsecutionAuthorityOUCode(prosecutionAuthorityOUCode)
                        .build())
                .withDefendants(defendants)
                .build();
        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(prosecutionCase1);

        formEventProcessor.formCreated(requestEnvelope);
        verify(cpsApiService, times(0)).sendNotification(any());
    }

    @Test
    public void shouldHandleServeBcmSubmittedPublicEvent() {

        final String payload = getPayload("cps-serve-bcm-submitted.json");
        final JsonObject jsonPayload = jsonFromString(payload);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("public.prosecutioncasefile.cps-serve-bcm-submitted"),
                jsonPayload);
        formEventProcessor.handleServeFormSubmittedPublicEvent(envelope);
        verify(sender, atLeastOnce()).send(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldHandleServeBcmSubmittedPublicEvent_RaiseUpdateCpsDefendantIdCommand() {

        final String payload = getPayload("cps-serve-bcm-submitted.json");
        final JsonObject jsonPayload = jsonFromString(payload);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("public.prosecutioncasefile.cps-serve-bcm-submitted"),
                jsonPayload);
        formEventProcessor.handleServeFormSubmittedPublicEvent(envelope);
        verify(sender, times(2)).send(envelopeArgumentCaptor.capture());
        final List<JsonEnvelope> envelopeList = envelopeArgumentCaptor.getAllValues();
        assertThat(((Envelope) envelopeList.get(0)).metadata().name(), is("progression.command.create-form"));
        verifyUpdateCpsDefendantIdCommand("1a8fe782-a287-11eb-bcbc-0242ac130077", "071e108d-8a70-4532-b7da-2168d0260d08", "2ee7407e-ed91-41fa-8409-db373ab486a0", ((Envelope) envelopeList.get(1)));
    }

    @Test
    public void shouldSendBcmNotificationOnFormUpdation() throws IOException {
        final String courtFormId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String caseUrn = "caseUrn123";
        final String prosecutionAuthorityReference = "paf123";
        final String prosecutionAuthorityOUCode = "ouCode123";
        final UUID DEF_ID1 = randomUUID();
        final UUID DEF_ID2 = randomUUID();

        final String formData = Resources.toString(getResource("formDataOnUpdate.json"), defaultCharset())
                .replace("DEF_ID1",DEF_ID1.toString())
                .replace("DEF_ID2",DEF_ID2.toString());

        when(usersGroupService.getUserById(any(), any())).thenReturn(USER_DETAILS);
        when(usersGroupService.isUserPartOfGroup(any(), any())).thenReturn(true);

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_FORM_UPDATED).withUserId(userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, courtFormId)
                        .add(CASE_ID, caseId)
                        .add(USER_NAME, "userName")
                        .add(FORM_DATA, formData)
                        .add(FORM_TYPE, FormType.BCM.toString())
        );

        final JsonObject defenceJsonObject = createObjectBuilder().add("caseId",randomUUID().toString())
                .add("assigneeId", randomUUID().toString())
                .add("isAdvocateDefendingOrProsecuting", "defending")
                .build();

        when(defenceService.getRoleInCaseByCaseId(any(),any())).thenReturn(defenceJsonObject);
        when(response.getStatus()).thenReturn(200);

        final JsonObject prosecutionCaseJsonObject = createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                .add("id", caseId)
                .add("prosecutionCaseIdentifier", createObjectBuilder()
                        .add("caseURN" , caseUrn)
                        .add("prosecutionAuthorityReference", prosecutionAuthorityReference)
                        .add("prosecutionAuthorityOUCode", prosecutionAuthorityOUCode)
                        .build())
                .build())
                .build();
        when(progressionService.getProsecutionCaseDetailById(any(),any())).thenReturn(java.util.Optional.ofNullable(prosecutionCaseJsonObject));
        when(payload.getJsonObject(any())).thenReturn(prosecutionCaseJsonObject);

        final List<Defendant> defendants = new ArrayList<>();
        final Defendant def1 = Defendant.defendant()
                .withId(DEF_ID1)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withArrestSummonsNumber("arrestSummonsNo1")
                        .build())
                .build();
        defendants.add(def1);

        final ProsecutionCase prosecutionCase1 = ProsecutionCase.prosecutionCase()
                .withId(UUID.fromString(caseId))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN(caseUrn)
                        .withProsecutionAuthorityReference(prosecutionAuthorityReference)
                        .withProsecutionAuthorityOUCode(prosecutionAuthorityOUCode)
                        .build())
                .withDefendants(defendants)
                .build();
        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(prosecutionCase1);

        formEventProcessor.formUpdated(requestEnvelope);
        final ArgumentCaptor<JsonObject> payload = ArgumentCaptor.forClass(JsonObject.class);
        verify(cpsApiService, times(1)).sendNotification(payload.capture());
    }

    @Test
    public void shouldHandleServePtphSubmittedPublicEvent() {

        final String payload = getPayload("cps-serve-ptph-submitted.json");
        final JsonObject jsonPayload = jsonFromString(payload);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("public.prosecutioncasefile.cps-serve-ptph-submitted"),
                jsonPayload);
        formEventProcessor.handleServePtphFormSubmittedPublicEvent(envelope);
        verify(sender, atLeastOnce()).send(envelopeArgumentCaptor.capture());
        final Envelope publicEvent = envelopeArgumentCaptor.getAllValues().get(0);
        assertThat(publicEvent.metadata().name(), is("progression.command.create-form"));
    }

    @Test
    public void shouldHandleServePtphSubmittedPublicEvent_RaiseUpdateCpsDefendantIdCommand() {

        final String payload = getPayload("cps-serve-ptph-submitted.json");
        final JsonObject jsonPayload = jsonFromString(payload);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("public.prosecutioncasefile.cps-serve-ptph-submitted"),
                jsonPayload);
        formEventProcessor.handleServePtphFormSubmittedPublicEvent(envelope);
        verify(sender, times(2)).send(envelopeArgumentCaptor.capture());
        List<JsonEnvelope> envelopeList = envelopeArgumentCaptor.getAllValues();
        final Envelope publicEvent = envelopeList.get(0);
        assertThat(publicEvent.metadata().name(), is("progression.command.create-form"));
        verifyUpdateCpsDefendantIdCommand("1a8fe782-a287-11eb-bcbc-0242ac130077", "071e108d-8a70-4532-b7da-2168d0260d08", "2ee7407e-ed91-41fa-8409-db373ab486a0", ((Envelope) envelopeList.get(1)));
    }

    private void verifyUpdateCpsDefendantIdCommand(final String cpsDefendantId1, final String caseId, final String defendantId1, final Envelope envelope) {
        assertThat(envelope.metadata().name(), CoreMatchers.is(PROGRESSION_COMMAND_UPDATE_CPS_DEFENDANT_ID));
        UpdateCpsDefendantId payload = (UpdateCpsDefendantId) envelope.payload();
        assertThat(payload.getCaseId().toString(), CoreMatchers.is(caseId));
        assertThat(payload.getDefendantId().toString(), CoreMatchers.is(defendantId1));
        assertThat(payload.getCpsDefendantId().toString(), CoreMatchers.is(cpsDefendantId1));
    }
}
