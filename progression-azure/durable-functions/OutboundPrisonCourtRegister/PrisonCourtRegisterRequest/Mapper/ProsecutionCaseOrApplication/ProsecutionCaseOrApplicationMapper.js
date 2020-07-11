const ProsecutionCaseOrApplication = require('../../Model/ProsecutionCaseOrApplication');
const OffenceMapper = require('../Offence/OffenceMapper');
const ResultMapper = require('../Result/ResultMapper');
const CounselMapper = require('../Counsel/CounselMapper');
const LEVEL_TYPE = require('../../../../NowsHelper/service/LevelTypeEnum');
const _ = require('lodash');

class ProsecutionCaseOrApplicationMapper {

    constructor(registerDefendant, hearingJson) {
        this.registerDefendant = registerDefendant;
        this.hearingJson = hearingJson;
    }

    build() {
        const associatedProsecutionCases = this.getAssociatedProsecutionCases();
        const associatedCourtApplications = this.getAssociatedCourtApplications();
        return associatedProsecutionCases.concat(associatedCourtApplications);
    }

    getAssociatedProsecutionCases() {
        const prosecutionCases = [];
        const caseList = Array.from(this.registerDefendant.cases.values());

        caseList.forEach(caseId => {
            const prosecutionCase = this.hearingJson.prosecutionCases.find(pcase => pcase.id === caseId);
            const prosecutionCaseInfo = new ProsecutionCaseOrApplication();
            prosecutionCaseInfo.prosecutorName = prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityName;
            prosecutionCaseInfo.caseOrApplicationReference =
                prosecutionCase.prosecutionCaseIdentifier.caseURN ||
                prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityReference;

            // Get all judicial result at the case level for the current case
            const caseJudicialResults = this.registerDefendant.results.filter(
                r => r.level === LEVEL_TYPE.CASE &&
                     r.prosecutionCaseId === caseId).map(r => r.judicialResult);

            prosecutionCaseInfo.results = this.getResultMapper(caseJudicialResults).build();

            prosecutionCaseInfo.offences =
                this.getOffenceMapper(this.getProsecutionCaseOffences(prosecutionCase)).build();

            prosecutionCaseInfo.prosecutionCounsels =
                this.getCounselMapper(this.getProsecutionCaseCounsels(prosecutionCase)).build();
            prosecutionCaseInfo.arrestSummonsNumber = this.getASN(prosecutionCase);
            prosecutionCases.push(prosecutionCaseInfo);
        });

        return prosecutionCases;
    }

    getASN(prosecutionCase) {
        const asn = prosecutionCase.defendants
            .filter(d => d.masterDefendantId === this.registerDefendant.masterDefendantId)
            .filter(d => d.personDefendant.arrestSummonsNumber)
            .map(d => d.personDefendant.arrestSummonsNumber);

        if (asn && asn.length) {
            return asn[0];
        }
    }

    getAssociatedCourtApplications() {

        const applications = [];

        const applicationIds = Array.from(this.registerDefendant.applications.values());

        applicationIds.forEach(applicationId => {

            const courtApplication = this.hearingJson.courtApplications.find((ca) => ca.id === applicationId);

            const applicantInfo = this.getCourtApplicationInfoByApplicant(courtApplication);

            const respondentInfo = this.getCourtApplicationInfoByRespondents(courtApplication);

            if(applicantInfo) {
                applications.push(applicantInfo);
            }

            if(respondentInfo) {
                applications.push(respondentInfo);
            }
        });

        return applications;
    }

    getCourtApplicationInfoByRespondents(courtApplication) {
        const respondents = this.getDefendantFromRespondents(courtApplication);

        if(respondents) {
            respondents.forEach(respondent => {
                const courtApplicationInfo = new ProsecutionCaseOrApplication();
                courtApplicationInfo.caseOrApplicationReference = courtApplication.applicationReference;
                courtApplicationInfo.results = this.getResultMapper(this.getApplicationResults(courtApplication.id)).build();
                courtApplicationInfo.counsels = this.getCounselMapper(this.getRespondentCounsels(respondent.id)).build();
                courtApplicationInfo.applicationType = courtApplication.type.applicationType;
                courtApplicationInfo.applicationDecision = this.getApplicationDecision(courtApplication);
                courtApplicationInfo.applicationDecisionDate = this.getApplicationDecisionDate(courtApplication);
                courtApplicationInfo.applicationResponse = this.getApplicationResponse(respondent);
                courtApplicationInfo.applicationResponseDate = this.getApplicationResponseDate(respondent);
                courtApplicationInfo.offences = undefined;
                courtApplicationInfo.prosecutionCounsels = undefined;
                return courtApplicationInfo;
            });
        }
    }

