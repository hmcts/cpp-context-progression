package uk.gov.moj.cpp.progression.query.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.api.resource.service.UsersAndGroupsService;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.Date;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefenceAssociationQueryApiTest {

    @Mock
    private JsonEnvelope query;
    @Mock
    private JsonEnvelope response;
    @Mock
    private Requester requester;
    @InjectMocks
    private DefenceAssociationQueryApi defenceAssociationQueryApi;
    @Mock
    private UsersAndGroupsService usersAndGroupsService;

    @Test
    public void shouldReturnAssociatedOrganisationDetails() {

        //Given
        final UUID userId = randomUUID();
        final UUID organisationId = randomUUID();
        final String organisationName = "TEST_ORG";
        when(requester.request(query))
                .thenReturn(stubbedDefenceAssociationDataPersistedAfterOrganisationAssociation(userId.toString(), organisationId.toString()));
        when(usersAndGroupsService.getOrganisationDetailsForUser(query))
                .thenReturn(stubbedDefenceAssociationDataReturnedFromUsersAndGroupService(organisationId.toString(), organisationName));

        //When
        final JsonEnvelope associatedOrganizationResponse = defenceAssociationQueryApi.getAssociatedOrganisation(query);

        //Then
        final JsonObject association = associatedOrganizationResponse.payloadAsJsonObject().getJsonObject("association");
        assertThat(getValue(association, "organisationId"), equalTo(organisationId.toString()));
        assertThat(getValue(association, "organisationName"), equalTo(organisationName));
        assertThat(getValue(association, "status"), equalTo("ASSOCIATED"));
    }

    @Test
    public void shouldReturnEmptyOrganisationDetailsWhenNoOrganisationAssociated() {

        //Given
        final UUID userId = randomUUID();
        when(requester.request(query)).thenReturn(emptyOrganisationDetails(userId));

        //When
        final JsonEnvelope associatedOrganizationResponse = defenceAssociationQueryApi.getAssociatedOrganisation(query);

        //Then
        final JsonObject association = associatedOrganizationResponse.payloadAsJsonObject().getJsonObject("association");
        assertThat(association.toString(), equalTo("{}"));
    }


    @Test
    public void shouldReturnEmptyOrganisationDetailsWhenOrganisationIdReceivedFromUsersAndGroupIsDifferent() {

        //Given
        final UUID userId = randomUUID();
        final UUID organisationId = randomUUID();
        final String organisationName = "TEST_ORG";
        when(requester.request(query))
                .thenReturn(stubbedDefenceAssociationDataPersistedAfterOrganisationAssociation(userId.toString(), organisationId.toString()));
        when(usersAndGroupsService.getOrganisationDetailsForUser(query))
                .thenReturn(stubbedDefenceAssociationDataReturnedFromUsersAndGroupService(randomUUID().toString(), organisationName));

        //When
        final JsonEnvelope associatedOrganizationResponse = defenceAssociationQueryApi.getAssociatedOrganisation(query);

        //Then
        final JsonObject association = associatedOrganizationResponse.payloadAsJsonObject().getJsonObject("association");
        assertThat(association.toString(), equalTo("{}"));
    }

    private JsonEnvelope emptyOrganisationDetails(final UUID userId) {
        return JsonEnvelope.envelopeFrom(
                stubbedMetadataBuilder(userId),
                Json.createObjectBuilder()
                        .add("association", Json.createObjectBuilder())
                        .build());
    }

    private String getValue(final JsonObject associationsJsonObject, final String key) {
        return associationsJsonObject.getString(key);
    }

    private JsonEnvelope stubbedDefenceAssociationDataPersistedAfterOrganisationAssociation(final String userId, final String organisationId) {
        return JsonEnvelope.envelopeFrom(
                stubbedMetadataBuilder(UUID.fromString(userId)),
                stubbedDefenceAssociationDataToReturnFromPersistedData(organisationId));
    }

    private MetadataBuilder stubbedMetadataBuilder(final UUID userId) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("usersgroups.get-organisation-details-for-user")
                .withCausation(randomUUID())
                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withUserId(userId.toString());
    }

    private JsonObject stubbedDefenceAssociationDataToReturnFromPersistedData(final String organisationId) {
        return Json.createObjectBuilder()
                .add("association", Json.createObjectBuilder()
                        .add("organisationId", organisationId)
                        .add("status", "ASSOCIATED")
                        .add("startDate", new Date().toString())
                )
                .build();
    }

    private JsonObject stubbedDefenceAssociationDataReturnedFromUsersAndGroupService(final String organisationId, final String organisationName) {
        return Json.createObjectBuilder()
                .add("organisationId", organisationId)
                .add("organisationName", organisationName)
                .add("addressLine1", "add line 1")
                .add("addressLine4", "add line 4")
                .add("addressPostcode", "CR01XG")
                .add("phoneNumber", "1234567890")
                .add("email", "moj@email.com")
                .build();
    }

}
