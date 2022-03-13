package uk.gov.moj.cpp.progression.event.util;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.isNull;

public class DuplicateOffencesHelper {

    private DuplicateOffencesHelper() {
    }

    public static void filterDuplicateOffencesById(final List<Offence> offences) {
        if (isNull(offences) || offences.isEmpty()) {
            return;
        }
        final Set<UUID> offenceIds = new HashSet<>();
        offences.removeIf(e -> !offenceIds.add(e.getId()));
    }

    public static void filterDuplicateOffencesByIdForCase(final ProsecutionCase prosecutionCase) {
        if (isNull(prosecutionCase)) {
            return;
        }
        if(prosecutionCase.getDefendants() != null) {
            prosecutionCase.getDefendants().forEach(c -> filterDuplicateOffencesByIdForDefendant(c));
        }
    }

    public static void filterDuplicateOffencesByIdForCases(final List<ProsecutionCase> prosecutionCases) {
        if (isNull(prosecutionCases)) {
            return;
        }
        prosecutionCases.stream().forEach(c -> filterDuplicateOffencesByIdForCase(c));
    }

    public static void filterDuplicateOffencesByIdForHearing(final Hearing hearing) {
        if (isNull(hearing)) {
            return;
        }
        if(hearing.getProsecutionCases() != null) {
            hearing.getProsecutionCases().forEach(c -> filterDuplicateOffencesByIdForCase(c));
        }
    }

    public static void filterDuplicateOffencesByIdForDefendant(final Defendant defendant) {
        if (isNull(defendant)) {
            return;
        }
        filterDuplicateOffencesById(defendant.getOffences());
    }


}
