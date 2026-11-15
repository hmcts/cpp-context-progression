package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets the SJP decisions to show on Case at a Glance for a case created from SJP (CHD-3300 / CHD-3311): the decision IDs
 * from SJP, cut at the latest set-aside, then each decision's published results, with "Shared on" and short codes added.
 *
 * <p>Never fails the CAAG page: if SJP or Results is unavailable, it logs and returns no decisions; if reference data is
 * unavailable, the results are shown without short codes.
 */
public class SjpDecisionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SjpDecisionService.class);
    private static final String HEARING = "hearing";
    private static final String SHARED_TIME = "sharedTime";

    @Inject
    private SjpQueryService sjpQueryService;

    @Inject
    private SjpDecisionResultsSource sjpDecisionResultsSource;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    public List<SjpDecisionHearing> getDecisionHearings(final JsonEnvelope envelope, final UUID caseId,
                                                        final InitiationCode initiationCode,
                                                        final List<CourtApplication> linkedApplications) {
        if (!SjpDecisionScope.isInScope(initiationCode, linkedApplications)) {
            return emptyList();
        }
        try {
            final List<UUID> decisionIds = SjpDecisionSelector.selectDecisionIds(sjpQueryService.getCaseDecisions(envelope, caseId));
            if (decisionIds.isEmpty()) {
                LOGGER.info("No SJP decisions to show on CAAG for case {}", caseId);
                return emptyList();
            }
            final List<JsonObject> payloads = sjpDecisionResultsSource.getDecisionPayloads(envelope, decisionIds);
            final Map<UUID, String> shortCodes = getShortCodes(envelope, payloads);
            return payloads.stream().map(payload -> toDecisionHearing(payload, shortCodes)).collect(toList());
        } catch (final RuntimeException e) {
            LOGGER.warn("Unable to get SJP decisions for case {}; CAAG is returned without them", caseId, e);
            return emptyList();
        }
    }

    private Map<UUID, String> getShortCodes(final JsonEnvelope envelope, final List<JsonObject> payloads) {
        final Set<UUID> resultDefinitionIds = payloads.stream()
                .flatMap(payload -> SjpDecisionPayloads.resultDefinitionIds(payload.get(HEARING)).stream())
                .collect(toSet());
        try {
            return referenceDataService.getResultShortCodes(envelope, resultDefinitionIds);
        } catch (final RuntimeException e) {
            LOGGER.warn("Unable to get result short codes from reference data; SJP results are shown without them", e);
            return emptyMap();
        }
    }

    private SjpDecisionHearing toDecisionHearing(final JsonObject payload, final Map<UUID, String> shortCodes) {
        final boolean hasSharedTime = payload.containsKey(SHARED_TIME) && !payload.isNull(SHARED_TIME);
        JsonValue hearingJson = SjpDecisionPayloads.withShortCodes(payload.get(HEARING), shortCodes);
        if (hasSharedTime) {
            hearingJson = SjpDecisionPayloads.withLastSharedDateTime(hearingJson, payload.get(SHARED_TIME));
        }
        final Hearing hearing = jsonObjectToObjectConverter.convert((JsonObject) hearingJson, Hearing.class);
        return new SjpDecisionHearing(hearing, hasSharedTime ? ZonedDateTime.parse(payload.getString(SHARED_TIME)) : null);
    }
}
