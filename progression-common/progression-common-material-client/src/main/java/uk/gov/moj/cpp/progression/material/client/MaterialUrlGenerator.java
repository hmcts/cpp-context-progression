package uk.gov.moj.cpp.progression.material.client;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

/**
 * Progression-owned copy of material's URL-building helper (decouples progression from the
 * material-client JAR - PEG-3444). Builds material download URL strings; no HTTP call is made here.
 */
@ApplicationScoped
public class MaterialUrlGenerator {

    private static final String BASE_URI = "http://localhost:8080/material-query-api/query/api/rest/material";
    private static final String MATERIAL_REQUEST_PATH = "/material/";
    private static final String MATERIAL_STREAM_PDF_PARAMETERS = "?stream=true&requestPdf=true";

    private String baseFileStreamUrl(final UUID materialId) {
        return BASE_URI + MATERIAL_REQUEST_PATH + materialId;
    }

    public String pdfFileStreamUrlFor(final UUID materialId) {
        return baseFileStreamUrl(materialId) + MATERIAL_STREAM_PDF_PARAMETERS;
    }

    public String fileStreamUrlFor(final UUID materialId, final boolean pdfStream) {
        return pdfStream ? pdfFileStreamUrlFor(materialId) : baseFileStreamUrl(materialId);
    }

    public String fileStreamUrlFor(final UUID materialId) {
        return fileStreamUrlFor(materialId, false);
    }

}
