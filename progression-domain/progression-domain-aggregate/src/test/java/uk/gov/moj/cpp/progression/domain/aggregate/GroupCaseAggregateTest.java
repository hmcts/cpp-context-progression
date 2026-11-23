package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.CourtProceedingsInitiated;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import uk.gov.moj.cpp.progression.events.CaseRemovalFromGroupCasesRejected;
import uk.gov.moj.cpp.progression.events.CaseRemovedFromGroupCases;
import uk.gov.moj.cpp.progression.events.GroupCreatedFromMovedCases;
import uk.gov.moj.cpp.progression.events.LastCaseToBeRemovedFromGroupCasesRejected;
import uk.gov.moj.cpp.progression.events.LastCasesToBeMovedFromGroupRejected;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hamcrest.CoreMatchers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

public class GroupCaseAggregateTest {

    private static final UUID GROUP_ID = randomUUID();
    private static final UUID CASE1_ID = randomUUID();
    private static final UUID CASE2_ID = randomUUID();
    private static final UUID CASE3_ID = randomUUID();
    private static final UUID CASE4_ID = randomUUID();

    private GroupCaseAggregate aggregate;

    @BeforeEach
    public void setUp() {
        aggregate = new GroupCaseAggregate();
    }

    @Test
    public void shouldRemoveMemberFromGroup() {
        final ProsecutionCase case1 = getProsecutionCase(GROUP_ID, CASE1_ID, false);
        final ProsecutionCase case2 = getProsecutionCase(GROUP_ID, CASE2_ID, false);
        final ProsecutionCase case3 = getProsecutionCase(GROUP_ID, CASE3_ID, false);
        final ProsecutionCase case4 = getProsecutionCase(GROUP_ID, CASE4_ID, true);

        addCivilCasesToGroup(asList(case1, case2, case3, case4));

        final List<Object> eventStream = aggregate.removeCaseFromGroupCases(GROUP_ID,
                ProsecutionCase.prosecutionCase()
                        .withValuesFrom(case3)
                        .withIsGroupMember(Boolean.FALSE)
                        .withIsGroupMaster(Boolean.FALSE)
                        .build(), null).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object event = eventStream.get(0);
        assertThat(event.getClass(), is(CoreMatchers.equalTo((CaseRemovedFromGroupCases.class))));
        final CaseRemovedFromGroupCases caseRemovedFromGroupCases = (CaseRemovedFromGroupCases) event;
        assertThat(caseRemovedFromGroupCases.getGroupId(), is(GROUP_ID));
        assertThat(caseRemovedFromGroupCases.getRemovedCase().getId(), is(CASE3_ID));
        assertThat(caseRemovedFromGroupCases.getRemovedCase().getIsCivil(), is(Boolean.TRUE));
        assertThat(caseRemovedFromGroupCases.getRemovedCase().getIsGroupMember(), is(Boolean.FALSE));
        assertThat(caseRemovedFromGroupCases.getRemovedCase().getIsGroupMaster(), is(Boolean.FALSE));
        assertThat(caseRemovedFromGroupCases.getNewGroupMaster(), nullValue());
        assertThat(caseRemovedFromGroupCases.getNumberOfGroupCases(), is(3));
    }

