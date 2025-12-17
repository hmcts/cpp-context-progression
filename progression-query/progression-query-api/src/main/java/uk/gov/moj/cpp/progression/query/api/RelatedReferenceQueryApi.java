package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.view.RelatedReferenceQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class RelatedReferenceQueryApi {

    @Inject
    private RelatedReferenceQueryView relatedReferenceQueryView;

    @Handles("progression.query.related-references")
    public JsonEnvelope getProsecutionCaseWithRelatedUrn(final JsonEnvelope envelope) {
        return relatedReferenceQueryView.getProsecutionCaseWithRelatedUrn(envelope);
    }

}
