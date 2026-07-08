package uk.gov.moj.cpp.progression.event.contract;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Validates that every sample event payload published by mi (via the
 * mireportdata-progression-schemas jar) still matches its declared JSON Schema.
 *
 * Replaces the previous approach of unpacking mireportdata-progression-event-listener's
 * -listener-tests and -processor-tests classifiers into progression's test-classes
 * directory. That approach coupled progression to mi's compiled bytecode (and
 * therefore to mi's framework version). This test consumes only JSON files, so
 * mi and progression can now upgrade their framework versions independently.
 */
@DisplayName("MI contract: progression event samples must match published schemas")
class MiProgressionSchemaContractTest {

    private static final String SCHEMAS_ROOT = "uk/gov/moj/cpp/mi/reportdata/progression/schemas";
    private static final String SAMPLES_ROOT = "uk/gov/moj/cpp/mi/reportdata/progression/samples";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonSchemaFactory SCHEMA_FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

    static Stream<String> sampleEventNames() throws IOException, URISyntaxException {
        return listResourceFiles(SAMPLES_ROOT).stream()
                .filter(name -> name.endsWith(".json"))
                .map(name -> name.substring(0, name.length() - ".json".length()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sampleEventNames")
    void sampleShouldConformToSchema(final String eventName) throws IOException {
        final InputStream schemaStream = classpathResource(SCHEMAS_ROOT + "/" + eventName + ".json");
        if (schemaStream == null) {
            // Sample exists but no matching schema in mi. Flag it — this indicates
            // a contract that mi ships a sample for but doesn't formally define.
            fail("No JSON Schema found for event '" + eventName
                    + "'. Expected on classpath at " + SCHEMAS_ROOT + "/" + eventName + ".json. "
                    + "Either mi should publish the schema, or the sample should be removed.");
            return;
        }

        final JsonSchema schema = SCHEMA_FACTORY.getSchema(schemaStream);
        final JsonNode payload;
        try (InputStream sampleStream = classpathResource(SAMPLES_ROOT + "/" + eventName + ".json")) {
            payload = MAPPER.readTree(sampleStream);
        }

        final Set<ValidationMessage> errors = schema.validate(payload);
        assertTrue(errors.isEmpty(),
                () -> "Sample payload for event '" + eventName
                        + "' failed JSON Schema validation. "
                        + "This means mi's schema and its own sample payload disagree — "
                        + "notify the mi team so they can align them.\nErrors:\n"
                        + errors.stream().map(ValidationMessage::toString).collect(toList()));
    }

    private static InputStream classpathResource(final String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }

    private static List<String> listResourceFiles(final String path) throws IOException, URISyntaxException {
        // Resources are packaged inside a jar dependency, so we list them by walking
        // the jar's classpath entry. This avoids hardcoding a filename list.
        final java.net.URL root = Thread.currentThread().getContextClassLoader().getResource(path);
        if (root == null) {
            throw new IllegalStateException(
                    "Resource directory '" + path + "' not found on classpath. "
                            + "Ensure mireportdata-progression-schemas is on the test classpath.");
        }
        if ("jar".equals(root.getProtocol())) {
            final String jarPath = root.getPath().substring(5, root.getPath().indexOf('!'));
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath)) {
                return jar.stream()
                        .map(java.util.jar.JarEntry::getName)
                        .filter(name -> name.startsWith(path + "/") && !name.endsWith("/"))
                        .map(name -> name.substring(path.length() + 1))
                        .collect(toList());
            }
        }
        // Fallback for exploded classpath (e.g. running from an IDE against the module directly).
        final Path dir = Paths.get(root.toURI());
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.map(p -> p.getFileName().toString()).collect(toList());
        }
    }
}
