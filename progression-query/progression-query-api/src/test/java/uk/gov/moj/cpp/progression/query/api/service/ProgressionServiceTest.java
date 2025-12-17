package uk.gov.moj.cpp.progression.query.api.service;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.progression.query.PetQueryView;

import java.io.IOException;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProgressionServiceTest {

    @InjectMocks
    private ProgressionService progressionService;

    @Mock
    private PetQueryView petQueryView;

    @Mock
    private JsonEnvelope envelope;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Test
    public void shouldGetPetsForCase(){
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withName("progression.query.trial-readiness-details")
                .withId(randomUUID());

        final JsonObject petsForCasePayload = stringToJsonObjectConverter.convert(getPayload("json/progression.query.pets-for-case-one-defendant-only.json")
                .replaceAll("PET_ID", randomUUID().toString())
                .replaceAll("DEFENDANT_ID", randomUUID().toString())
                .replaceAll("CASE_ID", randomUUID().toString())
                .replaceAll("OFFENCE_ID1", randomUUID().toString())
                .replaceAll("OFFENCE_ID2", randomUUID().toString()));

        final JsonEnvelope jsonEnvelope = JsonEnvelopeBuilder.envelope()
                .with(metadataBuilder)
                .withPayloadFrom(petsForCasePayload)
                .build();

        when(petQueryView.getPetsForCase(any())).thenReturn(jsonEnvelope);
        final JsonObject jsonObject =  progressionService.getPetsForCase(petQueryView, jsonEnvelope, randomUUID().toString());
        assertThat(jsonObject, is(notNullValue()));
    }

    public static String getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), defaultCharset());
        } catch (final IOException e) {
            fail("Error consuming file from location " + path);
        }
        return request;
    }
}
