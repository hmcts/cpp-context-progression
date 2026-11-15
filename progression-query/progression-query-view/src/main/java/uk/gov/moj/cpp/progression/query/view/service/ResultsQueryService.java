package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the SJP decision payloads that cpp-context-results stored from {@code public.events.hearing.hearing-resulted}
 * ({@code results.get-hearing-details-internal}). For a normal SJP case decision the hearing ID is the decision ID.
 */
public class ResultsQueryService implements SjpDecisionResultsSource {

    static final String RESULTS_GET_HEARING_DETAILS_INTERNAL = "results.get-hearing-details-internal";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsQueryService.class);
    private static final String HEARING = "hearing";

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    @Override
    public List<JsonObject> getDecisionPayloads(final JsonEnvelope envelope, final List<UUID> decisionIds) {
        final List<JsonObject> payloads = new ArrayList<>();
        decisionIds.forEach(decisionId -> getDecisionPayload(envelope, decisionId).ifPresent(payloads::add));
        return payloads;
    }

    private Optional<JsonObject> getDecisionPayload(final JsonEnvelope envelope, final UUID decisionId) {
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName(RESULTS_GET_HEARING_DETAILS_INTERNAL),
                createObjectBuilder().add("hearingId", decisionId.toString()).build());
        final JsonEnvelope response = requester.requestAsAdmin(request);
        if (isNull(response) || !(response.payload() instanceof JsonObject) || !response.payloadAsJsonObject().containsKey(HEARING)) {
            LOGGER.warn("No stored results in Results for SJP decision {}", decisionId);
            return Optional.empty();
        }
        return Optional.of(response.payloadAsJsonObject());
    }
}
