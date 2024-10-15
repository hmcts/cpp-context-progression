package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.ejectCaseApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.AzureSteCaseFilterServiceStub.stubPostSetCaseEjected;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EjectCaseApplicationIT extends AbstractIT {
    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    private static final String CASE_OR_APPLICATION_EJECTED
            = "public.progression.events.case-or-application-ejected";

    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String REMOVAL_REASON = "Legal";
    private static final String STATUS_EJECTED = "EJECTED";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_UN_ALLOCATED = "UN_ALLOCATED";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;
    private String userId;

    private static final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

    @BeforeEach
    public void setUp() {
        stubInitiateHearing();
        DocumentGeneratorStub.stubDocumentCreate(STRING.next());
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        userId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        courtCentreName = "Lavender Hill Magistrate's Court";

    }


    @SuppressWarnings("squid:S1607")
    @Test
    public void shouldEjectStandaloneCourtApplicationAndGetConfirmation() throws Exception {

        String newCourtCentreName = "Narnia Magistrate's Court";
        String applicationId = randomUUID().toString();

        final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPublicJmsMessageConsumerClientProvider().withEventNames(COURT_APPLICATION_CREATED).getMessageConsumerClient();

        addStandaloneCourtApplication(applicationId, randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated(consumerForCourtApplicationCreated);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        String prosecutionAuthorityReference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getHearingJsonObject("public.listing.hearing-confirmed-applications-only.json",
                caseId, hearingId, defendantId, courtCentreId, applicationId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollForApplicationStatus(applicationId, "LISTED");
        final String adjournedHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        final JmsMessageConsumerClient consumerForCaseOrApplicationEjected = newPublicJmsMessageConsumerClientProvider().withEventNames(CASE_OR_APPLICATION_EJECTED).getMessageConsumerClient();

        ejectCaseApplication(applicationId, caseId, REMOVAL_REASON, "eject/progression.eject-application.json");

        Matcher[] applicationEjectedMatchers = {
                withJsonPath("$.courtApplication.id", equalTo(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", equalTo(STATUS_EJECTED)),
                withJsonPath("$.courtApplication.removalReason", equalTo(REMOVAL_REASON))
        };
        pollForApplication(applicationId, applicationEjectedMatchers);


        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingResultedJsonObject("public.events.hearing.hearing-resulted.application-ejected.json", caseId,
                hearingId, defendantId, courtCentreId, applicationId,adjournedHearingId, prosecutionAuthorityReference, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        verifyInMessagingQueueForCaseOrApplicationEjected(consumerForCaseOrApplicationEjected);

        pollForResponse("/hearingSearch/" + hearingId,
                PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.courtApplications[0].applicationStatus", is(STATUS_EJECTED)));
    }

    @Test
    public void shouldEjectStandaloneCourtApplicationWithoutHearingIdAndGetConfirmation() throws Exception {

        String applicationId = randomUUID().toString();

        final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPublicJmsMessageConsumerClientProvider().withEventNames(COURT_APPLICATION_CREATED).getMessageConsumerClient();


        addStandaloneCourtApplication(applicationId, randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated(consumerForCourtApplicationCreated);

        pollForApplicationStatus(applicationId, STATUS_DRAFT);

        final JmsMessageConsumerClient consumerForCaseOrApplicationEjected = newPublicJmsMessageConsumerClientProvider().withEventNames(CASE_OR_APPLICATION_EJECTED).getMessageConsumerClient();

        ejectCaseApplication(applicationId, caseId, REMOVAL_REASON, "eject/progression.eject-application.json");

        Matcher[] matchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is(STATUS_EJECTED)),
                withJsonPath("$.courtApplication.removalReason", is(REMOVAL_REASON))
        };
        pollForApplication(applicationId, matchers);

        verifyInMessagingQueueForCaseOrApplicationEjected(consumerForCaseOrApplicationEjected);
    }

    @Test
    public void shouldCreateCourtApplicationLinkedWithCaseAndGetConfirmation() throws Exception {
        stubPostSetCaseEjected();
        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        // Creating first application for the case
        String firstApplicationId = randomUUID().toString();

        final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPublicJmsMessageConsumerClientProvider().withEventNames(COURT_APPLICATION_CREATED).getMessageConsumerClient();

        initiateCourtProceedingsForCourtApplication(firstApplicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(firstApplicationId, consumerForCourtApplicationCreated);
        //assert first application
        pollForApplication(firstApplicationId, getMatcherForApplication(STATUS_UN_ALLOCATED));
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getHearingJsonObject("public.listing.hearing-confirmed.json",
                caseId, hearingId, defendantId, courtCentreId, randomUUID().toString(), courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        // Creating second application for the case
        String secondApplicationId = randomUUID().toString();

        final JmsMessageConsumerClient consumerForCourtApplicationCreated2 = newPublicJmsMessageConsumerClientProvider().withEventNames(COURT_APPLICATION_CREATED).getMessageConsumerClient();

        initiateCourtProceedingsForCourtApplication(secondApplicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");
        verifyInMessagingQueueForCourtApplicationCreated(secondApplicationId, consumerForCourtApplicationCreated2);
        //assert second application
        pollForApplication(secondApplicationId, getMatcherForApplication(STATUS_UN_ALLOCATED));

        //assert linked applications
        pollProsecutionCasesProgressionFor(caseId, getLinkedApplicationsMatcher(STATUS_UN_ALLOCATED));

        final JmsMessageConsumerClient consumerForCaseOrApplicationEjected = newPublicJmsMessageConsumerClientProvider().withEventNames(CASE_OR_APPLICATION_EJECTED).getMessageConsumerClient();

        // Eject case
        ejectCaseApplication(randomUUID().toString(), caseId, REMOVAL_REASON, "eject/progression.eject-case.json");

        final Matcher[] additionalMatchers = {
                withJsonPath("$.prosecutionCase.caseStatus", is(STATUS_EJECTED)),
                withJsonPath("$.prosecutionCase.removalReason", is(REMOVAL_REASON))
        };

        pollProsecutionCasesProgressionFor(caseId, getLinkedApplicationsMatcher(STATUS_EJECTED, additionalMatchers));

        // assert applications ejected
        pollForApplication(firstApplicationId, getMatcherForApplication(STATUS_EJECTED));

        pollForApplication(secondApplicationId, getMatcherForApplication(STATUS_EJECTED));
        verifyInMessagingQueueForCaseOrApplicationEjected(consumerForCaseOrApplicationEjected);

        pollForResponse("/hearingSearch/" + hearingId,
                PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.prosecutionCases[0].caseStatus", is(STATUS_EJECTED))
        );

    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private Matcher[] getMatcherForApplication(final String status) {

        List<Matcher> matchers = Lists.newArrayList(
                withJsonPath("$.courtApplication.applicationStatus", is(status))
        );

        if (STATUS_EJECTED.equals(status)) {
            matchers.add(withJsonPath("$.courtApplication.removalReason", is(REMOVAL_REASON)));
        }

        return matchers.toArray(new Matcher[0]);


    }

    private Matcher[] getLinkedApplicationsMatcher(final String status, final Matcher... extraneousMatchers) {

        List<Matcher> matchers = Lists.newArrayList(
                withJsonPath("$.linkedApplicationsSummary", hasSize(2)),
                withJsonPath("$.linkedApplicationsSummary[0].applicationStatus", is(status)),
                withJsonPath("$.linkedApplicationsSummary[1].applicationStatus", is(status))
        );

        if (STATUS_EJECTED.equals(status)) {
            matchers.add(withJsonPath("$.linkedApplicationsSummary[0].removalReason", is(REMOVAL_REASON)));
            matchers.add(withJsonPath("$.linkedApplicationsSummary[1].removalReason", is(REMOVAL_REASON)));
        }

        matchers.addAll(Arrays.asList(extraneousMatchers));

        return matchers.toArray(new Matcher[0]);
    }


    private static void verifyInMessagingQueueForCourtApplicationCreated(String applicationId, final JmsMessageConsumerClient consumerForCourtApplicationCreated) {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String idResponse = message.get().getJsonObject("courtApplication").getString("id");
        assertThat(idResponse, equalTo(applicationId));
    }

    private static void verifyInMessagingQueueForCaseOrApplicationEjected(final JmsMessageConsumerClient consumerForCaseOrApplicationEjected) {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCaseOrApplicationEjected);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForStandaloneCourtApplicationCreated(final JmsMessageConsumerClient consumerForCourtApplicationCreated) {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String referenceResponse = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(10, is(referenceResponse.length()));
    }

    public JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                           final String defendantId, final String courtCentreId, final String applicationId, final String courtCentreName) {
        final String strPayload = getPayload(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("OFFENCE_ID", "3789ab16-0bb7-4ef1-87ef-c936bf0364f1")
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    public JsonObject getHearingResultedJsonObject(final String path, final String caseId, final String hearingId,
                                           final String defendantId, final String courtCentreId, final String applicationId,
                                           final String adjournedHearingId, final String reference, final String courtCentreName) {
        final String strPayload = getPayload(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("OFFENCE_ID", "3789ab16-0bb7-4ef1-87ef-c936bf0364f1")
                .replaceAll("ADJOURNED_ID", adjournedHearingId)
                .replaceAll("APPLICATION_REF", reference)
                .replaceAll("ORDERED_DATE", "2023-01-01")
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }
}

