package uk.gov.moj.cpp.progression.query.api;

import static org.junit.Assert.*;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtRegisterRequestApiTest {

    @Mock
    private Requester requester;

    @InjectMocks
    private CourtRegisterRequestApi courtRegisterRequestApi;

    @Test
    public void getCourtRegisterDocumentRequest() {
        final JsonObjectBuilder courtDocumentPayload = Json.createObjectBuilder();
        final JsonEnvelope response = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.query.court-register-document-request"),
                courtDocumentPayload);
        courtRegisterRequestApi.getCourtRegisterDocumentRequest(response);
        Mockito.verify(requester).request(response);
    }

    @Test
    public void getCourtRegisterDocumentRequestByMaterial() {
        final JsonObjectBuilder courtDocumentPayload = Json.createObjectBuilder();
        final JsonEnvelope response = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.query.court-register-document-by-material"),
                courtDocumentPayload);
        courtRegisterRequestApi.getCourtRegisterDocumentRequestByMaterial(response);
        Mockito.verify(requester).request(response);

    }

    @Test
    public void getCourtRegisterDocumentRequestByDate() {
        final JsonObjectBuilder courtDocumentPayload = Json.createObjectBuilder();
        final JsonEnvelope response = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.query.court-register-document-by-request-date"),
                courtDocumentPayload);
        courtRegisterRequestApi.getCourtRegisterDocumentByRequestDate(response);
        Mockito.verify(requester).request(response);

    }
}