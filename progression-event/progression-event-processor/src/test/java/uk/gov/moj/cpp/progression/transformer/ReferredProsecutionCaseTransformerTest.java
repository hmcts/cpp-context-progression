package uk.gov.moj.cpp.progression.transformer;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.HearingLanguage.ENGLISH;
import static uk.gov.justice.core.courts.HearingLanguage.WELSH;
import static uk.gov.justice.core.courts.Verdict.verdict;
import static uk.gov.justice.core.courts.VerdictType.verdictType;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildJsonEnvelope;
import static uk.gov.moj.cpp.progression.service.RefDataService.ETHNICITY;
import static uk.gov.moj.cpp.progression.service.RefDataService.ETHNICITY_CODE;
import static uk.gov.moj.cpp.progression.service.RefDataService.NATIONALITY;
import static uk.gov.moj.cpp.progression.service.RefDataService.NATIONALITY_CODE;
import static uk.gov.moj.cpp.progression.service.RefDataService.PROSECUTOR;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.CJS_OFFENCE_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.LEGISLATION;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.LEGISLATION_WELSH;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.MODEOFTRIAL_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.OFFENCE_TITLE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.WELSH_OFFENCE_TITLE;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAlias;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReferredAssociatedPerson;
import uk.gov.justice.core.courts.ReferredDefendant;
import uk.gov.justice.core.courts.ReferredOffence;
import uk.gov.justice.core.courts.ReferredPerson;
import uk.gov.justice.core.courts.ReferredPersonDefendant;
import uk.gov.justice.core.courts.ReferredProsecutionCase;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.DataValidationException;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.ExpectedException;

import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"squid:S1607"})
public class ReferredProsecutionCaseTransformerTest {
    private final PodamFactory factory = new PodamFactoryImpl();

    @Mock
    private ReferenceDataOffenceService referenceDataOffenceService;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferredProsecutionCaseTransformer referredProsecutionCaseTransformer;

    private static ReferredProsecutionCase getReferredProsecutionCaseWithMinimalPayload() {
        return ReferredProsecutionCase.referredProsecutionCase()
                .withId(randomUUID())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("TFL")
                        .withProsecutionAuthorityCode("TFL")
                        .withCaseURN("PAR123")
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .withInitiationCode(InitiationCode.C)
                .withDefendants(singletonList(ReferredDefendant.referredDefendant()
                        .withId(randomUUID())
                        .withAssociatedPersons(singletonList(ReferredAssociatedPerson.referredAssociatedPerson()
                                .withRole("Role")
                                .withPerson(getReferredPerson()).build()))
                        .withOffences(singletonList(getReferredOffence()))
                        .withProsecutionCaseId(randomUUID())
                        .build()))
                .build();
    }

