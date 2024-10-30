package uk.gov.moj.cpp.progression.query.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.QueryClientTestBase.readJson;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialServiceTest {

    @InjectMocks
    private MaterialService materialService;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope materialresponse;

    @Test
    public void shouldGetPet(){
        final JsonEnvelope jsonEnvelope = JsonEnvelopeBuilder.envelope()
                .with(metadataBuilder()
                        .withName("progression.query.pet-for-defendant")
                        .withId(randomUUID()))
                .build();

        final JsonObject jsonObjectPayload = readJson("json/material.query.structured-form.json", JsonObject.class);

        when(requester.request(any())).thenReturn(materialresponse);
        when(materialresponse.payloadAsJsonObject()).thenReturn(jsonObjectPayload);

        final JsonObject jsonObject1 = materialService.getPet(requester,jsonEnvelope,randomUUID().toString());
        assertThat(jsonObject1, is(notNullValue()));
    }
}
