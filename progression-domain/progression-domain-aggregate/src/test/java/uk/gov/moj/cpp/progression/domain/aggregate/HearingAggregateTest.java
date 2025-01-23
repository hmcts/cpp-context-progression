package uk.gov.moj.cpp.progression.domain.aggregate;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.SeedingHearing.seedingHearing;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;


import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CaseHearingDetailsUpdatedInUnifiedSearch;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingDefendantUpdated;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUpdatedForAllocationFields;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV3;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.UpdateHearingForAllocationFields;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.justice.listing.courts.ListNextHearingsV3;
import uk.gov.justice.progression.courts.DeletedHearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.HearingDeleted;
import uk.gov.justice.progression.courts.HearingMarkedAsDuplicate;
import uk.gov.justice.progression.courts.HearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.OffenceInHearingDeleted;
import uk.gov.justice.progression.courts.OffencesRemovedFromHearing;
import uk.gov.justice.progression.courts.RelatedHearingUpdated;
import uk.gov.justice.progression.courts.UnscheduledHearingAllocationNotified;
import uk.gov.justice.progression.courts.VejDeletedHearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.VejHearingPopulatedToProbationCaseworker;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingAggregateTest {

    @Mock
    private Hearing hearing;

    @Mock
    private DefenceCounsel defenceCounsel;

    @InjectMocks
    private HearingAggregate hearingAggregate;

    @Test
    public void shouldIgnoreDuplicateDefenceCounselsAdded() {
        final UUID userId1 = randomUUID();
        final UUID userId2 = randomUUID();

        final UUID id1 = randomUUID();
        final UUID id2 = randomUUID();
        final String firstName = "firstName";
        final String lastName = "lastName";
        final String title = "title";
        final String middleName = "middleName";
        final List<UUID> defendantList = new ArrayList();
        defendantList.add(randomUUID());
        final List<LocalDate> attendanceDays = new ArrayList();
        attendanceDays.add(LocalDate.now());

        final DefenceCounsel defenceCounsel1 = getDefenceCounsel(id1, userId1, firstName, lastName, title, middleName, defendantList,attendanceDays);
        final DefenceCounsel defenceCounselDuplicate = getDefenceCounsel(id1, userId1, firstName, lastName, title, middleName, defendantList,attendanceDays);
        final DefenceCounsel defenceCounsel2 = getDefenceCounsel(id2, userId2, firstName, lastName, title, middleName, defendantList,attendanceDays);

        final List<Object> eventStream1 = hearingAggregate.addDefenceCounselToHearing(defenceCounsel1).collect(toList());
        assertThat(eventStream1.size(), is(2));
        final ProsecutionCaseDefendantListingStatusChanged event1  = (ProsecutionCaseDefendantListingStatusChanged) eventStream1.get(1);
        assertThat(event1.getHearing().getDefenceCounsels().size(),is(1));

        //Duplicate
        final List<Object> eventStream2 = hearingAggregate.addDefenceCounselToHearing(defenceCounselDuplicate).collect(toList());
        assertThat(eventStream2.size(), is(2));
        final ProsecutionCaseDefendantListingStatusChanged event2 = (ProsecutionCaseDefendantListingStatusChanged) eventStream2.get(1);
        assertThat(event2.getHearing().getDefenceCounsels().size(),is(1));

        final List<Object> eventStream3 = hearingAggregate.addDefenceCounselToHearing(defenceCounsel2).collect(toList());
        assertThat(eventStream3.size(), is(2));
        final ProsecutionCaseDefendantListingStatusChanged event3 = (ProsecutionCaseDefendantListingStatusChanged) eventStream3.get(1);
        assertThat(event3.getHearing().getDefenceCounsels().size(),is(2));
    }


    @Test
    public void shouldClearVerdict() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final Boolean notifyNCES = true;
        final Hearing hearing = getHearingForVej(prosecutionCaseId, courtApplicationId, hearingId, offenceId);
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_RESULTED;
        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);
        setField(hearingAggregate, "notifyNCES", notifyNCES);

       final Verdict verdict = Verdict.verdict().withOffenceId(offenceId).withVerdictType(VerdictType.verdictType().
               withCategory("category").withId(randomUUID()).withCategoryType("categoryType").withCategory("category").build())
               .withIsDeleted(true).build();

        final List<Object> eventStream = hearingAggregate.updateHearingWithVerdict(verdict).collect(toList());
        assertThat(eventStream.size(), is(2));

        final ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) eventStream.get(1);
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getVerdict(), nullValue());

    }


    @Test
    public void shouldMarkHearingDuplicate() {
        final UUID hearingId = randomUUID();
        final List<UUID> caseIds = Arrays.asList(randomUUID(), randomUUID());
        final List<UUID> defendantIds = Arrays.asList(randomUUID(), randomUUID());
        final Hearing hearing = Hearing
                .hearing()
                .withId(hearingId).build();
        setField(hearingAggregate, "hearing", hearing);

        final List<Object> eventStream = hearingAggregate.markAsDuplicate(hearingId, caseIds, defendantIds).collect(toList());
        assertThat(eventStream.size(), is(2));
        final HearingMarkedAsDuplicate hearingMarkedAsDuplicate = (HearingMarkedAsDuplicate) eventStream.get(0);
        final DeletedHearingPopulatedToProbationCaseworker deletedHearingPopulatedToProbationCaseworker = (DeletedHearingPopulatedToProbationCaseworker) eventStream.get(1);

        assertThat(hearingMarkedAsDuplicate.getHearingId(), is(hearingId));
        assertThat(hearingMarkedAsDuplicate.getCaseIds(), is(caseIds));
        assertThat(hearingMarkedAsDuplicate.getDefendantIds(), is(defendantIds));
        assertThat(deletedHearingPopulatedToProbationCaseworker.getHearing().getId(), is(hearingId));
    }

    @Test
    public void shouldNotRaiseNewEventIfAlreadyMarkedAsDuplicate() {
        setField(hearingAggregate, "duplicate", true);

        final UUID hearingId = randomUUID();
        final List<UUID> caseIds = Arrays.asList(randomUUID(), randomUUID());
        final List<UUID> defendantIds = Arrays.asList(randomUUID(), randomUUID());
        final Hearing hearing = Hearing
                .hearing()
                .withId(hearingId).build();
        setField(hearingAggregate, "hearing", hearing);
        final List<Object> eventStream = hearingAggregate.markAsDuplicate(hearingId, caseIds, defendantIds).collect(toList());

        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void shouldDeleteHearingForProsecutionCaseAndCourtApplication() {

        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withOffences(singletonList(Offence.offence()
                                                        .withId(offenceId)
                                                        .build()))
                                                .build()))
                                        .build()
                        )
                )
                .withCourtApplications(
                        asList(
                                CourtApplication.courtApplication()
                                        .withId(courtApplicationId)
                                        .withSubject(CourtApplicationParty.courtApplicationParty().build())
                                        .build()
                        )
                )
                .build();

        setField(hearingAggregate, "hearing", hearing);
        final List<Object> eventStream = hearingAggregate.deleteHearing(hearingId).collect(toList());

        assertThat(eventStream.size(), is(4));
        final HearingDeleted hearingDeleted = (HearingDeleted) eventStream.get(0);
        assertThat(hearingDeleted.getHearingId(), is(hearingId));
        assertThat(hearingDeleted.getProsecutionCaseIds().get(0), is(prosecutionCaseId));
        assertThat(hearingDeleted.getCourtApplicationIds().get(0), is(courtApplicationId));
        final DeletedHearingPopulatedToProbationCaseworker deletedHearingPopulatedToProbationCaseworker = (DeletedHearingPopulatedToProbationCaseworker) eventStream.get(2);
        assertThat(deletedHearingPopulatedToProbationCaseworker.getHearing().getId(), is(hearingId));
        assertThat(deletedHearingPopulatedToProbationCaseworker.getHearing().getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(deletedHearingPopulatedToProbationCaseworker.getHearing().getCourtApplications().get(0).getId(), is(courtApplicationId));

        final OffenceInHearingDeleted offenceInHearingDeleted = (OffenceInHearingDeleted) eventStream.get(1);
        assertThat(offenceInHearingDeleted.getProsecutionCaseIds().get(0), is(prosecutionCaseId));
        assertThat(offenceInHearingDeleted.getOffenceIds().get(0), is(offenceId));
    }

    @Test
    public void shouldRemoveProsecutionCaseWhenAllOffencesAreRemoved() {

        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId1 = randomUUID();
        final UUID prosecutionCaseId2 = randomUUID();
        final UUID prosecutionCaseId3 = randomUUID();

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();

        final UUID offence1 = randomUUID();
        final UUID offence2 = randomUUID();
        final UUID offence3 = randomUUID();
        final UUID offence4 = randomUUID();

        final ProsecutionCase prosecutionCase1 = createProsecutionCase(prosecutionCaseId1, defendantId1, offence1, offence2);
        final ProsecutionCase prosecutionCase2 = createProsecutionCase(prosecutionCaseId2, defendantId2, offence3, offence4);
        final ProsecutionCase prosecutionCase3 = createProsecutionCase(prosecutionCaseId3, defendantId3, offence3, offence4);

        final Hearing hearing = Hearing
                .hearing()
                .withProsecutionCases(new ArrayList<>(Arrays.asList(prosecutionCase1, prosecutionCase2, prosecutionCase3))).build();
        setField(hearingAggregate, "hearing", hearing);

        final List<Object> eventStream1 = hearingAggregate.removeOffenceFromHearing(hearingId, Arrays.asList(offence1)).collect(toList());

        assertThat(eventStream1.size(), is(1));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) eventStream1.get(0);
        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getProsecutionCaseIds().size(), is(0));
    }


    @Test
    public void shouldRemoveDefendantWhenAllOffencesAreRemoved() {

        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        final UUID offence1 = randomUUID();
        final UUID offence2 = randomUUID();
        final UUID offence3 = randomUUID();

        //defendant to be removed
        final Defendant defendantToBeRemoved = Defendant.defendant()
                .withId(defendantId1)
                .withOffences(
                        Stream.of(Offence.offence().withId(offence1).build(),
                                        Offence.offence().withId(offence2).build())
                                .collect(toList())).build();
        //other defendant
        final Defendant defendantToBeRemained = Defendant.defendant()
                .withId(defendantId2)
                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence().withId(offence3).build()))).build();


        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(Stream.of(defendantToBeRemoved, defendantToBeRemained).collect(toList()))
                .build();

        final Hearing hearing = Hearing
                .hearing()
                .withProsecutionCases(new ArrayList<>(Arrays.asList(prosecutionCase))).build();
        setField(hearingAggregate, "hearing", hearing);

        final List<Object> eventStream1 = hearingAggregate.removeOffenceFromHearing(hearingId, Arrays.asList(offence1, offence2)).collect(toList());

        assertThat(eventStream1.size(), is(1));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) eventStream1.get(0);
        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getDefendantIds(), hasItem(defendantId1));
        assertThat(offencesRemovedFromHearing.getProsecutionCaseIds().size(), is(0));
    }

    @Test
    public void shouldNotRemoveDefendantWhenOneOffenceRemoved() {

        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        final UUID offence1 = randomUUID();
        final UUID offence2 = randomUUID();
        final UUID offence3 = randomUUID();

        //defendant to be removed
        final Defendant defendantToBeRemoved = Defendant.defendant()
                .withId(defendantId1)
                .withOffences(
                        Stream.of(Offence.offence().withId(offence1).build(),
                                Offence.offence().withId(offence2).build())
                                .collect(toList())).build();
        //other defendant
        final Defendant defendantToBeRemained = Defendant.defendant()
                .withId(defendantId2)
                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence().withId(offence3).build()))).build();


        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(Stream.of(defendantToBeRemoved, defendantToBeRemained).collect(toList()))
                .build();

        final Hearing hearing = Hearing
                .hearing()
                .withProsecutionCases(new ArrayList<>(Arrays.asList(prosecutionCase))).build();
        setField(hearingAggregate, "hearing", hearing);

        final List<Object> eventStream1 = hearingAggregate.removeOffenceFromHearing(hearingId, Arrays.asList(offence1)).collect(toList());

        assertThat(eventStream1.size(), is(1));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) eventStream1.get(0);
        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getDefendantIds().size(), is(0));
        assertThat(offencesRemovedFromHearing.getProsecutionCaseIds().size(), is(0));
    }

    @Test
    public void shouldNotRaiseEventWhenOneOffenceIsNotInHearing() {

        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        final UUID offence1 = randomUUID();
        final UUID offence2 = randomUUID();
        final UUID offence3 = randomUUID();

        final Defendant defendantToBeRemoved = Defendant.defendant()
                .withId(defendantId1)
                .withOffences(
                        Stream.of(Offence.offence().withId(offence1).build(),
                                        Offence.offence().withId(offence2).build())
                                .collect(toList())).build();

        final Defendant defendantToBeRemained = Defendant.defendant()
                .withId(defendantId2)
                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence().withId(offence3).build()))).build();


        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(Stream.of(defendantToBeRemoved, defendantToBeRemained).collect(toList()))
                .build();

        final Hearing hearing = Hearing
                .hearing()
                .withProsecutionCases(new ArrayList<>(Arrays.asList(prosecutionCase))).build();
        setField(hearingAggregate, "hearing", hearing);

        final List<Object> eventStream1 = hearingAggregate.removeOffenceFromHearing(hearingId, Arrays.asList(randomUUID())).collect(toList());

        assertThat(eventStream1.size(), is(0));

    }


    @Test
    public void shouldPopulateHearingPopulatedToProbationCaseworker() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();

        final Hearing hearing = getHearingForVej(prosecutionCaseId, courtApplicationId, hearingId, offenceId);
        setField(hearingAggregate, "hearing", hearing);
        final List<Object> eventStream1 = hearingAggregate.populateHearingToProbationCaseWorker().collect(toList());
        assertThat(eventStream1.size(), is(1));
        final HearingPopulatedToProbationCaseworker eventHearing = (HearingPopulatedToProbationCaseworker) eventStream1.get(0);
        assertThat(eventHearing.getHearing().getProsecutionCases().size(), is(1));
        assertThat(hearingId, is(eventHearing.getHearing().getId()));
        final ProsecutionCase prosecutionCase = eventHearing.getHearing().getProsecutionCases().get(0);
        assertThat(prosecutionCase.getDefendants().size(), is(1));
        assertThat(prosecutionCaseId, is(prosecutionCase.getId()));
        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        assertThat(defendant.getOffences().size(), is(1));
        final Offence offence = defendant.getOffences().get(0);
        assertThat(offenceId, is(offence.getId()));

        assertThat(eventHearing.getHearing().getCourtApplications().size(), is(1));
        final CourtApplication courtApplication = eventHearing.getHearing().getCourtApplications().get(0);
        assertThat(courtApplicationId, is(courtApplication.getId()));

    }

    private Hearing getHearingForVej(final UUID prosecutionCaseId, final UUID courtApplicationId, final UUID hearingId, final UUID offenceId) {
        final Hearing hearing = getHearingBuilder(Hearing.hearing()
                .withId(hearingId), getProsecutionCaseCases(prosecutionCaseId, offenceId))
                .withCourtApplications(
                        getCourtApplications(courtApplicationId)
                )
                .build();
        return hearing;
    }

    private Hearing getHearingForVej(final UUID prosecutionCaseId, final UUID courtApplicationId, final UUID hearingId, final UUID defendantId, final UUID offenceId) {
        final Hearing hearing = getHearingBuilder(Hearing.hearing()
                .withId(hearingId), getProsecutionCaseCases(prosecutionCaseId, defendantId, offenceId))
                .withCourtApplications(
                        getCourtApplications(courtApplicationId)
                )
                .build();
        return hearing;
    }

    private List<CourtApplication> getCourtApplications(final UUID courtApplicationId) {
        return asList(
                CourtApplication.courtApplication()
                        .withId(courtApplicationId)
                        .withSubject(CourtApplicationParty.courtApplicationParty().build())
                        .build()
        );
    }

    private List<ProsecutionCase> getProsecutionCaseCases(final UUID prosecutionCaseId, final UUID offenceId) {
       final List<Defendant> defendantList =  new ArrayList<>();
       defendantList.add(Defendant.defendant().withId(randomUUID())
               .withOffences(singletonList(Offence.offence()
                       .withId(offenceId)
                       .withVerdict(Verdict.verdict().withOffenceId(offenceId).withVerdictType(VerdictType.verdictType().withId(randomUUID()).withCategory("test").withCategoryType("test type").build()).build())
                       .build()))
               .build());
        return asList(
                ProsecutionCase.prosecutionCase()
                        .withId(prosecutionCaseId)
                        .withDefendants(defendantList)
                        .build()
        );
    }

    private List<ProsecutionCase> getProsecutionCaseCases(final UUID prosecutionCaseId, final UUID defendantId, final UUID offenceId) {
        return asList(
                ProsecutionCase.prosecutionCase()
                        .withId(prosecutionCaseId)
                        .withDefendants(asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(singletonList(Offence.offence()
                                        .withId(offenceId)
                                        .build()))
                                .build()))
                        .build()
        );
    }

    private Hearing getHearingForVejWithoutCourtApplications(final UUID prosecutionCaseId, final UUID courtApplicationId, final UUID hearingId, final UUID offenceId) {
        final Hearing hearing = getHearingBuilder(Hearing.hearing()
                .withId(hearingId), getProsecutionCaseCases(prosecutionCaseId, offenceId))
                .build();
        return hearing;
    }

    private Hearing.Builder getHearingBuilder(final Hearing.Builder hearingId, final List<ProsecutionCase> prosecutionCaseId) {
        return hearingId
                .withProsecutionCases(
                        prosecutionCaseId
                );
    }

    @Test
    public void shouldDeletedHearingPopulatedToProbationCaseworker() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();

        final Hearing hearing = getHearingForVej(prosecutionCaseId, courtApplicationId, hearingId, offenceId);
        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "deleted", true);
        final List<Object> eventStream1 = hearingAggregate.populateHearingToProbationCaseWorker().collect(toList());
        assertThat(eventStream1.size(), is(1));
        final DeletedHearingPopulatedToProbationCaseworker eventHearing = (DeletedHearingPopulatedToProbationCaseworker) eventStream1.get(0);
        assertThat(eventHearing.getHearing().getProsecutionCases().size(), is(1));
        assertThat(hearingId, is(eventHearing.getHearing().getId()));
        final ProsecutionCase prosecutionCase = eventHearing.getHearing().getProsecutionCases().get(0);
        assertThat(prosecutionCase.getDefendants().size(), is(1));
        assertThat(prosecutionCaseId, is(prosecutionCase.getId()));
        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        assertThat(defendant.getOffences().size(), is(1));
        final Offence offence = defendant.getOffences().get(0);
        assertThat(offenceId, is(offence.getId()));

        assertThat(eventHearing.getHearing().getCourtApplications().size(), is(1));
        final CourtApplication courtApplication = eventHearing.getHearing().getCourtApplications().get(0);
        assertThat(courtApplicationId, is(courtApplication.getId()));

    }

    @Test
    public void shouldPopulateHearingToProbationCaseWorkerWithoutProsecutionCasesAndCourtApplications() {
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();
        setField(hearingAggregate, "hearing", hearing);
        final List<Object> eventStream1 = hearingAggregate.populateHearingToProbationCaseWorker().collect(toList());
        assertThat(eventStream1.size(), is(0));
    }

    @Test
    public void shouldPopulateHearingToProbationCaseWorkerWithHearingStatusSentForListing() {
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();
        final HearingListingStatus hearingListingStatus = HearingListingStatus.SENT_FOR_LISTING;
        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);
        final List<Object> eventStream1 = hearingAggregate.populateHearingToProbationCaseWorker().collect(toList());
        assertThat(eventStream1.size(), is(0));
    }

    @Test
    public void shouldPopulateHearingToProbationCaseWorkerWithBoxHearing() {
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing().withId(hearingId).withIsBoxHearing(true).build();
        setField(hearingAggregate, "hearing", hearing);

        final List<Object> eventStream1 = hearingAggregate.populateHearingToProbationCaseWorker().collect(toList());
        assertThat(eventStream1.size(), is(0));
    }

    @Test
    public void shouldPopulateHearingToProbationCaseWorkerWithHearingStatusHearingResulted() {
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_RESULTED;
        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);
        final List<Object> eventStream1 = hearingAggregate.populateHearingToProbationCaseWorker().collect(toList());
        assertThat(eventStream1.size(), is(0));
    }

    @Test
    public void shouldUpdateDefendantListingStatusHearingResulted() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final Boolean notifyNCES = true;
        final Hearing hearing = getHearingForVej(prosecutionCaseId, courtApplicationId, hearingId, offenceId);
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_RESULTED;
        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);
        setField(hearingAggregate, "notifyNCES", notifyNCES);
        final List<Object> eventStream1 = hearingAggregate.updateDefendantListingStatus(hearing, hearingListingStatus, notifyNCES,null).collect(toList());
        assertThat(eventStream1.size(), is(2));
        ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = (ProsecutionCaseDefendantListingStatusChangedV2) eventStream1.get(0);
        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV2.getHearing().getId()));
    }

    @Test
    public void shouldUpdateDefendantListingStatusHearingInitialised() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();

        final Hearing hearing = getHearingForVejWithoutCourtApplications(prosecutionCaseId, courtApplicationId, hearingId, offenceId);
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_INITIALISED;
        final Boolean notifyNCES = true;
        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);
        setField(hearingAggregate, "notifyNCES", notifyNCES);
        final List<Object> eventStream1 = hearingAggregate.updateDefendantListingStatus(hearing, hearingListingStatus, notifyNCES, null).collect(toList());
        assertThat(eventStream1.size(), is(4));
        UnscheduledHearingAllocationNotified unscheduledHearingAllocationNotified = (UnscheduledHearingAllocationNotified) eventStream1.get(0);
        assertThat(hearingId, is(unscheduledHearingAllocationNotified.getHearing().getId()));
        ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = (ProsecutionCaseDefendantListingStatusChangedV2) eventStream1.get(1);
        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV2.getHearing().getId()));
        HearingPopulatedToProbationCaseworker hearingPopulatedToProbationCaseworker = (HearingPopulatedToProbationCaseworker) eventStream1.get(2);
        assertThat(hearingId, is(hearingPopulatedToProbationCaseworker.getHearing().getId()));
        VejHearingPopulatedToProbationCaseworker vejHearingPopulatedToProbationCaseworker = (VejHearingPopulatedToProbationCaseworker) eventStream1.get(3);
        assertThat(hearingId, is(vejHearingPopulatedToProbationCaseworker.getHearing().getId()));
    }

    @Test
    public void shouldUpdateMasterDefendantIdAndUpdateDefendantListingStatusHearingInitialised() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();

        final Hearing hearing = getHearingForVejWithoutCourtApplications(prosecutionCaseId, courtApplicationId, hearingId, offenceId);
        final UUID masterDefendantId = randomUUID();
        final UUID defendantId = randomUUID();
        final Defendant defendant = Defendant.defendant().withId(defendantId).withMasterDefendantId(masterDefendantId)
                .withOffences(singletonList(Offence.offence()
                        .withId(randomUUID())
                        .build()))
                .build();
        hearing.getProsecutionCases().get(0).getDefendants().add(defendant);
        final Hearing updatedHearing = getHearingForVejWithoutCourtApplications(prosecutionCaseId, courtApplicationId, hearingId, offenceId);
        updatedHearing.getProsecutionCases().get(0).getDefendants().add(defendant);

        final Defendant defendantWithoutMasterId = Defendant.defendant().withValuesFrom(defendant).withMasterDefendantId(null).build();
        hearing.getProsecutionCases().get(0).getDefendants().add(defendantWithoutMasterId);
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_INITIALISED;
        final Boolean notifyNCES = true;
        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);
        setField(hearingAggregate, "notifyNCES", notifyNCES);


        final List<Object> eventStream1 = hearingAggregate.updateDefendantListingStatus(updatedHearing, hearingListingStatus, notifyNCES, null).collect(toList());
        assertThat(eventStream1.size(), is(4));
        UnscheduledHearingAllocationNotified unscheduledHearingAllocationNotified = (UnscheduledHearingAllocationNotified) eventStream1.get(0);
        assertThat(hearingId, is(unscheduledHearingAllocationNotified.getHearing().getId()));
        ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = (ProsecutionCaseDefendantListingStatusChangedV2) eventStream1.get(1);
        assertThat(masterDefendantId, is(prosecutionCaseDefendantListingStatusChangedV2.getHearing().getProsecutionCases().stream().flatMap(c -> c.getDefendants().stream()).filter(d -> d.getMasterDefendantId() != null).map(d -> d.getMasterDefendantId()).findFirst().get()));
        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV2.getHearing().getId()));
        HearingPopulatedToProbationCaseworker hearingPopulatedToProbationCaseworker = (HearingPopulatedToProbationCaseworker) eventStream1.get(2);
        assertThat(hearingId, is(hearingPopulatedToProbationCaseworker.getHearing().getId()));
        VejHearingPopulatedToProbationCaseworker vejHearingPopulatedToProbationCaseworker = (VejHearingPopulatedToProbationCaseworker) eventStream1.get(3);
        assertThat(hearingId, is(vejHearingPopulatedToProbationCaseworker.getHearing().getId()));
    }

    @Test
    public void shouldDeleteHearing() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();

        final Hearing hearing = getHearingForVejWithoutCourtApplications(prosecutionCaseId, courtApplicationId, hearingId, offenceId);
        setField(hearingAggregate, "hearing", hearing);

        final List<Object> eventStream1 = hearingAggregate.deleteHearing(hearing.getId()).collect(toList());
        assertThat(eventStream1.size(), is(4));
        HearingDeleted hearingDeleted = (HearingDeleted) eventStream1.get(0);
        assertThat(hearingId, is(hearingDeleted.getHearingId()));
        OffenceInHearingDeleted offenceInHearingDeleted = (OffenceInHearingDeleted) eventStream1.get(1);
        assertThat(offenceId, is(offenceInHearingDeleted.getOffenceIds().get(0)));
        DeletedHearingPopulatedToProbationCaseworker deletedHearingPopulatedToProbationCaseworker = (DeletedHearingPopulatedToProbationCaseworker) eventStream1.get(2);
        assertThat(hearingId, is(deletedHearingPopulatedToProbationCaseworker.getHearing().getId()));
        VejDeletedHearingPopulatedToProbationCaseworker vejDeletedHearingPopulatedToProbationCaseworker = (VejDeletedHearingPopulatedToProbationCaseworker) eventStream1.get(3);
        assertThat(hearingId, is(vejDeletedHearingPopulatedToProbationCaseworker.getHearing().getId()));
    }


    @Test
    public void shouldDeleteHearingReturnsEmptyWhenHearingObjectIsNull() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();

        final Hearing hearing = getHearingForVejWithoutCourtApplications(prosecutionCaseId, courtApplicationId, hearingId, offenceId);
        setField(hearingAggregate, "hearing", null);

        final List<Object> eventStream = hearingAggregate.deleteHearing(hearing.getId()).collect(toList());
        assertThat(eventStream.size(), is(0));

    }

    @Test
    public void shouldUpdateApplication() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();

        final Hearing hearing = getHearingForVej(prosecutionCaseId, courtApplicationId, hearingId, offenceId);

        setField(hearingAggregate, "hearing", hearing);

        final List<Object> eventStream1 = hearingAggregate.updateApplication(hearing.getCourtApplications().get(0)).collect(toList());
        assertThat(eventStream1.size(), is(3));
    }

    @Test
    public void shouldUpdateAllocationFields() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID roomId = randomUUID();
        final UUID hearingTypeId = randomUUID();
        final Hearing hearing = getHearingForVej(prosecutionCaseId, courtApplicationId, hearingId, offenceId);
        final CourtApplication courtApplication = getCourtApplications(courtApplicationId).get(0);
        Address address = getAddress();
        CourtCentre courtCentre = getCourtCentre(courtCentreId, roomId, address);
        List<HearingDay> hearingDays = asList(getHearingDay(courtCentreId, roomId));
        HearingLanguage hearingLanguage = HearingLanguage.ENGLISH;
        HearingType hearingType = getHearingType(hearingTypeId);
        final UpdateHearingForAllocationFields updateHearingForAllocationFields = getUpdateHearingForAllocationFields(hearingId, hearingDays, hearingLanguage, hearingType, courtCentre, courtApplication);

        setField(hearingAggregate, "hearing", hearing);

        final List<Object> eventStream1 = hearingAggregate.updateAllocationFields(updateHearingForAllocationFields).collect(toList());
        assertThat(eventStream1.size(), is(3));
        HearingUpdatedForAllocationFields hearingUpdatedForAllocationFields = (HearingUpdatedForAllocationFields) eventStream1.get(0);
        assertThat(hearingDays, is(hearingUpdatedForAllocationFields.getHearingDays()));
        HearingPopulatedToProbationCaseworker hearingPopulatedToProbationCaseworker = (HearingPopulatedToProbationCaseworker) eventStream1.get(1);
        assertThat(hearingId, is(hearingPopulatedToProbationCaseworker.getHearing().getId()));
    }

    @Test
    public void shouldRaiseRelatedHearingWithAddedOffencesInSameDefendantAndCaseWhenCaseSplitAtOffenceLevelAndMergedBack() {

        final UUID defendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final Hearing hearing = getHearingForVej(prosecutionCaseId, courtApplicationId, hearingId, defendantId, offenceId);
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_INITIALISED;

        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);

        HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(newArrayList(ProsecutionCase.prosecutionCase().withId(prosecutionCaseId).withDefendants(new ArrayList<>(singletonList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(asList(Offence.offence().withId(offenceId2).build()))
                        .build()))).build()))
                .build();
        final SeedingHearing seedingHearing = seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build();
        final List<Object> eventStream = hearingAggregate.updateRelatedHearing(hearingListingNeeds, true, randomUUID(),false, seedingHearing, null).collect(toList());

        assertThat(eventStream.size(), is(2));

        ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = (ProsecutionCaseDefendantListingStatusChangedV2) eventStream.get(0);

        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV2.getHearing().getId()));
        RelatedHearingUpdated relatedHearingUpdated = (RelatedHearingUpdated) eventStream.get(1);
        assertThat(relatedHearingUpdated.getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().size(), is(1));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(0).getDefendants().get(0).getOffences().size(), is(2));


    }

    @Test
    public void shouldRaiseRelatedHearingWithAddedOffencesInSameDefendantAndCaseWhenCaseSplitAtOffenceLevelWithMultipleOffencesAndMergedBack() {

        final UUID defendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID offenceId4 = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final Hearing hearing = getHearingForVej(prosecutionCaseId, courtApplicationId, hearingId, defendantId, offenceId);
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_INITIALISED;

        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);

        HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(newArrayList(ProsecutionCase.prosecutionCase().withId(prosecutionCaseId).withDefendants(new ArrayList<>(singletonList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(asList(Offence.offence().withId(offenceId2).build(), Offence.offence().withId(offenceId3).build(),Offence.offence().withId(offenceId4).build()))
                        .build()))).build()))
                .build();
        final SeedingHearing seedingHearing = seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build();
        final List<Object> eventStream = hearingAggregate.updateRelatedHearing(hearingListingNeeds, true, randomUUID(),false, seedingHearing, null).collect(toList());

        assertThat(eventStream.size(), is(2));

        ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = (ProsecutionCaseDefendantListingStatusChangedV2) eventStream.get(0);

        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV2.getHearing().getId()));
        RelatedHearingUpdated relatedHearingUpdated = (RelatedHearingUpdated) eventStream.get(1);
        assertThat(relatedHearingUpdated.getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().size(), is(1));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(0).getDefendants().get(0).getOffences().size(), is(4));


    }

    @Test
    public void shouldRaiseRelatedHearingWithAddedDefendantsInSameCaseWhenCaseSplitAtDefendantLevelAndMergedBack() {

        final UUID defendantId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final Hearing hearing = getHearingForVej(prosecutionCaseId, courtApplicationId, hearingId, defendantId, offenceId);
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_INITIALISED;

        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);

        HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(newArrayList(ProsecutionCase.prosecutionCase().withId(prosecutionCaseId).withDefendants(new ArrayList<>(singletonList(Defendant.defendant()
                        .withId(defendantId2)
                        .withOffences(asList(Offence.offence().withId(offenceId2).build()))
                        .build()))).build()))
                .build();
        final SeedingHearing seedingHearing = seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build();
        final List<Object> eventStream = hearingAggregate.updateRelatedHearing(hearingListingNeeds, true, randomUUID(),false, seedingHearing, null).collect(toList());

        assertThat(eventStream.size(), is(2));

        ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = (ProsecutionCaseDefendantListingStatusChangedV2) eventStream.get(0);

        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV2.getHearing().getId()));
        RelatedHearingUpdated relatedHearingUpdated = (RelatedHearingUpdated) eventStream.get(1);
        assertThat(relatedHearingUpdated.getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().size(), is(1));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(0).getDefendants().size(), is(2));


    }

    @Test
    public void shouldRaiseRelatedHearingWithDifferentCasetAndMergedBack() {

        final UUID defendantId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID prosecutionCaseId2 = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final Hearing hearing = getHearingForVej(prosecutionCaseId, courtApplicationId, hearingId, defendantId, offenceId);
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_INITIALISED;

        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);

        HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(newArrayList(ProsecutionCase.prosecutionCase().withId(prosecutionCaseId2).withDefendants(new ArrayList<>(singletonList(Defendant.defendant()
                        .withId(defendantId2)
                        .withOffences(asList(Offence.offence().withId(offenceId2).build()))
                        .build()))).build()))
                .build();
        final SeedingHearing seedingHearing = seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build();
        final List<Object> eventStream = hearingAggregate.updateRelatedHearing(hearingListingNeeds, true, randomUUID(),false, seedingHearing, null).collect(toList());

        assertThat(eventStream.size(), is(2));

        ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = (ProsecutionCaseDefendantListingStatusChangedV2) eventStream.get(0);

        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV2.getHearing().getId()));
        RelatedHearingUpdated relatedHearingUpdated = (RelatedHearingUpdated) eventStream.get(1);
        assertThat(relatedHearingUpdated.getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().size(), is(2));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(1).getDefendants().size(), is(1));


    }

    @Test
    public void shouldRaiseRelatedHearingWithAddedMultipleDefendantsInSameCaseWhenCaseSplitAtDefendantLevelAndMergedBack() {

        final UUID defendantId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID offenceId4 = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final Hearing hearing = getHearingForVej(prosecutionCaseId, courtApplicationId, hearingId, defendantId, offenceId);
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_INITIALISED;

        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);

        HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(newArrayList(ProsecutionCase.prosecutionCase().withId(prosecutionCaseId).withDefendants(asList(Defendant.defendant()
                        .withId(defendantId2)
                        .withOffences(asList(Offence.offence().withId(offenceId2).build()))
                        .build(),Defendant.defendant()
                        .withId(defendantId3)
                        .withOffences(asList(Offence.offence().withId(offenceId3).build(),Offence.offence().withId(offenceId4).build()))
                        .build())).build()))
                .build();
        final SeedingHearing seedingHearing = seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build();
        final List<Object> eventStream = hearingAggregate.updateRelatedHearing(hearingListingNeeds, true, randomUUID(),false, seedingHearing, null).collect(toList());

        assertThat(eventStream.size(), is(2));

        ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = (ProsecutionCaseDefendantListingStatusChangedV2) eventStream.get(0);

        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV2.getHearing().getId()));
        RelatedHearingUpdated relatedHearingUpdated = (RelatedHearingUpdated) eventStream.get(1);
        assertThat(relatedHearingUpdated.getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().size(), is(1));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(0).getDefendants().size(), is(3));
        final Defendant defendant3 = relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(0).getDefendants().stream().filter(defendant -> defendant.getId().equals(defendantId3)).findFirst().get();
        assertThat(defendant3.getOffences().size(), is(2));


    }

    @Test
    public void testUpdateHearingDetailsInUnifiedSearch() {

        final UUID hearingId = randomUUID();
        setField(hearingAggregate, "hearing", Hearing.hearing()
                .withId(hearingId)
                .build());

        final List<Object> events = hearingAggregate.updateHearingDetailsInUnifiedSearch(hearingId).collect(toList());

        final CaseHearingDetailsUpdatedInUnifiedSearch caseHearingDetailsUpdatedInUnifiedSearch = (CaseHearingDetailsUpdatedInUnifiedSearch) events.get(0);
        assertThat(caseHearingDetailsUpdatedInUnifiedSearch.getHearing().getId(), is(hearingId));

    }

    @Test
    public void shouldRaiseDefendantUpdatedEventWhenTheHearingNotDeleted(){
        final UUID hearingId = randomUUID();
        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate().build();

        final List<Object> events = hearingAggregate.updateDefendant(hearingId, defendantUpdate).
                collect(toList());

        final HearingDefendantUpdated hearingDefendantUpdated = (HearingDefendantUpdated) events.get(0);
        assertThat(hearingDefendantUpdated.getHearingId(), is(hearingId));
        assertThat(hearingDefendantUpdated.getDefendant(), is(defendantUpdate));
    }

    @Test
    public void shouldNotRaiseDefendantUpdatedEventWhenTheHearingDeleted(){
        final UUID hearingId = randomUUID();
        hearingAggregate.deleteHearing(hearingId);

        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate().build();
        final List<Object> events = hearingAggregate.updateDefendant(hearingId, defendantUpdate).
                collect(toList());

        assertThat(events.isEmpty(), is(true));

    }

    @Test
    public void shouldNotRaiseDefendantUpdatedEventWhenTheHearingDuplicated(){
        final UUID hearingId = randomUUID();
        hearingAggregate.markAsDuplicate(hearingId, new ArrayList<>(), new ArrayList<>());

        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate().build();
        final List<Object> events = hearingAggregate.updateDefendant(hearingId, defendantUpdate).
                collect(toList());

        assertThat(events.isEmpty(), is(true));

    }

    @Test
    public void shouldUpdateDefendantListingStatusHearingInitialisedWithListNextHearings() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();

        final Hearing hearing = getHearing(prosecutionCaseId, courtApplicationId, hearingId, offenceId, seedingHearingId);
        final ListNextHearingsV3 listNextHearings = buildListNextHearings(hearingId, seedingHearingId);
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_INITIALISED;
        final Boolean notifyNCES = true;
        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);
        setField(hearingAggregate, "notifyNCES", notifyNCES);
        final List<Object> eventStream1 = hearingAggregate.updateDefendantListingStatusV3(hearing, hearingListingStatus, notifyNCES, listNextHearings).collect(toList());
        assertThat(eventStream1.size(), is(3));
        ProsecutionCaseDefendantListingStatusChangedV3 prosecutionCaseDefendantListingStatusChangedV3 = (ProsecutionCaseDefendantListingStatusChangedV3) eventStream1.get(0);
        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV3.getHearing().getId()));

        assertThat(prosecutionCaseDefendantListingStatusChangedV3.getListNextHearings().getHearings().size(), is(1));
        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV3.getListNextHearings().getHearings().get(0).getId()));
        assertThat(seedingHearingId, is(prosecutionCaseDefendantListingStatusChangedV3.getListNextHearings().getSeedingHearing().getSeedingHearingId()));

        HearingPopulatedToProbationCaseworker hearingPopulatedToProbationCaseworker = (HearingPopulatedToProbationCaseworker) eventStream1.get(1);
        assertThat(hearingId, is(hearingPopulatedToProbationCaseworker.getHearing().getId()));
        VejHearingPopulatedToProbationCaseworker vejHearingPopulatedToProbationCaseworker = (VejHearingPopulatedToProbationCaseworker) eventStream1.get(2);
        assertThat(hearingId, is(vejHearingPopulatedToProbationCaseworker.getHearing().getId()));
    }

    @Test
    public void shouldUpdateDefendantListingStatusHearingResultedWithListNextHearings() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final Boolean notifyNCES = true;

        final Hearing hearing = getHearing(prosecutionCaseId, courtApplicationId, hearingId, offenceId, seedingHearingId);
        final ListNextHearingsV3 listNextHearings = buildListNextHearings(hearingId, seedingHearingId);
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_RESULTED;
        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);
        setField(hearingAggregate, "notifyNCES", notifyNCES);
        final List<Object> eventStream1 = hearingAggregate.updateDefendantListingStatusV3(hearing, HearingListingStatus.SENT_FOR_LISTING, notifyNCES, listNextHearings).collect(toList());
        assertThat(eventStream1.size(), is(2));
        ProsecutionCaseDefendantListingStatusChangedV3 prosecutionCaseDefendantListingStatusChangedV3 = (ProsecutionCaseDefendantListingStatusChangedV3) eventStream1.get(0);
        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV3.getHearing().getId()));
        assertThat(HearingListingStatus.HEARING_RESULTED, is(prosecutionCaseDefendantListingStatusChangedV3.getHearingListingStatus()));

        assertThat(prosecutionCaseDefendantListingStatusChangedV3.getListNextHearings().getHearings().size(), is(1));
        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV3.getListNextHearings().getHearings().get(0).getId()));
        assertThat(seedingHearingId, is(prosecutionCaseDefendantListingStatusChangedV3.getListNextHearings().getSeedingHearing().getSeedingHearingId()));
    }

    @Test
    public void shouldUpdateMasterDefendantIdAndUpdateDefendantListingStatusHearingInitialisedWithListNextHearings() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();

        final Hearing hearing = getHearingForVejWithoutCourtApplications(prosecutionCaseId, courtApplicationId, hearingId, offenceId);
        final ListNextHearingsV3 listNextHearings = buildListNextHearings(hearingId, seedingHearingId);
        final UUID masterDefendantId = randomUUID();
        final UUID defendantId = randomUUID();
        final Defendant defendant = Defendant.defendant().withId(defendantId).withMasterDefendantId(masterDefendantId)
                .withOffences(singletonList(Offence.offence()
                        .withId(randomUUID())
                        .build()))
                .build();
        hearing.getProsecutionCases().get(0).getDefendants().add(defendant);
        final Hearing updatedHearing = getHearingForVejWithoutCourtApplications(prosecutionCaseId, courtApplicationId, hearingId, offenceId);
        updatedHearing.getProsecutionCases().get(0).getDefendants().add(defendant);

        final Defendant defendantWithoutMasterId = Defendant.defendant().withValuesFrom(defendant).withMasterDefendantId(null).build();
        hearing.getProsecutionCases().get(0).getDefendants().add(defendantWithoutMasterId);
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_INITIALISED;
        final Boolean notifyNCES = true;
        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);
        setField(hearingAggregate, "notifyNCES", notifyNCES);

        final List<Object> eventStream1 = hearingAggregate.updateDefendantListingStatusV3(updatedHearing, HearingListingStatus.HEARING_INITIALISED, notifyNCES, listNextHearings).collect(toList());
        assertThat(eventStream1.size(), is(3));
        ProsecutionCaseDefendantListingStatusChangedV3 prosecutionCaseDefendantListingStatusChangedV3 = (ProsecutionCaseDefendantListingStatusChangedV3) eventStream1.get(0);
        assertThat(masterDefendantId, is(prosecutionCaseDefendantListingStatusChangedV3.getHearing().getProsecutionCases().stream().flatMap(c -> c.getDefendants().stream()).filter(d -> d.getMasterDefendantId() != null).map(d -> d.getMasterDefendantId()).findFirst().get()));
        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV3.getHearing().getId()));

        assertThat(prosecutionCaseDefendantListingStatusChangedV3.getListNextHearings().getHearings().size(), is(1));
        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV3.getListNextHearings().getHearings().get(0).getId()));
        assertThat(seedingHearingId, is(prosecutionCaseDefendantListingStatusChangedV3.getListNextHearings().getSeedingHearing().getSeedingHearingId()));

        HearingPopulatedToProbationCaseworker hearingPopulatedToProbationCaseworker = (HearingPopulatedToProbationCaseworker) eventStream1.get(1);
        assertThat(hearingId, is(hearingPopulatedToProbationCaseworker.getHearing().getId()));
        VejHearingPopulatedToProbationCaseworker vejHearingPopulatedToProbationCaseworker = (VejHearingPopulatedToProbationCaseworker) eventStream1.get(2);
        assertThat(hearingId, is(vejHearingPopulatedToProbationCaseworker.getHearing().getId()));
    }

    @Test
    public void shouldNotUpdateDefendantListingStatusHearingSentForListingWithListNextHearings() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final Boolean notifyNCES = true;

        final Hearing hearing = getHearing(prosecutionCaseId, courtApplicationId, hearingId, offenceId, seedingHearingId);
        final ListNextHearingsV3 listNextHearings = buildListNextHearings(hearingId, seedingHearingId);
        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_RESULTED;
        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);
        setField(hearingAggregate, "notifyNCES", notifyNCES);
        final List<Object> eventStream = hearingAggregate.updateDefendantListingStatusV3(hearing, HearingListingStatus.SENT_FOR_LISTING, notifyNCES, listNextHearings).collect(toList());
        assertThat(eventStream.size(), is(2));
        ProsecutionCaseDefendantListingStatusChangedV3 prosecutionCaseDefendantListingStatusChangedV3 = (ProsecutionCaseDefendantListingStatusChangedV3) eventStream.get(0);
        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV3.getHearing().getId()));
        assertThat(HearingListingStatus.HEARING_RESULTED, is(prosecutionCaseDefendantListingStatusChangedV3.getHearingListingStatus()));

        assertThat(seedingHearingId, is(prosecutionCaseDefendantListingStatusChangedV3.getListNextHearings().getSeedingHearing().getSeedingHearingId()));
        assertThat(prosecutionCaseDefendantListingStatusChangedV3.getListNextHearings().getHearings().size(), is(1));
        assertThat(hearingId, is(prosecutionCaseDefendantListingStatusChangedV3.getListNextHearings().getHearings().get(0).getId()));
        assertThat(seedingHearingId, is(prosecutionCaseDefendantListingStatusChangedV3.getListNextHearings().getSeedingHearing().getSeedingHearingId()));
    }

    private HearingType getHearingType(UUID hearingTypeId) {
        return HearingType.hearingType().withId(hearingTypeId).withDescription("First Hearing").withWelshDescription("welsh").withDescription("First Hearing")
                .build();
    }

    private HearingDay getHearingDay(UUID courtCentreId, UUID roomId) {
        return HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).withListedDurationMinutes(30).withCourtRoomId(roomId).withCourtCentreId(courtCentreId)
                .withListingSequence(4).withHasSharedResults(true).withIsCancelled(false)
                .build();
    }

    private UpdateHearingForAllocationFields getUpdateHearingForAllocationFields(UUID hearingId, List<HearingDay> hearingDays, HearingLanguage english, HearingType hearingType, CourtCentre courtCentre, CourtApplication courtApplication) {
        return UpdateHearingForAllocationFields.updateHearingForAllocationFields()
                .withId(hearingId).withHearingDays(hearingDays).withHearingLanguage(english).withType(hearingType)
                .withCourtCentre(courtCentre).withCourtApplication(courtApplication)
                .build();
    }

    private CourtCentre getCourtCentre(UUID courtCentreId, UUID roomId, Address address) {
        return CourtCentre.courtCentre().withWelshCourtCentre(true).withId(courtCentreId).withCode("C")
                .withName("courtCentre").withWelshName("welsh").withRoomName("CourtRoom1").withRoomId(roomId).withWelshRoomName("Room1")
                .withAddress(address).withWelshAddress(address).withCourtHearingLocation("London").withPsaCode(1)
                .withLja(LjaDetails.ljaDetails().withLjaCode("ljaCode").withLjaName("ljaName")
                        .withWelshLjaName("welshLja")
                        .build()).withCourtLocationCode("C").
                build();
    }

    private Address getAddress() {
        return Address.address().withAddress1("address1").withAddress2("address2").withAddress3("address3").withAddress4("address4")
                .withAddress5("address5").withPostcode("E6 2AJ").withWelshAddress1("welsh1").withWelshAddress2("welsh2")
                .withWelshAddress3("welsh3").withWelshAddress4("welsh4").withWelshAddress5("welsh5").build();
    }

    private ProsecutionCase createProsecutionCase(final UUID caseId, final UUID newDefendantID, final UUID offenceId1, final UUID offenceId2) {
        final List<Offence> offences = new ArrayList<>();
        offences.add(Offence.offence()
                .withId(offenceId1)
                .build());

        offences.add(Offence.offence()
                .withId(offenceId2)
                .build());

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withId(newDefendantID)
                .withOffences(offences)
                .build());

        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(defendants)
                .build();
    }

    private DefenceCounsel getDefenceCounsel(final UUID id, final UUID userId,
                                             final String firstName, final String lastName,
                                             final String title, final String middleName,
                                             final List<UUID> defendantList, final List<LocalDate> attendanceDays) {
        final DefenceCounsel defenceCounsel =  DefenceCounsel.defenceCounsel()
                .withId(id)
                .withDefendants(defendantList)
                .withAttendanceDays(attendanceDays)
                .withFirstName(firstName)
                .withLastName(lastName)
                .withTitle(title)
                .withMiddleName(middleName)
                .withUserId(userId).build();
        return defenceCounsel;
    }

    private Hearing getHearing(final UUID prosecutionCaseId, final UUID courtApplicationId, final UUID hearingId,
                               final UUID offenceId, final UUID seedingHearingId) {
        final Hearing hearing = getHearingBuilder(Hearing.hearing()
                .withId(hearingId), getProsecutionCaseCases(prosecutionCaseId, offenceId))
                .withCourtApplications(getCourtApplications(courtApplicationId))
                .withSeedingHearing(seedingHearing().withSeedingHearingId(seedingHearingId).build())
                .build();
        return hearing;
    }

    private ListNextHearingsV3 buildListNextHearings(final UUID hearingId, final UUID seedingHearingId){
        return ListNextHearingsV3.listNextHearingsV3()
                .withHearings(Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                        .withId(hearingId)
                        .build()))
                .withSeedingHearing(seedingHearing().withSeedingHearingId(seedingHearingId).build())
                .build();
    }
}
