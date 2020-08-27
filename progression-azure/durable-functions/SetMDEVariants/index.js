const _ = require('lodash');

/** The idea here is to take the UserGroupsNowVariants
 * For each UserGroupsNowVariant, get all the MDE prompts.
 * For each MDE prompt, work out the MDE variants.
 */

let uniqueResultsByType = new Map();
let allResults = [];

class MDEVariants {

    constructor(userGroupsNowVariants) {
        this.userGroupsNowVariants = userGroupsNowVariants;
        this.cachedResultDefinitionWithPromptsMap = new Map();
    }

    buildMDEVariants() {

        const mdeVariants = [];

        this.userGroupsNowVariants.forEach((userGroupVariant) => {

            //Collecting MDE prompts for the given Now
            const resultDefinitionsWithPrompts = this.getResultDefinitionWithPrompts(
                userGroupVariant.now);

            if (resultDefinitionsWithPrompts.size === 0) {
                mdeVariants.push(userGroupVariant);
                return;
            }

            //Filter MDE Judicial results
            const mdeResults = this.getMdeResults(resultDefinitionsWithPrompts, userGroupVariant);
            console.log(userGroupVariant.now.name + ' has ' + mdeResults.length + ' MDE results.');

            // Check if we have any Judicial results to process
            if (mdeResults.length === 0) {
                mdeVariants.push(userGroupVariant);
                return;
            }

            //Filter non MDE Judicial results
            const nonMdeResults = this.getNonMdeResults(resultDefinitionsWithPrompts,
                                                        userGroupVariant);
            console.log(
                userGroupVariant.now.name + ' has ' + nonMdeResults.length + ' NON-MDE results.');

            // Group MDE Judicial results by JudicialResultTypeId
            const groupedJudicialResultsByType = this.groupByJudicialResultTypeId(mdeResults);
            groupedJudicialResultsByType.forEach(type => {
                console.log(type.judicialResultTypeId + ' has ' + type.judicialResults.length + ' '
                            + type.judicialResults[0].label + ' results.');
            });

            let numberOfTypes = [];

            //Iterate each JudicialResultType and sort out unique judicial results
            groupedJudicialResultsByType.forEach(groupedJudicialResultByType => {
                const uniqueJudicialResultsMap = this.groupJudicialResultsByMdePrompts(
                    resultDefinitionsWithPrompts, groupedJudicialResultByType);
                uniqueResultsByType.set(groupedJudicialResultByType.judicialResultTypeId,
                                        uniqueJudicialResultsMap);
                console.log(groupedJudicialResultByType.judicialResultTypeId + ' has '
                            + uniqueJudicialResultsMap.size + ' unique prompt value.');
                numberOfTypes.push(uniqueJudicialResultsMap.size);
            });

            const numberOfClones = this.getNumberOfClones(uniqueResultsByType);
            console.log('number Of Clones to be created ' + numberOfClones);

            this.generateUniqueResults(numberOfTypes, this.reshuffleResults);

            if (numberOfClones !== allResults.length) {
                console.log(
                    'Something went wrong !!!' + numberOfClones + ' : ' + allResults.length);
            }

            userGroupVariant.results = []; //reset userGroupVariants with empty results

            const finalJudicialResults = [];
            for (let clone = 0; clone < numberOfClones; clone++) {
                let groupResults = [].concat(...allResults[clone]);
                const resultsArray = this.divideResults(groupResults);
                resultsArray.forEach(results => {
                    const judicialResults = [...results];
                    const judicialResultsExist = this.isExistJudicialResultIds(finalJudicialResults,
                                                                               judicialResults);
                    if (!judicialResultsExist) {
                        finalJudicialResults.push(judicialResults);
                    }
                });
            }

            for (let clone = 0; clone < finalJudicialResults.length; clone++) {
                const clonedUserGroupVariant = _.cloneDeep(userGroupVariant);
                let results = [].concat(...finalJudicialResults[clone]);
                clonedUserGroupVariant.results = [...nonMdeResults, ...results];
                mdeVariants.push(clonedUserGroupVariant);
            }

            console.log(userGroupVariant.now.name + ' has ' + finalJudicialResults.length
                        + ' MDE variants.');

            this.reset();
        });

        return mdeVariants;
    }

