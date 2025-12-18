package uk.gov.moj.cpp.progression.command.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.command.CommandClientTestBase;

import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OrganisationServiceTest {

    private static final String JSON_ASSOCIATED_ORGANISATION_JSON = "json/associatedOrganisation.json";
    private static final String DEFENCE_ASSOCIATION_QUERY = "defence.query.associated-organisation";
    private static final String DEFENCE_ASSOCIATED_DEFENDANTS_QUERY = "defence.query.get-associated-defendants";

    @InjectMocks
    private OrganisationService organisationService;

    @Mock
    private Requester requester;

    @Test
    public void shouldReturnOrganisationDetails() {


        final JsonObject jsonObjectPayload = CommandClientTestBase.readJson(JSON_ASSOCIATED_ORGANISATION_JSON, JsonObject.class);
        final Metadata metadata = CommandClientTestBase.metadataFor(DEFENCE_ASSOCIATION_QUERY, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);

        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final JsonObject associatedOrganisation = organisationService.getAssociatedOrganisation(envelope, randomUUID().toString(), requester);
        assertThat(associatedOrganisation.getString("organisationId"), is("2fc69990-bf59-4c4a-9489-d766b9abde9a"));

    }

    @Test
    public void shouldReturnEmptyOrganisationDetails() {
        final JsonObject jsonObjectPayload = JsonObjects.createObjectBuilder()
                .add("association", JsonObjects.createObjectBuilder())
                .build();
        final Metadata metadata = CommandClientTestBase.metadataFor(DEFENCE_ASSOCIATION_QUERY, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);

        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final JsonObject associatedOrganisation = organisationService.getAssociatedOrganisation(envelope, randomUUID().toString(), requester);
        assertThat(associatedOrganisation.getString("organisationId", null), nullValue());
    }

    @Test
    public void shouldReturnDefendantIdsWhenDefenceIsAssociatedWithDefendants() {

        final JsonObject jsonObjectPayload = JsonObjects.createObjectBuilder()
                .add("defendantIds", JsonObjects.createArrayBuilder()
                        .add(randomUUID().toString())
                        .add(randomUUID().toString())
                ).build();
        final Metadata metadata = CommandClientTestBase.metadataFor(DEFENCE_ASSOCIATED_DEFENDANTS_QUERY, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);

        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final List<UUID> associatedDefendants = organisationService.getAssociatedDefendants(envelope, requester);
        assertThat(associatedDefendants.size(), is(2));

    }

    @Test
    public void shouldReturnEmptyDefendantIdsWhenDefenceIsNotAssociatedWithDefendants() {

        final JsonObject jsonObjectPayload = JsonObjects.createObjectBuilder()
                .add("defendantIds", JsonObjects.createArrayBuilder()).build();
        final Metadata metadata = CommandClientTestBase.metadataFor(DEFENCE_ASSOCIATED_DEFENDANTS_QUERY, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);

        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final List<UUID> associatedDefendants = organisationService.getAssociatedDefendants(envelope, requester);
        assertThat(associatedDefendants.size(), is(0));

    }

}