    @Test
    public void shouldRemoveGroupMasterFromGroup() {
        final ProsecutionCase case1 = getProsecutionCase(GROUP_ID, CASE1_ID, false);
        final ProsecutionCase case2 = getProsecutionCase(GROUP_ID, CASE2_ID, false);
        final ProsecutionCase case3 = getProsecutionCase(GROUP_ID, CASE3_ID, true);

        addCivilCasesToGroup(asList(case1, case2, case3));

        final List<Object> eventStream = aggregate.removeCaseFromGroupCases(GROUP_ID, ProsecutionCase.prosecutionCase()
                        .withValuesFrom(case3)
                        .withIsGroupMember(Boolean.FALSE)
                        .withIsGroupMaster(Boolean.FALSE)
                        .build(),
                ProsecutionCase.prosecutionCase()
                        .withValuesFrom(case1)
                        .withIsGroupMember(Boolean.TRUE)
                        .withIsGroupMaster(Boolean.TRUE)
                        .build()).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object event = eventStream.get(0);
        assertThat(event.getClass(), is(CoreMatchers.equalTo((CaseRemovedFromGroupCases.class))));
        final CaseRemovedFromGroupCases caseRemovedFromGroupCases = (CaseRemovedFromGroupCases) event;
        assertThat(caseRemovedFromGroupCases.getGroupId(), is(GROUP_ID));
        assertThat(caseRemovedFromGroupCases.getRemovedCase().getId(), is(CASE3_ID));
        assertThat(caseRemovedFromGroupCases.getRemovedCase().getIsCivil(), is(Boolean.TRUE));
        assertThat(caseRemovedFromGroupCases.getRemovedCase().getIsGroupMember(), is(Boolean.FALSE));
        assertThat(caseRemovedFromGroupCases.getRemovedCase().getIsGroupMaster(), is(Boolean.FALSE));
        assertThat(caseRemovedFromGroupCases.getNewGroupMaster().getId(), is(CASE1_ID));
        assertThat(caseRemovedFromGroupCases.getNewGroupMaster().getIsCivil(), is(Boolean.TRUE));
        assertThat(caseRemovedFromGroupCases.getNewGroupMaster().getIsGroupMember(), is(Boolean.TRUE));
        assertThat(caseRemovedFromGroupCases.getNewGroupMaster().getIsGroupMaster(), is(Boolean.TRUE));
        assertThat(caseRemovedFromGroupCases.getNumberOfGroupCases(), is(2));
        assertThat(aggregate.getMemberCases().size(), is(2));
    }

    @Test
    public void shouldUseRequestedNewGroupMasterWhenGroupMasterRemoved() {
        addCivilCasesToGroup(asList(
                getProsecutionCase(GROUP_ID, CASE1_ID, false),
                getProsecutionCase(GROUP_ID, CASE2_ID, false),
                getProsecutionCase(GROUP_ID, CASE3_ID, false),
                getProsecutionCase(GROUP_ID, CASE4_ID, true)));

        // every remaining member can be chosen, whatever order the members are held in
        for (final UUID requested : asList(CASE1_ID, CASE2_ID, CASE3_ID)) {
            assertThat(aggregate.getNewGroupMaster(CASE4_ID, requested), is(requested));
        }
    }

    @Test
    public void shouldPickAnotherMemberWhenGroupMasterRemovedWithoutRequestedNewGroupMaster() {
        addCivilCasesToGroup(asList(
                getProsecutionCase(GROUP_ID, CASE1_ID, false),
                getProsecutionCase(GROUP_ID, CASE2_ID, false),
                getProsecutionCase(GROUP_ID, CASE3_ID, true)));

        assertThat(aggregate.isInvalidNewGroupMaster(CASE3_ID, null), is(false));
        assertThat(asList(CASE1_ID, CASE2_ID).contains(aggregate.getNewGroupMaster(CASE3_ID, null)), is(true));
    }

    @Test
    public void shouldTreatRequestedNewGroupMasterAsInvalidWhenItIsNotARemainingMember() {
        addCivilCasesToGroup(asList(
                getProsecutionCase(GROUP_ID, CASE1_ID, false),
                getProsecutionCase(GROUP_ID, CASE2_ID, false),
                getProsecutionCase(GROUP_ID, CASE3_ID, true)));

        // the removed master itself, and a case that is not in the group
        assertThat(aggregate.isInvalidNewGroupMaster(CASE3_ID, CASE3_ID), is(true));
        assertThat(aggregate.isInvalidNewGroupMaster(CASE3_ID, randomUUID()), is(true));
        // a remaining member is valid
        assertThat(aggregate.isInvalidNewGroupMaster(CASE3_ID, CASE1_ID), is(false));
        // when a member (not the master) is removed the requested master is ignored
        assertThat(aggregate.isInvalidNewGroupMaster(CASE1_ID, randomUUID()), is(false));
    }

