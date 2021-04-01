package uk.gov.moj.cpp.progression.domain.aggregate.utils;


import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import uk.gov.justice.core.courts.Category;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.progression.courts.AddedOffences;
import uk.gov.justice.progression.courts.DeletedOffences;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
import uk.gov.justice.progression.courts.UpdatedOffences;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;

@SuppressWarnings({"squid:S3655", "squid:S1067", "squid:MethodCyclomaticComplexity", "squid:S2234"})
public class DefendantHelper {

    public static final String SEXUAL_OFFENCE_REPORTING_RESTRICTION_LABEL = "Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992";
    public static final String SEXUAL_OFFENCE_REPORTING_RESTRICTION_CODE = "YES";

    DefendantHelper() {
    }


    public static boolean isOffencesUpdated(final List<Offence> commandOffences, final List<Offence> existingOffences) {
        if (!commandOffences.isEmpty() && !existingOffences.isEmpty()) {
            return isAddedOffences(commandOffences, existingOffences) || isDeletedOffences(commandOffences, existingOffences) || isUpdatedOffences(commandOffences, existingOffences);
        }
        return Boolean.TRUE;
    }

    public static boolean isAllDefendantProceedingConcluded(ProsecutionCase prosecutionCase, List<Defendant> updatedDefendants) {
        return prosecutionCase.getDefendants().stream().map(defendant -> {
            final List<Offence> updatedOffences = new ArrayList<>();

            final boolean proceedingConcluded = defendant.getOffences().stream()
                    .map(offence -> getUpdatedOffence(updatedOffences, offence, isConcluded(offence)))
                    .map(Offence::getProceedingsConcluded)
                    .collect(toList()).stream().allMatch(finalCategory -> finalCategory.equals(Boolean.TRUE));

            final Defendant updatedDefendant = getDefendant(defendant, updatedOffences, proceedingConcluded);

            updatedDefendants.add(updatedDefendant);

            return proceedingConcluded;
        }).collect(toList()).stream().allMatch(proceedingConcluded -> proceedingConcluded.equals(Boolean.TRUE));
    }

    private static boolean isConcluded(Offence offence) {
        return isNotEmpty(offence.getJudicialResults()) && offence.getJudicialResults().stream()
                .anyMatch(judicialResult -> judicialResult.getCategory().equals(Category.FINAL));
    }

    private static Defendant getDefendant(Defendant defendant, final List<Offence> updatedOffences, boolean proceedingConcluded) {
        return Defendant.defendant().withValuesFrom(defendant).withOffences(updatedOffences).withProceedingsConcluded(proceedingConcluded).build();
    }

    private static Offence getUpdatedOffence(final List<Offence> updatedOffences, Offence offence, boolean proceedingConcluded) {
        final Offence updatedOffence = Offence.offence().withValuesFrom(offence).withProceedingsConcluded(proceedingConcluded).build();
        updatedOffences.add(updatedOffence);
        return updatedOffence;
    }


    private static boolean isDeletedOffences(final List<Offence> commandOffences, final List<Offence> existingOffences) {
        return !getAddedOffences(existingOffences, commandOffences).isEmpty();
    }

    private static boolean isAddedOffences(final List<Offence> commandOffences, final List<Offence> existingOffences) {
        return !getAddedOffences(commandOffences, existingOffences).isEmpty();
    }

    private static List<Offence> getAddedOffences(final List<Offence> commandOffences, final List<Offence> existingOffences) {
        return commandOffences.stream().filter(commandOffence -> !existingOffences.stream().map(Offence::getId).collect(Collectors.toList()).contains(commandOffence.getId())).collect(Collectors.toList());
    }

    private static boolean isUpdatedOffences(final List<Offence> commandOffences, final List<Offence> existingOffences) {
        return !getUpdatedOffences(commandOffences, existingOffences).isEmpty();
    }

