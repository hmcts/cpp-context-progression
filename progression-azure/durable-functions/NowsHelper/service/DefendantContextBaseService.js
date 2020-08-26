const LEVEL_TYPE = require('./LevelTypeEnum');
const dateService = require('../service/DateService');

class DefendantContextBase {
    constructor() {
        this.defendantIds = [];
        this.results = [];
        this.cases = [];
        this.applications = [];
        this.masterDefendantId = undefined;
        this.isYouthDefendant = false;
        this.orderedDate = undefined;
    }
}

class Result {
    constructor() {
        this.prosecutionCaseId = undefined;
        this.defendantId = undefined;
        this.offenceId = undefined;
        this.applicationId = undefined;
        this.level = undefined;
        this.masterDefendantId = undefined;
        this.judicialResult = undefined;
    }
}

class DefendantContextService {

    constructor(hearingResultedJson) {
        this.hearingResultedJson = hearingResultedJson;
    }

    getDefendantContextBaseList() {

        const defendantMap = new Map();

        this.setJudicialResultsAtDefendantAndOffenceLevel(defendantMap);

        this.setJudicialResultsAtCourtApplicationLevel(defendantMap);

        this.setJudicialResultsAtDefendantCaseLevel(defendantMap);

        const defendantContextBaseList = [];

        Array.from(defendantMap.values()).forEach(defendantContextBase => {
            if(defendantContextBase.masterDefendantId) {
                defendantContextBase.orderedDate = this.getOrderedDate(defendantContextBase);
                defendantContextBaseList.push(defendantContextBase);
            }
        });

        return defendantContextBaseList;
    }

    setJudicialResultsAtDefendantAndOffenceLevel(defendantMap) {

        if (this.hearingResultedJson.prosecutionCases) {

            this.hearingResultedJson.prosecutionCases.forEach(prosecutionCase => {

                prosecutionCase.defendants.forEach((defendant) => {

                    const defendantBase = defendantMap.get(defendant.masterDefendantId)
                                          || new DefendantContextBase();

                    defendantBase.cases.push(prosecutionCase.id);

                    defendantBase.defendantIds.push(defendant.id);

                    if (defendant.defendantCaseJudicialResults || defendant.judicialResults) {
                        const defendantCaseJudicialResults = [];
                        if (defendant.defendantCaseJudicialResults) {
                            defendant.defendantCaseJudicialResults.forEach(
                                defendantCaseJudicialResult => {
                                    if(!defendantCaseJudicialResult.isDeleted) {
                                        defendantCaseJudicialResult.level = LEVEL_TYPE.CASE;
                                        const result = new Result();
                                        result.prosecutionCaseId = prosecutionCase.id;
                                        result.defendantId = defendant.id;
                                        result.level = LEVEL_TYPE.CASE;
                                        result.judicialResult = defendantCaseJudicialResult;
                                        result.masterDefendantId = defendant.masterDefendantId;
                                        defendantCaseJudicialResults.push(result);
                                    }
                                });
                        }

                        if(defendant.judicialResults) {
                            defendant.judicialResults.forEach(
                                judicialResult => {
                                    if(!judicialResult.isDeleted) {
                                        judicialResult.level = LEVEL_TYPE.CASE;
                                        const result = new Result();
                                        result.prosecutionCaseId = prosecutionCase.id;
                                        result.defendantId = defendant.id;
                                        result.level = LEVEL_TYPE.CASE;
                                        result.judicialResult = judicialResult;
                                        result.masterDefendantId = defendant.masterDefendantId;
                                        defendantCaseJudicialResults.push(result);
                                    }
                                });
                        }
                        defendantBase.results = defendantBase.results.concat(defendantCaseJudicialResults);
                    }

                    const offenceJudicialResults = [];
                    defendant.offences.forEach(offence => {
                        if (offence.judicialResults) {
                            offence.judicialResults.forEach(judicialResult => {
                                if(!judicialResult.isDeleted) {
                                    judicialResult.level = LEVEL_TYPE.OFFENCE;
                                    const result = new Result();
                                    result.prosecutionCaseId = prosecutionCase.id;
                                    result.defendantId = defendant.id;
                                    result.offenceId = offence.id;
                                    result.level = LEVEL_TYPE.OFFENCE;
                                    result.judicialResult = judicialResult;
                                    result.masterDefendantId = defendant.masterDefendantId;
                                    offenceJudicialResults.push(result);
                                }
                            });
                        }
                    });

                    defendantBase.results = defendantBase.results.concat(offenceJudicialResults);

                    if (!defendantBase.masterDefendantId) {
                        defendantBase.masterDefendantId = defendant.masterDefendantId;
                        defendantBase.isYouthDefendant = defendant.isYouth;
                    }

                    defendantMap.set(defendant.masterDefendantId, defendantBase);
                });

            });
        }
    }

