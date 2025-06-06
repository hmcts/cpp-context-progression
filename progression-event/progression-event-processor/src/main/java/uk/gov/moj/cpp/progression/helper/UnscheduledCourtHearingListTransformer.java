package uk.gov.moj.cpp.progression.helper;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.TypeOfList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S1192")
public class UnscheduledCourtHearingListTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnscheduledCourtHearingListTransformer.class);

    public static final UUID RESULT_DEFINITION_SAC = UUID.fromString("77b2055a-33af-47f9-bb60-1f8bf3a95cd8");
    public static final UUID RESULT_DEFINITION_NHCCS = UUID.fromString("fbed768b-ee95-4434-87c8-e81cbc8d24c8");
    public static final UUID HEARING_TYPE_HRG_ID = UUID.fromString("ff7f05e1-8a65-39bf-8d65-f73ef9a4ebed");
    public static final String HEARING_TYPE_HRG_DESC = "Hearing";
    public static final String DATE_AND_TIME_TO_BE_FIXED = "Date and time to be fixed";

    public List<HearingUnscheduledListingNeeds> transformHearing(final Hearing hearing) {
        final List<HearingUnscheduledListingNeeds> hearingUnscheduledListingNeeds = new ArrayList<>();
        if (hearing.getCourtApplications() != null) {
            hearing.getCourtApplications().stream()
                    .filter(ca -> nonNull(ca.getJudicialResults()))
                    .forEach(courtApplication -> {
                        final Optional<HearingUnscheduledListingNeeds> item = transformCourtApplication(hearing, courtApplication);
                        if (item.isPresent()) {
                            hearingUnscheduledListingNeeds.add(item.get());
                        }
                    });
        } else if (hearing.getProsecutionCases() != null) {

            hearing.getProsecutionCases().forEach(
                    prosecutionCase -> prosecutionCase.getDefendants().forEach(
                            defendant -> hearingUnscheduledListingNeeds.addAll(transformDefendant(hearing, prosecutionCase, defendant))
                    )
            );

        }

        return hearingUnscheduledListingNeeds;
    }

    public List<HearingUnscheduledListingNeeds> transformWithSeedHearing(final Hearing hearing, final SeedingHearing seedingHearing) {
        final List<HearingUnscheduledListingNeeds> hearingUnscheduledListingNeeds = new ArrayList<>();
        if (hearing.getCourtApplications() != null) {
            hearing.getCourtApplications().stream()
                    .filter(ca -> nonNull(ca.getJudicialResults()))
                    .forEach(courtApplication -> {
                        final Optional<HearingUnscheduledListingNeeds> item = transformCourtApplication(hearing, courtApplication);
                        if (item.isPresent()) {
                            hearingUnscheduledListingNeeds.add(item.get());
                        }
                    });
        }
        if(hearingUnscheduledListingNeeds.size() == 0 && nonNull(hearing.getProsecutionCases())) {
            hearing.getProsecutionCases().stream().forEach(
                    prosecutionCase -> prosecutionCase.getDefendants().stream().forEach(
                            defendant -> hearingUnscheduledListingNeeds.addAll(transformDefendantWithSeedHearing(hearing, prosecutionCase, defendant, seedingHearing))
                    )
            );

        }
        return hearingUnscheduledListingNeeds;
    }

    private Optional<HearingUnscheduledListingNeeds> transformCourtApplication(final Hearing hearing, final CourtApplication courtApplication) {

        final Optional<JudicialResult> judicialResultWithNextHearing = courtApplication.getJudicialResults().stream().filter(this::hasNextHearingWithDateToBeFixed).findFirst();

        if (judicialResultWithNextHearing.isPresent()) {

            final TypeOfList typeOfList = buildTypeOfList(judicialResultWithNextHearing.get(), courtApplication.getJudicialResults(), true);

            final JurisdictionType jurisdictionType = getJuristictionType(hearing.getJurisdictionType(), judicialResultWithNextHearing.get());

            final HearingType hearingType = judicialResultWithNextHearing.get().getNextHearing().getType();

            final CourtCentre courtCentre = judicialResultWithNextHearing.get().getNextHearing().getCourtCentre();

            final HearingUnscheduledListingNeeds hearingListingNeeds = createHearingListingNeeds(hearing, typeOfList, jurisdictionType, hearing.getProsecutionCases(),
                    Arrays.asList(createCourtApplication(courtApplication, judicialResultWithNextHearing.get())), hearingType, courtCentre);

            LOGGER.info("Unscheduled listing (nextHearing) New HearingId: {} created with typeOfList {} , jurisdictionType {} ," +
                            "hearingType {} , courtCentre {} from court application {}  in HearingId: {}.",
                    hearingListingNeeds.getId(), typeOfList, jurisdictionType, hearingType, courtCentre, courtApplication.getId(), hearing.getId());

            return Optional.of(hearingListingNeeds);

        } else {

            final Optional<JudicialResult> judicialResultWithUnscheduledFlag = courtApplication.getJudicialResults().stream().filter(this::hasUnscheduledFlag).findFirst();

            if (judicialResultWithUnscheduledFlag.isPresent()) {

                final TypeOfList typeOfList = buildTypeOfList(judicialResultWithUnscheduledFlag.get(), courtApplication.getJudicialResults(), false);

                final JurisdictionType jurisdictionType = getJuristictionType(hearing.getJurisdictionType(), judicialResultWithUnscheduledFlag.get());
                final HearingType hearingType = HearingType.hearingType().withId(HEARING_TYPE_HRG_ID).withDescription(HEARING_TYPE_HRG_DESC).build();

                final HearingUnscheduledListingNeeds hearingListingNeeds = createHearingListingNeeds(hearing, typeOfList, jurisdictionType, hearing.getProsecutionCases(),
                        Arrays.asList(createCourtApplication(courtApplication, judicialResultWithUnscheduledFlag.get())), hearingType, hearing.getCourtCentre());

                LOGGER.info("Unscheduled listing (result) New HearingId: {} created with typeOfList {} , jurisdictionType {} ," +
                                "hearingType {} , courtCentre {} from court application {}  in HearingId: {}.",
                        hearingListingNeeds.getId(), typeOfList, jurisdictionType, hearingType, hearing.getCourtCentre(), courtApplication.getId(), hearing.getId());

                return Optional.of(hearingListingNeeds);
            }
        }

        return Optional.empty();
    }

    private List<HearingUnscheduledListingNeeds> transformDefendant(final Hearing originalHearing,
                                                                    final ProsecutionCase prosecutionCase,
                                                                    final Defendant defendant) {

        final List<HearingUnscheduledListingNeeds> hearingUnscheduledListingNeeds = new ArrayList<>();


        final Map<UUID, List<HearingUnscheduledListingNeeds>> uuidListMap =
                defendant.getOffences().stream()
                        .filter(offence -> nonNull(offence.getJudicialResults()))
                        .map(offence -> transformDefendantOffence(originalHearing, prosecutionCase, defendant, offence))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.groupingBy(o -> o.getTypeOfList().getId()));

        uuidListMap.forEach((key, value) -> {
            if (value.size() > 1) {
                final Optional<HearingUnscheduledListingNeeds> listingNeeds = mergeListingNeedsWithSameTypeOfList(originalHearing, prosecutionCase, defendant, value);
                listingNeeds.ifPresent(hearingUnscheduledListingNeeds::add);

            } else {
                hearingUnscheduledListingNeeds.addAll(value);
            }
        });

        return hearingUnscheduledListingNeeds;
    }

    private List<HearingUnscheduledListingNeeds> transformDefendantWithSeedHearing(final Hearing originalHearing,
                                                                                   final ProsecutionCase prosecutionCase,
                                                                                   final Defendant defendant,
                                                                                   final SeedingHearing seedingHearing) {

        final List<HearingUnscheduledListingNeeds> hearingUnscheduledListingNeeds = new ArrayList<>();

        final Map<UUID, List<HearingUnscheduledListingNeeds>> uuidListMap =
                defendant.getOffences().stream()
                        .filter(offence -> nonNull(offence.getJudicialResults()))
                        .map(offence -> Offence.offence().withValuesFrom(offence).withSeedingHearing(seedingHearing).build())
                        .map(offenceWithSeedHearing -> transformDefendantOffence(originalHearing, prosecutionCase, defendant, offenceWithSeedHearing))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.groupingBy(o -> o.getTypeOfList().getId()));

        uuidListMap.forEach((key, value) -> {
            if (value.size() > 1) {
                final Optional<HearingUnscheduledListingNeeds> listingNeeds = mergeListingNeedsWithSameTypeOfList(originalHearing, prosecutionCase, defendant, value);
                listingNeeds.ifPresent(hearingUnscheduledListingNeeds::add);

            } else {
                hearingUnscheduledListingNeeds.addAll(value);
            }
        });

        return hearingUnscheduledListingNeeds;
    }

    private Optional<HearingUnscheduledListingNeeds> mergeListingNeedsWithSameTypeOfList(final Hearing originalHearing,
                                                                                         final ProsecutionCase prosecutionCase,
                                                                                         final Defendant defendant,
                                                                                         final List<HearingUnscheduledListingNeeds> hearingUnscheduledListingNeeds) {
        final List<Offence> offences = hearingUnscheduledListingNeeds.stream()
                .flatMap(s -> s.getProsecutionCases().stream())
                .flatMap(s -> s.getDefendants().stream())
                .flatMap(s -> s.getOffences().stream())
                .collect(Collectors.toList());
        final ProsecutionCase pc = createProsecutionCase(prosecutionCase, defendant, offences);

        final Optional<HearingUnscheduledListingNeeds> unscheduledListingNeeds = hearingUnscheduledListingNeeds.stream().findFirst();

        if (unscheduledListingNeeds.isPresent()) {
            return Optional.of(createHearingListingNeeds(originalHearing, unscheduledListingNeeds.get().getTypeOfList(), unscheduledListingNeeds.get().getJurisdictionType(),
                    Arrays.asList(pc), null, unscheduledListingNeeds.get().getType(), unscheduledListingNeeds.get().getCourtCentre()));
        }
        return Optional.empty();
    }

    private Optional<HearingUnscheduledListingNeeds> transformDefendantOffence(final Hearing originalHearing,
                                                                               final ProsecutionCase prosecutionCase,
                                                                               final Defendant defendant,
                                                                               final Offence offence) {
        final Optional<JudicialResult> judicialResultWithNextHearing = offence.getJudicialResults().stream().filter(this::hasNextHearingWithDateToBeFixed).findFirst();

        if (judicialResultWithNextHearing.isPresent()) {
            final TypeOfList typeOfList = buildTypeOfList(judicialResultWithNextHearing.get(), offence.getJudicialResults(), true);
            final JurisdictionType jurisdictionType = getJuristictionType(originalHearing.getJurisdictionType(), judicialResultWithNextHearing.get());
            final HearingType hearingType = judicialResultWithNextHearing.get().getNextHearing().getType();
            final CourtCentre courtCentre = judicialResultWithNextHearing.get().getNextHearing().getCourtCentre();
            final ProsecutionCase pc = createProsecutionCase(prosecutionCase, defendant, Arrays.asList(offence));

            final HearingUnscheduledListingNeeds hearingUnscheduledListingNeeds = createHearingListingNeeds(originalHearing, typeOfList, jurisdictionType,
                    Arrays.asList(pc), null, hearingType, courtCentre);

            LOGGER.info("Unscheduled listing (nextHearing) New HearingId: {} created with typeOfList {} , jurisdictionType {} ," +
                            "hearingType {} , courtCentre {} from court application {}  in HearingId: {}.",
                    hearingUnscheduledListingNeeds.getId(), typeOfList, jurisdictionType, hearingType, originalHearing.getCourtCentre(), offence.getId(), originalHearing.getId());

            return Optional.of(hearingUnscheduledListingNeeds);
        }

        final Optional<JudicialResult> judicialResultWithUnscheduledFlag = offence.getJudicialResults().stream().filter(this::hasUnscheduledFlag).findFirst();

        if (judicialResultWithUnscheduledFlag.isPresent()) {

            final TypeOfList typeOfList = buildTypeOfList(judicialResultWithUnscheduledFlag.get(), offence.getJudicialResults(), false);
            final JurisdictionType jurisdictionType = getJuristictionType(originalHearing.getJurisdictionType(), judicialResultWithUnscheduledFlag.get());
            final ProsecutionCase pc = createProsecutionCase(prosecutionCase, defendant, Arrays.asList(offence));

            final HearingType hearingType = HearingType.hearingType().withId(HEARING_TYPE_HRG_ID).withDescription(HEARING_TYPE_HRG_DESC).build();

            final HearingUnscheduledListingNeeds hearingUnscheduledListingNeeds = createHearingListingNeeds(originalHearing, typeOfList, jurisdictionType,
                    Arrays.asList(pc), null, hearingType, originalHearing.getCourtCentre());

            LOGGER.info("Unscheduled listing (result) New HearingId: {} created with typeOfList {} , jurisdictionType {} ," +
                            "hearingType {} , courtCentre {} from court application {}  in HearingId: {}.",
                    hearingUnscheduledListingNeeds.getId(), typeOfList, jurisdictionType, hearingType, originalHearing.getCourtCentre(), offence.getId(), originalHearing.getId());

            return Optional.of(hearingUnscheduledListingNeeds);
        }

        return Optional.empty();

    }

    private TypeOfList buildTypeOfList(final JudicialResult judicialResult, final List<JudicialResult> allJudicialResults, final boolean hasNextHearing) {

        final Optional<JudicialResult> sacJR = allJudicialResults.stream()
                .filter(jr -> RESULT_DEFINITION_SAC.equals(jr.getJudicialResultTypeId()))
                .findFirst();

        if (hasNextHearing && sacJR.isPresent()) {
            return TypeOfList.typeOfList()
                    .withId(RESULT_DEFINITION_NHCCS)
                    .withDescription(String.format("%s / %s", DATE_AND_TIME_TO_BE_FIXED, sacJR.get().getLabel()))
                    .build();
        }
        if (hasNextHearing) {
            return TypeOfList.typeOfList()
                    .withId(RESULT_DEFINITION_NHCCS)
                    .withDescription(DATE_AND_TIME_TO_BE_FIXED)
                    .build();
        }
        return TypeOfList.typeOfList()
                .withId(judicialResult.getJudicialResultTypeId())
                .withDescription(judicialResult.getLabel())
                .build();
    }

    private JurisdictionType getJuristictionType(final JurisdictionType hearingJurisdictionType, final JudicialResult judicialResult) {
        if (hasNextHearingWithDateToBeFixed(judicialResult)) {
            return JurisdictionType.CROWN;
        } else {
            return hearingJurisdictionType;
        }
    }

    private boolean hasUnscheduledFlag(final JudicialResult judicialResult) {
        return Boolean.TRUE.equals(judicialResult.getIsUnscheduled());
    }

    public boolean hasNextHearingWithDateToBeFixed(final JudicialResult judicialResult) {

        return (judicialResult.getNextHearing() != null && judicialResult.getNextHearing().getDateToBeFixed() != null)
                ? judicialResult.getNextHearing().getDateToBeFixed() : Boolean.FALSE;
    }

    private CourtApplication createCourtApplication(final CourtApplication courtApplication, final JudicialResult judicialResult) {
        return CourtApplication.courtApplication()
                .withValuesFrom(courtApplication)
                .withJudicialResults(Arrays.asList(judicialResult))
                .build();
    }

    private ProsecutionCase createProsecutionCase(final ProsecutionCase prosecutionCase, final Defendant defendant,
                                                  final List<Offence> offences) {

        return ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withDefendants(Arrays.asList(createDefendant(defendant, offences)))
                .build();

    }

    private Defendant createDefendant(final Defendant defendant, final List<Offence> offences) {

        return Defendant.defendant()
                .withValuesFrom(defendant)
                .withOffences(offences)
                .build();

    }

    private HearingUnscheduledListingNeeds createHearingListingNeeds(final Hearing hearing, final TypeOfList typeOfList,
                                                                     final JurisdictionType jurisdictionType,
                                                                     final List<ProsecutionCase> prosecutionCases,
                                                                     final List<CourtApplication> courtApplications, final HearingType hearingType, final CourtCentre courtCentre) {

        return HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                .withId(UUID.randomUUID())
                .withTypeOfList(typeOfList)
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withProsecutionCases(prosecutionCases)
                .withJurisdictionType(jurisdictionType)
                .withJudiciary(hearing.getJudiciary())
                .withType(HearingType.hearingType()
                        .withId(hearingType.getId())
                        .withDescription(hearingType.getDescription())
                        .build())
                .withEstimatedMinutes(0)
                .withEstimatedDuration(hearing.getEstimatedDuration())
                .withCourtCentre(courtCentre)
                .withCourtApplications(courtApplications)
                .build();
    }


}
