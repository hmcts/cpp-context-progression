
class NowsHelper {

    constructor() {
    }

    static groupResultsByMasterDefendantId(hearingResultedJson) {
        let resultByMasterDefendantId;
        hearingResultedJson.prosecutionCases.forEach(prosecutionCase => {
            resultByMasterDefendantId = prosecutionCase.defendants.reduce((acc, defendant) => {
                acc[defendant.masterDefendantId] = acc[defendant.masterDefendantId] || [];
                defendant.offences.forEach(offence => {
                    offence.judicialResults.forEach((judicialResult) => {
                        acc[defendant.masterDefendantId].push(judicialResult);
                    });
                });
                return acc;
            }, {});
        });
        return resultByMasterDefendantId;
    }
}

module.exports = NowsHelper;