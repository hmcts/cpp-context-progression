package uk.gov.moj.cpp.progression.helper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class NullAwareJsonObjectBuilderTest {
    @Test
    public void testAddString() {
        final JsonObjectBuilder builder =
                        NullAwareJsonObjectBuilder.wrap(Json.createObjectBuilder());
        final String value = null;
        final String name = null;
        final JsonObject json =
                        builder.add("name", value).add(name, "value").add("myname", "myvalue")
                                        .build();
        assertThat(json.getString("name", null), equalTo(null));
        assertThat(json.getString("myname"), equalTo("myvalue"));
    }
}