    @Test
    public void shouldRaiseCaseRemovalRejectedEventAndKeepGroupUnchanged() {
        addCivilCasesToGroup(asList(
                getProsecutionCase(GROUP_ID, CASE1_ID, false),
                getProsecutionCase(GROUP_ID, CASE2_ID, true)));
        final UUID invalidLead = randomUUID();

        final List<Object> eventStream = aggregate.rejectCaseRemovalFromGroup(GROUP_ID, CASE2_ID, invalidLead, "INVALID_NEW_LEAD_CASE")
                .collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(CaseRemovalFromGroupCasesRejected.class));
        final CaseRemovalFromGroupCasesRejected rejected = (CaseRemovalFromGroupCasesRejected) eventStream.get(0);
        assertThat(rejected.getGroupId(), is(GROUP_ID));
        assertThat(rejected.getCaseId(), is(CASE2_ID));
        assertThat(rejected.getRequestedNewLeadCaseId(), is(invalidLead));
        assertThat(rejected.getReason(), is("INVALID_NEW_LEAD_CASE"));
        assertThat(aggregate.getMemberCases().size(), is(2));
        assertThat(aggregate.getGroupMaster(), is(CASE2_ID));
    }

    @Test
    public void shouldNotChangeGroupMasterWhenMemberRemovedEvenIfNewGroupMasterRequested() {
        addCivilCasesToGroup(asList(
                getProsecutionCase(GROUP_ID, CASE1_ID, false),
                getProsecutionCase(GROUP_ID, CASE2_ID, false),
                getProsecutionCase(GROUP_ID, CASE3_ID, true)));

        assertThat(aggregate.getNewGroupMaster(CASE1_ID, CASE2_ID), nullValue());
        assertThat(aggregate.getNewGroupMaster(CASE1_ID, null), nullValue());
    }

    @Test
    public void shouldSetRemainingNumberOfGroupCasesIncludingMasterWhenMemberRemovedFromGroupOfThree() {
        final ProsecutionCase case1 = getProsecutionCase(GROUP_ID, CASE1_ID, true);
        final ProsecutionCase case2 = getProsecutionCase(GROUP_ID, CASE2_ID, false);
        final ProsecutionCase case3 = getProsecutionCase(GROUP_ID, CASE3_ID, false);

        addCivilCasesToGroup(asList(case1, case2, case3));

        final List<Object> eventStream = aggregate.removeCaseFromGroupCases(GROUP_ID, removed(case2), null).collect(toList());

        assertThat(eventStream.size(), is(1));
        final CaseRemovedFromGroupCases caseRemovedFromGroupCases = (CaseRemovedFromGroupCases) eventStream.get(0);
        assertThat(caseRemovedFromGroupCases.getMasterCaseId(), is(CASE1_ID));
        assertThat(caseRemovedFromGroupCases.getNumberOfGroupCases(), is(2));
        assertThat(aggregate.getMemberCases().size(), is(2));
        assertThat(aggregate.getGroupMaster(), is(CASE1_ID));
    }

    @Test
    public void shouldSetNumberOfGroupCasesToOneWhenMemberRemovedFromGroupOfTwo() {
        final ProsecutionCase case1 = getProsecutionCase(GROUP_ID, CASE1_ID, true);
        final ProsecutionCase case2 = getProsecutionCase(GROUP_ID, CASE2_ID, false);

        addCivilCasesToGroup(asList(case1, case2));

        final List<Object> eventStream = aggregate.removeCaseFromGroupCases(GROUP_ID, removed(case2), null).collect(toList());

        assertThat(eventStream.size(), is(1));
        final CaseRemovedFromGroupCases caseRemovedFromGroupCases = (CaseRemovedFromGroupCases) eventStream.get(0);
        assertThat(caseRemovedFromGroupCases.getNumberOfGroupCases(), is(1));
        assertThat(aggregate.getMemberCases().size(), is(1));
    }

    @Test
    public void shouldNotRaiseCaseRemovedEventWhenLastCaseOfGroupIsRejected() {
        final ProsecutionCase case1 = getProsecutionCase(GROUP_ID, CASE1_ID, true);

        addCivilCasesToGroup(singletonList(case1));

        assertThat(aggregate.canBeRemoved(CASE1_ID), is(false));
        final List<Object> eventStream = aggregate.rejectLastCaseToBeRemovedFromGroup(GROUP_ID, CASE1_ID).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(LastCaseToBeRemovedFromGroupCasesRejected.class));
        assertThat(aggregate.getMemberCases().size(), is(1));
        assertThat(aggregate.getGroupMaster(), is(CASE1_ID));
    }

    @Test
    public void shouldNotRaiseCaseRemovedEventWhenCaseAlreadyRemovedFromGroup() {
        final ProsecutionCase case1 = getProsecutionCase(GROUP_ID, CASE1_ID, true);
        final ProsecutionCase case2 = getProsecutionCase(GROUP_ID, CASE2_ID, false);
        final ProsecutionCase case3 = getProsecutionCase(GROUP_ID, CASE3_ID, false);

        addCivilCasesToGroup(asList(case1, case2, case3));
        aggregate.removeCaseFromGroupCases(GROUP_ID, removed(case2), null).collect(toList());

        final List<Object> eventStream = aggregate.removeCaseFromGroupCases(GROUP_ID, removed(case2), null).collect(toList());

        assertThat(eventStream.size(), is(0));
        assertThat(aggregate.getMemberCases().size(), is(2));
    }

    @Test
    public void shouldAllowMovingOutCasesThatLeaveAtLeastOneMemberBehind() {
        addCivilCasesToGroup(asList(
                getProsecutionCase(GROUP_ID, CASE1_ID, true),
                getProsecutionCase(GROUP_ID, CASE2_ID, false),
                getProsecutionCase(GROUP_ID, CASE3_ID, false)));

        assertThat(aggregate.canMoveCasesOut(Set.of(CASE2_ID, CASE3_ID)), is(true));
    }

    @Test
    public void shouldNotAllowMovingOutEveryMemberOfTheGroup() {
        addCivilCasesToGroup(asList(
                getProsecutionCase(GROUP_ID, CASE1_ID, true),
                getProsecutionCase(GROUP_ID, CASE2_ID, false),
                getProsecutionCase(GROUP_ID, CASE3_ID, false)));

        assertThat(aggregate.canMoveCasesOut(Set.of(CASE1_ID, CASE2_ID, CASE3_ID)), is(false));
    }

    @Test
    public void shouldRejectMoveThatWouldLeaveGroupEmptyAndKeepMembershipUnchanged() {
        addCivilCasesToGroup(asList(
                getProsecutionCase(GROUP_ID, CASE1_ID, true),
                getProsecutionCase(GROUP_ID, CASE2_ID, false)));
        final Set<UUID> allCaseIds = Set.of(CASE1_ID, CASE2_ID);

        assertThat(aggregate.canMoveCasesOut(allCaseIds), is(false));
        final List<Object> eventStream = aggregate.rejectLastCasesToBeMovedFromGroup(GROUP_ID, allCaseIds).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(LastCasesToBeMovedFromGroupRejected.class));
        final LastCasesToBeMovedFromGroupRejected rejected = (LastCasesToBeMovedFromGroupRejected) eventStream.get(0);
        assertThat(rejected.getGroupId(), is(GROUP_ID));
        assertThat(new HashSet<>(rejected.getCaseIds()), is(allCaseIds));
        assertThat(aggregate.getMemberCases().size(), is(2));
        assertThat(aggregate.getGroupMaster(), is(CASE1_ID));
    }

    @Test
    public void shouldMoveMultipleCasesOutOfGroupRaisingOneEventPerCase() {
        final ProsecutionCase case1 = getProsecutionCase(GROUP_ID, CASE1_ID, true);
        final ProsecutionCase case2 = getProsecutionCase(GROUP_ID, CASE2_ID, false);
        final ProsecutionCase case3 = getProsecutionCase(GROUP_ID, CASE3_ID, false);
        final ProsecutionCase case4 = getProsecutionCase(GROUP_ID, CASE4_ID, false);

        addCivilCasesToGroup(asList(case1, case2, case3, case4));

        final List<Object> eventStream = aggregate.moveCasesToNewGroup(GROUP_ID, asList(removed(case2), removed(case3)), null).collect(toList());

        assertThat(eventStream.size(), is(2));
        assertThat(aggregate.getMemberCases(), is(Set.of(CASE1_ID, CASE4_ID)));
        assertThat(aggregate.getGroupMaster(), is(CASE1_ID));
    }

    @Test
    public void shouldReassignGroupMasterOnlyOnTheEventForTheMovedMaster() {
        final ProsecutionCase case1 = getProsecutionCase(GROUP_ID, CASE1_ID, true);
        final ProsecutionCase case2 = getProsecutionCase(GROUP_ID, CASE2_ID, false);
        final ProsecutionCase case3 = getProsecutionCase(GROUP_ID, CASE3_ID, false);

        addCivilCasesToGroup(asList(case1, case2, case3));

        final ProsecutionCase newMaster = ProsecutionCase.prosecutionCase()
                .withValuesFrom(case3)
                .withIsGroupMember(Boolean.TRUE)
                .withIsGroupMaster(Boolean.TRUE)
                .build();

        final List<Object> eventStream = aggregate.moveCasesToNewGroup(GROUP_ID, asList(removed(case1), removed(case2)), newMaster).collect(toList());

        assertThat(eventStream.size(), is(2));
        final CaseRemovedFromGroupCases masterRemovalEvent = (CaseRemovedFromGroupCases) eventStream.stream()
                .filter(e -> ((CaseRemovedFromGroupCases) e).getRemovedCase().getId().equals(CASE1_ID))
                .findFirst().orElseThrow();
        assertThat(masterRemovalEvent.getNewGroupMaster().getId(), is(CASE3_ID));

        final CaseRemovedFromGroupCases memberRemovalEvent = (CaseRemovedFromGroupCases) eventStream.stream()
                .filter(e -> ((CaseRemovedFromGroupCases) e).getRemovedCase().getId().equals(CASE2_ID))
                .findFirst().orElseThrow();
        assertThat(memberRemovalEvent.getNewGroupMaster(), nullValue());

        assertThat(aggregate.getMemberCases(), is(Set.of(CASE3_ID)));
        assertThat(aggregate.getGroupMaster(), is(CASE3_ID));
    }

    @Test
    public void shouldSkipCasesAlreadyMovedByAPriorAttemptWithoutRaisingEventsForThem() {
        final ProsecutionCase case1 = getProsecutionCase(GROUP_ID, CASE1_ID, true);
        final ProsecutionCase case2 = getProsecutionCase(GROUP_ID, CASE2_ID, false);
        final ProsecutionCase case3 = getProsecutionCase(GROUP_ID, CASE3_ID, false);

        addCivilCasesToGroup(asList(case1, case2, case3));
        aggregate.moveCasesToNewGroup(GROUP_ID, singletonList(removed(case2)), null).collect(toList());

        // retried request includes the already-moved case2 again, alongside case3 which hasn't moved yet
        final List<Object> eventStream = aggregate.moveCasesToNewGroup(GROUP_ID, asList(removed(case2), removed(case3)), null).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(((CaseRemovedFromGroupCases) eventStream.get(0)).getRemovedCase().getId(), is(CASE3_ID));
        assertThat(aggregate.getMemberCases(), is(Set.of(CASE1_ID)));
    }

    @Test
    public void shouldCreateGroupFromMovedCasesSeedingMembershipAndMasterCase() {
        final UUID newGroupId = randomUUID();
        final ProsecutionCase leadCase = ProsecutionCase.prosecutionCase()
                .withId(CASE1_ID)
                .withGroupId(newGroupId)
                .withIsCivil(true)
                .withIsGroupMember(true)
                .withIsGroupMaster(true)
                .build();

        final List<Object> eventStream = aggregate.createGroupFromMovedCases(newGroupId, asList(CASE1_ID, CASE2_ID), leadCase).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(GroupCreatedFromMovedCases.class));
        final GroupCreatedFromMovedCases event = (GroupCreatedFromMovedCases) eventStream.get(0);
        assertThat(event.getGroupId(), is(newGroupId));
        assertThat(new HashSet<>(event.getCaseIds()), is(Set.of(CASE1_ID, CASE2_ID)));
        assertThat(event.getLeadCase().getId(), is(CASE1_ID));

        assertThat(aggregate.getMemberCases(), is(Set.of(CASE1_ID, CASE2_ID)));
        assertThat(aggregate.getGroupMaster(), is(CASE1_ID));
        assertThat(aggregate.getMasterCase().getId(), is(CASE1_ID));
    }

    private ProsecutionCase removed(final ProsecutionCase prosecutionCase) {
        return ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withIsGroupMember(Boolean.FALSE)
                .withIsGroupMaster(Boolean.FALSE)
                .build();
    }

    private void addCivilCasesToGroup(final List<ProsecutionCase> prosecutionCases) {
        final CourtReferral courtReferral = CourtReferral.courtReferral()
                .withProsecutionCases(prosecutionCases)
                .build();

        final List<Object> eventStream = aggregate.initiateCourtProceedings(courtReferral).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object event = eventStream.get(0);
        assertThat(event.getClass(), is(CoreMatchers.equalTo((CourtProceedingsInitiated.class))));
        final CourtProceedingsInitiated courtProceedingsInitiated = (CourtProceedingsInitiated) event;
        assertThat(courtProceedingsInitiated.getCourtReferral().getProsecutionCases().size(), is(prosecutionCases.size()));
    }

    private ProsecutionCase getProsecutionCase(final UUID groupId, final UUID caseId, final Boolean isGroupMaster) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withGroupId(groupId)
                .withIsCivil(true)
                .withIsGroupMember(true)
                .withIsGroupMaster(isGroupMaster)
                .build();
    }
}