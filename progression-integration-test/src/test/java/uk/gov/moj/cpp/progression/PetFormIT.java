package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithoutCourtDocumentAndCpsOrganisation;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupMaterialStructuredPetQuery;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryCpsProsecutorData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryPetFormData;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.CpsServeMaterialHelper;
import uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub;
import uk.gov.moj.cpp.progression.util.CaseProsecutorUpdateHelper;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.io.Resources;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PetFormIT extends AbstractIT {
    public static final String FINALISE_PET_FORM_ENDPOINT = "/pet/%s";
    public static final String UPDATE_PET_DETAIL_ENDPOINT = "/pet/%s";
    public static final String FINALISE_PET_FORM_MEDIA_TYPE = "application/vnd.progression.finalise-pet-form+json";

    public static final String UPDATE_PET_FORM_FOR_DEFENDANT_ENDPOINT = "/defendant/%s/update-pet-form";
    public static final String UPDATE_PET_FORM_FOR_DEFENDANT_MEDIA_TYPE = "application/vnd.progression.update-pet-form-for-defendant+json";

    public static final String CREATE_PET_FORM_ENDPOINT = "/pet";
    public static final String UPDATE_PET_FORM_ENDPOINT = "/pet/%s";

    public static final String UPDATE_PET_DETAIL_MEDIA_TYPE = "application/vnd.progression.update-pet-detail+json";

    public static final String CREATE_PET_FORM_MEDIA_TYPE = "application/vnd.progression.create-pet-form+json";
    public static final String UPDATE_PET_FORM_MEDIA_TYPE = "application/vnd.progression.update-pet-form+json";
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PET_SUBMITTED = "public.prosecutioncasefile.cps-serve-pet-submitted";
    private static final String EDIT_FORM_ENDPOINT = "/prosecutioncases/%caseId%/form/%courtFormId%";
    private static final String REQUEST_EDIT_FORM_MEDIA_TYPE = "application/vnd.progression.request-edit-form+json";


    private final JmsMessageConsumerClient consumerForPetFormCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.pet-form-created").getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForPetDetailUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.pet-detail-updated").getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForPetFormUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.pet-form-updated").getMessageConsumerClient();

    private final JmsMessageConsumerClient consumerForPetFormDefendantUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.pet-form-defendant-updated").getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForPetFormReleased = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.pet-form-released").getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForPetFormFinalised = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.pet-form-finalised").getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForEditFormRequested = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.edit-form-requested").getMessageConsumerClient();

    public static final String NAME = "name";
    public static final String USER_NAME_VALUE = "cps user name";
    public static final String UPDATED_BY = "updatedBy";
    public static final String DATA = "data";
    public static final String PET_FORM_DATA = "petFormData";
    final UUID petId = randomUUID();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @BeforeEach
    public void setUp() {
        setupLoggedInUsersPermissionQueryStub();
        setupMaterialStructuredPetQuery(petId.toString());
    }

    @Test
    public void shouldCreatePetForm() throws IOException, JSONException {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId.toString(), defendantId.toString());
        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[1].id", is("4789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                )

        );

        pollProsecutionCasesProgressionFor(caseId.toString(), caseWithOffenceMatchers);

        final JsonObject payload = createObjectBuilder()
                .add("petId", petId.toString())
                .add("caseId", caseId.toString())
                .add("formId", formId.toString())
                .add("petDefendants", createArrayBuilder().add(createObjectBuilder().add("defendantId", defendantId.toString())
                        .build()))
                .add("petFormData", createObjectBuilder().build().toString()).build();

        final Response responseForCreatePetForm = postCommand(getWriteUrl(CREATE_PET_FORM_ENDPOINT), CREATE_PET_FORM_MEDIA_TYPE, payload.toString());
        assertThat(responseForCreatePetForm.getStatusCode(), is(ACCEPTED.getStatusCode()));
        verifyInMessagingQueueForPetFormCreated();

        //query pets by caseId
        queryAndVerifyPetCaseDetail(caseId, petId, defendantId, false);

        //edit form to check lock status
        //when form is unlocked
        final UUID userId = randomUUID();
        UsersAndGroupsStub.stubGetUsersAndGroupsUserDetailsQuery(userId.toString());
        final String editFormEndpointUrl = EDIT_FORM_ENDPOINT
                .replaceAll("%caseId%", caseId.toString())
                .replaceAll("%courtFormId%", petId.toString());
        final Response responseForEditForm = postCommandWithUserId(getWriteUrl(editFormEndpointUrl), REQUEST_EDIT_FORM_MEDIA_TYPE,
                createObjectBuilder()
                        .build()
                        .toString(),
                userId.toString());
        assertThat(responseForEditForm.getStatusCode(), Matchers.is(ACCEPTED.getStatusCode()));

        JsonObject editRequestedEvent = verifyInMessagingQueueForEditFormRequested();
        assertEditFormRequestedFromEventStream(caseId, petId, null, null, false, editRequestedEvent);

        //when form is locked
        final UUID userId2 = randomUUID();
        final Response responseForEditForm2 = postCommandWithUserId(getWriteUrl(editFormEndpointUrl), REQUEST_EDIT_FORM_MEDIA_TYPE,
                createObjectBuilder()
                        .build()
                        .toString(),
                userId2.toString());
        assertThat(responseForEditForm2.getStatusCode(), Matchers.is(ACCEPTED.getStatusCode()));

        JsonObject editRequestedEvent2 = verifyInMessagingQueueForEditFormRequested();
        assertEditFormRequestedFromEventStream(caseId, petId, userId, userId2, true, editRequestedEvent2);

        final Response responseForEditFormLast = postCommandWithUserId(getWriteUrl(editFormEndpointUrl), REQUEST_EDIT_FORM_MEDIA_TYPE,
                createObjectBuilder()
                        .add("extend", true)
                        .add("extendTime", 5)
                        .build()
                        .toString(),
                userId.toString());
        assertThat(responseForEditFormLast.getStatusCode(), Matchers.is(ACCEPTED.getStatusCode()));

        JsonObject editRequestedEventLast = verifyInMessagingQueueForEditFormRequested();
        assertEditFormRequestedFromEventStream(caseId, petId, null, null, false, editRequestedEventLast);

        // update pet form
        final JsonObject payloadForUpdate = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("petFormData", createObjectBuilder().build().toString()).build();

        final Response response = postCommand(getWriteUrl(format(UPDATE_PET_FORM_ENDPOINT, petId)), UPDATE_PET_FORM_MEDIA_TYPE, payloadForUpdate.toString());
        assertThat(response.getStatusCode(), is(ACCEPTED.getStatusCode()));

        verifyInMessagingQueueForPetFormUpdated();

        //update pet form for defendant
        final JsonObject payloadForUpdatePetForDefendant = createObjectBuilder()
                .add("petId", petId.toString())
                .add("caseId", caseId.toString())
                .add("defendantId", defendantId.toString())
                .add("defendantData", createObjectBuilder().build().toString()).build();

        final Response responseForUpdatePetFormForDefendant = postCommand(getWriteUrl(format(UPDATE_PET_FORM_FOR_DEFENDANT_ENDPOINT, petId)), UPDATE_PET_FORM_FOR_DEFENDANT_MEDIA_TYPE, payloadForUpdatePetForDefendant.toString());
        assertThat(responseForUpdatePetFormForDefendant.getStatusCode(), is(ACCEPTED.getStatusCode()));
        verifyInMessagingQueueForPetFormDefendantUpdated();

        //finalise pet form

        final Response responseForFinalisePetFormForDefendant = postCommand(getWriteUrl(format(FINALISE_PET_FORM_ENDPOINT, petId)), FINALISE_PET_FORM_MEDIA_TYPE, buildPayloadForFinaliseFormApi(caseId.toString()).toString());
        assertThat(responseForFinalisePetFormForDefendant.getStatusCode(), is(ACCEPTED.getStatusCode()));
        verifyInMessagingQueueForPetFormFinalised();

        final JsonObject updatePetDetail = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("petDefendants",
                        createArrayBuilder().add(
                                createObjectBuilder()
                                        .add("defendantId", defendantId.toString())))
                .add("petId", petId.toString())
                .build();

        final Response updatePetDetailResponse = postCommand(getWriteUrl(format(FINALISE_PET_FORM_ENDPOINT, petId)), UPDATE_PET_DETAIL_MEDIA_TYPE, updatePetDetail.toString());

        assertThat(updatePetDetailResponse.getStatusCode(), is(ACCEPTED.getStatusCode()));

    }

    @Test
    public void shouldUpdatePetFormForYouth() throws IOException, JSONException {

        final boolean isYouthFlagWhenCreated = false;
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId.toString(), defendantId.toString());
        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[1].id", is("4789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                )
        );

        pollProsecutionCasesProgressionFor(caseId.toString(), caseWithOffenceMatchers);

        final JsonObject payload = createObjectBuilder()
                .add("petId", petId.toString())
                .add("caseId", caseId.toString())
                .add("formId", formId.toString())
                .add("isYouth", isYouthFlagWhenCreated)
                .add("petDefendants", createArrayBuilder().add(createObjectBuilder().add("defendantId", defendantId.toString())
                        .build()))
                .add("petFormData", createObjectBuilder().build().toString()).build();

        final Response responseForCreatePetForm = postCommand(getWriteUrl(CREATE_PET_FORM_ENDPOINT), CREATE_PET_FORM_MEDIA_TYPE, payload.toString());
        assertThat(responseForCreatePetForm.getStatusCode(), is(ACCEPTED.getStatusCode()));
        verifyInMessagingQueueForPetFormCreated();

        //query pets by caseId
        queryAndVerifyPetCaseDetail(caseId, petId, defendantId, isYouthFlagWhenCreated);

        // update pet form
        final JsonObject payloadForUpdate = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("petFormData", createObjectBuilder().build().toString()).build();

        final Response response = postCommand(getWriteUrl(format(UPDATE_PET_FORM_ENDPOINT, petId)), UPDATE_PET_FORM_MEDIA_TYPE, payloadForUpdate.toString());
        assertThat(response.getStatusCode(), is(ACCEPTED.getStatusCode()));

        verifyInMessagingQueueForPetFormUpdated();

        //query pets by caseId
        queryAndVerifyPetCaseDetail(caseId, petId, defendantId, isYouthFlagWhenCreated);

        //update pet form for defendant
        final JsonObject payloadForUpdatePetForDefendant = createObjectBuilder()
                .add("petId", petId.toString())
                .add("caseId", caseId.toString())
                .add("defendantId", defendantId.toString())
                .add("defendantData", createObjectBuilder().build().toString()).build();

        final Response responseForUpdatePetFormForDefendant = postCommand(getWriteUrl(format(UPDATE_PET_FORM_FOR_DEFENDANT_ENDPOINT, petId)), UPDATE_PET_FORM_FOR_DEFENDANT_MEDIA_TYPE, payloadForUpdatePetForDefendant.toString());
        assertThat(responseForUpdatePetFormForDefendant.getStatusCode(), is(ACCEPTED.getStatusCode()));
        verifyInMessagingQueueForPetFormDefendantUpdated();

        //finalise pet form
        final Response responseForFinalisePetFormForDefendant = postCommand(getWriteUrl(format(FINALISE_PET_FORM_ENDPOINT, petId)), FINALISE_PET_FORM_MEDIA_TYPE, buildPayloadForFinaliseFormApi(caseId.toString()).toString());
        assertThat(responseForFinalisePetFormForDefendant.getStatusCode(), is(ACCEPTED.getStatusCode()));
        verifyInMessagingQueueForPetFormFinalised();

        //update pet details
        final boolean isYouthFlagWhenUpdated = true;
        final JsonObject updatePetDetail = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("petDefendants",
                        createArrayBuilder().add(
                                createObjectBuilder()
                                        .add("defendantId", defendantId.toString())))
                .add("petId", petId.toString())
                .add("isYouth", isYouthFlagWhenUpdated)
                .build();

        final Response updatePetDetailResponse = postCommand(getWriteUrl(format(UPDATE_PET_DETAIL_ENDPOINT, petId)), UPDATE_PET_DETAIL_MEDIA_TYPE, updatePetDetail.toString());
        verifyInMessagingQueueForPetDetailUpdated();
        queryAndVerifyPetCaseDetail(caseId, petId, defendantId, isYouthFlagWhenUpdated);

        assertThat(updatePetDetailResponse.getStatusCode(), is(ACCEPTED.getStatusCode()));

    }

    @Test
    public void shouldUpdatePetDetailsWithIsYouthFlagFlagFalse() throws IOException, JSONException {

        final boolean isYouthFlagWhenCreated = true;
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId.toString(), defendantId.toString());
        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[1].id", is("4789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                )

        );

        pollProsecutionCasesProgressionFor(caseId.toString(), caseWithOffenceMatchers);

        final JsonObject payload = createObjectBuilder()
                .add("petId", petId.toString())
                .add("caseId", caseId.toString())
                .add("formId", formId.toString())
                .add("isYouth", isYouthFlagWhenCreated)
                .add("petDefendants", createArrayBuilder().add(createObjectBuilder().add("defendantId", defendantId.toString())
                        .build()))
                .add("petFormData", createObjectBuilder().build().toString()).build();

        final Response responseForCreatePetForm = postCommand(getWriteUrl(CREATE_PET_FORM_ENDPOINT), CREATE_PET_FORM_MEDIA_TYPE, payload.toString());
        assertThat(responseForCreatePetForm.getStatusCode(), is(ACCEPTED.getStatusCode()));
        verifyInMessagingQueueForPetFormCreated();

        //query pets by caseId
        queryAndVerifyPetCaseDetail(caseId, petId, defendantId, isYouthFlagWhenCreated);

        //update pet details
        final boolean isYouthFlagWhenUpdated = false;
        final JsonObject updatePetDetail = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("petDefendants",
                        createArrayBuilder().add(
                                createObjectBuilder()
                                        .add("defendantId", defendantId.toString())))
                .add("petId", petId.toString())
                .add("isYouth", isYouthFlagWhenUpdated)
                .build();

        final Response updatePetDetailResponse = postCommand(getWriteUrl(format(UPDATE_PET_DETAIL_ENDPOINT, petId)), UPDATE_PET_DETAIL_MEDIA_TYPE, updatePetDetail.toString());
        verifyInMessagingQueueForPetDetailUpdated();
        queryAndVerifyPetCaseDetail(caseId, petId, defendantId, isYouthFlagWhenUpdated);

        assertThat(updatePetDetailResponse.getStatusCode(), is(ACCEPTED.getStatusCode()));

    }

    @Test
    public void shouldAutomaticallySendToCpsOnFinaliseWhenProsecutorSsCps() throws IOException, JSONException {
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();

        stubQueryCpsProsecutorData("/restResource/referencedata.query.prosecutor.by.oucode1.json", randomUUID(), HttpStatus.SC_OK);
        initiateCourtProceedingsWithoutCourtDocumentAndCpsOrganisation(caseId.toString(), defendantId.toString());
        pollProsecutionCasesProgressionFor(caseId.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId.toString()));
        addCaseProsecutor(caseId.toString());

        final JsonObject payload = createObjectBuilder()
                .add("petId", petId.toString())
                .add("caseId", caseId.toString())
                .add("formId", formId.toString())
                .add("petDefendants", createArrayBuilder().add(createObjectBuilder().add("defendantId", defendantId.toString())
                        .build()))
                .add("petFormData", createObjectBuilder().build().toString()).build();

        final Response responseForCreatePetForm = postCommand(getWriteUrl(CREATE_PET_FORM_ENDPOINT), CREATE_PET_FORM_MEDIA_TYPE, payload.toString());
        assertThat(responseForCreatePetForm.getStatusCode(), is(ACCEPTED.getStatusCode()));
        verifyInMessagingQueueForPetFormCreated();

        //query pets by caseId
        queryAndVerifyPetCaseDetail(caseId, petId, defendantId, false);

        //finalise pet form
        final Response responseForFinalisePetFormForDefendant = postCommand(getWriteUrl(format(FINALISE_PET_FORM_ENDPOINT, petId)), FINALISE_PET_FORM_MEDIA_TYPE, buildPayloadForFinaliseFormApi(caseId.toString()).toString());
        assertThat(responseForFinalisePetFormForDefendant.getStatusCode(), is(ACCEPTED.getStatusCode()));
        verifyInMessagingQueueForPetFormFinalised();
    }

    @Test
    public void shouldSubmitCpsPetForm() throws IOException, JSONException {

        final UUID caseId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID cpsDefendantId1 = randomUUID();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId.toString(), defendantId1.toString());
        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[1].id", is("4789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                )

        );

        pollProsecutionCasesProgressionFor(caseId.toString(), caseWithOffenceMatchers);

        final CpsServeMaterialHelper cpsServeMaterialHelper = new CpsServeMaterialHelper();

        stubQueryPetFormData("/restResource/referencedata.query.latest-pet-form.json", randomUUID(), HttpStatus.SC_OK);
        final JsonObject cpsServePetSubmittedPublicEvent = buildPayloadForCpsServePetSubmitted(caseId.toString(),
                defendantId1.toString(), defendantId2.toString(), cpsDefendantId1.toString());

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PET_SUBMITTED, randomUUID()), cpsServePetSubmittedPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PET_SUBMITTED, publicEventEnvelope);

        assertThat(cpsServeMaterialHelper.getPetFormCreatedPrivateEvent(), is(notNullValue()));
        final JsonEnvelope publicEvent = cpsServeMaterialHelper.getPetFormCreatedPublicEvent();
        assertThat(publicEvent, is(notNullValue()));
        final JsonObject eventPayload = publicEvent.payloadAsJsonObject();
        assertThat(eventPayload.getJsonObject(UPDATED_BY).getString(NAME), is(USER_NAME_VALUE));
        assertTrue(eventPayload.getString(PET_FORM_DATA).contains(DATA));

        final Matcher[] caseWithCpsDefendantIdMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].cpsDefendantId", CoreMatchers.is(cpsDefendantId1.toString()))
                )
        );

        pollProsecutionCasesProgressionFor(caseId.toString(), caseWithCpsDefendantIdMatchers);
    }

    @Test
    public void shouldReleasePetForm() throws IOException, JSONException {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId.toString(), defendantId.toString());
        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[1].id", is("4789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                )

        );

        pollProsecutionCasesProgressionFor(caseId.toString(), caseWithOffenceMatchers);

        final JsonObject payload = createObjectBuilder()
                .add("petId", petId.toString())
                .add("caseId", caseId.toString())
                .add("formId", formId.toString())
                .add("petDefendants", createArrayBuilder().add(createObjectBuilder().add("defendantId", defendantId.toString())
                        .build()))
                .add("petFormData", createObjectBuilder().build().toString()).build();

        final Response responseForCreatePetForm = postCommand(getWriteUrl(CREATE_PET_FORM_ENDPOINT), CREATE_PET_FORM_MEDIA_TYPE, payload.toString());
        assertThat(responseForCreatePetForm.getStatusCode(), is(ACCEPTED.getStatusCode()));
        verifyInMessagingQueueForPetFormCreated();

        //query pets by caseId
        queryAndVerifyPetCaseDetail(caseId, petId, defendantId, false);

        //edit form to check lock status
        //when form is unlocked
        final UUID userId = randomUUID();
        UsersAndGroupsStub.stubGetUsersAndGroupsUserDetailsQuery(userId.toString());
        final String editFormEndpointUrl = EDIT_FORM_ENDPOINT
                .replaceAll("%caseId%", caseId.toString())
                .replaceAll("%courtFormId%", petId.toString());
        final Response responseForEditForm = postCommandWithUserId(getWriteUrl(editFormEndpointUrl), REQUEST_EDIT_FORM_MEDIA_TYPE,
                createObjectBuilder()
                        .build()
                        .toString(),
                userId.toString());
        assertThat(responseForEditForm.getStatusCode(), Matchers.is(ACCEPTED.getStatusCode()));

        JsonObject editRequestedEvent = verifyInMessagingQueueForEditFormRequested();
        assertEditFormRequestedFromEventStream(caseId, petId, null, null, false, editRequestedEvent);

        //when form is locked
        final UUID userId2 = randomUUID();
        final Response responseForEditForm2 = postCommandWithUserId(getWriteUrl(editFormEndpointUrl), REQUEST_EDIT_FORM_MEDIA_TYPE,
                createObjectBuilder()
                        .build()
                        .toString(),
                userId2.toString());
        assertThat(responseForEditForm2.getStatusCode(), Matchers.is(ACCEPTED.getStatusCode()));

        JsonObject editRequestedEvent2 = verifyInMessagingQueueForEditFormRequested();
        assertEditFormRequestedFromEventStream(caseId, petId, userId, userId2, true, editRequestedEvent2);

        // update pet form
        final JsonObject payloadForUpdate = createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();

        final Response response = postCommand(getWriteUrl(format(UPDATE_PET_FORM_ENDPOINT, petId)), "application/vnd.progression.release-pet-form+json", payloadForUpdate.toString());
        assertThat(response.getStatusCode(), is(ACCEPTED.getStatusCode()));

        verifyInMessagingQueueForPetFormReleased();
    }

    private void addCaseProsecutor(final String caseId) {
        CaseProsecutorUpdateHelper caseProsecutorUpdateHelper = new CaseProsecutorUpdateHelper(caseId);
        caseProsecutorUpdateHelper.updateCaseProsecutor();
    }

    private void verifyInMessagingQueueForPetFormDefendantUpdated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForPetFormDefendantUpdated);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForPetFormCreated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForPetFormCreated);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForPetFormUpdated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForPetFormUpdated);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForPetFormReleased() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForPetFormReleased);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForPetFormFinalised() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForPetFormFinalised);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForPetDetailUpdated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForPetDetailUpdated);
        assertTrue(message.isPresent());
    }

    private JsonObject buildPayloadForFinaliseFormApi(final String caseId) throws IOException {
        final String inputEvent = Resources.toString(getResource("progression.finalise-pet-form.json"), defaultCharset());
        final JsonObject readData = stringToJsonObjectConverter.convert(inputEvent);
        JsonObjectBuilder payloadBuilder = createObjectBuilder();
        JsonArrayBuilder arrayBuilder = createArrayBuilder();
        readData.getJsonArray("finalisedFormData").forEach(x -> arrayBuilder.add(x.toString()));
        payloadBuilder.add("caseId", caseId);
        payloadBuilder.add("finalisedFormData", arrayBuilder.build());
        return payloadBuilder.build();
    }

    private JsonObject buildPayloadForCpsServePetSubmitted(final String caseId, final String defendantId1, final String defendantId2, final String cpsDefendantId1) throws IOException {
        final String inputEvent = Resources.toString(getResource("public.prosecutioncasefile.cps-serve-pet-submitted.json"), defaultCharset());
        return stringToJsonObjectConverter.convert(inputEvent
                .replaceAll("<CASE_ID>", caseId)
                .replaceAll("<DEFENDANT_ID_1>", defendantId1)
                .replaceAll("<DEFENDANT_ID_2>", defendantId2)
                .replaceAll("<CPS_DEFENDANT_ID_1>", cpsDefendantId1));
    }


    private void assertEditFormRequestedFromEventStream(final UUID caseId, final UUID courtFormId, final UUID lockedBy, final UUID lockRequestedBy, final boolean isLocked, final JsonObject event) {
        assertThat(event, Matchers.notNullValue());
        assertThat(caseId.toString(), Matchers.is(event.getString("caseId")));
        assertThat(courtFormId.toString(), Matchers.is(event.getString("courtFormId")));
        assertThat(event.get("lockStatus"), Matchers.notNullValue());
        final JsonObject lockStatus = event.getJsonObject("lockStatus");

        if (nonNull(lockedBy)) {
            final JsonObject lockedByJsonObject = lockStatus.getJsonObject("lockedBy");
            assertThat(lockedBy.toString(), Matchers.is(lockedByJsonObject.getString("userId")));
            assertThat("rickey", Matchers.is(lockedByJsonObject.getString("firstName")));
            assertThat("vaughn", Matchers.is(lockedByJsonObject.getString("lastName")));
            assertThat("rickey.vaughn@test.probation.gsi.gov.uk", Matchers.is(lockedByJsonObject.getString("email")));
        } else {
            assertThat(lockStatus.getString("lockedBy", null), Matchers.is(nullValue()));
        }

        if (nonNull(lockRequestedBy)) {
            final JsonObject lockRequestedByJsonObject = lockStatus.getJsonObject("lockRequestedBy");
            assertThat(lockRequestedBy.toString(), Matchers.is(lockRequestedByJsonObject.getString("userId")));
        } else {
            assertThat(lockStatus.getString("lockRequestedBy", null), nullValue());
        }

        assertThat(isLocked, Matchers.is(lockStatus.getBoolean("isLocked")));
    }

    private JsonObject verifyInMessagingQueueForEditFormRequested() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForEditFormRequested);
        assertTrue(message.isPresent());
        return message.get();
    }

    public void queryAndVerifyPetCaseDetail(final UUID caseId, final UUID petId, final UUID defendantId, final boolean isYouth) {
        pollForResponse(format("/prosecutioncases/%s/pet", caseId),
                "application/vnd.progression.query.pets-for-case+json",
                randomUUID().toString(),
                withJsonPath("$.pets[0].defendants[0].caseId", Matchers.is(caseId.toString())),
                withJsonPath("$.pets[0].defendants[0].defendantId", is(defendantId.toString())),
                withJsonPath("$.pets[0].petId", is(petId.toString())),
                withJsonPath("$.pets[0].isYouth", is(isYouth))
        );
    }
}
