package uk.gov.moj.cpp.progression.domain.aggregate.utils;


import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupReportingRestrictions;

import uk.gov.justice.core.courts.CivilOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.progression.courts.AddedOffences;
import uk.gov.justice.progression.courts.DeletedOffences;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
import uk.gov.justice.progression.courts.UpdatedOffences;
import uk.gov.moj.cpp.progression.events.CustodialEstablishment;
import uk.gov.moj.cpp.progression.events.MatchedDefendants;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.progression.plea.json.schemas.TemplateType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;

@SuppressWarnings({"squid:S3655", "squid:S1067", "squid:MethodCyclomaticComplexity", "squid:S2234", "squid:S1188", "squid:S1066"})
public class DefendantHelper {

    public static final String SEXUAL_OFFENCE_REPORTING_RESTRICTION_LABEL = "Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992";
    public static final String SEXUAL_OFFENCE_REPORTING_RESTRICTION_CODE = "YES";
    public static final String GUILTY = "GUILTY";
    public static final String NOT_GUILTY = "NOT_GUILTY";

    DefendantHelper() {
    }


    public static boolean isOffencesUpdated(final List<Offence> commandOffences, final List<Offence> existingOffences) {
        if (!commandOffences.isEmpty() && !existingOffences.isEmpty()) {
            return isAddedOffences(commandOffences, existingOffences) || isDeletedOffences(commandOffences, existingOffences) || isUpdatedOffences(commandOffences, existingOffences);
        }
        return TRUE;
    }


    public static boolean hearingCaseDefendantsProceedingsConcluded(final ProsecutionCase prosecutionCase) {
        return getUpdatedDefendants(prosecutionCase).stream()
                .allMatch(defendant -> TRUE.equals(defendant.getProceedingsConcluded()));
    }

    public static boolean isAllDefendantProceedingConcluded(final ProsecutionCase prosecutionCase, final List<Defendant> updatedDefendants) {
        return prosecutionCase.getDefendants().stream().map(defendant -> {
            final List<Offence> updatedOffences = new ArrayList<>();
            final boolean proceedingConcluded = defendant.getOffences().stream()
                        .map(offence -> getUpdatedOffence(updatedOffences, offence, isConcluded(offence)))
                        .map(Offence::getProceedingsConcluded)
                        .collect(toList()).stream().allMatch(finalCategory -> finalCategory.equals(Boolean.TRUE));

            final Defendant updatedDefendant = getDefendant(defendant, updatedOffences, proceedingConcluded);
            updatedDefendants.add(updatedDefendant);

            return proceedingConcluded;
        }).collect(toList()).stream().allMatch(proceedingConcluded -> proceedingConcluded.equals(TRUE));
    }

    public static List<Defendant> getUpdatedDefendants(final ProsecutionCase prosecutionCase) {
        return prosecutionCase.getDefendants().stream()
                .map(DefendantHelper::getUpdatedDefendant)
                .collect(Collectors.toList());
    }

    public static ProsecutionCase getUpdatedDefendantWithMasterDefendantId(final ProsecutionCase prosecutionCase, final Defendant defendant, final List<MatchedDefendants> matchedDefendantsList) {
        final Optional<Defendant> defendantInCase =  prosecutionCase.getDefendants().stream()
                .filter(def ->  def.getId().equals(defendant.getId()))
                 .findFirst();
        if(defendantInCase.isPresent()) {
            final MatchedDefendants masterDefendant =  getMasterDefendant(matchedDefendantsList);
            if(null != masterDefendant) {
                final Defendant updatedDefendant = Defendant.defendant().withValuesFrom(defendant).withMasterDefendantId(masterDefendant.getMasterDefendantId()).build();
                prosecutionCase.getDefendants().removeIf(x -> x.getId().equals(defendant.getId()));
                prosecutionCase.getDefendants().add(updatedDefendant);
            }
        }
        return prosecutionCase;
    }

