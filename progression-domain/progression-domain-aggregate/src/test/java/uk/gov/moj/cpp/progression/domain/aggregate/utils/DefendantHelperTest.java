package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.DocumentationLanguageNeeds;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Title;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class DefendantHelperTest {

    PersonDefendant personDefendant;
    PersonDefendant updatedPersonDefendant;
    Person personDetails;
    Address contactAddress;
    ContactNumber contactDetails;
    Organisation organisation;

    @Before
    public void setUp() throws Exception {
        contactDetails = ContactNumber.contactNumber().withHome("0845123574").withWork("0334578522").withMobile("07896542875").withPrimaryEmail("john.smith@hmcts.net").withSecondaryEmail("john.smith@hmcts.net").withFax("0845123574").build();

        contactAddress = Address.address().withAddress1("22").withAddress2("Acacia Avenue").withAddress3("Acacia Street").withAddress4("Acacia Town").withAddress5("Acacia County").withPostcode("GIR 0AA").build();

        organisation = Organisation.organisation().withName("HMCTS").withIncorporationNumber("INC-45875").withAddress(contactAddress).withContact(contactDetails).build();
        final UUID observedEthnicityId = randomUUID();
        final UUID selfDefEthnicityId = randomUUID();
        personDetails = Person.person().withTitle(Title.MR).withFirstName("John").withMiddleName("S").withLastName("Smith").withDateOfBirth("2000-01-01").withNationalityId(randomUUID()).withAdditionalNationalityId(randomUUID()).withEthnicityId(randomUUID()).withGender(Gender.MALE).withInterpreterLanguageNeeds("John").withDocumentationLanguageNeeds(DocumentationLanguageNeeds.WELSH).withNationalInsuranceNumber("SK384524").withOccupation("Student").withSpecificRequirements("Screen").withAddress(contactAddress).withContact(contactDetails).build();

        personDefendant = PersonDefendant.personDefendant().withBailStatus(BailStatus.IN_CUSTODY).withCustodyTimeLimit("2018-12-01").withObservedEthnicityId(observedEthnicityId).withAliases(Collections.singletonList("ALIAS")).withSelfDefinedEthnicityId(selfDefEthnicityId).withPncId("12345678").withArrestSummonsNumber("arrest123").withPersonDetails(personDetails).withEmployerOrganisation(organisation).build();

       updatedPersonDefendant = PersonDefendant.personDefendant().withBailStatus(BailStatus.IN_CUSTODY).withCustodyTimeLimit("2018-12-01").withObservedEthnicityId(observedEthnicityId).withAliases(Collections.singletonList("UPDATED_ALIAS")).withSelfDefinedEthnicityId(selfDefEthnicityId).withPncId("12345678").withArrestSummonsNumber("arrest123").withPersonDetails(personDetails).withEmployerOrganisation(organisation).build();
    }


    @Test
    public void testOffencesUpdatedWithNewOffence() {
        final List<Offence> commandOffenceList = new ArrayList<>();
        final List<Offence> existingOffenceList = new ArrayList<>();
        final Offence offenceOne = createOffence(randomUUID(), "first");
        final Offence offenceTwo = createOffence(randomUUID(), "second");
        commandOffenceList.add(offenceOne);
        commandOffenceList.add(offenceTwo);

        existingOffenceList.add(offenceOne);

        assertTrue(DefendantHelper.isOffencesUpdated(commandOffenceList, existingOffenceList));

    }

    @Test
    public void testOffencesUpdatedWithDeletedOffence() {
        final List<Offence> commandOffenceList = new ArrayList<>();
        final List<Offence> existingOffenceList = new ArrayList<>();
        final Offence offenceOne = createOffence(randomUUID(), "first");
        final Offence offenceTwo = createOffence(randomUUID(), "second");
        commandOffenceList.add(offenceOne);

        existingOffenceList.add(offenceOne);
        existingOffenceList.add(offenceTwo);

        assertTrue(DefendantHelper.isOffencesUpdated(commandOffenceList, existingOffenceList));

    }

    @Test
    public void testOffencesUpdatedWithModifiedOffence() {
        final List<Offence> commandOffenceList = new ArrayList<>();
        final List<Offence> existingOffenceList = new ArrayList<>();
        final UUID offenceOneId = randomUUID();
        final Offence offenceOne = createOffence(offenceOneId, "first");
        final Offence offenceTwo = createOffence(randomUUID(), "second");
        final Offence updatedOffence = createOffence(offenceOneId, "updated");

        commandOffenceList.add(updatedOffence);
        commandOffenceList.add(offenceTwo);

        existingOffenceList.add(offenceOne);
        existingOffenceList.add(offenceTwo);

        assertTrue(DefendantHelper.isOffencesUpdated(commandOffenceList, existingOffenceList));

    }

    @Test
    public void testUnchangedOffences() {
        final List<Offence> commandOffenceList = new ArrayList<>();
        final List<Offence> existingOffenceList = new ArrayList<>();
        final Offence offenceOne = createOffence(randomUUID(), "first");
        final Offence offenceTwo = createOffence(randomUUID(), "second");
        commandOffenceList.add(offenceOne);

        existingOffenceList.add(offenceOne);

        assertFalse(DefendantHelper.isOffencesUpdated(commandOffenceList, existingOffenceList));

    }

    private static Offence createOffence(final UUID offenceId, final String offenceCode) {
        return Offence.offence().withId(offenceId).withOffenceCode(offenceCode).withStartDate(LocalDate.now().toString()).withArrestDate(LocalDate.now().toString()).withChargeDate(LocalDate.now().toString()).withConvictionDate(LocalDate.now().toString()).withEndDate(LocalDate.now().toString()).withOffenceTitle("title").withOffenceTitleWelsh("welsh title").withWording("wording").withOffenceLegislation("legisltation").withOffenceLegislationWelsh("welsh legisltation").withCount(1).build();
    }
}
