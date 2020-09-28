const ReferenceDataService = require('../NowsHelper/service/ReferenceDataService');
const DefendantContextBaseService = require('../NowsHelper/service/DefendantContextBaseService');
const dateService = require('../NowsHelper/service/DateService');
const VocabularyService = require('../NowsHelper/service/VocabularyService');
const NowVariant = require('./NowVariant');
const _ = require('lodash');

class SetNowVariants {
    constructor(nowMetadata, hearingJson, defendantContextBaseList, context) {
        this.nowMetadata = nowMetadata;
        this.hearingJson = hearingJson;
        this.defendantContextBaseList = defendantContextBaseList;
        this.context = context;
    }

    async buildNowVariants() {
        const nowVariants = [];

        if(this.nowMetadata && this.nowMetadata.length) {
            this.context.log.warn('No NOWs metadata found.');
            return nowVariants;
        }

        if (this.defendantContextBaseList.length === 0) {
            this.context.log.warn('No Master Defendant found');
            return nowVariants;
        }

        this.defendantContextBaseList.forEach(defendantContextBase => {
            const nows = [];
            const nowIds = new Set();
            const primaryResultDefinitionIds = new Set();
            defendantContextBase.results.forEach(result => {
                this.enrichResultWithPrimaryFlag(result.judicialResult, primaryResultDefinitionIds);
            });

            this.context.log.warn('Master Defendant Id : ' + defendantContextBase.masterDefendantId
                                  + ' primary result definitions  are : '
                                  + [...primaryResultDefinitionIds]);

            primaryResultDefinitionIds.forEach(resultId => {
                this.nowMetadata.nows.forEach(nowDefinition => {
                    const originalNowRequirements = nowDefinition.nowRequirements;
                    const flattenNowRequirements = [];
                    this.extractFlattenNowRequirements(originalNowRequirements, flattenNowRequirements);
                    flattenNowRequirements.forEach(nowRequirement => {
                        if ((resultId === nowRequirement.resultDefinitionId) && nowRequirement.primary) {
                            if (!nowIds.has(nowDefinition.id)) {
                                nowIds.add(nowDefinition.id);
                                nows.push(nowDefinition);
                                nowDefinition.primaryResultDefinitionIds = [];
                            }
                            nowDefinition.primaryResultDefinitionIds.push(resultId);
                        }
                    });
                });
            });

            nows.forEach((now) => {
                const nowVariant = this.buildNowVariant(now, defendantContextBase);
                nowVariants.push(nowVariant);
            });
        });

        this.context.log.warn('=== List of NOWS to be generate are ==== ');
        nowVariants.forEach(nowVariant => {
            this.context.log.warn(
                ' (' + nowVariant.now.id + ') ' + JSON.stringify(nowVariant.now.name) + ' is a '
                + ((nowVariant.complianceCorrelationId !== undefined) ? 'financial nows '
                                                                      : 'non-financial nows ')
                + 'and has ' + nowVariant.results.length + ' results.' + ((nowVariant.now.includeAllResults) ? 'Incldues all results.' : ''));
        });
        return nowVariants;
    }

    enrichResultWithPrimaryFlag(result, primaryResultDefinitionIds) {
        this.nowMetadata.nows.forEach(nowDefinition => {
            const originalNowRequirements = nowDefinition.nowRequirements;
            const flattenNowRequirements = [];
            this.extractFlattenNowRequirements(originalNowRequirements, flattenNowRequirements);
            flattenNowRequirements.forEach(nowRequirement => {
                //check for results
                if (result.judicialResultTypeId === nowRequirement.resultDefinitionId && nowRequirement.primary && nowRequirement.rootResultDefinitionId === undefined) {
                    result.primary = nowRequirement.primary;
                    if (result.primary) {
                        result.rootResultDefinitionId = result.judicialResultTypeId;
                        primaryResultDefinitionIds.add(result.judicialResultTypeId);
                    }
                }

                //check for result prompts
                if (!result.primary && result.judicialResultPrompts) {
                    result.judicialResultPrompts.forEach(prompt => {
                        if (prompt.judicialResultPromptTypeId === nowRequirement.resultDefinitionId && nowRequirement.primary && nowRequirement.rootResultDefinitionId === undefined) {
                            result.primary = nowRequirement.primary;
                            if (result.primary) {
                                result.rootResultDefinitionId = prompt.judicialResultPromptTypeId;
                                primaryResultDefinitionIds.add(prompt.judicialResultPromptTypeId);
                            }
                        }
                    });
                }
            });
        });
    }

