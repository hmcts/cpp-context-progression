package uk.gov.moj.cpp.progression.domain.transformation.corechanges.transform;

import uk.gov.justice.services.messaging.Metadata;

import javax.json.JsonObject;

public interface ProgressionEventTransformer {
    JsonObject transform(Metadata eventMetadata, JsonObject payload);
}
