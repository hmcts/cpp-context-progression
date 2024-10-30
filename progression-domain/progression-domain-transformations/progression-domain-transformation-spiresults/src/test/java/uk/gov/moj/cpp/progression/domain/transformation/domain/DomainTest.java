package uk.gov.moj.cpp.progression.domain.transformation.domain;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.io.File;
import java.io.IOException;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

public class DomainTest {

    @Test
    public void testStringToDomainConversion() throws IOException {
        final String resultDefinitionPayload = readFileToString(new File("src/test/resources/sample-result-definition-payload.json"));
        final ResultDefinition resultDefinition = new Gson().fromJson(resultDefinitionPayload, ResultDefinition.class);
        assertThat(resultDefinition, notNullValue());
        assertThat(resultDefinition.getId(), is("418b3aa7-65ab-4a4a-bab9-2f96b698118c"));
        assertThat(resultDefinition.getLabel(), is("value"));
        assertThat(resultDefinition.getUrgent(), is("Y"));
        assertThat(resultDefinition.getD20(), nullValue());
        assertThat(resultDefinition.getPrompts(), hasSize(1));
        assertThat(resultDefinition.getPrompts().get(0).getId(), is("d6caa3c4-ec9d-41ec-8f86-2c617ef0d5d9"));
        assertThat(resultDefinition.getPrompts().get(0).getLabel(), is("prompt label"));
    }
}
