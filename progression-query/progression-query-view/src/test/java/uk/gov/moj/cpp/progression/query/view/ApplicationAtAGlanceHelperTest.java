package uk.gov.moj.cpp.progression.query.view;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.AssociatedPerson.associatedPerson;
import static uk.gov.justice.core.courts.BailStatus.bailStatus;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.justice.core.courts.CourtApplicationPayment.courtApplicationPayment;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.courts.progression.query.ApplicationDetails;
import uk.gov.justice.courts.progression.query.ThirdParties;
import uk.gov.justice.courts.progression.query.ThirdPartyRepresentatives;
import uk.gov.justice.progression.courts.ApplicantDetails;
import uk.gov.justice.progression.courts.RespondentDetails;
import uk.gov.justice.progression.courts.RespondentRepresentatives;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.BooleanGenerator;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.progression.query.view.service.OrganisationService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationAtAGlanceHelperTest {

    private static final StringGenerator STRING_GENERATOR = new StringGenerator();
    private static final BooleanGenerator BOOLEAN_GENERATOR = new BooleanGenerator();

    @Mock
    private OrganisationService organisationService;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private ApplicationAtAGlanceHelper applicationAtAGlanceHelper;

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

        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("progression.query.application.aaag"), payload);

        final ApplicantDetails applicantDetails = applicationAtAGlanceHelper.getApplicantDetails(courtApplication, jsonEnvelope);
        assertThat(applicantDetails.getName(), is(format("%s %s", person.getFirstName(), person.getLastName())));
        assertThat(applicantDetails.getAddress(), is(address));
        assertThat(applicantDetails.getInterpreterLanguageNeeds(), is(person.getInterpreterLanguageNeeds()));
        assertThat(applicantDetails.getRepresentation(), is(representationOrganisation.getName()));
    }

    @Test
    public void shouldGetApplicantDetailsWhenApplicantIsAnIndividualAndRepresentationOrganisationIsNull() {
        final Address address = mock(Address.class);

        final UUID caseId = randomUUID();
        final UUID applicantId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID masterDefendantId = randomUUID();

        final Person person = person()
                .withFirstName(STRING_GENERATOR.next())
                .withLastName(STRING_GENERATOR.next())
                .withAddress(address)
                .withDateOfBirth(now().minusYears(18L))
                .withNationalityDescription(STRING_GENERATOR.next())
                .build();

        final CourtApplicationParty applicant = courtApplicationParty()
                .withId(applicantId)
                .withPersonDetails(person)
                .withMasterDefendant(MasterDefendant.masterDefendant().
                        withDefendantCase(List.of(DefendantCase.defendantCase().withDefendantId(defendantId).withCaseId(caseId).build())).build())
                .build();

        final UUID id = randomUUID();
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();

        final CourtApplicationCase courtApplicationCase1 = CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(caseId1).build();
        final CourtApplicationCase courtApplicationCase2 = CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(caseId2).build();

        final CourtApplication courtApplication = courtApplication()
                .withId(id)
                .withSubject(courtApplicationParty().withId(applicantId)
                        .withMasterDefendant(MasterDefendant.masterDefendant().withMasterDefendantId(masterDefendantId).build()).
                        build())
                .withApplicant(applicant)
                .withCourtApplicationCases(List.of(courtApplicationCase1, courtApplicationCase2))
                .build();

        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("progression.query.application.aaag"), payload);

        final JsonObject representation = Json.createObjectBuilder()
                .add("defendants", Json.createArrayBuilder().add(
                        Json.createObjectBuilder()
                                .add("defendantId", masterDefendantId.toString())
                                .add("organisationName", "organisationName")
                                .add("organisationAddress", Json.createObjectBuilder()
                                        .add("address1", "address1")
                                        .add("address2", "address2")
                                        .add("address3", "address3")
                                        .add("address4", "address4")
                                        .add("addressPostcode", "addressPostcode")
                                )
                )).build();

        final ProsecutionCaseEntity prosecutionCaseEntity = mock(ProsecutionCaseEntity.class);
        final String prosecutionCaseEntityPayload = "payload";
        final JsonObject prosecutionCaseEntityJsonObject = mock(JsonObject.class);
        final Defendant defendant1 = Defendant.defendant().withId(masterDefendantId).withMasterDefendantId(masterDefendantId).build();
        final Defendant defendant2 = Defendant.defendant().withId(randomUUID()).withMasterDefendantId(randomUUID()).build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withDefendants(List.of(defendant1, defendant2)).build();

        when(organisationService.getAssociatedCaseDefendantsWithOrganisationAddress(any(Envelope.class), anyString()))
                .thenReturn(representation);
        when(prosecutionCaseRepository.findByCaseId(caseId1)).thenReturn(prosecutionCaseEntity);
        when(prosecutionCaseEntity.getPayload()).thenReturn(prosecutionCaseEntityPayload);
        when(stringToJsonObjectConverter.convert(prosecutionCaseEntityPayload)).thenReturn(prosecutionCaseEntityJsonObject);
        when(jsonObjectToObjectConverter.convert(prosecutionCaseEntityJsonObject, ProsecutionCase.class)).thenReturn(prosecutionCase);

        final ApplicantDetails applicantDetails = applicationAtAGlanceHelper.getApplicantDetails(courtApplication, jsonEnvelope);
        assertThat(applicantDetails.getName(), is(format("%s %s", person.getFirstName(), person.getLastName())));
        assertThat(applicantDetails.getAddress(), is(address));
        assertThat(applicantDetails.getInterpreterLanguageNeeds(), is(person.getInterpreterLanguageNeeds()));
        assertThat(applicantDetails.getRepresentation(), is("organisationName,address1,address2,address3,address4,addressPostcode"));
    }

    @Test
    public void shouldGetApplicantDetailsWhenApplicantIsAnOrganisation() {

        final Organisation applicantOrgan = organisation()
                .withName(STRING_GENERATOR.next())
                .withIsProbationBreach(true)
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

        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("progression.query.application.aaag"), payload);

        final ApplicantDetails applicantDetails = applicationAtAGlanceHelper.getApplicantDetails(courtApplication, jsonEnvelope);
        assertThat(applicantDetails.getName(), is(applicantOrgan.getName()));
        assertTrue(applicantDetails.getIsProbationBreach());
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

        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("progression.query.application.aaag"), payload);

        final ApplicantDetails applicantDetails = applicationAtAGlanceHelper.getApplicantDetails(courtApplication, jsonEnvelope);
        //assertThat(applicantDetails.getApplicantSynonym(), is(courtApplication.getType().getApplicantSynonym()));
        assertThat(applicantDetails.getName(), is(format("%s %s", person.getFirstName(), person.getLastName())));
        assertThat(applicantDetails.getAddress(), is(address));
        assertThat(applicantDetails.getInterpreterLanguageNeeds(), is(person.getInterpreterLanguageNeeds()));
        assertThat(applicantDetails.getRemandStatus(), is(defendant.getPersonDefendant().getBailStatus().getDescription()));
        assertThat(applicantDetails.getRepresentation(), is(representationOrganisation.getName()));
    }

    @Test
    public void shouldHandleApplicantMasterDefendantId() {
        final UUID caseId = randomUUID();
        final UUID applicantId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID masterDefendantId = randomUUID();

        final CourtApplicationParty applicant = courtApplicationParty()
                .withId(applicantId)
                .withMasterDefendant(MasterDefendant.masterDefendant().
                        withDefendantCase(List.of(DefendantCase.defendantCase().withDefendantId(defendantId).withCaseId(caseId).build())).build())
                .build();

        final UUID id = randomUUID();
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();

        final CourtApplicationCase courtApplicationCase1 = CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(caseId1).build();
        final CourtApplicationCase courtApplicationCase2 = CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(caseId2).build();

        final CourtApplication courtApplication = courtApplication()
                .withId(id)
                .withSubject(courtApplicationParty().withId(applicantId)
                        .withMasterDefendant(MasterDefendant.masterDefendant().withMasterDefendantId(masterDefendantId).build()).
                        build())
                .withApplicant(applicant)
                .withCourtApplicationCases(List.of(courtApplicationCase1, courtApplicationCase2))
                .build();

        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("progression.query.application.aaag"), payload);

        final JsonObject representation = Json.createObjectBuilder()
                .add("defendants", Json.createArrayBuilder().add(
                        Json.createObjectBuilder()
                                .add("defendantId", masterDefendantId.toString())
                                .add("organisationName", "organisationName")
                                .add("organisationAddress", Json.createObjectBuilder()
                                        .add("address1", "address1")
                                        .add("address2", "address2")
                                        .add("address3", "address3")
                                        .add("address4", "address4")
                                        .add("addressPostcode", "addressPostcode")
                                )
                )).build();

        final ProsecutionCaseEntity prosecutionCaseEntity = mock(ProsecutionCaseEntity.class);
        final String prosecutionCaseEntityPayload = "payload";
        final JsonObject prosecutionCaseEntityJsonObject = mock(JsonObject.class);
        final Defendant defendant1 = Defendant.defendant().withId(masterDefendantId).withMasterDefendantId(masterDefendantId).build();
        final Defendant defendant2 = Defendant.defendant().withId(randomUUID()).withMasterDefendantId(randomUUID()).build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withDefendants(List.of(defendant1, defendant2)).build();

        when(organisationService.getAssociatedCaseDefendantsWithOrganisationAddress(any(Envelope.class), anyString()))
                .thenReturn(representation);
        when(prosecutionCaseRepository.findByCaseId(caseId1)).thenReturn(prosecutionCaseEntity);
        when(prosecutionCaseEntity.getPayload()).thenReturn(prosecutionCaseEntityPayload);
        when(stringToJsonObjectConverter.convert(prosecutionCaseEntityPayload)).thenReturn(prosecutionCaseEntityJsonObject);
        when(jsonObjectToObjectConverter.convert(prosecutionCaseEntityJsonObject, ProsecutionCase.class)).thenReturn(prosecutionCase);

        final ApplicantDetails applicantDetails = applicationAtAGlanceHelper.getApplicantDetails(courtApplication, jsonEnvelope);
        assertThat(applicantDetails, notNullValue());
        assertThat(applicantDetails.getRepresentation(), is("organisationName,address1,address2,address3,address4,addressPostcode"));
    }

    @Test
    public void shouldHandleMissingFields() {

        final CourtApplicationParty applicant = courtApplicationParty()
                .build();

        final CourtApplication courtApplication = courtApplication()
                .withApplicant(applicant)
                .build();

        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("progression.query.application.aaag"), payload);

        final ApplicantDetails applicantDetails = applicationAtAGlanceHelper.getApplicantDetails(courtApplication, jsonEnvelope);
        assertThat(applicantDetails, notNullValue());
    }

    @Test
    public void shouldGetResondantDetails() {
        final String organisationName = STRING_GENERATOR.next();
        final Address address = mock(Address.class);

        final Organisation organisation = organisation()
                .withName(organisationName)
                .withAddress(address)
                .withIsProbationBreach(true)
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

        final List<RespondentDetails> respondentDetails = applicationAtAGlanceHelper.getRespondentDetails(courtApplication);
        assertThat(respondentDetails.size(), is(1));
        final RespondentDetails details = respondentDetails.get(0);
        assertThat(details.getName(), is(organisationName));
        assertThat(details.getAddress(), is(address));
        assertTrue(details.getIsProbationBreach());
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