    extractFlattenNowRequirements(nowRequirements, flattenNowRequirements) {
        nowRequirements.forEach(nowRequirement => {
            if (nowRequirement.nowRequirements) {
                this.extractFlattenNowRequirements(nowRequirement.nowRequirements, flattenNowRequirements);
            }
            const requirement = _.cloneDeep(nowRequirement);
            flattenNowRequirements.push(requirement);
        });
    }

    flattenNowRequirements(nowRequirements, flattenNowRequirements) {
        nowRequirements.forEach(nowRequirement => {
            if (nowRequirement.nowRequirements) {
                this.extractFlattenNowRequirements(nowRequirement.nowRequirements, flattenNowRequirements);
            }
            const requirement = _.cloneDeep(nowRequirement);
            flattenNowRequirements.push(requirement);
        });

        flattenNowRequirements.forEach(flattenNowRequirement => {
            if(flattenNowRequirement.nowRequirements) {
                flattenNowRequirement.nowRequirements = undefined;
            }
        });
    }

    buildNowVariant(now, defendantContextBase) {
        // create NowVariant
        const nowVariant = new NowVariant();
        nowVariant.now = _.cloneDeep(now);

        // Set the master defendant id when relevant
        nowVariant.masterDefendantId = defendantContextBase.masterDefendantId;
        nowVariant.registerDefendant = defendantContextBase;

        // Set the vocabulary
        const vocabularyService = new VocabularyService(this.hearingJson, defendantContextBase);
        nowVariant.vocabulary = vocabularyService.getVocabularyInfo();
        nowVariant.orderDate = this.getOrderDate(defendantContextBase.results);

        if (now.includeAllResults) {
            this.includeAllResults(nowVariant, defendantContextBase);
        } else {
            this.filterResults(nowVariant, defendantContextBase);
        }

        // Set Amendment date
        // Set Sequence
        // Set Compliance Correlation Id
        nowVariant.results.forEach(result => {
            if (result.amendmentDate) {
                nowVariant.amendmentDate = result.amendmentDate;
            }

            if (result.isFinancialResult && defendantContextBase.complianceCorrelationId) {
                nowVariant.complianceCorrelationId = defendantContextBase.complianceCorrelationId;
            }

            const originalNowRequirements = nowVariant.now.nowRequirements;
            const flattenNowRequirements = [];
            this.extractFlattenNowRequirements(originalNowRequirements, flattenNowRequirements);

            flattenNowRequirements.forEach(nowRequirement => {
                if (result.judicialResultTypeId === nowRequirement.resultDefinitionId) {
                    result.sequence = nowRequirement.sequence;
                }
            });
        });

        return nowVariant;
    }

