package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getReferProsecutionCaseToCrownCourtJsonBody;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getStringFromResource;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.OffencesForDefendantChangedVerificationHelper.verifyInitialOffence;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.OffencesForDefendantChangedVerificationHelper.verifyUpdatedOffence;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;

import java.io.IOException;
import java.util.Optional;
import java.util.Random;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class OffencesForDefendantChangedIT {

    private static final String REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.prosecution-case-refer-to-court.json";
    private static final String DEFENDANT_CHANGED_EVENT = "progression.events.offences-for-defendant-changed";
    private static final String EVENT_LOCATION = "ingestion/progression.update-offences-for-prosecution-case.json";


    private static final MessageConsumer messageConsumer = privateEvents.createConsumer(DEFENDANT_CHANGED_EVENT);
    private static final MessageProducer messageProducer = privateEvents.createProducer();

    private ElasticSearchIndexRemoverUtil elasticSearchIndexRemoverUtil;

    private String caseId;
    private String defendantId;
    private String offenceId;
    private String caseUrn;

    @Before
    public void setUp() throws IOException {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        caseUrn = PreAndPostConditionHelper.generateUrn();

        offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";

        elasticSearchIndexRemoverUtil = new ElasticSearchIndexRemoverUtil();
        elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();

        cleanViewStoreTables();
        cleanEventStoreTables();

    }

    @AfterClass
    public static void tearDown() throws JMSException {
        cleanEventStoreTables();
        cleanViewStoreTables();

        messageConsumer.close();
        messageProducer.close();
    }


    @Test
    public void shouldIndexProgressionCaseCreatedEvent() throws Exception {

        addProsecutionCaseToCrownCourtForIngestion(caseId, defendantId, randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);

        final Matcher[] initialMatchers = {withJsonPath("$.parties[*].offences[*]", hasSize(1))};

        final Optional<JsonObject> prosecussionCaseResponseJsonObject = findBy(initialMatchers);

        assertTrue(prosecussionCaseResponseJsonObject.isPresent());

        final JsonObject outputOffences = prosecussionCaseResponseJsonObject.get().getJsonArray("parties").getJsonObject(0).getJsonArray("offences").getJsonObject(0);

        final DocumentContext initialOffenceDetails = getInitialOffenceDetails(caseUrn);

        verifyInitialOffence(initialOffenceDetails, outputOffences);

        sendDefendantChangedEventToMessageQueue();

        final Matcher[] matchers = {withJsonPath("$.parties[*].offences[*].startDate", hasItem(equalTo("2020-01-01")))};

        final Optional<JsonObject> updatedOffenceResponseJsonObject = findBy(matchers);

        assertTrue(updatedOffenceResponseJsonObject.isPresent());

        final JsonObject defendantChangedEvent = getDefendantChangedEvent(EVENT_LOCATION)
                .getJsonArray("updatedOffences")
                .getJsonObject(0)
                .getJsonArray("offences")
                .getJsonObject(0);

        final JsonObject responseJsonObject = updatedOffenceResponseJsonObject.get().getJsonArray("parties").getJsonObject(0).getJsonArray("offences").getJsonObject(0);

        verifyUpdatedOffence(defendantChangedEvent, responseJsonObject);
    }

    private void sendDefendantChangedEventToMessageQueue() {
        final Metadata metadata = createMetadata(DEFENDANT_CHANGED_EVENT);

        final JsonObject defendantChangedEvent = getDefendantChangedEvent(EVENT_LOCATION);

        sendMessage(messageProducer, DEFENDANT_CHANGED_EVENT, defendantChangedEvent, metadata);

        verifyInMessagingQueue();
    }

    private DocumentContext getInitialOffenceDetails(final String caseUrn) throws IOException {
        final String commandJson = getReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);

        final JsonObject commandJsonInputJson = jsonFromString(commandJson);
        final DocumentContext prosecutionCase = parse(commandJsonInputJson);
        final JsonObject offences = prosecutionCase.read("$.courtReferral.prosecutionCases[0].defendants[0].offences[0]");
        return parse(offences);
    }

    private JsonObject getDefendantChangedEvent(final String fileName) {
        final String eventPayload = getStringFromResource(fileName)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("OFFENCE_ID", offenceId);

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
