class NowContent {

    constructor() {
        this.orderName;
        this.welshOrderName = undefined;
        this.courtClerkName;
        this.orderDate;
        this.amendmentDate = undefined;
        this.orderAddressee;
        this.defendant = undefined;
        this.applicant = undefined;
        this.courtApplicationRespondent = undefined;
        this.financialOrderDetails = undefined;
        this.nextHearingCourtDetails = undefined;
        this.civilNow = undefined;
        this.cases = undefined;
        this.caseApplicationReferences = undefined;
        this.nowText = undefined;
        this.orderingCourt = undefined;
        this.nowRequirementText = undefined;
        this.sequence = undefined;
    }
}

class NowAddress {
    constructor() {
        this.line1 = undefined;
        this.line2 = undefined;
        this.line3 = undefined;
        this.line4 = undefined;
        this.line5 = undefined;
        this.postCode = undefined;
        this.emailAddress1 = undefined;
        this.emailAddress2 = undefined;
    }
}

class OrderAddressee {
    constructor() {
        this.name;
        this.address;
    }
}

class NowParty {
    constructor() {
        this.name;
        this.title = undefined;
        this.lastName = undefined;
        this.middleName = undefined;
        this.firstName = undefined;
        this.dateOfBirth = undefined;
        this.address;
    }
}

class NextHearingCourtDetails {
    constructor() {
        this.courtName;
        this.welshCourtName;
        this.hearingDate = undefined;
        this.hearingTime = undefined;
        this.hearingWeekCommencing = undefined;
        this.courtAddress = undefined;
        this.welshCourtAddress = undefined;
    }
}

class FinancialOrderDetails {
    constructor() {
        this.enforcementPhoneNumber;
        this.enforcementAddress;
        this.enforcementEmail;
        this.accountingDivisionCode;
        this.accountPaymentReference = undefined;
        this.bacsBankName;
        this.bacsSortCode;
        this.bacsAccountNumber;
        this.totalAmountImposed;
        this.totalBalance;
        this.paymentTerms = undefined;
    }
}

class NowDefendant {
    constructor() {
        this.name;
        this.dateOfBirth = undefined;
        this.address;
        this.pncId = undefined;
        this.nationality = undefined;
        this.prosecutingAuthorityReference = undefined;
        this.mobileNumber = undefined;
        this.landLineNumber = undefined;
        this.nationalInsuranceNumber = undefined;
        this.ethnicity = undefined;
        this.gender = undefined;
        this.driverNumber = undefined;
        this.defendantResults = undefined;
        this.solicitor = undefined;
        this.occupation = undefined;
        this.aliasNames;
        this.title = undefined;
        this.firstName = undefined;
        this.middleName = undefined;
        this.lastName = undefined;
        this.isCivil = undefined;
        this.selfDefinedEthnicity = undefined;
        this.interpreterLanguageNeeds = undefined;
        this.specialNeeds = undefined;
        this.isYouth = undefined;
    }
}

class NowSolicitor {
    constructor() {
        this.name;
        this.address = undefined;
    }
}

class Case {
    constructor() {
        this.reference;
        this.caseMarkers = undefined;
        this.defendantCaseResults = undefined;
        this.defendantCaseOffences;
        this.prosecutor = undefined;
        this.applicationType = undefined;
        this.applicationTypeWelsh = undefined;
        this.applicationLegislation = undefined;
        this.applicationLegislationWelsh = undefined;
        this.applicationReceivedDate = undefined;
        this.applicationParticulars = undefined;
        this.allegationOrComplaintStartDate = undefined;
        this.allegationOrComplaintEndDate = undefined;
        this.applicationCode = undefined;
    }
}

class Result {
    constructor() {
        this.label;
        this.welshLabel = undefined;
        this.resultIdentifier = undefined;
        this.nowRequirementText = undefined;
        this.publishedForNows = undefined;
        this.prompts = undefined;
        this.resultWording = undefined;
        this.welshResultWording = undefined;
        this.resultDefinitionGroup = undefined;
    }
}

class ResultPrompt {
    constructor() {
        this.label;
        this.welshLabel = undefined;
        this.value;
        this.welshValue = undefined;
        this.promptIdentifier = undefined;
        this.promptReference = undefined;
    }
}

class DefendantCaseOffence {
    constructor() {
        this.wording;
        this.welshWording;
        this.title = undefined;
        this.welshTitle = undefined;
        this.legislation = undefined;
        this.civilOffence = undefined;
        this.dvlaCode = undefined;
        this.startDate;
        this.endDate = undefined;
        this.convictionDate = undefined;
        this.convictionStatus = undefined;
        this.plea = undefined;
        this.vehicleRegistration = undefined;
        this.results;
        this.alcoholReadingAmount = undefined;
        this.alcoholReadingMethodCode = undefined;
        this.alcoholReadingMethodDescription = undefined;
        this.code = undefined;
        this.modeOfTrial = undefined;
        this.allocationDecision = undefined;
        this.verdictType = undefined;
    }
}

class NowText {
    constructor() {
        this.label;
        this.value;
        this.welshValue;
    }
}

class OrderCourt {
    constructor() {
        this.ljaCode = undefined;
        this.ljaName = undefined;
        this.welshLjaName = undefined;
        this.courtCentreName;
        this.welshCourtCentreName = undefined;
        this.address;
        this.welshAddress = undefined;
    }
}

class ParentGuardian {
    constructor() {
        this.name = undefined;
        this.address1 = undefined;
        this.address2 = undefined;
        this.address3 = undefined;
        this.address4 = undefined;
        this.address5 = undefined;
        this.postCode = undefined;
        this.emailAddress1 = undefined;
        this.emailAddress2 = undefined;
    }
}

module.exports = {
    NowContent,
    NowAddress,
    OrderAddressee,
    NowParty,
    NextHearingCourtDetails,
    FinancialOrderDetails,
    NowDefendant,
    NowSolicitor,
    Case,
    Result,
    ResultPrompt,
    DefendantCaseOffence,
    NowText,
    OrderCourt,
    ParentGuardian
};
