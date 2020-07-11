class InformantRegisterSubscription {
    constructor(prosecutionAuthorityId) {
        this.prosecutionAuthorityId = prosecutionAuthorityId;
        this.registerDefendants;
    }
}

class Hearing {
    constructor(){
        this.prosecutionCases;
        this.courtCentre = {
            roomName: ""
        };
    }
}

class HearingDay {
    constructor(sittingDay, startTime) {
        this.sittingDay = sittingDay;
        this.startTime = startTime;
    }
}

class CourtApplication {
    constructor(linkedCaseId, applicationReference) {
        this.linkedCaseId = linkedCaseId;
        this.applicationReference = applicationReference;
    }
}

class ProsecutionCase {
    constructor(prosecutionAuthorityId){
        this.prosecutionCaseIdentifier = {
            prosecutionAuthorityId: prosecutionAuthorityId,
            caseURN: 'caseURN'
        };
        this.id;
        this.defendants;
    }
}

class Defendant {
    constructor(offences){
        this.masterDefendantId;
        this.defendantCaseJudicialResults;
        this.personDefendant = {
            firstName: "",
            middleName: "",
            lastName: "",
            dateOfBirth: "",
            nationalityCode: "",
            arrestSummonsNumber: "",
            address: {}
        };
        this.offences = offences;
    }
}

class Offence {
    constructor(offenceCode, orderIndex, offenceTitle){
        this.id;
        this.offenceCode = offenceCode;
        this.orderIndex = orderIndex;
        this.offenceTitle = offenceTitle;
        this.plea = {};
        this.verdict = {};
    }
}

class Result {
    constructor(cjsCode, resultText){
        this.cjsCode = cjsCode;
        this.resultText = resultText;
    }
}
class DefendantContextBase {
    constructor(masterDefendantId) {
        this.results = [];
        this.masterDefendantId = masterDefendantId;
    }
}

module.exports = {InformantRegisterSubscription, Hearing, HearingDay, CourtApplication, ProsecutionCase, Defendant, Offence, Result, DefendantContextBase};