    public static MatchedDefendants getMasterDefendant(final List<MatchedDefendants> matchedDefendants) {
        final Comparator<MatchedDefendants> comparator = comparing(MatchedDefendants::getCourtProceedingsInitiated);
        return matchedDefendants.stream()
                .filter(def -> nonNull(def.getCourtProceedingsInitiated()))
                .min(comparator)
                .orElse(null);
    }

    private static Defendant getUpdatedDefendant(final Defendant defendant) {
        final List<Offence> updatedOffences = new ArrayList<>();
        final boolean proceedingConcluded = defendant.getOffences().stream()
                    .map(offence -> getUpdatedOffence(updatedOffences, offence, isConcluded(offence)))
                    .map(Offence::getProceedingsConcluded)
                    .collect(toList()).stream().allMatch(finalCategory -> finalCategory.equals(TRUE));
        return getDefendant(defendant, updatedOffences, proceedingConcluded);
    }

    public static void updatedDefendantsWithProceedingConcludedState(ProsecutionCase inputProsecutionCase, Map<UUID, List<uk.gov.justice.core.courts.Offence>> offenceProceedingConcluded, List<Defendant> updatedDefendantsForProceedingsConcludedEvent, List<DefendantJudicialResult> hearingDefendantJudicialResults) {
        final List<uk.gov.justice.core.courts.Defendant> updatedDefendants = new ArrayList<>();
        getDefendantsWithLAAAndProceedingConcluded(inputProsecutionCase, updatedDefendants, offenceProceedingConcluded, hearingDefendantJudicialResults);
        updateOffencesWithProceedingConcludedState(offenceProceedingConcluded, updatedDefendants, updatedDefendantsForProceedingsConcludedEvent);
    }

    public static List<Offence> getAllDefendantsOffences(final List<Defendant> defendants) {
        if (isNull(defendants) || defendants.isEmpty()) {
            return emptyList();
        }

        return defendants.stream()
                .filter(defendant -> nonNull(defendant.getOffences()))
                .flatMap(defendant -> defendant.getOffences().stream())
                .collect(Collectors.toList());
    }

    public static List<DefendantJudicialResult> getDefendantJudicialResultsOfDefendantsAssociatedToTheCase(final List<Defendant> defendants,
                                                                                                           final List<DefendantJudicialResult> defendantJudicialResults) {
        if (isEmpty(defendants) || isEmpty(defendantJudicialResults)) {
            return emptyList();
        }

        final List<UUID> masterDefendantIdList = defendants.stream()
                .map(Defendant::getMasterDefendantId)
                .filter(Objects::nonNull)
                .collect(toList());

        return defendantJudicialResults.stream()
                .filter(djr -> nonNull(djr.getMasterDefendantId()))
                .filter(djr -> masterDefendantIdList.contains(djr.getMasterDefendantId()))
                .collect(Collectors.toList());
    }

    private static void getDefendantsWithLAAAndProceedingConcluded(ProsecutionCase prosecutionCase, List<Defendant> updatedDefendants, Map<UUID, List<uk.gov.justice.core.courts.Offence>> offenceProceedingConcluded, final List<DefendantJudicialResult> hearingDefendantJudicialResults) {
        final List<JudicialResult> defendantCaseJudicialResults = prosecutionCase.getDefendants().stream()
                        .flatMap(defendant -> ofNullable(defendant.getDefendantCaseJudicialResults()).map(Collection::stream).orElseGet(Stream::empty))
                                .collect(toList());
        prosecutionCase.getDefendants().stream().forEach(existingDefendant -> {
            final List<Offence> updatedOffences = new ArrayList<>();
            final List<Offence> caseOffences = offenceProceedingConcluded.get(existingDefendant.getId());
            if (hasLaaReference(existingDefendant) || hasLaaReference(caseOffences)) {
                final boolean proceedingConcluded = existingDefendant.getOffences().stream()
                        .map(existingOffence -> getUpdatedOffence(updatedOffences, existingOffence, isConcluded(existingOffence, hearingDefendantJudicialResults, defendantCaseJudicialResults)))
                        .map(Offence::getProceedingsConcluded)
                        .collect(toList()).stream().allMatch(finalCategory -> ((Boolean.TRUE).equals(finalCategory)));
                final Defendant updatedDefendant = Defendant.defendant().withValuesFrom(existingDefendant).withOffences(updatedOffences).withProceedingsConcluded(proceedingConcluded).build();
                updatedDefendants.add(updatedDefendant);

            }
        });
    }

