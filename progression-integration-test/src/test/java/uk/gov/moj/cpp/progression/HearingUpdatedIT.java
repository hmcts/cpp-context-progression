package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.JsonPath.compile;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.matchDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.ReadContext;
import io.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class HearingUpdatedIT {

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final String PUBLIC_HEARING_DETAIL_CHANGED = "public.hearing-detail-changed";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";

    private static final String PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING = "public.events.listing.offences-removed-from-allocated-hearing";
    private static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression.prosecution-cases-referred-to-court";
    private static final String PUBLIC_PROGRESSION_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING = "public.progression.offences-removed-from-existing-allocated-hearing";

    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private static final JmsMessageConsumerClient messageConsumerClientPublicForReferToCourtOnHearingInitiated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicForHearingDetailChanged = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_HEARING_DETAIL_CHANGED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicOffenceRemovedFromHearing = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientProgressionPublicOffenceRemovedFromHearing = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerHearingPopulatedToProbationCaseWorker = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.events.hearing-populated-to-probation-caseworker").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerHearingOffenceUpdated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.hearing-offences-updated").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerListingNumberUpdated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecution-case-listing-number-updated").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerListingNumberIncreased = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecution-case-listing-number-increased").getMessageConsumerClient();

    private static final String OFFENCE_ID = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private static final String OFFENCE_ID2 = "4789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String caseId;
    private String defendantId;
    private String userId;
    private String courtCentreId;
    private String hearingId;

    @BeforeAll
    public static void setUpClass() {
        stubInitiateHearing();
    }
    private static final JmsMessageConsumerClient messageConsumerCourtDocumentAddedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.command.add-court-document").getMessageConsumerClient();


    @BeforeEach
    public void setUp() {
        stubDocumentCreate(randomAlphanumeric(20));
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
    }

    @Test
    public void shouldUpdateHearing() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        final JsonObject hearingConfirmedJson = getHearingConfirmedJsonObject(hearingId);
        final JsonEnvelope publicEventConfirmedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventConfirmedEnvelope);

        verifyInMessagingQueue(messageConsumerClientPublicForReferToCourtOnHearingInitiated);

        final Filter hearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(courtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", hearingIdFilter)));

        final String updatedCourtCentreId = randomUUID().toString();
        final JsonObject hearingUpdatedJson = getHearingUpdatedJsonObject(hearingId, updatedCourtCentreId);

        final JsonEnvelope publicEventUpdatedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, userId), hearingUpdatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventUpdatedEnvelope);

        final Filter updatedHearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(updatedCourtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", updatedHearingIdFilter)));
        verifyInMessagingQueue(messageConsumerClientPublicForHearingDetailChanged);
    }


    @Test
    public void shouldUpdateHearingWhenDefendantMatched() throws Exception {
        final String prosecutionCaseId_1 = randomUUID().toString();
        final String defendantId_1 = randomUUID().toString();
        final String masterDefendantId_1 = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        pollProsecutionCasesProgressionFor(caseId, new Matcher[]{
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].id", equalTo(defendantId)),
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", equalTo(defendantId))
        });
        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].id", Matchers.is(caseId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].id", Matchers.is(defendantId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].masterDefendantId", Matchers.is(defendantId))
        );

        matchDefendant(caseId, defendantId, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);

        pollProsecutionCasesProgressionFor(caseId, new Matcher[]{
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].id", equalTo(defendantId)),
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", equalTo(masterDefendantId_1))
        });
        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].id", Matchers.is(caseId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].id", Matchers.is(defendantId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].masterDefendantId", Matchers.is(masterDefendantId_1))
        );
    }

    @Test
    public void shouldUpdateHearingWhenCaseOffenceHasBeenUpdated() throws IOException, JSONException {
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        final JsonObject hearingConfirmedJson = getHearingConfirmedJsonObject(hearingId);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyInMessagingQueue(messageConsumerClientPublicForReferToCourtOnHearingInitiated);

        final Filter hearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(courtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath(
                        "$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", hearingIdFilter)));
        final String offenceId = hearingConfirmedJson.getJsonObject("confirmedHearing")
                .getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences")
                .getJsonObject(0).getString("id");
        ProsecutionCaseUpdateOffencesHelper helper = new ProsecutionCaseUpdateOffencesHelper(caseId, defendantId, offenceId);
        // when
        helper.updateOffences();
        verifyInMessagingQueueForHearingOffenceUpdated(hearingId, offenceId);
        verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId);
    }

    @Disabled("DD-33449")
    @Test
    public void shouldHearingWithApplicationWhenLinkedApplicationToHearing() throws Exception {
        String prosecutionAuthorityReference;
        String courtCentreName = "Lavender Hill Magistrate's Court";

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId, defendantId);
        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        prosecutionAuthorityReference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        String courtApplicationId = randomUUID().toString();
        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtApplicationId, randomUUID().toString(), prosecutionAuthorityReference, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);


        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, hearingId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application-adjorn.json");
        final JsonPath matchers1 = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(Matchers.allOf(
                        withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                        withJsonPath("$.hearing.courtApplications[0].id", is(courtApplicationId))
                )
        ));
        assertNotNull(matchers1);
    }

    @Test
    public void shouldRaiseProbationEventWhenAllocationChanged() throws Exception {
        HearingStub.stubInitiateHearing();
        final String applicationId = randomUUID().toString();
        final String hearingId;
        addStandaloneCourtApplication(applicationId, UUID.randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");
        pollForApplication(applicationId);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();

        final JsonEnvelope publicEventConfirmedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingWithStandAloneApplicationJsonObject("public.listing.hearing-confirmed-application-with-linked-case.json",
                applicationId, hearingId, caseId, defendantId, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventConfirmedEnvelope);

        pollForApplicationStatus(applicationId, "LISTED");
        final JsonPath matchers1 = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(Matchers.allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.courtCentre.address.address1", is("176a Lavender Hill")),
                withJsonPath("$.hearing.courtCentre.code", is("B01LY00")),
                withJsonPath("$.hearing.hearingDays[0].sittingDay", is("2018-09-28T12:13:00.000Z")),
                withJsonPath("$.hearing.hearingLanguage", is("ENGLISH")),
                withJsonPath("$.hearing.type.description", is("Sentence")),
                withJsonPath("$.hearing.type.id", is("cf73207f-3ced-488a-82a0-3fba79c2ce04"))
        )));
        assertNotNull(matchers1);


        final String updatedCourtCentreId = randomUUID().toString();
        final JsonObject hearingUpdatedJson = getHearingUpdatedForApplicationJsonObject(hearingId, updatedCourtCentreId, applicationId);

        final JsonEnvelope publicEventUpdatedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, userId), hearingUpdatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventUpdatedEnvelope);

        final JsonPath matchers2 = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(Matchers.allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.courtCentre.id", is("d9bff7d8-6168-4163-ad77-3b98d61de174")),
                withJsonPath("$.hearing.hearingDays[0].sittingDay", is("2018-09-28T12:13:00.000Z")),
                withJsonPath("$.hearing.hearingLanguage", is("WELSH")),
                withJsonPath("$.hearing.type.description", is("Application")),
                withJsonPath("$.hearing.type.id", is("cf73207f-3ced-488a-82a0-3fba79c2ce05"))
        )));
        assertNotNull(matchers2);
    }


    @Test()
    public void shouldUpdateHearingWhenHearingListedWithListingNumber() throws IOException, JMSException, JSONException {
        String courtCentreName = "Lavender Hill Magistrate's Court";
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));
        String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        final String prosecutionAuthorityReference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), randomUUID().toString(), prosecutionAuthorityReference, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);


        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerListingNumberIncreased, isJson(withJsonPath("$.prosecutionCaseId", CoreMatchers.is(caseId))));
        assertNotNull(message);
        final List<HashMap> listingNumbers = message.getList("offenceListingNumbers", HashMap.class);
        assertThat(listingNumbers.get(0).get("offenceId"), is(OFFENCE_ID));
        assertThat(listingNumbers.get(0).get("listingNumber"), is(1));

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].id", Matchers.is(caseId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].id", Matchers.is(defendantId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(OFFENCE_ID)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber", is(1))
        );

        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.id", Matchers.is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].id", Matchers.is(defendantId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is(OFFENCE_ID)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].listingNumber", is(1)));

        JsonPath messageDaysMatchers = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(Matchers.allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber", is(1)))));

        assertNotNull(messageDaysMatchers);

    }

    @Test()
    public void shouldUpdateHearingWhenHearingListedWithListingNumberForSomeOffences() throws IOException, JMSException, JSONException {
        String courtCentreName = "Lavender Hill Magistrate's Court";
        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));
        String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        final String prosecutionAuthorityReference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), randomUUID().toString(), prosecutionAuthorityReference, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);


        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerListingNumberIncreased, isJson(withJsonPath("$.prosecutionCaseId", CoreMatchers.is(caseId))));
        assertNotNull(message);
        final List<HashMap> listingNumbers = message.getList("offenceListingNumbers", HashMap.class);
        assertThat(listingNumbers.size(), is(1));
        assertThat(listingNumbers.get(0).get("offenceId"), is(OFFENCE_ID));
        assertThat(listingNumbers.get(0).get("listingNumber"), is(1));

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].id", Matchers.is(caseId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].id", Matchers.is(defendantId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(OFFENCE_ID)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber", is(1)),
                withoutJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[1]")
        );

        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.id", Matchers.is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].id", Matchers.is(defendantId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is(OFFENCE_ID)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].listingNumber", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].id", is(OFFENCE_ID2)),
                withoutJsonPath("$.prosecutionCase.defendants[0].offences[1].listingNumber"));

        JsonPath messageDaysMatchers = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(Matchers.allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber", is(1)),
                withoutJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[1]")
        )));

        assertNotNull(messageDaysMatchers);

        verifyInMessagingQueue(messageConsumerClientProgressionPublicOffenceRemovedFromHearing);
    }

    @Test
    public void shouldRemoveOffenceFromHearing() throws Exception {
        final String offenceId1 = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        final String offenceId2 = "4789ab16-0bb7-4ef1-87ef-c936bf0364f1";

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        final JsonObject hearingConfirmedJson = getHearingConfirmedWithTwoOffencesJsonObject(hearingId, offenceId1, offenceId2);

        final JsonEnvelope publicEventConfirmedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventConfirmedEnvelope);

        verifyInMessagingQueue(messageConsumerClientPublicForReferToCourtOnHearingInitiated);

        final Filter hearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(courtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));
        final Filter offencesFilter = filter(where("offences").size(2)
                .and("offence[0].id").is(offenceId1)
                .and("offence[1].id").is(offenceId2));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", hearingIdFilter)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[0].defendants[0]", offencesFilter)));

        final JsonObject hearingOffenceRemovedJson = getOffenceRemovedFromExistngHearingJsonObject(hearingId, offenceId1);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING, userId), hearingOffenceRemovedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING, publicEventEnvelope);

        final Filter updatedHearingIdFilter = filter(where("id").is(hearingId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));
        final Filter updatedOffencesFilter = filter(where("offences").size(1)
                .and("offence[0].id").is(offenceId2));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", updatedHearingIdFilter)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[0].defendants[0]", updatedOffencesFilter)));
        verifyInMessagingQueue(messageConsumerClientPublicOffenceRemovedFromHearing);
    }

    @Test
    public void shouldRemoveWholeDefendant() throws Exception {
        final String offenceId1 = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        final String offenceId2 = "4789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1));
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId1, getProsecutionCaseMatchers(caseId, defendantId1));

        final JsonObject hearingConfirmedJson = getHearingConfirmedWithTwoDefendantsJsonObject(hearingId, defendantId1, defendantId2);

        final JsonEnvelope publicEventConfirmedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventConfirmedEnvelope);

        verifyInMessagingQueue(messageConsumerClientPublicForReferToCourtOnHearingInitiated);

        final Filter hearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(courtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));
        final Filter defendantsFilter = filter(where("defendants").size(2)
                .and("defendants[0].id").is(defendantId1)
                .and("defendants[1].id").is(defendantId2));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", hearingIdFilter)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[0].defendants[0]", defendantsFilter)));

        final JsonObject hearingOffenceRemovedJson = getOffenceRemovedFromExistngHearingJsonObject(hearingId, offenceId1);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING, userId), hearingOffenceRemovedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING, publicEventEnvelope);

        final Filter updatedDefendantsFilter = filter(where("id").is(hearingId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));
        final Filter updatedOffencesFilter = filter(where("defendants").size(1)
                .and("defendants[0].id").is(defendantId2));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", updatedDefendantsFilter)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[0].defendants[0]", updatedOffencesFilter)));
        verifyInMessagingQueue(messageConsumerClientPublicOffenceRemovedFromHearing);

    }

    @Test
    public void shouldUpdateHearing_SendNotificationToParties() throws Exception {

        final JmsMessageConsumerClient messageConsumerEmailRequestPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.email-requested").getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerPrintRequestPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.print-requested").getMessageConsumerClient();

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor-noncps-no-email.json", randomUUID());

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        final JsonObject hearingConfirmedJson = getHearingConfirmedJsonObject(hearingId);

        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyInMessagingQueue(messageConsumerClientPublicForReferToCourtOnHearingInitiated);

        final Filter hearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(courtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", hearingIdFilter)));

        final String updatedCourtCentreId = randomUUID().toString();
        final JsonObject hearingUpdatedJson = getHearingUpdatedJsonObjectWithNotificationFlagTrue(hearingId, updatedCourtCentreId);

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, userId), hearingUpdatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventEnvelope);


        final Filter updatedHearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(updatedCourtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", updatedHearingIdFilter)));
        verifyInMessagingQueue(messageConsumerClientPublicForHearingDetailChanged);
        doVerifyListHearingRequestedPrivateEvent(messageConsumerEmailRequestPrivateEvent, caseId);
        doVerifyListHearingRequestedPrivateEvent(messageConsumerPrintRequestPrivateEvent, caseId);
        verifyAddCourtDocument(messageConsumerCourtDocumentAddedPrivateEvent);
    }

    private void doVerifyListHearingRequestedPrivateEvent(final JmsMessageConsumerClient messageConsumerProgressionCommandEmail, final String caseId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProgressionCommandEmail);
        assertThat(message.get(), Matchers.notNullValue());
        final JsonObject progressionCommandNotificationEvent = message.get();
        assertThat(progressionCommandNotificationEvent.getString("caseId", EMPTY), Matchers.is(caseId));
    }

    private void verifyAddCourtDocument(JmsMessageConsumerClient messageConsumerCourtDocumentAddedPrivateEvent) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerCourtDocumentAddedPrivateEvent);
        assertThat(message.get(), Matchers.notNullValue());
        final JsonObject progressionCourtDocumentAddedEvent = message.get();
        JsonObject courtDocument = progressionCourtDocumentAddedEvent.getJsonObject("courtDocument");
        assertThat(courtDocument.getString("documentTypeDescription"), containsString("Electronic Notifications"));
        assertThat(courtDocument.getString("name"), containsString("AmendedHearingNotification"));
        assertThat(courtDocument.getBoolean("containsFinancialMeans"), Matchers.is(false));
        assertThat(courtDocument.getBoolean("sendToCps"), Matchers.is(false));
    }

    List<Matcher<? super ReadContext>> matchers = newArrayList(
            withJsonPath("$.prosecutionCase.id", is(caseId)),
            withJsonPath("$.prosecutionCase.originatingOrganisation", is("G01FT01AB")),
            withJsonPath("$.prosecutionCase.initiationCode", is("J")),
            withJsonPath("$.prosecutionCase.statementOfFacts", is("You did it")),
            withJsonPath("$.prosecutionCase.statementOfFactsWelsh", is("You did it in Welsh"))
    );

    private JsonObject getHearingConfirmedJsonObject(final String hearingId) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-confirmed.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }

    private JsonObject getHearingConfirmedWithTwoOffencesJsonObject(final String hearingId, final String offenceId1, final String offenceId2) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-confirmed-with-two-offences.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("OFFENCE_ID_1", offenceId1)
                        .replaceAll("OFFENCE_ID_2", offenceId2)
        );
    }

    private JsonObject getHearingConfirmedWithTwoDefendantsJsonObject(final String hearingId, final String defendantId1, final String defendantId2) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-confirmed-one-case-two-defendants.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("DEFENDANT_ID_1", defendantId1)
                        .replaceAll("DEFENDANT_ID_2", defendantId2)
        );
    }


    private JsonObject getOffenceRemovedFromExistngHearingJsonObject(final String hearingId, final String offenceIdToRemove) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.hearing.selected-offences-removed-from-existing-hearing.json")
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("OFFENCE_ID_TO_REMOVE", offenceIdToRemove)
        );
    }

    private JsonObject getHearingUpdatedJsonObject(final String hearingId, final String courtCentreId) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-updated.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }

    private JsonObject getHearingUpdatedJsonObjectWithNotificationFlagTrue(final String hearingId, final String courtCentreId) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-updated-notification-flag-true.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }

    private JsonObject getHearingUpdatedForApplicationJsonObject(final String hearingId, final String courtCentreId, final String applicationId) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-updated-for-application.json")
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("APPLICATION_ID", applicationId)
        );
    }

    private static void verifyInMessagingQueue(final JmsMessageConsumerClient consumer) {
        final Optional<JsonObject> message = retrieveMessageBody(consumer);
        assertThat(message.isPresent(), is(true));
    }

    private void verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(final String hearingId) {
        final JsonPath retrieveMessage = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(allOf(
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].offenceCode", is("TFL123")))));
        assertThat(retrieveMessage, notNullValue());
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private JsonObject getHearingWithStandAloneApplicationJsonObject(final String path, final String applicationId, final String hearingId, final String caseId, final String defendantId, final String courtCentreId) {
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String applicationId,
                                            final String adjournedHearingId, final String reference,
                                            final String courtCentreId, final String courtCentreName) {
        return getHearingJsonObject(path, caseId, hearingId, defendantId, applicationId, adjournedHearingId, reference, courtCentreId, courtCentreName, "2020-01-01");
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String applicationId,
                                            final String adjournedHearingId, final String reference,
                                            final String courtCentreId, final String courtCentreName,
                                            final String orderedDate) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("APPLICATION_ID", applicationId)
                        .replaceAll("ADJOURNED_ID", adjournedHearingId)
                        .replaceAll("APPLICATION_REF", reference)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                        .replaceAll("OFFENCE_ID", "3789ab16-0bb7-4ef1-87ef-c936bf0364f1")
                        .replaceAll("ORDERED_DATE", orderedDate)
        );
    }

    private  void verifyInMessagingQueueForHearingOffenceUpdated(final String hearingId, final String offenceId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerHearingOffenceUpdated);
        assertTrue(message.isPresent());
        final JsonObject jsonObject = message.get();
        assertThat(jsonObject.getString("hearingId"), CoreMatchers.is(hearingId));
        assertThat(jsonObject.getJsonArray("updatedOffences").getJsonObject(0).getString("id"), is(offenceId));
    }
}
