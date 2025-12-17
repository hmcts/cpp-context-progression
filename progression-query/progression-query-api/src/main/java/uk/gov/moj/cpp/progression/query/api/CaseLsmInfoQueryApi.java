package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.CaseLsmInfoQuery;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CaseLsmInfoQueryApi {

    @Inject
    private CaseLsmInfoQuery caseLsmInfoQuery;

    @Handles("progression.query.case-lsm-info")
    public JsonEnvelope getProsecutionCaseLsmInfo(final JsonEnvelope query) {
        return caseLsmInfoQuery.getCaseLsmInfo(query);
    }

}
