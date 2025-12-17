package uk.gov.moj.cpp.progression.query.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.QueryClientTestBase.metadataFor;
import static uk.gov.QueryClientTestBase.readJson;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.util.Optional;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefenceServiceTest {
    @Mock
    private Requester requester;

    @InjectMocks
    private DefenceService defenceService;

    @Mock
    ReferenceDataService referenceDataService;

    @Spy
    Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Test
    public void shouldGetIdpcDetailsForDefendant(){
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withName("progression.query.trial-readiness-details")
                .withId(randomUUID());

        final JsonEnvelope jsonEnvelope = JsonEnvelopeBuilder.envelope()
                .with(metadataBuilder)
                .build();

        final JsonObject jsonObjectPayload = readJson("json/defendant.idpc.metadata.json", JsonObject.class);
        final Metadata metadata = metadataFor("defence.query.defendant-idpc-metadata", randomUUID());
        final Envelope envelope = envelopeFrom(metadata, jsonObjectPayload);

        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final Optional<JsonObject> jsonObject = defenceService.getIdpcDetailsForDefendant(requester,jsonEnvelope,randomUUID().toString());
        assertThat(jsonObject.get(), is(notNullValue()));
    }
}
