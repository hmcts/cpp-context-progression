package uk.gov.moj.cpp.progression.query.view.service;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * The SJP case decision (from {@code sjp.query.case}) to show on CAAG. {@code id} is also the hearing ID SJP published
 * the decision's results under.
 */
public record SjpCaseDecision(UUID id, ZonedDateTime savedAt) {
}
