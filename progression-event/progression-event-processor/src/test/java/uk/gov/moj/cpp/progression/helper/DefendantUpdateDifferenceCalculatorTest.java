package uk.gov.moj.cpp.progression.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Person;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

class DefendantUpdateDifferenceCalculatorTest {

    @Test
    public void shouldUpdateDefendantWhenOnlyParentGuardian() {
        final UUID originalDefendantId = UUID.randomUUID();
        final UUID originalProsecutionCaseId = UUID.randomUUID();
        final AssociatedPerson associatedPerson1 = AssociatedPerson
                .associatedPerson()
                .withRole("PARENT")
                .withPerson(Person.person().build())
                .build();
        final AssociatedPerson associatedPerson2 = AssociatedPerson
                .associatedPerson()
                .withPerson(Person.person().build())
                .withRole("PARENT")
                .build();
        final String firstName = RandomStringUtils.random(3);
        final AssociatedPerson associatedPerson3 = AssociatedPerson
                .associatedPerson()
                .withPerson(Person.person().withFirstName(firstName).build())
                .withRole("ParentGuardian")
                .build();
        final DefendantUpdate originalDefendantPreviousVersion = DefendantUpdate.defendantUpdate()
                .withId(originalDefendantId)
                .withProsecutionCaseId(originalProsecutionCaseId)
                .withAssociatedPersons(List.of(associatedPerson1))
                .build();
        final DefendantUpdate originalDefendantNextVersion = DefendantUpdate.defendantUpdate()
                .withId(originalDefendantId)
                .withProsecutionCaseId(originalProsecutionCaseId)
                .withAssociatedPersons(List.of(associatedPerson2))
                .build();
        final DefendantUpdate matchedDefendantPreviousVersion = DefendantUpdate.defendantUpdate()
                .withId(originalDefendantId)
                .withProsecutionCaseId(originalProsecutionCaseId)
                .withAssociatedPersons(List.of(associatedPerson3))
                .build();
        DefendantUpdateDifferenceCalculator defendantUpdateDifferenceCalculator = new DefendantUpdateDifferenceCalculator(originalDefendantPreviousVersion, originalDefendantNextVersion, matchedDefendantPreviousVersion);

        final List<AssociatedPerson> associatedPeople = defendantUpdateDifferenceCalculator.calculateAssociatedPersons();
        assertThat(associatedPeople.get(0).getPerson().getFirstName(), is(firstName));
    }

}