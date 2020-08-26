package uk.gov.moj.cpp.progression.service;

import static java.util.Objects.nonNull;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.Cases;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.MatchedDefendantsResult;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.domain.MatchDefendantSearchResult;
import uk.gov.moj.cpp.progression.helper.MatchedDefendantCriteria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MatchedDefendantLoadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchedDefendantLoadService.class);

    private static final Integer DEFAULT_PAGE_SIZE = 25;
    private static final String QUERY_DEFENDANT_CASES = "unifiedsearch.query.defendant.cases";
    private static final String START_FROM = "startFrom";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    public void aggregateDefendantsSearchResultForAProsecutionCase(final Envelope<?> envelope, final ProsecutionCase prosecutionCase) throws EventStreamException {
        final MatchDefendantSearchResult matchDefendantSearchResult = getDefendantsSearchResults(envelope, prosecutionCase);

        final EventStream prosecutionCaseStream = eventSource.getStreamById(prosecutionCase.getId());
        final CaseAggregate caseAggregate = aggregateService.get(prosecutionCaseStream, CaseAggregate.class);

        final Map<UUID, List<Cases>> exactMatchedDefendants = matchDefendantSearchResult.getFullyMatchedDefendants();
        for (final Map.Entry<UUID, List<Cases>> e : exactMatchedDefendants.entrySet()) {
            final Stream<Object> events = caseAggregate.aggregateExactMatchedDefendantSearchResult(e.getKey(), e.getValue());
            appendEventsToStream(envelope, prosecutionCaseStream, events);
        }

        final Map<UUID, List<Cases>> partialMatchedDefendants = matchDefendantSearchResult.getPartiallyMatchedDefendants();
        for (final Map.Entry<UUID, List<Cases>> e : partialMatchedDefendants.entrySet()) {
            final Stream<Object> events = caseAggregate.aggregatePartialMatchedDefendantSearchResult(e.getKey(), e.getValue());
            appendEventsToStream(envelope, prosecutionCaseStream, events);
        }
    }

    private MatchDefendantSearchResult getDefendantsSearchResults(final Envelope<?> envelope, final ProsecutionCase prosecutionCase) {

        final Map<UUID, List<Cases>> partialMatchedDefendants = new HashMap<>();
        final Map<UUID, List<Cases>> exactMatchedDefendants = new HashMap<>();
        for (final Defendant defendant : prosecutionCase.getDefendants()) {
            if (nonNull(defendant.getPersonDefendant())) {
                LOGGER.info("Person Defendant in prosecution case");
                addExactMatchAndPartialMatchDefendants(envelope, partialMatchedDefendants, exactMatchedDefendants, defendant);
            }
        }
        return MatchDefendantSearchResult.matchDefendantSearchResult()
                .withFullyMatchedDefendants(exactMatchedDefendants)
                .withPartiallyMatchedDefendants(partialMatchedDefendants)
                .build();
    }

    private void addExactMatchAndPartialMatchDefendants(final Envelope<?> envelope, final Map<UUID, List<Cases>> partialMatchedDefendants, final Map<UUID, List<Cases>> exactMatchedDefendants, final Defendant defendant) {
        final MatchedDefendantCriteria matchedDefendantCriteria = new MatchedDefendantCriteria(defendant);
        List<Cases> cases = callUnifiedSearchQueryForExactMatch(matchedDefendantCriteria, envelope);
        if (CollectionUtils.isNotEmpty(cases)) {
            LOGGER.info("Defendant with id is fully matched in CP Search");
            exactMatchedDefendants.put(defendant.getId(), cases);
        } else {
            cases = callUnifiedSearchQueryForPartialMatch(matchedDefendantCriteria, envelope);
            if (CollectionUtils.isNotEmpty(cases)) {
                LOGGER.info("Defendant with id is partially matched in CP Search");
                partialMatchedDefendants.put(defendant.getId(), cases);
            }
        }
    }

    private List<Cases> callUnifiedSearchQuery(final Envelope<?> envelope, JsonObject criteria) {
        Integer page = 0;
        Integer totalResult = 0;
        final List<Cases> casesList = new ArrayList<>();
        while (true) {
            final JsonObjectBuilder criteriaBuilder = Json.createObjectBuilder();
            criteria.forEach(criteriaBuilder::add);
            criteriaBuilder.add(START_FROM, page);

            LOGGER.info("Cp search invoked with {}, page:{}", criteria, page);
            final Envelope<MatchedDefendantsResult> response = requester
                    .requestAsAdmin(envelop(criteriaBuilder.build())
                                    .withName(QUERY_DEFENDANT_CASES)
                                    .withMetadataFrom(envelope)
                            , MatchedDefendantsResult.class);

            if (response != null && response.payload() != null) {
                casesList.addAll(response.payload().getCases());
                totalResult = response.payload().getTotalResults();
                LOGGER.info("Cp search return {} result", totalResult);
            }

            page++;
            if (page * DEFAULT_PAGE_SIZE + 1 > totalResult) {
                break;
            }
        }

        return casesList;
    }

    private List<Cases> callUnifiedSearchQueryForExactMatch(final MatchedDefendantCriteria matchedDefendantCriteria, final Envelope<?> envelope) {
        final List<Cases> casesList = new ArrayList<>();
        while (casesList.isEmpty() && matchedDefendantCriteria.hasMoreExactSteps()) {
            final int lastExactStep = matchedDefendantCriteria.getCurrentExactStep();
            while (lastExactStep == matchedDefendantCriteria.getCurrentExactStep() && matchedDefendantCriteria.hasMoreSubSteps()) {
                if (matchedDefendantCriteria.nextExactCriteria()) {
                    casesList.addAll(callUnifiedSearchQuery(envelope, matchedDefendantCriteria.getExactCriteria().build()));
                }
            }
        }

        return casesList;
    }

    private List<Cases> callUnifiedSearchQueryForPartialMatch(final MatchedDefendantCriteria matchedDefendantCriteria, final Envelope<?> envelope) {
        final List<Cases> casesList = new ArrayList<>();
        while (casesList.isEmpty() && matchedDefendantCriteria.hasMorePartialSteps()) {
            final int lastPartialStep = matchedDefendantCriteria.getCurrentPartialStep();
            while (lastPartialStep == matchedDefendantCriteria.getCurrentPartialStep() && matchedDefendantCriteria.hasMoreSubSteps()) {
                if (matchedDefendantCriteria.nextPartialCriteria()) {
                    casesList.addAll(callUnifiedSearchQuery(envelope, matchedDefendantCriteria.getPartialCriteria().build()));
                }
            }
        }

        return casesList;
    }

    public static void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        final Stream<JsonEnvelope> envelopeStream = events.map(toEnvelopeWithMetadataFrom(jsonEnvelope));
        eventStream.append(envelopeStream);
    }
}
