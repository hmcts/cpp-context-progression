package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyAddCourtApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyStandaloneApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getStringFromResource;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.moj.cpp.progression.AbstractIT;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


public class CourtApplicationCreatedIngesterIT extends AbstractIT {

    private static final String CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.create-court-application.json";
    public static final String APPLICATIONS_PROGRESSION_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION_JSON = "ingestion/progression.initiate-court-proceedings-for-application.json";

    private String applicationId;
    private String applicantId;
    private String applicantDefendantId;
    private String respondantId;
    private String respondantDefendantId;
    private String caseId;
    private String applicationStatus;

    @BeforeEach
    public void setup() {
        applicationId = randomUUID().toString();
        applicantId = randomUUID().toString();
        applicantDefendantId = randomUUID().toString();
        respondantId = randomUUID().toString();
        respondantDefendantId = randomUUID().toString();
        applicationStatus= ApplicationStatus.DRAFT.toString();
        deleteAndCreateIndex();
    }

    @Test
    public void shouldIndexCreateCourtApplicationEvent() throws Exception {

        addCourtApplicationForIngestion(applicationId, applicationId, applicantId, applicantDefendantId, respondantId, respondantDefendantId, applicationStatus,CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);

        final Matcher[] matchers = {withJsonPath("$.caseId", equalTo(applicationId))};

        final Optional<JsonObject> courApplicationCreatedResponseJsonObject = findBy(matchers);

        assertTrue(courApplicationCreatedResponseJsonObject.isPresent());

        final String payloadStr = getStringFromResource(CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId)
                .replaceAll("RANDOM_RESPONDANT_ID", respondantId)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondantDefendantId)
                .replaceAll("APPLICATION_STATUS", applicationStatus)
                .replaceAll("RANDOM_REFERENCE", UUID.randomUUID().toString());


        final JsonObject inputApplication = jsonFromString(payloadStr);

        final DocumentContext inputCourtApplication = parse(inputApplication);

        verifyStandaloneApplication(applicationId, courApplicationCreatedResponseJsonObject.get());
        verifyAddCourtApplication(inputCourtApplication, courApplicationCreatedResponseJsonObject.get(), applicationId);
    }

    @Disabled("DD-20992")
    @Test
    public void initiateCourtProceedingsForApplicationShouldInitialiseHearingListingStatusAndIngestJurisdictionToUnifiedSearch() throws IOException {
        caseId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, APPLICATIONS_PROGRESSION_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION_JSON);

        final Matcher[] matchers = {withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$._is_magistrates", equalTo(true))};

        final Optional<JsonObject> courApplicationCreatedResponseJsonObject = findBy(matchers);
        assertTrue(courApplicationCreatedResponseJsonObject.isPresent());
    }
}



