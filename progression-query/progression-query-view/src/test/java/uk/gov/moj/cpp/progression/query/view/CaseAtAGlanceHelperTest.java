package uk.gov.moj.cpp.progression.query.view;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.PleaValue;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.justice.progression.courts.CaagDefendants;
import uk.gov.justice.progression.courts.CaseDetails;
import uk.gov.justice.progression.courts.Defendants;
import uk.gov.justice.progression.courts.HearingListingStatus;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.ProsecutorDetails;
import uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService;

import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.LegalEntityDefendant.legalEntityDefendant;
import static uk.gov.justice.core.courts.Marker.marker;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.Verdict.verdict;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.ADDRESS_1;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.ADDRESS_2;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.ADDRESS_3;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.ADDRESS_4;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.ADDRESS_5;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.POSTCODE;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.YOUTH_MARKER_TYPE;

@RunWith(MockitoJUnitRunner.class)
public class CaseAtAGlanceHelperTest {

    private static final String CASE_URN = "CASEURN";
    private static final String PROSECUTION_AUTHORITY_CODE = "CPS";
    private static final String PROSECUTION_AUTHORITY_REFERENCE = "3603e667";
    private static final String INTERPRETER_LANGUAGE_NEEDS = "Spanish";
    private static final String NATIONALITY_DESCRIPTION = "British";
    private static final String ADD_NATIONALITY_DESCRIPTION = "Australian";
    private static final Address ADDRESS = Address.address().withAddress1("line1").withAddress2("line2").build();
    private static final String REMAND_STATUS = "Conditional bail";
    private static final String OFFENCE_CODE = "CJSCODEGAPS";
    private static final String OFFENCE_TITLE = "Offence Title";
    private static final String OFFENCE_TITLE_WELSH = "Offence Title Welsh";
    private static final String GUILTY = "Guilty";
    private static final String OFFENCE_LEGISLATION = "OffenceLegislation";
    private static final String LEGAL_REP_NAME = "Legal Rep name";
    private static final String LABEL = "result label";
    private static final String AMEND_REASON = "Amend reason";
    private static final String VALUE = "value";
    private static final String CASE_STATUS = "EJECTED";
    private static final String REMOVAL_REASON = "CPP cannot handle this case";
    private static final UUID RECENT_JUDICIAL_RESULT_ID = randomUUID();
    private static final String LEGAL_AID_STATUS = "Granted";
    private static final UUID JHON_SMITH_ID = randomUUID();
    private static final UUID JHON_RAMBO_ID = randomUUID();
    private static final UUID ALAN_SMITH_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private CaseAtAGlanceHelper caseAtAGlanceHelper;
    private final LocalDate DATE_OF_BIRTH = LocalDate.of(1990, 9, 29);
    private static final String ORG_NAME = "Org name";

    @Mock
    private ReferenceDataService referenceDataService;

    @Test
    public void shouldReturnEmptyCaseDetailsWhenNoDataFound() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(prosecutionCase().build(), new ArrayList<>(), referenceDataService);
        final CaseDetails caseDetails = caseAtAGlanceHelper.getCaseDetails();

