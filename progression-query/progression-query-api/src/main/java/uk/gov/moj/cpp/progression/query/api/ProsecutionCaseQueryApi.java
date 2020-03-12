package uk.gov.moj.cpp.progression.query.api;


import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class ProsecutionCaseQueryApi {

    @Inject
    private Requester requester;

    @Handles("progression.query.prosecutioncase")
    public JsonEnvelope getCaseProsecutionCase(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("progression.query.prosecutioncase.caag")
    public JsonEnvelope getProsecutionCaseForCaseAtAGlance(final JsonEnvelope query) {
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
}
