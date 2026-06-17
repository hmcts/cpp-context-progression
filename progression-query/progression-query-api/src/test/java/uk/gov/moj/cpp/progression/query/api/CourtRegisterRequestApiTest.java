package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.query.CourtRegisterDocumentRequestQueryView;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
public class CourtRegisterRequestApiTest {

    @Mock
    private CourtRegisterDocumentRequestQueryView courtRegisterDocumentRequestQueryView;

    @InjectMocks
    private CourtRegisterRequestApi courtRegisterRequestApi;

    @Test
    public void getCourtRegisterDocumentRequest() {
        final JsonObjectBuilder courtDocumentPayload = JsonObjects.createObjectBuilder();
        final JsonEnvelope response = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.query.court-register-document-request"),
                courtDocumentPayload);
        courtRegisterRequestApi.getCourtRegisterDocumentRequest(response);
        Mockito.verify(courtRegisterDocumentRequestQueryView).getCourtRegisterRequests(response);
    }

    @Test
    public void getCourtRegisterDocumentRequestByMaterial() {
        final JsonObjectBuilder courtDocumentPayload = JsonObjects.createObjectBuilder();
        final JsonEnvelope response = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.query.court-register-document-by-material"),
                courtDocumentPayload);
        courtRegisterRequestApi.getCourtRegisterDocumentRequestByMaterial(response);
        Mockito.verify(courtRegisterDocumentRequestQueryView).getCourtRegisterByMaterial(response);

    }

    @Test
    public void getCourtRegisterDocumentRequestByDate() {
        final JsonObjectBuilder courtDocumentPayload = JsonObjects.createObjectBuilder();
        final JsonEnvelope response = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.query.court-register-document-by-request-date"),
                courtDocumentPayload);
        courtRegisterRequestApi.getCourtRegisterDocumentByRequestDate(response);
        Mockito.verify(courtRegisterDocumentRequestQueryView).getCourtRegistersByRequestDate(response);

    }
}