    filterResults(nowVariant, defendantContextBase) {
        const originalNowRequirements = nowVariant.now.nowRequirements;
        const flattenNowRequirements = [];
        const judicialResultIds = [];
        this.flattenNowRequirements(originalNowRequirements, flattenNowRequirements);
        const primaryResultDefinitionIds = nowVariant.now.primaryResultDefinitionIds;
        defendantContextBase.results.forEach((r) => {

            const result = _.cloneDeep(r.judicialResult);

            flattenNowRequirements.forEach(nowRequirement => {

                if(result.judicialResultTypeId === nowRequirement.resultDefinitionId) {
                    if (nowRequirement.primary && nowRequirement.rootResultDefinitionId === undefined) {
                        if ((judicialResultIds.indexOf(result.judicialResultId)) === -1) {
                            result.excludeFromMDE = nowRequirement.excludeFromMDE;
                            judicialResultIds.push(result.judicialResultId);
                            nowVariant.results.push(result);
                        }
                    }

                    if (!nowRequirement.primary && primaryResultDefinitionIds.indexOf(nowRequirement.rootResultDefinitionId) !== -1 && primaryResultDefinitionIds.indexOf(result.rootJudicialResultTypeId) !== -1) {
                        if ((judicialResultIds.indexOf(result.judicialResultId)) === -1) {
                            result.excludeFromMDE = nowRequirement.excludeFromMDE;
                            judicialResultIds.push(result.judicialResultId);
                            nowVariant.results.push(result);
                        }
                    }

                    if (!nowRequirement.primary && nowRequirement.rootResultDefinitionId === undefined) {
                        if ((judicialResultIds.indexOf(result.judicialResultId)) === -1) {
                            result.excludeFromMDE = nowRequirement.excludeFromMDE;
                            judicialResultIds.push(result.judicialResultId);
                            nowVariant.results.push(result);
                        }
                    }
                }

                //TODO:
                /*if (result.isFinancialResult && defendantContextBase.complianceCorrelationId) {
                    nowVariant.complianceCorrelationId = defendantContextBase.complianceCorrelationId;
                }*/

                /*if (result.judicialResultTypeId === nowRequirement.resultDefinitionId) {
                    if (nowRequirement.primary && nowRequirement.rootResultDefinitionId === undefined) {
                        result.excludeFromMDE = nowRequirement.excludeFromMDE;
                        if ((judicialResultIds.indexOf(result.judicialResultId)) === -1) {
                            judicialResultIds.push(result.judicialResultId);
                            nowVariant.results.push(result);
                        }
                    }*/

                /*if (!nowRequirement.primary && primaryResultDefinitionIds.indexOf(nowRequirement.rootResultDefinitionId) !== -1
                    && primaryResultDefinitionIds.indexOf(result.rootJudicialResultTypeId) !== -1) {
                    result.excludeFromMDE = nowRequirement.excludeFromMDE;
                    if ((judicialResultIds.indexOf(result.judicialResultId)) === -1) {
                        judicialResultIds.push(result.judicialResultId);
                        nowVariant.results.push(result);
                    }
                }*/
                // }
            });
        });
    }

    getNowRequirements(defendantContextBase, flattenNowRequirements) {
        let requirements = [];
        const requirementIds = [];
        defendantContextBase.results.forEach((r) => {
            const result = r.judicialResult;
            const nowRequirements = flattenNowRequirements.filter(flattenNowRequirement =>
                                                                      flattenNowRequirement.resultDefinitionId
                                                                      === result.judicialResultTypeId
                                                                      && flattenNowRequirement.rootResultDefinitionId
                                                                      === undefined);
            nowRequirements.forEach(nowRequirement => {
                if (requirementIds.indexOf(nowRequirement.id) === -1) {
                    requirements = requirements.concat(nowRequirements);
                    requirementIds.push(nowRequirement.id);
                }
            });
        });
        const nowRequirements = [];
        this.flattenNowRequirements(requirements, nowRequirements);
        return nowRequirements;
    }

    includeAllResults(nowVariant, defendantContextBase) {
        defendantContextBase.results.forEach((r) => {
            const result = _.cloneDeep(r.judicialResult);
            nowVariant.results.push(result);
        });
    }

    getOrderDate(results) {
        try {
            const datesToSort = new Set();

            results.forEach(result => {
                datesToSort.add(result.judicialResult.orderedDate);
            });

            const sortedDates = [...datesToSort].sort((a, b) => {
                return dateService.parse(b).getTime() - dateService.parse(a).getTime();
            });

            return sortedDates[0];
        } catch (ex) {
            this.context.log('Could not parse date', ex);
        }
    }
}

module.exports = async (context) => {

    const hearingJson = context.bindings.params.hearingResultedJson;

    const complianceEnforcementList = context.bindings.params.complianceEnforcements;

    const defendantContextBaseList = new DefendantContextBaseService(
        hearingJson).getDefendantContextBaseList();

    let resultWithOrderedDate;

    defendantContextBaseList.forEach(defendantContextBase => {
        const complianceEnforcement = complianceEnforcementList.find(
            enforcement => enforcement.masterDefendantId
                           === defendantContextBase.masterDefendantId);
        if (complianceEnforcement) {
            defendantContextBase.complianceCorrelationId =
                complianceEnforcement.complianceCorrelationId;
        }

        resultWithOrderedDate =
            defendantContextBase.results.find(result => result.judicialResult.orderedDate);
    });
    const orderedDate = resultWithOrderedDate ? resultWithOrderedDate.judicialResult.orderedDate
                                              : undefined;
    console.time('getNowMetadata');
    const nowMetadata = await new ReferenceDataService().getNowMetadata(context, orderedDate);
    console.timeEnd('getNowMetadata');
    return await new SetNowVariants(nowMetadata, hearingJson, defendantContextBaseList,
                                    context).buildNowVariants();
};