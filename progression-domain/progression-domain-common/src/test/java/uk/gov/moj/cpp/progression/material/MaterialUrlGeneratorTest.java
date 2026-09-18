package uk.gov.moj.cpp.progression.material;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * These assert the finished URLs as literal strings rather than by rebuilding them from the
 * constants in MaterialUrls. Rebuilding would make the test agree with whatever the code produces,
 * including a change that silently breaks the contract material publishes; spelling the URLs out
 * means anyone editing a constant has to change this file and say why.
 */
class MaterialUrlGeneratorTest {

    private static final UUID MATERIAL_ID = fromString("4b0e3a9c-9d1e-4f2a-8a35-1c7c0a6d8e21");

    private final MaterialUrlGenerator materialUrlGenerator = new MaterialUrlGenerator();

    @Test
    void shouldBuildTheFileStreamUrlFromTheQueryApiBaseAndTheMaterialId() {
        assertThat(materialUrlGenerator.fileStreamUrlFor(MATERIAL_ID),
                is("http://localhost:8080/material-query-api/query/api/rest/material/material/4b0e3a9c-9d1e-4f2a-8a35-1c7c0a6d8e21"));
    }

    @Test
    void shouldAppendTheStreamAndPdfParametersToThePdfFileStreamUrl() {
        assertThat(materialUrlGenerator.pdfFileStreamUrlFor(MATERIAL_ID),
                is("http://localhost:8080/material-query-api/query/api/rest/material/material/4b0e3a9c-9d1e-4f2a-8a35-1c7c0a6d8e21?stream=true&requestPdf=true"));
    }

    @Test
    void shouldReturnThePdfUrlWhenPdfStreamIsRequested() {
        assertThat(materialUrlGenerator.fileStreamUrlFor(MATERIAL_ID, true),
                is(materialUrlGenerator.pdfFileStreamUrlFor(MATERIAL_ID)));
    }

    @Test
    void shouldReturnTheUrlWithoutPdfParametersWhenPdfStreamIsNotRequested() {
        assertThat(materialUrlGenerator.fileStreamUrlFor(MATERIAL_ID, false),
                is("http://localhost:8080/material-query-api/query/api/rest/material/material/4b0e3a9c-9d1e-4f2a-8a35-1c7c0a6d8e21"));
    }

    @Test
    void shouldNotRequestPdfWhenOnlyTheMaterialIdIsGiven() {
        assertThat(materialUrlGenerator.fileStreamUrlFor(MATERIAL_ID),
                is(materialUrlGenerator.fileStreamUrlFor(MATERIAL_ID, false)));
    }

    @Test
    void shouldPutTheMaterialIdIntoTheUrlSoTwoMaterialsNeverShareOne() {
        final UUID otherMaterialId = fromString("9f2b7c11-3d44-4e55-9a66-0b1c2d3e4f50");

        assertThat(materialUrlGenerator.fileStreamUrlFor(otherMaterialId),
                is("http://localhost:8080/material-query-api/query/api/rest/material/material/9f2b7c11-3d44-4e55-9a66-0b1c2d3e4f50"));
    }
}
