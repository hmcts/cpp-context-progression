package uk.gov.moj.cpp.progression.query.view;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static uk.gov.justice.core.courts.AssociatedPerson.associatedPerson;
import static uk.gov.justice.core.courts.BailStatus.bailStatus;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.justice.core.courts.CourtApplicationPayment.courtApplicationPayment;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.courts.progression.query.ApplicationDetails;
import uk.gov.justice.courts.progression.query.ThirdParties;
import uk.gov.justice.courts.progression.query.ThirdPartyRepresentatives;
import uk.gov.justice.progression.courts.ApplicantDetails;
import uk.gov.justice.progression.courts.RespondentDetails;
import uk.gov.justice.progression.courts.RespondentRepresentatives;
import uk.gov.justice.services.test.utils.core.random.BooleanGenerator;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApplicationAtAGlanceHelperTest {

    private static final StringGenerator STRING_GENERATOR = new StringGenerator();
    private static final BooleanGenerator BOOLEAN_GENERATOR = new BooleanGenerator();
    private ApplicationAtAGlanceHelper applicationAtAGlanceHelper;

    @BeforeEach
    public void setUp() {
        applicationAtAGlanceHelper = new ApplicationAtAGlanceHelper();
    }

    @Test
    public void shouldGetApplicationDetails() {

        final CourtApplication courtApplication = courtApplication()
                .withApplicationReference(STRING_GENERATOR.next())
                .withType(courtApplicationType().withType(STRING_GENERATOR.next()).withAppealFlag(BOOLEAN_GENERATOR.next()).withApplicantAppellantFlag(BOOLEAN_GENERATOR.next()).build())
                .withApplicationReceivedDate(now())
                .withApplicationParticulars(STRING_GENERATOR.next())
                .withCourtApplicationPayment(courtApplicationPayment().withPaymentReference(STRING_GENERATOR.next()).build())
                .withJudicialResults(Collections.singletonList(JudicialResult.judicialResult()
                        .withResultText("REVU - Further review of court order")
                        .build()))
                .build();

        final ApplicationDetails applicationDetails = applicationAtAGlanceHelper.getApplicationDetails(courtApplication);

        assertThat(applicationDetails.getApplicationReference(), is(courtApplication.getApplicationReference()));
        assertThat(applicationDetails.getApplicationType(), is(courtApplication.getType().getType()));
        assertThat(applicationDetails.getAppeal(), is(courtApplication.getType().getAppealFlag()));
        assertThat(applicationDetails.getApplicantAppellantFlag(), is(courtApplication.getType().getApplicantAppellantFlag()));
        assertThat(applicationDetails.getApplicationReceivedDate(), is(courtApplication.getApplicationReceivedDate()));
        assertThat(applicationDetails.getApplicationParticulars(), is(courtApplication.getApplicationParticulars()));
        assertThat(applicationDetails.getPaymentReference(), is(courtApplication.getCourtApplicationPayment().getPaymentReference()));
        assertThat(applicationDetails.getAagResults().get(0).getResultText(), is("REVU - Further review of court order"));
        assertThat(applicationDetails.getAagResults().get(0).getUseResultText(), is(true));
    }

    @Test
    public void shouldGetApplicantDetailsWhenApplicantIsAnIndividual() {

        final Address address = mock(Address.class);

        final Person person = person()
                .withFirstName(STRING_GENERATOR.next())
                .withLastName(STRING_GENERATOR.next())
                .withAddress(address)
                .withDateOfBirth(now().minusYears(18L))
                .withNationalityDescription(STRING_GENERATOR.next())
                .build();

        final Organisation representationOrganisation = organisation()
                .withName(STRING_GENERATOR.next())
                .build();

        final CourtApplicationParty applicant = courtApplicationParty()
                .withPersonDetails(person)
                .withRepresentationOrganisation(representationOrganisation)
                .build();

        final CourtApplication courtApplication = courtApplication()
                .withApplicant(applicant)
                .withType(courtApplicationType().build())
                .build();

        final ApplicantDetails applicantDetails = applicationAtAGlanceHelper.getApplicantDetails(courtApplication);
        assertThat(applicantDetails.getName(), is(format("%s %s", person.getFirstName(), person.getLastName())));
        assertThat(applicantDetails.getAddress(), is(address));
        assertThat(applicantDetails.getInterpreterLanguageNeeds(), is(person.getInterpreterLanguageNeeds()));
        assertThat(applicantDetails.getRepresentation(), is(representationOrganisation.getName()));
    }

    @Test
    public void shouldGetApplicantDetailsWhenApplicantIsAnOrganisation() {

        final Address address = mock(Address.class);
        final Organisation applicantOrgan = organisation()
                .withName(STRING_GENERATOR.next())
                .withAddress(Address.address()
                        .withAddress1("Address 1")
                        .withAddress2("Address 2")
                        .withPostcode("DD4 4DD")
                        .build())
                .build();
        final Organisation representationOrganisation = organisation()
                .withName(STRING_GENERATOR.next())
                .withAddress(Address.address()
                        .withAddress1("Address 1")
                        .withAddress2("Address 2")
                        .withPostcode("DD4 4DD")
                        .build())
                .build();
        final CourtApplicationParty applicant = courtApplicationParty()
                .withRepresentationOrganisation(representationOrganisation)
                .withOrganisation(applicantOrgan)
                .withPersonDetails(null)
                .withOrganisationPersons(Arrays.asList(AssociatedPerson.associatedPerson()
                        .withPerson(Person.person()
                                .withFirstName("Rob")
                                .withMiddleName("Martin")
                                .withLastName("Paine")
                                .withAddress(Address.address()
                                        .withAddress1("36 Brown House")
                                        .withAddress2("450 London Street")
                                        .withAddress3("Ocean way")
                                        .withAddress4("Bristol")
                                        .withAddress5("Somerset")
                                        .withPostcode("BS1 4TE")
                                        .build())
                                .build())
                        .build()
                        ,
                        AssociatedPerson.associatedPerson().withPerson(Person.person()
                                .withFirstName("Ali")
                                .withMiddleName("Veli")
                                .withLastName("Lawson")
                                .withAddress(Address.address()
                                        .withAddress1("40 Brown House")
                                        .withAddress2("450 London Street")
                                        .withAddress3("Ocean way")
                                        .withAddress4("Bristol")
                                        .withAddress5("Somerset")
                                        .withPostcode("BS1 4TE")
                                        .build())
                                .build())
                                .build()
                        ,
                        AssociatedPerson.associatedPerson().withPerson(Person.person()
                                .withFirstName(null)
                                .withLastName("Dur")
                                .withAddress(Address.address()
                                        .withAddress1("40 Brown House")
                                        .withAddress2("450 London Street")
                                        .withAddress3("Ocean way")
                                        .withAddress4("Bristol")
                                        .withAddress5("Somerset")
                                        .withPostcode("BS3 4TH")
                                        .build())
                                .build())
                                .build()

                ))
                .build();

        final CourtApplication courtApplication = courtApplication()
                .withApplicant(applicant)
                .withType(courtApplicationType().build())

                .build();

        final ApplicantDetails applicantDetails = applicationAtAGlanceHelper.getApplicantDetails(courtApplication);
        assertThat(applicantDetails.getName(), is(applicantOrgan.getName()));
        assertThat(applicantDetails.getAddress(), is(applicantOrgan.getAddress()));
        assertThat(courtApplication.getApplicant().getOrganisationPersons(), notNullValue());
        assertThat(applicantDetails.getRepresentation(),is("Rob Paine, Ali Lawson"));

    }

    @Test
    public void shouldGetApplicantDetailsWhenApplicantIsADefendant() {

        final Address address = mock(Address.class);

        final Person person = person()
                .withFirstName(STRING_GENERATOR.next())
                .withLastName(STRING_GENERATOR.next())
                .withAddress(address)
                .withDateOfBirth(now().minusYears(18L))
                .withNationalityDescription(STRING_GENERATOR.next())
                .build();

        final PersonDefendant personDefendant = personDefendant()
                .withPersonDetails(person)
                .withBailStatus(bailStatus().withDescription(STRING_GENERATOR.next()).build())
                .build();

        final MasterDefendant defendant = MasterDefendant.masterDefendant()
                .withPersonDefendant(personDefendant).build();

        final Organisation representationOrganisation = organisation()
                .withName(STRING_GENERATOR.next())
                .build();

        final CourtApplicationParty applicant = courtApplicationParty()
                .withMasterDefendant(defendant)
                .withRepresentationOrganisation(representationOrganisation)
                .build();


        final CourtApplication courtApplication = courtApplication()
                .withApplicant(applicant)
                .withType(courtApplicationType().build())
                .build();

        final ApplicantDetails applicantDetails = applicationAtAGlanceHelper.getApplicantDetails(courtApplication);
        //assertThat(applicantDetails.getApplicantSynonym(), is(courtApplication.getType().getApplicantSynonym()));
        assertThat(applicantDetails.getName(), is(format("%s %s", person.getFirstName(), person.getLastName())));
        assertThat(applicantDetails.getAddress(), is(address));
        assertThat(applicantDetails.getInterpreterLanguageNeeds(), is(person.getInterpreterLanguageNeeds()));
        assertThat(applicantDetails.getRemandStatus(), is(defendant.getPersonDefendant().getBailStatus().getDescription()));
        assertThat(applicantDetails.getRepresentation(), is(representationOrganisation.getName()));
    }


    @Test
    public void shouldHandleMissingFields() {

        final CourtApplicationParty applicant = courtApplicationParty()
                .build();

        final CourtApplication courtApplication = courtApplication()
                .withApplicant(applicant)
                .build();

        final ApplicantDetails applicantDetails = applicationAtAGlanceHelper.getApplicantDetails(courtApplication);
        assertThat(applicantDetails, notNullValue());
    }

    @Test
    public void shouldGetResondantDetails() {
        final String organisationName = STRING_GENERATOR.next();
        final Address address = mock(Address.class);

        final Organisation organisation = organisation()
                .withName(organisationName)
                .withAddress(address)
                .build();

        final String firstName = STRING_GENERATOR.next();
        final String lastName = STRING_GENERATOR.next();

        final Person person = person()
                .withFirstName(firstName)
                .withLastName(lastName)
                .build();

        final String role = STRING_GENERATOR.next();

        final AssociatedPerson associatedPerson = associatedPerson()
                .withPerson(person)
                .withRole(role)
                .build();

        final List<AssociatedPerson> organisationPersons = asList(associatedPerson);

        final CourtApplicationParty courtApplicationParty = courtApplicationParty()
                .withOrganisation(organisation)
                .withOrganisationPersons(organisationPersons)
                .build();

        final List<CourtApplicationParty> courtApplicationRespondents = asList(courtApplicationParty);

        final CourtApplication courtApplication = courtApplication()
                .withRespondents(courtApplicationRespondents)
                .build();

        final List<RespondentDetails> respondentDetails = applicationAtAGlanceHelper.       getRespondentDetails(courtApplication);
        assertThat(respondentDetails.size(), is(1));
        final RespondentDetails details = respondentDetails.get(0);
        assertThat(details.getName(), is(organisationName));
        assertThat(details.getAddress(), is(address));
        assertThat(details.getRespondentRepresentatives().size(), is(1));
        final RespondentRepresentatives respondentRepresentatives = details.getRespondentRepresentatives().get(0);
        assertThat(respondentRepresentatives.getRepresentativeName(), is(firstName + " " + lastName));
        assertThat(respondentRepresentatives.getRepresentativePosition(), is(role));
    }

    @Test
    public void shouldGetThirdPartyDetails() {
        final String organisationName = STRING_GENERATOR.next();
        final Address address = mock(Address.class);

        final Organisation organisation = organisation()
                .withName(organisationName)
                .withAddress(address)
                .build();

        final String firstName = STRING_GENERATOR.next();
        final String lastName = STRING_GENERATOR.next();

        final Person person = person()
                .withFirstName(firstName)
                .withLastName(lastName)
                .build();

        final String role = STRING_GENERATOR.next();

        final AssociatedPerson associatedPerson = associatedPerson()
                .withPerson(person)
                .withRole(role)
                .build();

        final List<AssociatedPerson> organisationPersons = asList(associatedPerson);

        final CourtApplicationParty courtApplicationParty = courtApplicationParty()
                .withOrganisation(organisation)
                .withOrganisationPersons(organisationPersons)
                .build();

        final List<CourtApplicationParty> courtApplicationParties = asList(courtApplicationParty);

        final CourtApplication courtApplication = courtApplication()
                .withThirdParties(courtApplicationParties)
                .build();

        final List<ThirdParties> thirdPartyDetails = applicationAtAGlanceHelper.getThirdPartyDetails(courtApplication);
        assertThat(thirdPartyDetails.size(), is(1));
        final ThirdParties details = thirdPartyDetails.get(0);
        assertThat(details.getName(), is(organisationName));
        assertThat(details.getAddress(), is(address));
        assertThat(details.getThirdPartyRepresentatives().size(), is(1));
        final ThirdPartyRepresentatives thirdPartyRepresentatives = details.getThirdPartyRepresentatives().get(0);
        assertThat(thirdPartyRepresentatives.getRepresentativeName(), is(firstName + " " + lastName));
        assertThat(thirdPartyRepresentatives.getRepresentativePosition(), is(role));
    }

    @Test
    public void shouldGetThirdPartyDetailsWhenProsecutingAuthorityExists() {
        final Address address = mock(Address.class);

        final String name = STRING_GENERATOR.next();

        ProsecutingAuthority prosecutingAuthority = ProsecutingAuthority.prosecutingAuthority()
                .withAddress(address)
                .withName(name)
                .build();
        final CourtApplicationParty courtApplicationParty = courtApplicationParty()
                .withProsecutingAuthority(prosecutingAuthority)
                .build();

        final List<CourtApplicationParty> courtApplicationParties = asList(courtApplicationParty);

        final CourtApplication courtApplication = courtApplication()
                .withThirdParties(courtApplicationParties)
                .build();

        final List<ThirdParties> thirdPartyDetails = applicationAtAGlanceHelper.getThirdPartyDetails(courtApplication);
        assertThat(thirdPartyDetails.size(), is(1));
        final ThirdParties details = thirdPartyDetails.get(0);
        assertThat(details.getName(), is(name));
        assertThat(details.getAddress(), is(address));
    }

    @Test
    public void shouldGetRespondentPersonDetailsWhenOrganizationIsEmpty() {

        final Address address = mock(Address.class);

        final String firstName = STRING_GENERATOR.next();
        final String lastName = STRING_GENERATOR.next();

        final Person person = person()
                .withFirstName(firstName)
                .withLastName(lastName)
                .withAddress(address)
                .build();

        final String representationOrgName = STRING_GENERATOR.next();
        Organisation representationOrganisation = organisation().withName(representationOrgName).build();

        final CourtApplicationParty courtApplicationParty = courtApplicationParty()
                .withRepresentationOrganisation(representationOrganisation)
                .withPersonDetails(person)
                .build();

        final List<CourtApplicationParty> courtApplicationRespondents = asList(courtApplicationParty);

        final CourtApplication courtApplication = courtApplication()
                .withRespondents(courtApplicationRespondents)
                .build();

        final List<RespondentDetails> respondentDetails = applicationAtAGlanceHelper.getRespondentDetails(courtApplication);
        assertThat(respondentDetails.size(), is(1));
        final RespondentDetails details = respondentDetails.get(0);
        assertThat(details.getName(), is(firstName+" "+lastName));
        assertThat(details.getAddress(), is(address));

        assertThat(details.getRespondentRepresentatives().size(), is(1));
        final RespondentRepresentatives respondentRepresentatives = details.getRespondentRepresentatives().get(0);
        assertThat(respondentRepresentatives.getRepresentativeName(), is(representationOrgName));
    }
    @Test
    public void shouldGetRespondentForMasterDefendant() {

        final Address address = mock(Address.class);

        final String firstName = STRING_GENERATOR.next();
        final String lastName = STRING_GENERATOR.next();

        final Person person = person()
                .withFirstName(firstName)
                .withLastName(lastName)
                .withAddress(address)
                .build();

        final CourtApplicationParty courtApplicationParty = courtApplicationParty()
                .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(personDefendant().withPersonDetails(person).build()).build())
                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority().withProsecutionAuthorityCode("ABCD").build())
                .build();

        final List<CourtApplicationParty> courtApplicationRespondents = asList(courtApplicationParty);

        final CourtApplication courtApplication = courtApplication()
                .withRespondents(courtApplicationRespondents)
                .build();

        final List<RespondentDetails> respondentDetails = applicationAtAGlanceHelper.getRespondentDetails(courtApplication);
        assertThat(respondentDetails.size(), is(1));
        final RespondentDetails details = respondentDetails.get(0);
        assertThat(details.getName(), is(firstName+" "+lastName));
        assertThat(details.getAddress(), is(address));
    }

    @Test
    public void shouldGetThirdPartyPersonDetailsWhenOrganizationIsEmpty() {

        final Address address = mock(Address.class);

        final String firstName = STRING_GENERATOR.next();
        final String lastName = STRING_GENERATOR.next();

        final Person person = person()
                .withFirstName(firstName)
                .withLastName(lastName)
                .withAddress(address)
                .build();

        final String representationOrgName = STRING_GENERATOR.next();
        Organisation representationOrganisation = organisation().withName(representationOrgName).build();

        final CourtApplicationParty courtApplicationParty = courtApplicationParty()
                .withRepresentationOrganisation(representationOrganisation)
                .withPersonDetails(person)
                .build();

        final List<CourtApplicationParty> courtApplicationParties = asList(courtApplicationParty);

        final CourtApplication courtApplication = courtApplication()
                .withThirdParties(courtApplicationParties)
                .build();

        final List<ThirdParties> thirdPartyDetails = applicationAtAGlanceHelper.getThirdPartyDetails(courtApplication);
        assertThat(thirdPartyDetails.size(), is(1));
        final ThirdParties details = thirdPartyDetails.get(0);
        assertThat(details.getName(), is(firstName+" "+lastName));
        assertThat(details.getAddress(), is(address));

        assertThat(details.getThirdPartyRepresentatives().size(), is(1));
        final ThirdPartyRepresentatives thirdPartyRepresentatives = details.getThirdPartyRepresentatives().get(0);
        assertThat(thirdPartyRepresentatives.getRepresentativeName(), is(representationOrgName));
    }

    @Test
    public void shouldReturnEmptyListWhenRespondentDetailsNotFound() {
        final CourtApplication courtApplication = courtApplication().build();
        final List<RespondentDetails> respondentDetails = applicationAtAGlanceHelper.getRespondentDetails(courtApplication);
        assertThat(respondentDetails, empty());
    }
}
