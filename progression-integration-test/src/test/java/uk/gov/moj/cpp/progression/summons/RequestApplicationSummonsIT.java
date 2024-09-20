package uk.gov.moj.cpp.progression.summons;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.progression.courts.HearingResulted.hearingResulted;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromJsonString;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.applications.SummonsResultUtil.getSummonsApprovedResult;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.progression.helper.MaterialHelper.sendEventToConfirmMaterialAdded;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByApplication;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyCreateLetterRequested;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithAttachment;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithoutAttachment;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyNoEmailNotificationIsRaised;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyNoLetterRequested;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.getLanguagePrefix;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.getSubjectDateOfBirth;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.verifyMaterialRequestRecordedAndExtractMaterialId;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.verifyTemplatePayloadValues;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Disabled("DD-33449")
public class RequestApplicationSummonsIT extends AbstractIT {

    private static final String PROSECUTOR_COST = "£245.56";

    public static Stream<Arguments> applicationSummonsSuppressed() {
        return Stream.of(
                // summons template type, summons required, youth defendant, number of documents, isWelsh
                Arguments.of(SummonsTemplateType.GENERIC_APPLICATION, SummonsType.APPLICATION, "Application", true, 1, true),
                Arguments.of(SummonsTemplateType.BREACH, SummonsType.BREACH, "Breach", false, 1, true)
        );
    }

    public static Stream<Arguments> applicationSummonsNotSuppressed() {
        return Stream.of(
                // summons template type, summons required, youth defendant, number of documents, isWelsh
                Arguments.of(SummonsTemplateType.GENERIC_APPLICATION, SummonsType.APPLICATION, "Application", false, 1, false),
                Arguments.of(SummonsTemplateType.BREACH, SummonsType.BREACH, "Breach", true, 2, false)
        );
    }

    private static final String PUBLIC_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String INITIATE_COURT_HEARING_AFTER_SUMMONS_APPROVED = "progression.event.initiate-court-hearing-after-summons-approved";
    private static final String PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED = "public.progression.boxwork-application-referred";
    private static final String PRIVATE_EVENT_NOWS_MATERIAL_REQUEST_RECORDED = "progression.event.nows-material-request-recorded";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";

    private static final JsonObjectToObjectConverter JSON_OBJECT_TO_OBJECT_CONVERTER = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    private static final ObjectToJsonObjectConverter OBJECT_TO_JSON_OBJECT_CONVERTER = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    private static final StringToJsonObjectConverter STRING_TO_JSON_OBJECT_CONVERTER = new StringToJsonObjectConverter();


    private String caseId;
    private String applicationId;
    private String defendantId;
    private boolean personalService;
    private String prosecutorEmailAddress;

    private String firstName;
    private String middleName;
    private String lastName;
    private String parentFirstName;
    private String parentMiddleName;
    private String parentLastName;

    @BeforeEach
    public void setUp() {
        stubInitiateHearing();
        stubDocumentCreate(randomAlphanumeric(20));
        IdMapperStub.setUp();
        NotificationServiceStub.setUp();
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        caseId = randomUUID().toString();
        applicationId = randomUUID().toString();
        defendantId = randomUUID().toString();
        personalService = BOOLEAN.next();
        prosecutorEmailAddress = randomAlphanumeric(20) + "@random.com";

        firstName = "F_" + STRING.next();
        middleName = "M_" + STRING.next();
        lastName = "L_" + STRING.next();
        parentFirstName = "PF_" + STRING.next();
        parentMiddleName = "PM_" + STRING.next();
        parentLastName = "PL_" + STRING.next();
    }

    @MethodSource("applicationSummonsNotSuppressed")
    @ParameterizedTest
    public void shouldGenerateSummonsAfterApplicationApproved_SummonsNotSuppressedV2(final SummonsTemplateType summonsTemplateType, final SummonsType summonsRequired, final String templateName, final boolean isYouth, final int numberOfDocuments, final boolean isWelsh) throws Exception {
        final JmsMessageConsumerClient nowsMaterialRequestRecordedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PRIVATE_EVENT_NOWS_MATERIAL_REQUEST_RECORDED).getMessageConsumerClient();
        final boolean summonsSuppressed = false;

