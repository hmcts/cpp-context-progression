package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Offence;

import java.time.LocalDate;
import java.util.UUID;

public class CourtApplicationHelper {

    private CourtApplicationHelper(){
        throw new IllegalStateException("Utility class");
    }

    private static CourtOrderOffence getCourtOrderOffenceWithConvictionDate(final CourtOrderOffence o, final UUID offenceId, final LocalDate convictionDate) {
        return !o.getOffence().getId().equals(offenceId) ? o : CourtOrderOffence.courtOrderOffence().withValuesFrom(o)
                .withOffence(Offence.offence().withValuesFrom(o.getOffence())
                        .withConvictionDate(convictionDate)
                        .build())
                .build();
    }

    private static Offence getCourtApplicationOffenceWithConvictionDate(final Offence courtApplicationOffence, final UUID offenceId, final LocalDate convictionDate) {
        return !courtApplicationOffence.getId().equals(offenceId) ? courtApplicationOffence :
                Offence.offence().withValuesFrom(courtApplicationOffence)
                                .withConvictionDate(convictionDate)
                                .build();
    }

    public static CourtApplication getCourtApplicationWithConvictionDate(final CourtApplication courtApplication, final LocalDate convictionDate){
        return courtApplication().withValuesFrom(courtApplication)
                .withConvictionDate(convictionDate)
                .build();
    }

    public static CourtApplication getCourtApplicationWithConvictionDate(final CourtApplication courtApplication, final UUID offenceId, final LocalDate convictionDate){
        return courtApplication().withValuesFrom(courtApplication)
                .withCourtApplicationCases(courtApplication.getCourtApplicationCases() == null ? null : courtApplication.getCourtApplicationCases().stream()
                        .map(applicationCase -> CourtApplicationCase.courtApplicationCase().withValuesFrom(applicationCase)
                                .withOffences(applicationCase.getOffences().stream()
                                        .map(courtApplicationOffence -> getCourtApplicationOffenceWithConvictionDate(courtApplicationOffence, offenceId, convictionDate))
                                        .collect(toList()))
                                .build())
                        .collect(toList()))
                .withCourtOrder(courtApplication.getCourtOrder() == null ? null : of(courtApplication.getCourtOrder())
                        .map(co -> CourtOrder.courtOrder().withValuesFrom(co)
                                .withCourtOrderOffences(co.getCourtOrderOffences().stream()
                                        .map(o -> getCourtOrderOffenceWithConvictionDate(o, offenceId, convictionDate))
                                        .collect(toList()))
                                .build())
                        .orElse(null))
                .build();
    }
}
