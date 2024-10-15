package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.unifiedsearch.test.util.constant.ApplicationExternalCreatorType;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApplicationReferredToBoxworkIngesterIT extends AbstractIT {

    private String applicationId;
    private String caseId;

    @BeforeEach
    public void setup() throws IOException {
        caseId = randomUUID().toString();
        applicationId = randomUUID().toString();
        cleanViewStoreTables();
        deleteAndCreateIndex();
    }

    @AfterAll
    public static void tearDown() {
        cleanEventStoreTables();
        cleanViewStoreTables();
    }

    @Test
    public void shouldIngestApplicationReferredToBoxworkEvent() throws IOException, InterruptedException {

        //GIVEN - WHEN
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "ingestion/progression.initiate-court-proceedings-for-standalone-application-box-hearing.json");
        TimeUnit.MILLISECONDS.sleep(4000);

        //THEN
        final Matcher[] caseMatcher = {withJsonPath("$.caseId", equalTo(caseId))};
        final Optional<JsonObject> courtApplicationResponseJsonObject = findBy(caseMatcher);
        assertTrue(courtApplicationResponseJsonObject.isPresent());

        final String outApplicationStatus = courtApplicationResponseJsonObject.get().getJsonArray("applications").getJsonObject(0).getJsonString("applicationStatus").getString();
        final String outapplicationExternalCreatorType = courtApplicationResponseJsonObject.get().getJsonArray("applications").getJsonObject(0).getJsonString("applicationExternalCreatorType").getString();
        final String outputCaseApplicationId = courtApplicationResponseJsonObject.get().getJsonArray("applications").getJsonObject(0).getJsonString("applicationId").getString();
        final String outputCaseId = courtApplicationResponseJsonObject.get().getJsonString("caseId").getString().toString();

        assertEquals(ApplicationStatus.IN_PROGRESS.toString(), outApplicationStatus);
        assertEquals(ApplicationExternalCreatorType.PROSECUTOR.name(), outapplicationExternalCreatorType);
        assertEquals(applicationId, outputCaseApplicationId);
        assertEquals(caseId, outputCaseId);
    }
}
