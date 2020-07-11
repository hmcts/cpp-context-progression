class YouthDefendant {
    constructor() {
        this.name = undefined;
        this.dateOfBirth = undefined;
        this.address = undefined;
        this.gender = undefined;
        this.nationality = undefined;
        this.postHearingCustodyStatus = undefined;
        this.masterDefendantId = undefined;
        this.parentGuardian = undefined;
        this.hearing = undefined;
        this.prosecutionCasesOrApplications = [];
        this.aliases = [];
        this.defenceCounsels = [];
        this.defendantResults = [];
        this.ethnicity = undefined;
    }
}

module.exports = YouthDefendant;