    getCourtApplicationInfoByApplicant(courtApplication) {

        const applicant = this.getDefendantFromApplicant(courtApplication);

        if (applicant) {
            const courtApplicationInfo = new ProsecutionCaseOrApplication();
            courtApplicationInfo.caseOrApplicationReference = courtApplication.applicationReference;
            courtApplicationInfo.counsels = this.getCounselMapper(this.getApplicantCounsels(courtApplication.applicant.id)).build();
            courtApplicationInfo.results = this.getResultMapper(this.getApplicationResults(courtApplication.id)).build();
            courtApplicationInfo.applicationType = courtApplication.type.applicationType;
            courtApplicationInfo.applicationDecision = this.getApplicationDecision(courtApplication);
            courtApplicationInfo.applicationDecisionDate = this.getApplicationDecisionDate(courtApplication);
            courtApplicationInfo.offences = undefined;
            courtApplicationInfo.prosecutionCounsels = undefined;
            return courtApplicationInfo;
        }
    }

    getApplicationResults(applicationId) {
        return this.registerDefendant.results
            .filter(r => r.level === LEVEL_TYPE.APPLICATION && r.applicationId === applicationId)
            .map(r => r.judicialResult);
    }

    getApplicationResponse(respondant) {
        if(respondant.applicationResponse) {
            return respondant.applicationResponse.applicationResponseType.description;
        }
    }

    getApplicationResponseDate(respondant) {
        if(respondant.applicationResponse) {
            return respondant.applicationResponse.applicationResponseDate;
        }
    }

    getDefendantFromRespondents(courtApplication) {
        if (courtApplication.respondents) {
            return courtApplication.respondents.filter(respondant => respondant.partyDetails.defendant &&
                                                                     respondant.partyDetails.defendant.masterDefendantId === this.registerDefendant.masterDefendantId);
        }
    }

    getDefendantFromApplicant(courtApplication) {
        return courtApplication.applicant.defendant &&
               courtApplication.applicant.defendant.masterDefendantId === this.registerDefendant.masterDefendantId;
    }

    getApplicationDecisionDate(courtApplication) {
        return courtApplication.applicationOutcome ? courtApplication.applicationOutcome.applicationOutcomeDate : undefined;
    }

    getApplicationDecision(courtApplication) {
        return courtApplication.applicationOutcome ? courtApplication.applicationOutcome.applicationOutcomeType.description : undefined;
    }

    getOffenceMapper(offences) {
        return new OffenceMapper(offences, this.registerDefendant);
    }

    getResultMapper(results) {
        return new ResultMapper(results);
    }

    getCounselMapper(counsels) {
        return new CounselMapper(counsels);
    }

    getProsecutionCaseOffences(prosecutionCase) {
        return _(prosecutionCase.defendants)
            .filter(d => d.masterDefendantId === this.registerDefendant.masterDefendantId)
            .flatMap('offences').value();
    }

    getProsecutionCaseCounsels(prosecutionCase) {
        const counsels = [];
        if (this.hearingJson.prosecutionCounsels && this.hearingJson.prosecutionCounsels.length) {
            this.hearingJson.prosecutionCounsels.forEach(prosecutionCounsel => {
                if (prosecutionCounsel.prosecutionCases.includes(prosecutionCase.id)) {
                    counsels.push(prosecutionCounsel);
                }
            });
        }

        return counsels;
    }

    getApplicantCounsels(applicantId) {
        const counsels = [];
        if (this.hearingJson.applicantCounsels && this.hearingJson.applicantCounsels.length) {
            this.hearingJson.applicantCounsels.forEach(applicantCounsel => {
                if (applicantCounsel.applicants.includes(applicantId)) {
                    counsels.push(applicantCounsel);
                }
            });
        }
        return counsels;
    }

    getRespondentCounsels(respondantId) {
        const counsels = [];
        if (this.hearingJson.respondentCounsels && this.hearingJson.respondentCounsels.length) {
            this.hearingJson.respondentCounsels.forEach(respondentCounsel => {
                if (respondentCounsel.respondents.includes(respondantId)) {
                    counsels.push(respondentCounsel);
                }
            });
        }
        return counsels;
    }
}

module.exports = ProsecutionCaseOrApplicationMapper;