package uk.gov.moj.cpp.progression.helper;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class SummonsDataHelperTest {

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private Requester requester;

    @Before
    public void initMocks() {
        setField(this.objectToJsonObjectConverter, "mapper", objectMapper);
        setField(this.jsonObjectToObjectConverter, "objectMapper", objectMapper);
    }

    @Test
    public void shouldTransformPersonDefendantToAddressee() {
        Defendant defendant = createPersonDefendant();
        JsonObject addresseeJson = SummonsDataHelper.extractAddressee(objectToJsonObjectConverter.convert(defendant));
        assertThat("Tim Martin Paine", is(addresseeJson.getString("name")));
        assertThat("22 Crown House", is(addresseeJson.getJsonObject("address").getString("line1")));
        assertThat("40 Albert Street", is(addresseeJson.getJsonObject("address").getString("line2")));
        assertThat("Broad way", is(addresseeJson.getJsonObject("address").getString("line3")));
        assertThat("Taunton", is(addresseeJson.getJsonObject("address").getString("line4")));
        assertThat("Somerset", is(addresseeJson.getJsonObject("address").getString("line5")));
        assertThat("TA5 5TA", is(addresseeJson.getJsonObject("address").getString("postCode")));
    }

    @Test
    public void shouldTransformLegalDefendantToAddressee() {
        Defendant defendant = createLegalEntityDefendant();
        JsonObject addresseeJson = SummonsDataHelper.extractAddressee(objectToJsonObjectConverter.convert(defendant));
        assertThat("ABC Ltd", is(addresseeJson.getString("name")));
        assertThat("22 Crown House", is(addresseeJson.getJsonObject("address").getString("line1")));
        assertThat("40 Albert Street", is(addresseeJson.getJsonObject("address").getString("line2")));
        assertThat("Broad way", is(addresseeJson.getJsonObject("address").getString("line3")));
        assertThat("Taunton", is(addresseeJson.getJsonObject("address").getString("line4")));
        assertThat("Somerset", is(addresseeJson.getJsonObject("address").getString("line5")));
        assertThat("TA5 5TA", is(addresseeJson.getJsonObject("address").getString("postCode")));
    }

    @Test
    public void shouldExtractDefendantFromPersonDefendant() {
        Defendant defendant = createPersonDefendant();
        JsonObject defendantJson = SummonsDataHelper.extractDefendant(objectToJsonObjectConverter.convert(defendant));
        assertThat("Tim Martin Paine", is(defendantJson.getString("name")));
        assertThat("2000-01-15", is(defendantJson.getString("dateOfBirth")));
        assertThat("22 Crown House", is(defendantJson.getJsonObject("address").getString("line1")));
        assertThat("40 Albert Street", is(defendantJson.getJsonObject("address").getString("line2")));
        assertThat("Broad way", is(defendantJson.getJsonObject("address").getString("line3")));
        assertThat("Taunton", is(defendantJson.getJsonObject("address").getString("line4")));
        assertThat("Somerset", is(defendantJson.getJsonObject("address").getString("line5")));
        assertThat("TA5 5TA", is(defendantJson.getJsonObject("address").getString("postCode")));
    }

    @Test
    public void shouldExtractDefendantFromLegalDefendant() {
        Defendant defendant = createLegalEntityDefendant();
        JsonObject defendantJson = SummonsDataHelper.extractDefendant(objectToJsonObjectConverter.convert(defendant));
        assertThat("ABC Ltd", is(defendantJson.getString("name")));
        assertThat("22 Crown House", is(defendantJson.getJsonObject("address").getString("line1")));
        assertThat("40 Albert Street", is(defendantJson.getJsonObject("address").getString("line2")));
        assertThat("Broad way", is(defendantJson.getJsonObject("address").getString("line3")));
        assertThat("Taunton", is(defendantJson.getJsonObject("address").getString("line4")));
        assertThat("Somerset", is(defendantJson.getJsonObject("address").getString("line5")));
        assertThat("TA5 5TA", is(defendantJson.getJsonObject("address").getString("postCode")));
    }

    @Test
    public void shouldExtractGuardianFromDefendant() {
        Defendant defendant = createPersonDefendant();
        JsonObject guardianAddresseeJson = SummonsDataHelper.extractGuardianAddressee(objectToJsonObjectConverter.convert(defendant));
        assertThat("Rob Martin Paine", is(guardianAddresseeJson.getString("name")));
        assertThat("36 Brown House", is(guardianAddresseeJson.getJsonObject("address").getString("line1")));
        assertThat("450 London Street", is(guardianAddresseeJson.getJsonObject("address").getString("line2")));
        assertThat("Ocean way", is(guardianAddresseeJson.getJsonObject("address").getString("line3")));
        assertThat("Bristol", is(guardianAddresseeJson.getJsonObject("address").getString("line4")));
        assertThat("Somerset", is(guardianAddresseeJson.getJsonObject("address").getString("line5")));
        assertThat("BS1 4TE", is(guardianAddresseeJson.getJsonObject("address").getString("postCode")));
    }

    @Test
    public void shouldExtractYouthOnlyFromDefendant() {
        Defendant defendant = createPersonDefendant();
        JsonObject guardianAddresseeJson = SummonsDataHelper.extractYouth(objectToJsonObjectConverter.convert(defendant), true);
        assertThat("36 Brown House", is(guardianAddresseeJson.getJsonObject("address").getString("line1")));
        assertThat("450 London Street", is(guardianAddresseeJson.getJsonObject("address").getString("line2")));
        assertThat("Ocean way", is(guardianAddresseeJson.getJsonObject("address").getString("line3")));
        assertThat("Bristol", is(guardianAddresseeJson.getJsonObject("address").getString("line4")));
        assertThat("Somerset", is(guardianAddresseeJson.getJsonObject("address").getString("line5")));
        assertThat("BS1 4TE", is(guardianAddresseeJson.getJsonObject("address").getString("postCode")));
    }

    @Test
    public void shouldExtractYouthGuardianFromDefendant() {
        Defendant defendant = createPersonDefendant();
        JsonObject guardianAddresseeJson = SummonsDataHelper.extractYouth(objectToJsonObjectConverter.convert(defendant), false);
        assertThat("Rob Martin Paine", is(guardianAddresseeJson.getString("parentGuardianName")));
        assertThat("36 Brown House", is(guardianAddresseeJson.getJsonObject("address").getString("line1")));
        assertThat("450 London Street", is(guardianAddresseeJson.getJsonObject("address").getString("line2")));
        assertThat("Ocean way", is(guardianAddresseeJson.getJsonObject("address").getString("line3")));
        assertThat("Bristol", is(guardianAddresseeJson.getJsonObject("address").getString("line4")));
        assertThat("Somerset", is(guardianAddresseeJson.getJsonObject("address").getString("line5")));
        assertThat("BS1 4TE", is(guardianAddresseeJson.getJsonObject("address").getString("postCode")));
    }

    @Test
    public void shouldExtractOffencesFromDefendant() {
        Defendant defendant = createPersonDefendant();
        JsonArray offences = SummonsDataHelper.extractOffences(objectToJsonObjectConverter.convert(defendant));
        assertThat(1, is(offences.size()));
        assertThat("off title", is(offences.getJsonObject(0).getString("offenceTitle")));
        assertThat("off title wel", is(offences.getJsonObject(0).getString("offenceTitleWelsh")));
        assertThat("off leg", is(offences.getJsonObject(0).getString("offenceLegislation")));
        assertThat("off leg wel", is(offences.getJsonObject(0).getString("offenceLegislationWelsh")));
    }

    @Test
    public void shouldPopulateReferral() {
        JsonObject referenceData = createReferralReferenceData();
        JsonObject referralReason = SummonsDataHelper.populateReferral(objectToJsonObjectConverter.convert(referenceData));
        assertThat(referenceData.getString("id"), is(referralReason.getString("id")));
        assertThat(referenceData.getString("reason"), is(referralReason.getString("referralReason")));
        assertThat(referenceData.getString("welshReason"), is(referralReason.getString("referralReasonWelsh")));
        assertThat(referenceData.getString("subReason"), is(referralReason.getString("referralText")));
        assertThat(referenceData.getString("welshSubReason"), is(referralReason.getString("referralTextWelsh")));
    }

    @Test
    public void shouldPopulateAddress() {
        JsonObject referenceData = createCourtCentreAddress();
        JsonObject referralReason = SummonsDataHelper.populateCourtCentreAddress(objectToJsonObjectConverter.convert(referenceData));
        assertThat(referenceData.getString("address1"), is(referralReason.getString("line1")));
        assertThat(referenceData.getString("address2"), is(referralReason.getString("line2")));
        assertThat(referenceData.getString("address3"), is(referralReason.getString("line3")));
        assertThat(referenceData.getString("address4"), is(referralReason.getString("line4")));
        assertThat(referenceData.getString("address5"), is(referralReason.getString("line5")));
        assertThat(referenceData.getString("postcode"), is(referralReason.getString("postCode")));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionDocumentAccessDataWithFileReference() {
        UUID documentAccessId = randomUUID();
        when(referenceDataService.getDocumentTypeAccessData(documentAccessId, jsonEnvelope, requester)).thenReturn(Optional.empty());
        SummonsDataHelper.getDocumentTypeData(documentAccessId, referenceDataService, jsonEnvelope, requester);
    }

    @Test
    public void shouldReturnDocumentAccessDataWithFileReference() {
        UUID documentAccessId = randomUUID();
        final JsonObject returnValue = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("section", "Charges")
                .build();
        when(referenceDataService.getDocumentTypeAccessData(documentAccessId, jsonEnvelope, requester)).thenReturn(Optional.of(returnValue));
        final JsonObject documentTypeDataForSummons = SummonsDataHelper.getDocumentTypeData(documentAccessId, referenceDataService, jsonEnvelope, requester);
        assertThat(documentTypeDataForSummons, is(returnValue));
    }
    private JsonObject createCourtCentreAddress() {
        return createObjectBuilder()
                .add("address1", "Liverpool Crown Court")
                .add("address2", "London Road")
                .add("address3", "London City")
                .add("address4", "Greater London")
                .add("address5", "England")
                .add("postcode", "EC15 3ER")
                .build();
    }

    private JsonObject createReferralReferenceData() {
        return createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("reason", "ref reason")
                .add("welshReason", "ref wel reason")
                .add("subReason", "sub reason")
                .add("welshSubReason", "wel sub reason")
                .build();
    }


    private Defendant createPersonDefendant() {
        return Defendant.defendant()
                .withId(randomUUID())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withFirstName("Tim")
                                .withMiddleName("Martin")
                                .withLastName("Paine")
                                .withDateOfBirth(LocalDate.of(2000, Month.JANUARY, 15))
                                .withAddress(Address.address()
                                        .withAddress1("22 Crown House")
                                        .withAddress2("40 Albert Street")
                                        .withAddress3("Broad way")
                                        .withAddress4("Taunton")
                                        .withAddress5("Somerset")
                                        .withPostcode("TA5 5TA")
                                        .build())
                                .build())
                        .build())
                .withAssociatedPersons(Arrays.asList(AssociatedPerson.associatedPerson()
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
                        .build()))
                .withOffences(Arrays.asList(Offence.offence()
                        .withOffenceTitle("off title")
                        .withOffenceTitleWelsh("off title wel")
                        .withOffenceLegislation("off leg")
                        .withOffenceLegislationWelsh("off leg wel")
                        .build()))
                .build();
    }

    private Defendant createLegalEntityDefendant() {
        return Defendant.defendant()
                .withId(randomUUID())
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withName("ABC Ltd")
                                .withAddress(Address.address()
                                        .withAddress1("22 Crown House")
                                        .withAddress2("40 Albert Street")
                                        .withAddress3("Broad way")
                                        .withAddress4("Taunton")
                                        .withAddress5("Somerset")
                                        .withPostcode("TA5 5TA")
                                        .build())
                                .build())
                        .build())
                .withAssociatedPersons(Arrays.asList(AssociatedPerson.associatedPerson()
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
                        .build()))
                .withOffences(Arrays.asList(Offence.offence()
                        .withOffenceTitle("off title")
                        .withOffenceTitleWelsh("off title wel")
                        .withOffenceLegislation("off leg")
                        .withOffenceLegislationWelsh("off leg wel")
                        .build()))
                .build();
    }

}