    isExistJudicialResultIds(uniqueJudicialResultIdArray, judicialResultIds) {

        if (uniqueJudicialResultIdArray.length === 0) {
            return false;
        }

        let isExist = false;

        for (let judicialResultIdArray of uniqueJudicialResultIdArray) {

            const ids = this.getIds(judicialResultIdArray);

            if (ids.length !== judicialResultIds.length) {
                continue;
            }
            console.log('judicialResultIdArray === ' + JSON.stringify(ids));
            const exists = [];
            for (let judicialResultId of judicialResultIds) {
                console.log('judicialResultId === ' + JSON.stringify(judicialResultId.judicialResultId));
                exists.push(ids.indexOf(judicialResultId.judicialResultId) !== -1);
            }

            console.log(JSON.stringify(exists));
            const allEqual = arr => arr.every(v => v);

            if (allEqual(exists)) {
                isExist = true;
                break;
            }


        }
        return isExist;
    }

    divideResults(results) {

        const uniqueReferencesPerResults = this.groupByJudicialResultTypeIdByPromptReference(
            results);

        const typeIds = this.groupByTypes(uniqueReferencesPerResults);

        const dividedResults = [];

        for (let [key, value] of typeIds) {
            let resultArrays = [];
            value.forEach(v => {
                const r = results.filter(r => r.judicialResultTypeId === v);
                resultArrays.push(...r);
            });
            dividedResults.push(resultArrays);
        }

        return dividedResults;
    }

    groupByTypes(uniqueReferencesPerResults) {

        const typeIds = new Map();

        for (let [key, references] of uniqueReferencesPerResults) {

            const resultTypes = [];

            resultTypes.push(key);

            const referencesForOtherTypesMap = this.getUniqueReferencesForOtherTypes(key,
                                                                                     uniqueReferencesPerResults);

            for (let [typeId, promptReferences] of referencesForOtherTypesMap) {

                let found = false;

                for (let reference of references) {
                    if (promptReferences.indexOf(reference) !== -1) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    references = references.concat(...promptReferences);
                    resultTypes.push(typeId);
                }
            }

            if (typeIds.size > 0) {
                for (let [k, v] of typeIds) {
                    if (!this.arraysEqual(v, resultTypes)) {
                        typeIds.set(key, resultTypes);
                    }
                }
            } else {
                typeIds.set(key, resultTypes);
            }
        }

        return typeIds;
    }

    arraysEqual(arr1, arr2) {
        if (arr1.length !== arr2.length) {
            return false;
        }
        for (let i = arr1.length; i--;) {
            if (arr2.indexOf(arr1[i]) === -1) {
                return false;
            }
        }
        return true;
    }

    getUniqueReferencesForOtherTypes(TypeId, uniqueReferencesPerResults) {
        const otherTypesMap = new Map();
        for (let [key, values] of uniqueReferencesPerResults) {
            if (key !== TypeId) {
                const referencesArray = [];
                values.forEach(value => {
                    if (referencesArray.indexOf(value) === -1) {
                        referencesArray.push(value);
                    }
                });

                otherTypesMap.set(key, referencesArray);
            }
        }

        return otherTypesMap;
    }

    groupByJudicialResultTypeIdByPromptReference(results) {
        const groupedJudicialResultsByType = this.groupByJudicialResultTypeId(results);

        const uniqueReferencePerResults = new Map();

        //Iterate each JudicialResultType and sort out unique judicial results
        groupedJudicialResultsByType.forEach(groupedJudicialResultByType => {
            const uniquePromptReferences = this.getUniquePromptReference(
                groupedJudicialResultByType.judicialResults);
            uniqueReferencePerResults.set(groupedJudicialResultByType.judicialResultTypeId,
                                          uniquePromptReferences);
        });

        return uniqueReferencePerResults;
    }

