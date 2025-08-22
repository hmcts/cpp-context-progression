package uk.gov.moj.cpp.progression.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Person;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

class DefendantUpdateDifferenceCalculatorTest {

    @Test
    public void shouldUpdateDefendantWithParentGuardian() {
        final UUID originalDefendantId = UUID.randomUUID();
        final UUID originalProsecutionCaseId = UUID.randomUUID();
        final AssociatedPerson associatedPerson1 = AssociatedPerson
                .associatedPerson()
                .withRole("PARENT")
                .withPerson(Person.person()
                        .withFirstName("Parent1_FN")
                        .withLastName("Parent1_LN")
                        .withGender(Gender.MALE)
                        .withAddress(Address.address().build())
                        .build())
                .build();
        final AssociatedPerson associatedPerson2 = AssociatedPerson
                .associatedPerson()
                .withPerson(Person.person()
                        .withFirstName("Parent2_FN")
                        .withLastName("Parent2_LN")
                        .withGender(Gender.MALE)
                        .withAddress(Address.address().build())
                        .build())
                .withRole("PARENT")
                .build();
        final String firstName = RandomStringUtils.random(3);
        final AssociatedPerson associatedPerson3 = AssociatedPerson
                .associatedPerson()
                .withPerson(Person.person()
                        .withFirstName(firstName)
                        .withLastName(firstName)
                        .withGender(Gender.MALE)
                        .build())
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

        final List<AssociatedPerson> associatedPersons = defendantUpdateDifferenceCalculator.calculateAssociatedPersons();
        //Keeps 2 entries, one from matched defendant and other is next version
        assertThat(associatedPersons.size(), is(2));
        assertThat(associatedPersons.get(0).getPerson().getFirstName(), is(firstName));
    }

    @Test
    public void shouldUpdateDefendantWhenMatchedDefendantHasNoAssociatedPerson() {
        final UUID originalDefendantId = UUID.randomUUID();
        final UUID originalProsecutionCaseId = UUID.randomUUID();
        final UUID matchedDefendantId = UUID.randomUUID();
        final UUID matchedProsecutionCaseId = UUID.randomUUID();
        final AssociatedPerson associatedPerson1 = AssociatedPerson
                .associatedPerson()
                .withRole("PARENT")
                .withPerson(Person.person()
                        .withFirstName("Parent1_FN")
                        .withLastName("Parent1_LN")
                        .withGender(Gender.MALE)
                        .withAddress(Address.address().build())
                        .build())
                .build();
        final AssociatedPerson associatedPerson2 = AssociatedPerson
                .associatedPerson()
                .withPerson(Person.person()
                        .withFirstName("Parent2_FN")
                        .withLastName("Parent2_LN")
                        .withGender(Gender.FEMALE)
                        .withAddress(Address.address().build())
                        .build())
                .withRole("PARENT")
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
                .withId(matchedDefendantId)
                .withProsecutionCaseId(matchedProsecutionCaseId)
                .build();
        DefendantUpdateDifferenceCalculator defendantUpdateDifferenceCalculator = new DefendantUpdateDifferenceCalculator(originalDefendantPreviousVersion, originalDefendantNextVersion, matchedDefendantPreviousVersion);

        final List<AssociatedPerson> associatedPersons = defendantUpdateDifferenceCalculator.calculateAssociatedPersons();
        //Keeps 1 entries, only from next version
        assertThat(associatedPersons.size(), is(1));
    }

    @Test
    public void shouldUpdateDefendantWhenDefendantNextVersionHasNoAssociatedPerson() {
        final UUID originalDefendantId = UUID.randomUUID();
        final UUID originalProsecutionCaseId = UUID.randomUUID();
        final UUID matchedDefendantId = UUID.randomUUID();
        final UUID matchedProsecutionCaseId = UUID.randomUUID();
        final AssociatedPerson associatedPerson1 = AssociatedPerson
                .associatedPerson()
                .withRole("PARENT")
                .withPerson(Person.person()
                        .withFirstName("Parent1_FN")
                        .withLastName("Parent1_LN")
                        .withGender(Gender.MALE)
                        .withAddress(Address.address().build())
                        .build())
                .build();
        final DefendantUpdate originalDefendantPreviousVersion = DefendantUpdate.defendantUpdate()
                .withId(originalDefendantId)
                .withProsecutionCaseId(originalProsecutionCaseId)
                .withAssociatedPersons(List.of(associatedPerson1))
                .build();
        final DefendantUpdate originalDefendantNextVersion = DefendantUpdate.defendantUpdate()
                .withId(originalDefendantId)
                .withProsecutionCaseId(originalProsecutionCaseId)
                .build();
        final DefendantUpdate matchedDefendantPreviousVersion = DefendantUpdate.defendantUpdate()
                .withId(matchedDefendantId)
                .withProsecutionCaseId(matchedProsecutionCaseId)
                .build();
        DefendantUpdateDifferenceCalculator defendantUpdateDifferenceCalculator = new DefendantUpdateDifferenceCalculator(originalDefendantPreviousVersion, originalDefendantNextVersion, matchedDefendantPreviousVersion);

        final List<AssociatedPerson> associatedPersons = defendantUpdateDifferenceCalculator.calculateAssociatedPersons();
        //Keeps 1 entries, only from next version
        assertThat(associatedPersons.size(), is(0));
    }

}