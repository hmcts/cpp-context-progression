package uk.gov.moj.cpp.progression.query.view.service;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService.REFERENCEDATA_QUERY_RESULT_DEFINITIONS_BY_IDS;
import static uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService.REFERENCEDATA_GET_PROSECUTOR;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceTest {

    @Mock
    private Requester requester;

    @Mock
    private Enveloper enveloper;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonEnvelope queryEnvelope;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Mock
    private JsonObject responsePayload;

    @Captor
    private ArgumentCaptor<JsonEnvelope> requestJsonEnvelope;

    private String prosecutorId = UUID.randomUUID().toString();

    @Test
    public void shouldReturnProsecutorJson() {

        when(requester.requestAsAdmin(requestJsonEnvelope.capture())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(responsePayload);

        Optional<JsonObject> prosecutorJson = referenceDataService.getProsecutor(prosecutorId);

        assertThat(prosecutorJson.isPresent(), is(true));
        assertThat(requestJsonEnvelope.getValue().payloadAsJsonObject().getString("id"), is(prosecutorId));
        assertThat(requestJsonEnvelope.getValue().metadata().id(), notNullValue());
        assertThat(requestJsonEnvelope.getValue().metadata().name(), is(REFERENCEDATA_GET_PROSECUTOR));
    }

    @Test
    public void shouldNotReturnProsecutorJson() {

        when(requester.requestAsAdmin(requestJsonEnvelope.capture())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(null);

        Optional<JsonObject> prosecutorJson = referenceDataService.getProsecutor(prosecutorId);

        assertThat(prosecutorJson.isPresent(), is(false));
        assertThat(requestJsonEnvelope.getValue().payloadAsJsonObject().getString("id"), is(prosecutorId));
        assertThat(requestJsonEnvelope.getValue().metadata().id(), notNullValue());
        assertThat(requestJsonEnvelope.getValue().metadata().name(), is(REFERENCEDATA_GET_PROSECUTOR));
    }

    @Test
    void shouldGetHearingTypes() {
        when(requester.request(requestJsonEnvelope.capture())).thenReturn(jsonEnvelope);
        final JsonArray hearingTypes = createArrayBuilder()
                .add(createObjectBuilder().add("id", UUID.randomUUID().toString()).build())
                .build();
        when(queryEnvelope.metadata()).thenReturn(JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("name").build());
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("hearingTypes", hearingTypes)
                .build()
        );

        final JsonArray result = referenceDataService.getHearingTypes(queryEnvelope);

        assertThat(result, is(hearingTypes));
        assertThat(requestJsonEnvelope.getValue().metadata().name(), is("referencedata.query.hearing-types"));

    }

    @Test
    void shouldGetResultShortCodesForResultDefinitionIdsInOneRequest() {
        final UUID fo = UUID.randomUUID();
        final UUID fcost = UUID.randomUUID();
        final UUID noShortCode = UUID.randomUUID();
        final JsonEnvelope query = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());
        when(requester.requestAsAdmin(requestJsonEnvelope.capture())).thenReturn(JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("resultDefinitions", createArrayBuilder()
                        .add(createObjectBuilder().add("id", fo.toString()).add("shortCode", "FO"))
                        .add(createObjectBuilder().add("id", fcost.toString()).add("shortCode", "FCOST"))
                        .add(createObjectBuilder().add("id", noShortCode.toString())))
                        .build()));

        final Map<UUID, String> shortCodes = referenceDataService.getResultShortCodes(query, new LinkedHashSet<>(List.of(fo, fcost, noShortCode)));

        assertThat(shortCodes, is(Map.of(fo, "FO", fcost, "FCOST")));
        assertThat(requestJsonEnvelope.getValue().metadata().name(), is(REFERENCEDATA_QUERY_RESULT_DEFINITIONS_BY_IDS));
        assertThat(requestJsonEnvelope.getValue().payloadAsJsonObject().getString("ids"), is(fo + "," + fcost + "," + noShortCode));
    }

    @Test
    void shouldNotCallReferenceDataForNoResultDefinitionIds() {
        final JsonEnvelope query = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

        assertThat(referenceDataService.getResultShortCodes(query, Set.of()), is(Map.of()));
        verifyNoInteractions(requester);
    }
}
