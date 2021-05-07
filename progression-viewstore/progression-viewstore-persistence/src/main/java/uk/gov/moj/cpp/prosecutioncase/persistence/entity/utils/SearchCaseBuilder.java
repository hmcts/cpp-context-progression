package uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.defaultString;

import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;

import java.util.UUID;

import javax.json.Json;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings({"squid:S00107", "squid:S1845", "PMD.BeanMembersShouldSerialize"})
public class SearchCaseBuilder {

    private static final String DELIMITER = " | ";
    private static final String CASE_ID = "caseId";
    private static final String REFERENCE = "reference";
    private static final String DEFENDANT_NAME = "defendantName";
    private static final String DOB = "dob";
    private static final String STATUS = "status";
    private static final String PROSECUTOR = "prosecutor";
    private static final String CPSPROSECUTOR = "cpsProsecutor";
    private static final String IS_STANDALONE_APPLICATION = "isStandaloneApplication";

    private UUID defendantId;

    private String caseId;

    private String reference;

    private String defendantFirstName;

    private String defendantMiddleName;

    private String defendantLastName;

    private String defendantDob;

    private String prosecutor;

    private String cpsProsecutor;

    private String status;

    private String searchTarget;

    private String resultPayload;

    private String defendantFullName;

    private Boolean isStandaloneApplication;

    public SearchCaseBuilder(final UUID defendantId, final String caseId, final String reference, final String defendantFirstName,
                             final String defendantMiddleName, final String defendantLastName, final String defendantFullName,
                             final String defendantDob, final String prosecutor, final String cpsProsecutor, final String status, final String searchTarget,
                             final String resultPayload, final Boolean isStandaloneApplication) {

        this.defendantId = defendantId;
        this.caseId = caseId;
        this.reference = reference;
        this.defendantFirstName = defendantFirstName;
        this.defendantMiddleName = defendantMiddleName;
        this.defendantLastName = defendantLastName;
        this.defendantFullName = defendantFullName;
        this.defendantDob = defendantDob;
        this.prosecutor = prosecutor;
        this.cpsProsecutor = cpsProsecutor;
        this.status = status;
        this.searchTarget = searchTarget;
        this.resultPayload = resultPayload;
        this.isStandaloneApplication = isStandaloneApplication;

    }

