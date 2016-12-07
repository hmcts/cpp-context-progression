package uk.gov.moj.cpp.progression.query.api;


import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.dispatcher.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;

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

    @Handles("progression.query.defendant")
    public JsonEnvelope getDefendant(final JsonEnvelope query) {
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
     *
     * @param envelope
     * @return
     */
    @Handles("progression.query.crown-court.magistrate-courts")
    public JsonEnvelope getMagistratesCourts(final JsonEnvelope envelope) {
        final JsonObject requestPayload = envelope.payloadAsJsonObject();
        final String crownCourtId = requestPayload.getString("crownCourtId");
        return envelopeFrom(envelope.metadata(), buildRequestPayload(crownCourtId));
    }

    private JsonObject buildRequestPayload(final String crownCourtId) {
        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        List<String> magistratesCourts = getMagistratesCourts(crownCourtId);
        magistratesCourts.forEach(arrayBuilder::add);
        return Json.createObjectBuilder().add("values", arrayBuilder).build();
    }

    private List<String> getMagistratesCourts(String crownCourtId) {
        switch (crownCourtId) {
            case "LCC":
                return newArrayList("Liverpool & Knowsley Magistrates Court", "Ormskirk Magistrates Court", "Sefton Magistrates Court",
                        "St Helens Magistrates Court", "Wigan Magistrates Court", "Wirral Magistrates Court", "Other");
            default:
                return newArrayList();
        }
    }
}
