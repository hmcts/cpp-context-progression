package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.HearingDeleted;
import uk.gov.justice.progression.courts.HearingMarkedAsDuplicate;
import uk.gov.justice.progression.courts.OffencesRemovedFromHearing;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingAggregateTest {

    @InjectMocks
    private HearingAggregate hearingAggregate;

    @Test
    public void shouldMarkHearingDuplicate() {
        final UUID hearingId = randomUUID();
        final List<UUID> caseIds = Arrays.asList(randomUUID(), randomUUID());
        final List<UUID> defendantIds = Arrays.asList(randomUUID(), randomUUID());

        final List<Object> eventStream = hearingAggregate.markAsDuplicate(hearingId, caseIds, defendantIds).collect(toList());

        assertThat(eventStream.size(), is(1));
        final HearingMarkedAsDuplicate hearingMarkedAsDuplicate = (HearingMarkedAsDuplicate) eventStream.get(0);
        assertThat(hearingMarkedAsDuplicate.getHearingId(), is(hearingId));
        assertThat(hearingMarkedAsDuplicate.getCaseIds(), is(caseIds));
        assertThat(hearingMarkedAsDuplicate.getDefendantIds(), is(defendantIds));
    }

    @Test
    public void shouldNotRaiseNewEventIfAlreadyMarkedAsDuplicate() {
        setField(hearingAggregate, "duplicate", true);

        final UUID hearingId = randomUUID();
        final List<UUID> caseIds = Arrays.asList(randomUUID(), randomUUID());
        final List<UUID> defendantIds = Arrays.asList(randomUUID(), randomUUID());

        final List<Object> eventStream = hearingAggregate.markAsDuplicate(hearingId, caseIds, defendantIds).collect(toList());

        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void shouldDeleteHearingForProsecutionCaseAndCourtApplication() {

        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                        .build()
                        )
                )
                .withCourtApplications(
                        asList(
                                CourtApplication.courtApplication()
                                        .withId(courtApplicationId)
                                        .build()
                        )
                )
                .build();

        setField(hearingAggregate, "hearing", hearing);
        final List<Object> eventStream = hearingAggregate.deleteHearing(hearingId).collect(toList());

        assertThat(eventStream.size(), is(1));
        final HearingDeleted hearingDeleted = (HearingDeleted) eventStream.get(0);
        assertThat(hearingDeleted.getHearingId(), is(hearingId));
        assertThat(hearingDeleted.getProsecutionCaseIds().get(0), is(prosecutionCaseId));
        assertThat(hearingDeleted.getCourtApplicationIds().get(0), is(courtApplicationId));

    }

    @Test
    public void shouldRemoveProsecutionCaseWhenAllOffencesAreRemoved() {

        final UUID hearingId = UUID.randomUUID();
        final UUID prosecutionCaseId1 = UUID.randomUUID();
        final UUID prosecutionCaseId2 = UUID.randomUUID();
        final UUID prosecutionCaseId3 = UUID.randomUUID();

        final UUID defendantId1 = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final UUID defendantId3 = UUID.randomUUID();

        final UUID offence1 = UUID.randomUUID();
        final UUID offence2 = UUID.randomUUID();
        final UUID offence3 = UUID.randomUUID();
        final UUID offence4 = UUID.randomUUID();

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
}
