package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.progression.query.laa.ApplicationLaa;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;
@SuppressWarnings("squid:S1168")
public class ApplicationLaaConverter extends LAAConverter {

    @Inject
    private CaseSummaryLaaConverter caseSummaryLaaConverter;
    @Inject
    private HearingSummaryLaaConverter hearingSummaryLaaConverter;
    @Inject
    private SubjectSummaryLaaConverter subjectSummaryLaaConverter;
    @Inject
    private JudicialResultsConverter judicialResultsConverter;

    public ApplicationLaa convert(final CourtApplication courtApplication, final List<Hearing> hearingList, final String laaApplicationShortId) {
        return ApplicationLaa.applicationLaa()
                .withApplicationId(courtApplication.getId())
                .withApplicationReference(courtApplication.getApplicationReference())
                .withApplicationStatus(ofNullable(courtApplication.getApplicationStatus()).map(ApplicationStatus::toString).orElse(null))
                .withApplicationTitle(courtApplication.getType().getType())
                .withApplicationType(courtApplication.getType().getCode())
                .withReceivedDate(ofNullable(courtApplication.getApplicationReceivedDate())
                        .map(LocalDate::toString)
                        .orElse(null))
                .withCaseSummary(caseSummaryLaaConverter.convert(courtApplication))
                .withHearingSummary(hearingSummaryLaaConverter.convert(hearingList))
                .withSubjectSummary(subjectSummaryLaaConverter.convert(courtApplication, hearingList))
                .withJudicialResults(judicialResultsConverter.convert(courtApplication.getJudicialResults()))
                .withLaaApplicationShortId(laaApplicationShortId)
                .build();
    }

}
