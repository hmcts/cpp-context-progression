const {Result} = require('../Model/HearingVenue');
const LEVEL_TYPE = require('../../../NowsHelper/service/LevelTypeEnum');

class ResultMapper {

    constructor(defendantContext) {
        this.defendantContext = defendantContext;
    }

    buildDefendantLevelResults() {
        const results = this.defendantContext.results
            .filter(r => r.level === LEVEL_TYPE.DEFENDANT)
            .map(r => new Result(r.judicialResult.cjsCode, r.judicialResult.resultText));

        if(results.length) {
            return results;
        }
    }

    buildDefendantCaseLevelResults(caseId) {
        const results = this.defendantContext.results
            .filter(r => r.level === LEVEL_TYPE.CASE && r.prosecutionCaseId === caseId)
            .map(r => new Result(r.judicialResult.cjsCode, r.judicialResult.resultText));

        if(results.length) {
            return results;
        }
    }

    buildOffenceLevelResults(offence) {
        const results = this.defendantContext.results
            .filter(r => r.level === LEVEL_TYPE.OFFENCE && r.offenceId === offence.id)
            .map(r => new Result(r.judicialResult.cjsCode, r.judicialResult.resultText));

        if(results.length) {
            return results;
        }
    }
}

module.exports = ResultMapper;