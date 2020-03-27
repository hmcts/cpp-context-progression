package uk.gov.moj.cpp.progression.domain.transformation.helper;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.is;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import junit.framework.TestCase;
import org.hamcrest.MatcherAssert;

public class JsonPathHelperTest extends TestCase {

    public void testGetValueForJsonPathReturningSingleValue() throws IOException {
        final String payload = readFileToString(new File("src/test/resources/sample-helper-payload.json"));
        final Optional<String> firstValueForJsonPath = JsonPathHelper.getFirstValueForJsonPath(payload, "$.prompts[0].id");
        MatcherAssert.assertThat(firstValueForJsonPath.get(), is("d6caa3c4-ec9d-41ec-8f86-2c617ef0d5d9"));
    }

    public void testGetFirstValueForJsonPathReturningList() throws IOException {
        final String payload = readFileToString(new File("src/test/resources/sample-helper-payload.json"));
        final Optional<String> firstValueForJsonPath = JsonPathHelper.getFirstValueForJsonPath(payload, "$..label");
        MatcherAssert.assertThat(firstValueForJsonPath.get(), is("value"));
    }

    public void testGetLastValueForJsonPathReturningList() throws IOException {
        final String payload = readFileToString(new File("src/test/resources/sample-helper-payload.json"));
        final Optional<String> firstValueForJsonPath = JsonPathHelper.getLastValueForJsonPath(payload, "$..label");
        MatcherAssert.assertThat(firstValueForJsonPath.get(), is("prompt label 2"));
    }
}