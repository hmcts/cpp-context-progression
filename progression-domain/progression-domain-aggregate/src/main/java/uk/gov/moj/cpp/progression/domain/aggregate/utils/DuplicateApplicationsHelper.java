package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public class DuplicateApplicationsHelper {

    private DuplicateApplicationsHelper() {
    }

    public static Hearing deDupAllApplications(final Hearing hearing) {
        if (hearing == null) {
            return null;
        }
        return Hearing.hearing().
                withValuesFrom(hearing).
                withProsecutionCases(hearing.getProsecutionCases()).
                withCourtApplications(distinctCourtApplicationList(hearing.getCourtApplications())).
                build();

    }

    private static List<CourtApplication> distinctCourtApplicationList(final List<CourtApplication> courtApplicationListOriginal) {
        if (isNull(courtApplicationListOriginal) || courtApplicationListOriginal.isEmpty()) {
            return courtApplicationListOriginal;
        }
        final Set<UUID> courtApplicationIds = new HashSet<>();
        return courtApplicationListOriginal.stream().filter(x -> courtApplicationIds.add(x.getId())).collect(Collectors.toList());
    }

}

