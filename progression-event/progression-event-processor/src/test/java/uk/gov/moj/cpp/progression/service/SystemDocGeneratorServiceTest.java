package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SystemDocGeneratorServiceTest {

    @Mock
    private Sender sender;
    @InjectMocks
    private SystemDocGeneratorService systemDocGenerator;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> argumentCaptor;

    @Test
    public void shouldTestGenerateDocument() {
        final String sourceCorrelationId = randomUUID().toString();
        final UUID payloadFileServiceId = randomUUID();
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                "test",
                "PRISON_COURT_REGISTER",
                ConversionFormat.PDF,
                sourceCorrelationId,
                payloadFileServiceId
        );

        systemDocGenerator.generateDocument(request, envelope());

        verify(sender).sendAsAdmin(argumentCaptor.capture());
        final Envelope<JsonObject> actual = argumentCaptor.getValue();
        assertThat(actual.metadata().name(), equalTo("systemdocgenerator.generate-document"));
        assertThat(actual.payload().getString("originatingSource"), equalTo("test"));
        assertThat(actual.payload().getString("templateIdentifier"), equalTo("PRISON_COURT_REGISTER"));
        assertThat(actual.payload().getString("conversionFormat"), equalTo("pdf"));
        assertThat(actual.payload().getString("sourceCorrelationId"), equalTo(sourceCorrelationId));
        assertThat(actual.payload().getString("payloadFileServiceId"), equalTo(payloadFileServiceId.toString()));
    }

    private JsonEnvelope envelope() {
        return envelopeFrom(metadataWithRandomUUID("test envelope"), createObjectBuilder().build());
    }
}