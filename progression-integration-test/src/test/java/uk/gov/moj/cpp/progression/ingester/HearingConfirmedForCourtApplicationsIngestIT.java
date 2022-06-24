package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.ACTIVE;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.createReferProsecutionCaseToCrownCourtJsonBody;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsString;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.HearingResultedCaseUpdatedVerificationHelper.verifyInitialElasticSearchCase;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseCreated;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryExactMatchWithEmptyResults;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryPartialMatch;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class HearingConfirmedForCourtApplicationsIngestIT extends AbstractIT {
    private static final String REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.prosecution-case-refer-to-court.json";

    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_HEARING_RESULTED_CASE_UPDATED = "public.hearing.resulted-case-updated";

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String DOCUMENT_TEXT = STRING.next();
    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private ElasticSearchIndexRemoverUtil elasticSearchIndexRemoverUtil;

    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;
    private String applicationId;
    private String initialCaseUrn;

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();

        cleanEventStoreTables();
        cleanViewStoreTables();
    }

    @Before
    public void setUp() throws IOException {
        HearingStub.stubInitiateHearing();
        IdMapperStub.setUp();
        stubDocumentCreate(DOCUMENT_TEXT);
        userId = randomUUID().toString();

        elasticSearchIndexRemoverUtil = new ElasticSearchIndexRemoverUtil();
        elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();

        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.randomUUID().toString();
        applicationId = UUID.randomUUID().toString();
        initialCaseUrn = PreAndPostConditionHelper.generateUrn();
        final String pncId = "2099/1234567L";
        final String croNumber = "1234567";
        deleteAndCreateIndex();
        stubUnifiedSearchQueryExactMatchWithEmptyResults();
        stubUnifiedSearchQueryPartialMatch(randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), pncId, croNumber);
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName) {
        final String strPayload = getPayload(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("APPLICATION_ID", applicationId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    @Ignore("Will be fixed as part of CPI-353, Needs to work with new public.event.hearing.hearing-resulted event from AmendAndReshare feature, see Jira for details")
    @Test
    public void shouldReopenCaseWhenAnewApplicationAddedAndHasFutureHearings() throws Exception {

        courtCentreName = "Lavender Hill Magistrate's Court";
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents.createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId, initialCaseUrn);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        try (final MessageConsumer progressionEventHearingResultedCaseUpdated = privateEvents.createPrivateConsumer("progression.event.hearing-resulted-case-updated")) {
            sendMessage(messageProducerClientPublic,
                    PUBLIC_HEARING_RESULTED, getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_CASE_UPDATED + ".json", caseId,
                            hearingId, defendantId, courtCentreId, "C", "Remedy", "2593cf09-ace0-4b7d-a746-0703a29f33b5"), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyHearingResultedCaseUpdated(progressionEventHearingResultedCaseUpdated, new Matcher[]{
                    withJsonPath("$.prosecutionCase.caseStatus", is(INACTIVE.getDescription()))
            });
        }

        final Matcher[] initialMatchers = {withJsonPath("$.caseStatus", equalTo("INACTIVE")),
                withJsonPath("$.caseId", equalTo(caseId))};

        final Optional<JsonObject> initialElasticSearchCaseResponseJsonObject = findBy(initialMatchers);

        assertTrue(initialElasticSearchCaseResponseJsonObject.isPresent());

        final DocumentContext inputProsecutionCase = initialCase();

        verifyInitialElasticSearchCase(inputProsecutionCase, initialElasticSearchCaseResponseJsonObject.get(), "INACTIVE");

        pollProsecutionCasesProgressionFor(caseId, getCaseStatusMatchers(INACTIVE.getDescription()));

        final String caseUrn = PreAndPostConditionHelper.generateUrn();

        addCourtApplication(caseId, applicationId, caseUrn, "progression.command.create-court-application.json");

        pollForApplicationStatus(applicationId, "DRAFT");

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed-case-reopen.json",
                        caseId, hearingId, defendantId, courtCentreId, courtCentreName), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        pollProsecutionCasesProgressionFor(caseId, getCaseStatusMatchers(ACTIVE.getDescription()));

        final Matcher[] finalMatchers = {withJsonPath("$.caseStatus", equalTo("ACTIVE")),
                withJsonPath("$.caseId", equalTo(caseId))
                ,withJsonPath("$.caseReference", equalTo(initialCaseUrn))};

        final Optional<JsonObject> finalElasticSearchCaseResponseJsonObject = findBy(finalMatchers);

        assertTrue(finalElasticSearchCaseResponseJsonObject.isPresent());

        verifyCaseCreated(3l, inputProsecutionCase, finalElasticSearchCaseResponseJsonObject.get());
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
        );
    }

    private Matcher[] getCaseStatusMatchers(final String caseStatus) {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.caseStatus", equalTo(caseStatus))

        };
    }

    private DocumentContext initialCase() throws IOException {
        final String commandJson = createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), initialCaseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);
        final JsonObject commandJsonInputJson = jsonFromString(commandJson);
        final DocumentContext prosecutionCase = parse(commandJsonInputJson);
        final JsonObject prosecutionCaseJO = prosecutionCase.read("$.courtReferral.prosecutionCases[0]");
        final JsonObject prosecutionCaseEvent = Json.createObjectBuilder().add("prosecutionCase", prosecutionCaseJO).build();
        return parse(prosecutionCaseEvent);
    }

    private boolean isPartiesPopulated(final JsonObject jsonObject) {
        return jsonFromString(getJsonArray(jsonObject, "index").get().getString(0))
                .containsKey("parties");
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private void doVerifyHearingResultedCaseUpdated(final MessageConsumer progressionEventHearingResultedCaseUpdated, final Matcher[] matchers) {
        final Poller poller = new Poller(100, 1000L);
        poller.pollUntilFound(() -> {
            try {
                final Optional<String> message = retrieveMessageAsString(progressionEventHearingResultedCaseUpdated, 1000L);
                assertThat(message.get(), isJson(allOf(matchers)));
                return message;
            } catch (AssertionError e) {
                return empty();
            } catch (Exception e) {
                fail();
            }
            return empty();
        });
    }
}
