package uk.gov.moj.cpp.progression.service.utils;

import static org.junit.Assert.assertEquals;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class DefendantDetailsExtractorTest {

    @Test
    public void shouldGetDefendantDateOfBirthReturnEmptyOptionalWhenThereIsNoPersonDefendant() {
        Defendant defendant = Defendant.defendant().build();

        Optional<LocalDate> result = DefendantDetailsExtractor.getDefendantDateOfBirth(defendant);

        assertEquals(Optional.empty(), result);
    }

    @Test
    public void shouldGetDefendantDateOfBirthReturnEmptyOptionalWhenThereIsNoPersonDetails() {
        Defendant defendant = Defendant.defendant().withPersonDefendant(null).build();

        Optional<LocalDate> result = DefendantDetailsExtractor.getDefendantDateOfBirth(defendant);

        assertEquals(Optional.empty(), result);
    }

    @Test
    public void shouldGetDefendantDateOfBirthReturnEmptyOptionalWhenThereIsNoDateOfBirth() {
        Defendant defendant = Defendant.defendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(Person.person().build()).build()).build();

        Optional<LocalDate> result = DefendantDetailsExtractor.getDefendantDateOfBirth(defendant);

        assertEquals(Optional.empty(), result);
    }

    @Test
    public void shouldGetDefendantDateOfBirthReturnDateOfBirth() {
        LocalDate dateOfBirth = LocalDate.now();
        Defendant defendant = Defendant.defendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(Person.person().withDateOfBirth(dateOfBirth).build()).build()).build();

        Optional<LocalDate> result = DefendantDetailsExtractor.getDefendantDateOfBirth(defendant);

        assertEquals(Optional.of(dateOfBirth), result);
    }
}
