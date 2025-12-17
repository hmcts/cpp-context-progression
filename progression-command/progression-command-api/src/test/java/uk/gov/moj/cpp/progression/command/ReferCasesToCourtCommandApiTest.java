package uk.gov.moj.cpp.progression.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferCasesToCourtCommandApiTest {

    private static final ObjectToJsonObjectConverter objectToJsonConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope commandEnvelope;

    @InjectMocks
    private ReferCasesToCourtCommandApi referCasesToCourtCommandApi;

    @Mock
    private RefDataService referenceDataService;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());


    @Test
    public void shouldAddDefendant() {
        final JsonObject referCasesToCourtJsonObject = CommandClientTestBase.readJson("json/progression.refer-cases-to-court.json", JsonObject.class);
        final JsonObject prosecutorFromReferenceData = CommandClientTestBase.readJson("json/cps_prosecutor_from_reference_data.json", JsonObject.class);

        when(commandEnvelope.payloadAsJsonObject()).thenReturn(referCasesToCourtJsonObject);
        when(commandEnvelope.metadata()).thenReturn(CommandClientTestBase.metadataFor("progression.refer-cases-to-court", UUID.randomUUID().toString()));
        when(referenceDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(prosecutorFromReferenceData));

        referCasesToCourtCommandApi.handle(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.refer-cases-to-court"));
        final JsonObject expectedReferCasesToCourtJsonObject = CommandClientTestBase.readJson("json/progression.refer-cases-to-court-expected.json", JsonObject.class);
    }
}
