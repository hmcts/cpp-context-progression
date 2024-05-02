package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.core.Is;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.constant.ApplicationExternalCreatorType;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class HearingResultedApplicationUpdatedIT extends AbstractIT {

    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED_TO_NEXT_HEARING_V2 = "ingestion/public.hearing.resulted.application-adjourned-to-next-hearing-with-application-case-V2.json";
    private static final String PUBLIC_HEARING_RESULTED_WITH_DRIVER_NUMBER = "public.hearing.resulted.application-with-driver-number.json";
    private static final String EVENT_CASE_DEFENDANT_UPDATED = "progression.event.prosecution-case-defendant-updated";
    private static final String EVENT_CASE_DEFENDANT_DRIVER_NUMBER_UPDATED = "progression.event.case-defendant-updated-with-driver-number";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();
    private static final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged =
            privateEvents.createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2");
    private MessageConsumer privateEventsConsumerCaseDefendantUpdated;
    private MessageConsumer privateEventsConsumerDriverNumberUpdated;


    private String applicationId;
    private String caseId;
    private String defendantId;
    private String hearingId;
    private String userId;
    private String courtCentreId;
    private String courtCentreName;
    private String newCourtCentreId;
    private String newCourtCentreName;

    @Before
    public void setup() throws IOException {
        caseId = randomUUID().toString();
        applicationId = randomUUID().toString();
        defendantId = randomUUID().toString();
        userId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        newCourtCentreName = "Narnia Magistrate's Court";
        cleanViewStoreTables();
        deleteAndCreateIndex();
    }

    @AfterClass
    public static void tearDown() {
        cleanEventStoreTables();
        cleanViewStoreTables();
    }

    @Test
    public void shouldIngestHearingResultedApplicationUpdatedEvent() throws Exception {

        //GIVEN - WHEN
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        final String prosecutionAuthorityReference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, applicationId, randomUUID().toString(), prosecutionAuthorityReference, courtCentreId, courtCentreName);
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

        final String courtApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, hearingId, "ingestion/progression.initiate-court-proceedings-for-generic-linked-application.json");
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String adjournedHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED_V2, getHearingJsonObject(PUBLIC_HEARING_RESULTED_TO_NEXT_HEARING_V2, caseId,
                        hearingId, defendantId, courtApplicationId, adjournedHearingId, prosecutionAuthorityReference, newCourtCentreId, newCourtCentreName, "2021-05-26"), metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED_V2, getHearingJsonObject(PUBLIC_HEARING_RESULTED_TO_NEXT_HEARING_V2, caseId,
                        hearingId, defendantId, courtApplicationId, adjournedHearingId, prosecutionAuthorityReference, newCourtCentreId, newCourtCentreName, "2021-05-25"), metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());
        TimeUnit.MILLISECONDS.sleep(4000);

        // THEN
        final Matcher[] caseMatcher = {
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.applications[*].applicationStatus", hasItem(ApplicationStatus.FINALISED.toString())),
        };
        final Optional<JsonObject> courtApplicationResponseJsonObject = findBy(caseMatcher);
        assertTrue(courtApplicationResponseJsonObject.isPresent());

        final String outApplicationStatus = courtApplicationResponseJsonObject.get().getJsonArray("applications").getJsonObject(0).getJsonString("applicationStatus").getString();
        final String outapplicationExternalCreatorType = courtApplicationResponseJsonObject.get().getJsonArray("applications").getJsonObject(0).getJsonString("applicationExternalCreatorType").getString();
        final String outputCaseId = courtApplicationResponseJsonObject.get().getJsonString("caseId").getString().toString();

        assertEquals(ApplicationStatus.FINALISED.toString(), outApplicationStatus);
        assertEquals(ApplicationExternalCreatorType.PROSECUTOR.name(), outapplicationExternalCreatorType);
        assertEquals(caseId, outputCaseId);
    }

    @Test
    public void shouldUpdateDriveNumberForApplication() throws Exception {

        //GIVEN - WHEN
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        final String prosecutionAuthorityReference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, applicationId, randomUUID().toString(), prosecutionAuthorityReference, courtCentreId, courtCentreName);
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

        final String courtApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, hearingId, "ingestion/progression.initiate-court-proceedings-for-generic-linked-application.json");
        final String adjournedHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        final JsonObject hearingResultedEventJsonObject = getHearingJsonObject(PUBLIC_HEARING_RESULTED_WITH_DRIVER_NUMBER, caseId,
                hearingId, defendantId, courtApplicationId, adjournedHearingId, prosecutionAuthorityReference, newCourtCentreId, newCourtCentreName, "2021-05-26");
        final String driverNumber = hearingResultedEventJsonObject.getJsonObject("hearing").getJsonArray("courtApplications")
                .getJsonObject(0).getJsonObject("applicant").getJsonObject("masterDefendant").getJsonObject("personDefendant").getString("driverNumber");
        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED_V2, hearingResultedEventJsonObject, metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());


        // THEN
        verifyDriverNumberUpdatedEvents(caseId, defendantId, driverNumber);

        poll(requestParams(getReadUrl("/prosecutioncases/" + caseId), "application/vnd.progression.query.prosecutioncase+json")
                .withHeader(USER_ID, userId).build())
                .timeout(30, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(
                                allOf(
                                        withJsonPath("$.prosecutionCase.id", is(caseId)),
                                        withJsonPath("$.prosecutionCase.defendants[0].personDefendant.driverNumber", is("DVL12345"))
                                )
                        ));

    }

    public void verifyDriverNumberUpdatedEvents(final String caseId, final String defendantId, final String driverNumber) {
        privateEventsConsumerCaseDefendantUpdated = privateEvents.createPrivateConsumer(EVENT_CASE_DEFENDANT_UPDATED);
        privateEventsConsumerDriverNumberUpdated = privateEvents.createPrivateConsumer(EVENT_CASE_DEFENDANT_DRIVER_NUMBER_UPDATED);

        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumerCaseDefendantUpdated);
        assertThat(jsonResponse.getString("defendant.prosecutionCaseId"), Is.is(caseId));
        assertThat(jsonResponse.getString("defendant.id"), Is.is(defendantId));
        assertThat(jsonResponse.getString("defendant.personDefendant.driverNumber"), Is.is(driverNumber));


        final JsonPath driverNoJsonResponse = retrieveMessage(privateEventsConsumerDriverNumberUpdated);
        assertThat(driverNoJsonResponse.getString("prosecutionCaseId"), Is.is(caseId));
        assertThat(driverNoJsonResponse.getString("defendantId"), Is.is(defendantId));
        assertThat(driverNoJsonResponse.getString("driverNumber"), Is.is(driverNumber));
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String applicationId,
                                            final String adjournedHearingId, final String reference,
                                            final String courtCentreId, final String courtCentreName) {
        return getHearingJsonObject(path, caseId, hearingId, defendantId, applicationId, adjournedHearingId, reference, courtCentreId, courtCentreName, "0000-01-01");
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
}
