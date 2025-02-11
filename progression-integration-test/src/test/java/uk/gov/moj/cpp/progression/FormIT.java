package uk.gov.moj.cpp.progression;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.core.courts.FormType.BCM;
import static uk.gov.justice.core.courts.FormType.PTPH;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.PetFormIT.DATA;
import static uk.gov.moj.cpp.progression.PetFormIT.NAME;
import static uk.gov.moj.cpp.progression.PetFormIT.UPDATED_BY;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatRequestIsAccepted;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.stubMaterialStructuredFormQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubEndpoint;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubAdvocateRoleInCaseByCaseId;

import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.CpsServeMaterialHelper;
import uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.io.Resources;
import io.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FormIT extends AbstractIT {

    private static final String CREATE_FORM_ENDPOINT = "/prosecutioncases/%caseId%/form";
    private static final String UPDATE_FORM_ENDPOINT = "/prosecutioncases/%caseId%/form/%courtFormId%";
    private static final String FINALISE_FORM_ENDPOINT = "/prosecutioncases/%caseId%/form/%courtFormId%";
    private static final String UPDATE_BCM_DEFENDANTS_ENDPOINT = "/prosecutioncases/%caseId%/form/%courtFormId%";
    private static final String EDIT_FORM_ENDPOINT = "/prosecutioncases/%caseId%/form/%courtFormId%";

    private static final String CREATE_FORM_MEDIA_TYPE = "application/vnd.progression.create-form+json";
    private static final String UPDATE_FORM_MEDIA_TYPE = "application/vnd.progression.update-form+json";
    private static final String REQUEST_EDIT_FORM_MEDIA_TYPE = "application/vnd.progression.request-edit-form+json";
    private static final String FINALISE_FORM_MEDIA_TYPE = "application/vnd.progression.finalise-form+json";
    private static final String UPDATE_BCM_DEFENDANTS_MEDIA_TYPE = "application/vnd.progression.update-form-defendants+json";
    private static final String PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_BCM_SUBMITTED = "public.prosecutioncasefile.cps-serve-bcm-submitted";
    private static final String PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PTPH_SUBMITTED = "public.prosecutioncasefile.cps-serve-ptph-submitted";

    private static final String FORM_EDIT_COMMAND_NAME = "edit-form";
    private static final String FORM_CREATION_COMMAND_NAME = "form-created";
    private static final String FORM_UPDATE_COMMAND_NAME = "update-form";
    private static final String MESSAGE_FOR_DUPLICATE_COURT_FORM_ID = "courtFormId already exists";
    private static final String MESSAGE_FOR_COURT_FORM_ID_NOT_PRESENT = "courtFormId (%s) does not exists.";
    private static final String UPDATE_BCM_DEFENDANT_OPERATION_IS_FAILED = "update BCM defendant operation is failed";
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    public static final String FORM_DATA = "formData";
    public static final String SUBMISSION_ID = "submissionId";
    public static final String CASE_ID = "caseId";
    public static final String FORM_DEFENDANTS = "formDefendants";
    public static final String COURT_FORM_ID = "courtFormId";
    public static final String USER_NAME_VALUE = "ptph user name";


    private static final String UPDATED_FORM_DATA = createObjectBuilder()
            .add("name", "updated name")
            .add("offence", "burglary")
            .build().toString();

    public static final String USERS_GROUPS_SERVICE_NAME = "usergroups-service";
    private static final String GROUPS_FOR_LOGGED_IN_USER_MEDIA_TYPE =
            "application/vnd.usersgroups.get-logged-in-user-groups+json";
    public static final String BASE_QUERY = "/usersgroups-service/query/api/rest/usersgroups";
    public static final String GROUPS_BY_LOGGEDIN_USER = "/users/logged-in-user/groups";
    public static final String GET_GROUPS_BY_LOGGEDIN_USER_QUERY = BASE_QUERY + GROUPS_BY_LOGGEDIN_USER;

    public static final String SUBMISSION_ID_VALUE = "e85d2c62-af1f-4674-863a-0891e67e325b";

    private final JmsMessageConsumerClient consumerForFormCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.form-created").getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForFormFinalised = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.form-finalised").getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForPublicCourtDocumentAdded = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.court-document-added").getMessageConsumerClient();

    private final JmsMessageConsumerClient consumerForFormOperationFailed = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.form-operation-failed").getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForFormUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.form-updated").getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForEditFormRequested = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.edit-form-requested").getMessageConsumerClient();
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @BeforeAll
    public static void setUpClass() {
        setupLoggedInUsersPermissionQueryStub();
        stubMaterialStructuredFormQuery(UPDATED_FORM_DATA);
    }

    @Test
    public void shouldVerify_Create_Update_Edit_Finalise_VerifyFormEvents() throws IOException, JSONException {
        final UUID courtFormId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant2 = randomUUID();
        final UUID formId = randomUUID();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId.toString(), defendant1.toString());
        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId.toString(), defendant1.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].offences[*].id",
                                hasItems("3789ab16-0bb7-4ef1-87ef-c936bf0364f1", "4789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                )
        );

        final String responseForCaseQuery = pollProsecutionCasesProgressionFor(caseId.toString(), caseWithOffenceMatchers);
        final JsonObject caseObject = stringToJsonObjectConverter.convert(responseForCaseQuery);

        final String offenceId = caseObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("id");

        //create form
        final JsonObject payload = buildPayloadForCreateFormApi(courtFormId, formId, BCM, defendant1, offenceId);
        final String createFormEndpointUrl = CREATE_FORM_ENDPOINT.replaceAll("%caseId%", caseId.toString());
        final Response responseForCreateForm1 = postCommand(getWriteUrl(createFormEndpointUrl), CREATE_FORM_MEDIA_TYPE, payload.toString());
        assertThatRequestIsAccepted(responseForCreateForm1);
        verifyInMessagingQueueForFormCreated();
        queryAndVerifyFormDetailWithCaseId(courtFormId, caseId, defendant1, BCM);

        // Create another form and Verify the no of forms created
        final JsonObject payload1 = buildPayloadForCreateFormApi(randomUUID(), randomUUID(), BCM, defendant2, offenceId);
        final Response responseForCreateForm2 = postCommand(getWriteUrl(createFormEndpointUrl), CREATE_FORM_MEDIA_TYPE, payload1.toString());
        assertThatRequestIsAccepted(responseForCreateForm2);

        verifyInMessagingQueueForFormCreated();
        queryAndVerifyNoOfForms(caseId, 2);

        // update form defendants
        final String updateBcmDefendantsEndpointUrl = UPDATE_BCM_DEFENDANTS_ENDPOINT.replaceAll("%caseId%", caseId.toString()).replaceAll("%courtFormId%", courtFormId.toString());
        final JsonObject updateBcmDefendantsPayload = buildPayloadForUpdateBcmDefendantsApi(courtFormId, BCM, defendant1, offenceId);


        final JmsMessageConsumerClient consumerForBcmDefendantsUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.event.form-defendants-updated").getMessageConsumerClient();
        final Response responseForCreateFormFailed = postCommand(getWriteUrl(updateBcmDefendantsEndpointUrl), UPDATE_BCM_DEFENDANTS_MEDIA_TYPE, updateBcmDefendantsPayload.toString());
        assertThatRequestIsAccepted(responseForCreateFormFailed);

        final JsonObject BcmDefendantsUpdated = verifyInMessagingQueue(consumerForBcmDefendantsUpdated);
        assertThat(BcmDefendantsUpdated.getString("courtFormId"), is(courtFormId.toString()));
        assertThat(BcmDefendantsUpdated.getString("formType"), is(BCM.name()));

        queryAndVerifyForm(courtFormId, caseId, defendant1, offenceId);

        // update form
        final JsonObject payloadForUpdate = createObjectBuilder()
                .add("formData", UPDATED_FORM_DATA).build();
        final String updateFormEndpointUrl = UPDATE_FORM_ENDPOINT.replaceAll("%caseId%", caseId.toString()).replaceAll("%courtFormId%", courtFormId.toString());
        final Response responseForUpdateForm = postCommand(getWriteUrl(updateFormEndpointUrl), UPDATE_FORM_MEDIA_TYPE, payloadForUpdate.toString());

        assertThatRequestIsAccepted(responseForUpdateForm);
        verifyInMessagingQueueForFormUpdated();

        // verify saved form data via query
        queryAndVerifyForm(courtFormId, caseId, defendant1, offenceId);

        //edit form to check lock status
        //when form is unlocked
        final UUID userId = randomUUID();
        UsersAndGroupsStub.stubGetUsersAndGroupsUserDetailsQuery(userId.toString());
        final String editFormEndpointUrl = EDIT_FORM_ENDPOINT
                .replaceAll("%caseId%", caseId.toString())
                .replaceAll("%courtFormId%", courtFormId.toString());
        final Response responseForEditForm = postCommandWithUserId(getWriteUrl(editFormEndpointUrl), REQUEST_EDIT_FORM_MEDIA_TYPE,
                createObjectBuilder()
                        .build()
                        .toString(),
                userId.toString());
        assertThatRequestIsAccepted(responseForEditForm);

        JsonObject editRequestedEvent = verifyInMessagingQueueForEditFormRequested();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, null, null, false, editRequestedEvent);

        //when form is locked
        final UUID userId2 = randomUUID();
        final Response responseForEditForm2 = postCommandWithUserId(getWriteUrl(editFormEndpointUrl), REQUEST_EDIT_FORM_MEDIA_TYPE,
                createObjectBuilder()
                        .build()
                        .toString(),
                userId2.toString());
        assertThatRequestIsAccepted(responseForEditForm2);

        JsonObject editRequestedEvent2 = verifyInMessagingQueueForEditFormRequested();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId2, true, editRequestedEvent2);

        //finalise form
        final JsonObject finalisePayload = buildPayloadForFinaliseFormApi(defendant1.toString());
        final String finaliseFormEndpointUrl = FINALISE_FORM_ENDPOINT
                .replaceAll("%caseId%", caseId.toString())
                .replaceAll("%courtFormId%", courtFormId.toString());
        final Response responseForFinaliseForm = postCommand(getWriteUrl(finaliseFormEndpointUrl), FINALISE_FORM_MEDIA_TYPE, finalisePayload.toString());
        assertThatRequestIsAccepted(responseForFinaliseForm);

        verifyInMessagingQueueForFormFinalised();
        final JsonObject jsonObject = verifyInMessagingQueueForAddCourtDocument();
        assertThat(jsonObject.getJsonObject("courtDocument").getBoolean("sendToCps"), is(true));
    }


    @Test
    public void verifyFormOperationFailedPublicEvent_WhenForm_Create_Update_Edit_Finalise() throws IOException, JSONException {
        final UUID courtFormId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId.toString(), defendantId.toString());
        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].offences[*].id",
                                hasItems("3789ab16-0bb7-4ef1-87ef-c936bf0364f1", "4789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                )
        );

        final String responseForCaseQuery = pollProsecutionCasesProgressionFor(caseId.toString(), caseWithOffenceMatchers);
        final JsonObject caseObject = stringToJsonObjectConverter.convert(responseForCaseQuery);

        final String offenceId = caseObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("id");

        final JsonObject payload = buildPayloadForCreateFormApi(courtFormId, formId, BCM, defendantId, offenceId);

        final String createFormEndpointUrl = CREATE_FORM_ENDPOINT.replaceAll("%caseId%", caseId.toString());
        final Response responseForCreateForm = postCommand(getWriteUrl(createFormEndpointUrl), CREATE_FORM_MEDIA_TYPE, payload.toString());
        assertThatRequestIsAccepted(responseForCreateForm);

        verifyInMessagingQueueForFormCreated();
        queryAndVerifyFormDetailsWithCaseIdAndFormType(courtFormId, caseId, BCM, defendantId);

        //Form operation failed event when curt form id is already present
        final UUID defendantId2 = randomUUID();
        final UUID formId2 = randomUUID();
        final JsonObject duplicateCourtFormIdPayload = buildPayloadForCreateFormApi(courtFormId, formId2, BCM, defendantId2, offenceId);

        final Response responseForCreateFormFailed = postCommand(getWriteUrl(createFormEndpointUrl), CREATE_FORM_MEDIA_TYPE, duplicateCourtFormIdPayload.toString());
        assertThatRequestIsAccepted(responseForCreateFormFailed);

        final JsonObject failureEvent = verifyInMessagingQueueForFormOperationsFailed();
        assertFormOperationFailedEvent(courtFormId, failureEvent, MESSAGE_FOR_DUPLICATE_COURT_FORM_ID, FORM_CREATION_COMMAND_NAME, BCM);

        //Form operation failed event when form edit failed due to form not present
        final UUID userId = randomUUID();
        final UUID randomCourtFormId = randomUUID();
        final String editFormEndpointUrl = EDIT_FORM_ENDPOINT
                .replaceAll("%caseId%", caseId.toString())
                .replaceAll("%courtFormId%", randomCourtFormId.toString());
        final Response responseForEditForm = postCommandWithUserId(getWriteUrl(editFormEndpointUrl), REQUEST_EDIT_FORM_MEDIA_TYPE,
                createObjectBuilder()
                        .build()
                        .toString(),
                userId.toString());
        assertThatRequestIsAccepted(responseForEditForm);

        JsonObject editRequestedEvent = verifyInMessagingQueueForFormOperationsFailed();
        assertFormOperationFailedEvent(randomCourtFormId, editRequestedEvent,
                format(MESSAGE_FOR_COURT_FORM_ID_NOT_PRESENT, randomCourtFormId), FORM_EDIT_COMMAND_NAME, null);

        //Form operation failed event when form update failed for defendants
        final UUID updateDefendantId = randomUUID();
        final UUID updateCourtFormId = randomUUID();
        final String updateBcmDefendantsEndpointUrl = UPDATE_BCM_DEFENDANTS_ENDPOINT.replaceAll("%caseId%", caseId.toString()).replaceAll("%courtFormId%", updateCourtFormId.toString());
        final JsonObject updateBcmDefendantsPayload = buildPayloadForUpdateBcmDefendantsApi(updateCourtFormId, BCM, updateDefendantId, offenceId);

        final Response responseForFormUpdateFailed = postCommand(getWriteUrl(updateBcmDefendantsEndpointUrl), UPDATE_BCM_DEFENDANTS_MEDIA_TYPE, updateBcmDefendantsPayload.toString());
        assertThatRequestIsAccepted(responseForFormUpdateFailed);

        final JsonObject formUpdateFailureEvent = verifyInMessagingQueueForFormOperationsFailed();
        assertFormOperationFailedEvent(updateCourtFormId, formUpdateFailureEvent, UPDATE_BCM_DEFENDANT_OPERATION_IS_FAILED, FORM_UPDATE_COMMAND_NAME, BCM);

        //Form operation failed event when form finalise failed
        final JsonObject finalisePayload = buildPayloadForFinaliseFormApi(defendantId.toString());
        final String finaliseFormEndpointUrl = FINALISE_FORM_ENDPOINT
                .replaceAll("%caseId%", randomUUID().toString())
                .replaceAll("%courtFormId%", courtFormId.toString());
        final Response responseForFinaliseForm = postCommand(getWriteUrl(finaliseFormEndpointUrl), FINALISE_FORM_MEDIA_TYPE, finalisePayload.toString());
        assertThatRequestIsAccepted(responseForFinaliseForm);

        verifyInMessagingQueueForFormOperationsFailed();
    }

    @Test
    public void shouldSubmitCpsBCMForm() throws IOException, JSONException {

        final UUID caseId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID cpsDefendantId1 = randomUUID();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId.toString(), defendantId1.toString());
        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", CoreMatchers.is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[1].id", CoreMatchers.is("4789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                )

        );

        pollProsecutionCasesProgressionFor(caseId.toString(), caseWithOffenceMatchers);

        final CpsServeMaterialHelper cpsServeMaterialHelper = new CpsServeMaterialHelper();

        final JsonObject cpsServeBcmSubmittedPublicEvent = buildPayloadForCpsServeBcmSubmitted(caseId.toString(),
                defendantId1.toString(), defendantId2.toString(), cpsDefendantId1.toString());

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_BCM_SUBMITTED, randomUUID()), cpsServeBcmSubmittedPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_BCM_SUBMITTED, publicEventEnvelope);

        final JsonEnvelope publicEvent = cpsServeMaterialHelper.getFormCreatedPublicEvent();
        assertThat(publicEvent, is(notNullValue()));
        final JsonObject eventPayload = publicEvent.payloadAsJsonObject();
        assertThat(eventPayload.getString(CASE_ID), is(notNullValue()));
        assertThat(eventPayload.getJsonArray(FORM_DEFENDANTS), is(notNullValue()));
        assertThat(eventPayload.getString(FORM_DATA), is(notNullValue()));
        assertTrue(eventPayload.getString(FORM_DATA).contains(DATA));

        final Matcher[] caseWithCpsDefendantIdMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].cpsDefendantId", CoreMatchers.is(cpsDefendantId1.toString()))
                )
        );

        pollProsecutionCasesProgressionFor(caseId.toString(), caseWithCpsDefendantIdMatchers);
    }

    @Test
    public void shouldSubmitCpsPTPHForm() throws IOException, JSONException {

        final UUID caseId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID cpsDefendantId1 = randomUUID();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId.toString(), defendantId1.toString());
        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", CoreMatchers.is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[1].id", CoreMatchers.is("4789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                )

        );

        pollProsecutionCasesProgressionFor(caseId.toString(), caseWithOffenceMatchers);

        final CpsServeMaterialHelper cpsServeMaterialHelper = new CpsServeMaterialHelper();

        final JsonObject cpsServePtphSubmittedPublicEvent = buildPayloadFor("public.prosecutioncasefile.cps-serve-ptph-submitted.json",
                caseId.toString(), defendantId1.toString(), defendantId2.toString(), cpsDefendantId1.toString());

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PTPH_SUBMITTED, randomUUID()), cpsServePtphSubmittedPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PTPH_SUBMITTED, publicEventEnvelope);

        final JsonEnvelope publicEvent = cpsServeMaterialHelper.getFormCreatedPublicEvent();
        assertThat(publicEvent, is(notNullValue()));
        final JsonObject eventPayload = publicEvent.payloadAsJsonObject();
        assertThat(eventPayload.getString(CASE_ID), is(notNullValue()));
        assertThat(eventPayload.getJsonArray(FORM_DEFENDANTS), is(notNullValue()));
        assertTrue(eventPayload.getString(FORM_DATA).contains(DATA));
        assertTrue(eventPayload.getString(SUBMISSION_ID).contains(SUBMISSION_ID_VALUE));
        assertThat(eventPayload.getString("formType"), is(PTPH.name()));

        JsonObject updatedBy = eventPayload.getJsonObject(UPDATED_BY);
        assertThat(updatedBy.getString(NAME), is(USER_NAME_VALUE));

        final Matcher[] caseWithCpsDefendantIdMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].cpsDefendantId", CoreMatchers.is(cpsDefendantId1.toString()))
                )
        );

        pollProsecutionCasesProgressionFor(caseId.toString(), caseWithCpsDefendantIdMatchers);
    }

    @Test
    public void shouldVerifyBCMNotificationOnUpdate() throws IOException, JSONException {
        final UUID courtFormId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant2 = randomUUID();
        final UUID formId = randomUUID();
        final UUID userId = randomUUID();

        stubGetAdvocatesGroupsForLoggedInQuery(userId.toString());

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId.toString(), defendant1.toString());
        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId.toString(), defendant1.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].offences[*].id",
                                hasItems("3789ab16-0bb7-4ef1-87ef-c936bf0364f1", "4789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                )
        );

        final String responseForCaseQuery = pollProsecutionCasesProgressionFor(caseId.toString(), caseWithOffenceMatchers);
        final JsonObject caseObject = stringToJsonObjectConverter.convert(responseForCaseQuery);

        final String offenceId = caseObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("id");

        //create form
        final JsonObject payload = buildPayloadForCreateFormApi(courtFormId, formId, BCM, defendant1, offenceId);
        final String createFormEndpointUrl = CREATE_FORM_ENDPOINT.replaceAll("%caseId%", caseId.toString());
        final Response responseForCreateForm1 = postCommand(getWriteUrl(createFormEndpointUrl), CREATE_FORM_MEDIA_TYPE, payload.toString());
        assertThatRequestIsAccepted(responseForCreateForm1);

        verifyInMessagingQueueForFormCreated();
        queryAndVerifyFormDetailWithCaseId(courtFormId, caseId, defendant1, BCM);

        final String userRoleInCase = getPayload("stub-data/defence.advocate.query.role-in-case-by-caseid.json")
                .replace("%CASE_ID%", caseId.toString())
                .replace("%USER_ROLE_IN_CASE%", "defending");

        stubAdvocateRoleInCaseByCaseId(caseId.toString(), userRoleInCase);

        // update form
        final String formData = getPayload("formDataOnUpdate.json")
                .replace("DEF_ID1", defendant1.toString())
                .replace("DEF_ID2", defendant2.toString());

        final JsonObject payloadForUpdate = createObjectBuilder()
                .add(COURT_FORM_ID, courtFormId.toString())
                .add(CASE_ID, caseId.toString())
                .add(FORM_DATA, formData)
                .build();

        stubFor(post(urlPathEqualTo("/CPS/v1/notification/bcm-notification"))
                .withRequestBody(equalToJson(payloadForUpdate.toString()))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("Ocp-Apim-Subscription-Key", "3674a16507104b749a76b29b6c837352")
                        .withHeader("Ocp-Apim-Trace", "true")));

        final String updateFormEndpointUrl = UPDATE_FORM_ENDPOINT.replaceAll("%caseId%", caseId.toString()).replaceAll("%courtFormId%", courtFormId.toString());
        final Response responseForUpdateForm = postCommand(getWriteUrl(updateFormEndpointUrl), UPDATE_FORM_MEDIA_TYPE, payloadForUpdate.toString());

        assertThatRequestIsAccepted(responseForUpdateForm);
    }

    private static void stubGetAdvocatesGroupsForLoggedInQuery(final String userId) {
        stubEndpoint(USERS_GROUPS_SERVICE_NAME,
                GET_GROUPS_BY_LOGGEDIN_USER_QUERY,
                GROUPS_FOR_LOGGED_IN_USER_MEDIA_TYPE,
                userId,
                "stub-data/usersGroups.get-Advocates-Groups-by-loggedIn-user.json");
    }

    private JsonObject buildPayloadForFinaliseFormApi(final String defendantId) throws IOException {
        final String inputEvent = Resources.toString(getResource("progression.finalise-form.json"), defaultCharset());
        final JsonObject readData = stringToJsonObjectConverter.convert(inputEvent.replaceAll("RANDOM_DEFENDANT_ID", defendantId));
        JsonObjectBuilder payloadBuilder = createObjectBuilder();
        JsonArrayBuilder arrayBuilder = createArrayBuilder();
        readData.getJsonArray("finalisedFormData").forEach(x -> arrayBuilder.add(x.toString()));
        payloadBuilder.add("finalisedFormData", arrayBuilder.build());
        return payloadBuilder.build();
    }

    private static void queryAndVerifyFormDetailWithCaseId(final UUID courtFormId, final UUID caseId, final UUID defendantId, FormType formType) {
        pollForResponse(format("/prosecutioncases/%s/form", caseId),
                "application/vnd.progression.query.forms-for-case+json",
                randomUUID().toString(),
                withJsonPath("$.forms[0].courtFormId", is(courtFormId.toString())),
                withJsonPath("$.caseId", is(caseId.toString())),
                withJsonPath("$.forms[0].formType", is(formType.name())),
                withJsonPath("$.forms[0].defendants[0].defendantId", is(defendantId.toString()))
        );
    }

    private static void queryAndVerifyNoOfForms(final UUID caseId, int size) {
        pollForResponse(format("/prosecutioncases/%s/form", caseId),
                "application/vnd.progression.query.forms-for-case+json",
                randomUUID().toString(),
                withJsonPath("$.forms", hasSize(size))
        );
    }

    private static void queryAndVerifyFormDetailsWithCaseIdAndFormType(final UUID courtFormId, final UUID caseId, FormType formType, final UUID defendantId) {
        pollForResponse(format("/prosecutioncases/%s/form".concat("?formType=").concat(formType.name()), caseId),
                "application/vnd.progression.query.forms-for-case+json",
                randomUUID().toString(),
                withJsonPath("$.forms[0].courtFormId", is(courtFormId.toString())),
                withJsonPath("$.caseId", is(caseId.toString())),
                withJsonPath("$.forms[0].formType", is(formType.name())),
                withJsonPath("$.forms[0].defendants[0].defendantId", is(defendantId.toString()))
        );
    }

    private static void queryAndVerifyForm(final UUID courtFormId, final UUID caseId, final UUID defendantId, final String offenceId) {
        pollForResponse(format("/prosecutioncases/%s/form/%s", caseId, courtFormId),
                "application/vnd.progression.query.form+json",
                randomUUID().toString(),
                withJsonPath("$.courtFormId", is(courtFormId.toString())),
                withJsonPath("$.caseId", is(caseId.toString())),
                withJsonPath("$.lastUpdated", notNullValue()),
                withJsonPath("$.formData", is("{\"name\":\"updated name\",\"offence\":\"burglary\"}")),
                withJsonPath("$.formType", is(BCM.name())),
                withJsonPath("$.defendants", hasSize(1)),
                withJsonPath("$.defendants[0].defendantId", is(defendantId.toString()))
        );
    }

    private void assertEditFormRequestedFromEventStream(final UUID caseId, final UUID courtFormId, final UUID lockedBy, final UUID lockRequestedBy, final boolean isLocked, final JsonObject event) {
        assertThat(event, notNullValue());
        assertThat(caseId.toString(), is(event.getString("caseId")));
        assertThat(courtFormId.toString(), is(event.getString("courtFormId")));
        assertThat(event.get("lockStatus"), notNullValue());
        final JsonObject lockStatus = event.getJsonObject("lockStatus");

        if (nonNull(lockedBy)) {
            final JsonObject lockedByJsonObject = lockStatus.getJsonObject("lockedBy");
            assertThat(lockedBy.toString(), is(lockedByJsonObject.getString("userId")));
            assertThat("rickey", is(lockedByJsonObject.getString("firstName")));
            assertThat("vaughn", is(lockedByJsonObject.getString("lastName")));
            assertThat("rickey.vaughn@test.probation.gsi.gov.uk", is(lockedByJsonObject.getString("email")));
        } else {
            assertThat(lockStatus.getString("lockedBy", null), is(nullValue()));
        }

        if (nonNull(lockRequestedBy)) {
            final JsonObject lockRequestedByJsonObject = lockStatus.getJsonObject("lockRequestedBy");
            assertThat(lockRequestedBy.toString(), is(lockRequestedByJsonObject.getString("userId")));
        } else {
            assertThat(lockStatus.getString("lockRequestedBy", null), nullValue());
        }

        assertThat(isLocked, is(lockStatus.getBoolean("isLocked")));
    }


    private void assertFormOperationFailedEvent(final UUID courtFormId, final JsonObject failureEvent, final String formCreationFailureMessage,
                                                final String formCommand, final FormType formType) {
        assertThat(failureEvent.getString("courtFormId"), is(courtFormId.toString()));
        if (nonNull(formType)) {
            assertThat(failureEvent.getString("formType"), is(formType.name()));
        }
        assertThat(failureEvent.getString("message"), is(formCreationFailureMessage));
        assertThat(failureEvent.getString("operation"), is(formCommand));
    }

    private JsonObject buildPayloadForCreateFormApi(final UUID courtFormId, final UUID formId, final FormType formType, final UUID defendantId, final String offenceId) {
        return createObjectBuilder()
                .add("courtFormId", courtFormId.toString())
                .add("formId", formId.toString())
                .add("formType", formType.name())
                .add("formDefendants", createArrayBuilder().add(createObjectBuilder().add("defendantId", defendantId.toString())
                        .build()))
                .add("formData", createObjectBuilder().build().toString()).build();
    }

    private JsonObject buildPayloadForUpdateBcmDefendantsApi(final UUID caseId, final FormType formType, final UUID defendantId, final String offenceId) {
        return createObjectBuilder()
                .add("formType", formType.name())
                .add("formDefendants", createArrayBuilder().add(createObjectBuilder().add("defendantId", defendantId.toString())
                        .build()))
                .build();
    }

    private JsonObject verifyInMessagingQueueForFormCreated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForFormCreated);
        assertTrue(message.isPresent());
        return message.get();
    }

    private JsonObject verifyInMessagingQueueForFormOperationsFailed() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForFormOperationFailed);
        assertTrue(message.isPresent());
        return message.get();
    }

    private JsonObject verifyInMessagingQueueForEditFormRequested() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForEditFormRequested);
        assertTrue(message.isPresent());
        return message.get();
    }

    private JsonObject verifyInMessagingQueueForFormFinalised() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForFormFinalised);
        assertTrue(message.isPresent());
        return message.get();
    }

    private JsonObject verifyInMessagingQueueForAddCourtDocument() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForPublicCourtDocumentAdded);
        assertTrue(message.isPresent());
        return message.get();
    }

    private void verifyInMessagingQueueForFormUpdated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForFormUpdated);
        assertTrue(message.isPresent());
    }

    private JsonObject verifyInMessagingQueue(final JmsMessageConsumerClient messageConsumer) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumer);
        assertTrue(message.isPresent());
        return message.get();
    }


    private JsonObject buildPayloadForCpsServeBcmSubmitted(final String caseId, final String defendantId1, final String defendantId2,
                                                           final String cpsDefendantId1) throws IOException {
        final String inputEvent = Resources.toString(getResource("public.prosecutioncasefile.cps-serve-bcm-submitted.json"), defaultCharset());
        return stringToJsonObjectConverter.convert(inputEvent
                .replaceAll("<CASE_ID>", caseId)
                .replaceAll("<DEFENDANT_ID_1>", defendantId1)
                .replaceAll("<DEFENDANT_ID_2>", defendantId2)
                .replaceAll("<CPS_DEFENDANT_ID_1>", cpsDefendantId1));
    }

    private JsonObject buildPayloadFor(String jsonFileName, final String caseId, final String defendantId1, final String defendantId2,
                                       final String cpsDefendantId1) throws IOException {
        final String inputEvent = Resources.toString(getResource(jsonFileName), defaultCharset());
        return stringToJsonObjectConverter.convert(inputEvent
                .replaceAll("<CASE_ID>", caseId)
                .replaceAll("<DEFENDANT_ID_1>", defendantId1)
                .replaceAll("<DEFENDANT_ID_2>", defendantId2)
                .replaceAll("<CPS_DEFENDANT_ID_1>", cpsDefendantId1));
    }
}

