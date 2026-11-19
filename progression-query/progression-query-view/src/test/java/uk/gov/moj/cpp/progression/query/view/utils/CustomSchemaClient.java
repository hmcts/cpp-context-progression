package uk.gov.moj.cpp.progression.query.view.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import uk.gov.justice.schema.catalog.CatalogObjectFactory;
import uk.gov.justice.schema.catalog.RawCatalog;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.everit.json.schema.loader.SchemaClient;

public class CustomSchemaClient implements SchemaClient {

    public static final String HTTP_JUSTICE_GOV_UK_CORE_COURTS_EXTERNAL = "http://justice.gov.uk/core/courts/external";

    private static RawCatalog rawCatalog;

    @Override
    public InputStream get(String url) {
        if (url.startsWith(HTTP_JUSTICE_GOV_UK_CORE_COURTS_EXTERNAL)) {
            String resourcePath = url.replace(HTTP_JUSTICE_GOV_UK_CORE_COURTS_EXTERNAL, "json/schema/external/global");
            return CustomSchemaClient.class.getClassLoader().getResourceAsStream(resourcePath);
        }
        // Any other id is looked up in the schema catalogs on the classpath (e.g. progression datatypes-common, core domain)
        return rawCatalog().getRawJsonSchema(url)
                .map(schema -> (InputStream) new ByteArrayInputStream(schema.getBytes(UTF_8)))
                .orElseThrow(() -> new RuntimeException("Unsupported schema reference: " + url));
    }

    private static synchronized RawCatalog rawCatalog() {
        if (rawCatalog == null) {
            rawCatalog = new CatalogObjectFactory().rawCatalog();
            rawCatalog.initialize();
        }
        return rawCatalog;
    }
}
