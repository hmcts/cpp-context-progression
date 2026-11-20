package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Picks the SJP decisions whose results CAAG shows, using SJP's own rule (SJP query view
 * {@code ResultsService.removePreviousDecisions}): drop every decision saved at or before the latest application decision
 * (a set-aside after a reopen or stat dec), then drop any remaining application decisions.
 */
public final class SjpDecisionSelector {

    private SjpDecisionSelector() {
    }

    /**
     * @return the IDs of the decisions to show, oldest first.
     */
    public static List<UUID> selectDecisionIds(final List<SjpCaseDecision> caseDecisions) {
        final Optional<ZonedDateTime> latestApplicationDecision = caseDecisions.stream()
                .filter(SjpCaseDecision::applicationDecision)
                .map(SjpCaseDecision::savedAt)
                .max(ZonedDateTime::compareTo);

        return caseDecisions.stream()
                .filter(decision -> !decision.applicationDecision())
                .filter(decision -> latestApplicationDecision.map(cut -> decision.savedAt().isAfter(cut)).orElse(true))
                .sorted(comparing(SjpCaseDecision::savedAt))
                .map(SjpCaseDecision::id)
                .collect(toList());
    }
}
