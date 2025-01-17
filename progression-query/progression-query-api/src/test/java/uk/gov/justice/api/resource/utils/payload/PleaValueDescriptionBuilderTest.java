package uk.gov.justice.api.resource.utils.payload;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.services.core.requester.Requester;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleaValueDescriptionBuilderTest {

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private Requester requester;

    @InjectMocks
    private PleaValueDescriptionBuilder pleaValueDescriptionBuilder;

    @BeforeEach
    public void setUp() {
        when(referenceDataService.retrievePleaTypeDescriptions()).thenReturn(buildPleaTypeDescriptions());
    }

    @Test
    public void shouldRebuildWithPleaValueDescription() throws Exception {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final InputStream stream = loader.getResourceAsStream("payload.json");
             final JsonReader jsonReader = Json.createReader(stream);

             final InputStream streamResult = loader.getResourceAsStream("payload-with-description.json");
             final JsonReader jsonResultReader = Json.createReader(streamResult)) {
             final JsonObject result = jsonResultReader.readObject();

             final JsonObject payload = jsonReader.readObject();
             final JsonObject newPayload = pleaValueDescriptionBuilder.rebuildPleaWithDescription(payload);
             assertThat(newPayload, is(result));
        }
    }

    @Test
    public void shouldRebuildAnyObjectAsIs() throws Exception {
        final JsonObjectBuilder target = createObjectBuilder();
        target.add("test1", "new string test1");
        final JsonArrayBuilder array = createArrayBuilder();
        array.add("1");
        array.add("2");
        target.add("array", array.build());
        final JsonObject payload = target.build();
        final JsonObject newPayload = pleaValueDescriptionBuilder.rebuildPleaWithDescription(payload);
        assertThat(newPayload, is(payload));
    }

    private Map<String, String> buildPleaTypeDescriptions() {
        final Map<String, String> pleaStatusTypeDescriptions = new HashMap<>();
        pleaStatusTypeDescriptions.put("CHANGE_TO_GUILTY_AFTER_SWORN_IN", "Change of Plea: Not Guilty to Guilty (After Jury sworn in)");
        pleaStatusTypeDescriptions.put("CHANGE_TO_GUILTY_NO_SWORN_IN", "Change of Plea: Not Guilty to Guilty (No Jury sworn in)");
        return pleaStatusTypeDescriptions;
    }

}