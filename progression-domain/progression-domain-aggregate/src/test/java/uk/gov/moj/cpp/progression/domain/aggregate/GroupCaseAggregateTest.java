package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.CourtProceedingsInitiated;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import uk.gov.moj.cpp.progression.events.CaseRemovedFromGroupCases;

import java.util.List;
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
        assertThat(aggregate.getMemberCases().size(), is(2));
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