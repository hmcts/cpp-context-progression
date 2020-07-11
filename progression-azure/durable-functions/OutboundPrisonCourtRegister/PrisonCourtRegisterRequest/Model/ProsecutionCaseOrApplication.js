class ProsecutionCaseOrApplication {
    constructor() {
        this.caseOrApplicationReference = undefined;
        this.prosecutorName = undefined;
        this.applicationType = undefined;
        this.applicationDecision = undefined;
        this.applicationDecisionDate = undefined;
        this.applicationResponse = undefined;
        this.applicationResponseDate = undefined;
        this.offences = [];
        this.prosecutionCounsels = [];
        this.results = [];
    }
}

module.exports = ProsecutionCaseOrApplication;