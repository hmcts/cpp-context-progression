package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.POST_CODE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_APPLICATION_AAAG_FOR_DEFENCE_JSON;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_APPLICATION_AAAG_JSON;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationForApplicationAtAGlance;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getApplicationFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.TIMEOUT;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubAdvocateRoleInCaseByCaseId;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApplicationAtAGlanceIT extends AbstractIT {
    private static final JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final String PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON = "progression.command.create-court-application-aaag.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FILE = "public.listing.hearing-confirmed.json";
    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final String DOCUMENT_TEXT = STRING.next();

    private String userId;
    private String caseId;
    private String defendantId;
    private String hearingId;
    private String courtCentreId;
    private String courtApplicationId;
    private String particulars;
    private String applicantReceivedDate;
    private String applicationType;
    private Boolean appeal;
    private Boolean applicantAppellantFlag;
    private String paymentReference;
    private String applicantSynonym;
    private String applicantFirstName;
    private String applicantLastName;
    private String applicantNationality;
    private String applicantRemandStatus;
    private String applicantRepresentation;
    private String interpreterLanguageNeeds;
    private LocalDate applicantDoB;
    private String applicantAddress1;
    private String applicantAddress2;
    private String applicantAddress3;
    private String applicantAddress4;
    private String applicantAddress5;
    private String applicantPostCode;
    private String applicationReference;
    private String respondentDefendantId;
    private String respondentOrganisationName;
    private String respondentOrganisationAddress1;
    private String respondentOrganisationAddress2;
    private String respondentOrganisationAddress3;
    private String respondentOrganisationAddress4;
    private String respondentOrganisationAddress5;
    private String respondentOrganisationPostcode;
    private String respondentRepresentativeFirstName;
    private String respondentRepresentativeLastName;
    private String respondentRepresentativePosition;
    private String prosecutionCaseId;
    private String prosecutionAuthorityId;
    private String prosecutionAuthorityCode;
    private String prosecutionAuthorityReference;

    @BeforeEach
    public void setUp() {
        stubDocumentCreate(DOCUMENT_TEXT);
        stubInitiateHearing();
        setupData();
    }



    @Test
    public void shouldVerifyLinkedApplicationsInApplicationAtAGlance() throws Exception {
        doReferCaseToCourtAndVerify();
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));
        doHearingConfirmedAndVerify();
        final String firstApplicationId = courtApplicationId;
        doAddCourtApplicationAndVerify(PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON, randomUUID().toString());
        verifyApplicationAtAGlance(courtApplicationId, PROGRESSION_QUERY_APPLICATION_AAAG_JSON);

        setupData();
        final String linkedApplicationId = courtApplicationId;
        doAddCourtApplicationAndVerify(PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON, firstApplicationId);
        verifyLinkedApplications(firstApplicationId, linkedApplicationId);
    }

    @Test
    public void shouldVerifyLinkedCasesInApplicationAtAGlanceForDefence() throws Exception {
        final String userRoleInCase = getPayload("stub-data/defence.advocate.query.role-in-case-by-caseid.json")
                .replace("%CASE_ID%", prosecutionCaseId)
                .replace("%USER_ROLE_IN_CASE%", "prosecuting");

        stubAdvocateRoleInCaseByCaseId(prosecutionCaseId, userRoleInCase);
        doReferCaseToCourtAndVerify();
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));
        doHearingConfirmedAndVerify();
        doAddCourtApplicationAndVerify(PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON, randomUUID().toString());

        verifyApplicationAtAGlance(courtApplicationId, PROGRESSION_QUERY_APPLICATION_AAAG_FOR_DEFENCE_JSON);
    }

    private void doReferCaseToCourtAndVerify() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
    }

    private void doHearingConfirmedAndVerify() {

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getHearingJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE, caseId, hearingId, defendantId, courtCentreId));
        publicMessageProducerClient.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId))
        );
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        final String strPayload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private void doAddCourtApplicationAndVerify(final String filename, final String parentApplicationId) throws Exception {
        addCourtApplicationForApplicationAtAGlance(caseId,
                courtApplicationId,
                particulars,
                applicantReceivedDate,
                applicationType,
                appeal,
                applicantAppellantFlag,
                paymentReference,
                applicantSynonym,
                applicantFirstName,
                applicantLastName,
                applicantNationality,
                applicantRemandStatus,
                applicantRepresentation,
                interpreterLanguageNeeds,
                applicantDoB,
                applicantAddress1,
                applicantAddress2,
                applicantAddress3,
                applicantAddress4,
                applicantAddress5,
                applicantPostCode,
                applicationReference,
                respondentDefendantId,
                respondentOrganisationName,
                respondentOrganisationAddress1,
                respondentOrganisationAddress2,
                respondentOrganisationAddress3,
                respondentOrganisationAddress4,
                respondentOrganisationAddress5,
                respondentOrganisationPostcode,
                respondentRepresentativeFirstName,
                respondentRepresentativeLastName,
                respondentRepresentativePosition,
                prosecutionCaseId,
                prosecutionAuthorityId,
                prosecutionAuthorityCode,
                prosecutionAuthorityReference,
                parentApplicationId,
                filename);

        final String caseResponse = getApplicationFor(courtApplicationId);
        assertThat(caseResponse, is(notNullValue()));
    }

    private void verifyApplicationAtAGlance(final String applicationId, final String mediaType) {

        poll(requestParams(getReadUrl("/applications/" + applicationId), mediaType)
                .withHeader(USER_ID, randomUUID()))
                .timeout(TIMEOUT, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.applicationId", equalTo(applicationId)),
                                withJsonPath("$.applicationDetails.applicationReference", notNullValue()),
                                withJsonPath("$.applicationDetails.applicationParticulars", equalTo(particulars)),
                                withJsonPath("$.applicationDetails.applicationReceivedDate", equalTo(applicantReceivedDate)),
                                withJsonPath("$.applicationDetails.applicationType", equalTo(applicationType)),
                                withJsonPath("$.applicationDetails.appeal", equalTo(appeal)),
                                withJsonPath("$.applicationDetails.applicantAppellantFlag", equalTo(applicantAppellantFlag)),
                                withJsonPath("$.applicationDetails.aagResults.length()", equalTo(1)),
                                withJsonPath("$.applicationDetails.aagResults[0].id", equalTo("f8e926eb-704a-457a-a794-8c3ad40d3113")),
                                withJsonPath("$.applicationDetails.aagResults[0].label", equalTo("wording for results")),
                                withJsonPath("$.applicationDetails.aagResults[0].orderedDate", equalTo("2019-01-01")),
                                withJsonPath("$.applicationDetails.aagResults[0].lastSharedDateTime", equalTo("2019-02-01")),
                                withJsonPath("$.applicationDetails.aagResults[0].amendmentDate", equalTo("2019-03-01")),
                                withJsonPath("$.applicationDetails.aagResults[0].amendmentReason", equalTo("wording for amendment")),
                                withJsonPath("$.applicationDetails.aagResults[0].amendedBy", equalTo("delegatedPowers a delegatedPowers b")),
                                withJsonPath("$.applicationDetails.aagResults[0].resultText", equalTo("code - resultText")),
                                withJsonPath("$.applicationDetails.aagResults[0].useResultText", equalTo(true)),
                                withJsonPath("$.applicationDetails.paymentReference", equalTo(paymentReference)),
                                withJsonPath("$.applicantDetails.address.address1", equalTo(applicantAddress1)),
                                withJsonPath("$.applicantDetails.address.address2", equalTo(applicantAddress2)),
                                withJsonPath("$.applicantDetails.address.address3", equalTo(applicantAddress3)),
                                withJsonPath("$.applicantDetails.address.address4", equalTo(applicantAddress4)),
                                withJsonPath("$.applicantDetails.address.address5", equalTo(applicantAddress5)),
                                withJsonPath("$.applicantDetails.address.postcode", equalTo(applicantPostCode)),
                                withJsonPath("$.applicantDetails.interpreterLanguageNeeds", equalTo(interpreterLanguageNeeds)),
                                withJsonPath("$.applicantDetails.name", equalTo(format("%s %s", applicantFirstName, applicantLastName))),
                                withJsonPath("$.applicantDetails.representation", equalTo(applicantRepresentation)),
                                withJsonPath("$.respondentDetails[0].name", equalTo(respondentOrganisationName)),
                                withJsonPath("$.respondentDetails.length()", equalTo(2)),
                                withJsonPath("$.respondentDetails[0].address.address1", equalTo(respondentOrganisationAddress1)),
                                withJsonPath("$.respondentDetails[0].address.address2", equalTo(respondentOrganisationAddress2)),
                                withJsonPath("$.respondentDetails[0].address.address3", equalTo(respondentOrganisationAddress3)),
                                withJsonPath("$.respondentDetails[0].address.address4", equalTo(respondentOrganisationAddress4)),
                                withJsonPath("$.respondentDetails[0].address.address5", equalTo(respondentOrganisationAddress5)),
                                withJsonPath("$.respondentDetails[0].address.postcode", equalTo(respondentOrganisationPostcode)),
                                withJsonPath("$.respondentDetails[0].respondentRepresentatives[0].representativeName", equalTo(format("%s %s", respondentRepresentativeFirstName, respondentRepresentativeLastName))),
                                withJsonPath("$.respondentDetails[0].respondentRepresentatives[0].representativePosition", equalTo(respondentRepresentativePosition)),
                                withJsonPath("$.respondentDetails[1].name", equalTo("David lloyd")),
                                withJsonPath("$.respondentDetails[1].address.address1", equalTo("44, Wilson Patten Street")),
                                withJsonPath("$.linkedCases[0].prosecutionCaseId", equalTo(prosecutionCaseId)),
                                withJsonPath("$.linkedCases[0].prosecutionCaseIdentifier.prosecutionAuthorityId", equalTo(prosecutionAuthorityId)),
                                withJsonPath("$.linkedCases[0].prosecutionCaseIdentifier.prosecutionAuthorityCode", equalTo(prosecutionAuthorityCode)),
                                withJsonPath("$.linkedCases[0].prosecutionCaseIdentifier.prosecutionAuthorityReference", equalTo(prosecutionAuthorityReference))
//                                withJsonPath("$.linkedCases[0].offences.length()", equalTo(1)),
//                                withJsonPath("$.linkedCases[0].offences[0].wording", equalTo("Some offence wording"))
//                                withJsonPath("$.linkedCases[0].offences[0].aagResults.length()", equalTo(1)),
//                                withJsonPath("$.linkedCases[0].offences[0].aagResults[0].id", equalTo("f8e926eb-704a-457a-a794-8c3ad40d3113")),
//                                withJsonPath("$.linkedCases[0].offences[0].aagResults[0].label", equalTo("wording for linked case results")),
//                                withJsonPath("$.linkedCases[0].offences[0].aagResults[0].orderedDate", equalTo("2019-01-01")),
//                                withJsonPath("$.linkedCases[0].offences[0].aagResults[0].lastSharedDateTime", equalTo("2019-02-01")),
//                                withJsonPath("$.linkedCases[0].offences[0].aagResults[0].amendmentDate", equalTo("2019-03-01")),
//                                withJsonPath("$.linkedCases[0].offences[0].aagResults[0].amendmentReason", equalTo("wording for linked case amendment")),
//                                withJsonPath("$.linkedCases[0].offences[0].aagResults[0].amendedBy", equalTo("scott dale"))
                        )));
    }

    private void verifyLinkedApplications(final String applicationId, final String linkedApplicationId) {

        poll(requestParams(getReadUrl("/applications/" + applicationId), PROGRESSION_QUERY_APPLICATION_AAAG_JSON)
                .withHeader(USER_ID, randomUUID()))
                .timeout(TIMEOUT, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.applicationId", equalTo(applicationId)),
                                withJsonPath("$.linkedApplications[0].applicationId", equalTo(linkedApplicationId)),
                                withJsonPath("$.linkedApplications[0].applicantDisplayName", notNullValue()),
                                withJsonPath("$.linkedApplications[0].applicationReference", notNullValue()),
                                withJsonPath("$.linkedApplications[0].applicationStatus", notNullValue()),
                                withJsonPath("$.linkedApplications[0].applicationTitle", notNullValue()),
                                withJsonPath("$.linkedApplications[0].respondentDisplayNames", notNullValue())
                        )));
    }

    private void setupData() {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        courtApplicationId = randomUUID().toString();
        particulars = STRING.next();
        applicantReceivedDate = now().toLocalDate().toString();
        applicationType = STRING.next();
        appeal = BOOLEAN.next();
        applicantAppellantFlag = BOOLEAN.next();
        paymentReference = STRING.next();
        applicantSynonym = STRING.next();
        applicantFirstName = STRING.next();
        applicantLastName = STRING.next();
        applicantNationality = STRING.next();
        applicantRemandStatus = STRING.next();
        applicantRepresentation = STRING.next();
        interpreterLanguageNeeds = STRING.next();
        applicantDoB = PAST_LOCAL_DATE.next();
        applicantAddress1 = STRING.next();
        applicantAddress2 = STRING.next();
        applicantAddress3 = STRING.next();
        applicantAddress4 = STRING.next();
        applicantAddress5 = STRING.next();
        applicantPostCode = POST_CODE.next();
        applicationReference = STRING.next();
        respondentDefendantId = randomUUID().toString();
        respondentOrganisationName = STRING.next();
        respondentOrganisationAddress1 = STRING.next();
        respondentOrganisationAddress2 = STRING.next();
        respondentOrganisationAddress3 = STRING.next();
        respondentOrganisationAddress4 = STRING.next();
        respondentOrganisationAddress5 = STRING.next();
        respondentOrganisationPostcode = POST_CODE.next();
        respondentRepresentativeFirstName = STRING.next();
        respondentRepresentativeLastName = STRING.next();
        respondentRepresentativePosition = STRING.next();
        prosecutionCaseId = randomUUID().toString();
        prosecutionAuthorityId = randomUUID().toString();
        prosecutionAuthorityCode = STRING.next();
        prosecutionAuthorityReference = STRING.next();
    }
}

