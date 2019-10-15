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
    public static final String START_DATE = "startDate";
    public static final String ORGANISATION_NAME = "organisationName";
    public static final String ADDRESS = "address";
    public static final String ADDRESS_1 = "address1";
    public static final String ADDRESS_2 = "address2";
    public static final String ADDRESS_3 = "address3";
    public static final String ADDRESS_4 = "address4";
    public static final String ADDRESS_LINE_1 = "addressLine1";
    public static final String ADDRESS_LINE_2 = "addressLine2";
    public static final String ADDRESS_LINE_3 = "addressLine3";
    public static final String ADDRESS_LINE_4 = "addressLine4";
    public static final String ADDRESS_POSTCODE = "addressPostcode";
    public static final String EMPTY_JSON_OBJECT = "{}";
    public static final String REPRESENTATION_TYPE = "representationType";
    public static final String GROUP_ID = "groupId";
    public static final String GROUP_NAME = "groupName";

    @Inject
    private Requester requester;

    @Inject
    private UsersAndGroupsService usersAndGroupsService;

    @Handles("progression.query.associated-organisation")
    public JsonEnvelope getAssociatedOrganisation(final JsonEnvelope query) {

        final JsonEnvelope associationEnvelope = requester.request(query);
        final JsonObject association = associationEnvelope.payloadAsJsonObject().getJsonObject(ASSOCIATION);
        if (associationExists(association)) {
            return populateOrganisationDetails(associationEnvelope);
        } else {
            return emptyOrganisationDetails(query);
        }
    }

    private JsonEnvelope populateOrganisationDetails(final JsonEnvelope associationEnvelope) {
        final JsonEnvelope usersAndGroupsRequestEnvelope = buildUsersAndGroupsRequestEnvelope(associationEnvelope);
        final JsonObject organisationDetailsFromUsersAndGroupsService = usersAndGroupsService.getOrganisationDetails(usersAndGroupsRequestEnvelope);
        return JsonEnvelope.envelopeFrom(
                associationEnvelope.metadata(),
                formResponsePayload(associationEnvelope.payloadAsJsonObject().getJsonObject(ASSOCIATION),
                        organisationDetailsFromUsersAndGroupsService));

    }

    private JsonEnvelope buildUsersAndGroupsRequestEnvelope(final JsonEnvelope associationEnvelope) {
        return JsonEnvelope.envelopeFrom(associationEnvelope.metadata(), Json.createObjectBuilder()
                .add(ORGANISATION_ID,
                        associationEnvelope.payloadAsJsonObject().getJsonObject(ASSOCIATION).getJsonString(ORGANISATION_ID)).build());
    }

    private boolean associationExists(final JsonObject association) {
        return !association.toString().equals(EMPTY_JSON_OBJECT);
    }

    private JsonEnvelope emptyOrganisationDetails(final JsonEnvelope query) {
        return JsonEnvelope.envelopeFrom(
                query.metadata(),
                Json.createObjectBuilder()
                        .add(ASSOCIATION, Json.createObjectBuilder())
                        .build());
    }

    private JsonObject formResponsePayload(final JsonObject association, final JsonObject organisationDetailsForUserJsonObject) {
        final String status = association.getString(STATUS);
        final String startDate = association.getString(START_DATE);
        final String representationType = association.getString(REPRESENTATION_TYPE);
        String address2 = "";
        String address3 = "";
        if (organisationDetailsForUserJsonObject.toString().contains(ADDRESS_LINE_2)) {
            address2 = organisationDetailsForUserJsonObject.getString(ADDRESS_LINE_2);
        }
        if (organisationDetailsForUserJsonObject.toString().contains(ADDRESS_LINE_3)) {
            address3 = organisationDetailsForUserJsonObject.getString(ADDRESS_LINE_3);
        }
        return Json.createObjectBuilder()
                .add(ASSOCIATION, Json.createObjectBuilder()
                        .add(ORGANISATION_ID, organisationDetailsForUserJsonObject.getString(ORGANISATION_ID))
                        .add(ORGANISATION_NAME, organisationDetailsForUserJsonObject.getString(ORGANISATION_NAME))
                        .add(ADDRESS, Json.createObjectBuilder()
                                .add(ADDRESS_1, organisationDetailsForUserJsonObject.getString(ADDRESS_LINE_1))
                                .add(ADDRESS_2, address2)
                                .add(ADDRESS_3, address3)
                                .add(ADDRESS_4, organisationDetailsForUserJsonObject.getString(ADDRESS_LINE_4))
                                .add(ADDRESS_POSTCODE, organisationDetailsForUserJsonObject.getString(ADDRESS_POSTCODE))
                        )
                        .add(STATUS, status)
                        .add(START_DATE, startDate)
                        .add(REPRESENTATION_TYPE, representationType)
                )
                .build();
    }

}
