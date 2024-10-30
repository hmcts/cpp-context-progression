package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;

import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.listing.courts.ListNextHearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PartialHearingConfirmServiceTest {
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID SEEDING_HEARING_ID = randomUUID();
    private static final UUID CASE1_ID = randomUUID();
    private static final UUID CASE1_DEFENDANT1_ID = randomUUID();
    private static final UUID CASE1_DEFENDANT1_OFFENCE1_ID = randomUUID();
    private static final UUID CASE1_DEFENDANT1_OFFENCE2_ID = randomUUID();
    private static final UUID CASE1_DEFENDANT1_OFFENCE3_ID = randomUUID();
    private static final UUID CASE1_DEFENDANT2_ID = randomUUID();
    private static final UUID CASE1_DEFENDANT2_OFFENCE1_ID = randomUUID();
    private static final UUID CASE1_DEFENDANT2_OFFENCE2_ID = randomUUID();

    private static final UUID CASE2_ID = randomUUID();
    private static final UUID CASE2_DEFENDANT1_ID = randomUUID();
    private static final UUID CASE2_DEFENDANT1_OFFENCE1_ID = randomUUID();
    private static final UUID CASE2_DEFENDANT1_OFFENCE2_ID = randomUUID();
    private static final UUID CASE2_DEFENDANT2_ID = randomUUID();
    private static final UUID CASE2_DEFENDANT2_OFFENCE1_ID = randomUUID();
    private static final UUID CASE2_DEFENDANT2_OFFENCE2_ID = randomUUID();
    private static final UUID CASE3_ID = randomUUID();
    private static final UUID CASE3_DEFENDANT1_ID = randomUUID();
    private static final UUID CASE3_DEFENDANT1_OFFENCE1_ID = randomUUID();


    @Mock
    private ProgressionService progressionService;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private PartialHearingConfirmService partialHearingConfirmService;

    @Test
    public void shouldTransformConfirmProsecutionCasesToUpdateHearingForPartialAllocation() {
        final Map<UUID, Map<UUID, List<UUID>>> sampleMap = new HashMap<>();
        final Map<UUID, List<UUID>> case1DefendantsOffencesIds = new HashMap<>();
        final Map<UUID, List<UUID>> case2DefendantsOffencesIds = new HashMap<>();

        case1DefendantsOffencesIds.put(CASE1_DEFENDANT1_ID, Arrays.asList(CASE1_DEFENDANT1_OFFENCE1_ID, CASE1_DEFENDANT1_OFFENCE2_ID));
        case1DefendantsOffencesIds.put(CASE1_DEFENDANT2_ID, Arrays.asList(CASE1_DEFENDANT2_OFFENCE1_ID, CASE1_DEFENDANT2_OFFENCE2_ID));
        case2DefendantsOffencesIds.put(CASE2_DEFENDANT1_ID, Arrays.asList(CASE2_DEFENDANT1_OFFENCE1_ID, CASE2_DEFENDANT1_OFFENCE2_ID));
        case2DefendantsOffencesIds.put(CASE2_DEFENDANT2_ID, Arrays.asList(CASE2_DEFENDANT2_OFFENCE1_ID, CASE2_DEFENDANT2_OFFENCE2_ID));
        sampleMap.put(CASE1_ID, case1DefendantsOffencesIds);
        sampleMap.put(CASE2_ID, case2DefendantsOffencesIds);
        final UUID hearingId = randomUUID();

        UpdateHearingForPartialAllocation command = partialHearingConfirmService.transformConfirmProsecutionCasesToUpdateHearingForPartialAllocation(hearingId, buildSampleHearingConfirmed(sampleMap).getProsecutionCases());

        assertThat(command.getHearingId(), is(hearingId));
        assertThat(command.getProsecutionCasesToRemove().stream().filter(p -> p.getCaseId().equals(CASE1_ID)).findFirst().isPresent(), is(true));
        assertThat(command.getProsecutionCasesToRemove().stream().filter(p -> p.getCaseId().equals(CASE2_ID)).findFirst().isPresent(), is(true));

        assertThat(command.getProsecutionCasesToRemove().stream().filter(p -> p.getCaseId().equals(CASE1_ID)).findFirst().get().getDefendantsToRemove().size(), equalTo(2));
        assertThat(command.getProsecutionCasesToRemove().stream().filter(p -> p.getCaseId().equals(CASE1_ID)).findFirst().get().getDefendantsToRemove().stream()
                .filter(d -> d.getDefendantId().equals(CASE1_DEFENDANT1_ID)).findFirst().get().getOffencesToRemove().size(), equalTo(2));
        assertThat(command.getProsecutionCasesToRemove().stream().filter(p -> p.getCaseId().equals(CASE1_ID)).findFirst().get().getDefendantsToRemove().stream()
                .filter(d -> d.getDefendantId().equals(CASE1_DEFENDANT2_ID)).findFirst().get().getOffencesToRemove().size(), equalTo(2));

        assertThat(command.getProsecutionCasesToRemove().stream().filter(p -> p.getCaseId().equals(CASE2_ID)).findFirst().get().getDefendantsToRemove().size(), equalTo(2));
        assertThat(command.getProsecutionCasesToRemove().stream().filter(p -> p.getCaseId().equals(CASE2_ID)).findFirst().get().getDefendantsToRemove().stream()
                .filter(d -> d.getDefendantId().equals(CASE2_DEFENDANT1_ID)).findFirst().get().getOffencesToRemove().size(), equalTo(2));
        assertThat(command.getProsecutionCasesToRemove().stream().filter(p -> p.getCaseId().equals(CASE2_ID)).findFirst().get().getDefendantsToRemove().stream()
                .filter(d -> d.getDefendantId().equals(CASE2_DEFENDANT2_ID)).findFirst().get().getOffencesToRemove().size(), equalTo(2));
    }

    @Test
    public void shouldReturnEmptyListWhenConfirmedListSameWith() {
        final Map<UUID, Map<UUID, List<UUID>>> sampleMap = new HashMap<>();
        final Map<UUID, List<UUID>> case1DefendantsOffencesIds = new HashMap<>();
        final Map<UUID, List<UUID>> case2DefendantsOffencesIds = new HashMap<>();

        case1DefendantsOffencesIds.put(CASE1_DEFENDANT1_ID, Arrays.asList(CASE1_DEFENDANT1_OFFENCE1_ID, CASE1_DEFENDANT1_OFFENCE2_ID));
        case1DefendantsOffencesIds.put(CASE1_DEFENDANT2_ID, Arrays.asList(CASE1_DEFENDANT2_OFFENCE1_ID, CASE1_DEFENDANT2_OFFENCE2_ID));
        case1DefendantsOffencesIds.put(CASE2_DEFENDANT1_ID, Arrays.asList(CASE2_DEFENDANT1_OFFENCE1_ID, CASE2_DEFENDANT1_OFFENCE2_ID));
        case1DefendantsOffencesIds.put(CASE2_DEFENDANT2_ID, Arrays.asList(CASE2_DEFENDANT2_OFFENCE1_ID, CASE2_DEFENDANT2_OFFENCE2_ID));
        sampleMap.put(CASE1_ID, case1DefendantsOffencesIds);
        sampleMap.put(CASE2_ID, case2DefendantsOffencesIds);

        List<ProsecutionCase> delta = partialHearingConfirmService.getDifferences(buildSampleHearingConfirmed(sampleMap), buildSampleHearing(sampleMap));

        assertThat(delta.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnEmptyListWhenHearingIsEmpty() {
        final Map<UUID, Map<UUID, List<UUID>>> sampleMap = new HashMap<>();
        final Map<UUID, List<UUID>> case1DefendantsOffencesIds = new HashMap<>();
        final Map<UUID, List<UUID>> case2DefendantsOffencesIds = new HashMap<>();

        case1DefendantsOffencesIds.put(CASE1_DEFENDANT1_ID, Arrays.asList(CASE1_DEFENDANT1_OFFENCE1_ID, CASE1_DEFENDANT1_OFFENCE2_ID));
        case1DefendantsOffencesIds.put(CASE1_DEFENDANT2_ID, Arrays.asList(CASE1_DEFENDANT2_OFFENCE1_ID, CASE1_DEFENDANT2_OFFENCE2_ID));
        case1DefendantsOffencesIds.put(CASE2_DEFENDANT1_ID, Arrays.asList(CASE2_DEFENDANT1_OFFENCE1_ID, CASE2_DEFENDANT1_OFFENCE2_ID));
        case1DefendantsOffencesIds.put(CASE2_DEFENDANT2_ID, Arrays.asList(CASE2_DEFENDANT2_OFFENCE1_ID, CASE2_DEFENDANT2_OFFENCE2_ID));
        sampleMap.put(CASE1_ID, case1DefendantsOffencesIds);
        sampleMap.put(CASE2_ID, case2DefendantsOffencesIds);

        List<ProsecutionCase> delta = partialHearingConfirmService.getDifferences(buildSampleHearingConfirmed(sampleMap), buildSampleHearing(sampleMap));

        assertThat(delta.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnDifferentListWhenConfirmedListIsNotSame() {
        final Map<UUID, Map<UUID, List<UUID>>> sampleHearingMap = new HashMap<>();
        final Map<UUID, List<UUID>> case1DefendantsOffencesIds = new HashMap<>();
        final Map<UUID, List<UUID>> case2DefendantsOffencesIds = new HashMap<>();
        final Map<UUID, List<UUID>> case3DefendantsOffencesIds = new HashMap<>();
        final Map<UUID, Map<UUID, List<UUID>>> sampleConfirmMap = new HashMap<>();
        final Map<UUID, List<UUID>> case1DefendantsOffencesIdsForConfirm = new HashMap<>();
        final Map<UUID, List<UUID>> case2DefendantsOffencesIdsForConfirm = new HashMap<>();
        final Map<UUID, List<UUID>> case3DefendantsOffencesIdsForConfirm = new HashMap<>();

        case1DefendantsOffencesIds.put(CASE1_DEFENDANT1_ID, Arrays.asList(CASE1_DEFENDANT1_OFFENCE1_ID, CASE1_DEFENDANT1_OFFENCE2_ID));
        case1DefendantsOffencesIdsForConfirm.put(CASE1_DEFENDANT1_ID, Arrays.asList(CASE1_DEFENDANT1_OFFENCE1_ID, CASE1_DEFENDANT1_OFFENCE2_ID));

        case1DefendantsOffencesIds.put(CASE1_DEFENDANT2_ID, Arrays.asList(CASE1_DEFENDANT2_OFFENCE1_ID, CASE1_DEFENDANT2_OFFENCE2_ID));

        case2DefendantsOffencesIds.put(CASE2_DEFENDANT1_ID, Arrays.asList(CASE2_DEFENDANT1_OFFENCE1_ID, CASE2_DEFENDANT1_OFFENCE2_ID));
        case2DefendantsOffencesIdsForConfirm.put(CASE2_DEFENDANT1_ID, Arrays.asList(CASE2_DEFENDANT1_OFFENCE1_ID, CASE2_DEFENDANT1_OFFENCE2_ID));

        case2DefendantsOffencesIds.put(CASE2_DEFENDANT2_ID, Arrays.asList(CASE2_DEFENDANT2_OFFENCE1_ID, CASE2_DEFENDANT2_OFFENCE2_ID));
        case2DefendantsOffencesIdsForConfirm.put(CASE2_DEFENDANT2_ID, Arrays.asList(CASE2_DEFENDANT2_OFFENCE1_ID));

        case3DefendantsOffencesIds.put(CASE3_DEFENDANT1_ID, Arrays.asList(CASE3_DEFENDANT1_OFFENCE1_ID));
        case3DefendantsOffencesIdsForConfirm.put(CASE3_DEFENDANT1_ID, Arrays.asList(CASE3_DEFENDANT1_OFFENCE1_ID));

        sampleHearingMap.put(CASE1_ID, case1DefendantsOffencesIds);
        sampleHearingMap.put(CASE2_ID, case2DefendantsOffencesIds);
        sampleHearingMap.put(CASE3_ID, case3DefendantsOffencesIds);
        sampleConfirmMap.put(CASE1_ID, case1DefendantsOffencesIdsForConfirm);
        sampleConfirmMap.put(CASE2_ID, case2DefendantsOffencesIdsForConfirm);
        sampleConfirmMap.put(CASE3_ID, case3DefendantsOffencesIdsForConfirm);

        List<ProsecutionCase> delta = partialHearingConfirmService.getDifferences(buildSampleHearingConfirmed(sampleConfirmMap), buildSampleHearing(sampleHearingMap));

        assertThat(delta.isEmpty(), is(false));
        assertThat(delta.size(), equalTo(2));
        assertThat(delta.stream()
                .map(ProsecutionCase::getId).collect(toList())
                .containsAll(Arrays.asList(CASE1_ID, CASE2_ID)), equalTo(true));

        assertThat(delta.stream()
                .filter(p -> p.getId().equals(CASE1_ID))
                .map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .map(Defendant::getId)
                .collect(toList()).equals(Arrays.asList(CASE1_DEFENDANT2_ID)), equalTo(true));

        assertThat(delta.stream()
                .filter(p -> p.getId().equals(CASE2_ID))
                .map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .map(Defendant::getId)
                .collect(toList()).equals(Arrays.asList(CASE2_DEFENDANT2_ID)), equalTo(true));

        assertThat(delta.stream()
                        .filter(p -> p.getId().equals(CASE1_ID))
                        .map(ProsecutionCase::getDefendants)
                        .flatMap(Collection::stream)
                        .map(Defendant::getOffences)
                        .flatMap(Collection::stream)
                        .map(Offence::getId)
                        .collect(toList())
                        .equals(Arrays.asList(CASE1_DEFENDANT2_OFFENCE1_ID, CASE1_DEFENDANT2_OFFENCE2_ID))
                , equalTo(true));

        assertThat(delta.stream()
                        .filter(p -> p.getId().equals(CASE2_ID))
                        .map(ProsecutionCase::getDefendants)
                        .flatMap(Collection::stream)
                        .map(Defendant::getOffences)
                        .flatMap(Collection::stream)
                        .map(Offence::getId)
                        .collect(toList())
                        .equals(Arrays.asList(CASE2_DEFENDANT2_OFFENCE2_ID))
                , equalTo(true));


    }

    @Test
    public void shouldTransformToUpdateHearingForPartialAllocation() {
        final UUID hearingId = randomUUID();


        UpdateHearingForPartialAllocation updateHearingForPartialAllocation = partialHearingConfirmService.transformToUpdateHearingForPartialAllocation(hearingId, buildDeltaProsecutionCases());
        assertThat(updateHearingForPartialAllocation.getHearingId(), equalTo(hearingId));
        final List<ProsecutionCasesToRemove> prosecutionCasesToRemove = updateHearingForPartialAllocation.getProsecutionCasesToRemove();
        assertThat(prosecutionCasesToRemove.size(), equalTo(1));
        assertThat(prosecutionCasesToRemove.get(0).getCaseId(), equalTo(CASE1_ID));
        assertThat(prosecutionCasesToRemove.get(0).getDefendantsToRemove().size(), equalTo(1));
        assertThat(prosecutionCasesToRemove.get(0).getDefendantsToRemove().get(0).getDefendantId(), equalTo(CASE1_DEFENDANT2_ID));
        assertThat(prosecutionCasesToRemove.get(0).getDefendantsToRemove().get(0).getOffencesToRemove().size(), equalTo(1));
        assertThat(prosecutionCasesToRemove.get(0).getDefendantsToRemove().get(0).getOffencesToRemove().get(0).getOffenceId(), equalTo(CASE1_DEFENDANT2_OFFENCE1_ID));
    }

    @Test
    public void shouldTransformToListCourtHearing() {
        final UUID courtApplicationId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID courtCentreIdInProgression = randomUUID();
        final UUID hearingTypeId = randomUUID();
        final String reportRestrictionReason = "reportRestrictionReason";
        final String courtCentreName = "courtCentreName";
        final String courtCentreNameInProgression = "Lavender Hill";
        final ZonedDateTime sittingDay = ZonedDateTime.now();
        final ZonedDateTime sittingDayInProgression = ZonedDateTime.now().plusDays(2);
        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication().withId(courtApplicationId).build()))
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId).withName(courtCentreName).build())
                .withType(HearingType.hearingType().withId(hearingTypeId).build())
                .withJurisdictionType(CROWN)
                .withReportingRestrictionReason(reportRestrictionReason)
                .withHearingDays(Arrays.asList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .build();
        final Hearing hearingInProgression = Hearing.hearing()
                .withId(HEARING_ID)
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication().withId(courtApplicationId).build()))
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreIdInProgression).withName(courtCentreNameInProgression).build())
                .withType(HearingType.hearingType().withId(hearingTypeId).build())
                .withJurisdictionType(CROWN)
                .withReportingRestrictionReason(reportRestrictionReason)
                .withHearingDays(Arrays.asList(HearingDay.hearingDay().withSittingDay(sittingDayInProgression).build()))
                .build();
        final ListCourtHearing listCourtHearing = partialHearingConfirmService.transformToListCourtHearing(buildDeltaProsecutionCases(), hearing, hearingInProgression);

        final HearingListingNeeds hearingListingNeeds = listCourtHearing.getHearings().get(0);
        assertThat(hearingListingNeeds.getId(), notNullValue());
        assertThat(hearingListingNeeds.getCourtApplications().get(0).getId(), equalTo(courtApplicationId));
        assertThat(hearingListingNeeds.getCourtCentre().getId(), equalTo(hearingInProgression.getCourtCentre().getId()));
        assertThat(hearingListingNeeds.getCourtCentre().getName(), equalTo(hearingInProgression.getCourtCentre().getName()));
        assertThat(hearingListingNeeds.getType().getId(), equalTo(hearingTypeId));
        assertThat(hearingListingNeeds.getJurisdictionType(), equalTo(CROWN));
        assertThat(hearingListingNeeds.getReportingRestrictionReason(), equalTo(reportRestrictionReason));
        assertThat(hearingListingNeeds.getEstimatedMinutes(), equalTo(30));
        assertThat(hearingListingNeeds.getEarliestStartDateTime(), equalTo(sittingDayInProgression));

        final List<ProsecutionCase> prosecutionCases = hearingListingNeeds.getProsecutionCases();
        assertThat(prosecutionCases.size(), equalTo(1));
        assertThat(prosecutionCases.get(0).getId(), equalTo(CASE1_ID));


    }

    @Test
    public void shouldTransformToListNextCourtHearing() {
        final UUID courtApplicationId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID courtCentreIdInProgression = randomUUID();
        final UUID hearingTypeId = randomUUID();
        final String reportRestrictionReason = "reportRestrictionReason";
        final String courtCentreName = "courtCentreName";
        final String courtCentreNameInProgression = "Lavender Hill";
        final ZonedDateTime sittingDay = ZonedDateTime.now();
        final ZonedDateTime sittingDayInProgression = ZonedDateTime.now().plusDays(2);
        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication().withId(courtApplicationId).build()))
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId).withName(courtCentreName).build())
                .withType(HearingType.hearingType().withId(hearingTypeId).build())
                .withJurisdictionType(CROWN)
                .withReportingRestrictionReason(reportRestrictionReason)
                .withHearingDays(Arrays.asList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .build();
        final Hearing hearingInProgression = Hearing.hearing()
                .withId(HEARING_ID)
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication().withId(courtApplicationId).build()))
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreIdInProgression).withName(courtCentreNameInProgression).build())
                .withType(HearingType.hearingType().withId(hearingTypeId).build())
                .withJurisdictionType(CROWN)
                .withReportingRestrictionReason(reportRestrictionReason)
                .withHearingDays(Arrays.asList(HearingDay.hearingDay().withSittingDay(sittingDayInProgression).build()))
                .build();

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(SEEDING_HEARING_ID)
                .build();
        final ListNextHearings listNextHearings = partialHearingConfirmService.transformToListNextCourtHearing(buildDeltaProsecutionCases(), hearing, hearingInProgression, seedingHearing);

        final HearingListingNeeds hearingListingNeeds = listNextHearings.getHearings().get(0);
        assertThat(hearingListingNeeds.getId(), notNullValue());
        assertThat(hearingListingNeeds.getCourtApplications().get(0).getId(), equalTo(courtApplicationId));
        assertThat(hearingListingNeeds.getCourtCentre().getId(), equalTo(hearingInProgression.getCourtCentre().getId()));
        assertThat(hearingListingNeeds.getCourtCentre().getName(), equalTo(hearingInProgression.getCourtCentre().getName()));
        assertThat(hearingListingNeeds.getType().getId(), equalTo(hearingTypeId));
        assertThat(hearingListingNeeds.getJurisdictionType(), equalTo(CROWN));
        assertThat(hearingListingNeeds.getReportingRestrictionReason(), equalTo(reportRestrictionReason));
        assertThat(hearingListingNeeds.getEstimatedMinutes(), equalTo(30));
        assertThat(hearingListingNeeds.getEarliestStartDateTime(), equalTo(sittingDayInProgression));

        final List<ProsecutionCase> prosecutionCases = hearingListingNeeds.getProsecutionCases();
        assertThat(prosecutionCases.size(), equalTo(1));
        assertThat(prosecutionCases.get(0).getId(), equalTo(CASE1_ID));

        assertThat(listNextHearings.getHearingId(), is(SEEDING_HEARING_ID));
    }

    @Test
    public void shouldTransformToListNextCourtHearingWhenHearingDaysNull() {
        final UUID courtApplicationId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID courtCentreIdInProgression = randomUUID();
        final UUID hearingTypeId = randomUUID();
        final String reportRestrictionReason = "reportRestrictionReason";
        final String courtCentreName = "courtCentreName";
        final String courtCentreNameInProgression = "Lavender Hill";
        final ZonedDateTime sittingDay = ZonedDateTime.now();
        final ZonedDateTime sittingDayInProgression = ZonedDateTime.now().plusDays(2);
        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication().withId(courtApplicationId).build()))
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId).withName(courtCentreName).build())
                .withType(HearingType.hearingType().withId(hearingTypeId).build())
                .withJurisdictionType(CROWN)
                .withReportingRestrictionReason(reportRestrictionReason)
                .withHearingDays(Arrays.asList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .build();
        final Hearing hearingInProgression = Hearing.hearing()
                .withId(HEARING_ID)
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication().withId(courtApplicationId).build()))
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreIdInProgression).withName(courtCentreNameInProgression).build())
                .withType(HearingType.hearingType().withId(hearingTypeId).build())
                .withJurisdictionType(CROWN)
                .withReportingRestrictionReason(reportRestrictionReason)
                .build();

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(SEEDING_HEARING_ID)
                .build();
        final ListNextHearings listNextHearings = partialHearingConfirmService.transformToListNextCourtHearing(buildDeltaProsecutionCases(), hearing, hearingInProgression, seedingHearing);

        final HearingListingNeeds hearingListingNeeds = listNextHearings.getHearings().get(0);
        assertThat(hearingListingNeeds.getId(), notNullValue());
        assertThat(hearingListingNeeds.getCourtApplications().get(0).getId(), equalTo(courtApplicationId));
        assertThat(hearingListingNeeds.getCourtCentre().getId(), equalTo(hearingInProgression.getCourtCentre().getId()));
        assertThat(hearingListingNeeds.getCourtCentre().getName(), equalTo(hearingInProgression.getCourtCentre().getName()));
        assertThat(hearingListingNeeds.getType().getId(), equalTo(hearingTypeId));
        assertThat(hearingListingNeeds.getJurisdictionType(), equalTo(CROWN));
        assertThat(hearingListingNeeds.getReportingRestrictionReason(), equalTo(reportRestrictionReason));
        assertThat(hearingListingNeeds.getEstimatedMinutes(), equalTo(30));
        assertThat(hearingListingNeeds.getEarliestStartDateTime(), nullValue());

        final List<ProsecutionCase> prosecutionCases = hearingListingNeeds.getProsecutionCases();
        assertThat(prosecutionCases.size(), equalTo(1));
        assertThat(prosecutionCases.get(0).getId(), equalTo(CASE1_ID));

        assertThat(listNextHearings.getHearingId(), is(SEEDING_HEARING_ID));


    }


    @Test
    public void shouldGetDeltaSeededProsecutionCases() {
        final UUID seedingHearingId = randomUUID();
        final UUID seedingHearingId2 = randomUUID();
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).withJurisdictionType(CROWN).build();
        final SeedingHearing seedingHearing2 = SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId2).withJurisdictionType(CROWN).build();

        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(HEARING_ID)
                .withProsecutionCases( new ArrayList<>(Arrays.asList(
                        ConfirmedProsecutionCase.confirmedProsecutionCase()
                                .withId(CASE1_ID)
                                .withDefendants(new ArrayList<>(Arrays.asList(ConfirmedDefendant.confirmedDefendant()
                                        .withId(CASE1_DEFENDANT1_ID)
                                        .withOffences(new ArrayList<>(Arrays.asList(ConfirmedOffence.confirmedOffence()
                                                .withId(CASE1_DEFENDANT1_OFFENCE3_ID)
                                                .withSeedingHearing(seedingHearing)
                                                .build()
                                        )))
                                        .build())))
                                .build()
                )))
                .build();

        final Hearing hearingInProgression = Hearing.hearing()
                .withId(HEARING_ID)
                .withProsecutionCases(new ArrayList<>(Arrays.asList(
                        ProsecutionCase.prosecutionCase()
                                .withId(CASE1_ID)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                                .withId(CASE1_DEFENDANT1_ID)
                                                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                                                .withId(CASE1_DEFENDANT1_OFFENCE1_ID)
                                                                .withSeedingHearing(seedingHearing)
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(CASE1_DEFENDANT1_OFFENCE2_ID)
                                                                .withSeedingHearing(seedingHearing)
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(CASE1_DEFENDANT1_OFFENCE3_ID)
                                                                .withSeedingHearing(seedingHearing)
                                                                .build())))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(CASE1_DEFENDANT2_ID)
                                                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                                                .withId(CASE1_DEFENDANT2_OFFENCE1_ID)
                                                                .withSeedingHearing(seedingHearing)
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(CASE1_DEFENDANT2_OFFENCE2_ID)
                                                                .withSeedingHearing(seedingHearing2)
                                                                .build())))
                                                .build())))
                                .build(),
                        ProsecutionCase.prosecutionCase()
                                .withId(CASE2_ID)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                        .withId(CASE2_DEFENDANT1_ID)
                                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                                .withId(CASE2_DEFENDANT1_OFFENCE1_ID)
                                                .withSeedingHearing(seedingHearing2)
                                                .build())))
                                        .build())))
                                .build()
                )))
                .build();

        final List<ProsecutionCase> deltaProsecutionCases = partialHearingConfirmService.getDeltaSeededProsecutionCases(confirmedHearing, hearingInProgression, seedingHearing);

        assertThat(deltaProsecutionCases.size(), is(1));
        final ProsecutionCase prosecutionCase = deltaProsecutionCases.get(0);
        assertThat(prosecutionCase.getId(), is(CASE1_ID));
        assertThat(prosecutionCase.getDefendants().size(), is(2));
        final Defendant defendant1 = prosecutionCase.getDefendants().get(0);
        assertThat(defendant1.getId(), is(CASE1_DEFENDANT1_ID));
        assertThat(defendant1.getOffences().size(), is(2));
        assertThat(defendant1.getOffences().get(0).getId(), is(CASE1_DEFENDANT1_OFFENCE1_ID));
        assertThat(defendant1.getOffences().get(1).getId(), is(CASE1_DEFENDANT1_OFFENCE2_ID));
        final Defendant defendant2 = prosecutionCase.getDefendants().get(1);
        assertThat(defendant2.getId(), is(CASE1_DEFENDANT2_ID));
        assertThat(defendant2.getOffences().size(), is(1));
        assertThat(defendant2.getOffences().get(0).getId(), is(CASE1_DEFENDANT2_OFFENCE1_ID));
    }

    @Test
    public void shouldGetRelatedSeedingHearingsProsecutionCasesMap(){
        final UUID seedingHearingId = randomUUID();
        final UUID seedingHearingId2 = randomUUID();
        final UUID seedingHearingId3 = randomUUID();
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .withJurisdictionType(CROWN)
                .build();
        final SeedingHearing seedingHearing2 = SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId2)
                .withJurisdictionType(CROWN)
                .build();
        final SeedingHearing seedingHearing3 = SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId3)
                .withJurisdictionType(CROWN)
                .build();

        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(HEARING_ID)
                .withProsecutionCases( new ArrayList<>(Arrays.asList(
                        ConfirmedProsecutionCase.confirmedProsecutionCase()
                                .withId(CASE1_ID)
                                .withDefendants(new ArrayList<>(Arrays.asList(ConfirmedDefendant.confirmedDefendant()
                                        .withId(CASE1_DEFENDANT1_ID)
                                        .withOffences(new ArrayList<>(Arrays.asList(ConfirmedOffence.confirmedOffence()
                                                        .withId(CASE1_DEFENDANT1_OFFENCE3_ID)
                                                        .withSeedingHearing(seedingHearing)
                                                        .build()
                                        )))
                                        .build())))
                                .build()
                )))
                .build();

        final Hearing hearingInProgression = Hearing.hearing()
                .withId(HEARING_ID)
                .withProsecutionCases(new ArrayList<>(Arrays.asList(
                        ProsecutionCase.prosecutionCase()
                                .withId(CASE1_ID)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                                .withId(CASE1_DEFENDANT1_ID)
                                                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                                                .withId(CASE1_DEFENDANT1_OFFENCE1_ID)
                                                                .withSeedingHearing(seedingHearing)
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(CASE1_DEFENDANT1_OFFENCE2_ID)
                                                                .withSeedingHearing(seedingHearing)
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(CASE1_DEFENDANT1_OFFENCE3_ID)
                                                                .withSeedingHearing(seedingHearing)
                                                                .build())))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(CASE1_DEFENDANT2_ID)
                                                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                                                .withId(CASE1_DEFENDANT2_OFFENCE1_ID)
                                                                .withSeedingHearing(seedingHearing)
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(CASE1_DEFENDANT2_OFFENCE2_ID)
                                                                .withSeedingHearing(seedingHearing2)
                                                                .build())))
                                                .build())))
                                .build(),
                        ProsecutionCase.prosecutionCase()
                                .withId(CASE2_ID)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                        .withId(CASE2_DEFENDANT1_ID)
                                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                                        .withId(CASE2_DEFENDANT1_OFFENCE1_ID)
                                                        .withSeedingHearing(seedingHearing2)
                                                        .build(),
                                                Offence.offence()
                                                        .withId(CASE2_DEFENDANT1_OFFENCE2_ID)
                                                        .withSeedingHearing(seedingHearing3)
                                                        .build()
                                        )))
                                        .build())))
                                .build()
                )))
                .build();


        final Map<SeedingHearing, List<ProsecutionCase>> relatedSeedingHearingsProsecutionCasesMap =
                partialHearingConfirmService.getRelatedSeedingHearingsProsecutionCasesMap(confirmedHearing, hearingInProgression, seedingHearing);
        assertThat(relatedSeedingHearingsProsecutionCasesMap.size(), is(2));

        final List<ProsecutionCase> seedingHearing2sProsecutionCases = relatedSeedingHearingsProsecutionCasesMap.get(seedingHearing2);
        assertThat(seedingHearing2sProsecutionCases.size(), is(2));
        final ProsecutionCase seedingHearing2sProsecutionCase = seedingHearing2sProsecutionCases.get(0);
        assertThat(seedingHearing2sProsecutionCase.getId(), is(CASE1_ID));
        assertThat(seedingHearing2sProsecutionCase.getDefendants().size(), is(1));
        final Defendant seedingHearing2sDefendant = seedingHearing2sProsecutionCase.getDefendants().get(0);
        assertThat(seedingHearing2sDefendant.getId(), is(CASE1_DEFENDANT2_ID));
        assertThat(seedingHearing2sDefendant.getOffences().size(), is(1));
        assertThat(seedingHearing2sDefendant.getOffences().get(0).getId(), is(CASE1_DEFENDANT2_OFFENCE2_ID));

        final ProsecutionCase seedingHearing2sProsecutionCase2 = seedingHearing2sProsecutionCases.get(1);
        assertThat(seedingHearing2sProsecutionCase2.getId(), is(CASE2_ID));
        assertThat(seedingHearing2sProsecutionCase2.getDefendants().size(), is(1));
        final Defendant seedingHearings2Defendant = seedingHearing2sProsecutionCase2.getDefendants().get(0);
        assertThat(seedingHearings2Defendant.getId(), is(CASE2_DEFENDANT1_ID));
        assertThat(seedingHearings2Defendant.getOffences().size(), is(1));
        assertThat(seedingHearings2Defendant.getOffences().get(0).getId(), is(CASE2_DEFENDANT1_OFFENCE1_ID));


        final List<ProsecutionCase> seedingHearing3sProsecutionCases = relatedSeedingHearingsProsecutionCasesMap.get(seedingHearing3);
        assertThat(seedingHearing3sProsecutionCases.size(), is(1));
        final ProsecutionCase seeding3sProsecutionCase = seedingHearing3sProsecutionCases.get(0);
        assertThat(seeding3sProsecutionCase.getId(), is(CASE2_ID));
        assertThat(seeding3sProsecutionCase.getDefendants().size(), is(1));
        final Defendant seedingHearing3sDefendant = seeding3sProsecutionCase.getDefendants().get(0);
        assertThat(seedingHearing3sDefendant.getId(), is(CASE2_DEFENDANT1_ID));
        assertThat(seedingHearing3sDefendant.getOffences().size(), is(1));
        assertThat(seedingHearing3sDefendant.getOffences().get(0).getId(), is(CASE2_DEFENDANT1_OFFENCE2_ID));
    }

    private List<ProsecutionCase> buildDeltaProsecutionCases() {
        final Map<UUID, Map<UUID, List<UUID>>> sampleDeltaMap = new HashMap<>();
        final Map<UUID, List<UUID>> case1DefendantsOffencesIdsForDelta = new HashMap<>();
        case1DefendantsOffencesIdsForDelta.put(CASE1_DEFENDANT2_ID, Arrays.asList(CASE1_DEFENDANT2_OFFENCE1_ID));
        sampleDeltaMap.put(CASE1_ID, case1DefendantsOffencesIdsForDelta);

        return buildProsecutionCases(sampleDeltaMap);
    }

    private List<ProsecutionCase> buildProsecutionCases(final Map<UUID, Map<UUID, List<UUID>>> caseDefendantsOffencesIds) {
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        caseDefendantsOffencesIds.forEach((c, d) -> prosecutionCases.add(ProsecutionCase.prosecutionCase()
                .withId(c)
                .withDefendants(buildDefendants(d))
                .build()));
        return prosecutionCases;
    }

    private ConfirmedHearing buildSampleHearingConfirmed(final Map<UUID, Map<UUID, List<UUID>>> caseDefendantsOffencesIds) {
        final List<ConfirmedProsecutionCase> prosecutionCases = new ArrayList<>();
        caseDefendantsOffencesIds.forEach((c, d) -> prosecutionCases.add(ConfirmedProsecutionCase.confirmedProsecutionCase()
                .withId(c)
                .withDefendants(buildConfirmedDefendants(d))
                .build()));


        return ConfirmedHearing.confirmedHearing()
                .withId(HEARING_ID)
                .withProsecutionCases(prosecutionCases)
                .build();
    }


    private Hearing buildSampleHearing(final Map<UUID, Map<UUID, List<UUID>>> caseDefendantsOffencesIds) {
        final List<ProsecutionCase> prosecutionCases = buildProsecutionCases(caseDefendantsOffencesIds);

        return Hearing.hearing()
                .withId(HEARING_ID)
                .withProsecutionCases(prosecutionCases)
                .build();
    }


    private List<ConfirmedOffence> buildConfirmedOffences(List<UUID> offenceIds) {
        final List<ConfirmedOffence> offences = new ArrayList<>();
        offenceIds.forEach(o -> offences.add(ConfirmedOffence.confirmedOffence()
                .withId(o)
                .build()));
        return offences;
    }

    private List<ConfirmedDefendant> buildConfirmedDefendants(Map<UUID, List<UUID>> defendantsOffenceIds) {
        final List<ConfirmedDefendant> defendants = new ArrayList<>();
        defendantsOffenceIds.forEach((defendantId, offenceIds) -> defendants.add(ConfirmedDefendant.confirmedDefendant()
                .withId(defendantId)
                .withOffences(buildConfirmedOffences(offenceIds))
                .build()));
        return defendants;
    }

    private List<Defendant> buildDefendants(Map<UUID, List<UUID>> defendantsOffenceIds) {
        final List<Defendant> defendants = new ArrayList<>();
        defendantsOffenceIds.forEach((defendantId, offenceIds) -> defendants.add(Defendant.defendant()
                .withId(defendantId)
                .withOffences(buildOffences(offenceIds))
                .build()));
        return defendants;
    }

    private List<Offence> buildOffences(List<UUID> offenceIds) {
        final List<Offence> offences = new ArrayList<>();
        offenceIds.forEach(o -> offences.add(Offence.offence()
                .withId(o)
                .build()));
        return offences;
    }


}
