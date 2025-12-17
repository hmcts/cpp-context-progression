package uk.gov.justice.api.resource;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.providers.Provider;
import uk.gov.moj.cpp.progression.query.CourtDocumentQueryView;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

@Provider
@ApplicationScoped
public class CourtDocumentCommandProvider {

    @Inject
    private CourtDocumentQueryView courtDocumentQueryView;

    public Action getDocumentTypeId(final Action action) {

        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder().add("courtDocumentId", action.envelope().payloadAsJsonObject().getString("courtDocumentId"));

        final MetadataBuilder metadataBuilder = metadataFrom(action.metadata()).withName("progression.query.courtdocument");

        final JsonEnvelope requestEnvelope = envelopeFrom(metadataBuilder, jsonObjectBuilder.build());

        final JsonEnvelope response = courtDocumentQueryView.getCourtDocument(requestEnvelope);

        return new Action(response);
    }
}
