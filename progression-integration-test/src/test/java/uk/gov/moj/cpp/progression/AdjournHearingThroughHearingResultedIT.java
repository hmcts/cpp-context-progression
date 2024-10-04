package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.CourtSchedulerServiceStub.stubGetProvisionalBookedSlotsForNonExistingBookingId;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyListNextHearingRequestsAsStreamV2;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearingV2;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.ListingStub;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class AdjournHearingThroughHearingResultedIT extends AbstractIT {
    private final String DOCUMENT_TEXT = STRING.next();
    private static final String APPLICATION_REFERRED_AND_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.court-application-created").getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForCourtApplicationUpdated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.hearing-resulted-application-updated").getMessageConsumerClient();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;
    private String newCourtCentreId;
    private String newCourtCentreName;
    private String applicationId;


    @BeforeEach
    public void setUp() {
        stubDocumentCreate(DOCUMENT_TEXT);
        HearingStub.stubInitiateHearing();
        IdMapperStub.setUp();
        ListingStub.stubListCourtHearing();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        newCourtCentreName = "Narnia Magistrate's Court";
        applicationId = randomUUID().toString();

    }


    @Test
    public void shouldCallListingToNewHearingWithCourtOrderV2() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        final String prosecutionAuthorityReference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, applicationId, randomUUID().toString(), prosecutionAuthorityReference, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        final String courtApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, hearingId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application-adjorn.json");
        verifyCourtApplicationCreatedPrivateEvent();

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.applicationStatus", is("LISTED")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication..courtOrder.courtOrderOffences[0].prosecutionCaseId", notNullValue()),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].prosecutionCaseIdentifier.caseURN", is("TVL1234")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test"))
        };

        pollForCourtApplication(courtApplicationId, applicationMatchers);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final String adjournedHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject("public.events.hearing.hearing-resulted.application-adjourned-to-next-hearing-with-court-order.json", caseId,
                hearingId, defendantId, courtApplicationId, adjournedHearingId, prosecutionAuthorityReference, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventResultedEnvelope);

        verifyCourtApplicationUpdatedPrivateEvent();

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].lastAdjournDate", Matchers.is("2020-01-01")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].lastAdjournedHearingType", Matchers.is("adjournmentReason")),
                withJsonPath("$.hearing.courtApplications[0].courtOrder.courtOrderOffences[0].offence.lastAdjournDate", Matchers.is("2020-06-27")),
                withJsonPath("$.hearing.courtApplications[0].courtOrder.courtOrderOffences[0].offence.lastAdjournedHearingType", Matchers.is("adjournmentReason"))
        );

        final Matcher[] adjournOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].lastAdjournDate", is("2020-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].lastAdjournedHearingType", is("adjournmentReason"))
        };

        pollProsecutionCasesProgressionFor(caseId, adjournOffenceUpdatedMatchers);

        verifyListNextHearingRequestsAsStreamV2(hearingId, "1 week");
    }


    @Test
    public void shouldAdjournApplicationToNewHearingInMagistrateV2() throws Exception {

        stubGetProvisionalBookedSlotsForNonExistingBookingId();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        String prosecutionAuthorityReference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, applicationId, randomUUID().toString(), prosecutionAuthorityReference, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, hearingId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");
        verifyCourtApplicationCreatedPrivateEvent();

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].prosecutionCaseId", notNullValue()),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].prosecutionCaseIdentifier.caseURN", is("TVL1234")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test"))
        };

        pollForApplication(applicationId, applicationMatchers);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String adjournedHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject("public.events.hearing.hearing-resulted.application-adjourned-to-next-hearing-in-mag-with-non-existing-booking-ref.json", caseId,
                hearingId, defendantId, applicationId, adjournedHearingId, prosecutionAuthorityReference, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventResultedEnvelope);

        final Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].id", hasItem(hearingId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(newCourtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.name", hasItem(newCourtCentreName)),
                withJsonPath("$.hearingsAtAGlance.defendantHearings.[*].defendantId", hasItem(defendantId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].address.address2", hasItem("Leamington Avenue")) // Need to fix why this is not working
        };

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);

    }


    @Test
    public void shouldKeepLastAdjournValuesWhenCourtApplicationHasCaseAdjournedV2() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        final String prosecutionAuthorityReference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, applicationId, randomUUID().toString(), prosecutionAuthorityReference, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        final String courtApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, hearingId, "applications/progression.initiate-court-proceedings-for-generic-linked-application.json");
        verifyCourtApplicationCreatedPrivateEvent();

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.applicationStatus", is("LISTED")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences", notNullValue()),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test"))
        };

        pollForCourtApplication(courtApplicationId, applicationMatchers);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final String adjournedHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject("public.hearing.resulted.application-adjourned-to-next-hearing-with-application-case-V2.json", caseId,
                hearingId, defendantId, courtApplicationId, adjournedHearingId, prosecutionAuthorityReference, newCourtCentreId, newCourtCentreName, "2021-05-26"));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventResultedEnvelope);

        verifyPostListCourtHearingV2();

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].lastAdjournDate", Matchers.is("2021-05-26")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].lastAdjournedHearingType", Matchers.is("adjournmentReason"))
        );

        final Matcher[] adjournOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].lastAdjournDate", is("2021-05-26")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].lastAdjournedHearingType", is("adjournmentReason"))
        };

        pollProsecutionCasesProgressionFor(caseId, adjournOffenceUpdatedMatchers);

        final JsonEnvelope publicEventResultedEnvelope2 = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject("public.hearing.resulted.application-adjourned-to-next-hearing-with-application-case-V2.json", caseId,
                hearingId, defendantId, courtApplicationId, adjournedHearingId, prosecutionAuthorityReference, newCourtCentreId, newCourtCentreName, "2021-05-27"));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventResultedEnvelope2);

        verifyPostListCourtHearingV2();

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].lastAdjournDate", Matchers.is("2021-05-27")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].lastAdjournedHearingType", Matchers.is("adjournmentReason"))
        );

        final Matcher[] adjournOffenceUpdatedMatchers2 = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].lastAdjournDate", is("2021-05-27")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].lastAdjournedHearingType", is("adjournmentReason"))
        };

        pollProsecutionCasesProgressionFor(caseId, adjournOffenceUpdatedMatchers2);

        final JsonEnvelope publicEventResultedEnvelope3 = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject("public.hearing.resulted.application-adjourned-to-next-hearing-with-application-case-V2.json", caseId,
                hearingId, defendantId, courtApplicationId, adjournedHearingId, prosecutionAuthorityReference, newCourtCentreId, newCourtCentreName, "2021-05-25"));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventResultedEnvelope3);

        verifyPostListCourtHearingV2();

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].lastAdjournDate", Matchers.is("2021-05-27")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].lastAdjournedHearingType", Matchers.is("adjournmentReason"))
        );

        pollProsecutionCasesProgressionFor(caseId, adjournOffenceUpdatedMatchers2);

        verifyListNextHearingRequestsAsStreamV2(hearingId, "1 week");
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
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

    private void verifyCourtApplicationCreatedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(applicationReference, is(notNullValue()));
    }

    private void verifyCourtApplicationUpdatedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationUpdated);
        assertTrue(message.isPresent());
    }
}

