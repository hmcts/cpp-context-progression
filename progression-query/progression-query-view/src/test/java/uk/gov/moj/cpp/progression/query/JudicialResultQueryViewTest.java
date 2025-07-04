package uk.gov.moj.cpp.progression.query;


import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.io.IOException;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JudicialResultQueryViewTest {

    @InjectMocks
    private JudicialResultQueryView judicialResultQueryView;

    @Mock
    private HearingRepository hearingRepository;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    void shouldGetJudicialChildResults() throws IOException {
        final UUID hearingId = randomUUID();
        final String masterDefendantId = "e438510e-55a8-4e2e-a663-bc8ada09247f";
        final String judicialResultTypeId = "418b3aa7-65ab-4a4a-bab9-2f96b698118c";
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(getPayload("hearing-payload-for-prosecutionCase-child-judicial-results.json"));

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("masterDefendantId", masterDefendantId)
                .add("judicialResultTypeId", judicialResultTypeId)
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.judicial-child-results").build(),
                jsonObject);

        final JsonEnvelope response = judicialResultQueryView.getJudicialChildResults(jsonEnvelope);

        final JsonArray result = response.payloadAsJsonObject().getJsonArray("judicialChildResults");

        assertThat(result.size(), is(1));
        assertThat(result.get(0).asJsonObject().getString("label"), is("Unpaid work. Requirement to be completed within 12 months."));
        assertThat(result.get(0).asJsonObject().getString("judicialResultId"), is("8bcfbd2e-e17e-449e-a003-2c2f698a5fd4"));
        assertThat(result.get(0).asJsonObject().getString("judicialResultTypeId"), is("9bec5977-1796-4645-9b9e-687d4f23d37d"));
    }

    private String getPayload(final String resourceName) throws IOException {
        return Resources.toString(Resources.getResource(resourceName), defaultCharset());
    }

}