    public static CaseBuilder searchCaseBuilder() {
        return new SearchCaseBuilder.CaseBuilder();
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(final String caseId) {
        this.caseId = caseId;
    }

    public String getReference() {
        return reference;
    }

    public UUID getDefendantId() {
        return defendantId;
    }
    
    public String getDefendantFirstName() {
        return defendantFirstName;
    }

    public String getDefendantMiddleName() {
        return defendantMiddleName;
    }

    public String getDefendantLastName() {
        return defendantLastName;
    }

    public String getDefendantDob() {
        return defendantDob;
    }

    public String getProsecutor() {
        return prosecutor;
    }

    public String getCpsProsecutor() {
        return cpsProsecutor;
    }

    public String getStatus() {
        return status;
    }

    public String getSearchTarget() {
        return searchTarget;
    }

    public String getResultPayload() {
        return resultPayload;
    }

    public String getDefendantFullName() {
        return defendantFullName;
    }

    public Boolean getStandaloneApplication() {
        return isStandaloneApplication;
    }

    public static class CaseBuilder {

        private UUID defendantId;

        private String caseId;

        private String reference;

        private String defendantFirstName;

        private String defendantMiddleName;

        private String defendantLastName;

        private String defendantDob;

        private String prosecutor;

        private String cpsProsecutor;

        private String status;

        private String searchTarget;

        private String resultPayload;

        private String defendantFullName;

        private Boolean isStandaloneApplication;

        public SearchCaseBuilder.CaseBuilder withDefendantId(final UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public SearchCaseBuilder.CaseBuilder withCaseId(final String caseId) {
            this.caseId = caseId;
            return this;
        }

        public SearchCaseBuilder.CaseBuilder withProsecutionCaseIdentifier(final ProsecutionCaseIdentifier caseIdentifier) {
            reference = StringUtils.isNotEmpty(caseIdentifier.getProsecutionAuthorityReference())
                    ? caseIdentifier.getProsecutionAuthorityReference() : caseIdentifier.getCaseURN();
            status = StringUtils.isNotEmpty(caseIdentifier.getCaseURN()) ? CaseStatusEnum.READY_FOR_REVIEW.toString() : CaseStatusEnum.SJP_REFERRAL.toString();
            prosecutor = caseIdentifier.getProsecutionAuthorityCode();
            return this;
        }

        public SearchCaseBuilder.CaseBuilder withCpsProsecutor(final Prosecutor prosecutor) {
            if(nonNull(prosecutor)){
                cpsProsecutor = prosecutor.getProsecutorCode();
            }
            return this;
        }

        public SearchCaseBuilder.CaseBuilder withPersonDefendant(final PersonDefendant personDefendant) {
            if (nonNull(personDefendant)) {
                final Person person = personDefendant.getPersonDetails();
                defendantFirstName = defaultString(person.getFirstName());
                defendantMiddleName = defaultString(person.getMiddleName());
                defendantLastName = person.getLastName();
                if (person.getDateOfBirth() != null) {
                    defendantDob = person.getDateOfBirth().toString();
                }
            }
            return this;
        }

        public SearchCaseBuilder.CaseBuilder withLegalEntityDefendant(final LegalEntityDefendant legalEntityDefendant) {
            if (nonNull(legalEntityDefendant)) {
                final Organisation organisation = legalEntityDefendant.getOrganisation();
                defendantFirstName = defaultString(organisation.getName());
            }
            return this;
        }

        public SearchCaseBuilder.CaseBuilder withSearchCaseEntity(final SearchProsecutionCaseEntity searchCaseEntity) {
            this.defendantId = searchCaseEntity.getDefendantId();
            this.caseId = searchCaseEntity.getCaseId();
            this.reference = searchCaseEntity.getReference();
            this.defendantFirstName = searchCaseEntity.getDefendantFirstName();
            this.defendantMiddleName = searchCaseEntity.getDefendantMiddleName();
            this.defendantLastName = searchCaseEntity.getDefendantLastName();
            this.defendantDob = searchCaseEntity.getDefendantDob();
            this.prosecutor = searchCaseEntity.getProsecutor();
            this.cpsProsecutor = searchCaseEntity.getCpsProsecutor();
            this.status = searchCaseEntity.getStatus();
            this.isStandaloneApplication = searchCaseEntity.getStandaloneApplication();
            return this;
        }

        public SearchCaseBuilder.CaseBuilder withSearchTarget() {
            final StringBuilder searchTargetStringBuilder = new StringBuilder();
            searchTargetStringBuilder.append(this.reference);
            if (StringUtils.isNotBlank(defendantFullName)) {
                searchTargetStringBuilder.append(DELIMITER)
                        .append(defendantFullName);
            }
            if (StringUtils.isNotBlank(defendantDob)) {
                searchTargetStringBuilder.append(DELIMITER)
                        .append(defendantDob);
            }
            this.searchTarget = searchTargetStringBuilder.toString();
            return this;
        }

        public SearchCaseBuilder.CaseBuilder withSearchTargetForApplication() {
            this.searchTarget = new StringBuilder().append(this.reference).append(DELIMITER)
                    .append(defendantFullName).append(DELIMITER)
                    .append(defaultString(prosecutor)).toString();
            return this;
        }

        public SearchCaseBuilder.CaseBuilder withDefendantFullName() {
            this.defendantFullName = new StringBuilder()
                    .append(defaultString(defendantFirstName)).append(" ")
                    .append(defaultString(defendantMiddleName)).append(" ")
                    .append(defaultString(defendantLastName)).toString();
            return this;
        }

        public SearchCaseBuilder.CaseBuilder withResultPayload() {
            resultPayload = Json.createObjectBuilder()
                    .add(CASE_ID, this.caseId)
                    .add(REFERENCE, this.reference)
                    .add(DEFENDANT_NAME, defendantFullName)
                    .add(DOB, defaultString(this.defendantDob))
                    .add(STATUS, defaultString(this.status))
                    .add(PROSECUTOR, defaultString(this.prosecutor))
                    .add(CPSPROSECUTOR, defaultString(this.cpsProsecutor))
                    .add(IS_STANDALONE_APPLICATION, this.isStandaloneApplication)
                    .build().toString();
            return this;
        }

        public SearchCaseBuilder build() {
            return new SearchCaseBuilder(defendantId, caseId, reference, defendantFirstName, defendantMiddleName, defendantLastName, defendantFullName, defendantDob, prosecutor, cpsProsecutor, status, searchTarget, resultPayload, isStandaloneApplication);
        }
    }

}
