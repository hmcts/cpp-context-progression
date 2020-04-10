package uk.gov.moj.cpp.progression.domain.transformation.corechanges.transform;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_APPLICATION_REFERRED_TO_COURT;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_BOXWORK_APPLICATION_REFERRED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_COURT_APPLICATION_ADDED_TO_CASE;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_COURT_APPLICATION_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_COURT_APPLICATION_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_COURT_PROCEEDINGS_INITIATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_HEARING_APPLICATION_LINK_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_HEARING_EXTENDED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_HEARING_INITIATE_ENRICHED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_HEARING_RESULTED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_HEARING_RESULTED_CASE_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_LISTED_COURT_APPLICATION_CHANGED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_NOWS_REQUESTED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_PROSECUTION_CASE_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_PROSECUTION_CASE_DEFENDANT_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_REFERRED_TO_COURT;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CourtProceedingsInitiatedEventTransformerTest {


    private final String file;
    private final String eventName;

    public CourtProceedingsInitiatedEventTransformerTest(final String file, final String eventName) {
        this.file = file;
        this.eventName = eventName;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"progression.event.application-referred-to-court.json", PROGRESSION_APPLICATION_REFERRED_TO_COURT},
                {"progression.event.boxwork-application-referred.json", PROGRESSION_BOXWORK_APPLICATION_REFERRED},
                {"progression.event.cases-referred-to-court.json", PROGRESSION_REFERRED_TO_COURT},
                {"progression.event.court-application-created.json", PROGRESSION_COURT_APPLICATION_CREATED},
                {"progression.event.court-application-added-to-case.json", PROGRESSION_COURT_APPLICATION_ADDED_TO_CASE},
                {"progression.event.court-application-updated.json", PROGRESSION_COURT_APPLICATION_UPDATED},
                {"progression.hearing-initiate-enriched.json", PROGRESSION_HEARING_INITIATE_ENRICHED},
                {"progression.event.hearing-application-link-created.json", PROGRESSION_HEARING_APPLICATION_LINK_CREATED},
                {"progression.event.hearing-extended.json", PROGRESSION_HEARING_EXTENDED},
                {"progression.event.hearing-resulted.json", PROGRESSION_HEARING_RESULTED},
                {"progression.event.hearing-resulted-case-updated.json", PROGRESSION_HEARING_RESULTED_CASE_UPDATED},
                {"progression.event.nows-requested.json", PROGRESSION_NOWS_REQUESTED},
                {"progression.event.prosecution-case-created.json", PROGRESSION_PROSECUTION_CASE_CREATED},
                {"progression.event.prosecutionCase-defendant-listing-status-changed.json", PROGRESSION_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED},
                {"progression.event.prosecution-case-defendant-updated.json", PROGRESSION_PROSECUTION_CASE_DEFENDANT_UPDATED},
                {"progression.event.court-proceedings-initiated.json", PROGRESSION_COURT_PROCEEDINGS_INITIATED},
                {"progression.event.listed-court-application-changed.json", PROGRESSION_LISTED_COURT_APPLICATION_CHANGED},
        });
    }

    @Test
    public void transform() {
        final JsonObject oldJsonObject = loadTestFile("court-proceedings-initiated/old/" + file);
        final JsonObject expectedJsonObject = loadTestFile("court-proceedings-initiated/new/" + file);
        final Metadata metadata = DefaultJsonMetadata.metadataBuilder()
                .withName(eventName).withId(randomUUID())
                .createdAt(ZonedDateTime.parse("2020-02-21T11:49:18.116Z")).build();
        final JsonObject resultJsonObject = new CourtProceedingsInitiatedEventTransformer().transform(
                metadata, oldJsonObject);
        assertThat(expectedJsonObject.toString(), equalTo(resultJsonObject.toString()));
    }

    private JsonObject loadTestFile(final String resourceFileName) {
        try {
            final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceFileName);
            final JsonReader jsonReader = Json.createReader(is);
            return jsonReader.readObject();
        } catch (final Exception ex) {
            throw new RuntimeException("failed to load test file " + resourceFileName, ex);
        }
    }
}
