package uk.gov.moj.cpp.data.anonymization;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class SchemaValidatorUtil {
    private SchemaValidatorUtil() {}

    public static void validateAgainstSchema(final String schemaFileName, final String jsonString) throws IOException {
        final URL resource = SchemaValidatorUtil.class.getResource(schemaFileName);
        final InputStream inputStream = resource.openStream();
        final JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));

        final Schema schema = SchemaLoader.load(rawSchema);
        schema.validate(new JSONObject(jsonString));
    }
}