    public static void updateOffencesWithProceedingConcludedState(Map<UUID, List<uk.gov.justice.core.courts.Offence>> defendantCaseOffences, List<Defendant> updatedDefendants, List<Defendant> updatedDefendantsWithPreviousProceedingChange) {
        if (isNotEmpty(updatedDefendants) && !defendantCaseOffences.isEmpty()) {

            updatedDefendants.stream().forEach(defendant -> {
                final List<Offence> caseOffencesStateList = defendantCaseOffences.get(defendant.getId());
                if (isNotEmpty(caseOffencesStateList)) {
                    final List<Offence> updatedOffences = new ArrayList<>();
                    getUpdatedOffencesForProceedingConcluded(caseOffencesStateList, defendant, updatedOffences);
                    if( updatedOffences.isEmpty()) { //DD-34308: If there are no matching prev offences, then copy over new offences.
                        updatedOffences.addAll(defendant.getOffences());
                    }
                    final Defendant updatedDef = Defendant.defendant().withValuesFrom(defendant).withOffences(updatedOffences.stream().distinct().collect(toList())).build();
                    updatedDefendantsWithPreviousProceedingChange.add(updatedDef);
                } else {
                    updatedDefendantsWithPreviousProceedingChange.addAll(updatedDefendants);
                }

            });
        }
    }

    private static void getUpdatedOffencesForProceedingConcluded(final List<Offence> caseOffencesStateList, final Defendant defendant, final List<Offence> updatedOffences) {
        caseOffencesStateList.stream().forEach(previousOffenceState -> {
            if (isNotEmpty(previousOffenceState.getJudicialResults())) {
                defendant.getOffences().stream().forEach(offence -> {
                    if (offence.getId().equals(previousOffenceState.getId())) {
                        getOffenceWithProceedingConcluded(updatedOffences, offence, previousOffenceState);
                    } else if (defendant.getOffences().stream().noneMatch(offence1 -> offence1.getId().equals(previousOffenceState.getId()))) {
                        updateOffenceMap(updatedOffences, previousOffenceState);
                        updatedOffences.add(previousOffenceState);
                    }
                });
            } else {
                defendant.getOffences().stream()
                        .filter(inputOffence -> inputOffence.getId().equals(previousOffenceState.getId()))
                        .map(updatedOffences::add)
                        .collect(toList());

            }
        });
    }

    private static Offence getOffenceWithProceedingConcluded(final List<Offence> updatedOffences, final Offence existingOffence, final Offence previousOffence) {
        final Offence.Builder offenceBuilder = Offence.offence();
        offenceBuilder.withValuesFrom(existingOffence);
        updateOffenceMap(updatedOffences, previousOffence);
        updatedOffences.add(offenceBuilder.build());
        return offenceBuilder.build();
    }

    private static void updateOffenceMap(final List<Offence> updatedOffences, final Offence previousOffence) {
        if (updatedOffences.stream().anyMatch(offence -> offence.getId().equals(previousOffence.getId()))) {
            final int index = IntStream.range(0, updatedOffences.size())
                    .filter(i -> updatedOffences.get(i).getId().equals(previousOffence.getId()))
                    .findFirst().orElse(-1);
            if (index >= 0) {
                updatedOffences.remove(index);
            }
        }
    }

