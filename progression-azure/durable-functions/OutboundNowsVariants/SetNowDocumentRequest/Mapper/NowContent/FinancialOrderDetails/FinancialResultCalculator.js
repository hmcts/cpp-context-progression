const PromptType = require('../../../../../OutboundComplianceEnforcement/PromptType');
const _ = require('lodash');
const FINANCIAL_TOTAL_AMOUNT_IMPOSED_PROMPT_REFERENCES = [PromptType.AMOUNT_OF_SURCHARGE,
                                                          PromptType.AMOUNT_OF_FINE,
                                                          PromptType.AMOUNT_OF_COMPENSATION,
                                                          PromptType.AMOUNT_OF_BACK_DUTY,
                                                          PromptType.COSTS_TO_CROWN_PROSECUTION_SERVICE_AMOUNT_PROMPT_REFERENCE,
                                                          PromptType.AMOUNT_OF_COSTS,
                                                          PromptType.AMOUNT_OF_REPARATION_COMPENSATION];

class FinancialResultCalculator {
    constructor(judicialResults) {
        this.judicialResults = judicialResults;
    }

    buildFinancialCalculations() {
        return this.getTotalAmount(this.judicialResults);
    }

    getTotalAmount(financialResults) {

        const judicialResultPrompts = _(financialResults).flatMapDeep('judicialResultPrompts').value().filter(prompt => prompt !== undefined);

        const total = judicialResultPrompts.filter(
            prompt => prompt.promptReference && FINANCIAL_TOTAL_AMOUNT_IMPOSED_PROMPT_REFERENCES.includes(
                prompt.promptReference)).map(prompt => prompt.value ? prompt.value : "0")
            .reduce((acc, value) => (parseFloat(acc) + parseFloat(value.replace("£", ""))), 0.00)
            .toFixed(2);

        const promptWithTotalAmountEnforced = judicialResultPrompts.find(
            prompt => PromptType.TOTAL_AMOUNT_ENFORCED_PROMPT_REFERENCE
                      === prompt.promptReference);

        const alreadyPaid = promptWithTotalAmountEnforced && promptWithTotalAmountEnforced.value ? promptWithTotalAmountEnforced.value.replace("£", "") : 0.0;

        const outstandingBalance = (parseFloat(total) - parseFloat(alreadyPaid)).toFixed(2);

        return {total, outstandingBalance};
    }
}

module.exports = FinancialResultCalculator;