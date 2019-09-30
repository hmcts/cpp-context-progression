package uk.gov.moj.cpp.data.anonymization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.data.anonymization.generator.ParseDataGenerator;

import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class ParseDataGeneratorTest {

    final ParseDataGenerator parseDataGenerator = new ParseDataGenerator();

    @Test
    public void shouldGeneratAnonynisedParsedData() throws IOException {
        final StringWriter stringWriter = new StringWriter();
        final InputStream stream = ParseDataGeneratorTest.class.getResourceAsStream("/testFieldData.json");
        IOUtils.copy(stream, stringWriter, "UTF-8");
        final String anonymisedParseData = parseDataGenerator.convert(stringWriter.toString());
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonObject anonymisedJsonPayload = mapper.readValue(StringEscapeUtils.unescapeJson(anonymisedParseData.trim()), JsonObject.class);

        assertEquals("XXXX",anonymisedJsonPayload.getString("defendantFirstName"));
        assertEquals("14001063-c81a-4db1-9f4c-fb8fdb07e0b6",anonymisedJsonPayload.getString("targetId"));
    }
}
