package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.unifiedsearch.test.util.constant.ApplicationExternalCreatorType;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApplicationReferredToBoxworkIngesterIT extends AbstractIT {

    private String applicationId;
    private String caseId;

    @BeforeEach
    public void setup() {
        caseId = randomUUID().toString();
        applicationId = randomUUID().toString();
        deleteAndCreateIndex();
    }

    @AfterAll
    public static void tearDown() {
        cleanEventStoreTables();
    }

    @Test
    public void shouldIngestApplicationReferredToBoxworkEvent() throws IOException {

        //GIVEN - WHEN
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "ingestion/progression.initiate-court-proceedings-for-standalone-application-box-hearing.json");
        pollForCourtApplication(applicationId);

        //THEN
        final Matcher[] caseMatcher = {
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.applications[0].applicationStatus", equalTo(ApplicationStatus.IN_PROGRESS.toString())),
                withJsonPath("$.applications[0].applicationExternalCreatorType", equalTo(ApplicationExternalCreatorType.PROSECUTOR.name())),
                withJsonPath("$.applications[0].applicationId", equalTo(applicationId)),
        };
        final Optional<JsonObject> courtApplicationResponseJsonObject = findBy(caseMatcher);
        assertTrue(courtApplicationResponseJsonObject.isPresent());
    }
}