    private static List<Offence> getUpdatedOffences(final List<Offence> commandOffences, final List<Offence> existingOffences) {
        final List<Offence> updatedOffences = new ArrayList<>();
        final List<Offence> commonExistingOffences = existingOffences.stream().filter(existingOffence -> commandOffences.stream().map(Offence::getId).collect(Collectors.toList()).contains(existingOffence.getId())).collect(Collectors.toList());


        final List<Offence> commonCommandOffences = commandOffences.stream().filter(commandOffence -> existingOffences.stream().map(Offence::getId).collect(Collectors.toList()).contains(commandOffence.getId())).collect(Collectors.toList());

        commonExistingOffences.forEach(existingOffence -> commonCommandOffences.forEach(commandOffence -> {
            if (existingOffence.getId().equals(commandOffence.getId()) && isOffenceForDefendantChanged(commandOffence, existingOffence)) {
                updatedOffences.add(commandOffence);
            }
        }));
        return updatedOffences;
    }

    private static boolean isOffenceForDefendantChanged(final Offence commandOffenceForDefendant, final Offence previousOffenceForDefendant) {
        return !(new EqualsBuilder().append(commandOffenceForDefendant.getOffenceCode(), previousOffenceForDefendant.getOffenceCode())
                .append(commandOffenceForDefendant.getWording(), previousOffenceForDefendant.getWording())
                .append(commandOffenceForDefendant.getStartDate(), previousOffenceForDefendant.getStartDate())
                .append(commandOffenceForDefendant.getOffenceTitle(), previousOffenceForDefendant.getOffenceTitle())
                .append(commandOffenceForDefendant.getCount(), previousOffenceForDefendant.getCount())
                .append(commandOffenceForDefendant.getOffenceDateCode(), previousOffenceForDefendant.getOffenceDateCode())
                .append(nonNull(commandOffenceForDefendant.getOffenceTitleWelsh()) ? commandOffenceForDefendant.getOffenceTitleWelsh() : previousOffenceForDefendant.getOffenceTitleWelsh(), previousOffenceForDefendant.getOffenceTitleWelsh())
                .append(nonNull(commandOffenceForDefendant.getOffenceLegislation()) ? commandOffenceForDefendant.getOffenceLegislation() : previousOffenceForDefendant.getOffenceLegislation(), previousOffenceForDefendant.getOffenceLegislation())
                .append(nonNull(commandOffenceForDefendant.getLaaApplnReference()) ? commandOffenceForDefendant.getLaaApplnReference() : previousOffenceForDefendant.getLaaApplnReference(), previousOffenceForDefendant.getLaaApplnReference())
                .append(nonNull(commandOffenceForDefendant.getOffenceLegislationWelsh()) ? commandOffenceForDefendant.getOffenceLegislationWelsh() : previousOffenceForDefendant.getOffenceLegislationWelsh(), previousOffenceForDefendant.getOffenceLegislationWelsh())
                .isEquals()
                && CollectionUtils.isEqualCollection(isNull(commandOffenceForDefendant.getReportingRestrictions()) ? Collections.emptyList() : commandOffenceForDefendant.getReportingRestrictions(),
                isNull(previousOffenceForDefendant.getReportingRestrictions()) ? Collections.emptyList() : previousOffenceForDefendant.getReportingRestrictions())
        );
    }

