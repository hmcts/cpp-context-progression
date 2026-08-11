package uk.gov.moj.cpp.progression.material.client;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class MaterialUrlGeneratorTest {

    private static final String BASE_URI = "http://localhost:8080/material-query-api/query/api/rest/material";
    private static final String MATERIAL_REQUEST_PATH = "/material/";
    private static final String MATERIAL_STREAM_PDF_PARAMETERS = "?stream=true&requestPdf=true";

    private final MaterialUrlGenerator materialUrlGenerator = new MaterialUrlGenerator();

    @Test
    public void shouldGeneratePdfMaterialUrlStringWithGivenMaterialId() {
        final UUID materialId = randomUUID();
        final String expectedUrl = BASE_URI + MATERIAL_REQUEST_PATH + materialId + MATERIAL_STREAM_PDF_PARAMETERS;

        assertThat(materialUrlGenerator.pdfFileStreamUrlFor(materialId), is(expectedUrl));
    }

    @Test
    public void shouldGenerateNonPdfMaterialUrlStringWithGivenMaterialId() {
        final UUID materialId = randomUUID();
        final String expectedUrl = BASE_URI + MATERIAL_REQUEST_PATH + materialId;

        assertThat(materialUrlGenerator.fileStreamUrlFor(materialId), is(expectedUrl));
    }

    @Test
    public void shouldGeneratePdfMaterialUrlStringWithGivenMaterialIdWithPdfFlag() {
        final UUID materialId = randomUUID();
        final String expectedUrl = BASE_URI + MATERIAL_REQUEST_PATH + materialId + MATERIAL_STREAM_PDF_PARAMETERS;

        assertThat(materialUrlGenerator.fileStreamUrlFor(materialId, true), is(expectedUrl));
    }

    @Test
    public void shouldGenerateNonPdfMaterialUrlStringWithGivenMaterialIdWithNonPdfFlag() {
        final UUID materialId = randomUUID();
        final String expectedUrl = BASE_URI + MATERIAL_REQUEST_PATH + materialId;

        assertThat(materialUrlGenerator.fileStreamUrlFor(materialId, false), is(expectedUrl));
    }

}
