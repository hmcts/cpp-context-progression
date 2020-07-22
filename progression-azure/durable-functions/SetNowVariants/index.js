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
                            }
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
                + 'and has ' + nowVariant.results.length + ' results.');
        });

        return nowVariants;
    }

    enrichResultWithPrimaryFlag(result, primaryResultDefinitionIds) {
        result.nowId = [];
        this.nowMetadata.nows.forEach(nowDefinition => {
            const originalNowRequirements = nowDefinition.nowRequirements;
            const flattenNowRequirements = [];
            this.extractFlattenNowRequirements(originalNowRequirements, flattenNowRequirements);
            flattenNowRequirements.forEach(nowRequirement => {
                //check for results
                if (result.judicialResultTypeId === nowRequirement.resultDefinitionId) {
                    result.primary = nowRequirement.primary;
                    result.nowId.push(nowDefinition.id);
                    if (result.primary) {
                        primaryResultDefinitionIds.add(result.judicialResultTypeId);
                    }
                }

                //check for result prompts
                if (!result.primary && result.judicialResultPrompts) {
                    result.judicialResultPrompts.forEach(prompt => {
                        if (prompt.judicialResultPromptTypeId
                            === nowRequirement.resultDefinitionId) {
                            result.primary = nowRequirement.primary;
                            result.nowId.push(nowDefinition.id);
                            if (result.primary) {
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
                this.extractFlattenNowRequirements(nowRequirement.nowRequirements,
                                                   flattenNowRequirements);
            }
            flattenNowRequirements.push(nowRequirement);
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

        defendantContextBase.results.forEach((r) => {
            const result = r.judicialResult;
            if (result.nowId.includes(now.id)) {
                if (result.isFinancialResult && defendantContextBase.complianceCorrelationId) {
                    nowVariant.complianceCorrelationId =
                        defendantContextBase.complianceCorrelationId;
                }
                if (!now.includeAllResults) {
                    nowVariant.results.push(result);
                }
            }

            // Set the amendment date
            if (result.amendmentDate) {
                nowVariant.amendmentDate = result.amendmentDate;
            }
        });

        if (now.includeAllResults) {
            defendantContextBase.results.forEach((r) => {
                const result = r.judicialResult;
                nowVariant.results.push(result);
            });
        }

        nowVariant.orderDate = this.getOrderDate(defendantContextBase.results);

        return nowVariant;
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
