package uk.gov.moj.cpp.progression.query.view.service;

import uk.gov.justice.core.courts.Hearing;

import java.time.ZonedDateTime;

/**
 * One SJP decision as SJP published it ({@code public.events.hearing.hearing-resulted}): the core-domain hearing, with
 * that decision's results only, and the time it was shared.
 */
public record SjpDecisionHearing(Hearing hearing, ZonedDateTime sharedTime) {
}
