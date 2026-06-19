package uk.gov.moj.cpp.progression.transformer;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.progression.transformer.HearingOffenceFilter.OffenceOwnerResolver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class HearingOffenceFilterTest {

    private static final OffenceOwnerResolver NO_OWNER = (caseId, offenceId) -> Optional.empty();

    // ---- Guards -------------------------------------------------------------

    @Test
    void shouldReturnNullWhenHearingIsNull() {
        assertThat(HearingOffenceFilter.filterOffences(null, NO_OWNER), is(nullValue()));
    }

    @Test
    void shouldReturnSameHearingWhenNoCourtApplications() {
        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withProsecutionCases(singletonList(prosecutionCase(randomUUID(), defendant(randomUUID(), activeOffence(randomUUID())))))
                .build();

        assertThat(HearingOffenceFilter.filterOffences(hearing, NO_OWNER), is(sameInstance(hearing)));
    }

    @Test
    void shouldReturnSameHearingWhenApplicationsHaveNoOffences() {
        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                                .withProsecutionCaseId(randomUUID())
                                .build()))
                        .build()))
                .build();

        assertThat(HearingOffenceFilter.filterOffences(hearing, NO_OWNER), is(sameInstance(hearing)));
    }

    // ---- Branch 1: application hearing (all application offences concluded) --

    @Test
    void shouldDropProsecutionCasesWhenAllApplicationOffencesConcluded() {
        final UUID caseId = randomUUID();
        final Offence concluded1 = concludedOffence(randomUUID());
        final Offence concluded2 = concludedOffence(randomUUID());
        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withCourtApplications(singletonList(application(caseId, concluded1, concluded2)))
                .withProsecutionCases(singletonList(prosecutionCase(caseId, defendant(randomUUID(), activeOffence(randomUUID())))))
                .build();

        final Hearing result = HearingOffenceFilter.filterOffences(hearing, NO_OWNER);

        assertThat(result.getProsecutionCases(), is(nullValue()));
        // application offences are left untouched
        assertThat(offenceIds(result.getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences()),
                containsInAnyOrder(concluded1.getId(), concluded2.getId()));
    }

    // ---- Branch 2: active application offences move to prosecution ----------

    @Test
    void shouldKeepActiveOffenceUnderProsecutionAndRemoveItFromApplication() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID sharedOffenceId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withCourtApplications(singletonList(application(caseId, activeOffence(sharedOffenceId))))
                .withProsecutionCases(singletonList(prosecutionCase(caseId, defendant(defendantId, activeOffence(sharedOffenceId)))))
                .build();

        final Hearing result = HearingOffenceFilter.filterOffences(hearing, NO_OWNER);

        // offence stays once under prosecution
        assertThat(offenceIds(result.getProsecutionCases().get(0).getDefendants().get(0).getOffences()), contains(sharedOffenceId));
        // and is removed from the application case (only offence -> list nulled)
        assertThat(result.getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences(), is(nullValue()));
    }

    @Test
    void shouldKeepConcludedApplicationOffenceAndMoveOnlyTheActiveOne() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID activeId = randomUUID();
        final UUID concludedId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withCourtApplications(singletonList(application(caseId, activeOffence(activeId), concludedOffence(concludedId))))
                .withProsecutionCases(singletonList(prosecutionCase(caseId, defendant(defendantId, activeOffence(activeId)))))
                .build();

        final Hearing result = HearingOffenceFilter.filterOffences(hearing, NO_OWNER);

        // active offence kept under prosecution
        assertThat(offenceIds(result.getProsecutionCases().get(0).getDefendants().get(0).getOffences()), contains(activeId));
        // concluded offence remains on the application side; active one removed
        assertThat(offenceIds(result.getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences()), contains(concludedId));
    }

    @Test
    void shouldRemoveProsecutionOffencesNotReferencedByApplication() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID referencedId = randomUUID();
        final UUID unreferencedId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withCourtApplications(singletonList(application(caseId, activeOffence(referencedId))))
                .withProsecutionCases(singletonList(prosecutionCase(caseId,
                        defendant(defendantId, activeOffence(referencedId), activeOffence(unreferencedId)))))
                .build();

        final Hearing result = HearingOffenceFilter.filterOffences(hearing, NO_OWNER);

        assertThat(offenceIds(result.getProsecutionCases().get(0).getDefendants().get(0).getOffences()), contains(referencedId));
    }

    @Test
    void shouldDropDefendantsAndCasesLeftEmptyAfterCleanup() {
        final UUID caseId = randomUUID();
        final UUID keptDefendantId = randomUUID();
        final UUID emptiedDefendantId = randomUUID();
        final UUID referencedId = randomUUID();
        final UUID unreferencedId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withCourtApplications(singletonList(application(caseId, activeOffence(referencedId))))
                .withProsecutionCases(singletonList(prosecutionCase(caseId,
                        defendant(keptDefendantId, activeOffence(referencedId)),
                        defendant(emptiedDefendantId, activeOffence(unreferencedId)))))
                .build();

        final Hearing result = HearingOffenceFilter.filterOffences(hearing, NO_OWNER);

        assertThat(result.getProsecutionCases().get(0).getDefendants(), hasSize(1));
        assertThat(result.getProsecutionCases().get(0).getDefendants().get(0).getId(), is(keptDefendantId));
    }

    @Test
    void shouldNullProsecutionCasesWhenCleanupRemovesEverything() {
        // active application offence references nothing present in prosecution, and the resolver cannot place it
        final UUID caseId = randomUUID();
        final UUID appOnlyActiveId = randomUUID();
        final UUID prosecutionOnlyId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withCourtApplications(singletonList(application(caseId, activeOffence(appOnlyActiveId))))
                .withProsecutionCases(singletonList(prosecutionCase(caseId, defendant(randomUUID(), activeOffence(prosecutionOnlyId)))))
                .build();

        final Hearing result = HearingOffenceFilter.filterOffences(hearing, NO_OWNER);

        assertThat(result.getProsecutionCases(), is(nullValue()));
        // unresolved active offence is left on the application side (warn + leave)
        assertThat(offenceIds(result.getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences()), contains(appOnlyActiveId));
    }

    // ---- Move-add fallback via resolver -------------------------------------

    @Test
    void shouldMoveActiveOffenceToResolvedDefendantWhenAbsentFromProsecution() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID presentActiveId = randomUUID();
        final UUID absentActiveId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withCourtApplications(singletonList(application(caseId, activeOffence(presentActiveId), activeOffence(absentActiveId))))
                .withProsecutionCases(singletonList(prosecutionCase(caseId, defendant(defendantId, activeOffence(presentActiveId)))))
                .build();

        final OffenceOwnerResolver resolver = (resolveCaseId, offenceId) ->
                absentActiveId.equals(offenceId) ? Optional.of(defendantId) : Optional.empty();

        final Hearing result = HearingOffenceFilter.filterOffences(hearing, resolver);

        assertThat(offenceIds(result.getProsecutionCases().get(0).getDefendants().get(0).getOffences()),
                containsInAnyOrder(presentActiveId, absentActiveId));
        // both active offences moved off the application
        assertThat(result.getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences(), is(nullValue()));
    }

    @Test
    void shouldLeaveActiveOffenceOnApplicationWhenResolvedDefendantNotInHearing() {
        final UUID caseId = randomUUID();
        final UUID presentActiveId = randomUUID();
        final UUID absentActiveId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withCourtApplications(singletonList(application(caseId, activeOffence(presentActiveId), activeOffence(absentActiveId))))
                .withProsecutionCases(singletonList(prosecutionCase(caseId, defendant(randomUUID(), activeOffence(presentActiveId)))))
                .build();

        // resolver points at a defendant that is not in the hearing's prosecution case
        final OffenceOwnerResolver resolver = (resolveCaseId, offenceId) -> Optional.of(randomUUID());

        final Hearing result = HearingOffenceFilter.filterOffences(hearing, resolver);

        assertThat(offenceIds(result.getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences()), contains(absentActiveId));
        assertThat(offenceIds(result.getProsecutionCases().get(0).getDefendants().get(0).getOffences()), contains(presentActiveId));
    }

    // ---- Cross-cutting ------------------------------------------------------

    @Test
    void shouldLeaveCourtOrderOffencesUntouched() {
        final UUID caseId = randomUUID();
        final UUID sharedOffenceId = randomUUID();
        final UUID courtOrderOffenceId = randomUUID();

        final CourtApplication application = CourtApplication.courtApplication()
                .withId(randomUUID())
                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                        .withProsecutionCaseId(caseId)
                        .withOffences(singletonList(activeOffence(sharedOffenceId)))
                        .build()))
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                                .withOffence(activeOffence(courtOrderOffenceId))
                                .build()))
                        .build())
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withCourtApplications(singletonList(application))
                .withProsecutionCases(singletonList(prosecutionCase(caseId, defendant(randomUUID(), activeOffence(sharedOffenceId)))))
                .build();

        final Hearing result = HearingOffenceFilter.filterOffences(hearing, NO_OWNER);

        final CourtOrder resultCourtOrder = result.getCourtApplications().get(0).getCourtOrder();
        assertThat(resultCourtOrder.getCourtOrderOffences().get(0).getOffence().getId(), is(courtOrderOffenceId));
    }

    @Test
    void shouldNotMutateInputHearing() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID sharedOffenceId = randomUUID();
        final UUID unreferencedId = randomUUID();

        final CourtApplicationCase applicationCase = CourtApplicationCase.courtApplicationCase()
                .withProsecutionCaseId(caseId)
                .withOffences(singletonList(activeOffence(sharedOffenceId)))
                .build();
        final Defendant inputDefendant = defendant(defendantId, activeOffence(sharedOffenceId), activeOffence(unreferencedId));
        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withCourtApplications(singletonList(CourtApplication.courtApplication().withId(randomUUID())
                        .withCourtApplicationCases(singletonList(applicationCase)).build()))
                .withProsecutionCases(singletonList(prosecutionCase(caseId, inputDefendant)))
                .build();

        HearingOffenceFilter.filterOffences(hearing, NO_OWNER);

        // input application case still holds its offence; input defendant still holds both offences
        assertThat(applicationCase.getOffences(), hasSize(1));
        assertThat(inputDefendant.getOffences(), hasSize(2));
    }

    // ---- Fixture helpers ----------------------------------------------------

    private static Offence activeOffence(final UUID id) {
        return Offence.offence().withId(id).withProceedingsConcluded(false).build();
    }

    private static Offence concludedOffence(final UUID id) {
        return Offence.offence().withId(id).withProceedingsConcluded(true).build();
    }

    private static CourtApplication application(final UUID prosecutionCaseId, final Offence... offences) {
        return CourtApplication.courtApplication()
                .withId(randomUUID())
                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withOffences(asList(offences))
                        .build()))
                .build();
    }

    private static Defendant defendant(final UUID id, final Offence... offences) {
        return Defendant.defendant().withId(id).withOffences(asList(offences)).build();
    }

    private static ProsecutionCase prosecutionCase(final UUID id, final Defendant... defendants) {
        return ProsecutionCase.prosecutionCase().withId(id).withDefendants(asList(defendants)).build();
    }

    private static List<UUID> offenceIds(final List<Offence> offences) {
        return offences.stream().map(Offence::getId).collect(Collectors.toList());
    }
}
