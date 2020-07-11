const ResultDefinitions = require('../../../NowsHelper/constants/ResultDefinitionConstants');
const _ = require('lodash');

class Mapper {

    constructor(complianceEnforcement, hearingJson) {
        this.complianceEnforcement = complianceEnforcement;
        this.hearingJson = hearingJson;
    }

    getDefendant() {
        return _(this.hearingJson.prosecutionCases).flatMap('defendants').value()
            .find(defendant => defendant.masterDefendantId
                                 === this.complianceEnforcement.masterDefendantId); //find the first defendant
    }

    collectPromptsFromJudicialResults(arrayOfjudicialResults) {
        return arrayOfjudicialResults && arrayOfjudicialResults.reduce((acc, judicialResult) => {
            acc = acc || [];
            judicialResult.judicialResultPrompts && judicialResult.judicialResultPrompts.forEach(
                resultPrompt => {
                    acc.push(resultPrompt);
                })
            return acc;
        }, []);
    }

    getPromptValueByReference(allPromptsFromJudicialResults, promptReference) {
        const matchingPromptReference = allPromptsFromJudicialResults && allPromptsFromJudicialResults.find(
            resultPrompt => resultPrompt.promptReference === promptReference
        );
        return matchingPromptReference && matchingPromptReference.value;
    }
}

module.exports = Mapper;