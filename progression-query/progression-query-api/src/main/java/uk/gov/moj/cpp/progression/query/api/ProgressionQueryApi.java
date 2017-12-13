package uk.gov.moj.cpp.progression.query.api;


import static com.google.common.collect.Lists.newArrayList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

@ServiceComponent(Component.QUERY_API)
public class ProgressionQueryApi {

    @Inject
    private Requester requester;

    @Handles("progression.query.caseprogressiondetail")
    public JsonEnvelope getCaseprogressiondetail(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("progression.query.cases")
    public JsonEnvelope getCases(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("progression.query.case-by-urn")
    public JsonEnvelope getCaseByUrn(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("progression.query.cases-search-by-material-id")
    public JsonEnvelope getCaseSearchByMaterialId(final JsonEnvelope query) {
        return requester.request(query);
    }


    @Handles("progression.query.defendant")
    public JsonEnvelope getDefendant(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("progression.query.defendant.document")
    public JsonEnvelope getDefendantDocument(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("progression.query.defendants")
    public JsonEnvelope getDefendants(final JsonEnvelope query) {
        return requester.request(query);
    }

    /**
     * This is a temporary implementation to support list
     * of magistrate courts for liverpool crown court to
     * support CRC-2918. Ideally this needs to sit in reference
     * context once the origin of data is clear.
     */
    @Handles("progression.query.crown-court.magistrate-courts")
    public JsonEnvelope getMagistratesCourts(final JsonEnvelope envelope) {
        final JsonObject requestPayload = envelope.payloadAsJsonObject();
        final String crownCourtId = requestPayload.getString("crownCourtId");
        return envelopeFrom(envelope.metadata(), buildRequestPayload(crownCourtId));
    }

    @Handles("progression.query.defendant-offences")
    public JsonEnvelope findOffences(final JsonEnvelope query) {
        return requester.request(query);
    }


    private JsonObject buildRequestPayload(final String crownCourtId) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        List<String> magistratesCourts = getMagistratesCourts(crownCourtId);
        magistratesCourts.forEach(s -> arrayBuilder.add(createObjectBuilder().add("name", s)));
        return createObjectBuilder().add("values", arrayBuilder).build();
    }

    private List<String> getMagistratesCourts(String crownCourtId) {
        switch (crownCourtId) {
            case "LCC":
                return newArrayList("Liverpool", "Bootle", "Birkenhead", "Warrington");
            default:
                return newArrayList();
        }
    }
}
