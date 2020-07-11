const Result = require('../../Models/Result');

class ResultMapper {
    constructor(judicialResults) {
        this.judicialResults = judicialResults;
    }

    build() {
        if (this.judicialResults && this.judicialResults.length) {
            return this.judicialResults.map(judicialResult => {
                const result = new Result();
                result.resultText = judicialResult.resultText;
                result.cjsResultCode = judicialResult.cjsCode;
                return result;
            });
        }
    }
}

module.exports = ResultMapper;
