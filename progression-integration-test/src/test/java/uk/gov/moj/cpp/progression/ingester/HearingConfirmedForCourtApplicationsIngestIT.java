package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.createReferProsecutionCaseToCrownCourtJsonBody;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.HearingResultedCaseUpdatedVerificationHelper.verifyInitialElasticSearchCase;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseCreated;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryExactMatchWithEmptyResults;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryPartialMatch;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HearingConfirmedForCourtApplicationsIngestIT extends AbstractIT {
    private static final String REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.prosecution-case-refer-to-court.json";

    private static final String PUBLIC_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    private static final String PUBLIC_HEARING_RESULTED_CASE_UPDATED = "public.hearing.resulted-case-updated2";

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

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

    @BeforeEach
    public void setUp() throws IOException {

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

    @Test
    public void shouldReopenCaseWhenAnewApplicationAddedAndHasFutureHearings() throws Exception {

        courtCentreName = "Lavender Hill Magistrate's Court";
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");

        addProsecutionCaseToCrownCourt(caseId, defendantId, initialCaseUrn);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject publicEventPayload = getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_CASE_UPDATED + ".json", caseId,
                hearingId, defendantId, courtCentreId, "C", "Remedy", "2593cf09-ace0-4b7d-a746-0703a29f33b5");
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED, envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED, userId), publicEventPayload));


        final Matcher[] initialMatchers = {withJsonPath("$.caseStatus", equalTo("INACTIVE")),
                withJsonPath("$.caseId", equalTo(caseId))};

        final Optional<JsonObject> initialElasticSearchCaseResponseJsonObject = findBy(initialMatchers);

        assertTrue(initialElasticSearchCaseResponseJsonObject.isPresent());

        final DocumentContext inputProsecutionCase = initialCase();

        verifyInitialElasticSearchCase(inputProsecutionCase, initialElasticSearchCaseResponseJsonObject.get(), "INACTIVE");

        pollProsecutionCasesProgressionFor(caseId, getCaseStatusMatchers(INACTIVE.getDescription()));

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, hearingId, "applications/progression.initiate-court-proceedings-for-standalone-application-box-hearing.json");
        pollForApplication(applicationId);

        final JsonObject publicListingHearingConfirmedEventPayload = getHearingJsonObject("public.listing.hearing-confirmed-case-reopen.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), publicListingHearingConfirmedEventPayload));

        final Matcher[] finalMatchers = {withJsonPath("$.caseStatus", equalTo("ACTIVE")),
                withJsonPath("$.caseId", equalTo(caseId))
                , withJsonPath("$.caseReference", equalTo(initialCaseUrn))};

        final Optional<JsonObject> finalElasticSearchCaseResponseJsonObject = findBy(finalMatchers);

        assertTrue(finalElasticSearchCaseResponseJsonObject.isPresent());

        verifyCaseCreated(1l, inputProsecutionCase, finalElasticSearchCaseResponseJsonObject.get());
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
                        .replaceAll("APPLICATION_ID", applicationId)
        );
    }

    private Matcher[] getCaseStatusMatchers(final String caseStatus) {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.caseStatus", equalTo(caseStatus))

        };
    }

    private DocumentContext initialCase() {
        final String commandJson = createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), initialCaseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);
        final JsonObject commandJsonInputJson = jsonFromString(commandJson);
        final DocumentContext prosecutionCase = parse(commandJsonInputJson);
        final JsonObject prosecutionCaseJO = prosecutionCase.read("$.courtReferral.prosecutionCases[0]");
        final JsonObject prosecutionCaseEvent = JsonObjects.createObjectBuilder().add("prosecutionCase", prosecutionCaseJO).build();
        return parse(prosecutionCaseEvent);
    }
}
