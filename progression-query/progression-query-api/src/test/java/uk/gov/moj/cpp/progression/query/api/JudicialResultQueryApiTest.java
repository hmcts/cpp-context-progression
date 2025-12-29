package uk.gov.moj.cpp.progression.query.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.JudicialResultQueryView;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JudicialResultQueryApiTest {

    @InjectMocks
    private JudicialResultQueryApi judicialResultQueryApi;

    @Mock
    private JudicialResultQueryView judicialResultQueryView;

    @Mock
    private JsonEnvelope expectedJsonEnvelope;


    @Test
    void shouldGetJudicialChildResults() {
        final JsonObject jsonObject = JsonObjects.createObjectBuilder()
                .add("hearingId", randomUUID().toString())
                .add("masterDefendantId", randomUUID().toString())
                .add("judicialResultTypeId", randomUUID().toString())
                .build();

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.judicial-child-results").build(),
                jsonObject);
        when(judicialResultQueryView.getJudicialChildResults(query)).thenReturn(expectedJsonEnvelope);

        final JsonEnvelope result = judicialResultQueryApi.getJudicialChildResults(query);

        assertThat(result, is(expectedJsonEnvelope));
    }

    @Test
    void shouldGetJudicialChildResultsV2() {
        final JsonObject jsonObject = JsonObjects.createObjectBuilder()
                .add("hearingId", randomUUID().toString())
                .add("masterDefendantId", randomUUID().toString())
                .add("judicialResultTypeId", randomUUID().toString())
                .build();

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.judicial-child-results-v2").build(),
                jsonObject);
        when(judicialResultQueryView.getJudicialChildResultsV2(query)).thenReturn(expectedJsonEnvelope);

        final JsonEnvelope result = judicialResultQueryApi.getJudicialChildResultsV2(query);

        assertThat(result, is(expectedJsonEnvelope));
    }
}
