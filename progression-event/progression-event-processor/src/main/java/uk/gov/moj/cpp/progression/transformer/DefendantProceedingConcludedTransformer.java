package uk.gov.moj.cpp.progression.transformer;

import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.progression.courts.api.ApplicationConcluded;
import uk.gov.justice.progression.courts.api.ProsecutionConcludedForLAA;
import uk.gov.justice.progression.courts.exract.OffenceSummary;
import uk.gov.justice.progression.courts.exract.Plea;
import uk.gov.justice.progression.courts.exract.ProsecutionConcluded;
import uk.gov.justice.progression.courts.exract.Verdict;
import uk.gov.justice.progression.courts.exract.VerdictType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;

@SuppressWarnings({"squid:S3655"})
public class DefendantProceedingConcludedTransformer {

    public ProsecutionConcludedForLAA getProsecutionConcludedRequest(final List<Defendant> defendant, final UUID prosecutionCaseId, final UUID hearingId) {
        final List<ProsecutionConcluded> prosecutionConcludedArrayList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(defendant)) {
            defendant.stream().forEach(def -> {
                final List<OffenceSummary> concludedOffenceSummaryArrayList = new ArrayList<>();
                def.getOffences().forEach(offence -> concludedOffenceSummaryArrayList.add(getOffenceSummary(offence, hearingId)));
                final ProsecutionConcluded prosecutionConcluded = ProsecutionConcluded.prosecutionConcluded()
                        .withDefendantId(def.getId())
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withHearingIdWhereChangeOccurred(hearingId)
                        .withIsConcluded(def.getProceedingsConcluded())
                        .withOffenceSummary(concludedOffenceSummaryArrayList)
                        .build();
                prosecutionConcludedArrayList.add(prosecutionConcluded);
            });
        }
        return ProsecutionConcludedForLAA.prosecutionConcludedForLAA().withProsecutionConcluded(prosecutionConcludedArrayList).build();
    }

    public ProsecutionConcludedForLAA getApplicationConcludedRequest(final CourtApplication courtApplication, final UUID hearingId) {
        final ProsecutionConcluded prosecutionConcluded = ProsecutionConcluded.prosecutionConcluded()
                .withHearingIdWhereChangeOccurred(hearingId)
                .withIsConcluded(courtApplication.getProceedingsConcluded())
                .withOffenceSummary(getOffenceSummary(courtApplication, hearingId))
                .withApplicationConcluded(ApplicationConcluded.applicationConcluded()
                        .withApplicationId(courtApplication.getId())
                        .withApplicationResultCode(courtApplication.getApplicationResultCodeForLaa())
                        .withSubjectId(courtApplication.getSubject().getId())
                        .build())
                .build();
        return ProsecutionConcludedForLAA.prosecutionConcludedForLAA().withProsecutionConcluded(singletonList(prosecutionConcluded)).build();
    }

    private OffenceSummary getOffenceSummary(final Offence offence, final UUID hearingId) {

        final List<LocalDate> orderedDateList = isNotEmpty(offence.getJudicialResults()) ? offence.getJudicialResults().stream()
                .map(JudicialResult::getOrderedDate)
                .collect(toList()) : Collections.emptyList();
        final LocalDate latestOrderedDate = isNotEmpty(orderedDateList) ? orderedDateList.stream().max(Comparator.comparing(LocalDate::toEpochDay)).get() : null;

        final OffenceSummary.Builder offenceSummaryBuilder = OffenceSummary.offenceSummary()
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceId(offence.getId())
                .withProceedingsConcluded(offence.getProceedingsConcluded())
                .withProceedingsConcludedChangedDate(latestOrderedDate);

        if (nonNull(offence.getPlea())) {
            offenceSummaryBuilder.withPlea(Plea.plea()
                    .withOriginatingHearingId(hearingId)
                    .withPleaDate(offence.getPlea().getPleaDate().toString())
                    .withValue(offence.getPlea().getPleaValue())
                    .build());
        }

        if (nonNull(offence.getVerdict())) {
            offenceSummaryBuilder.withVerdict(Verdict.verdict()
                    .withOriginatingHearingId(hearingId)
                    .withVerdictDate(offence.getVerdict().getVerdictDate().toString())
                    .withVerdictType(VerdictType.verdictType()
                            .withCategory(offence.getVerdict().getVerdictType().getCategory())
                            .withCategoryType(offence.getVerdict().getVerdictType().getCategoryType())
                            .withDescription(offence.getVerdict().getVerdictType().getDescription())
                            .withSequence(offence.getVerdict().getVerdictType().getSequence())
                            .withVerdictTypeId(offence.getVerdict().getVerdictType().getId())
                            .build())
                    .build());
        }

        return offenceSummaryBuilder.build();
    }

    private List<OffenceSummary> getOffenceSummary(final CourtApplication courtApplication, final UUID hearingId) {
        if (isEmpty(courtApplication.getCourtApplicationCases())) {
            return Collections.emptyList();
        }
        return courtApplication.getCourtApplicationCases().stream()
                .map(CourtApplicationCase::getOffences)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(offence -> getOffenceSummary(offence, hearingId))
                .toList();
    }


}