    getUniquePromptReference(judicialResults) {
        const promptReferences = new Set();
        judicialResults.forEach(judicialResult => {
            if (judicialResult.judicialResultPrompts) {
                judicialResult.judicialResultPrompts.forEach(judicialResultPrompt => {
                    if (judicialResultPrompt.mde) {
                        promptReferences.add(judicialResultPrompt.promptReference);
                    }
                });
            }
        });

        return [...promptReferences];
    }

    getNumberOfClones(uniqueResultsByType) {
        let numberOfGroups = [];

        for (let judicialResultTypeId of uniqueResultsByType.keys()) {
            numberOfGroups.push(uniqueResultsByType.get(judicialResultTypeId).size);
        }

        return numberOfGroups.reduce((a, b) => a * b);
    }

    generateUniqueResults(maxIndices, func) { //generateUniqueResults
        this.doGenerateUniqueResults(maxIndices, func, [], 0);
    }

    doGenerateUniqueResults(maxIndices, func, args, index) {
        if (maxIndices.length === 0) {
            func(args);
        } else {
            let rest = maxIndices.slice(1);
            for (args[index] = 0; args[index] < maxIndices[0]; ++args[index]) {
                this.doGenerateUniqueResults(rest, func, args, index + 1);
            }
        }
    }

    /**
     * RDID-1   - key1 - [a] | RDID-2 - key4 - [d]  | RDID-3 - key6 - [f] |
     *          - key2 - [b] |        - key5 - [e]  |        - key7 - [g] |
     *          - key3 - [c] |
     */
    reshuffleResults(args) {
        const results = [];

        for (let param = 0; param < args.length; param++) {

            const index = args[param];

            let types = [...uniqueResultsByType.keys()];

            const uniquePrompt = uniqueResultsByType.get(types[param]);

            let keys = [...uniquePrompt.keys()];

            const result = uniquePrompt.get(keys[index]);

            results.push(result);

        }
        allResults.push(results);
    }

    groupJudicialResultsByMdePrompts(resultDefinitionsWithPrompts, groupedJudicialResultByType) {

        //Get Judicial Result's prompt values
        const judicialResultsByMdePromptsMap = this.getJudicialResultsByMdePrompts(
            resultDefinitionsWithPrompts, groupedJudicialResultByType);

        const judicialResultIdKeys = judicialResultsByMdePromptsMap.keys();

        const uniqueJudicialResultsMap = new Map();

        for (let judicialResultId of judicialResultIdKeys) {

            const promptValues = judicialResultsByMdePromptsMap.get(judicialResultId);

            const key = this.joinPromptValues(promptValues);
            console.log('prompt values : ' + key);

            if (uniqueJudicialResultsMap.has(key)) {
                const resultsArray = uniqueJudicialResultsMap.get(key);
                const judicialResult = groupedJudicialResultByType.judicialResults.find(
                    result => result.judicialResultId === judicialResultId);
                resultsArray.push(judicialResult);
            } else {
                const resultsArray = [];
                const judicialResult = groupedJudicialResultByType.judicialResults.find(
                    result => result.judicialResultId === judicialResultId);
                resultsArray.push(judicialResult);
                uniqueJudicialResultsMap.set(key, resultsArray);
            }
        }

        return uniqueJudicialResultsMap;
    }

    joinPromptValues(promptValues) {
        const promptValue = [];

        for (let promptKey of promptValues.keys()) {
            promptValue.push(promptValues.get(promptKey));
        }

        return promptValue.join('|');
    }

    getPromptValues(orderPromptIds, result) {
        const promptValues = new Map();

        if (result.judicialResultPrompts && result.judicialResultPrompts.length) {
            orderPromptIds.forEach(promptId => {
                result.judicialResultPrompts.forEach(prompt => {
                    if (promptId === prompt.judicialResultPromptTypeId) {
                        prompt.mde = true;
                        promptValues.set(prompt.judicialResultPromptTypeId, prompt.value);
                    }
                });

                //If the prompt is not available, add as prompt as 'undefined'
                if (!promptValues.has(promptId)) {
                    promptValues.set(promptId, 'undefined');
                }
            });
        }

        return promptValues;
    }

