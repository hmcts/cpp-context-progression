class HearingVenue {
    constructor() {
        this.ljaName;
        this.courtHouse;
        this.courtSessions;
    }
}

class CourtSession {
    constructor() {
        this.courtRoom;
        this.hearingStartTime;
        this.defendants;
    }
}

class Defendant {
    constructor() {
        this.name;
        this.dateOfBirth;
        this.address1;
        this.address2;
        this.address3;
        this.address4;
        this.address5;
        this.postCode;
        this.nationality;
        this.title;
        this.firstName;
        this.lastName;
        this.prosecutionCasesOrApplications;
        this.results;
    }
}

class ProsecutionCaseOrApplication {
    constructor() {
        this.caseOrApplicationReference = undefined;
        this.arrestSummonsNumber;
        this.offences = [];
        this.results;
    }
}

class Offence {
    constructor() {
        this.offenceCode;
        this.orderIndex;
        this.offenceTitle;
        this.pleaValue;
        this.verdictCode;
        this.offenceResults;
    }
}

class Result {
    constructor(cjsResultCode, resultText) {
        this.cjsResultCode = cjsResultCode;
        this.resultText = resultText;
    }
}

module.exports = {HearingVenue, CourtSession, Defendant, ProsecutionCaseOrApplication, Offence, Result};
