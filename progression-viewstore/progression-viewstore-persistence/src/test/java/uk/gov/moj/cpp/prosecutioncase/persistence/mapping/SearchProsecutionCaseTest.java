package uk.gov.moj.cpp.prosecutioncase.persistence.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.moj.cpp.progression.domain.constant.ProsecutingAuthority.CPS;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Title;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SearchProsecutionCaseTest {

    @Spy
    ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    SearchProsecutionCase jpaMapper;

    @Mock
    private SearchProsecutionCaseRepository searchRepository;

    private ProsecutionCase prosecutionCase;

    private Defendant defendant;

    private CaseProgressionDetail cpsCaseDetail;

    private uk.gov.moj.cpp.progression.persistence.entity.Defendant cpsDefendant;

    @Before
    public void setUp() {
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        Person personDetails = Person.person()
                .withTitle(Title.MR).withFirstName("John")
                .withMiddleName("S").withLastName("Smith")
                .withDateOfBirth("1977-01-01")
                .build();

        PersonDefendant personDefendant = PersonDefendant.personDefendant()
                .withPersonDetails(personDetails)
                .build();

        ProsecutionCaseIdentifier prosecutionCaseIdentifier
                = ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withProsecutionAuthorityReference("PAR-100")
                .withProsecutionAuthorityCode("TFL")
                .build();

        defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withPersonDefendant(personDefendant)
                .build();

        prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(UUID.fromString("5002d600-af66-11e8-b568-0800200c9a66"))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier)
                .withCaseStatus("SJP Referral")
                .withDefendants(Collections.singletonList(defendant))
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
        assertEquals(expectedSearchTarget,searchProsecutionCaseDetails.getSearchTarget());

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
        assertEquals("SJP_REFERRAL", searchProsecutionCaseDetails.getStatus());
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
}
