package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupMaterialStructuredPetQuery;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.stubMaterialMetadata;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.jayway.restassured.response.Response;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class PetFormIT extends AbstractIT {
    public static final String FINALISE_PET_FORM_ENDPOINT = "/pet/%s";
    public static final String FINALISE_PET_FORM_MEDIA_TYPE = "application/vnd.progression.finalise-pet-form+json";

    public static final String UPDATE_PET_FORM_FOR_DEFENDANT_ENDPOINT = "/defendant/%s/update-pet-form";
    public static final String UPDATE_PET_FORM_FOR_DEFENDANT_MEDIA_TYPE = "application/vnd.progression.update-pet-form-for-defendant+json";

    public static final String CREATE_PET_FORM_ENDPOINT = "/pet";
    public static final String UPDATE_PET_FORM_ENDPOINT = "/pet/%s";

    public static final String UPDATE_PET_DETAIL_MEDIA_TYPE = "application/vnd.progression.update-pet-detail+json";

    public static final String CREATE_PET_FORM_MEDIA_TYPE = "application/vnd.progression.create-pet-form+json";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();
    public static final String UPDATE_PET_FORM_MEDIA_TYPE = "application/vnd.progression.update-pet-form+json";

    private static final String DOCUMENT_TEXT = STRING.next();

    private static MessageConsumer consumerForPetFormCreated = publicEvents
            .createPublicConsumer("public.progression.pet-form-created");

    private static MessageConsumer consumerForPetFormUpdated = publicEvents
            .createPublicConsumer("public.progression.pet-form-updated");

    private static MessageConsumer consumerForPetFormDefendantUpdated = publicEvents
            .createPublicConsumer("public.progression.pet-form-defendant-updated");

    private static MessageConsumer consumerForPetFormFinalised = publicEvents
            .createPublicConsumer("public.progression.pet-form-finalised");

    private static MessageConsumer consumerForCourtsDocumentAdded = privateEvents
            .createPrivateConsumer("progression.event.court-document-added");

    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    final UUID petId = randomUUID();

    @Before
    public void setUp() throws Exception {
        setupLoggedInUsersPermissionQueryStub();
        setupMaterialStructuredPetQuery(petId.toString());
        stubDocumentCreate(DOCUMENT_TEXT);
        stubMaterialMetadata();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        consumerForPetFormCreated.close();
        consumerForPetFormDefendantUpdated.close();
        consumerForPetFormFinalised.close();
        consumerForCourtsDocumentAdded.close();
        consumerForPetFormUpdated.close();
        messageProducerClientPublic.close();
    }

    @Test
    public void shouldCreatePetForm() throws IOException {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();
        final String offenceId2 = "3789ab16-0bb7-4ef1-87ef-c936bf0364f2";

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId.toString(), defendantId.toString());
        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[1].id", is("4789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                )

        );

        final String responseForCaseQuery = pollProsecutionCasesProgressionFor(caseId.toString(), caseWithOffenceMatchers);
        final JsonObject caseObject = stringToJsonObjectConverter.convert(responseForCaseQuery);

        final String offenceId = caseObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("id");

        final JsonObject payload = createObjectBuilder()
                .add("petId", petId.toString())
                .add("caseId", caseId.toString())
                .add("formId", formId.toString())
                .add("petDefendants", createArrayBuilder().add(createObjectBuilder().add("defendantId", defendantId.toString())
                        .add("offenceIds", createArrayBuilder().add(offenceId.toString()))
                        .build()))
                .add("petFormData", createObjectBuilder().build().toString()).build();

        final Response responseForCreatePetForm = postCommand(getWriteUrl(CREATE_PET_FORM_ENDPOINT), CREATE_PET_FORM_MEDIA_TYPE, payload.toString());
        assertThat(responseForCreatePetForm.getStatusCode(), is(ACCEPTED.getStatusCode()));
        verifyInMessagingQueueForPetFormCreated();

        //query pets by caseId
        queryAndVerifyPetCaseDetail(caseId, petId, defendantId);

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
        final JsonObject payloadForFinalisePet = createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();

        final Response responseForFinalisePetFormForDefendant = postCommand(getWriteUrl(format(FINALISE_PET_FORM_ENDPOINT, petId)), FINALISE_PET_FORM_MEDIA_TYPE, payloadForFinalisePet.toString());
        assertThat(responseForFinalisePetFormForDefendant.getStatusCode(), is(ACCEPTED.getStatusCode()));
        verifyInMessagingQueueForPetFormFinalised();
        verifyInMessagingQueueForCourtsDocumentAdded();


        final JsonObject updatePetDetail = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("petDefendants",
                        createArrayBuilder().add(
                                createObjectBuilder()
                                        .add("defendantId", defendantId.toString())
                                        .add("offenceIds", createArrayBuilder().add(offenceId2).add(offenceId))))
                .add("petId", petId.toString())
                .build();

        final Response updatePetDetailResponse = postCommand(getWriteUrl(format(FINALISE_PET_FORM_ENDPOINT, petId)), UPDATE_PET_DETAIL_MEDIA_TYPE, updatePetDetail.toString());

        assertThat(updatePetDetailResponse.getStatusCode(), is(ACCEPTED.getStatusCode()));

    }

    private void verifyInMessagingQueueForPetFormDefendantUpdated() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(consumerForPetFormDefendantUpdated);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForPetFormCreated() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(consumerForPetFormCreated);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForPetFormUpdated() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(consumerForPetFormUpdated);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForPetFormFinalised() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(consumerForPetFormFinalised);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForCourtsDocumentAdded() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(consumerForCourtsDocumentAdded);
        assertTrue(message.isPresent());
    }

    public static void queryAndVerifyPetCaseDetail(final UUID caseId, final UUID petId, final UUID defendantId) {
        poll(requestParams(AbstractTestHelper.getReadUrl(String.format("/prosecutioncases/%s/pet", caseId)),
                "application/vnd.progression.query.pets-for-case+json")
                .withHeader("CJSCPPUID", randomUUID()))
                .timeout(30, SECONDS)
                .until(status().is(OK),
                        payload().isJson(
                                allOf(
                                        withJsonPath("$.pets[0].defendants[0].caseId", Matchers.is(caseId.toString())),
                                        withJsonPath("$.pets[0].defendants[0].defendantId", is(defendantId.toString())),
                                        withJsonPath("$.pets[0].petId", is(petId.toString()))
                                )));
    }

}