    public static Offence updateOrderIndex(Offence offence, int orderIndex) {

        return Offence.offence()
                .withAllocationDecision(offence.getAllocationDecision())
                .withAquittalDate(offence.getAquittalDate())
                .withArrestDate(offence.getArrestDate())
                .withChargeDate(offence.getChargeDate())
                .withConvictionDate(offence.getConvictionDate())
                .withCount(offence.getCount())
                .withCustodyTimeLimit(offence.getCustodyTimeLimit())
                .withDateOfInformation(offence.getDateOfInformation())
                .withEndDate(offence.getEndDate())
                .withId(offence.getId())
                .withIndicatedPlea(offence.getIndicatedPlea())
                .withIsDiscontinued(offence.getIsDiscontinued())
                .withIsDisposed(offence.getIsDisposed())
                .withIntroducedAfterInitialProceedings(offence.getIntroducedAfterInitialProceedings())
                .withJudicialResults(offence.getJudicialResults())
                .withLaaApplnReference(offence.getLaaApplnReference())
                .withLaidDate(offence.getLaidDate())
                .withModeOfTrial(offence.getModeOfTrial())
                .withNotifiedPlea(offence.getNotifiedPlea())
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceDefinitionId(offence.getOffenceDefinitionId())
                .withOffenceFacts(offence.getOffenceFacts())
                .withOffenceLegislation(offence.getOffenceLegislation())
                .withOffenceLegislationWelsh(offence.getOffenceLegislationWelsh())
                .withOffenceTitle(offence.getOffenceTitle())
                .withOffenceTitleWelsh(offence.getOffenceTitleWelsh())
                .withOrderIndex(orderIndex)
                .withPlea(offence.getPlea())
                .withProceedingsConcluded(offence.getProceedingsConcluded())
                .withStartDate(offence.getStartDate())
                .withVerdict(offence.getVerdict())
                .withVictims(offence.getVictims())
                .withWording(offence.getWording())
                .withWordingWelsh(offence.getWordingWelsh())
                .withOffenceDateCode(offence.getOffenceDateCode())
                .withCommittingCourt(offence.getCommittingCourt())
                .withReportingRestrictions(offence.getReportingRestrictions())
                .withDvlaOffenceCode(offence.getDvlaOffenceCode())
                .build();

    }

    public static Optional<OffencesForDefendantChanged> getOffencesForDefendantChanged(final List<Offence> offences, final List<Offence> existingOffences, final UUID prosecutionCaseId, final UUID defendantId, final Optional<List<JsonObject>> referenceDataOffences) {
        final List<Offence> offencesAddedList = DefendantHelper.getAddedOffences(offences, existingOffences);
        final OffencesForDefendantChanged.Builder builder = OffencesForDefendantChanged.offencesForDefendantChanged();
        builder.withModifiedDate(LocalDate.now());
        boolean defendantOffencesChanged = false;
        if (!offencesAddedList.isEmpty()) {
            final List<AddedOffences> addedOffences = Arrays.asList(AddedOffences.addedOffences().withProsecutionCaseId(prosecutionCaseId).withDefendantId(defendantId)
                    .withOffences(processReportingRestrictionForAddedOffences(offencesAddedList, referenceDataOffences))
                    .build());
            builder.withAddedOffences(addedOffences);
            defendantOffencesChanged = true;
        }

        final List<Offence> offencesModifiedList = DefendantHelper.getUpdatedOffences(offences, existingOffences);
        if (!offencesModifiedList.isEmpty()) {
            final List<UpdatedOffences> updatedOffences = Arrays.asList(UpdatedOffences.updatedOffences().withDefendantId(defendantId).withProsecutionCaseId(prosecutionCaseId).withOffences(offencesModifiedList).build());
            builder.withUpdatedOffences(updatedOffences);
            defendantOffencesChanged = true;
        }

        final List<Offence> offencesDeletedList = DefendantHelper.getAddedOffences(existingOffences, offences);
        if (!offencesDeletedList.isEmpty()) {
            final List<DeletedOffences> deletedOffences = Arrays.asList(DeletedOffences.deletedOffences().withDefendantId(defendantId).withProsecutionCaseId(prosecutionCaseId).withOffences(offencesDeletedList.stream().map(Offence::getId).collect(Collectors.toList())).build());

            builder.withDeletedOffences(deletedOffences);
            defendantOffencesChanged = true;
        }
        return defendantOffencesChanged ? of(builder.build()) : Optional.empty();
    }

