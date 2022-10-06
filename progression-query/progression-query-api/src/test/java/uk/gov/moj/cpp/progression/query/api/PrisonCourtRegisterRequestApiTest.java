package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.query.PrisonCourtRegisterDocumentRequestQueryView;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PrisonCourtRegisterRequestApiTest {
    @Mock
    private PrisonCourtRegisterDocumentRequestQueryView prisonCourtRegisterDocumentRequestQueryView;

    @InjectMocks
    private PrisonCourtRegisterRequestApi prisonCourtRegisterRequestApi;

    @Test
    public void getPrisonCourtRegisterDocumentRequestByCourtCentre() {
        final JsonObjectBuilder prisonCourtRegisterDocumentPayload = Json.createObjectBuilder();
        final JsonEnvelope response = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.query.prison-court-register-document-by-court-centre"),
                prisonCourtRegisterDocumentPayload);
        prisonCourtRegisterRequestApi.getPrisonCourtRegisterDocumentRequestByCourtCentre(response);
        Mockito.verify(prisonCourtRegisterDocumentRequestQueryView).getPrisonCourtRegistersByCourtCentre(response);
    }
}
