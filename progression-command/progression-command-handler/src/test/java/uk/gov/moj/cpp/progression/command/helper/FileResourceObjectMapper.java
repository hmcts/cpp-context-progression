package uk.gov.moj.cpp.progression.command.helper;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FileResourceObjectMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    public <T> T convertFromFile(final String url, final Class<T> clazz) throws IOException {
        return OBJECT_MAPPER.readValue(new File(this.getClass().getClassLoader().getResource(url).getFile()), clazz);
    }
}