    private static List<Offence> processReportingRestrictionForAddedOffences(final List<Offence> offencesToBeProcessed, final Optional<List<JsonObject>> referenceDataOffences) {
        final List<Offence> offenceDetailListAddWithReportingRestrictions = new ArrayList<>();

        if (referenceDataOffences.isPresent() && isNotEmpty(referenceDataOffences.get())) {
            offencesToBeProcessed.forEach(offence ->
                    offenceDetailListAddWithReportingRestrictions.add(offenceWithSexualOffenceReportingRestriction(offence, referenceDataOffences))
            );

            return offenceDetailListAddWithReportingRestrictions;
        } else {
            return offencesToBeProcessed;
        }
    }

    public static Offence offenceWithSexualOffenceReportingRestriction(final Offence offence, final Optional<List<JsonObject>> referenceDataOffences) {
        final Function<JsonObject, String> offenceKey = offenceJsonObject -> offenceJsonObject.getString("cjsOffenceCode");
        final Map<String, JsonObject> offenceCodeMap = referenceDataOffences.get().stream().collect(Collectors.toMap(offenceKey, Function.identity()));
        final JsonObject referenceDataOffenceInfo = offenceCodeMap.get(offence.getOffenceCode());

        final Offence.Builder builder = Offence.offence().withValuesFrom(offence);

        if (nonNull(referenceDataOffenceInfo) && referenceDataOffenceInfo.containsKey("dvlaCode")) {
            final String dvlaCode = referenceDataOffenceInfo.getString("dvlaCode");
            builder.withDvlaOffenceCode(dvlaCode);
        }
        if (nonNull(referenceDataOffenceInfo) && equalsIgnoreCase(referenceDataOffenceInfo.getString("reportRestrictResultCode", StringUtils.EMPTY), SEXUAL_OFFENCE_REPORTING_RESTRICTION_CODE)) {
            final List<ReportingRestriction> reportingRestrictions = CollectionUtils.isEmpty(offence.getReportingRestrictions()) ? new ArrayList<>() : offence.getReportingRestrictions();
            if (reportingRestrictions.stream().filter(r -> SEXUAL_OFFENCE_REPORTING_RESTRICTION_LABEL.equalsIgnoreCase(r.getLabel())).count() == 0) {
                reportingRestrictions.add(new ReportingRestriction.Builder()
                        .withId(randomUUID())
                        .withLabel(SEXUAL_OFFENCE_REPORTING_RESTRICTION_LABEL)
                        .withOrderedDate(LocalDate.now())
                        .build());
            }
            builder.withReportingRestrictions(reportingRestrictions);
        }

        return builder.build();
    }

    public static Optional<OffencesForDefendantChanged> getOffencesForDefendantUpdated(final List<Offence> offences,
                                                                                       final List<Offence> existingOffences,
                                                                                       final UUID prosecutionCaseId,
                                                                                       final UUID defendantId) {
        final OffencesForDefendantChanged.Builder builder = OffencesForDefendantChanged.offencesForDefendantChanged();
        final List<Offence> updatedOffences = DefendantHelper.getUpdatedOffences(offences, existingOffences);
        if (!updatedOffences.isEmpty()) {
            return buildOffencesForDefendantChanged(prosecutionCaseId, defendantId, builder, updatedOffences);
        }
        return Optional.empty();
    }

    private static Optional<OffencesForDefendantChanged> buildOffencesForDefendantChanged(final UUID prosecutionCaseId,
                                                                                          final UUID defendantId,
                                                                                          final OffencesForDefendantChanged.Builder builder,
                                                                                          final List<Offence> offencesModifiedList) {
        final List<UpdatedOffences> updatedOffences = singletonList(createOffence(prosecutionCaseId, defendantId, offencesModifiedList));
        builder.withUpdatedOffences(updatedOffences);
        builder.withModifiedDate(LocalDate.now());
        return of(builder.build());
    }

    private static UpdatedOffences createOffence(final UUID prosecutionCaseId, final UUID defendantId, final List<Offence> offencesModifiedList) {
        return UpdatedOffences.updatedOffences()
                .withDefendantId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffences(offencesModifiedList)
                .build();
    }

}
