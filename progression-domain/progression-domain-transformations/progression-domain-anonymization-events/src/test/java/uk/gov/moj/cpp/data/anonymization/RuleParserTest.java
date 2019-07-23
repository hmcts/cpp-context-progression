package uk.gov.moj.cpp.data.anonymization;

import org.everit.json.schema.SchemaException;
import org.everit.json.schema.ValidationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.anyOf;

public class RuleParserTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testLoadAnanymisationRules() throws IOException {
        final Map<String, Map<String, String>> result = new RuleParser().loadAnanymisationRules("/data.anonymisation.json");
        assert (result.size() ==1);
    }
    @Test
    public void testLoadAnanymisationRules1() throws IOException {

        expectedException.expect(anyOf(
                instanceOf(IOException.class),
                instanceOf(SchemaException.class), instanceOf(ValidationException.class)));

        new RuleParser().loadAnanymisationRules("/data.anonymisation-invalid.json");
    }
}
