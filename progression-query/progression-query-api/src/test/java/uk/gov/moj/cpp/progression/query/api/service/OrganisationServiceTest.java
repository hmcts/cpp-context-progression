package uk.gov.moj.cpp.progression.query.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.QueryClientTestBase.metadataFor;
import static uk.gov.QueryClientTestBase.readJson;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.query.api.service.OrganisationService.DEFENCE_ASSOCIATED_CASE_DEFENDANTS_ORGANISATION_QUERY;
import static uk.gov.moj.cpp.progression.query.api.service.OrganisationService.DEFENCE_ASSOCIATION_QUERY;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OrganisationServiceTest {
    public static final String JSON_ASSOCIATED_ORGANISATION_JSON = "json/associatedOrganisation.json";
    public static final String JSON_ASSOCIATED_CASE_DEFENDANTS_ORGANISATION_JSON = "json/associatedCaseDefendantsOrganisation.json";

    @InjectMocks
    private OrganisationService organisationService;

    @Mock
    private Requester requester;
    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Test
    public void shouldReturnOrganisationDetails() {

        final JsonObject jsonObjectPayload = readJson(JSON_ASSOCIATED_ORGANISATION_JSON, JsonObject.class);
        final Metadata metadata = metadataFor(DEFENCE_ASSOCIATION_QUERY, randomUUID());
        final Envelope envelope = envelopeFrom(metadata, jsonObjectPayload);

        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final JsonObject associatedOrganisation = organisationService.getAssociatedOrganisation(envelope, randomUUID().toString(), requester);
        assertThat(associatedOrganisation.getString("organisationId"), is("2fc69990-bf59-4c4a-9489-d766b9abde9a"));
    }

    @Test
    public void shouldReturnEmptyOrganisationDetails() {
        final JsonObject jsonObjectPayload = Json.createObjectBuilder()
                .add("association", Json.createObjectBuilder())
                .build();
        final Metadata metadata = metadataFor(DEFENCE_ASSOCIATION_QUERY, randomUUID());
        final Envelope envelope = envelopeFrom(metadata, jsonObjectPayload);

        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final JsonObject associatedOrganisation = organisationService.getAssociatedOrganisation(envelope, randomUUID().toString(), requester);
        assertThat(associatedOrganisation.getString("organisationId", null), nullValue());
    }

    @Test
    public void shouldReturnAssociatedCaseDefendants() {
        final JsonObject jsonObjectPayload = readJson(JSON_ASSOCIATED_CASE_DEFENDANTS_ORGANISATION_JSON, JsonObject.class);
        final Metadata metadata = metadataFor(DEFENCE_ASSOCIATED_CASE_DEFENDANTS_ORGANISATION_QUERY, randomUUID());
        final Envelope envelope = envelopeFrom(metadata, jsonObjectPayload);

        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final JsonObject caseDefendantsWithOrganisationAddress = organisationService.getAssociatedCaseDefendantsWithOrganisationAddress(envelope, randomUUID().toString(), requester);

        assertThat(caseDefendantsWithOrganisationAddress.getString("caseId"), is("fcb1edc9-786a-462d-9400-318c95c7b700"));
        assertThat(caseDefendantsWithOrganisationAddress.getJsonArray("defendants").size(), is(2));
    }

    @Test
    public void shouldRequestCaseDefendantsWithOrganisationAddressWithAddressParamSetToTrue() {
        final JsonObject jsonObjectPayload = readJson(JSON_ASSOCIATED_CASE_DEFENDANTS_ORGANISATION_JSON, JsonObject.class);
        final Metadata metadata = metadataFor(DEFENCE_ASSOCIATED_CASE_DEFENDANTS_ORGANISATION_QUERY, randomUUID());
        final Envelope envelope = envelopeFrom(metadata, jsonObjectPayload);

        when(requester.requestAsAdmin(envelopeCaptor.capture(), any())).thenReturn(envelope);

        final String caseId = "fcb1edc9-786a-462d-9400-318c95c7b700";
        final JsonObject caseDefendantsWithOrganisationAddress = organisationService.getAssociatedCaseDefendantsWithOrganisationAddress(envelope, caseId, requester);

        assertThat(caseDefendantsWithOrganisationAddress.getString("caseId"), is(caseId));
        assertThat(envelopeCaptor.getValue().metadata().name(), is(DEFENCE_ASSOCIATED_CASE_DEFENDANTS_ORGANISATION_QUERY));
        assertThat(envelopeCaptor.getValue().payload().getString("caseId"), is(caseId));
        assertThat(envelopeCaptor.getValue().payload().getBoolean("withAddress"), is(true));
    }

}