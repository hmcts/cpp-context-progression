const {ProsecutionCaseOrApplication} = require('../Model/HearingVenue');
const OffenceMapper = require('./OffenceMapper');
const ResultMapper = require('./ResultMapper');

class ProsecutionCaseOrApplicationMapper {
    constructor(hearingJson, informantRegister, registerDefendant) {
        this.informantRegister = informantRegister;
        this.hearingJson = hearingJson;
        this.registerDefendant = registerDefendant;
    }

    build() {
        const prosecutionCaseOrApplications = [];

        if(this.registerDefendant.cases && this.registerDefendant.cases.length) {
            this.registerDefendant.cases.forEach(caseId => {
                const prosecutionCaseOrApplication = new ProsecutionCaseOrApplication();
                const prosecutionCase = this.hearingJson.prosecutionCases.find(pcase => pcase.id === caseId);
                if(prosecutionCase) {
                    prosecutionCaseOrApplication.caseOrApplicationReference =
                        prosecutionCase.prosecutionCaseIdentifier.caseURN ?
                        prosecutionCase.prosecutionCaseIdentifier.caseURN :
                        prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityReference;

                    prosecutionCaseOrApplication.results = this.getResultMapper(caseId);
                    prosecutionCaseOrApplication.offences = this.getOffenceMapper();
                    prosecutionCaseOrApplication.arrestSummonsNumber = this.getASN(prosecutionCase);
                    prosecutionCaseOrApplications.push(prosecutionCaseOrApplication);
                }
            });
        }

        if(prosecutionCaseOrApplications.length) {
            return prosecutionCaseOrApplications;
        }
    }

    getASN(prosecutionCase) {
        const asn = prosecutionCase.defendants
            .filter(d => d.masterDefendantId === this.registerDefendant.masterDefendantId)
            .filter(d => d.personDefendant && d.personDefendant.arrestSummonsNumber)
            .map(d => d.personDefendant.arrestSummonsNumber);

        if (asn && asn.length) {
            return asn[0];
        }
    }

    getResultMapper(caseId) {
        return new ResultMapper(this.registerDefendant).buildDefendantCaseLevelResults(caseId);
    }

    getOffenceMapper() {
        return new OffenceMapper(this.hearingJson, this.informantRegister, this.registerDefendant).build();
    }
}

module.exports = ProsecutionCaseOrApplicationMapper;