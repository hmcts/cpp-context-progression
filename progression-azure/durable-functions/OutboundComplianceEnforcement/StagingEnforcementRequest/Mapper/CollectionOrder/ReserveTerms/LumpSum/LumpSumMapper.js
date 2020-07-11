const Mapper = require('../../../Mapper');
const ReserveTermsType = require('../../ReserveTerms/ReserveTermsType');
const {LumpSum} = require('../../../../Model/CollectionOrder');
const PromptType = require('../../../../../PromptType');

const FINANCIAL_TOTAL_AMOUNT_IMPOSED_PROMPT_REFERENCES = [PromptType.AMOUNT_OF_SURCHARGE,
                                                          PromptType.AMOUNT_OF_FINE,
                                                          PromptType.AMOUNT_OF_COMPENSATION,
                                                          PromptType.AMOUNT_OF_BACK_DUTY,
                                                          PromptType.COSTS_TO_CROWN_PROSECUTION_SERVICE_AMOUNT_PROMPT_REFERENCE,
                                                          PromptType.AMOUNT_OF_COSTS];

class LumpSumMapper extends Mapper {

    constructor(complianceEnforcement, hearingJson) {
        super(complianceEnforcement, hearingJson);
        this.complianceEnforcement = complianceEnforcement;
        this.hearingJson = hearingJson;
    }

    buildLumpSum(reserveTermType) {
        if (reserveTermType !== ReserveTermsType.INSTALMENTS_ONLY) {
            const allPromptsFromJudicialResults = this.collectPromptsFromJudicialResults(this.complianceEnforcement.reserveTermsResults);
            const lumpSum = new LumpSum();
            const reserveTermsAmount = this.getReserveTermsAmount(allPromptsFromJudicialResults, reserveTermType);
            lumpSum.amount = reserveTermsAmount.outstandingBalance;
            lumpSum.amountImposed = reserveTermsAmount.total;
            // lumpSum.withinDays = null //Not sure yet.
            return lumpSum;
        }
    }

    getReserveTermsAmount(allPromptsFromJudicialResults, reserveTermType) {
        let outstandingBalance = 0;
        let total = 0;
        if (reserveTermType === ReserveTermsType.LUMP_SUM_PLUS_INSTALMENTS) {
            outstandingBalance = this.getPromptValue(allPromptsFromJudicialResults, PromptType.LUMP_SUM_AMOUNT)
        } else if (reserveTermType === ReserveTermsType.LUMP_SUM) {
            total = allPromptsFromJudicialResults.filter(prompt => FINANCIAL_TOTAL_AMOUNT_IMPOSED_PROMPT_REFERENCES.includes(prompt.promptReference))
                .map(prompt => prompt.value).reduce(0, (acc, value) => acc + value);
            const alreadyPaid = allPromptsFromJudicialResults.find(prompt => PromptType.TOTAL_AMOUNT_ENFORCED_PROMPT_REFERENCE === prompt.promptReference)
                .map(prompt => prompt ? prompt.value : 0);
            outstandingBalance = parseInt(total) - parseInt(alreadyPaid);
        }
        return {total, outstandingBalance};
    }

    getPromptValue(allPromptsFromJudicialResults, promptType) {
        for (const resultPrompt of allPromptsFromJudicialResults) {
            if (resultPrompt.promptReference === promptType) {
                return resultPrompt.value;
            }
        }
    }
}

module.exports = LumpSumMapper;