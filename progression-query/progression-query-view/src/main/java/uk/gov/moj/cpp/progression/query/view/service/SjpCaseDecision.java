package uk.gov.moj.cpp.progression.query.view.service;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * The parts of an SJP {@code caseDecisions[]} entry (from {@code sjp.query.case}) needed to pick which decisions to show
 * on CAAG. For a normal SJP case decision, {@code id} is also the hearing ID SJP published the results under.
 */
public record SjpCaseDecision(UUID id, ZonedDateTime savedAt, boolean applicationDecision) {
}
