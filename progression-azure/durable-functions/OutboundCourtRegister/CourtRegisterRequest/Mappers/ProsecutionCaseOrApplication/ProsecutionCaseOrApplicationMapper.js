const ProsecutionCaseOrApplication = require('../../Models/ProsecutionCaseOrApplication');
const OffenceMapper = require('../Offence/OffenceMapper');
const ResultMapper = require('../Result/ResultMapper');
const CounselMapper = require('../Counsel/CounselMapper');
const LEVEL_TYPE = require('../../../../NowsHelper/service/LevelTypeEnum');
const _ = require('lodash');

class ProsecutionCaseOrApplicationMapper {
    constructor(context, registerDefendant, hearingJson) {
        this.context = context;
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

        this.registerDefendant.cases.forEach(caseId => {
            const caseJudicialResults = this.registerDefendant.results
                .filter(r => r.level === LEVEL_TYPE.CASE && r.prosecutionCaseId === caseId)
                .map(r => r.judicialResult);

            const prosecutionCase = this.hearingJson.prosecutionCases.find(pcase => pcase.id === caseId);

            const prosecutionCaseInfo = new ProsecutionCaseOrApplication();
            prosecutionCaseInfo.caseOrApplicationReference = this.getCaseReference(prosecutionCase);
            prosecutionCaseInfo.results = this.getResultMapper(caseJudicialResults);
            prosecutionCaseInfo.offences = this.getOffenceMapper(this.getProsecutionCaseOffences(prosecutionCase));
            prosecutionCaseInfo.prosecutionCounsels = this.getCounselMapper(this.getProsecutionCaseCounsels(prosecutionCase));
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

        this.registerDefendant.applications.forEach(applicationId => {
            const courtApplication = this.hearingJson.courtApplications.find((ca) => ca.id === applicationId);

            // Check if defendant
            if (courtApplication.applicant.defendant && courtApplication.applicant.defendant.masterDefendantId === this.registerDefendant.masterDefendantId) {

                // Get results at the application level for current application
                const applicationJudicialResults = this.registerDefendant.results
                    .filter(r => r.level === LEVEL_TYPE.APPLICATION && r.applicationId === applicationId)
                    .map(r => r.judicialResult);

                const applicantCounsels = this.getApplicantCounsels(courtApplication.applicant.id);

                const courtApplicationInfo = new ProsecutionCaseOrApplication();
                courtApplicationInfo.caseOrApplicationReference = this.getApplicationReference(courtApplication);
                courtApplicationInfo.results = this.getResultMapper(applicationJudicialResults);
                courtApplicationInfo.prosecutionCounsels = this.getCounselMapper(applicantCounsels);
                courtApplicationInfo.applicationType = courtApplication.type.applicationType;
                if (courtApplication.applicationOutcome) {
                    courtApplicationInfo.applicationDecision = courtApplication.applicationOutcome.applicationOutcomeDate.applicationOutcomeType.description;
                    courtApplicationInfo.applicationDecisionDate = courtApplication.applicationOutcome.applicationOutcomeDate;
                }
                // Dont these are applicable here as defendant is applicant not a respondent
                courtApplicationInfo.applicationResponse = '';
                courtApplicationInfo.applicationResponseDate = '';
                applications.push(courtApplicationInfo);
            }

            // Check if defendant respondent
            if (courtApplication.respondents) {
                courtApplication.respondents.forEach(respondant => {
                    if (respondant.partyDetails.defendant &&
                        respondant.partyDetails.defendant.masterDefendantId === this.registerDefendant.masterDefendantId) {

                        const applicationJudicialResults = this.registerDefendant.results
                            .filter(r => r.level === LEVEL_TYPE.APPLICATION && r.applicationId === applicationId)
                            .map(r => r.judicialResult);

                        const courtApplicationInfo = new ProsecutionCaseOrApplication();
                        courtApplicationInfo.caseOrApplicationReference = this.getApplicationReference(courtApplication);
                        courtApplicationInfo.results = this.getResultMapper(applicationJudicialResults);
                        courtApplicationInfo.prosecutionCounsels = this.getCounselMapper(this.getRespondentCounsels(respondant.id));
                        courtApplicationInfo.applicationType = courtApplication.type.applicationType;
                        courtApplicationInfo.applicationDecision = this.getApplicationDecision(courtApplication);
                        courtApplicationInfo.applicationDecisionDate = courtApplication.applicationOutcome.applicationOutcomeDate;
                        courtApplicationInfo.applicationResponse = respondant.applicationResponse.applicationResponseType.description;
                        courtApplicationInfo.applicationResponseDate = respondant.applicationResponse.applicationResponseDate;
                        applications.push(courtApplicationInfo);
                    }
                });
            }
        });

        return applications;
    }

    getCaseReference(prosecutionCase) {
        return prosecutionCase.prosecutionCaseIdentifier.caseURN ||
               prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityReference;
    }

    getApplicationReference(courtApplication) {
        return courtApplication.applicationReference;
    }

    getApplicationDecision(courtApplication) {
        return courtApplication.applicationOutcome ? courtApplication.applicationOutcome.applicationOutcomeType.description : undefined;
    }

    getOffenceMapper(offences) {
        return new OffenceMapper(this.context, offences, this.registerDefendant).build();
    }

    getResultMapper(results) {
        return new ResultMapper(results).build();
    }

    getCounselMapper(counsels) {
        return new CounselMapper(counsels).build();
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