package uk.gov.moj.cpp.progression.query.view.utils;

import java.io.InputStream;

import org.everit.json.schema.loader.SchemaClient;

public class CustomSchemaClient implements SchemaClient {

    public static final String HTTP_JUSTICE_GOV_UK_CORE_COURTS_EXTERNAL = "http://justice.gov.uk/core/courts/external";

    @Override
    public InputStream get(String url) {
        if (url.startsWith(HTTP_JUSTICE_GOV_UK_CORE_COURTS_EXTERNAL)) {
            String resourcePath = url.replace(HTTP_JUSTICE_GOV_UK_CORE_COURTS_EXTERNAL, "json/schema/external/global");
            return CustomSchemaClient.class.getClassLoader().getResourceAsStream(resourcePath);
        }
        throw new RuntimeException("Unsupported schema reference: " + url);
    }

}