package uk.gov.moj.cpp.progression.query.controller;


import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.dispatcher.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_CONTROLLER)
public class ProgressionQueryController {

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

    @Handles("progression.query.defendant.document")
    public JsonEnvelope getDefendantDocument(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("progression.query.defendants")
    public JsonEnvelope getDefendants(final JsonEnvelope query) {
        return requester.request(query);
    }


}
