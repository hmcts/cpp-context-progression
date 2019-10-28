package uk.gov.moj.cpp.progression.transformer;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildCourtDocument;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildJsonEnvelope;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.ReferredCourtDocument;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferredCourtDocumentTransformerTest {

    public static final String CASE_DOCUMENT = "CaseDocument";
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Mock
    private ReferenceDataService referenceDataService;
    @InjectMocks
    private ReferredCourtDocumentTransformer referredCourtDocumentTransformer;

    @Test
    public void testTransform() {
        // Setup
        UUID documentTypeId = randomUUID();
        final ReferredCourtDocument referredCourtDocument = buildCourtDocument(documentTypeId);
        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        JsonObject jsonObject = Json.createObjectBuilder().add("documentType", CASE_DOCUMENT).build();
        when(referenceDataService.getDocumentTypeData(documentTypeId, jsonEnvelope))
                .thenReturn(Optional.of(jsonObject));

        // Run the test
        final CourtDocument result = referredCourtDocumentTransformer.transform
                (referredCourtDocument, jsonEnvelope);

        // Verify the results
        assertThat(documentTypeId, is(result.getDocumentTypeId()));
        assertThat(CASE_DOCUMENT, is(result.getDocumentTypeDescription()));
        assertTrue(result.getContainsFinancialMeans());
    }

    @Test
    public void shouldThrowException() {
        expectedException.expect(ReferenceDataNotFoundException.class);
        // Setup
        UUID documentTypeId = randomUUID();
        final ReferredCourtDocument referredCourtDocument = buildCourtDocument(documentTypeId);
        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        when(referenceDataService.getDocumentTypeData(documentTypeId, jsonEnvelope))
                .thenThrow(new ReferenceDataNotFoundException("", ""));

        // Run the test
        referredCourtDocumentTransformer.transform
                (referredCourtDocument, jsonEnvelope);

        verifyNoMoreInteractions(referenceDataService);
    }


}