        assertThat(caseDetails, notNullValue());
        assertThat(caseDetails.getCaseMarkers(), nullValue());
        assertThat(caseDetails.getCaseURN(), nullValue());
        assertThat(caseDetails.getCaseStatus(), nullValue());
        assertThat(caseDetails.getInitiationCode(), nullValue());
        assertThat(caseDetails.getRemovalReason(), nullValue());
    }

    @Test
    public void shouldGetCaseDetailsFromProsecutionCase() {

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), new ArrayList<>(), referenceDataService);
        final CaseDetails caseDetails = caseAtAGlanceHelper.getCaseDetails();

        assertThat(caseDetails, notNullValue());
        assertThat(caseDetails.getCaseMarkers().size(), is(2));
        assertThat(caseDetails.getCaseURN(), is(CASE_URN));
        assertThat(caseDetails.getInitiationCode(), is(InitiationCode.J.toString()));
    }

    @Test
    public void shouldGetCaseDetailsWithCaseStatusAndReason() {

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), new ArrayList<>(), referenceDataService);
        final CaseDetails caseDetails = caseAtAGlanceHelper.getCaseDetails();

        assertThat(caseDetails, notNullValue());
        assertThat(caseDetails.getCaseStatus(), is(CASE_STATUS));
        assertThat(caseDetails.getRemovalReason(), is(REMOVAL_REASON));
    }

    @Test
    public void shouldGetEmptyProsecutorDetailsFromProsecutionCase() {

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(prosecutionCase().build(), new ArrayList<>(),  referenceDataService);
        final ProsecutorDetails prosecutorDetails = caseAtAGlanceHelper.getProsecutorDetails();

        assertThat(prosecutorDetails, notNullValue());
        assertThat(prosecutorDetails.getProsecutionAuthorityCode(), nullValue());
        assertThat(prosecutorDetails.getProsecutionAuthorityReference(), nullValue());
    }

    @Test
    public void shouldGetProsecutorDetailsFromProsecutionCase() {

        final ProsecutionCase prosecutionCase = getProsecutionCaseWithCaseDetails();
        final String prosecutorId = prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId().toString();
        caseAtAGlanceHelper = spy(new CaseAtAGlanceHelper(prosecutionCase, new ArrayList<>(),  referenceDataService));

        when(referenceDataService.getProsecutor(prosecutorId)).thenReturn(getProsecutorFromReferenceData(prosecutorId));

        final ProsecutorDetails prosecutorDetails = caseAtAGlanceHelper.getProsecutorDetails();

        assertThat(prosecutorDetails, notNullValue());
        assertThat(prosecutorDetails.getProsecutionAuthorityCode(), is(PROSECUTION_AUTHORITY_CODE));
        assertThat(prosecutorDetails.getProsecutionAuthorityReference(), is(PROSECUTION_AUTHORITY_REFERENCE));
        assertThat(prosecutorDetails.getProsecutionAuthorityId(), notNullValue());
        assertThat(prosecutorDetails.getAddress().getAddress1(), is("address1"));
        assertThat(prosecutorDetails.getAddress().getAddress2(), is("address2"));
        assertThat(prosecutorDetails.getAddress().getPostcode(), is("postcode"));
    }

    @Test
    public void shouldGetDefendantPersonalDetails() {

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), new ArrayList<>(), referenceDataService);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getDefendantsWithOffenceDetails();

        assertThat(defendants, notNullValue());
        assertThat(defendants.size(), is(3));
        final CaagDefendants defendantSmith = defendants.get(0);
        assertThat(defendantSmith.getFirstName(), is("Jhon"));
        assertThat(defendantSmith.getLastName(), is("Smith"));
        assertThat(defendantSmith.getDateOfBirth(), is(DATE_OF_BIRTH));
        assertThat(defendantSmith.getAge(), notNullValue());
        assertThat(defendantSmith.getInterpreterLanguageNeeds(), is(INTERPRETER_LANGUAGE_NEEDS));
        assertThat(defendantSmith.getNationality(), is(NATIONALITY_DESCRIPTION));
        assertThat(defendantSmith.getAddress(), is(ADDRESS));
        assertThat(defendantSmith.getRemandStatus(), is(REMAND_STATUS));

        final CaagDefendants defendantRambo = defendants.get(1);
        assertThat(defendantRambo.getFirstName(), is("Jhon"));
        assertThat(defendantRambo.getLastName(), is("Rambo"));
        assertThat(defendantRambo.getDateOfBirth(), nullValue());
        assertThat(defendantRambo.getAge(), nullValue());
        assertThat(defendantRambo.getInterpreterLanguageNeeds(), nullValue());
        assertThat(defendantRambo.getNationality(), nullValue());
        assertThat(defendantRambo.getAddress(), nullValue());
        assertThat(defendantRambo.getRemandStatus(), nullValue());

    }

    @Test
    public void shouldGetDefendantPersonalDetailsWithMultipleNationalities() {

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), new ArrayList<>(), referenceDataService);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getDefendantsWithOffenceDetails();

        assertThat(defendants, notNullValue());
        assertThat(defendants.size(), is(3));

        final CaagDefendants defendantSmith = defendants.get(2);
        assertThat(defendantSmith.getFirstName(), is("Alan"));
        assertThat(defendantSmith.getLastName(), is("Smith"));
        assertThat(defendantSmith.getNationality(), is(String.format("%s, %s", NATIONALITY_DESCRIPTION, ADD_NATIONALITY_DESCRIPTION)));
    }


    @Test
    public void shouldGetDefendantMarkerWhenDefendantYouth() {

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), new ArrayList<>(), referenceDataService);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getDefendantsWithOffenceDetails();

        assertThat(defendants, notNullValue());
        assertThat(defendants.size(), is(3));

        final CaagDefendants defendantSmith = defendants.get(2);
        assertThat(defendantSmith.getFirstName(), is("Alan"));
        assertThat(defendantSmith.getLastName(), is("Smith"));
        assertTrue(defendantSmith.getDefendantMarkers().stream().anyMatch(m -> m.equals(YOUTH_MARKER_TYPE)));
    }

    @Test
    public void shouldGetDefendantOffenceDetails() {

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), getCaseHearings(), referenceDataService);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getDefendantsWithOffenceDetails();
        final CaagDefendants defendantSmith = defendants.get(0);
        assertThat(defendantSmith.getCaagDefendantOffences().isEmpty(), is(false));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getOffenceCode(), is(OFFENCE_CODE));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getOffenceTitle(), is(OFFENCE_TITLE));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getOffenceTitleWelsh(), is(OFFENCE_TITLE_WELSH));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCount(), is(2));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getPlea().getPleaValue(), is(PleaValue.GUILTY));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getVerdict().getVerdictType().getCategory(), is(GUILTY));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getOffenceLegislation(), is(OFFENCE_LEGISLATION));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getStartDate(), notNullValue());
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getEndDate(), notNullValue());
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().isEmpty(), is(false));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getLabel(), is(LABEL));

        final CaagDefendants defendantRambo = defendants.get(1);
        assertThat(defendantRambo.getCaagDefendantOffences().isEmpty(), is(false));
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getId(), notNullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getOffenceCode(), is(OFFENCE_CODE));
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getOffenceTitleWelsh(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getCount(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getPlea(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getVerdict(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getOffenceLegislation(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getStartDate(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getEndDate(), nullValue());
    }

    @Test
    public void shouldGetDefendantOffenceResultDetails() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), getCaseHearings(), referenceDataService);

        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getDefendantsWithOffenceDetails();

        final CaagDefendants defendantSmith = defendants.get(0);
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().isEmpty(), is(false));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getId(), notNullValue());
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getLabel(), is(LABEL));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getAmendmentReason(), is(AMEND_REASON));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getAmendedBy(), is("first last"));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getAmendmentDate(), notNullValue());
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getLastSharedDateTime(), notNullValue());
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getOrderedDate(), notNullValue());
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getCaagResultPrompts().isEmpty(), is(false));
    }

    @Test
    public void shouldGetDefendantOffenceResultDetailsInDescendingOrder() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), getCaseHearings(),  referenceDataService);

        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getDefendantsWithOffenceDetails();

        final CaagDefendants defendantSmith = defendants.get(0);
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().isEmpty(), is(false));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(0).getId(), is(RECENT_JUDICIAL_RESULT_ID));
    }

    @Test
    public void shouldGetDefendantWithLegalAidStatus() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), new ArrayList<>(), referenceDataService);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getDefendantsWithOffenceDetails();
        final CaagDefendants defendantSmith = defendants.get(0);
        assertThat(defendantSmith.getLegalAidStatus(), is(LEGAL_AID_STATUS));
    }

    @Test
    public void shouldGetDefendantLegalEntityOrganisationDetails() {

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithDefendantLegalEntity(), new ArrayList<>(),  referenceDataService);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getDefendantsWithOffenceDetails();

        assertThat(defendants, notNullValue());
        assertThat(defendants.size(), is(1));
        final CaagDefendants legalEntityDefendant = defendants.get(0);
        assertThat(legalEntityDefendant.getLegalEntityDefendant().getName(), is(ORG_NAME));
        assertThat(legalEntityDefendant.getLegalEntityDefendant().getAddress().getAddress1(), is(ADDRESS_1));
        assertThat(legalEntityDefendant.getLegalEntityDefendant().getAddress().getAddress2(), is(ADDRESS_2));
        assertThat(legalEntityDefendant.getLegalEntityDefendant().getAddress().getPostcode(), is(POSTCODE));
    }

    private ProsecutionCase getProsecutionCaseWithCaseDetails() {

        return prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(CASE_URN)
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(PROSECUTION_AUTHORITY_CODE)
                        .withProsecutionAuthorityReference(PROSECUTION_AUTHORITY_REFERENCE)
                        .build())

                .withCaseStatus(CASE_STATUS)
                .withRemovalReason(REMOVAL_REASON)
                       .withInitiationCode(InitiationCode.J)
                .withCaseMarkers(asList(marker().withId(randomUUID()).withMarkerTypeDescription("Vulnerable or intimidated victim").build(),
                        marker().withId(randomUUID()).withMarkerTypeDescription("Prohibited Weapons").build()))

                .withDefendants(asList(defendant().withId(JHON_SMITH_ID)
                                .withPersonDefendant(personDefendant()
                                        .withBailStatus(BailStatus.bailStatus().withDescription(REMAND_STATUS).build())
                                        .withPersonDetails(Person.person()
                                                .withFirstName("Jhon").withLastName("Smith")
                                                .withNationalityDescription(NATIONALITY_DESCRIPTION)
                                                .withDateOfBirth(DATE_OF_BIRTH)
                                                .withAddress(ADDRESS)
                                                .withInterpreterLanguageNeeds(INTERPRETER_LANGUAGE_NEEDS).build())
                                        .build())
                                .withDefenceOrganisation(Organisation.organisation().withName(LEGAL_REP_NAME).build())
                                .withOffences(asList(offence().withId(OFFENCE_ID)
                                                .withOffenceCode(OFFENCE_CODE)
                                                .withOffenceTitle(OFFENCE_TITLE)
                                                .withOffenceTitleWelsh(OFFENCE_TITLE_WELSH)
                                                .withCount(2)
                                                .withPlea(Plea.plea().withPleaValue(PleaValue.GUILTY).build())
                                                .withVerdict(verdict().withVerdictType(VerdictType.verdictType().withCategory(GUILTY).build()).build())
                                                .withOffenceLegislation(OFFENCE_LEGISLATION)
                                                .withStartDate(LocalDate.now())
                                                .withEndDate(LocalDate.now())
                                                .withJudicialResults(emptyList())
                                                .build(),
                                        offence().withId(randomUUID()).build()))
                                .withLegalAidStatus(LEGAL_AID_STATUS)
                                .build(),
                        defendant().withId(JHON_RAMBO_ID)
                                .withPersonDefendant(personDefendant().withPersonDetails(Person.person().withFirstName("Jhon").withLastName("Rambo").build()).build())
                                .withOffences(singletonList(offence().withId(randomUUID()).withOffenceCode(OFFENCE_CODE).build()))
                                .build(),
                        defendant().withId(ALAN_SMITH_ID)
                                .withIsYouth(Boolean.TRUE)
                                .withPersonDefendant(personDefendant()
                                        .withPersonDetails(Person.person().withFirstName("Alan").withLastName("Smith")
                                                .withNationalityDescription(NATIONALITY_DESCRIPTION)
                                                .withAdditionalNationalityDescription(ADD_NATIONALITY_DESCRIPTION)
                                                .build()).build())
                                .withOffences(singletonList(offence().withId(randomUUID()).withOffenceCode(OFFENCE_CODE).build()))
                                .build()))
                .build();
    }


    private Optional<JsonObject> getProsecutorFromReferenceData(final String prosecutorId) {

        return Optional.of(createObjectBuilder()
                .add("id", prosecutorId)
                .add("address", createObjectBuilder()
                        .add(ADDRESS_1, "address1")
                        .add(ADDRESS_2, "address2")
                        .add(ADDRESS_3, "address3")
                        .add(ADDRESS_4, "address4")
                        .add(ADDRESS_5, "address5")
                        .add(POSTCODE, "postcode")
                )
                .build());
    }

    private ProsecutionCase getProsecutionCaseWithDefendantLegalEntity() {

        return prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(CASE_URN)
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(PROSECUTION_AUTHORITY_CODE)
                        .withProsecutionAuthorityReference(PROSECUTION_AUTHORITY_REFERENCE)
                        .build())

                .withCaseStatus(CASE_STATUS)
                .withRemovalReason(REMOVAL_REASON)
                .withCaseMarkers(asList(marker().withId(randomUUID()).withMarkerTypeDescription("Vulnerable or intimidated victim").build(),
                        marker().withId(randomUUID()).withMarkerTypeDescription("Prohibited Weapons").build()))

                .withDefendants(singletonList(defendant().withId(randomUUID())
                        .withLegalEntityDefendant(legalEntityDefendant().withOrganisation(organisation().withName(ORG_NAME)
                                .withAddress(address()
                                        .withAddress1(ADDRESS_1)
                                        .withAddress2(ADDRESS_2)
                                        .withAddress3(ADDRESS_3)
                                        .withAddress4(ADDRESS_4)
                                        .withPostcode(POSTCODE)
                                        .build())
                                .build()).build())
                        .withDefenceOrganisation(organisation().withName(LEGAL_REP_NAME).build())
                        .build()))
                .build();
    }

    private List<Hearings> getCaseHearings() {
        return asList(Hearings.hearings()
                        .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                        .withDefendants(asList(Defendants.defendants()
                                .withId(JHON_SMITH_ID)
                                .withOffences(asList(Offences.offences()
                                        .withId(OFFENCE_ID)
                                        .withJudicialResults(asList(judicialResult().withIsDeleted(true).build(),
                                                judicialResult()
                                                        .withJudicialResultId(RECENT_JUDICIAL_RESULT_ID)
                                                        .withOrderedDate(LocalDate.now().plusDays(1))
                                                        .build(),
                                                judicialResult()
                                                        .withJudicialResultId(randomUUID())
                                                        .withDelegatedPowers(DelegatedPowers.delegatedPowers().withFirstName("first").withLastName("last").build())
                                                        .withOrderedDate(LocalDate.now())
                                                        .withAmendmentDate(LocalDate.now())
                                                        .withAmendmentReason(AMEND_REASON)
                                                        .withLastSharedDateTime(LocalDate.now().toString())
                                                        .withLabel(LABEL)
                                                        .withJudicialResultPrompts(singletonList(JudicialResultPrompt.judicialResultPrompt().withLabel(LABEL).withValue(VALUE).build()))
                                                        .build()))
                                        .build()))
                                .build()))
                        .build(),
                Hearings.hearings()
                        .withHearingListingStatus(HearingListingStatus.HEARING_INITIALISED)
                        .withDefendants(asList(Defendants.defendants()
                                .withId(JHON_SMITH_ID)
                                .withOffences(asList(Offences.offences()
                                        .withId(OFFENCE_ID)
                                        .withJudicialResults(asList(judicialResult().withIsDeleted(true).build(),
                                                judicialResult()
                                                        .withJudicialResultId(randomUUID())
                                                        .withDelegatedPowers(DelegatedPowers.delegatedPowers().withFirstName("first").withLastName("last").build())
                                                        .withOrderedDate(LocalDate.now())
                                                        .withAmendmentDate(LocalDate.now())
                                                        .withAmendmentReason(AMEND_REASON)
                                                        .withLastSharedDateTime(LocalDate.now().toString())
                                                        .withLabel(LABEL)
                                                        .withJudicialResultPrompts(singletonList(JudicialResultPrompt.judicialResultPrompt().withLabel(LABEL).withValue(VALUE).build()))
                                                        .build()))
                                        .build()))
                                .build()))
                        .build());
    }
}