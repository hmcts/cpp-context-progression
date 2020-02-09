package uk.gov.justice.api.resource.utils.payload;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.core.courts.PleaValue;

import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.junit.Test;

public class PleaValueDescriptionBuilderTest {

    @Test
    public void shouldHavePleaDescriptionForAllPleaValues() {
        //test to check that PleaValueDescription has all values in PleaValue
        for (final PleaValue pleaValue : PleaValue.values()) {
            assertThat(PleaValueDescriptionBuilder.PleaValueDescription.descriptionFor(pleaValue.toString()).isPresent(), is(true));
        }
    }

    @Test
    public void shouldRebuildWithPleaValueDescription() throws Exception {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final InputStream stream = loader.getResourceAsStream("payload.json");
             final JsonReader jsonReader = Json.createReader(stream);
             final InputStream streamResult = loader.getResourceAsStream("payload-with-description.json");
             final JsonReader jsonResultReader = Json.createReader(streamResult)) {
            final JsonObject payload = jsonReader.readObject();
            final JsonObject result = jsonResultReader.readObject();
            final PleaValueDescriptionBuilder pleaValueDescriptionBuilder = new PleaValueDescriptionBuilder();
            final JsonObject newPayload = pleaValueDescriptionBuilder.rebuildWithPleaValueDescription(payload);
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
        final PleaValueDescriptionBuilder pleaValueDescriptionBuilder = new PleaValueDescriptionBuilder();
        final JsonObject newPayload = pleaValueDescriptionBuilder.rebuildWithPleaValueDescription(payload);
        assertThat(newPayload, is(payload));
    }
}