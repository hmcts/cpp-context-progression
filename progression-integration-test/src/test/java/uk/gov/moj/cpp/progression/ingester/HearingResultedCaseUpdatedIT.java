package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.createReferProsecutionCaseToCrownCourtJsonBody;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.HearingResultedCaseUpdatedVerificationHelper.verifyInitialElasticSearchCase;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.HearingResultedCaseUpdatedVerificationHelper.verifyUpdatedElasticSearchCase;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getStringFromResource;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;

import java.io.IOException;
import java.util.Optional;
import java.util.Random;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
public class HearingResultedCaseUpdatedIT extends AbstractIT {

    private static final String REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.prosecution-case-refer-to-court.json";
    private static final String HEARING_RESULTED_EVENT = "progression.event.hearing-resulted-case-updated";
    private static final String EVENT_LOCATION = "ingestion/progression.event.hearing-resulted-case-updated.json";

    private static final MessageConsumer messageConsumer = privateEvents.createPrivateConsumer(HEARING_RESULTED_EVENT);
    private static final MessageProducer messageProducer = privateEvents.createPrivateProducer();

    private ElasticSearchIndexRemoverUtil elasticSearchIndexRemoverUtil;

    private String caseId;
    private String defendantId;
    private String caseUrn;

    @Before
    public void setUp() throws IOException {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        caseUrn = generateUrn();

        elasticSearchIndexRemoverUtil = new ElasticSearchIndexRemoverUtil();
        elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();

        cleanEventStoreTables();
        cleanViewStoreTables();
        deleteAndCreateIndex();
    }

    @After
    public void tearDown() throws JMSException {
        cleanEventStoreTables();
        cleanViewStoreTables();

        messageConsumer.close();
        messageProducer.close();
    }


    @Test
    public void shouldIndexHearingResultedCaseUpdatedEvent() throws Exception {

        addProsecutionCaseToCrownCourtForIngestion(caseId, defendantId, randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);

        final Matcher[] initialMatchers = {withJsonPath("$.caseStatus", equalTo("ACTIVE"))};

        final Optional<JsonObject> initialElasticSearchCaseResponseJsonObject = findBy(initialMatchers);

        assertTrue(initialElasticSearchCaseResponseJsonObject.isPresent());

        final DocumentContext inputProsecutionCase = initialCase();

        verifyInitialElasticSearchCase(inputProsecutionCase, initialElasticSearchCaseResponseJsonObject.get(), "ACTIVE");

        sendEventToMessageQueue();

        final Matcher[] postMatchers = {withJsonPath("$.caseStatus", equalTo("INACTIVE"))};
        final Optional<JsonObject> updatedElasticSearchCaseResponseJsonObject = findBy(postMatchers);

        assertTrue(updatedElasticSearchCaseResponseJsonObject.isPresent());

        final JsonObject caseUpdatedEvent = hearingResultedCaseUpdatedResultEvent(EVENT_LOCATION);

        verifyUpdatedElasticSearchCase(caseUpdatedEvent, updatedElasticSearchCaseResponseJsonObject.get());
    }


    private void sendEventToMessageQueue() {
        final Metadata metadata = createMetadata(HEARING_RESULTED_EVENT);

        final JsonObject hearingResultedCaseUpdatedResult = hearingResultedCaseUpdatedResultEvent(EVENT_LOCATION);

        sendMessage(messageProducer, HEARING_RESULTED_EVENT, hearingResultedCaseUpdatedResult, metadata);

        verifyInMessagingQueue();
        verifyMessageReceivedInViewStore(INACTIVE.getDescription());

    }

    private Matcher[] getDefendantUpdatedMatchers() {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.caseStatus", equalTo(INACTIVE.getDescription())),
                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", equalTo(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", equalTo(true))

        };
    }

    private void verifyMessageReceivedInViewStore(final String caseStatus) {
        final Matcher[] matcher = {
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.caseStatus", equalTo(caseStatus))
        };

        final JsonObject prosecutionCase = getJsonObject(pollProsecutionCasesProgressionFor(caseId, matcher)).getJsonObject("prosecutionCase");
        assertThat(prosecutionCase.getString("caseStatus"), equalTo(caseStatus));
    }

    private JsonObject hearingResultedCaseUpdatedResultEvent(final String fileName) {
        final String eventPayload = getStringFromResource(fileName)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("CASE_ID", caseId);

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

    private DocumentContext initialCase() throws IOException {
        final String commandJson = createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);
        final JsonObject commandJsonInputJson = jsonFromString(commandJson);
        final DocumentContext prosecutionCase = parse(commandJsonInputJson);
        final JsonObject prosecutionCaseJO = prosecutionCase.read("$.courtReferral.prosecutionCases[0]");
        final JsonObject prosecutionCaseEvent = Json.createObjectBuilder().add("prosecutionCase", prosecutionCaseJO).build();
        return parse(prosecutionCaseEvent);
    }
}