        final Hearing hearing = givenApplicationInitiatedInBoxWork(summonsTemplateType, isYouth, isWelsh);

        whenApplicationResultedAsApprovedAndCourtHearingInitiatedV2(summonsSuppressed, isWelsh, hearing);

        // perform following verifications
        verifyDocumentAddedToCdes(applicationId, numberOfDocuments);

        final String subjectTemplateName = "SP" + getLanguagePrefix(isWelsh) + "_" + templateName;
        verifyTemplatePayloadValues(true, subjectTemplateName, summonsRequired.toString(), PROSECUTOR_COST, personalService, firstName, middleName, lastName);

        final UUID materialId = verifyMaterialRequestRecordedAndExtractMaterialId(nowsMaterialRequestRecordedConsumer);
        sendEventToConfirmMaterialAdded(materialId);

        final List<String> expectedEmailDetails = newArrayList(prosecutorEmailAddress, format("%s %s %s", firstName, middleName, lastName));
        verifyEmailNotificationIsRaisedWithoutAttachment(expectedEmailDetails);
        verifyCreateLetterRequested(of("letterUrl", materialId.toString()));

        // applies only to breach application types
        if (isYouth && numberOfDocuments > 1) {
            verifyParentBreachSummonWhenNotSuppressed(summonsRequired, isWelsh, nowsMaterialRequestRecordedConsumer);
        }
    }

    @MethodSource("applicationSummonsSuppressed")
    @ParameterizedTest
    public void shouldInitiateCourtHearingAfterSummonsApproved_SummonsSuppressed(final SummonsTemplateType summonsTemplateType, final SummonsType summonsRequired, final String templateName, final boolean isYouth, final int numberOfDocuments, final boolean isWelsh) throws Exception {
        final boolean summonsSuppressed = true;
        final JmsMessageConsumerClient nowsMaterialRequestRecordedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PRIVATE_EVENT_NOWS_MATERIAL_REQUEST_RECORDED).getMessageConsumerClient();
        final Hearing hearing = givenApplicationInitiatedInBoxWork(summonsTemplateType, isYouth, isWelsh);

        whenApplicationResultedAsApprovedAndCourtHearingInitiated(summonsSuppressed, isWelsh, hearing);

        // perform following verifications
        verifyDocumentAddedToCdes(applicationId, numberOfDocuments);

        final String subjectTemplateName = "SP" + getLanguagePrefix(isWelsh) + "_" + templateName;
        verifyTemplatePayloadValues(true, subjectTemplateName, summonsRequired.toString(), PROSECUTOR_COST, personalService, firstName, middleName, lastName);

        final UUID materialId = verifyMaterialRequestRecordedAndExtractMaterialId(nowsMaterialRequestRecordedConsumer);
        sendEventToConfirmMaterialAdded(materialId);

        final List<String> expectedEmailDetails = newArrayList(prosecutorEmailAddress, format("%s %s %s", firstName, middleName, lastName));
        verifyEmailNotificationIsRaisedWithAttachment(expectedEmailDetails, materialId);
        verifyNoLetterRequested(of(materialId.toString()));

        // applies only to breach application types
        if (SummonsTemplateType.BREACH == summonsTemplateType && isYouth && numberOfDocuments > 1) {
            verifyParentBreachSummonWhenSuppressed(summonsRequired, isWelsh, nowsMaterialRequestRecordedConsumer);
        }
    }

    private void verifyParentBreachSummonWhenSuppressed(final SummonsType summonsRequired, final boolean isWelsh, final JmsMessageConsumerClient nowsMaterialRequestRecordedConsumer) {
        final String parentTemplateName = "SP" + getLanguagePrefix(isWelsh) + "_" + "BreachParent";
        verifyTemplatePayloadValues(true, parentTemplateName, summonsRequired.toString(), PROSECUTOR_COST, personalService, parentFirstName, parentMiddleName, parentLastName);
        final UUID parentMaterialId = verifyMaterialRequestRecordedAndExtractMaterialId(nowsMaterialRequestRecordedConsumer);
        sendEventToConfirmMaterialAdded(parentMaterialId);

        final List<String> expectedParentEmailDetails = newArrayList(prosecutorEmailAddress,
                format("%s %s %s (parent/guardian of %s %s %s)", parentFirstName, parentMiddleName, parentLastName,
                        firstName, middleName, lastName));
        verifyEmailNotificationIsRaisedWithAttachment(expectedParentEmailDetails, parentMaterialId);
        verifyNoLetterRequested(of(parentMaterialId.toString()));
    }

    private void verifyParentBreachSummonWhenNotSuppressed(final SummonsType summonsRequired, final boolean isWelsh, final JmsMessageConsumerClient nowsMaterialRequestRecordedConsumer) {
        final String parentTemplateName = "SP" + getLanguagePrefix(isWelsh) + "_" + "BreachParent";
        verifyTemplatePayloadValues(true, parentTemplateName, summonsRequired.toString(), PROSECUTOR_COST, personalService, parentFirstName, parentMiddleName, parentLastName);
        final UUID parentMaterialId = verifyMaterialRequestRecordedAndExtractMaterialId(nowsMaterialRequestRecordedConsumer);
        sendEventToConfirmMaterialAdded(parentMaterialId);

        final List<String> expectedParentEmailDetails = newArrayList(prosecutorEmailAddress,
                format("%s %s %s (parent/guardian of %s %s %s)", parentFirstName, parentMiddleName, parentLastName,
                        firstName, middleName, lastName));
        verifyNoEmailNotificationIsRaised(expectedParentEmailDetails);
        verifyCreateLetterRequested(of("letterUrl", parentMaterialId.toString()));
    }

    private void whenApplicationResultedAsApprovedAndCourtHearingInitiated(final boolean summonsSuppressed, final boolean isWelsh, final Hearing hearing) {
        final JmsMessageConsumerClient consumerForInitiateCourtHearingAfterSummonsApproved = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(INITIATE_COURT_HEARING_AFTER_SUMMONS_APPROVED).getMessageConsumerClient();
        final JudicialResult summonsApprovedResult = getSummonsApprovedJudicialResult(summonsSuppressed);

        sendHearingResultedPayload(createPublicHearingResulted(hearing, summonsApprovedResult));

        final String courtHearingId = getNewHearingId(consumerForInitiateCourtHearingAfterSummonsApproved, hearing.getCourtApplications().get(0).getId());

        verifyCourtHearingInitiate(courtHearingId);

        sendPublicEventToConfirmHearingForInitiatedCase(applicationId, courtHearingId, isWelsh);
    }

    private void whenApplicationResultedAsApprovedAndCourtHearingInitiatedV2(final boolean summonsSuppressed,
                                                                             final boolean isWelsh,
                                                                             final Hearing hearing) {
        final JmsMessageConsumerClient consumerForInitiateCourtHearingAfterSummonsApproved = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(INITIATE_COURT_HEARING_AFTER_SUMMONS_APPROVED).getMessageConsumerClient();
        final JudicialResult summonsApprovedResult = getSummonsApprovedJudicialResult(summonsSuppressed);

        final JsonObject hearingJO = OBJECT_TO_JSON_OBJECT_CONVERTER.convert(hearing);
        final JsonObject summonsApprovedResultJO = OBJECT_TO_JSON_OBJECT_CONVERTER.convert(summonsApprovedResult);
        final JsonObject publicHearingResultedV2 = createPublicHearingResultedV2(hearingJO, summonsApprovedResultJO);
        sendHearingResultedPayloadV2(publicHearingResultedV2);


        final String courtHearingId = getNewHearingId(consumerForInitiateCourtHearingAfterSummonsApproved, hearing.getCourtApplications().get(0).getId());
        verifyCourtHearingInitiate(courtHearingId);

        sendPublicEventToConfirmHearingForInitiatedCase(applicationId, courtHearingId, isWelsh);
    }

    private Hearing givenApplicationInitiatedInBoxWork(final SummonsTemplateType summonsTemplateType, final boolean isYouth, final boolean isWelsh) throws IOException, JSONException {
        final JmsMessageConsumerClient messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED).getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        initiateCourtProceedingsForCourtApplication(getPayloadForInitiatingCourtApplicationProceedings(summonsTemplateType, applicationId, caseId, isYouth, isWelsh));

        final Hearing hearing = getHearingInMessagingQueueForBoxWorkReferred(messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated, applicationId);
        final String boxworkHearingId = hearing.getId().toString();
        pollForResponse("/hearingSearch/" + boxworkHearingId, "application/vnd.progression.query.hearing+json", withJsonPath("$.hearing.id", Matchers.is(boxworkHearingId)));
        return hearing;
    }

    private JsonObject createPublicHearingResulted(final Hearing hearing, final JudicialResult summonsResult) {
        final CourtApplication courtApplication = hearing.getCourtApplications().get(0);
        final CourtApplication courtApplicationWithResults = courtApplication().withValuesFrom(courtApplication).withJudicialResults(singletonList(summonsResult)).build();
        final Hearing hearingWithApplicationResults = hearing().withValuesFrom(hearing).withCourtApplications(singletonList(courtApplicationWithResults)).build();
        return OBJECT_TO_JSON_OBJECT_CONVERTER.convert(hearingResulted().withHearing(hearingWithApplicationResults)
                .withSharedTime(ZonedDateTime.now()).build());
    }

    private JsonObject createPublicHearingResultedV2(final JsonObject hearing, final JsonObject summonResultJsonObject) {
        final JsonObject courtApplication = hearing.getJsonArray("courtApplications").getJsonObject(0);
        final JsonString sittingDay = hearing.getJsonArray("hearingDays").getJsonObject(0).getJsonString("sittingDay");
        return Json.createObjectBuilder()
                .add("isReshare", true)
                .add("hearingDay", fromJsonString(sittingDay).toLocalDate().toString())
                .add("hearing", createObjectBuilder()
                        .add("courtCentre", hearing.getJsonObject("courtCentre"))
                        .add("hearingDays", hearing.getJsonArray("hearingDays"))
                        .add("type", hearing.getJsonObject("type"))
                        .add("id", hearing.getString("id"))
                        .add("isBoxHearing", hearing.getBoolean("isBoxHearing"))
                        .add("isVirtualBoxHearing", hearing.getBoolean("isVirtualBoxHearing"))
                        .add("jurisdictionType", hearing.getString("jurisdictionType"))
                        .add("courtApplications", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("applicant", courtApplication.getJsonObject("applicant"))
                                        .add("courtApplicationCases", courtApplication.getJsonArray("courtApplicationCases"))
                                        .add("respondents", courtApplication.getJsonArray("respondents"))
                                        .add("subject", courtApplication.getJsonObject("subject"))
                                        .add("futureSummonsHearing", courtApplication.getJsonObject("futureSummonsHearing"))
                                        .add("thirdParties", courtApplication.getJsonArray("thirdParties"))
                                        .add("type", courtApplication.getJsonObject("type"))
                                        .add("allegationOrComplaintEndDate", courtApplication.getString("allegationOrComplaintEndDate"))
                                        .add("allegationOrComplaintStartDate", courtApplication.getString("allegationOrComplaintStartDate"))
                                        .add("applicationReceivedDate", courtApplication.getString("applicationReceivedDate"))
                                        .add("applicationReference", courtApplication.getString("applicationReference"))
                                        .add("applicationStatus", courtApplication.getString("applicationStatus"))
                                        .add("commissionerOfOath", courtApplication.getBoolean("commissionerOfOath"))
                                        .add("hasSummonsSupplied", courtApplication.getBoolean("hasSummonsSupplied"))
                                        .add("id", courtApplication.getString("id"))
                                        .add("judicialResults", createArrayBuilder().add(summonResultJsonObject)))))
                .add("sharedTime", "2021-02-03T19:37:24Z")
                .build();
    }


    private void verifyCourtHearingInitiate(String newHearingId) {
        pollForResponse("/hearingSearch/" + newHearingId, "application/vnd.progression.query.hearing+json", withJsonPath("$.hearing.id", Matchers.is(newHearingId)));
    }

    private String getNewHearingId(final JmsMessageConsumerClient consumerForInitiateCourtHearingAfterSummonsApproved, final UUID applicationId) {
        final JsonPath message = retrieveMessageAsJsonPath(consumerForInitiateCourtHearingAfterSummonsApproved, isJson(allOf(withJsonPath("$.application.id", is(applicationId.toString())))));
        assertThat(ofNullable(message).isPresent(), is(true));
        return message.getJsonObject("courtHearing.id");

    }

    public Hearing getHearingInMessagingQueueForBoxWorkReferred(final JmsMessageConsumerClient messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated, final String applicationId) {
        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated, isJson(allOf(withJsonPath("$.hearing.courtApplications[0].id", is(applicationId)))));
        assertThat(ofNullable(message).isPresent(), is(true));
        return JSON_OBJECT_TO_OBJECT_CONVERTER.convert(STRING_TO_JSON_OBJECT_CONVERTER.convert(message.prettify()).getJsonObject("hearing"), Hearing.class);
    }

    private void sendPublicEventToConfirmHearingForInitiatedCase(final String applicationId, final String hearingId, final boolean isWelsh) {
        final String payloadStr = getPayload("public.listing.hearing-confirmed-applications-only.json")
                .replaceAll("APPLICATION_ID", applicationId)
                .replaceAll("COURT_CENTRE_ID", isWelsh ? ReferenceDataStub.WELSH_COURT_ID : ReferenceDataStub.ENGLISH_COURT_ID)
                .replaceAll("HEARING_ID", hearingId);
        final JsonObject hearingConfirmedPayload = new StringToJsonObjectConverter().convert(payloadStr);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_CONFIRMED, randomUUID()), hearingConfirmedPayload);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_CONFIRMED, publicEventEnvelope);
    }

    private void verifyDocumentAddedToCdes(final String applicationId, final int numberOfDocuments) {

        final Matcher[] matchers = {
                withJsonPath("$.documentIndices", hasSize(greaterThanOrEqualTo(numberOfDocuments))),
                withJsonPath("$.documentIndices[*].document.name", hasItems("Summons"))
        };
        getCourtDocumentsByApplication(USER_ID, applicationId, matchers);
    }

    private String getPayloadForInitiatingCourtApplicationProceedings(final SummonsTemplateType summonsTemplateType, final String applicationId, final String caseId, final boolean isYouth, final boolean isWelsh) {
        final String resourceLocation = isYouth ? "applications/progression.initiate-court-proceedings-for-summons-generation-for-youth.json" : "applications/progression.initiate-court-proceedings-for-summons-generation.json";
        final String defendantDateOfBirth = getSubjectDateOfBirth(isYouth);

        return getPayload(resourceLocation)
                .replace("SUMMONS_TEMPLATE_TYPE", summonsTemplateType.toString())
                .replace("APPLICATION_ID", applicationId)
                .replace("CASE_ID", caseId)
                .replace("SUBJECT_DOB", defendantDateOfBirth)
                .replaceAll("COURT_CENTRE_ID", isWelsh ? ReferenceDataStub.WELSH_COURT_ID : ReferenceDataStub.ENGLISH_COURT_ID)
                .replace("PARENT_FIRST_NAME", parentFirstName)
                .replace("PARENT_MIDDLE_NAME", parentMiddleName)
                .replace("PARENT_LAST_NAME", parentLastName)
                .replace("FIRST_NAME", firstName)
                .replace("MIDDLE_NAME", middleName)
                .replace("LAST_NAME", lastName);
    }

    private void sendHearingResultedPayload(final JsonObject publicHearingResultedJsonObject) {
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED, USER_ID), publicHearingResultedJsonObject);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED, publicEventEnvelope);
    }

    private void sendHearingResultedPayloadV2(final JsonObject publicHearingResultedJsonObject) {
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, USER_ID), publicHearingResultedJsonObject);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);
    }

    private JudicialResult getSummonsApprovedJudicialResult(final boolean summonsSuppressed) {
        final String payloadUpdated = getSummonsApprovedResult(PROSECUTOR_COST, personalService, summonsSuppressed, prosecutorEmailAddress);
        return JSON_OBJECT_TO_OBJECT_CONVERTER.convert(new StringToJsonObjectConverter().convert(payloadUpdated), JudicialResult.class);
    }
}