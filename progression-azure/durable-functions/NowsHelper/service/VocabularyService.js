const PromptTypesConstant = require('../constants/PromptTypesConstant');

const LOCATION_TYPE = require('./LocationTypeEnum');

class VocabularyInfo {
    constructor(builder) {
        this.custodyLocationIsPolice = builder.custodyLocationIsPolice;
        this.custodyLocationIsPrison = builder.custodyLocationIsPrison;
        this.atleastOneCustodialResult = builder.atleastOneCustodialResult;
        this.allNonCustodialResults = builder.allNonCustodialResults;
        this.atleastOneNonCustodialResult = builder.atleastOneNonCustodialResult;
        this.appearedInPerson = builder.appearedInPerson;
        this.appearedByVideoLink = builder.appearedByVideoLink;
        this.anyAppearance = builder.anyAppearance;
        this.inCustody = builder.inCustody;
        this.youthDefendant = builder.youthDefendant;
        this.adultDefendant = builder.adultDefendant;
        this.adultOrYouthDefendant = builder.adultOrYouthDefendant;
        this.welshCourtHearing = builder.welshCourtHearing;
        this.englishCourtHearing = builder.englishCourtHearing;
        this.anyCourtHearing = builder.anyCourtHearing;
    }

    static Builder() {
        class Builder {
            constructor() {
            }

            withCustodyLocationIsPolice(custodyLocationIsPolice) {
                this.custodyLocationIsPolice = custodyLocationIsPolice;
                return this;
            }

            withAtleastOneCustodialResult(atleastOneCustodialResult) {
                this.atleastOneCustodialResult = atleastOneCustodialResult;
                return this;
            }

            withAppearedInPerson(appearedInPerson) {
                this.appearedInPerson = appearedInPerson;
                return this;
            }

            withAppearedByVideoLink(appearedByVideoLink) {
                this.appearedByVideoLink = appearedByVideoLink;
                return this;
            }

            withAllNonCustodialResults(allNonCustodialResults) {
                this.allNonCustodialResults = allNonCustodialResults;
                return this;
            }

            withAtleastOneNonCustodialResult(atleastOneNonCustodialResult) {
                this.atleastOneNonCustodialResult = atleastOneNonCustodialResult;
                return this;
            }

            withCustodyLocationIsPrison(custodyLocationIsPrison) {
                this.custodyLocationIsPrison = custodyLocationIsPrison;
                return this;
            }

            withAnyAppearance(anyAppearance) {
                this.anyAppearance = anyAppearance;
                return this;
            }

            withInCustody(inCustody) {
                this.inCustody = inCustody;
                return this;
            }

            withYouthDefendant(youthDefendant) {
                this.youthDefendant = youthDefendant;
                return this;
            }

            withAdultDefendant(adultDefendant) {
                this.adultDefendant = adultDefendant;
                return this;
            }

            withAdultOrYouthDefendant(adultOrYouthDefendant) {
                this.adultOrYouthDefendant = adultOrYouthDefendant;
                return this;
            }

            withWelshCourtHearing(welshCourtHearing) {
                this.welshCourtHearing = welshCourtHearing;
                return this;
            }

            withEnglishCourtHearing(englishCourtHearing) {
                this.englishCourtHearing = englishCourtHearing;
                return this;
            }

            withAnyCourtHearing(anyCourtHearing) {
                this.anyCourtHearing = anyCourtHearing;
                return this;
            }

            build() {
                return new VocabularyInfo(this);
            }
        }

        return new Builder();
    }
}

class VocabularyService {

    constructor(hearingResultedObj, defendantContextBase) {
        this.hearingResultedObj = hearingResultedObj;
        this.defendantContextBase = defendantContextBase;
    }