    /**
     * Define the order by which judicial results will be compared
     * Set objects are collections of values. You can iterate through the elements of a set in
     * insertion order.
     * @param resultDefinitionWithPrompts
     * @returns {Set<String>}
     */
    definePromptOrder(resultDefinitionWithPrompts) {
        const promptIds = new Set();

        for (let i = 0; i < resultDefinitionWithPrompts.length; i++) {
            promptIds.add(resultDefinitionWithPrompts[i].promptId);
        }

        return promptIds;
    }

    getMdeResults(resultDefinitionsWithPrompts, userGroupVariant) {
        const mdeResults = [];

        userGroupVariant.results.forEach(result => {
            if (resultDefinitionsWithPrompts.has(result.judicialResultTypeId)) {
                mdeResults.push(result);
            }
        });

        return mdeResults;
    }

    getNonMdeResults(resultDefinitionsWithPrompts, userGroupVariant) {
        const nonMdeResults = [];

        userGroupVariant.results.forEach(result => {
            let isIncluded = false;

            if (resultDefinitionsWithPrompts.has(result.judicialResultTypeId)) {
                isIncluded = true;
            }

            if (!isIncluded) {
                nonMdeResults.push(result);
            }
        });

        return nonMdeResults;
    }

    groupByJudicialResultTypeId(mdeJrs) {
        return _(mdeJrs)
            .groupBy(j => j.judicialResultTypeId)
            .map((value, key) => ({judicialResultTypeId: key, judicialResults: value}))
            .value();
    }

    getResultDefinitionWithPrompts(now) {

        if (this.cachedResultDefinitionWithPromptsMap.has(now.id)) {
            return this.cachedResultDefinitionWithPromptsMap.get(now.id);
        }

        const flattenNowRequirements = [];
        const resultDefinitionWithPromptsMap = new Map();

        this.extractFlattenNowRequirements(now.nowRequirements, flattenNowRequirements);

        flattenNowRequirements.forEach(nowRequirement => {
            if (nowRequirement.prompts) {
                const mdePrompts = nowRequirement.prompts.filter(prompt => prompt.variantData);
                if (mdePrompts.length) {
                    resultDefinitionWithPromptsMap.set(nowRequirement.resultDefinitionId,
                                                       mdePrompts);
                }
            }
        });

        this.cachedResultDefinitionWithPromptsMap.set(now.id, resultDefinitionWithPromptsMap);

        return resultDefinitionWithPromptsMap;
    }

    extractFlattenNowRequirements(nowRequirements, flattenNowRequirements) {
        nowRequirements.forEach(nowRequirement => {
            if (nowRequirement.nowRequirements) {
                this.extractFlattenNowRequirements(nowRequirement.nowRequirements,
                                                   flattenNowRequirements);
            }
            flattenNowRequirements.push(nowRequirement);
        });
    }

    getJudicialResultsByMdePrompts(resultDefinitionsWithPrompts, groupedJudicialResultByType) {

        const judicialResultTypeId = groupedJudicialResultByType.judicialResultTypeId;

        const judicialResults = groupedJudicialResultByType.judicialResults;

        const resultDefinitionWithPrompts = resultDefinitionsWithPrompts.get(judicialResultTypeId);

        const orderPromptIds = this.definePromptOrder(resultDefinitionWithPrompts);

        const judicialResultsByMdePromptsMap = new Map();

        for (let i = 0; i < judicialResults.length; i++) {

            const currentResult = judicialResults[i];

            const promptValues = this.getPromptValues(orderPromptIds, currentResult);

            judicialResultsByMdePromptsMap.set(currentResult.judicialResultId, promptValues);
        }
        return judicialResultsByMdePromptsMap;
    }

    reset() {
        uniqueResultsByType = new Map();
        allResults = [];
    }

    getIds(judicialResultIdArray) {
        const ids = [];
        judicialResultIdArray.forEach(idArray => {
            ids.push(idArray.judicialResultId);
        });
        return ids;
    }
}

module.exports = async (context) => {
    const userGroupsNowVariants = context.bindings.params.userGroupsNowVariants;
    return await new MDEVariants(userGroupsNowVariants).buildMDEVariants();
};