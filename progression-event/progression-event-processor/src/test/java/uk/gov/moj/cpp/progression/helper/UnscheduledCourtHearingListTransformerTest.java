package uk.gov.moj.cpp.progression.helper;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.moj.cpp.progression.helper.UnscheduledCourtHearingListTransformer.RESULT_DEFINITION_SAC;

import uk.gov.justice.core.courts.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UnscheduledCourtHearingListTransformerTest {
    private static final UUID WOFN = randomUUID();
    private static final String WOFN_LABEL = "WOFN";

    private static final UUID ASD = randomUUID();
    private static final String ASD_LABEL = "ASD";
    private static final String SAC_LABEL = "SAC";
    private static final String NHCCS_LABEL = "Date and time to be fixed";
    private static final String NHCCS_SAC_LABEL = "Date and time to be fixed / SAC";

    @InjectMocks
    private UnscheduledCourtHearingListTransformer unscheduledCourtHearingListTransformer;

    /***
     * Case1: Trivial case: Just One Offence, One Result and Unscheduled flag is true.
     * O1 - R1 (unscheduled: true)
     *
     * Expected: One Hearing with WOFN resultType
     */
    @Test
    public void shouldReturnOneHearingWhenOneOffenceAndOneResult() {
        final Offence offence = createOffenceWithJR(asList(wofnResult()));
        final Hearing hearing = createHearingWithOffences(asList(offence));
        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformHearing(hearing);
        assertThat(unscheduledListingNeedsList.size(), is(1));
        final HearingUnscheduledListingNeeds unscheduledListingNeeds = unscheduledListingNeedsList.get(0);
        assertThat(unscheduledListingNeeds.getTypeOfList().getId(), is(WOFN));
        assertThat(unscheduledListingNeeds.getTypeOfList().getDescription(), is(WOFN_LABEL));
        assertThat(unscheduledListingNeeds.getCourtApplications(), is(nullValue()));
        validateOffencesInUnscheduledListingNeeds(unscheduledListingNeeds, asList(offence));
        assertThat(unscheduledListingNeeds.getProsecutionCases().get(0).getCpsOrganisation(), is("A01"));
    }

    /**
     * As Case1 above, but the seed hearing id is added to each offence.
     */
    @Test
    public void shouldReturnOneHearingWhenOneOffenceAndOneResultWithSeedHearing() {
        final Offence offence = createOffenceWithJR(asList(wofnResult()));
        final Hearing hearing = createHearingWithOffences(asList(offence));
        final UUID seedingHearingId = randomUUID();

        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformWithSeedHearing(hearing, SeedingHearing
                .seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build());

        assertThat(unscheduledListingNeedsList.size(), is(1));
        final HearingUnscheduledListingNeeds unscheduledListingNeeds = unscheduledListingNeedsList.get(0);
        assertThat(unscheduledListingNeeds.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));
    }

    /**
     * Test to see if the unscheduledCourtHearingListTransformer works for hearing with application and cases.
     *
     */
    @Test
    public void shouldReturnHearingUnscheduledListingNeedsWhenHearingHasApplicationAndCases() {
        final Offence offence = createOffenceWithJR(asList(wofnResult()));
        final Hearing hearing = creatingHearingWithCaseAndApplication(asList(offence));
        final UUID seedingHearingId = randomUUID();

        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformWithSeedHearing(hearing, SeedingHearing
                .seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build());
        assertThat(unscheduledListingNeedsList.size(), is(1));
        assertThat(unscheduledListingNeedsList.get(0).getCourtApplications().size(), is(1));
        assertThat(unscheduledListingNeedsList.get(0).getProsecutionCases().size(), is(1));

    }

    @Test
    public void shouldReturnHearingUnscheduledListingNeedsWhenHearingHasApplicationAndCasesAndApplicationHasNoJudicialResults() {
        final Offence offence = createOffenceWithJR(asList(wofnResult()));
        final Hearing hearing = creatingHearingWithCaseAndApplicationAndApplicationHasNoJudicialResults(asList(offence));
        final UUID seedingHearingId = randomUUID();

        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformWithSeedHearing(hearing, SeedingHearing
                .seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build());
        assertThat(unscheduledListingNeedsList.size(), is(1));
        assertThat(unscheduledListingNeedsList.get(0).getProsecutionCases().size(), is(1));
        assertNull(unscheduledListingNeedsList.get(0).getCourtApplications());
    }


    /***
     * Case 2: One Offence, Two Result and both Unscheduled flags are true.
     * O1 - R1 (unscheduled: true - WOFN)
     *    - R2 (unscheduled: true - ASD)
     *
     * Expected: One Hearing with WOFN resultType
     */
    @Test
    public void shouldReturnOneHearingWithFirstTypeOfListWhenOneOffenceAndTwoResult() {
        final Offence offence = createOffenceWithJR(asList(wofnResult(), asdResult()));
        final Hearing hearing = createHearingWithOffences(asList(offence));
        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformHearing(hearing);
        assertThat(unscheduledListingNeedsList.size(), is(1));
        final HearingUnscheduledListingNeeds unscheduledListingNeeds = unscheduledListingNeedsList.get(0);
        assertThat(unscheduledListingNeeds.getTypeOfList().getId(), is(WOFN));
        assertThat(unscheduledListingNeeds.getTypeOfList().getDescription(), is(WOFN_LABEL));
        assertThat(unscheduledListingNeeds.getCourtApplications(), is(nullValue()));
        validateOffencesInUnscheduledListingNeeds(unscheduledListingNeeds, asList(offence));
    }

    /***
     * Case 3: Two Offences and One of the Unscheduled flag is true.
     * O1 - R1 (unscheduled: true)
     * O2 - R2 (unscheduled: false)
     *
     * Expected: OneHearing with WOFN resultType
     */
    @Test
    public void shouldReturnOneHearingWhenTwoOffenceAndOneUnscheduled() {
        final Offence offence1 = createOffenceWithJR(asList(wofnResult()));
        final Offence offence2 = createOffenceWithJR(asList(resultUnscheduledFalse()));
        final List<Offence> offences = asList(offence1, offence2);

        final Hearing hearing = createHearingWithOffences(offences);
        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformHearing(hearing);
        assertThat(unscheduledListingNeedsList.size(), is(1));
        final HearingUnscheduledListingNeeds unscheduledListingNeeds = unscheduledListingNeedsList.get(0);
        assertThat(unscheduledListingNeeds.getTypeOfList().getId(), is(WOFN));
        assertThat(unscheduledListingNeeds.getCourtApplications(), is(nullValue()));
        validateOffencesInUnscheduledListingNeeds(unscheduledListingNeeds, asList(offence1));
    }


    /***
     * Case 4: Two Offences and Both Unscheduled flags is true.
     * O1 - R1 (unscheduled: true, WOFN)
     * O2 - R2 (unscheduled: true, ASD)
     *
     * Expected: Two Hearings with resultTypes WOFN and ASD
     */
    @Test
    public void shouldReturnTwoHearingsWhenTwoOffenceAndBothUnscheduledAndDifferentTypeOfListing() {
        final Hearing hearing = createHearingWithOffences(Arrays.asList(
                createOffenceWithJR(Arrays.asList(wofnResult())),
                createOffenceWithJR(Arrays.asList(asdResult()))
        ));

        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformHearing(hearing);
        assertThat(unscheduledListingNeedsList.size(), is(2));
        final HearingUnscheduledListingNeeds unscheduledListingNeeds0 = unscheduledListingNeedsList.get(0);
        final HearingUnscheduledListingNeeds unscheduledListingNeeds1 = unscheduledListingNeedsList.get(1);
        if (unscheduledListingNeeds0.getTypeOfList().getId().equals(WOFN)) {
            assertThat(unscheduledListingNeeds1.getTypeOfList().getId(), is(ASD));
        } else {
            assertThat(unscheduledListingNeeds0.getTypeOfList().getId(), is(ASD));
            assertThat(unscheduledListingNeeds1.getTypeOfList().getId(), is(WOFN));
        }
        assertThat(unscheduledListingNeeds0.getCourtApplications(), is(nullValue()));
        assertThat(unscheduledListingNeeds1.getCourtApplications(), is(nullValue()));
    }


    /***
     * Case 4b: Two Offences and Both Unscheduled flags are true. Case 4b
     * O1 - R1 (unscheduled: true, WOFN)
     * O2 - R2 (unscheduled: true, WOFN)
     *
     * Expected: OneHearing with WOFN resultType
     */
    @Test
    public void shouldReturnOneHearingWhenTwoOffenceAndBothUnscheduledAndSameTypeOfListing() {

        final Offence offence1 = createOffenceWithJR(asList(wofnResult()));
        final Offence offence2 = createOffenceWithJR(asList(wofnResult()));
        final Offence offence3 = createOffenceWithJR(asList(wofnResult()));
        final List<Offence> offences = asList(offence1, offence2, offence3);
        final Hearing hearing = createHearingWithOffences(offences);

        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformHearing(hearing);
        assertThat(unscheduledListingNeedsList.size(), is(1));
        final HearingUnscheduledListingNeeds unscheduledListingNeeds = unscheduledListingNeedsList.get(0);
        assertThat(unscheduledListingNeeds.getTypeOfList().getId(), is(WOFN));
        assertThat(unscheduledListingNeeds.getProsecutionCases().stream().flatMap(s -> s.getDefendants().stream())
                .flatMap(s -> s.getOffences().stream()).count(), is(3L));
    }

    @Test
    public void shouldReturnOneHearingWhenTwoOffenceAndBothUnscheduledAndSameTypeOfListingWithNHCCS() {

        final Offence offence1 = createOffenceWithJR(asList(wofnResult(), resultWithNextHearingDateTobeFixed()));
        final Offence offence2 = createOffenceWithJR(asList(wofnResult()));
        final Offence offence3 = createOffenceWithJR(asList(wofnResult()));
        final List<Offence> offences = asList(offence1, offence2, offence3);
        final Hearing hearing = createHearingWithOffences(offences);

        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformHearing(hearing);
        assertThat(unscheduledListingNeedsList.size(), is(2));
        final HearingUnscheduledListingNeeds unscheduledListingNeeds = unscheduledListingNeedsList.stream().filter(s -> s.getTypeOfList().getId().equals(WOFN)).findFirst().get();
        assertThat(unscheduledListingNeeds.getTypeOfList().getId(), is(WOFN));
        assertThat(unscheduledListingNeeds.getProsecutionCases().stream().flatMap(s -> s.getDefendants().stream())
                .flatMap(s -> s.getOffences().stream()).count(), is(2L));
    }

    @Test
    public void shouldReturnOneHearingWhenTwoOffenceAndBothUnscheduledAndSameTypeOfListingWith2Defendants() {

        final Offence offence1 = createOffenceWithJR(asList(wofnResult()));
        final Offence offence2 = createOffenceWithJR(asList(wofnResult()));
        final Offence offence3 = createOffenceWithJR(asList(wofnResult()));
        final List<Offence> offences = asList(offence1, offence2, offence3);
        final Hearing hearing = Hearing.hearing()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .withDefendants(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(offences)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(asList(createOffenceWithJR(asList(wofnResult()))))
                                        .build()))
                        .build()))
                .build();


        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformHearing(hearing);
        assertThat(unscheduledListingNeedsList.size(), is(2));
        final HearingUnscheduledListingNeeds unscheduledListingNeeds = unscheduledListingNeedsList.get(0);
        assertThat(unscheduledListingNeeds.getTypeOfList().getId(), is(WOFN));

        assertThat(unscheduledListingNeeds.getProsecutionCases().stream().flatMap(s -> s.getDefendants().stream())
                .flatMap(s -> s.getOffences().stream()).count(), anyOf(is(3L), is(1L)));
    }


    /***
     * Case 5: Two Offences and Both have Two Results.
     * O1 - R1 (unscheduled: true, WOFN)
     *    - R2 (unscheduled: false, SAC)
     *
     * O2 - R3 (unscheduled: true, ASD)
     *    - R3 (unscheduled: false, R1)
     *
     * Expected: Two Hearings with resultTypes WOFN(SAC) and ASD
     */
    @Test
    public void shouldReturnTwoHearingsWhenTwoOffenceAndBothHaveTwoResults() {
        final Offence offence1 = createOffenceWithJR(asList(resultWithNextHearingDateTobeFixed(), sacResult()));
        final Offence offence2 = createOffenceWithJR(asList(asdResult(), resultUnscheduledFalse()));
        final Hearing hearing = createHearingWithOffences(asList(
                offence1,
                offence2
        ));

        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformHearing(hearing);
        assertThat(unscheduledListingNeedsList.size(), is(2));
        final Optional<HearingUnscheduledListingNeeds> h1 = unscheduledListingNeedsList.stream()
                .filter(u -> u.getTypeOfList().getId().equals(UnscheduledCourtHearingListTransformer.RESULT_DEFINITION_NHCCS))
                .findFirst();

        final Optional<HearingUnscheduledListingNeeds> h2 = unscheduledListingNeedsList.stream()
                .filter(u -> u.getTypeOfList().getId().equals(ASD))
                .findFirst();

        assertThat(h1.isPresent(), is(true));
        assertThat(h2.isPresent(), is(true));
        assertThat(h1.get().getTypeOfList().getDescription(), is(NHCCS_SAC_LABEL));
        validateOffencesInUnscheduledListingNeeds(h1.get(), asList(offence1));
        validateOffencesInUnscheduledListingNeeds(h2.get(), asList(offence2));
        assertThat(h1.get().getCourtApplications(), is(nullValue()));
        assertThat(h2.get().getCourtApplications(), is(nullValue()));
    }

    /***
     * Case 6: Two Defendants and Both results are Unscheduled.
     * D1 - O1 - R1 (unscheduled: true, WOFN)
     * D2 - O2 - R2 (unscheduled: true, ASD)
     *
     * Expected: Two Hearings with resultTypes WOFN and ASD
     */
    @Test
    public void shouldReturnTwoHearingsWhenTwoDefencesBothUnscheduled() {
        final Defendant defendant1 = Defendant.defendant().withId(randomUUID()).withOffences(
                asList(createOffenceWithJR(asList(wofnResult()))))
                .build();
        final Defendant defendant2 = Defendant.defendant().withId(randomUUID()).withOffences(
                asList(createOffenceWithJR(asList(asdResult()))))
                .build();

        final Hearing hearing = createHearingWithDefendants(asList(defendant1, defendant2));

        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformHearing(hearing);
        assertThat(unscheduledListingNeedsList.size(), is(2));
        final Optional<HearingUnscheduledListingNeeds> h1 = unscheduledListingNeedsList.stream()
                .filter(u -> u.getTypeOfList().getId().equals(WOFN))
                .findFirst();

        final Optional<HearingUnscheduledListingNeeds> h2 = unscheduledListingNeedsList.stream()
                .filter(u -> u.getTypeOfList().getId().equals(ASD))
                .findFirst();

        assertThat(h1.isPresent(), is(true));
        assertThat(h2.isPresent(), is(true));
        validateOffencesInUnscheduledListingNeeds(h1.get(), asList(defendant1.getOffences().get(0)));
        validateOffencesInUnscheduledListingNeeds(h2.get(), asList(defendant2.getOffences().get(0)));
        assertThat(h1.get().getCourtApplications(), is(nullValue()));
        assertThat(h2.get().getCourtApplications(), is(nullValue()));
    }

    /***
     * Case 7: Application and result is unscheduled
     * A1 - R1 (unscheduled: true, WOFN)
     *
     * Expected: OneHearing with WOFN resultType
     */
    @Test
    public void shouldReturnOneHearingsWhenAppResultIsUnscheduled() {
        final Hearing hearing = Hearing.hearing()
                .withCourtApplications(asList(CourtApplication.courtApplication()
                        .withJudicialResults(asList(wofnResult()))
                        .build()))
                .build();

        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformHearing(hearing);
        assertThat(unscheduledListingNeedsList.size(), is(1));
        final HearingUnscheduledListingNeeds unscheduledListingNeeds = unscheduledListingNeedsList.get(0);
        assertThat(unscheduledListingNeeds.getTypeOfList().getId(), is(WOFN));
        assertThat(unscheduledListingNeeds.getProsecutionCases(), is(nullValue()));
    }

    @Test
    public void shouldReturnOneHearingWhenOneOffenceHasResultWithPromptDateTimeToBeFixed() {
        final JudicialResult judicialResult = resultWithNextHearingDateTobeFixed();
        final Offence offence = createOffenceWithJR(asList(judicialResult));
        final Hearing hearing = createHearingWithOffences(asList(offence));
        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformHearing(hearing);
        assertThat(unscheduledListingNeedsList.size(), is(1));
        final HearingUnscheduledListingNeeds unscheduledListingNeeds = unscheduledListingNeedsList.get(0);
        assertThat(unscheduledListingNeeds.getTypeOfList().getId(), is(judicialResult.getJudicialResultTypeId()));
        assertThat(unscheduledListingNeeds.getTypeOfList().getDescription(), is(judicialResult.getLabel()));
        validateOffencesInUnscheduledListingNeeds(unscheduledListingNeeds, asList(offence));
        assertThat(unscheduledListingNeeds.getCourtApplications(), is(nullValue()));
    }

    @Test
    public void shouldReturnJuristictionTypeAsCrownWhenResultIsNHCCS() {
        final Offence offence = createOffenceWithJR(asList(resultWithNextHearingDateTobeFixed()));
        final Hearing hearing = createHearingWithOffences(asList(offence));
        final List<HearingUnscheduledListingNeeds> unscheduledListingNeedsList = unscheduledCourtHearingListTransformer.transformHearing(hearing);
        assertThat(unscheduledListingNeedsList.size(), is(1));
        final HearingUnscheduledListingNeeds unscheduledListingNeeds = unscheduledListingNeedsList.get(0);
        assertThat(unscheduledListingNeeds.getJurisdictionType(), is(JurisdictionType.CROWN));
        assertThat(unscheduledListingNeeds.getTypeOfList().getDescription(), is(NHCCS_LABEL));
        assertThat(unscheduledListingNeeds.getCourtApplications(), is(nullValue()));
        validateOffencesInUnscheduledListingNeeds(unscheduledListingNeeds, asList(offence));
    }

    private Hearing createHearingWithDefendants(final List<Defendant> defendants) {
        return Hearing.hearing()
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .withDefendants(defendants)
                        .build()))
                .build();
    }

    private Hearing createHearingWithOffences(final List<Offence> offences) {
        return Hearing.hearing()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .withCpsOrganisation("A01")
                        .withDefendants(asList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(offences)
                                .build()))
                        .build()))
                .build();
    }



    private Hearing creatingHearingWithCaseAndApplication(final List<Offence> offences) {
        return Hearing.hearing()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .withCpsOrganisation("A01")
                        .withDefendants(asList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(offences)
                                .build()))
                        .build()))
                .withCourtApplications(asList(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withJudicialResults(asList(JudicialResult.judicialResult()
                                .withNextHearing(NextHearing.nextHearing()
                                        .withDateToBeFixed(Boolean.TRUE)
                                        .withType(HearingType.hearingType()
                                                .withId(randomUUID())
                                                .withDescription("Plea")
                                                .build())
                                        .withCourtCentre(CourtCentre.courtCentre()
                                                .withId(randomUUID())
                                                .withRoomId(randomUUID())
                                                .build())
                                        .build())
                                .withJudicialResultPrompts(asList(JudicialResultPrompt.judicialResultPrompt()
                                        .withLabel("Next hearing in Crown Court")
                                        .withDurationElement(JudicialResultPromptDurationElement.judicialResultPromptDurationElement()
                                                .withPrimaryDurationValue(20)
                                                .build())

                                        .withValue("Date and time to be fixed:Yes\n" +
                                                "Courthouse organisation name:Mold Crown Court\n" +
                                                "Courthouse address line 1:The Law Courts\n")
                                        .build()))
                                .build()))
                        .build()))
                .build();

    }

    private Hearing creatingHearingWithCaseAndApplicationAndApplicationHasNoJudicialResults(final List<Offence> offences) {
        return Hearing.hearing()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .withCpsOrganisation("A01")
                        .withDefendants(asList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(offences)
                                .build()))
                        .build()))
                .withCourtApplications(asList(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withApplicant(CourtApplicationParty.courtApplicationParty()
                                .withId(randomUUID())
                                .withPersonDetails(Person.person()
                                        .withFirstName("John")
                                        .build())
                                .build())
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withId(randomUUID())
                                .withPersonDetails(Person.person()
                                        .withFirstName("XXX")
                                        .build())
                                .build())
                                .build()))
                .build();



    }


    private Offence createOffenceWithJR(final List<JudicialResult> judicialResults) {
        return Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(judicialResults)
                .build();
    }

    private void validateOffencesInUnscheduledListingNeeds(final HearingUnscheduledListingNeeds unscheduledListingNeeds, final List<Offence> expected) {
        assertThat(unscheduledListingNeeds.getProsecutionCases().size(), is(1));
        assertThat(unscheduledListingNeeds.getProsecutionCases().get(0).getDefendants().size(), is(1));
        final Defendant defendant = unscheduledListingNeeds.getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(defendant.getOffences().size(), is(expected.size()));
        Set<UUID> actual = defendant.getOffences().stream()
                .map(Offence::getId)
                .collect(Collectors.toSet());

        for (final Offence offence : expected) {
            if (!actual.contains(offence.getId())) {
                assertThat(String.format("Offence with id (%s) was expected but not found in actual.", offence.getId()), false);
            }
        }
    }

    private JudicialResult wofnResult() {
        return JudicialResult.judicialResult()
                .withIsUnscheduled(true)
                .withJudicialResultTypeId(WOFN)
                .withLabel(WOFN_LABEL)
                .withJudicialResultPrompts(Collections.emptyList())
                .build();
    }

    private JudicialResult asdResult() {
        return JudicialResult.judicialResult()
                .withIsUnscheduled(true)
                .withJudicialResultTypeId(ASD)
                .withLabel(ASD_LABEL)
                .withJudicialResultPrompts(Collections.emptyList())
                .build();
    }

    private JudicialResult sacResult() {
        return JudicialResult.judicialResult()
                .withIsUnscheduled(false)
                .withJudicialResultTypeId(RESULT_DEFINITION_SAC)
                .withLabel(SAC_LABEL)
                .withJudicialResultPrompts(Collections.emptyList())
                .build();
    }


    private JudicialResult resultUnscheduledFalse() {
        return JudicialResult.judicialResult()
                .withIsUnscheduled(false)
                .withJudicialResultTypeId(randomUUID())
                .withJudicialResultPrompts(Collections.emptyList())
                .withLabel("ANY")
                .build();
    }

    private JudicialResult resultWithNextHearingDateTobeFixed() {
        return JudicialResult.judicialResult()
                .withIsUnscheduled(false)
                .withJudicialResultTypeId(UnscheduledCourtHearingListTransformer.RESULT_DEFINITION_NHCCS)
                .withNextHearing(NextHearing.nextHearing().withDateToBeFixed(true)
                        .withType(HearingType.hearingType().withId(randomUUID()).withDescription("desc").build())
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(randomUUID())
                                .build())
                        .build())
                .withLabel(NHCCS_LABEL)
                .build();
    }


}