package uk.gov.moj.cpp.progression.helper;

import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.POST_CODE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import java.time.LocalDate;

import org.apache.commons.lang3.RandomStringUtils;

public class MockCourtApplication {

    private String userId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtApplicationId;
    private String particulars;
    private String applicantReceivedDate;
    private String applicationType;
    private Boolean appeal;
    private Boolean applicantAppellantFlag;
    private String paymentReference;
    private String applicantSynonym;
    private String applicantFirstName;
    private String applicantLastName;
    private String applicantNationality;
    private String applicantRemandStatus;
    private String applicantRepresentation;
    private String interpreterLanguageNeeds;
    private LocalDate applicantDoB;
    private String applicantAddress1;
    private String applicantAddress2;
    private String applicantAddress3;
    private String applicantAddress4;
    private String applicantAddress5;
    private String applicantPostCode;
    private String applicationReference;
    private String respondentDefendantId;
    private String respondentOrganisationName;
    private String respondentOrganisationAddress1;
    private String respondentOrganisationAddress2;
    private String respondentOrganisationAddress3;
    private String respondentOrganisationAddress4;
    private String respondentOrganisationAddress5;
    private String respondentOrganisationPostcode;
    private String respondentRepresentativeFirstName;
    private String respondentRepresentativeLastName;
    private String respondentRepresentativePosition;
    private String prosecutionCaseId;
    private String prosecutionAuthorityId;
    private String prosecutionAuthorityCode;
    private String prosecutionAuthorityReference;

    public MockCourtApplication() {
        setupData();
    }

    private void setupData() {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        courtApplicationId = randomUUID().toString();
        particulars = STRING.next();
        applicantReceivedDate = now().toLocalDate().toString();
        applicationType = STRING.next();
        appeal = BOOLEAN.next();
        applicantAppellantFlag = BOOLEAN.next();
        paymentReference = STRING.next();
        applicantSynonym = STRING.next();
        applicantFirstName = STRING.next();
        applicantLastName = STRING.next();
        applicantNationality = STRING.next();
        applicantRemandStatus = STRING.next();
        applicantRepresentation = STRING.next();
        interpreterLanguageNeeds = STRING.next();
        applicantDoB = PAST_LOCAL_DATE.next();
        applicantAddress1 = STRING.next();
        applicantAddress2 = STRING.next();
        applicantAddress3 = STRING.next();
        applicantAddress4 = STRING.next();
        applicantAddress5 = STRING.next();
        applicantPostCode = POST_CODE.next();
        applicationReference = RandomStringUtils.randomAlphanumeric(4).toUpperCase() + RandomStringUtils.randomNumeric(7);
        respondentDefendantId = randomUUID().toString();
        respondentOrganisationName = STRING.next();
        respondentOrganisationAddress1 = STRING.next();
        respondentOrganisationAddress2 = STRING.next();
        respondentOrganisationAddress3 = STRING.next();
        respondentOrganisationAddress4 = STRING.next();
        respondentOrganisationAddress5 = STRING.next();
        respondentOrganisationPostcode = POST_CODE.next();
        respondentRepresentativeFirstName = STRING.next();
        respondentRepresentativeLastName = STRING.next();
        respondentRepresentativePosition = STRING.next();
        prosecutionCaseId = randomUUID().toString();
        prosecutionAuthorityId = randomUUID().toString();
        prosecutionAuthorityCode = STRING.next();
        prosecutionAuthorityReference = STRING.next();
    }

    public String getUserId() {
        return userId;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getDefendantId() {
        return defendantId;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }

    public String getCourtApplicationId() {
        return courtApplicationId;
    }

    public String getParticulars() {
        return particulars;
    }

    public String getApplicantReceivedDate() {
        return applicantReceivedDate;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public Boolean getAppeal() {
        return appeal;
    }

    public Boolean getApplicantAppellantFlag() {
        return applicantAppellantFlag;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getApplicantSynonym() {
        return applicantSynonym;
    }

    public String getApplicantFirstName() {
        return applicantFirstName;
    }

    public String getApplicantLastName() {
        return applicantLastName;
    }

    public String getApplicantNationality() {
        return applicantNationality;
    }

    public String getApplicantRemandStatus() {
        return applicantRemandStatus;
    }

    public String getApplicantRepresentation() {
        return applicantRepresentation;
    }

    public String getInterpreterLanguageNeeds() {
        return interpreterLanguageNeeds;
    }

    public LocalDate getApplicantDoB() {
        return applicantDoB;
    }

    public String getApplicantAddress1() {
        return applicantAddress1;
    }

    public String getApplicantAddress2() {
        return applicantAddress2;
    }

    public String getApplicantAddress3() {
        return applicantAddress3;
    }

    public String getApplicantAddress4() {
        return applicantAddress4;
    }

    public String getApplicantAddress5() {
        return applicantAddress5;
    }

    public String getApplicantPostCode() {
        return applicantPostCode;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public String getRespondentDefendantId() {
        return respondentDefendantId;
    }

    public String getRespondentOrganisationName() {
        return respondentOrganisationName;
    }

    public String getRespondentOrganisationAddress1() {
        return respondentOrganisationAddress1;
    }

    public String getRespondentOrganisationAddress2() {
        return respondentOrganisationAddress2;
    }

    public String getRespondentOrganisationAddress3() {
        return respondentOrganisationAddress3;
    }

    public String getRespondentOrganisationAddress4() {
        return respondentOrganisationAddress4;
    }

    public String getRespondentOrganisationAddress5() {
        return respondentOrganisationAddress5;
    }

    public String getRespondentOrganisationPostcode() {
        return respondentOrganisationPostcode;
    }

    public String getRespondentRepresentativeFirstName() {
        return respondentRepresentativeFirstName;
    }

    public String getRespondentRepresentativeLastName() {
        return respondentRepresentativeLastName;
    }

    public String getRespondentRepresentativePosition() {
        return respondentRepresentativePosition;
    }

    public String getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public String getProsecutionAuthorityId() {
        return prosecutionAuthorityId;
    }

    public String getProsecutionAuthorityCode() {
        return prosecutionAuthorityCode;
    }

    public String getProsecutionAuthorityReference() {
        return prosecutionAuthorityReference;
    }
}
