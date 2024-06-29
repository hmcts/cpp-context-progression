package uk.gov.justice.api.resource.service;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.List;
import java.util.UUID;

import javax.json.JsonValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefenceQueryServiceTest {
    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private Requester requester;

    @InjectMocks
    private DefenceQueryService defenceQueryService;

    private String caseId = UUID.randomUUID().toString();

    @Mock
    private JsonEnvelope responseJsonEnvelope;

    @Before
    public void setup() {
        when(jsonEnvelope.metadata()).thenReturn(getMetadataBuilder(UUID.randomUUID()).build());
    }

    @Test
    public void shouldReturnFalseWhenQueryResponseIsNull() {
        when(requester.request(any())).thenReturn(null);

        assertThat(defenceQueryService.isUserProsecutingCase(jsonEnvelope, caseId), is(false));
        assertThat(defenceQueryService.isUserProsecutingOrDefendingCase(jsonEnvelope, caseId), is(false));
    }

    @Test
    public void shouldReturnFalseWhenResponseNotHasRoleDetails() {
        final JsonEnvelope response = envelopeFrom(getMetadataBuilder(UUID.randomUUID()).build(), createObjectBuilder().build());
        when(requester.request(any())).thenReturn(response);

        assertThat(defenceQueryService.isUserProsecutingCase(jsonEnvelope, caseId), is(false));
        assertThat(defenceQueryService.isUserProsecutingOrDefendingCase(jsonEnvelope, caseId), is(false));
    }

    @Test
    public void shouldReturnFalseWhenResponseNotHasRoleDetailsAndResponseIsNull() {
        final JsonEnvelope response = envelopeFrom(getMetadataBuilder(UUID.randomUUID()).build(), JsonValue.NULL);
        when(requester.request(any())).thenReturn(response);

        assertThat(defenceQueryService.isUserProsecutingCase(jsonEnvelope, caseId), is(false));
        assertThat(defenceQueryService.isUserProsecutingOrDefendingCase(jsonEnvelope, caseId), is(false));
    }

    @Test
    public void shouldReturnBooleanWhenRoleNotProsecution() {
        final JsonEnvelope response = envelopeFrom(getMetadataBuilder(UUID.randomUUID()).build(), createObjectBuilder().add("isAdvocateDefendingOrProsecuting", "defending").build());
        when(requester.request(any())).thenReturn(response);

        assertThat(defenceQueryService.isUserProsecutingCase(jsonEnvelope, caseId), is(false));
        assertThat(defenceQueryService.isUserProsecutingOrDefendingCase(jsonEnvelope, caseId), is(true));
    }

    @Test
    public void shouldReturnListOfDefendantIds() {
        final UUID defId1 = randomUUID();
        final UUID defId2 = randomUUID();
        final JsonEnvelope response = envelopeFrom(getMetadataBuilder(UUID.randomUUID()).build(), createObjectBuilder()
                .add("authorizedDefendantIds", createArrayBuilder()
                        .add(defId1.toString())
                        .add(defId2.toString()).build()).build());
        when(requester.request(any())).thenReturn(response);


        final List<UUID> defendantList = defenceQueryService.getDefendantList(jsonEnvelope, caseId);

        assertThat(defendantList.size(), is(2));
        assertThat(defendantList.get(0), is(defId1));
        assertThat(defendantList.get(1), is(defId2));
    }

    @Test
    public void shouldReturnEmptyListWhenGetDefendantListHasNoRoleDetails() {
        final JsonEnvelope response = envelopeFrom(getMetadataBuilder(UUID.randomUUID()).build(), createObjectBuilder().build());
        when(requester.request(any())).thenReturn(response);

        assertThat(defenceQueryService.getDefendantList(jsonEnvelope, caseId), is(emptyList()));
    }

    @Test
    public void shouldReturnTrueWhenRoleInProsecution() {
        final JsonEnvelope response = envelopeFrom(getMetadataBuilder(UUID.randomUUID()).build(), createObjectBuilder().add("isAdvocateDefendingOrProsecuting", "prosecuting").build());
        when(requester.request(any())).thenReturn(response);


        assertThat(defenceQueryService.isUserProsecutingCase(jsonEnvelope, caseId), is(true));
        assertThat(defenceQueryService.isUserProsecutingOrDefendingCase(jsonEnvelope, caseId), is(true));
    }

    @Test
    public void shouldReturnTrueWhenRoleInBothProsecutionAndDefence() {
        final JsonEnvelope response = envelopeFrom(getMetadataBuilder(UUID.randomUUID()).build(), createObjectBuilder().add("isAdvocateDefendingOrProsecuting", "both").build());
        when(requester.request(any())).thenReturn(response);

        assertThat(defenceQueryService.isUserProsecutingCase(jsonEnvelope, caseId), is(true));
        assertThat(defenceQueryService.isUserProsecutingOrDefendingCase(jsonEnvelope, caseId), is(true));
    }

    private MetadataBuilder getMetadataBuilder(final UUID userId) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("progression.query.courtdocuments.for.prosecution")
                .withCausation(randomUUID())
                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withUserId(userId.toString());
    }
}