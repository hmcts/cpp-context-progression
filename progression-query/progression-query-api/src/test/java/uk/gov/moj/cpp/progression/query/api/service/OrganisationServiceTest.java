package uk.gov.moj.cpp.progression.query.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import uk.gov.QueryClientTestBase;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OrganisationServiceTest {
    public static final String JSON_ASSOCIATED_ORGANISATION_JSON = "json/associatedOrganisation.json";
    private static final String DEFENCE_ASSOCIATION_QUERY = "defence.query.associated-organisation";


    @InjectMocks
    private OrganisationService organisationService;

    @Mock
    private Requester requester;

    @Test
    public void shouldReturnOrganisationDetails() {


        final JsonObject jsonObjectPayload = QueryClientTestBase.readJson(JSON_ASSOCIATED_ORGANISATION_JSON, JsonObject.class);
        final Metadata metadata = QueryClientTestBase.metadataFor(DEFENCE_ASSOCIATION_QUERY, randomUUID());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);

        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final JsonObject associatedOrganisation = organisationService.getAssociatedOrganisation(envelope, randomUUID().toString(), requester);
        assertThat(associatedOrganisation.getString("organisationId"), is("2fc69990-bf59-4c4a-9489-d766b9abde9a"));

    }

    @Test
    public void shouldReturnEmptyOrganisationDetails() {
        final JsonObject jsonObjectPayload = Json.createObjectBuilder()
                .add("association", Json.createObjectBuilder())
                .build();
        final Metadata metadata = QueryClientTestBase.metadataFor(DEFENCE_ASSOCIATION_QUERY, randomUUID());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);

        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final JsonObject associatedOrganisation = organisationService.getAssociatedOrganisation(envelope, randomUUID().toString(), requester);
        assertThat(associatedOrganisation.getString("organisationId", null), nullValue());
    }
}