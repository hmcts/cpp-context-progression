package uk.gov.moj.cpp.progression.material;

/**
 * Progression's own copy of material's endpoint constants.
 *
 * Progression used to compile against the material-client jar. That is a Java dependency on another
 * context, so it forces a release order: material has to publish before progression can build. It
 * also blocked the Java 25 upgrade outright, because the only Jakarta build of material-client is a
 * SNAPSHOT and a committed SNAPSHOT cannot be merged.
 *
 * The values are the published contract - the same URLs any consumer would be given - so copying
 * them is the same kind of contract ownership as holding a consumer's copy of an event schema.
 */
@SuppressWarnings("squid:S1075")
public final class MaterialUrls {

    public static final String BASE_URI = "http://localhost:8080/material-query-api/query/api/rest/material";
    public static final String COMMAND_BASE_URI = "http://localhost:8080/material-command-api/command/api/rest/material";
    public static final String MATERIAL_REQUEST_PATH = "/material/";
    public static final String MATERIAL_STREAM_PDF_PARAMETERS = "?stream=true&requestPdf=true";

    private MaterialUrls() {
    }
}