    getVocabularyInfo() {

        const locationInfo = this.getCustodyLocationInfo();

        const atleastOneCustodialResult = this.getHasAtleastOneCustodialResult();

        const allNonCustodialResults = !atleastOneCustodialResult;

        let atleastOneNonCustodialResult = allNonCustodialResults;

        if (!atleastOneNonCustodialResult) {
            atleastOneNonCustodialResult = this.getHasAtleastOneNonCustodialResult();
        }

        const attendanceInfo = this.getAttendanceInfo();

        const anyAppearance = attendanceInfo.appearedByVideoLink || attendanceInfo.appearedInPerson;
        const youthDefendant = !!this.defendantContextBase.isYouthDefendant;
        const adultDefendant = !youthDefendant;
        const adultOrYouthDefendant = youthDefendant || adultDefendant;
        const welshCourtHearing = !!this.hearingResultedObj.courtCentre.welshCourtCentre;
        const englishCourtHearing = !welshCourtHearing;
        const anyCourtHearing = welshCourtHearing || englishCourtHearing;
        const inCustody = locationInfo.custodyLocationIsPrison || locationInfo.custodyLocationIsPolice;
        return VocabularyInfo.Builder()
            .withAppearedByVideoLink(attendanceInfo.appearedByVideoLink)
            .withAppearedInPerson(attendanceInfo.appearedInPerson)
            .withAnyAppearance(anyAppearance)
            .withWelshCourtHearing(welshCourtHearing)
            .withEnglishCourtHearing(englishCourtHearing)
            .withAnyCourtHearing(anyCourtHearing)
            .withYouthDefendant(youthDefendant)
            .withAdultDefendant(adultDefendant)
            .withAdultOrYouthDefendant(adultOrYouthDefendant)
            .withInCustody(inCustody)
            .withCustodyLocationIsPolice(locationInfo.custodyLocationIsPolice)
            .withCustodyLocationIsPrison(locationInfo.custodyLocationIsPrison)
            .withAllNonCustodialResults(allNonCustodialResults)
            .withAtleastOneNonCustodialResult(atleastOneNonCustodialResult)
            .withAtleastOneCustodialResult(atleastOneCustodialResult)
            .build();
    }

    getCustodyLocationInfo() {
        let custodyLocationIsPolice = false;
        let custodyLocationIsPrison = false;

        this.hearingResultedObj.prosecutionCases.forEach(prosecutionCase => {
            prosecutionCase.defendants.forEach(defendant => {
                if (defendant &&
                    defendant.masterDefendantId === this.defendantContextBase.masterDefendantId &&
                    defendant.personDefendant &&
                    defendant.personDefendant.custodialEstablishment) {

                    switch (defendant.personDefendant.custodialEstablishment.custody) {
                        case LOCATION_TYPE.POLICE_STATION:
                            custodyLocationIsPolice = true;
                            break;
                        case LOCATION_TYPE.PRISON:
                            custodyLocationIsPrison = true;
                            break;
                    }
                }
            });
        });

        return {
            custodyLocationIsPolice,
            custodyLocationIsPrison
        }
    }

    getAttendanceInfo() {
        let appearedByVideoLink = false;
        let appearedInPerson = false;

        if (this.hearingResultedObj.defendantAttendance) {
            this.hearingResultedObj.defendantAttendance.forEach(defendantAttendance => {
                if (!this.defendantContextBase.defendantIds.includes(defendantAttendance.defendantId)) {
                    return;
                }
                defendantAttendance.attendanceDays.forEach(attendanceDay => {

                    const hasMatchedAttendanceDay = this.defendantContextBase.results.some(result => result.judicialResult.orderedDate === attendanceDay.day);

                    if (hasMatchedAttendanceDay && attendanceDay.attendanceType === 'IN_PERSON') {
                        appearedInPerson = true;
                    }

                    if (hasMatchedAttendanceDay && attendanceDay.attendanceType === 'BY_VIDEO') {
                        appearedByVideoLink = true;
                    }
                });
            });
        }

        return {
            appearedByVideoLink,
            appearedInPerson
        };

    }

    getHasAtleastOneCustodialResult() {
        let atleastOneCustodialResult = false;

        custodialResult:
            for (let result of this.defendantContextBase.results) {
                if (result.judicialResult.judicialResultPrompts) {
                    for (let prompt of result.judicialResult.judicialResultPrompts) {
                        if (prompt.promptReference &&
                            prompt.promptReference === PromptTypesConstant.PRISON) {
                            atleastOneCustodialResult = true;
                            break custodialResult;
                        }
                    }
                }
            }

        return atleastOneCustodialResult;
    }

    getHasAtleastOneNonCustodialResult() {
        let atleastOneNonCustodialResult = false;

        nonCustodial:
            for (let result of this.defendantContextBase.results) {
                if (result.judicialResult.judicialResultPrompts) {
                    for (let prompt of result.judicialResult.judicialResultPrompts) {
                        if (prompt.promptReference &&
                            prompt.promptReference !== PromptTypesConstant.PRISON) {
                            atleastOneNonCustodialResult = true;
                            break nonCustodial;
                        }
                    }
                }
            }

        return atleastOneNonCustodialResult;
    }

}

module.exports = VocabularyService;