    public static boolean isProceedingConcludedEventTriggered(Defendant defendant, Map<UUID, List<uk.gov.justice.core.courts.Offence>> offenceProceedingConcluded, Map<UUID, Boolean> defendantProceedingConcluded) {
        return nonNull(defendant.getProceedingsConcluded() && (!defendant.getProceedingsConcluded().equals(defendantProceedingConcluded.get(defendant.getId())) || isAnyChangeInProceedingConcludedFromPreviousState(defendant.getOffences(), offenceProceedingConcluded.get(defendant.getId()))));
    }

    private static boolean isAnyChangeInProceedingConcludedFromPreviousState(List<Offence> existingOffence, List<Offence> previousOffence) {
        boolean isChanged = false;
        if (isNotEmpty(previousOffence) && isNotEmpty(existingOffence)) {
            final List<Offence> offenceList = previousOffence.stream()
                    .filter(prevOffence -> existingOffence.stream()
                            .anyMatch(existOffence -> existOffence.getId().equals(prevOffence.getId()) && nonNull(existOffence.getProceedingsConcluded()) && nonNull(prevOffence.getProceedingsConcluded()) && !(existOffence.getProceedingsConcluded().equals(prevOffence.getProceedingsConcluded()))))
                    .collect(Collectors.toList());
            isChanged = isNotEmpty(offenceList);
        }


        return isChanged;
    }

    public static boolean isConcluded(final Offence offence) {
        return isNotEmpty(offence.getJudicialResults()) && offence.getJudicialResults().stream()
                .anyMatch(judicialResult -> judicialResult.getCategory().equals(JudicialResultCategory.FINAL));
    }

    public static boolean isConcluded(final Offence offence, final List<DefendantJudicialResult> defendantJudicialResults, final List<JudicialResult> defendantCaseJudicialResults) {
        final List<JudicialResult> caseJudicialResultsForOffence = ofNullable(defendantCaseJudicialResults).map(Collection::stream).orElseGet(Stream::empty)
                .filter(judicialResult -> nonNull(judicialResult) && offence.getId().equals(judicialResult.getOffenceId()))
                .collect(toList());

        final List<JudicialResult> defendantJudicialResultsForTheOffence = ofNullable(defendantJudicialResults).map(Collection::stream).orElseGet(Stream::empty)
                .map(DefendantJudicialResult::getJudicialResult)
                .filter(judicialResult -> nonNull(judicialResult) && offence.getId().equals(judicialResult.getOffenceId()))
                .collect(toList());

        final Optional<Boolean> caseLevelConcluded = isEmpty(caseJudicialResultsForOffence) ? empty() : of(caseJudicialResultsForOffence.stream()
                .anyMatch(judicialResult -> JudicialResultCategory.FINAL.equals(judicialResult.getCategory())));

        final Optional<Boolean> defendantLevelConcluded = isEmpty(defendantJudicialResultsForTheOffence) ? empty() : of(defendantJudicialResultsForTheOffence.stream()
                .anyMatch(judicialResult -> JudicialResultCategory.FINAL.equals(judicialResult.getCategory())));

        final Optional<Boolean> offenceLevelConcluded = isEmpty(offence.getJudicialResults()) ? empty() : of(offence.getJudicialResults().stream()
                .anyMatch(judicialResult -> JudicialResultCategory.FINAL.equals(judicialResult.getCategory())));

        if (!offenceLevelConcluded.isPresent() && !defendantLevelConcluded.isPresent() && !caseLevelConcluded.isPresent()) {
            return false;
        }

        final AtomicBoolean result = new AtomicBoolean(true);
        offenceLevelConcluded.ifPresent(b -> result.set(result.get() && b));
        defendantLevelConcluded.ifPresent(b -> result.set(result.get() && b));
        caseLevelConcluded.ifPresent(b -> result.set(result.get() && b));

        return result.get();
    }

