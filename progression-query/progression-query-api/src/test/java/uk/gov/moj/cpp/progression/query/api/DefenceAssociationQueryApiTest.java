package uk.gov.moj.cpp.progression.query.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.api.resource.service.UsersAndGroupsService;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.time.ZonedDateTime;
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
    @Mock
    private UsersAndGroupsService usersAndGroupsService;
    @InjectMocks
    private DefenceAssociationQueryApi defenceAssociationQueryApi;

    @Test
    public void shouldReturnAssociatedOrganisationDetails() {

        //Given
        final UUID userId = randomUUID();
        final UUID organisationId = randomUUID();
        final UUID defendantId = randomUUID();
        final String organisationName = "TEST_ORG";

        final MetadataBuilder metadataBuilder = stubbedMetadataBuilder(userId);
        final JsonEnvelope requestEnvelopeForApiView = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(defendantId.toString(), "defendantId").build();
        final JsonEnvelope responseEnvelopeForApiView = JsonEnvelope.envelopeFrom(metadataBuilder, stubbedDefenceAssociationDataToReturnFromPersistedData(organisationId.toString()));

        when(requester.request(requestEnvelopeForApiView)).thenReturn(responseEnvelopeForApiView);
        when(usersAndGroupsService.getOrganisationDetails(any()))
                .thenReturn(stubbedDefenceAssociationDataReturnedFromUsersAndGroupService(organisationId.toString(), organisationName));

        //When
        final JsonEnvelope associatedOrganizationResponse = defenceAssociationQueryApi.getAssociatedOrganisation(requestEnvelopeForApiView);

        //Then
        final JsonObject association = associatedOrganizationResponse.payloadAsJsonObject().getJsonObject("association");
        assertThat(getValue(association, "organisationId"), equalTo(organisationId.toString()));
        assertThat(getValue(association, "organisationName"), equalTo(organisationName));
        assertThat(getValue(association, "status"), equalTo("Active Barrister/Solicitor of record"));
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

    private MetadataBuilder stubbedMetadataBuilder(final UUID userId) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("progression.query.associated-organisation")
                .withCausation(randomUUID())
                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withUserId(userId.toString());
    }

    private JsonObject stubbedDefenceAssociationDataToReturnFromPersistedData(final String organisationId) {
        return Json.createObjectBuilder()
                .add("association", Json.createObjectBuilder()
                        .add("organisationId", organisationId)
                        .add("status", "Active Barrister/Solicitor of record")
                        .add("startDate", ZonedDateTime.now().toString())
                        .add("representationType", "PRO_BONO")
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
