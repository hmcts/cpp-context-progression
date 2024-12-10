package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPrivateJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getStringFromResource;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseDefendantListingStatusChangedEventHelper.assertCase;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseDefendantListingStatusChangedEventHelper.assertHearing;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseDefendantListingStatusChangedEventHelper.assertJudiciaryTypes;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.ingester.verificationHelpers.HearingVerificationHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class ProsecutionCaseDefendantListingStatusChangedEventIT extends AbstractIT {

    private final static String DEFENDANT_LISTING_STATUS_CHANGED_V2_EVENT = "progression.event.prosecutionCase-defendant-listing-status-changed-v2";
    private static final String EVENT_LOCATION_WITHOUT_COURT_CENTRE_IN_HEARING_DAYS = "ingestion/progression.event.prosecution-case-defendant-listing-status-changed-without-court-centre-in-hearing-days.json";
    private static final String EVENT_LOCATION = "ingestion/progression.event.prosecution-case-defendant-listing-status-changed.json";
    private static final String EVENT_WITH_LINKED_APPLICATION_LOCATION = "ingestion/progression.event.prosecution-case-defendant-listing-status-changed-with-linked-applications.json";

    private static final JmsMessageConsumerClient messageConsumerV2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(DEFENDANT_LISTING_STATUS_CHANGED_V2_EVENT).getMessageConsumerClient();
    private static final JmsMessageProducerClient messageProducer = newPrivateJmsMessageProducerClientProvider(CONTEXT_NAME).getMessageProducerClient();
    private static final String COURT_APPLICATIONS = "courtApplications";
    private static final String APPLICATIONS = "applications";
    public static final String APPLICATION_REFERENCE = "applicationReference";

    private String firstCaseId;
    private String secondCaseId;
    private String thirdCaseId;

    private final HearingVerificationHelper verificationHelper = new HearingVerificationHelper();

    private String courtId;

    @BeforeEach
    public void setup() {
        firstCaseId = randomUUID().toString();
        secondCaseId = randomUUID().toString();
        thirdCaseId = randomUUID().toString();
        courtId = randomUUID().toString();
        deleteAndCreateIndex();
    }

    @AfterAll
    public static void tearDown() {
        cleanEventStoreTables();
    }


    @Test
    public void shouldIngestProsecutionCaseDefendantListingStatusChangedV2Event() {
        final Metadata metadata = createMetadata(DEFENDANT_LISTING_STATUS_CHANGED_V2_EVENT);

        final JsonObject prosecutionCaseDefendantListingStatusChangedEvent = getProsecutionCaseDefendantListingStatusChangedPayload(EVENT_LOCATION);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCaseDefendantListingStatusChangedEvent);
        messageProducer.sendMessage(DEFENDANT_LISTING_STATUS_CHANGED_V2_EVENT, publicEventEnvelope);

        verifyInMessagingQueueV2();

        final Optional<JsonObject> prosecutionCaseResponseJsonObject = getPoller().pollUntilFound(() -> {

            try {
                final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 3) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                fail();
            }
            return empty();
        });

        assertTrue(prosecutionCaseResponseJsonObject.isPresent());

        final int indexSize = prosecutionCaseResponseJsonObject.get().getJsonArray("index").size();

        for (int i = 0; i < indexSize; i++) {
            final JsonObject outputCase = getCaseAt(prosecutionCaseResponseJsonObject, i);
            final JsonObject outputHearing = outputCase.getJsonArray("hearings").getJsonObject(0);
            final JsonObject inputHearing = prosecutionCaseDefendantListingStatusChangedEvent.getJsonObject("hearing");

            assertCase(outputCase, asList(firstCaseId, secondCaseId, thirdCaseId));
            assertHearing(outputHearing, inputHearing, prosecutionCaseDefendantListingStatusChangedEvent, false);

            final JsonArray outputJudiciaryTypesArray = outputHearing.getJsonArray("judiciaryTypes");
            final JsonArray inputJudiciaryTypesArray = inputHearing.getJsonArray("judiciary");

            assertJudiciaryTypes(outputJudiciaryTypesArray, inputJudiciaryTypesArray);

            if (outputCase.containsKey(APPLICATIONS)) {
                final JsonArray outputApplications = outputCase.getJsonArray(APPLICATIONS);
                final JsonArray inputCourtApplications = inputHearing.getJsonArray(COURT_APPLICATIONS);
                assertCourtApplications(outputApplications, inputCourtApplications);
            }
        }


        final Map<String, JsonObject> caseAndApplicationsOutputMap = new HashMap<>();
        final Map<String, JsonObject> applicationsInputMap = new HashMap<>();
        final Map<String, JsonObject> casesInputMap = new HashMap<>();

        final JsonObject hearing = prosecutionCaseDefendantListingStatusChangedEvent.getJsonObject("hearing");
        final JsonArray courtApplications = hearing.getJsonArray("courtApplications");
        final JsonArray cases = hearing.getJsonArray("prosecutionCases");

        for (int i = 0; i < indexSize; i++) {
            final JsonObject outputCase = getCaseAt(prosecutionCaseResponseJsonObject, i);
            caseAndApplicationsOutputMap.putIfAbsent(outputCase.getString("caseId"), outputCase);
        }

        for (int i = 0; i < courtApplications.size(); i++) {
            final JsonObject application = (JsonObject) hearing.getJsonArray("courtApplications").get(i);
            applicationsInputMap.putIfAbsent(application.getString("id"), application);
        }

        for (int i = 0; i < cases.size(); i++) {
            final JsonObject cases1 = (JsonObject) hearing.getJsonArray("prosecutionCases").get(i);
            casesInputMap.putIfAbsent(cases1.getString("id"), cases1);
        }

        for (int i = 0; i < courtApplications.size(); i++) {
            final JsonObject inputApplication = (JsonObject) hearing.getJsonArray("courtApplications").get(i);
            final JsonObject outputApplication = caseAndApplicationsOutputMap.get(inputApplication.getString("id"));
            verificationHelper.verifyApplication(parse(hearing), outputApplication, "$.courtApplications[" + i + "]");
            verificationHelper.verifyHearings(parse(prosecutionCaseDefendantListingStatusChangedEvent), outputApplication, 0);
        }

        for (int i = 0; i < cases.size(); i++) {
            final JsonObject inputCase = (JsonObject) hearing.getJsonArray("prosecutionCases").get(i);
            final JsonObject outputCase = caseAndApplicationsOutputMap.get(inputCase.getString("id"));
            verificationHelper.verifyProsecutionCase(parse(hearing), outputCase, "$.prosecutionCases[" + i + "]");
            verificationHelper.verifyHearings(parse(prosecutionCaseDefendantListingStatusChangedEvent), outputCase, 0);
        }
        verificationHelper.verifyCounts(3, 3, 0);
    }

    @Test
    public void shouldIngestProsecutionCaseDefendantListingStatusChangedV2EventWithLinkedApplications() {
        firstCaseId = "a1fc4f5e-3a2e-489e-9a65-9edb64fd335a";
        //Would be great to create a case first
        final Metadata metadata = createMetadata(DEFENDANT_LISTING_STATUS_CHANGED_V2_EVENT);

        final JsonObject prosecutionCaseDefendantListingStatusChangedEvent = getProsecutionCaseDefendantListingStatusChangedPayload(EVENT_WITH_LINKED_APPLICATION_LOCATION);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCaseDefendantListingStatusChangedEvent);
        messageProducer.sendMessage(DEFENDANT_LISTING_STATUS_CHANGED_V2_EVENT, publicEventEnvelope);

        verifyInMessagingQueueV2();

        final Matcher[] caseMatcher = {withJsonPath("$.caseId", equalTo(firstCaseId))};

        final Optional<JsonObject> prosecussionCaseResponseJsonObject = findBy(caseMatcher);

        assertThat(prosecussionCaseResponseJsonObject.isPresent(), is(true));

        final JsonObject outputCase = prosecussionCaseResponseJsonObject.get();

        final JsonObject outputHearing = outputCase.getJsonArray("hearings").getJsonObject(0);
        final JsonObject inputHearing = prosecutionCaseDefendantListingStatusChangedEvent.getJsonObject("hearing");

        assertCase(outputCase, asList(firstCaseId));
        assertHearing(outputHearing, inputHearing, prosecutionCaseDefendantListingStatusChangedEvent, false);

        final JsonArray outputJudiciaryTypesArray = outputHearing.getJsonArray("judiciaryTypes");
        final JsonArray inputJudiciaryTypesArray = inputHearing.getJsonArray("judiciary");

        assertJudiciaryTypes(outputJudiciaryTypesArray, inputJudiciaryTypesArray);

        if (outputCase.containsKey(APPLICATIONS)) {
            final JsonArray outputApplications = outputCase.getJsonArray(APPLICATIONS);
            final JsonArray inputCourtApplications = inputHearing.getJsonArray(COURT_APPLICATIONS);
            assertCourtApplications(outputApplications, inputCourtApplications);
        }

        final Map<String, JsonObject> caseAndApplicationsOutputMap = new HashMap<>();
        final Map<String, JsonObject> applicationsInputMap = new HashMap<>();
        final Map<String, JsonObject> casesInputMap = new HashMap<>();

        final JsonObject hearing = prosecutionCaseDefendantListingStatusChangedEvent.getJsonObject("hearing");
        final JsonArray courtApplications = hearing.getJsonArray("courtApplications");
        final JsonArray cases = hearing.getJsonArray("prosecutionCases");


        caseAndApplicationsOutputMap.putIfAbsent(outputCase.getString("caseId"), outputCase);

        for (int i = 0; i < courtApplications.size(); i++) {
            final JsonObject application = (JsonObject) hearing.getJsonArray("courtApplications").get(i);
            applicationsInputMap.putIfAbsent(application.getString("id"), application);
        }

        for (int i = 0; i < cases.size(); i++) {
            final JsonObject cases1 = (JsonObject) hearing.getJsonArray("prosecutionCases").get(i);
            casesInputMap.putIfAbsent(cases1.getString("id"), cases1);
        }

        for (int i = 0; i < courtApplications.size(); i++) {
            verificationHelper.verifyApplication(parse(hearing), outputCase, "$.courtApplications[" + i + "]");
        }

        for (int i = 0; i < cases.size(); i++) {
            verificationHelper.verifyProsecutionCase(parse(hearing), outputCase, "$.prosecutionCases[" + i + "]");
            verificationHelper.verifyHearings(parse(prosecutionCaseDefendantListingStatusChangedEvent), outputCase, 0);
        }
        verificationHelper.verifyCounts(2, 1, 0);
    }

    @Test
    public void shouldIngestProsecutionCaseDefendantListingStatusChangedV2EventWithoutCourtCentreInHearingDays() {
        final Metadata metadata = createMetadata(DEFENDANT_LISTING_STATUS_CHANGED_V2_EVENT);

        final JsonObject prosecutionCaseDefendantListingStatusChangedEvent = getProsecutionCaseDefendantListingStatusChangedPayload(EVENT_LOCATION_WITHOUT_COURT_CENTRE_IN_HEARING_DAYS);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCaseDefendantListingStatusChangedEvent);
        messageProducer.sendMessage(DEFENDANT_LISTING_STATUS_CHANGED_V2_EVENT, publicEventEnvelope);

        verifyInMessagingQueueV2();

        final Optional<JsonObject> prosecutionCaseResponseJsonObject = getPoller().pollUntilFound(() -> {

            try {
                final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 3) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                fail();
            }
            return empty();
        });

        assertTrue(prosecutionCaseResponseJsonObject.isPresent());

        final int indexSize = prosecutionCaseResponseJsonObject.get().getJsonArray("index").size();

        for (int i = 0; i < indexSize; i++) {
            final JsonObject outputCase = getCaseAt(prosecutionCaseResponseJsonObject, i);
            final JsonObject outputHearing = outputCase.getJsonArray("hearings").getJsonObject(0);
            final JsonObject inputHearing = prosecutionCaseDefendantListingStatusChangedEvent.getJsonObject("hearing");

            assertCase(outputCase, asList(firstCaseId, secondCaseId, thirdCaseId));
            assertHearing(outputHearing, inputHearing, prosecutionCaseDefendantListingStatusChangedEvent, true);

            final JsonArray outputJudiciaryTypesArray = outputHearing.getJsonArray("judiciaryTypes");
            final JsonArray inputJudiciaryTypesArray = inputHearing.getJsonArray("judiciary");

            assertJudiciaryTypes(outputJudiciaryTypesArray, inputJudiciaryTypesArray);

            if (outputCase.containsKey(APPLICATIONS)) {
                final JsonArray outputApplications = outputCase.getJsonArray(APPLICATIONS);
                final JsonArray inputCourtApplications = inputHearing.getJsonArray(COURT_APPLICATIONS);
                assertCourtApplications(outputApplications, inputCourtApplications);
            }
        }


        final Map<String, JsonObject> caseAndApplicationsOutputMap = new HashMap<>();
        final Map<String, JsonObject> applicationsInputMap = new HashMap<>();
        final Map<String, JsonObject> casesInputMap = new HashMap<>();

        final JsonObject hearing = prosecutionCaseDefendantListingStatusChangedEvent.getJsonObject("hearing");
        final JsonArray courtApplications = hearing.getJsonArray("courtApplications");
        final JsonArray cases = hearing.getJsonArray("prosecutionCases");

        for (int i = 0; i < indexSize; i++) {
            final JsonObject outputCase = getCaseAt(prosecutionCaseResponseJsonObject, i);
            caseAndApplicationsOutputMap.putIfAbsent(outputCase.getString("caseId"), outputCase);
        }

        for (int i = 0; i < courtApplications.size(); i++) {
            final JsonObject application = (JsonObject) hearing.getJsonArray("courtApplications").get(i);
            applicationsInputMap.putIfAbsent(application.getString("id"), application);
        }

        for (int i = 0; i < cases.size(); i++) {
            final JsonObject cases1 = (JsonObject) hearing.getJsonArray("prosecutionCases").get(i);
            casesInputMap.putIfAbsent(cases1.getString("id"), cases1);
        }

        for (int i = 0; i < courtApplications.size(); i++) {
            final JsonObject inputApplication = (JsonObject) hearing.getJsonArray("courtApplications").get(i);
            final JsonObject outputApplication = caseAndApplicationsOutputMap.get(inputApplication.getString("id"));
            verificationHelper.verifyApplication(parse(hearing), outputApplication, "$.courtApplications[" + i + "]");
            verificationHelper.verifyHearingsWithoutCourtCentre(parse(prosecutionCaseDefendantListingStatusChangedEvent), outputApplication, 0);
        }

        for (int i = 0; i < cases.size(); i++) {
            final JsonObject inputCase = (JsonObject) hearing.getJsonArray("prosecutionCases").get(i);
            final JsonObject outputCase = caseAndApplicationsOutputMap.get(inputCase.getString("id"));
            verificationHelper.verifyProsecutionCase(parse(hearing), outputCase, "$.prosecutionCases[" + i + "]");
            verificationHelper.verifyHearingsWithoutCourtCentre(parse(prosecutionCaseDefendantListingStatusChangedEvent), outputCase, 0);
        }
        verificationHelper.verifyCounts(3, 3, 0);
    }

    private JsonObject getCaseAt(final Optional<JsonObject> prosecutionCaseResponseJsonObject, final int i) {
        return jsonFromString(getJsonArray(prosecutionCaseResponseJsonObject.get(), "index").get().getString(i));
    }

    private void assertCourtApplications(final JsonArray outputApplications, final JsonArray inputCourtApplications) {
        for (final JsonValue outputApplication : outputApplications) {
            final JsonObject outputApplicationJsonObject = (JsonObject) outputApplication;
            final String applicationId = outputApplicationJsonObject.getString("applicationId");
            final Optional<JsonObject> inputCourtApplication = inputCourtApplications.stream().map(p -> (JsonObject) p).filter(p -> p.getString("id").equals(applicationId)).findFirst();
            if (!inputCourtApplication.isPresent()) {
                fail("Could not find input application for application " + applicationId);
            }

            assertApplication(outputApplicationJsonObject, inputCourtApplication.get());
        }
    }

    private void assertApplication(final JsonObject outputApplicationJsonObject, final JsonObject inputApplicationJsonObject) {
        assertThat(outputApplicationJsonObject.getString("applicationId"), is(inputApplicationJsonObject.getString("id")));
        assertThat(outputApplicationJsonObject.getString(APPLICATION_REFERENCE), is(inputApplicationJsonObject.getString(APPLICATION_REFERENCE)));
        assertThat(outputApplicationJsonObject.getString("applicationType"), is(inputApplicationJsonObject.getJsonObject("type").getString("type")));
        assertThat(outputApplicationJsonObject.getString("decisionDate"), is(inputApplicationJsonObject.getString("applicationDecisionSoughtByDate")));
        assertThat(outputApplicationJsonObject.getString("receivedDate"), is(inputApplicationJsonObject.getString("applicationReceivedDate")));
    }

    private JsonObject getProsecutionCaseDefendantListingStatusChangedPayload(final String fileName) {
        final String eventPayload = getStringFromResource(fileName)
                .replaceAll("CASE_ID_1", firstCaseId)
                .replaceAll("CASE_ID_2", secondCaseId)
                .replaceAll("CASE_ID_3", thirdCaseId)

                .replaceAll("COURT_ID", courtId);

        return new StringToJsonObjectConverter().convert(eventPayload);
    }


    private Metadata createMetadata(final String eventName) {
        return metadataBuilder()
                .withId(randomUUID())
                .withStreamId(randomUUID())
                .withPosition(1)
                .withPreviousEventNumber(123)
                .withEventNumber(new Random().nextLong())
                .withSource("event-indexer-test")
                .withName(eventName)
                .withUserId(randomUUID().toString())
                .build();
    }

    private static void verifyInMessagingQueueV2() {
        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerV2);
        assertNotNull(message);
    }

}
