package uk.gov.moj.cpp.progression.query.api;


import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.api.service.CourtOrderService;
import uk.gov.moj.cpp.progression.query.api.service.OrganisationService;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.query.api.helper.ProgressionQueryHelper.addProperty;

@ServiceComponent(Component.QUERY_API)
public class ProsecutionCaseQueryApi {
    private static final String ORGANISATION_NAME = "name";
    private static final String ADDRESS_LINE_1 = "addressLine1";
    private static final String ADDRESS_LINE_2 = "addressLine2";
    private static final String ADDRESS_LINE_3 = "addressLine3";
    private static final String ADDRESS_LINE_4 = "addressLine4";
    private static final String ADDRESS_POSTCODE = "addressPostcode";
    private static final String DEFENDANTS = "defendants";
    private static final String COURT_ORDERS = "courtOrders";
    private static final String MASTER_DEFENDANT_ID = "masterDefendantId";

    @Inject
    private Requester requester;

    @Inject
    private OrganisationService organisationService;

    @Inject
    private CourtOrderService courtOrderService;

    @Handles("progression.query.prosecutioncase")
    public JsonEnvelope getCaseProsecutionCase(final JsonEnvelope query) {

        final JsonEnvelope appQueryResponse = requester.request(query);
        final JsonObject queryViewPayload = appQueryResponse.payloadAsJsonObject();
        final JsonObject prosecutionCase = appQueryResponse.payloadAsJsonObject().getJsonObject("prosecutionCase");

        if (Objects.nonNull(prosecutionCase)) {
            final JsonArray defendants = prosecutionCase.getJsonArray(DEFENDANTS);
            final JsonArrayBuilder activeCourtOrdersArrayBuilder = Json.createArrayBuilder();

            final Set<UUID> uniqueMasterDefendantIds = defendants.stream()
                    .map(defendant -> UUID.fromString(((JsonObject) defendant).getString(MASTER_DEFENDANT_ID)))
                    .collect(Collectors.toSet());

            uniqueMasterDefendantIds.forEach(masterDefendantId -> {
                final JsonObject courtOrders = courtOrderService.getCourtOrdersByDefendant(query, masterDefendantId, requester);
                if (Objects.nonNull(courtOrders) && courtOrders.containsKey(COURT_ORDERS)) {
                    final JsonArray activeCourtOrders= courtOrders.getJsonArray(COURT_ORDERS);
                    if(!activeCourtOrders.isEmpty()) {
                        final JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
                                .add(MASTER_DEFENDANT_ID, masterDefendantId.toString())
                                .add(COURT_ORDERS, activeCourtOrders);
                        activeCourtOrdersArrayBuilder.add(objectBuilder.build());
                    }
                }
            });

            final JsonArray activeCourtOrders = activeCourtOrdersArrayBuilder.build();
            if (!activeCourtOrders.isEmpty()) {
                return envelopeFrom(query.metadata(), addProperty(queryViewPayload, "activeCourtOrders", activeCourtOrders));
            }
        }
        return appQueryResponse;
    }

    @Handles("progression.query.prosecutioncase.caag")
    public JsonEnvelope getProsecutionCaseForCaseAtAGlance(final JsonEnvelope query) {
        final JsonEnvelope appQueryResponse = requester.request(query);
        final JsonObject payload = appQueryResponse.payloadAsJsonObject();
        final JsonArray defendants = payload.getJsonArray(DEFENDANTS);
        final JsonArrayBuilder caagDefendantsBuilder = Json.createArrayBuilder();
        if (Objects.nonNull(defendants)) {
            defendants.forEach(defendantJson -> {
                final JsonObject caagDefendant = (JsonObject) defendantJson;
                final JsonObject associatedOrganisation = organisationService.getAssociatedOrganisation(query, caagDefendant.getString("id"), requester);
                if (associatedOrganisation.containsKey("organisationId")) {
                    final JsonObject representation = createOrganisation(associatedOrganisation);
                    caagDefendantsBuilder.add(addProperty(caagDefendant, "representation", representation));
                } else {
                    caagDefendantsBuilder.add(caagDefendant);
                }
            });
            final JsonObject resultPayload = addProperty(payload, DEFENDANTS, caagDefendantsBuilder.build());
            return envelopeFrom(query.metadata(), resultPayload);
        }
        return appQueryResponse;
    }

    @Handles("progression.query.case")
    public JsonEnvelope getProsecutionCase(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("progression.query.casehearings")
    public JsonEnvelope getCaseHearings(final JsonEnvelope query){
        return requester.request(query);
    }


    @Handles("progression.query.usergroups-by-material-id")
    public JsonEnvelope searchForUserGroupsByMaterialId(final JsonEnvelope query) {
        return this.requester.request(query);
    }


    @Handles("progression.query.search-cases")
    public JsonEnvelope searchCaseProsecutionCase(final JsonEnvelope query) {
        return requester.request(query);
    }


    /**
     * Handler returns document details and not document content. This is consequence of non
     * framework endpoint which uses standard framework interceptors. Handler is invoked at the end
     * of programmatically invoked interceptor chain, see DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource.
     */
    @Handles("progression.query.court-extract")
    public JsonEnvelope getCourtExtract(final JsonEnvelope query) {

        return query;
    }

    @Handles("progression.query.eject-case")
    public JsonEnvelope ejectCase(final JsonEnvelope query) {

        return query;
    }

    @Handles("progression.query.case.hearingtypes")
    public JsonEnvelope getCaseHearingTypes(final JsonEnvelope query){
        return requester.request(query);
    }

    private JsonObject createOrganisation(final JsonObject completeOrganisationDetails) {

        final JsonObject address = completeOrganisationDetails.getJsonObject("address");
        return Json.createObjectBuilder().add(ORGANISATION_NAME, completeOrganisationDetails.getString("organisationName"))
                .add("address", Json.createObjectBuilder()
                        .add(ADDRESS_LINE_1, address.getString("address1"))
                        .add(ADDRESS_LINE_2, address.getString("address2"))
                        .add(ADDRESS_LINE_3, address.getString("address3"))
                        .add(ADDRESS_LINE_4, address.getString("address4"))
                        .add(ADDRESS_POSTCODE, address.getString(ADDRESS_POSTCODE))
                ).build();
    }
}