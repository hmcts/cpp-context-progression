const Mapper = require('../Mapper');
const Employer = require('../../Model/Employer');
const PromptType = require('../../../PromptType');

class EmployerMapper extends Mapper {

    constructor(complianceEnforcement, hearingJson) {
        super(complianceEnforcement, hearingJson);
        this.complianceEnforcement = complianceEnforcement;
        this.hearingJson = hearingJson;
    }

    buildEmployer() {
        if(this.complianceEnforcement.employerResults){
            const employer = new Employer();
            const allPromptsFromJudicialResults = this.collectPromptsFromJudicialResults(
                this.complianceEnforcement.employerResults);
            employer.employerReference =
                this.getPromptValueByReference(allPromptsFromJudicialResults,
                                               PromptType.EMPLOYER_ORGANISATION_REFERENCE_NUMBER_PROMPT_REFERENCE);
            employer.employerCompanyName =
                this.getPromptValueByReference(allPromptsFromJudicialResults,
                                               PromptType.EMPLOYER_ORGANISATION_NAME_PROMPT_REFERENCE);
            employer.employerAddress1 =
                this.getPromptValueByReference(allPromptsFromJudicialResults,
                                               PromptType.EMPLOYER_ORGANISATION_ADDRESS1_PROMPT_REFERENCE);
            employer.employerAddress2 =
                this.getPromptValueByReference(allPromptsFromJudicialResults,
                                               PromptType.EMPLOYER_ORGANISATION_ADDRESS2_PROMPT_REFERENCE);
            employer.employerAddress3 =
                this.getPromptValueByReference(allPromptsFromJudicialResults,
                                               PromptType.EMPLOYER_ORGANISATION_ADDRESS3_PROMPT_REFERENCE);
            employer.employerAddress4 =
                this.getPromptValueByReference(allPromptsFromJudicialResults,
                                               PromptType.EMPLOYER_ORGANISATION_ADDRESS4_PROMPT_REFERENCE);
            employer.employerAddress5 =
                this.getPromptValueByReference(allPromptsFromJudicialResults,
                                               PromptType.EMPLOYER_ORGANISATION_ADDRESS5_PROMPT_REFERENCE);
            employer.employerPostcode =
                this.getPromptValueByReference(allPromptsFromJudicialResults,
                                               PromptType.EMPLOYER_ORGANISATION_POST_CODE_PROMPT_REFERENCE);
            return employer;
        }
        return undefined;
    }
}

module.exports = EmployerMapper;