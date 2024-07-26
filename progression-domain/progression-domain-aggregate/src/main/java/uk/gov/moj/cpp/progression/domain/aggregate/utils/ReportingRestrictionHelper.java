package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ReportingRestriction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportingRestrictionHelper {

    private ReportingRestrictionHelper() {

    }

    public static List<Offence> dedupAllReportingRestrictions(final List<Offence> updatedOffences) {
        final ArrayList<Offence> res = new ArrayList<>();
        for (final Offence offence : updatedOffences) {
            res.add(Offence.offence().withValuesFrom(offence).withReportingRestrictions(dedupReportingRestrictions(offence.getReportingRestrictions())).build());
        }
        return res;
    }

    public static List<ReportingRestriction> dedupReportingRestrictions(final List<ReportingRestriction> reportingRestrictions) {
        if (reportingRestrictions == null) {
            return reportingRestrictions;
        }

        final Map<String, ReportingRestriction> res = new LinkedHashMap<>();
        for (final ReportingRestriction current : reportingRestrictions) {
            final String key = getKey(current);
            ReportingRestriction prev = res.get(key);
            if (prev == null) {
                prev = current;
            }

            res.put(key, oldestOf(prev, current));
        }

        return new ArrayList<>(res.values());
    }

    private static String getKey(final ReportingRestriction current) {
        return String.format("%s-%s", current.getLabel(), current.getJudicialResultId() == null ? "" : current.getJudicialResultId().toString());
    }

    private static ReportingRestriction oldestOf(final ReportingRestriction reportingRestriction1, final ReportingRestriction reportingRestriction2) {
        if (reportingRestriction1.getOrderedDate() == null) {
            return reportingRestriction2;
        }

        if (reportingRestriction2.getOrderedDate() == null) {
            return reportingRestriction1;
        }


        if (reportingRestriction2.getOrderedDate().isBefore(reportingRestriction1.getOrderedDate())) {
            return reportingRestriction2;
        }

        return reportingRestriction2;
    }
}
