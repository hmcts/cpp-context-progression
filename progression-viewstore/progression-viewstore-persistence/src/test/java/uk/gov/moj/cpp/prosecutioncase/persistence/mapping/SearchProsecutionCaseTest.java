package uk.gov.moj.cpp.prosecutioncase.persistence.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.domain.constant.ProsecutingAuthority.CPS;
import static uk.gov.moj.cpp.prosecutioncase.persistence.mapping.SearchProsecutionCaseTest.ApplicationArbitraryValues.APPLICANT_ORGANISATION_NAME;
import static uk.gov.moj.cpp.prosecutioncase.persistence.mapping.SearchProsecutionCaseTest.ApplicationArbitraryValues.APPLICANT_PERSON_FIRST_NAME;
import static uk.gov.moj.cpp.prosecutioncase.persistence.mapping.SearchProsecutionCaseTest.ApplicationArbitraryValues.APPLICANT_PERSON_LAST_NAME;
import static uk.gov.moj.cpp.prosecutioncase.persistence.mapping.SearchProsecutionCaseTest.ApplicationArbitraryValues.APPLICANT_PERSON_MIDDLE_NAME;
import static uk.gov.moj.cpp.prosecutioncase.persistence.mapping.SearchProsecutionCaseTest.ApplicationArbitraryValues.APPLICATION_ARN;
import static uk.gov.moj.cpp.prosecutioncase.persistence.mapping.SearchProsecutionCaseTest.ApplicationArbitraryValues.APPLICATION_ID;
import static uk.gov.moj.cpp.prosecutioncase.persistence.mapping.SearchProsecutionCaseTest.ApplicationArbitraryValues.RESPONDENTS_1_PERSON_FIRST_NAME;
import static uk.gov.moj.cpp.prosecutioncase.persistence.mapping.SearchProsecutionCaseTest.ApplicationArbitraryValues.RESPONDENTS_1_PERSON_LAST_NAME;
import static uk.gov.moj.cpp.prosecutioncase.persistence.mapping.SearchProsecutionCaseTest.ApplicationArbitraryValues.RESPONDENTS_1_PERSON_MIDDLE_NAME;
import static uk.gov.moj.cpp.prosecutioncase.persistence.mapping.SearchProsecutionCaseTest.ApplicationArbitraryValues.RESPONDENTS_2_ORGANISATION_NAME;
import static uk.gov.moj.cpp.prosecutioncase.persistence.mapping.SearchProsecutionCaseTest.ApplicationArbitraryValues.RESPONDENTS_3_PERSON_LAST_NAME;
import static uk.gov.moj.cpp.prosecutioncase.persistence.mapping.SearchProsecutionCaseTest.ApplicationArbitraryValues.RESPONDENTS_4_ORGANISATION_NAME;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SearchProsecutionCaseTest {

    public static final String SPACE = " ";
    private static final String DELIMITER = " | ";
    private static final String COMMA = ",";
    @Spy
    ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @InjectMocks
    SearchProsecutionCase jpaMapper;
    @Mock
    private SearchProsecutionCaseRepository searchRepository;
    private ProsecutionCase prosecutionCase;
    private ProsecutionCase prosecutionCaseWithLegalEntity;
    private Defendant defendant;
    private Defendant defendantWithLegalEntity;
    private CaseProgressionDetail cpsCaseDetail;
    private uk.gov.moj.cpp.progression.persistence.entity.Defendant cpsDefendant;

    @BeforeEach
    public void setUp() {
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        Person personDetails = Person.person()
                .withTitle("DR").withFirstName("John")
                .withMiddleName("S").withLastName("Smith")
                .withDateOfBirth(LocalDate.of(1977, 01, 01))
                .build();

        PersonDefendant personDefendant = PersonDefendant.personDefendant()
                .withPersonDetails(personDetails)
                .build();

        LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant()
                .withOrganisation(Organisation.organisation().withName("ABC LTD").build()).build();

        ProsecutionCaseIdentifier prosecutionCaseIdentifier
                = ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withProsecutionAuthorityReference("PAR-100")
                .withProsecutionAuthorityCode("TFL")
                .build();

        defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withPersonDefendant(personDefendant)
                .build();

        defendantWithLegalEntity = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withLegalEntityDefendant(legalEntityDefendant)
                .build();

        prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(UUID.fromString("5002d600-af66-11e8-b568-0800200c9a66"))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier)
                .withProsecutor(Prosecutor.prosecutor().withProsecutorCode("CPS").build())
                .withCaseStatus("SJP Referral")
                .withDefendants(Collections.singletonList(defendant))
                .build();

        prosecutionCaseWithLegalEntity = ProsecutionCase.prosecutionCase()
                .withId(UUID.fromString("5002d600-af66-11e8-b568-0800200c9a66"))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier)
                .withCaseStatus("SJP Referral")
                .withDefendants(Collections.singletonList(defendantWithLegalEntity))
                .build();

        cpsCaseDetail = new CaseProgressionDetail();
        cpsCaseDetail.setCaseUrn("URN-101");
        cpsCaseDetail.setCaseId(UUID.randomUUID());

        cpsDefendant = new uk.gov.moj.cpp.progression.persistence.entity.Defendant();
        cpsDefendant.setDefendantId(UUID.randomUUID());

    }

    @Test
    public void testExpectedSearchTargetForSJPCases() {
        final SearchProsecutionCaseEntity searchProsecutionCaseDetails
                = jpaMapper.makeSearchable(prosecutionCase, defendant);

        final String expectedSearchTarget = "PAR-100 | John S Smith | 1977-01-01";
        assertNotNull(searchProsecutionCaseDetails);
        assertEquals(expectedSearchTarget, searchProsecutionCaseDetails.getSearchTarget());

    }

    @Test
    public void testExpectedSearchTargetForSJPCasesWithLegalEntity() {
        final SearchProsecutionCaseEntity searchProsecutionCaseDetails
                = jpaMapper.makeSearchable(prosecutionCaseWithLegalEntity, defendantWithLegalEntity);

        final String expectedSearchTarget = "PAR-100 | ABC LTD";
        assertNotNull(searchProsecutionCaseDetails);
        assertEquals(expectedSearchTarget, searchProsecutionCaseDetails.getSearchTarget());

    }

    @Test
    public void testExpectedSearchTargetNoPersonDefendant() {
        defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .build();
        final SearchProsecutionCaseEntity searchProsecutionCaseDetails
                = jpaMapper.makeSearchable(prosecutionCase, defendant);

        final String expectedSearchTarget = "PAR-100";
        assertNotNull(searchProsecutionCaseDetails);
        assertEquals(expectedSearchTarget, searchProsecutionCaseDetails.getSearchTarget());

    }


    @Test
    public void testExpectedSearchResponse() {
        final SearchProsecutionCaseEntity searchProsecutionCaseDetails
                = jpaMapper.makeSearchable(prosecutionCase, defendant);

        assertNotNull(searchProsecutionCaseDetails);
        assertEquals("5002d600-af66-11e8-b568-0800200c9a66", searchProsecutionCaseDetails.getCaseId());
        assertEquals("PAR-100", searchProsecutionCaseDetails.getReference());
        assertEquals("John", searchProsecutionCaseDetails.getDefendantFirstName());
        assertEquals("S", searchProsecutionCaseDetails.getDefendantMiddleName());
        assertEquals("Smith", searchProsecutionCaseDetails.getDefendantLastName());

        assertEquals("1977-01-01", searchProsecutionCaseDetails.getDefendantDob());
        assertEquals("TFL", searchProsecutionCaseDetails.getProsecutor());
        assertEquals("CPS", searchProsecutionCaseDetails.getCpsProsecutor());
        assertEquals("SJP Referral", searchProsecutionCaseDetails.getStatus());
    }

    @Test
    public void testParPresentInSearchTargetForNonCPSCases() {
        final SearchProsecutionCaseEntity searchCaseEntity = jpaMapper.makeSearchable(prosecutionCase, defendant);
        assertTrue(!searchCaseEntity.getReference().isEmpty());
    }

    @Test
    public void testUrnPresentInSearchTargetForCPSCases() {
        final SearchProsecutionCaseEntity searchCaseEntity = jpaMapper.makeSearchable(cpsCaseDetail, cpsDefendant);
        assertTrue(!searchCaseEntity.getReference().isEmpty());
    }

    @Test
    public void testCPSProsecutorPresentForCPSCases() {
        final SearchProsecutionCaseEntity searchCaseEntity = jpaMapper.makeSearchable(cpsCaseDetail, cpsDefendant);
        assertTrue(searchCaseEntity.getProsecutor().equals(CPS.getDescription()));
    }

    @Test
    public void testExpectedSearchTargetForApplicationWhenApplicantPersonAndRespondentsAreOrganisationsAndPersons() {
        //Given Person applicant and mix of multiple Organisation & Person respondents
        CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationReference(APPLICATION_ARN)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(Person.person()
                                .withFirstName(APPLICANT_PERSON_FIRST_NAME)
                                .withMiddleName(APPLICANT_PERSON_MIDDLE_NAME)
                                .withLastName(APPLICANT_PERSON_LAST_NAME)
                                .build())
                        .withMasterDefendant(MasterDefendant.masterDefendant().withMasterDefendantId(UUID.randomUUID()).build())
                        .build())
                .withRespondents(Arrays.asList(
                        CourtApplicationParty.courtApplicationParty()
                                .withPersonDetails(Person.person()
                                        .withFirstName(RESPONDENTS_1_PERSON_FIRST_NAME)
                                        .withMiddleName(RESPONDENTS_1_PERSON_MIDDLE_NAME)
                                        .withLastName(RESPONDENTS_1_PERSON_LAST_NAME)
                                        .build())
                                .build(),
                        CourtApplicationParty.courtApplicationParty()
                                .withOrganisation(Organisation.organisation()
                                        .withName(RESPONDENTS_2_ORGANISATION_NAME)
                                        .build())
                                .build(),
                        CourtApplicationParty.courtApplicationParty()
                                .withPersonDetails(Person.person()
                                        .withLastName(RESPONDENTS_3_PERSON_LAST_NAME) //Missing first & middle names.
                                        .build())
                                .build(),
                        CourtApplicationParty.courtApplicationParty()
                                .withOrganisation(Organisation.organisation()
                                        .withName(RESPONDENTS_4_ORGANISATION_NAME)
                                        .build())
                                .build()))
                .build();
        //when
        final SearchProsecutionCaseEntity searchProsecutionCaseDetails = jpaMapper.makeApplicationSearchable(courtApplication);

        //then
        final String expectedSearchTarget = APPLICATION_ARN.concat(DELIMITER)
                .concat(APPLICANT_PERSON_FIRST_NAME).concat(SPACE)
                .concat(APPLICANT_PERSON_MIDDLE_NAME).concat(SPACE)
                .concat(APPLICANT_PERSON_LAST_NAME).concat(DELIMITER)
                .concat(RESPONDENTS_1_PERSON_FIRST_NAME).concat(SPACE)
                .concat(RESPONDENTS_1_PERSON_MIDDLE_NAME).concat(SPACE)
                .concat(RESPONDENTS_1_PERSON_LAST_NAME).concat(COMMA)
                .concat(RESPONDENTS_2_ORGANISATION_NAME).concat(COMMA)
                .concat(SPACE).concat(SPACE)
                .concat(RESPONDENTS_3_PERSON_LAST_NAME).concat(COMMA)
                .concat(RESPONDENTS_4_ORGANISATION_NAME);

        assertNotNull(searchProsecutionCaseDetails);
        //assertEquals(expectedSearchTarget, searchProsecutionCaseDetails.getSearchTarget());

    }

    @Test
    public void testExpectedSearchTargetForApplicationWhenApplicantIsOrganisation() {
        //Given application is Organisation
        CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationReference(APPLICATION_ARN)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withOrganisation(Organisation.organisation()
                                .withName(APPLICANT_ORGANISATION_NAME)
                                .build())
                        .build())
                .withRespondents(Collections.singletonList(CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(Person.person()
                                .withFirstName(RESPONDENTS_1_PERSON_FIRST_NAME)
                                .withMiddleName(RESPONDENTS_1_PERSON_MIDDLE_NAME)
                                .withLastName(RESPONDENTS_1_PERSON_LAST_NAME)
                                .build())
                        .build()))
                .build();

        //when
        final SearchProsecutionCaseEntity searchProsecutionCaseDetails = jpaMapper.makeApplicationSearchable(courtApplication);

        //then
        final String expectedSearchTarget = APPLICATION_ARN.concat(DELIMITER)
                .concat(APPLICANT_ORGANISATION_NAME).concat("  ").concat(DELIMITER)
                .concat(RESPONDENTS_1_PERSON_FIRST_NAME).concat(SPACE)
                .concat(RESPONDENTS_1_PERSON_MIDDLE_NAME).concat(SPACE)
                .concat(RESPONDENTS_1_PERSON_LAST_NAME);
        assertNotNull(searchProsecutionCaseDetails);
        assertEquals(expectedSearchTarget, searchProsecutionCaseDetails.getSearchTarget());

    }


    static class ApplicationArbitraryValues {
        final static UUID APPLICATION_ID = UUID.randomUUID();
        final static String APPLICATION_ARN = "ARN";
        final static String APPLICANT_PERSON_FIRST_NAME = "ApplicantPersonFirstName";
        final static String APPLICANT_PERSON_MIDDLE_NAME = "ApplicantPersonMiddleName";
        final static String APPLICANT_PERSON_LAST_NAME = "ApplicantPersonLastName";
        final static String APPLICANT_ORGANISATION_NAME = "ApplicantOrganisationName";

        final static String RESPONDENTS_1_PERSON_FIRST_NAME = "Respondent1PersonFirstName";
        final static String RESPONDENTS_1_PERSON_MIDDLE_NAME = "Respondent1PersonMiddleName";
        final static String RESPONDENTS_1_PERSON_LAST_NAME = "Respondent1PersonLastName";
        final static String RESPONDENTS_2_ORGANISATION_NAME = "Respondent2Organisation";

        final static String RESPONDENTS_3_PERSON_LAST_NAME = "Respondent3PersonLastName";
        final static String RESPONDENTS_4_ORGANISATION_NAME = "Respondent4Organisation";

    }
}
