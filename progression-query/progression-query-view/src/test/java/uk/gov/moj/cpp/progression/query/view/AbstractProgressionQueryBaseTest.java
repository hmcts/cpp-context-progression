package uk.gov.moj.cpp.progression.query.view;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.DefendantBailDocument;
import uk.gov.moj.cpp.progression.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Person;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *
 * @deprecated
 *
 */
@Deprecated
@SuppressWarnings("squid:S2187")
public class AbstractProgressionQueryBaseTest {

    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final String TITLE = "Mr";
    private static final String FIRSTNAME = "James";
    private static final String LASTNAME = "Brown";
    private static final String GENDER = "Male";
    private static final String HOME_TELEPHONE = "02012345678";
    private static final String WORK_TELEPHONE = "02022222223";
    private static final String MOBILE = "07712345678";
    private static final String FAX = "02012345678";
    private static final String EMAIL = "email1@email.com";
    private static final String NATIONALITY = "England";
    private static final UUID ADDRESS_ID = UUID.randomUUID();
    private static final String ADDRESS_1 = "14 Tottenham Court Road";
    private static final String ADDRESS_2 = "London";
    private static final String ADDRESS_3 = "England";
    private static final String ADDRESS_4 = "UK";
    private static final String POST_CODE = "W1T 1JY";
    private static final int DOB_YEAR = 1980;
    private static final Month DOB_MONTH = Month.DECEMBER;
    private static final int DOB_DAY = 25;

    protected List<Defendant> getDefendants(UUID defendantId, UUID caseId) {
        UUID docId = UUID.randomUUID();
        List<Defendant> defendants = new ArrayList<>();
        Defendant defendant = new Defendant();
        defendant.setDefendantId(defendantId);
        CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();
        caseProgressionDetail.setCaseId(caseId);
        DefendantBailDocument defendantBailDocument = new DefendantBailDocument();
        defendantBailDocument.setDocumentId(docId);
        defendantBailDocument.setId(UUID.randomUUID());
        defendantBailDocument.setActive(Boolean.TRUE);
        defendantBailDocument.setDefendant(defendant);
        defendant.setAncillaryOrders("Ancillary Orders");
        defendant.setBailStatus("bailed");
        defendant.setCaseProgressionDetail(caseProgressionDetail);
        defendant.setCustodyTimeLimitDate(LocalDate.now());
        defendant.setDangerousnessAssessment(false);
        defendant.addDefendantBailDocument(defendantBailDocument);
        defendant.setOffences(new HashSet<>());
        defendant.setSentenceHearingReviewDecision(true);
        defendant.setDefenceSolicitorFirm("Unit Test Firm");
        defendant.setPoliceDefendantId("policeDefendantID");
        defendant.setPerson(getPerson());
        defendant.setSentenceHearingReviewDecisionDateTime(ZonedDateTime.now());
        defendant.setIsNoMoreInformationRequired(false);
        defendant.setIsAncillaryOrders(false);
        defendant.setIsPSRRequested(true);
        defendant.setIsStatementOffMeans(true);
        defendant.setIsMedicalDocumentation(false);
        defendant.setInterpreter(new InterpreterDetail(false, "english"));
        defendant.setProvideGuidance("Provide Guidance");
        defendant.setDefenceOthers("John Doe");
        defendant.setProsecutionOthers("James Brown");
        defendant.setMedicalDocumentation("medical documentation");
        defendant.setStatementOfMeans("statement of means");
        defendants.add(defendant);
        defendants.add(defendant);


        return defendants;

    }

    private Person getPerson() {
        return createPerson(PERSON_ID, TITLE, FIRSTNAME, LASTNAME, GENDER, HOME_TELEPHONE, WORK_TELEPHONE, MOBILE, FAX, EMAIL);
    }


    protected CaseProgressionDetail getCaseProgressionDetail(UUID caseId, UUID defendantId) throws Exception {
        Set<Defendant> defendants = getDefendants(defendantId, caseId).stream().collect(Collectors.toSet());
        CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();
        caseProgressionDetail.setCaseId(caseId);
        caseProgressionDetail.setStatus(CaseStatusEnum.READY_FOR_REVIEW);
        caseProgressionDetail.setSendingCommittalDate(LocalDate.now());
        caseProgressionDetail.setDefendants(defendants);
        caseProgressionDetail.setFromCourtCentre("From Centre Court");
        caseProgressionDetail.setCaseStatusUpdatedDateTime(ZonedDateTime.now());
        caseProgressionDetail.setDefendants(defendants);
        caseProgressionDetail.setCaseUrn("Case URN");
        caseProgressionDetail.setSentenceHearingDate(LocalDate.now());
        caseProgressionDetail.setCourtCentreId("court centre id");

        return caseProgressionDetail;
    }

    private Person createPerson(final UUID personId, final String title,
                                final String firstName, final String lastName, final String gender,
                                final String homeTelephone, String workTelephone,
                                final String mobile, final String fax, final String email) {
        return new Person().builder()
                .personId(personId)
                .title(title)
                .firstName(firstName)
                .lastName(lastName)
                .nationality(NATIONALITY)
                .gender(gender)
                .homeTelephone(homeTelephone)
                .workTelephone(workTelephone)
                .fax(fax)
                .mobile(mobile)
                .email(email)
                .dateOfBirth(LocalDate.of(DOB_YEAR, DOB_MONTH, DOB_DAY))
                .address(ADDRESS_ID, ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, POST_CODE).build();
    }
}
