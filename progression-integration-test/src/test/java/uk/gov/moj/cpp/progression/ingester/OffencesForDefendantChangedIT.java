package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPrivateJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.createReferProsecutionCaseToCrownCourtJsonBody;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getStringFromResource;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.OffencesForDefendantChangedVerificationHelper.verifyInitialOffence;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.OffencesForDefendantChangedVerificationHelper.verifyUpdatedOffences;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.AbstractIT;

import java.util.Optional;
import java.util.Random;

import javax.jms.JMSException;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OffencesForDefendantChangedIT extends AbstractIT {

    private static final String REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.prosecution-case-refer-to-court-offence-update.json";
    private static final String DEFENDANT_CHANGED_EVENT = "progression.events.offences-for-defendant-changed";
    private static final String PAYLOAD_LOCATION_UPDATE = "ingestion/progression.update-offences-for-prosecution-case-update.json";
    private static final String PAYLOAD_LOCATION_ADD = "ingestion/progression.update-offences-for-prosecution-case-add.json";
    private static final String PAYLOAD_LOCATION_DELETE = "ingestion/progression.update-offences-for-prosecution-case-delete.json";

    private static final JmsMessageProducerClient messageProducer = newPrivateJmsMessageProducerClientProvider(CONTEXT_NAME).getMessageProducerClient();
    private String caseId;
    private String defendantId;
    private String initialoffenceId1;
    private String initialoffenceId2;
    private String initialToBeUpdatedOffenceId3;
    private String initialToBeDeletedOffenceId4;
    private String addedOffenceId6;
    private String caseUrn;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        caseUrn = generateUrn();

        initialoffenceId1 = "11a7c849-1cb8-4ec7-848a-ff53b0e872f0";
        initialoffenceId2 = "393b1edf-b388-415f-8c61-8e72b30bacaf";
        initialToBeUpdatedOffenceId3 = "f20d95f7-8963-4eb4-bd17-a76ba5689438";
        initialToBeDeletedOffenceId4 = "70018321-3798-4ae2-a7ef-82ec0d61dae6";
        addedOffenceId6 = "208127fd-c8d9-4cfd-83c5-9d07d492159d";

        cleanEventStoreTables();
        deleteAndCreateIndex();
    }

    @AfterAll
    public static void tearDown() throws JMSException {
        cleanEventStoreTables();
    }


    @Test
    public void shouldIndexProgressionCaseCreatedEventUpdateOffence() throws Exception {

        generateInitialEventsToIndex();
        final JsonObject defendantChangedEvent = getDefendantChangedEventUpdate(PAYLOAD_LOCATION_UPDATE, initialToBeUpdatedOffenceId3);
        sendDefendantChangedEventToMessageQueue(defendantChangedEvent);

        final Matcher[] matchers = {withJsonPath("$.caseReference")};

        final Optional<JsonObject> updatedIndexJsonObject = findBy(matchers);

        assertTrue(updatedIndexJsonObject.isPresent());

        verifyUpdatedOffences(defendantChangedEvent, updatedIndexJsonObject.get(), 0, 0, true, "updatedOffences");

    }

    @Test
    public void shouldIndexProgressionCaseCreatedEventAddOffence() throws Exception {

        generateInitialEventsToIndex();
        final JsonObject defendantChangedEvent = getDefendantChangedEventAdd(PAYLOAD_LOCATION_ADD, addedOffenceId6);

        sendDefendantChangedEventToMessageQueue(defendantChangedEvent);

        final Matcher[] matchers = {withJsonPath("$.caseReference")};
        final Optional<JsonObject> updatedIndexJsonObject = findBy(matchers);

        assertTrue(updatedIndexJsonObject.isPresent());

        verifyUpdatedOffences(defendantChangedEvent, updatedIndexJsonObject.get(), 0, 0, true, "addedOffences");

    }

    @Test
    public void shouldIndexProgressionCaseCreatedEventDeleteOffence() throws Exception {

        generateInitialEventsToIndex();
        final JsonObject defendantChangedEvent = getDefendantChangedEventDelete(PAYLOAD_LOCATION_DELETE, initialToBeDeletedOffenceId4);

        sendDefendantChangedEventToMessageQueue(defendantChangedEvent);

        final Matcher[] matchers = {withJsonPath("$.caseReference")};

        final Optional<JsonObject> updatedIndexJsonObject = findBy(matchers);

        assertTrue(updatedIndexJsonObject.isPresent());

        verifyUpdatedOffences(defendantChangedEvent, updatedIndexJsonObject.get(), 0, 0, true, "deletedOffences");

    }

    private void sendDefendantChangedEventToMessageQueue(JsonObject defendantChangedEvent) {
        final Metadata metadata = createMetadata(DEFENDANT_CHANGED_EVENT);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(metadata, defendantChangedEvent);
        messageProducer.sendMessage(DEFENDANT_CHANGED_EVENT, publicEventEnvelope);

    }

    private DocumentContext getInitialOffenceDetails(final String caseUrn, final String initialoffenceId1, final String initialoffenceId2, final String initialToBeUpdatedOffenceId3, final String initialToBeDeletedOffenceId4) {
        final String commandJson = createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION, initialoffenceId1, initialoffenceId2, initialToBeUpdatedOffenceId3, initialToBeDeletedOffenceId4);

        final JsonObject commandJsonInputJson = jsonFromString(commandJson);
        final DocumentContext prosecutionCase = parse(commandJsonInputJson);
        final JsonArray offences = prosecutionCase.read("$.courtReferral.prosecutionCases[0].defendants[0].offences");
        return parse(offences);
    }

    private JsonObject getDefendantChangedEventUpdate(final String fileName, final String updatedOffenceId) {
        final String eventPayload = getStringFromResource(fileName)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("UPDATED_OFFENCE_ID", updatedOffenceId);
        return new StringToJsonObjectConverter().convert(eventPayload);
    }

    private JsonObject getDefendantChangedEventAdd(final String fileName, final String addedOffenceId) {
        final String eventPayload = getStringFromResource(fileName)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("ADDEDOFFENCEID", addedOffenceId);
        return new StringToJsonObjectConverter().convert(eventPayload);
    }

    private JsonObject getDefendantChangedEventDelete(final String fileName, final String deletedOffenceId1) {
        final String eventPayload = getStringFromResource(fileName)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DELETEDOFFENCEID1", deletedOffenceId1);

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

    private void generateInitialEventsToIndex() throws Exception {
        addProsecutionCaseToCrownCourtForIngestion(caseId, defendantId, randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION, initialoffenceId1, initialoffenceId2, initialToBeUpdatedOffenceId3, initialToBeDeletedOffenceId4);

        final Matcher[] initialMatchers = {withJsonPath("$.parties[*].offences[*]", hasSize(4))};

        final Optional<JsonObject> prosecussionCaseResponseJsonObject = findBy(initialMatchers);

        assertTrue(prosecussionCaseResponseJsonObject.isPresent());

        final JsonArray outputOffences = prosecussionCaseResponseJsonObject.get().getJsonArray("parties").getJsonObject(0).getJsonArray("offences");

        final DocumentContext initialOffenceDetails = getInitialOffenceDetails(caseUrn, initialoffenceId1, initialoffenceId2, initialToBeUpdatedOffenceId3, initialToBeDeletedOffenceId4);

        verifyInitialOffence(initialOffenceDetails, outputOffences);
    }
}
