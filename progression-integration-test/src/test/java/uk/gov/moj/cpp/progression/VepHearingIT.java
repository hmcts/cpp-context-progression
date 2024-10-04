package uk.gov.moj.cpp.progression;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatRequestIsAccepted;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorDataForGivenProsecutionAuthorityId;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorsByOucode;
import static uk.gov.moj.cpp.progression.stub.VejHearingStub.verifyHearingCommandInvoked;
import static uk.gov.moj.cpp.progression.stub.VejHearingStub.verifyHearingDeletedCommandInvoked;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.VejHearingStub;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.ReadContext;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VepHearingIT extends AbstractIT {
    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final JmsMessageConsumerClient messageConsumerVejHearingPopulatedToProbationCaseWorker = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.events.vej-hearing-populated-to-probation-caseworker").getMessageConsumerClient();
    private final JmsMessageConsumerClient messageConsumerVejDeletedHearingPopulatedToProbationCaseWorker = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.events.vej-deleted-hearing-populated-to-probation-caseworker").getMessageConsumerClient();

    private static final String PUBLIC_EVENTS_LISTING_HEARING_DELETED = "public.events.listing.hearing-deleted";
    public static final String PROGRESSION_EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V_2 = "progression.event.prosecutionCase-defendant-listing-status-changed-v2";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;

    private String ouCode;
    private String prosecutionAuthorityId;

    @BeforeEach
    public void setUp() {
        cleanViewStoreTables();
        stubDocumentCreate(DOCUMENT_TEXT);
        HearingStub.stubInitiateHearing();
        VejHearingStub.stubVejHearing();
        VejHearingStub.stubVejHearingDeleted();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        ouCode = randomAlphanumeric(7);
        prosecutionAuthorityId = randomUUID().toString();
        WireMock.removeStub(get(urlMatching("/referencedata-service/query/api/rest/referencedata/prosecutors.*")));
        // test specific stub to ensure prosecutor data is unique and consistent
        stubQueryProsecutorDataForGivenProsecutionAuthorityId("restResource/referencedata.query.police.prosecutor.json", prosecutionAuthorityId, ouCode);
        stubQueryProsecutorsByOucode("restResource/referencedata.query.police.prosecutor.json", prosecutionAuthorityId, ouCode);
    }

    @Test
    public void shouldRaiseHearingPopulatedToProbationCaseWorkerWhenPublicListingHearingConfirmed() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V_2).getMessageConsumerClient();

        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.police.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V_2).getMessageConsumerClient();


        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        verifyInMessagingQueueForVejHearingPopulatedToProbationCaseWorker(messageConsumerVejHearingPopulatedToProbationCaseWorker);
        verifyHearingCommandInvoked(asList(caseId, hearingId));

    }

    @Test
    public void shouldRaiseHearingDeletedToProbationCaseWorkerWhenPublicListingHearingDeleted() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V_2).getMessageConsumerClient();

        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.police.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V_2).getMessageConsumerClient();
        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata("public.events.hearing.hearing-resulted", userId), getHearingWithSingleCaseJsonObject("public.hearing.resulted-case-updated2.json", caseId,
                hearingId, defendantId, courtCentreId, "C", "Remedy", "2593cf09-ace0-4b7d-a746-0703a29f33b5"));
        messageProducerClientPublic.sendMessage("public.events.hearing.hearing-resulted", publicEventEnvelope);

        final JsonObject hearingDeletedJson = getHearingMarkedAsDeletedObject(hearingId);
        final JsonEnvelope publicEventDeletedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_LISTING_HEARING_DELETED, userId), hearingDeletedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_LISTING_HEARING_DELETED, publicEventDeletedEnvelope);

        verifyInMessagingQueueForDeletedHearingPopulatedToProbationCaseWorker(messageConsumerVejDeletedHearingPopulatedToProbationCaseWorker);
        verifyHearingDeletedCommandInvoked(asList(caseId, hearingId));
    }

    public static void verifyInMessagingQueueForVejHearingPopulatedToProbationCaseWorker(final JmsMessageConsumerClient messageConsumerVejHearingPopulatedToProbationCaseWorker) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerVejHearingPopulatedToProbationCaseWorker);
        assertTrue(message.isPresent());
    }

    public static void verifyInMessagingQueueForDeletedHearingPopulatedToProbationCaseWorker(final JmsMessageConsumerClient messageConsumerVejDeletedHearingPopulatedToProbationCaseWorker) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerVejDeletedHearingPopulatedToProbationCaseWorker);
        assertTrue(message.isPresent());
    }

    public static String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                        final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                        final String caseUrn, final String prosecutionAuthorityId, final String ouCode, final String filePath) {
        return getPayload(filePath)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("RR_ORDERED_DATE", LocalDate.now().toString())
                .replace("RANDOM_PROSECUTION_AUTHORITY_ID", prosecutionAuthorityId)
                .replace("RANDOM_OU_CODE", ouCode);
    }

    public static Matcher<? super ReadContext>[] getProsecutionCaseMatchers(final String caseId, final String defendantId) {
        return getProsecutionCaseMatchers(caseId, defendantId, Collections.emptyList());

    }

    public static Matcher<? super ReadContext>[] getProsecutionCaseMatchers(final String caseId, final String defendantId, final List<Matcher<? super ReadContext>> additionalMatchers) {
        final List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.originatingOrganisation", is("0450000")),
                withJsonPath("$.prosecutionCase.initiationCode", is("J")),
                withJsonPath("$.prosecutionCase.statementOfFacts", is("You did it")),
                withJsonPath("$.prosecutionCase.statementOfFactsWelsh", is("You did it in Welsh"))
        );

        matchers.addAll(getDefendantMatchers(caseId, defendantId));
        matchers.addAll(getDefendantOffenceMatchers());
        matchers.addAll(getOffenceFactMatchers());
        matchers.addAll(getNotifyPleatMatchers());
        matchers.addAll(getPersonMatchers());
        matchers.addAll(getPersonAddressMatchers());
        matchers.addAll(getPersonContactDetailsMatchers());
        matchers.addAll(getPersonDefendantMatchers());

        matchers.addAll(additionalMatchers);

        return matchers.toArray(new Matcher[0]);

    }

    public static List<Matcher<? super ReadContext>> getDefendantMatchers(final String caseId, final String defendantId) {
        return newArrayList(
                // defendant assertion
                withJsonPath("$.prosecutionCase.defendants[0].id", is(defendantId)),
                withJsonPath("$.prosecutionCase.defendants[0].prosecutionCaseId", is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].prosecutionAuthorityReference", is("SURRPF")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567"))
        );
    }

    public static List<Matcher<? super ReadContext>> getDefendantOffenceMatchers() {
        return newArrayList(
                // defendant offence assertion
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceDefinitionId", is("490dce00-8591-49af-b2d0-1e161e7d0c36")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].wording", is("No Travel Card")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].wordingWelsh", is("No Travel Card In Welsh")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].startDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].endDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].arrestDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].chargeDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].orderIndex", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].count", is(0)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY"))
        );
    }

    public static ArrayList<Matcher<? super ReadContext>> getOffenceFactMatchers() {
        return newArrayList(
                // offence facts
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceFacts.vehicleRegistration", is("AA12345")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceFacts.alcoholReadingAmount", is(111)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceFacts.alcoholReadingMethodCode", is("2222"))
        );
    }

    public static ArrayList<Matcher<? super ReadContext>> getNotifyPleatMatchers() {
        return newArrayList(
                // notified plea
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].notifiedPlea.offenceId", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].notifiedPlea.notifiedPleaDate", is("2018-04-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].notifiedPlea.notifiedPleaValue", is("NOTIFIED_GUILTY"))
        );
    }

    public static ArrayList<Matcher<? super ReadContext>> getPersonMatchers() {
        return newArrayList(
                // assert person
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.title", is("DR")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.middleName", is("Jack")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.lastName", is("Kane")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.dateOfBirth", is("1995-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.nationalityId", is("2daefec3-2f76-8109-82d9-2e60544a6c02")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.additionalNationalityId", is("2daefec3-2f76-8109-82d9-2e60544a6c02")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.disabilityStatus", is("a")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.gender", is("MALE")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.interpreterLanguageNeeds", is("Tamil")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.documentationLanguageNeeds", is("WELSH")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.nationalInsuranceNumber", is("NH222222B")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.occupation", is("Footballer")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.occupationCode", is("F"))
        );
    }

    public static ArrayList<Matcher<? super ReadContext>> getPersonAddressMatchers() {
        return newArrayList(
                // person address
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address1", is("22")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address2", is("Acacia Avenue")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address3", is("Acacia Town")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address4", is("Acacia City")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address5", is("Acacia Country")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.postcode", is("CR7 0AA"))
        );
    }

    public static ArrayList<Matcher<? super ReadContext>> getPersonContactDetailsMatchers() {
        return newArrayList(
                // person contact details
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.home", is("123456")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.work", is("7891011")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.mobile", is("+45678910")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.primaryEmail", is("harry.kane@spurs.co.uk")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.secondaryEmail", is("harry.kane@hotmail.com")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.fax", is("3425678"))
        );
    }

    public static ArrayList<Matcher<? super ReadContext>> getPersonDefendantMatchers() {
        return newArrayList(
                // person defendant details
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.title", is("DR")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.middleName", is("Jack")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.lastName", is("Kane Junior")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.ethnicity.observedEthnicityId", is("2daefec3-2f76-8109-82d9-2e60544a6c02")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.ethnicity.selfDefinedEthnicityId", is("2daefec3-2f76-8109-82d9-2e60544a6c02")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.id", is("2593cf09-ace0-4b7d-a746-0703a29f33b5")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.code", is("C")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.description", is("Remanded into Custody")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.driverNumber", is("AACC12345")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.employerOrganisation.name", is("Disneyland Paris")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.employerOrganisation.incorporationNumber", is("Mickeymouse1"))
        );
    }

    private JsonObject getHearingMarkedAsDeletedObject(final String hearingId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.events.listing.hearing-deleted.json")
                        .replaceAll("HEARING_ID", hearingId)
        );
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
        );
    }

    public void addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(final String caseId, final String defendantId) throws IOException, JSONException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(), prosecutionAuthorityId, ouCode, "progression.command.police.prosecution-case-refer-to-court-one-grown-defendant-two-offences.json"));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        final String caseCreationPayload = jsonPayload.toString();
        final Response response = postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                caseCreationPayload);
        assertThatRequestIsAccepted(response);
    }
    private JsonObject getHearingWithSingleCaseJsonObject(final String path, final String caseId, final String hearingId,
                                                          final String defendantId, final String courtCentreId, final String bailStatusCode,
                                                          final String bailStatusDescription, final String bailStatusId) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("BAIL_STATUS_ID", bailStatusId)
                        .replaceAll("BAIL_STATUS_CODE", bailStatusCode)
                        .replaceAll("BAIL_STATUS_DESCRIPTION", bailStatusDescription)
                        .replaceAll("APPLICATION_ID", randomUUID().toString())
        );
    }

}
