package uk.gov.moj.cpp.progression.material;

import static uk.gov.moj.cpp.progression.material.MaterialUrls.BASE_URI;
import static uk.gov.moj.cpp.progression.material.MaterialUrls.MATERIAL_REQUEST_PATH;
import static uk.gov.moj.cpp.progression.material.MaterialUrls.MATERIAL_STREAM_PDF_PARAMETERS;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Progression's copy of material's URL-building helper, replacing the one from the material-client
 * jar. It only builds strings - no HTTP call is made here - and the output is identical to the
 * original for every input.
 */
@ApplicationScoped
public class MaterialUrlGenerator {

    public String pdfFileStreamUrlFor(final UUID materialId) {
        return baseFileStreamUrl(materialId) + MATERIAL_STREAM_PDF_PARAMETERS;
    }

    public String fileStreamUrlFor(final UUID materialId, final boolean pdfStream) {
        return pdfStream ? pdfFileStreamUrlFor(materialId) : baseFileStreamUrl(materialId);
    }

    public String fileStreamUrlFor(final UUID materialId) {
        return fileStreamUrlFor(materialId, false);
    }

    private String baseFileStreamUrl(final UUID materialId) {
        return BASE_URI + MATERIAL_REQUEST_PATH + materialId;
    }
}
