package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.ApplicationExternalCreatorType;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.progression.query.laa.ApplicationLaa;
import uk.gov.justice.progression.query.laa.LaaApplnReference;
import uk.gov.justice.progression.query.laa.LinkedApplications;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils.CourtApplicationSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;

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
    @Inject
    private LaaApplnReferenceConverter laaApplnReferenceConverter;

    public ApplicationLaa convert(final CourtApplication courtApplication, final List<Hearing> hearingList, final String laaApplicationShortId, final List<CourtApplicationSummary> courtApplicationSummaryList) {
        final ApplicationLaa.Builder applicationLaaBuilder = ApplicationLaa.applicationLaa()
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
                .withApplicationTypeId(courtApplication.getType().getId())
                .withAllegationOrComplaintStartDate(ofNullable(courtApplication.getAllegationOrComplaintStartDate()).map(LocalDate::toString).orElse(null))
                .withLinkedApplications(getLinkedApplications(courtApplicationSummaryList));

        final LaaApplnReference laaApplnReference = laaApplnReferenceConverter.convert(courtApplication.getLaaApplnReference());

        if(nonNull(laaApplnReference)) {
            applicationLaaBuilder.withLaaApplnReference(laaApplnReference);
        }

        return applicationLaaBuilder.build();
    }

    private static List<LinkedApplications> getLinkedApplications(final List<CourtApplicationSummary> courtApplicationSummaryList) {

        if (CollectionUtils.isEmpty(courtApplicationSummaryList)) {
            return null;
        }

        return courtApplicationSummaryList
                .stream()
                .map(courtApplicationSummary
                        -> LinkedApplications.linkedApplications()
                        .withApplicationId(UUID.fromString(courtApplicationSummary.getApplicationId()))
                        .withApplicationStatus(courtApplicationSummary.getApplicationStatus())
                        .withApplicationTitle(courtApplicationSummary.getApplicationTitle())
                        .withApplicantDisplayName(courtApplicationSummary.getApplicantDisplayName())
                        .withApplicationReference(courtApplicationSummary.getApplicationReference())
                        .withIsAppeal(courtApplicationSummary.getIsAppeal())
                        .withRemovalReason(courtApplicationSummary.getRemovalReason())
                        .withRespondentDisplayNames(courtApplicationSummary.getRespondentDisplayNames())
                        .build())
                .toList();
    }

}