    public static boolean hasNewAmendment(final Offence offence) {
        return nonNull(offence) && isNotEmpty(offence.getJudicialResults()) && offence.getJudicialResults().stream()
                .anyMatch(judicialResult -> Boolean.TRUE.equals(judicialResult.getIsNewAmendment()));
    }

    public static Defendant getDefendant(final Defendant defendant, final List<Offence> updatedOffences, boolean proceedingConcluded) {
        return Defendant.defendant().withValuesFrom(defendant).withOffences(updatedOffences).withProceedingsConcluded(proceedingConcluded).build();
    }

    public static Defendant getDefendant(final Defendant defendant, final List<Offence> updatedOffences, boolean proceedingConcluded, CustodialEstablishment custodialEstablishmentFromMap) {

        if (nonNull(custodialEstablishmentFromMap)) {
            final uk.gov.justice.core.courts.CustodialEstablishment.Builder custodialEstablishmentBuilder = uk.gov.justice.core.courts.CustodialEstablishment.custodialEstablishment();
            custodialEstablishmentBuilder.withCustody(custodialEstablishmentFromMap.getCustody())
                    .withId(custodialEstablishmentFromMap.getId())
                    .withName(custodialEstablishmentFromMap.getName());
            final PersonDefendant.Builder updatedPersonDefendant = PersonDefendant.personDefendant();
            updatedPersonDefendant.withValuesFrom(defendant.getPersonDefendant())
                    .withCustodialEstablishment(custodialEstablishmentBuilder.build());

            final Defendant.Builder defendantBuilder = Defendant.defendant();
            defendantBuilder
                    .withValuesFrom(defendant)
                    .withOffences(updatedOffences)
                    .withProceedingsConcluded(proceedingConcluded)
                    .withPersonDefendant(updatedPersonDefendant.build());
            return defendantBuilder.build();
        }
        return getDefendant(defendant, updatedOffences, proceedingConcluded);
    }

    public static Offence getUpdatedOffence(final List<Offence> updatedOffences, Offence existingOffence, boolean proceedingConcluded) {
        final Offence updatedOffence = Offence.offence().withValuesFrom(existingOffence).withProceedingsConcluded(proceedingConcluded).build();
        updatedOffences.add(updatedOffence);
        return updatedOffence;
    }

    private static boolean hasLaaReference(Defendant defendant) {
        return nonNull(defendant)
                && hasLaaReference(defendant.getOffences());
    }

