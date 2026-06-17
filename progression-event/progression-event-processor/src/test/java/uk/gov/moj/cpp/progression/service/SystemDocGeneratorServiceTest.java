package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.equalToObject;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Map;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemDocGeneratorServiceTest {

    public static final String PRISON_COURT_REGISTER_ID = "prisonCourtRegisterId";
    public static final String ADDITIONAL_INFORMATION = "additionalInformation";
    @Mock
    private Sender sender;
    @InjectMocks
    private SystemDocGeneratorService systemDocGenerator;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> argumentCaptor;

    @Test
    public void shouldTestGenerateDocument() {
        final String sourceCorrelationId = randomUUID().toString();
        final String prisonCourtRegisterId = randomUUID().toString();
        final UUID payloadFileServiceId = randomUUID();
        final Map<String,String> additionInfoMap = ImmutableMap.of(PRISON_COURT_REGISTER_ID, prisonCourtRegisterId.toString());
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                "test",
                "PRISON_COURT_REGISTER",
                ConversionFormat.PDF,
                sourceCorrelationId,
                payloadFileServiceId,
                additionInfoMap
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
        assertThat(actual.payload().getJsonArray(ADDITIONAL_INFORMATION), notNullValue());
        assertThat(actual.payload().getJsonArray(ADDITIONAL_INFORMATION), hasSize(1));
        JsonArray additionalInfo = actual.payload().getJsonArray(ADDITIONAL_INFORMATION);
        assertThat(additionalInfo.get(0).toString(), containsString(prisonCourtRegisterId.toString()));
        assertThat(additionalInfo.get(0).toString(), containsString(PRISON_COURT_REGISTER_ID));
    }

    private JsonEnvelope envelope() {
        return envelopeFrom(metadataWithRandomUUID("test envelope"), createObjectBuilder().build());
    }
}