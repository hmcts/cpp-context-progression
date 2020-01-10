package uk.gov.moj.cpp.progression.ingester;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getStringFromResource;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseDefendantListingStatusChangedEventHelper.assertCase;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseDefendantListingStatusChangedEventHelper.assertHearing;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseDefendantListingStatusChangedEventHelper.assertJudiciaryTypes;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.AbstractIT;

import java.io.IOException;
import java.util.Optional;
import java.util.Random;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class ProsecutionCaseDefendantListingStatusChangedEventIT extends AbstractIT {

    private final static String DEFENDANT_LISTING_STATUS_CHANGED_EVENT = "progression.event.prosecutionCase-defendant-listing-status-changed";
    private static final String EVENT_LOCATION = "ingestion/progression.event.prosecution-case-defendant-listing-status-changed.json";

    private static final MessageConsumer messageConsumer = privateEvents.createConsumer(DEFENDANT_LISTING_STATUS_CHANGED_EVENT);
    private static final MessageProducer messageProducer = privateEvents.createProducer();
    private static final String COURT_APPLICATIONS = "courtApplications";
    private static final String APPLICATIONS = "applications";
    public static final String APPLICATIN_REFERENCE = "applicationReference";
    public static final String DUE_DATE = "dueDate";

    private String firstCaseId;
    private String secondCaseId;
    private String thirdCaseId;


    private String courtId;

    @Before
    public void setup() throws IOException {
        firstCaseId = randomUUID().toString();
        secondCaseId = randomUUID().toString();
        thirdCaseId = randomUUID().toString();
        courtId = randomUUID().toString();
        cleanViewStoreTables();
        deleteAndCreateIndex();
    }

    @AfterClass
    public static void tearDown() {
        cleanEventStoreTables();
        cleanViewStoreTables();
    }

    @Test
    public void shouldIngestProsecutionCaseDefendantListingStatusChangedEvent() throws IOException {
        final Metadata metadata = createMetadata(DEFENDANT_LISTING_STATUS_CHANGED_EVENT);

        final JsonObject prosecutionCaseDefendantListingStatusChangedEvent = getProsecutionCaseDefendantListingStatusChangedPayload(EVENT_LOCATION);

        sendMessage(messageProducer, DEFENDANT_LISTING_STATUS_CHANGED_EVENT, prosecutionCaseDefendantListingStatusChangedEvent, metadata);

        verifyInMessagingQueue();

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
            final JsonObject outputCase = jsonFromString(getJsonArray(prosecutionCaseResponseJsonObject.get(), "index").get().getString(i));
            final JsonObject outputHearing = outputCase.getJsonArray("hearings").getJsonObject(0);
            final JsonObject inputHearing = prosecutionCaseDefendantListingStatusChangedEvent.getJsonObject("hearing");

            assertCase(outputCase, asList(firstCaseId, secondCaseId, thirdCaseId));
            assertHearing(outputHearing, inputHearing, prosecutionCaseDefendantListingStatusChangedEvent);

            final JsonArray outputJudiciaryTypesArray = outputHearing.getJsonArray("judiciaryTypes");
            final JsonArray inputJudiciaryTypesArray = inputHearing.getJsonArray("judiciary");

            assertJudiciaryTypes(outputJudiciaryTypesArray, inputJudiciaryTypesArray);

            if (outputCase.containsKey(APPLICATIONS)) {
                final JsonArray outputApplications = outputCase.getJsonArray(APPLICATIONS);
                final JsonArray inputCourtApplications = inputHearing.getJsonArray(COURT_APPLICATIONS);
                assertCourtApplications(outputApplications, inputCourtApplications);
            }
        }
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
        assertThat(outputApplicationJsonObject.getString(APPLICATIN_REFERENCE), is(inputApplicationJsonObject.getString(APPLICATIN_REFERENCE)));
        assertThat(outputApplicationJsonObject.getString("applicationType"), is(inputApplicationJsonObject.getJsonObject("type").getString("applicationType")));
        assertThat(outputApplicationJsonObject.getString(DUE_DATE), is(inputApplicationJsonObject.getString(DUE_DATE)));
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


    private static void verifyInMessagingQueue() {
        final JsonPath message = retrieveMessage(messageConsumer);
        assertTrue(message != null);
    }

}
