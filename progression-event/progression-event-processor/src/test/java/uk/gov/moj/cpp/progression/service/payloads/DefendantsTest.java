package uk.gov.moj.cpp.progression.service.payloads;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantsTest {

    public static final String ORGANISATION_NAME_1 = "OrganisationName1";
    public static final String FIRST_NAME = "ExampleFirstName";
    public static final String LAST_NAME = "ExampleLastName";

    @Test
    public void shouldGetDefendantFullName() {
        Defendants defendants = Defendants.defendants().withOrganisationName("OrgnameOnly").build();
        assertThat(defendants.getDefendantFullName(), isEmptyString());

    }

    @Test
    public void shouldReturnNullIfFirstNameAndLastNameIsnull() {
        Defendants defendants = Defendants.defendants().withdefendantFirstName(null).withdefendantLastName(null).withOrganisationName(ORGANISATION_NAME_1).build();
        assertThat(defendants.getDefendantFullName(), isEmptyString());
    }

    @Test
    public void shouldReturnFirstNameandLastNameIfNotNull() {
        Defendants defendants = Defendants.defendants().withdefendantFirstName(FIRST_NAME).withdefendantLastName(LAST_NAME).build();
        assertThat(defendants.getDefendantFullName(), is(FIRST_NAME.concat(" ").concat(LAST_NAME)));
    }
}