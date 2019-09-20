package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.api.resource.service.UsersAndGroupsService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

@ServiceComponent(Component.QUERY_API)
public class DefenceAssociationQueryApi {

    public static final String ASSOCIATION = "association";
    public static final String ORGANISATION_ID = "organisationId";
    public static final String STATUS = "status";
    public static final String ASSOCIATION_DATE = "associationDate";
    public static final String ORGANISATION_NAME = "organisationName";
    public static final String ADDRESS = "address";
    public static final String ADDRESS_LINE_1 = "addressLine1";
    public static final String ADDRESS_LINE_2 = "addressLine2";
    public static final String ADDRESS_LINE_3 = "addressLine3";
    public static final String ADDRESS_LINE_4 = "addressLine4";
    public static final String ADDRESS_POSTCODE = "addressPostcode";
    public static final String EMAIL = "email";
    public static final String EMPTY_JSON_OBJECT = "{}";

    @Inject
    private Requester requester;

    @Inject
    private UsersAndGroupsService usersAndGroupsService;

    @Handles("progression.query.associated-organisation")
    public JsonEnvelope getAssociatedOrganisation(final JsonEnvelope query) {
        final JsonEnvelope jsonEnvelopeResponse = requester.request(query);
        final JsonObject association = jsonEnvelopeResponse.payloadAsJsonObject().getJsonObject(ASSOCIATION);
        if (isAssociationExist(association)) {
            return populateOrganisationDetails(query, association);
        } else {
            return emptyOrganisationDetails(query);
        }
    }

    private JsonEnvelope populateOrganisationDetails(final JsonEnvelope query, final JsonObject association) {
        final JsonObject organisationDetailsFromUsersAndGroupsService = usersAndGroupsService.getOrganisationDetailsForUser(query);
        if (isOrganisationIdEqual(association, organisationDetailsFromUsersAndGroupsService)) {
            return JsonEnvelope.envelopeFrom(
                    query.metadata(),
                    formResponsePayload(association, organisationDetailsFromUsersAndGroupsService));
        }
        return emptyOrganisationDetails(query);
    }

    private boolean isAssociationExist(final JsonObject association) {
        return !association.toString().equals(EMPTY_JSON_OBJECT);
    }

    private boolean isOrganisationIdEqual(final JsonObject association, final JsonObject organisationDetailsForUserJsonObject) {
        final String associatedOrganisationId = association.getString(ORGANISATION_ID);
        return associatedOrganisationId.equals(organisationDetailsForUserJsonObject.getString(ORGANISATION_ID));
    }

    private JsonEnvelope emptyOrganisationDetails(final JsonEnvelope query) {
        return JsonEnvelope.envelopeFrom(
                query.metadata(),
                Json.createObjectBuilder()
                        .add(ASSOCIATION, Json.createObjectBuilder())
                        .build());
    }

    private JsonObject formResponsePayload(JsonObject association, JsonObject organisationDetailsForUserJsonObject) {
        final String status = association.getString(STATUS);
        final String associationDate = association.getString(ASSOCIATION_DATE);
        String addressLine2 = "";
        String addressLine3 = "";
        if(organisationDetailsForUserJsonObject.toString().contains(ADDRESS_LINE_2)){
            addressLine2 = organisationDetailsForUserJsonObject.getString(ADDRESS_LINE_2);
        }
        if(organisationDetailsForUserJsonObject.toString().contains(ADDRESS_LINE_3)){
            addressLine3 = organisationDetailsForUserJsonObject.getString(ADDRESS_LINE_3);
        }
        return Json.createObjectBuilder()
                        .add(ASSOCIATION,Json.createObjectBuilder()
                                .add(ORGANISATION_ID, organisationDetailsForUserJsonObject.getString(ORGANISATION_ID))
                                .add(ORGANISATION_NAME, organisationDetailsForUserJsonObject.getString(ORGANISATION_NAME))
                                .add(ADDRESS, Json.createObjectBuilder()
                                        .add(ADDRESS_LINE_1, organisationDetailsForUserJsonObject.getString(ADDRESS_LINE_1))
                                        .add(ADDRESS_LINE_2, addressLine2)
                                        .add(ADDRESS_LINE_3,addressLine3)
                                        .add(ADDRESS_LINE_4, organisationDetailsForUserJsonObject.getString(ADDRESS_LINE_4))
                                        .add(ADDRESS_POSTCODE, organisationDetailsForUserJsonObject.getString(ADDRESS_POSTCODE))
                                        .add(EMAIL, organisationDetailsForUserJsonObject.getString(EMAIL))
                                )
                                .add(STATUS, status)
                                .add(ASSOCIATION_DATE, associationDate)
                        )
                .build();
    }
}