    setJudicialResultsAtCourtApplicationLevel(defendantMap) {

        if (this.hearingResultedJson.courtApplications) {

            this.hearingResultedJson.courtApplications.forEach(courtApplication => {

                if (courtApplication.judicialResults && courtApplication.judicialResults.length) {

                    if (courtApplication.applicant.defendant) {

                        const defendant = courtApplication.applicant.defendant;

                        const defendantBase = defendantMap.get(defendant.masterDefendantId);

                        if (defendantBase) {

                            defendantBase.applications.push(courtApplication.id);

                            const applicationJudicialResults = [];

                            courtApplication.judicialResults.forEach(judicialResult => {
                                if(!judicialResult.isDeleted) {
                                    judicialResult.level = LEVEL_TYPE.APPLICATION;
                                    const result = new Result();
                                    result.applicationId = courtApplication.id;
                                    result.level = LEVEL_TYPE.APPLICATION;
                                    result.judicialResult = judicialResult;
                                    result.isApplicant = true;
                                    result.masterDefendantId = defendantBase.masterDefendantId;
                                    applicationJudicialResults.push(result);
                                }
                            });

                            defendantBase.results = defendantBase.results.concat(applicationJudicialResults);
                        }
                    }

                    if (courtApplication.respondents) {

                        courtApplication.respondents.forEach(respondent => {

                            if (respondent.partyDetails.defendant) {

                                const defendant = respondent.partyDetails.defendant;

                                const defendantBase = defendantMap.get(defendant.masterDefendantId);

                                if (defendantBase) {

                                    defendantBase.applications.push(courtApplication.id);

                                    const applicationJudicialResults = [];

                                    courtApplication.judicialResults.forEach(judicialResult => {
                                        if(!judicialResult.isDeleted) {
                                            judicialResult.level = LEVEL_TYPE.APPLICATION;
                                            const result = new Result();
                                            result.applicationId = courtApplication.id;
                                            result.level = LEVEL_TYPE.APPLICATION;
                                            result.judicialResult = judicialResult;
                                            result.isApplicant = false;
                                            result.masterDefendantId = defendantBase.masterDefendantId;
                                            applicationJudicialResults.push(result);
                                        }
                                    });

                                    defendantBase.results = defendantBase.results.concat(applicationJudicialResults);
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    setJudicialResultsAtDefendantCaseLevel(defendantMap) {

        if (this.hearingResultedJson.defendantJudicialResults) {

            this.hearingResultedJson.defendantJudicialResults.forEach((defendantJudicialResult) => {

                const defendantJudicialResults = [];

                const defendantBase = defendantMap.get(defendantJudicialResult.masterDefendantId);
                if(!defendantJudicialResult.judicialResult.isDeleted) {
                    defendantJudicialResult.judicialResult.level = LEVEL_TYPE.DEFENDANT;
                    const result = new Result();
                    result.level = LEVEL_TYPE.DEFENDANT;
                    result.judicialResult = defendantJudicialResult.judicialResult;
                    result.masterDefendantId = defendantJudicialResult.masterDefendantId;
                    defendantJudicialResults.push(result);
                }

                defendantBase.results = defendantBase.results.concat(defendantJudicialResults);
            });
        }
    }

    getOrderedDate(defendantContextBase) {
        return defendantContextBase.results.filter((result) => result).map((result)=> result.judicialResult.orderedDate).sort((a, b) => {
            return dateService.parse(b).getTime() - dateService.parse(a).getTime();
        })[0];
    }
}

module.exports = DefendantContextService;
