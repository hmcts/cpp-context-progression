package uk.gov.moj.cpp.progression.query.api;


import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.query.api.helper.ProgressionQueryHelper.addProperty;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.api.service.OrganisationService;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

@ServiceComponent(Component.QUERY_API)
public class ProsecutionCaseQueryApi {
    private static final String ORGANISATION_NAME = "name";
    private static final String ADDRESS_LINE_1 = "addressLine1";
    private static final String ADDRESS_LINE_2 = "addressLine2";
    private static final String ADDRESS_LINE_3 = "addressLine3";
    private static final String ADDRESS_LINE_4 = "addressLine4";
    private static final String ADDRESS_POSTCODE = "addressPostcode";
    private static final String DEFENDANTS = "defendants";


    @Inject
    private Requester requester;

    @Inject
    private OrganisationService organisationService;


    @Handles("progression.query.prosecutioncase")
    public JsonEnvelope getCaseProsecutionCase(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("progression.query.prosecutioncase.caag")
    public JsonEnvelope getProsecutionCaseForCaseAtAGlance(final JsonEnvelope query) {
        final JsonEnvelope appQueryResponse = requester.request(query);
        final JsonObject payload = appQueryResponse.payloadAsJsonObject();
        final JsonArray defendants = payload.getJsonArray(DEFENDANTS);
        final JsonArrayBuilder caagDefendantsBuilder = Json.createArrayBuilder();
        if(defendants != null) {
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

    private JsonObject createOrganisation(final JsonObject completeOrganisationDetails) {

        final JsonObject address = completeOrganisationDetails.getJsonObject("address");
        return Json.createObjectBuilder().add(ORGANISATION_NAME, completeOrganisationDetails.getString("organisationName"))
                                         .add("address",Json.createObjectBuilder()
                                                                                    .add(ADDRESS_LINE_1, address.getString("address1"))
                                                                                    .add(ADDRESS_LINE_2, address.getString("address2"))
                                                                                    .add(ADDRESS_LINE_3, address.getString("address3"))
                                                                                    .add(ADDRESS_LINE_4, address.getString("address4"))
                                                                                    .add(ADDRESS_POSTCODE, address.getString(ADDRESS_POSTCODE))
                                         ).build();
    }
}