const ResultDefinitionConstants = require('../../../../../NowsHelper/constants/ResultDefinitionConstants');
const moment = require('moment');
const PaymentTermsTypeConstant = require('./PaymentTermsTypeConstant.js');

class PaymentTermsCalculator {
    constructor(judicialResults) {
        this.judicialResults = judicialResults;
    }

    buildPaymentTerms() {
        const paymentTermsResults = this.getPaymentTermsResults(this.judicialResults);
        return this.getPaymentTerms(paymentTermsResults);
    }

    getPaymentTermsResults(judicialResults) {
        const paymentTermsResults = [];
        judicialResults.forEach(judicialResult => {
            if (this.isResultDefinitionHasPaymentTerms(judicialResult.judicialResultTypeId)) {
                paymentTermsResults.push(judicialResult);
            }
        });

        return paymentTermsResults;
    }

    isResultDefinitionHasPaymentTerms(resultDefinition) {
        return resultDefinition === ResultDefinitionConstants.PAY_BY_DATE || resultDefinition
               === ResultDefinitionConstants.INSTALLMENTS_ONLY || resultDefinition
               === ResultDefinitionConstants.LUMP_SUM_PLUS_INSTALLMENTS;
    }

    getPaymentTerms(paymentTermsResults) {
        if(paymentTermsResults.length) {
            const resultWithPaymentTermsResultText = paymentTermsResults.find(result => result.resultText);
            return resultWithPaymentTermsResultText.resultText;
        }
        const startDate = moment().add(90, 'd').format('YYYY-MM-DD');
        return PaymentTermsTypeConstant.DEFAULT_PAYMENT_TERMS + ` ${startDate}`;
    }
}

module.exports = PaymentTermsCalculator;