    private static ReferredProsecutionCase getReferredProsecutionCaseWithName() {
        return ReferredProsecutionCase.referredProsecutionCase()
                .withId(randomUUID())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("TFL")
                        .withProsecutionAuthorityCode("TFL")
                        .withCaseURN("PAR123")
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityName("OrganisationName")
                        .build())
                .withInitiationCode(InitiationCode.C)
                .withDefendants(singletonList(ReferredDefendant.referredDefendant()
                        .withId(randomUUID())
                        .withAssociatedPersons(singletonList(ReferredAssociatedPerson.referredAssociatedPerson()
                                .withRole("Role")
                                .withPerson(getReferredPerson()).build()))
                        .withOffences(singletonList(getReferredOffence()))
                        .withProsecutionCaseId(randomUUID())
                        .build()))
                .build();
    }

    private static ReferredPerson getReferredPerson() {
        return ReferredPerson.referredPerson()
                .withLastName("LastName")
                .withGender(Gender.FEMALE)
                .withAddress(Address.address().withPostcode("CR7 " +
                        "0AA").build())
                .build();
    }

    private static ReferredOffence getReferredOffence() {
        return ReferredOffence.referredOffence()
                .withOffenceDefinitionId(randomUUID())
                .withStartDate(LocalDate.now())
                .withWording("wording")
                .build();
    }

    //Offence

    private static JsonObject getNationalityObject() {
        return Json.createObjectBuilder().add(NATIONALITY_CODE, "N12").add(NATIONALITY, "UK").build();
    }

    private static JsonObject getProsecutor() {
        return Json.createObjectBuilder().add(PROSECUTOR, "TFL").build();
    }

    private static JsonObject getOffence(final String modeoftrial) {
        return Json.createObjectBuilder().add(LEGISLATION, "E12")
                .add(LEGISLATION_WELSH, "123")
                .add(OFFENCE_TITLE, "title-of-offence")
                .add(WELSH_OFFENCE_TITLE, "welsh-title")
                .add(MODEOFTRIAL_CODE, modeoftrial)
                .add(CJS_OFFENCE_CODE, "British").build();
    }

    private static JsonObject getEthnicityObject() {
        return Json.createObjectBuilder().add(ETHNICITY_CODE, "E12").add(ETHNICITY, "British").build();
    }

    @Test
    public void testTransformPerson() {
        // Setup
        final UUID lastName = randomUUID();
        final UUID nationalityId = randomUUID();
        final ReferredPerson referredPerson = factory.populatePojo(ReferredPerson.referredPerson()
                .withLastName(lastName.toString())
                .withAdditionalNationalityId(nationalityId)
                .withNationalityId(nationalityId)
                .withAddress(Address.address().withPostcode("CR1256DF").build())
                .withTitle("DR")
                .build());
        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        when(referenceDataService.getNationality(jsonEnvelope, nationalityId, requester)).thenReturn(of(getNationalityObject()));

        final Ethnicity ethnicity = Ethnicity.ethnicity()
                .withObservedEthnicityCode("E12")
                .withSelfDefinedEthnicityCode("E13")
                .build();

        // Run the test
        final Person result = referredProsecutionCaseTransformer.transform(referredPerson, ethnicity, WELSH, jsonEnvelope);

        //Verify the results
        assertThat(lastName.toString(), is(result.getLastName()));
        assertThat("N12", is(result.getAdditionalNationalityCode()));
        assertThat("E12", is(result.getEthnicity().getObservedEthnicityCode()));
        assertThat("E13", is(result.getEthnicity().getSelfDefinedEthnicityCode()));
        assertThat(result.getHearingLanguageNeeds(), is(WELSH));
    }

    @Test
    public void shouldThrowExceptionForReferenceData() {

        final UUID lastName = randomUUID();
        final UUID nationalityId = randomUUID();
        final UUID ethnicityId = randomUUID();

        final ReferredPerson referredPerson = factory.populatePojo(ReferredPerson.referredPerson()
                .withLastName(lastName.toString())
                .withAdditionalNationalityId(nationalityId)
                .withEthnicityId(ethnicityId)
                .withNationalityId(nationalityId)
                .withAddress(Address.address().withPostcode("CR1256DF").build())
                .withTitle("DR")
                .build());

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        when(referenceDataService.getNationality(jsonEnvelope, nationalityId, requester)).thenThrow(new ReferenceDataNotFoundException("Country Nationality", nationalityId.toString()));

        // Run the test
        assertThrows(ReferenceDataNotFoundException.class, () -> referredProsecutionCaseTransformer.transform(referredPerson, null, null, jsonEnvelope));

        verifyNoMoreInteractions(referenceDataService);
    }

    @Test
    public void testTransformPersonDefendant() {
        // Setup

        final ReferredPersonDefendant referredPersonDefendant = factory
                .manufacturePojoWithFullData(ReferredPersonDefendant.class);

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        when(referenceDataService.getEthinicity(any(), any(), any())).thenReturn(of(getEthnicityObject()));
        when(referenceDataService.getNationality(any(), any(), any())).thenReturn(of(getNationalityObject()));

        // Run the test
        final PersonDefendant result = referredProsecutionCaseTransformer.transform(referredPersonDefendant, ENGLISH,
                jsonEnvelope);

        // Verify the results
        assertThat("E12", is(result.getPersonDetails().getEthnicity().getObservedEthnicityCode()));
        assertThat(ENGLISH , is(result.getPersonDetails().getHearingLanguageNeeds()));
    }

    @Test
    public void shouldThrowExceptionForPersonDefendantReferenceData() {

        final ReferredPersonDefendant referredPersonDefendant = factory.manufacturePojoWithFullData
                (ReferredPersonDefendant.class);

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        when(referenceDataService.getEthinicity(any(), any(), any())).thenThrow(new ReferenceDataNotFoundException
                ("Ethnicity", "E12"));

        // Run the test
        assertThrows(ReferenceDataNotFoundException.class,
                () -> referredProsecutionCaseTransformer.transform(referredPersonDefendant, WELSH, jsonEnvelope));

        verifyNoMoreInteractions(referenceDataService);
    }

    @Test
    public void testTransformOffence() {
        // Setup
        final UUID offenceDefinitionId = randomUUID();
        final UUID id = randomUUID();
        final UUID rrId = randomUUID();

        final ReportingRestriction reportingRestriction = ReportingRestriction.reportingRestriction()
                .withId(rrId)
                .withLabel("label")
                .build();
        final UUID offenceId = randomUUID();
        final String verdictCode = "ProvedSJP";
        final ReferredOffence referredOffence = factory.populatePojo(ReferredOffence.referredOffence()
                .withId(id)
                .withOffenceDefinitionId(offenceDefinitionId)
                .withOrderIndex(0)
                .withReportingRestrictions(singletonList(reportingRestriction))
                .withVerdict(verdict().withOffenceId(offenceId)
                        .withVerdictType(verdictType().
                                withId(offenceId)
                                .withVerdictCode(verdictCode)
                                .build())
                        .build())
                .build());

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        when(referenceDataOffenceService.getOffenceById(offenceDefinitionId, jsonEnvelope, requester))
                .thenReturn(of(getOffence("Indictable")));

        // Run the test
        final Offence result = referredProsecutionCaseTransformer.transform
                (referredOffence, jsonEnvelope, InitiationCode.C);

        //Verify the results

        assertThat(id, is(result.getId()));
        assertThat("Indictable", is(result.getModeOfTrial()));
        assertThat(rrId, is(result.getReportingRestrictions().get(0).getId()));
        assertThat("label", is(result.getReportingRestrictions().get(0).getLabel()));
        assertThat(verdictCode, is(result.getVerdict().getVerdictType().getVerdictCode()));
    }

    @Test
    public void testTransformOffenceDataValidationException() {
        // Setup

        final UUID offenceDefinitionId = randomUUID();
        final UUID id = randomUUID();

        final ReferredOffence referredOffence = factory.populatePojo(ReferredOffence.referredOffence()
                .withId(id)
                .withOffenceDefinitionId(offenceDefinitionId)
                .withOrderIndex(0)
                .build());

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        final JsonObject jsonObject = getOffence("Indictable");

        when(referenceDataOffenceService.getOffenceById(offenceDefinitionId, jsonEnvelope, requester))
                .thenReturn(of(jsonObject));

        // Run the test
        assertThrows(DataValidationException.class, () -> referredProsecutionCaseTransformer.transform(referredOffence, jsonEnvelope, InitiationCode.J));

        //Verify the results
        verifyNoMoreInteractions(referenceDataService);
    }

    @Test
    public void shouldThrowExceptionForOffenceReferenceData() {

        final ReferredOffence referredOffence = factory.populatePojo(ReferredOffence.referredOffence()
                .withId(randomUUID())
                .withOffenceDefinitionId(randomUUID())
                .withOrderIndex(0)
                .build());

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        when(referenceDataOffenceService.getOffenceById(any(), any(), any()))
                .thenThrow(new ReferenceDataNotFoundException("Offence", "id"));

        // Run the test
        assertThrows(ReferenceDataNotFoundException.class, () -> referredProsecutionCaseTransformer.transform(referredOffence, jsonEnvelope, InitiationCode.C));

        // Verify the results
        verifyNoMoreInteractions(referenceDataService);
    }

    @Test
    public void testTransformReferredDefendant() {
        // Setup

        DefendantAlias defendantAlias1 = new DefendantAlias("firstName", "lastName", null, "middleName", "MR");
        DefendantAlias defendantAlias2 = new DefendantAlias("null", null, "legalEntityName", null, null);
        List<DefendantAlias> defendantAliasList = new ArrayList<>();
        defendantAliasList.add(defendantAlias1);
        defendantAliasList.add(defendantAlias2);

        final ReferredDefendant referredDefendant = factory.populatePojo(ReferredDefendant.referredDefendant()
                .withId(randomUUID())
                .withAliases(defendantAliasList)
                .withOffences(
                        new ArrayList<>(singletonList(ReferredOffence.referredOffence().withId(randomUUID()).build())))
                .withAssociatedPersons(new ArrayList<>(singletonList(ReferredAssociatedPerson.referredAssociatedPerson()
                        .withPerson(ReferredPerson.referredPerson().build()).withRole("role").build())))
                .withPersonDefendant(ReferredPersonDefendant.referredPersonDefendant()
                        .withPersonDetails(ReferredPerson.referredPerson().withNationalityId(randomUUID()).build()).build())
                .build());

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        final JsonObject jsonObject = getOffence("Indictable");

        when(referenceDataService.getNationality(any(), any(), any())).thenReturn(of(getNationalityObject()));
        when(referenceDataOffenceService.getOffenceById(any(), any(), any())).thenReturn(of(jsonObject));

        // Run the test
        final Defendant result = referredProsecutionCaseTransformer.transform
                (referredDefendant, jsonEnvelope, InitiationCode.C, WELSH);

        //Verify the results
        assertThat("Indictable", is(result.getOffences().get(0).getModeOfTrial()));
        assertEquals("firstName", result.getAliases().get(0).getFirstName());
        assertEquals("lastName", result.getAliases().get(0).getLastName());
        assertEquals("middleName", result.getAliases().get(0).getMiddleName());
        assertEquals("MR", result.getAliases().get(0).getTitle());
        assertNotNull(result.getCourtProceedingsInitiated());
        assertNull(result.getAliases().get(0).getLegalEntityName());
        assertEquals("legalEntityName", result.getAliases().get(1).getLegalEntityName());
    }

    @Test
    public void testTransform() {
        // Setup

        final ReferredProsecutionCase referredProsecutionCase = factory.populatePojo(
                ReferredProsecutionCase.referredProsecutionCase()
                        .withId(randomUUID())
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                .withProsecutionAuthorityId(randomUUID()).build())
                        .withDefendants(new ArrayList<>(singletonList(ReferredDefendant.referredDefendant()
                                .withId(randomUUID())
                                .withPersonDefendant(ReferredPersonDefendant.referredPersonDefendant()
                                        .withSelfDefinedEthnicityId(randomUUID())
                                        .withPersonDetails(ReferredPerson.referredPerson().build())
                                        .build())
                                .withOffences(new ArrayList<>(singletonList(ReferredOffence.referredOffence()
                                        .withId(randomUUID()).build())))
                                .build())))
                        .build());

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        when(referenceDataService.getEthinicity(any(), any(), any())).thenReturn(of(getEthnicityObject()));
        when(referenceDataService.getNationality(any(), any(), any())).thenReturn(of(getNationalityObject()));
        when(referenceDataService.getProsecutor(any(), any(), any())).thenReturn(of(getProsecutor()));
        when(referenceDataOffenceService.getOffenceById(any(), any(), any())).thenReturn(of(getOffence("None")));

        // Run the test
        final ProsecutionCase result = referredProsecutionCaseTransformer.transform
                (referredProsecutionCase, WELSH, jsonEnvelope);

        //Verify the results
        assertThat("E12", is(result.getDefendants().get(0).getPersonDefendant().getPersonDetails().getEthnicity().getSelfDefinedEthnicityCode()));
        assertThat(WELSH, is(result.getDefendants().get(0).getPersonDefendant().getPersonDetails().getHearingLanguageNeeds()));
    }

    @Test
    public void testTransformWithMinimalData() {
        final ReferredProsecutionCase referredProsecutionCase = getReferredProsecutionCaseWithMinimalPayload();

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        when(referenceDataOffenceService.getOffenceById(any(), any(), any())).thenReturn(of(getOffence("Indictable")));
        when(referenceDataService.getProsecutor(any(), any(), any())).thenReturn(of(getProsecutor()));

        // Run the test
        final ProsecutionCase result = referredProsecutionCaseTransformer.transform
                (referredProsecutionCase, ENGLISH, jsonEnvelope);

        //Verify the results
        assertThat(Gender.FEMALE, is(result.getDefendants().get(0).getAssociatedPersons().get(0).getPerson()
                .getGender()));
    }

    @Test
    public void shouldThrowExceptionWhenProsecutorNotFound() {

        final ReferredProsecutionCase referredProsecutionCase = ReferredProsecutionCase.referredProsecutionCase()
                .withId(randomUUID())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("TFL")
                        .withProsecutionAuthorityCode("TFL")
                        .withCaseURN("PAR123")
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .build();

        when(referenceDataService.getProsecutor(any(), any(), any())).thenReturn(empty());

        // Run the test
        assertThrows(ReferenceDataNotFoundException.class,
                () -> referredProsecutionCaseTransformer.transform(referredProsecutionCase, WELSH, buildJsonEnvelope()));

        verifyNoMoreInteractions(referenceDataService);
    }

    @Test
    public void shouldNotCallReferenceDataForNonStdOrganisationProsecutor() {
        final ReferredProsecutionCase referredProsecutionCase = getReferredProsecutionCaseWithName();
        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        when(referenceDataOffenceService.getOffenceById(any(), any(), any())).thenReturn(of(getOffence("Indictable")));

        // Run the test
        referredProsecutionCaseTransformer.transform
                (referredProsecutionCase, WELSH, jsonEnvelope);

        verify(referenceDataService, times(0)).getProsecutor(any(), any(), any());

    }
}