    private static boolean hasLaaReference(final List<Offence> offences) {
        return isNotEmpty(offences)
                && offences.stream().anyMatch(offence -> Objects.nonNull(offence.getLaaApplnReference()));
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
                .append(commandOffenceForDefendant.getConvictingCourt(), previousOffenceForDefendant.getConvictingCourt())
                .append(commandOffenceForDefendant.getOffenceDateCode(), previousOffenceForDefendant.getOffenceDateCode())
                .append(nonNull(commandOffenceForDefendant.getOffenceTitleWelsh()) ? commandOffenceForDefendant.getOffenceTitleWelsh() : previousOffenceForDefendant.getOffenceTitleWelsh(), previousOffenceForDefendant.getOffenceTitleWelsh())
                .append(nonNull(commandOffenceForDefendant.getOffenceLegislation()) ? commandOffenceForDefendant.getOffenceLegislation() : previousOffenceForDefendant.getOffenceLegislation(), previousOffenceForDefendant.getOffenceLegislation())
                .append(nonNull(commandOffenceForDefendant.getLaaApplnReference()) ? commandOffenceForDefendant.getLaaApplnReference() : previousOffenceForDefendant.getLaaApplnReference(), previousOffenceForDefendant.getLaaApplnReference())
                .append(nonNull(commandOffenceForDefendant.getOffenceLegislationWelsh()) ? commandOffenceForDefendant.getOffenceLegislationWelsh() : previousOffenceForDefendant.getOffenceLegislationWelsh(), previousOffenceForDefendant.getOffenceLegislationWelsh())
                .append(nonNull(commandOffenceForDefendant.getOffenceFacts()) ? commandOffenceForDefendant.getOffenceFacts() : previousOffenceForDefendant.getOffenceFacts(), previousOffenceForDefendant.getOffenceFacts())
                .isEquals()
                && CollectionUtils.isEqualCollection(isNull(commandOffenceForDefendant.getReportingRestrictions()) ? Collections.emptyList() : commandOffenceForDefendant.getReportingRestrictions(),
                isNull(previousOffenceForDefendant.getReportingRestrictions()) ? Collections.emptyList() : previousOffenceForDefendant.getReportingRestrictions())
        );
    }

    public static Offence updateOrderIndexAndExparteValue(final Offence offence, final int orderIndex, final Optional<List<JsonObject>> referenceDataOffences, final boolean isCivil) {
        final List<JsonObject> matchingRefOffences = referenceDataOffences.orElse(new ArrayList<>());
        final Optional<JsonObject> matchedOffence = matchingRefOffences.stream().filter(jsonOffence -> getString(jsonOffence, "cjsOffenceCode").orElse("").equals(offence.getOffenceCode())).findFirst();
        final Offence.Builder offence1 = Offence.offence();
        offence1.withValuesFrom(offence);
        matchedOffence.ifPresent(off -> {
            offence1.withMaxPenalty(off.getString("maxPenalty", EMPTY));
            if (isCivil && isNull(offence.getCivilOffence())) {
                offence1.withCivilOffence(getCivilOffence(getExparteValueFromRefDataOffenceJsonObject(matchedOffence)));
            }
        });
        return offence1
                .withOrderIndex(orderIndex)
                .build();

    }

    public static Optional<OffencesForDefendantChanged> getOffencesForDefendantChanged(final List<Offence> offences, final List<Offence> existingOffences, final UUID prosecutionCaseId, final UUID defendantId, final Optional<List<JsonObject>> referenceDataOffences, final boolean isCivil) {
        final List<Offence> offencesAddedList = DefendantHelper.getAddedOffences(offences, existingOffences);
        final OffencesForDefendantChanged.Builder builder = OffencesForDefendantChanged.offencesForDefendantChanged();
        builder.withModifiedDate(LocalDate.now());
        boolean defendantOffencesChanged = false;
        if (!offencesAddedList.isEmpty()) {
            final List<AddedOffences> addedOffences = asList(AddedOffences.addedOffences().withProsecutionCaseId(prosecutionCaseId).withDefendantId(defendantId)
                    .withOffences(processReportingRestrictionForAddedOffences(offencesAddedList, referenceDataOffences, isCivil))
                    .build());
            builder.withAddedOffences(addedOffences);
            defendantOffencesChanged = true;
        }

        final List<Offence> offencesModifiedList = DefendantHelper.getUpdatedOffences(offences, existingOffences);
        if (!offencesModifiedList.isEmpty()) {
            final List<UpdatedOffences> updatedOffences = asList(UpdatedOffences.updatedOffences().withDefendantId(defendantId).withProsecutionCaseId(prosecutionCaseId).withOffences(offencesModifiedList).build());
            builder.withUpdatedOffences(updatedOffences);
            defendantOffencesChanged = true;
        }


        return defendantOffencesChanged ? of(builder.build()) : empty();
    }

    private static List<Offence> processReportingRestrictionForAddedOffences(final List<Offence> offencesToBeProcessed, final Optional<List<JsonObject>> referenceDataOffences, final boolean isCivil) {
        final List<Offence> offenceDetailListAddWithReportingRestrictions = new ArrayList<>();

        if (referenceDataOffences.isPresent() && isNotEmpty(referenceDataOffences.get())) {
            offencesToBeProcessed.forEach(offence ->
                    offenceDetailListAddWithReportingRestrictions.add(offenceWithSexualOffenceReportingRestrictionAndExparteValue(offence, referenceDataOffences, isCivil))
            );

            return offenceDetailListAddWithReportingRestrictions;
        } else {
            return offencesToBeProcessed;
        }
    }

    public static Offence offenceWithSexualOffenceReportingRestrictionAndExparteValue(final Offence offence, final Optional<List<JsonObject>> referenceDataOffences, final boolean isCivil) {
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
            builder.withReportingRestrictions(dedupReportingRestrictions(reportingRestrictions));
        }

        if (isCivil && isNull(offence.getCivilOffence()) && nonNull(referenceDataOffenceInfo)) {
            builder.withCivilOffence(getCivilOffence(getExparteValueFromRefDataOffenceJsonObject(Optional.of(referenceDataOffenceInfo))));
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
        return empty();
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


    public static List<uk.gov.justice.core.courts.Defendant> getUpdatedDefendantsForOnlinePlea(final List<uk.gov.justice.core.courts.Defendant> allDefendants, final UUID defendantId, final List<uk.gov.moj.cpp.progression.plea.json.schemas.Offence> pleadOffences) {
        return allDefendants.stream()
                .map(defendant -> defendant.getId().equals(defendantId) ? getUpdatedDefendantForOnlinePlea(defendant, pleadOffences) : defendant)
                .collect(Collectors.toList());
    }

    private static uk.gov.justice.core.courts.Defendant getUpdatedDefendantForOnlinePlea(final uk.gov.justice.core.courts.Defendant defendant, final List<uk.gov.moj.cpp.progression.plea.json.schemas.Offence> pleadOffences) {
        return uk.gov.justice.core.courts.Defendant.defendant()
                .withValuesFrom(defendant)
                .withOffences(getUpdatedOffencesForOnlinePlea(defendant.getOffences(), pleadOffences))
                .build();
    }

    private static List<uk.gov.justice.core.courts.Offence> getUpdatedOffencesForOnlinePlea(List<uk.gov.justice.core.courts.Offence> existingOffences, final List<uk.gov.moj.cpp.progression.plea.json.schemas.Offence> pleadOffences) {
        return existingOffences.stream()
                .map(existingOffence -> isOffencePlead(existingOffence, pleadOffences) ? getUpdatedOffenceForOnlinePlea(existingOffence) : existingOffence)
                .collect(Collectors.toList());
    }

    private static uk.gov.justice.core.courts.Offence getUpdatedOffenceForOnlinePlea(final uk.gov.justice.core.courts.Offence existingOffence) {
        return uk.gov.justice.core.courts.Offence.offence()
                .withValuesFrom(existingOffence)
                .withOnlinePleaReceived(true)
                .build();
    }

    private static boolean isOffencePlead(final uk.gov.justice.core.courts.Offence existingOffence, final List<uk.gov.moj.cpp.progression.plea.json.schemas.Offence> pleadOffences) {
        return pleadOffences.stream().anyMatch(offence -> offence.getId().equals(existingOffence.getId().toString()));
    }

    public static Optional<TemplateType> sendEmailNotificationToDefendant(final PleadOnline pleadOnline) {
        boolean soleOffence = false;
        if (nonNull(pleadOnline)) {
            final List<uk.gov.moj.cpp.progression.plea.json.schemas.Offence> offences = pleadOnline.getOffences();
            if (nonNull(offences)) {
                if (offences.size() == 1) {
                    soleOffence = true;
                }
                return getTemplateType(pleadOnline, soleOffence, offences);
            }
        }
        return empty();
    }

    private static Optional<TemplateType> getTemplateType(final PleadOnline pleadOnline, final boolean soleOffence, final List<uk.gov.moj.cpp.progression.plea.json.schemas.Offence> offences) {
        if (isOnlineGuiltyPleaCourtHearing(pleadOnline, soleOffence, offences)) {
            return of(TemplateType.ONLINEGUILTYPLEACOURTHEARING);
        }
        if (isOnlineNotGuiltyPlea(soleOffence, offences)) {
            return of(TemplateType.ONLINENOTGUILTYPLEA);
        }
        if (isOnlineGuiltyPleaNoCourtHearing(pleadOnline, soleOffence, offences)) {
            return of(TemplateType.ONLINEGUILTYPLEANOCOURTHEARING);
        }
        return empty();
    }

    private static boolean isOnlineGuiltyPleaNoCourtHearing(PleadOnline pleadOnline, boolean soleOffence, List<uk.gov.moj.cpp.progression.plea.json.schemas.Offence> offences) {
        return (nonNull(pleadOnline.getComeToCourt()) && !(pleadOnline.getComeToCourt())) && ((soleOffence && offences.get(0).getPlea().toString().equals(GUILTY)) || (!soleOffence && offences.stream().map(e -> (e.getPlea().toString())).allMatch(e -> e.equals(GUILTY))));
    }

    private static boolean isOnlineNotGuiltyPlea(boolean soleOffence, List<uk.gov.moj.cpp.progression.plea.json.schemas.Offence> offences) {
        return soleOffence && offences.get(0).getPlea().toString().equals(NOT_GUILTY) || !soleOffence && offences.stream().map(e -> (e.getPlea().toString())).allMatch(e -> e.equals(NOT_GUILTY));
    }

    private static boolean isOnlineGuiltyPleaCourtHearing(PleadOnline pleadOnline, boolean soleOffence, List<uk.gov.moj.cpp.progression.plea.json.schemas.Offence> offences) {
        return (nonNull(pleadOnline.getComeToCourt()) && pleadOnline.getComeToCourt()) && ((soleOffence && (offences.get(0).getPlea().toString().equals(GUILTY))) || (!soleOffence && offences.stream().map(e -> (e.getPlea().toString())).anyMatch(e -> e.equals(GUILTY))));
    }

    public static String getDefendantEmail(final PleadOnline pleadOnline) {
        if (nonNull(pleadOnline.getLegalEntityDefendant())
                && nonNull(pleadOnline.getLegalEntityDefendant().getContactDetails().getEmail())) {
            return pleadOnline.getLegalEntityDefendant().getContactDetails().getEmail();
        }

        if (nonNull(pleadOnline.getPersonalDetails()) && nonNull(pleadOnline.getPersonalDetails().getContactDetails().getEmail())) {
            return pleadOnline.getPersonalDetails().getContactDetails().getEmail();
        }
        return null;
    }

    public static String getDefendantPostcode(final PleadOnline pleadOnline) {
        if (nonNull(pleadOnline.getLegalEntityDefendant())
                && nonNull(pleadOnline.getLegalEntityDefendant().getAddress().getPostcode())) {
            return pleadOnline.getLegalEntityDefendant().getAddress().getPostcode();
        }

        if (nonNull(pleadOnline.getPersonalDetails()) && nonNull(pleadOnline.getPersonalDetails().getAddress().getPostcode())) {
            return pleadOnline.getPersonalDetails().getAddress().getPostcode();
        }
        return null;
    }

    public static CivilOffence getCivilOffence(final Boolean isExParte) {
        if(isExParte == null) {
            return null;
        }
        return CivilOffence.civilOffence().withIsExParte(isExParte).build();
    }

    public static Boolean getExparteValueFromRefDataOffenceJsonObject(Optional<JsonObject> refDataOffence) {
        Boolean exparteValue = null;
        if (refDataOffence.isPresent()) {
            JsonObject offence = refDataOffence.get();
            exparteValue = offence.containsKey("exParte") ? valueOf(offence.getBoolean("exParte")) : null;
        }
        return exparteValue;
    }

}
