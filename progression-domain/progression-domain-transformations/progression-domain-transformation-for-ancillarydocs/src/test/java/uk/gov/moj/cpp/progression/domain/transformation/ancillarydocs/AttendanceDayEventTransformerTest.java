package uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs.core.SchemaVariableConstants.PROGRESSION_EVENT_HEARING_APPLICATION_LINK_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs.core.SchemaVariableConstants.PROGRESSION_EVENT_HEARING_RESULTED;
import static uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs.core.SchemaVariableConstants.PROGRESSION_EVENT_NOWS_REQUESTED;
import static uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs.core.SchemaVariableConstants.PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.Test;
@ExtendWith(MockitoExtension.class)
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AttendanceDayEventTransformerTest {


    private String file;
    private String eventName;

    public AttendanceDayEventTransformerTest(final String file, final String eventName) {
        this.file = file;
        this.eventName = eventName;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"progression.event.hearing-resulted.json", PROGRESSION_EVENT_HEARING_RESULTED},
                {"progression.event.nows-requested.json", PROGRESSION_EVENT_NOWS_REQUESTED},
                {"progression.event.hearing-application-link-created.json", PROGRESSION_EVENT_HEARING_APPLICATION_LINK_CREATED},
                {"progression.event.prosecutionCase-defendant-listing-status-changed.json", PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED}
        });
    }

    @Test
    public void transform() {
        JsonObject oldJsonObject = loadTestFile("old/" + file);
        JsonObject expectedJsonObject = loadTestFile("new/" + file);
        JsonObject resultJsonObject = new AttendanceDayEventTransformer().transform(eventName, oldJsonObject);
        assertThat(expectedJsonObject.toString(), equalTo(resultJsonObject.toString()));
    }

    private JsonObject loadTestFile(String resourceFileName) {
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceFileName);
            final JsonReader jsonReader = Json.createReader(is);
            return jsonReader.readObject();

        } catch (Exception ex) {
            throw new RuntimeException("failed to load test file " + resourceFileName, ex);
        }
    }
}