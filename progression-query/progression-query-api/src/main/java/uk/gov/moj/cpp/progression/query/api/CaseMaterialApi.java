
package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.MaterialBulkQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CaseMaterialApi {

    @Inject
    private MaterialBulkQueryView materialBulkQueryView;

    @Handles("progression.query.material-content-bulk")
    public JsonEnvelope findMaterialIdMappingsInBulk(final JsonEnvelope envelope) {
        return materialBulkQueryView.findMaterialIdMappingsInBulk(envelope);
    }
}