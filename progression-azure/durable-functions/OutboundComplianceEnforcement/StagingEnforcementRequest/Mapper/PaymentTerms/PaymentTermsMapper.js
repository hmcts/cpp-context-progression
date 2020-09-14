const Mapper = require('../Mapper');
const ResultDefinitions = require('../../../../NowsHelper/constants/ResultDefinitionConstants');
const PaymentTermsType = require('./PaymentTermsType');
const PromptType = require('../../../PromptType');
const PaymentTerms = require('../../Model/PaymentTerms');
const moment = require('moment');

const PAYMENT_CARD_REQUIRED_UUID = "6dbddb7c-007c-4a81-a06d-17b09b68c01a";

class PaymentTermsMapper extends Mapper {

    constructor(complianceEnforcement, hearingJson) {
        super(complianceEnforcement, hearingJson);
        this.complianceEnforcement = complianceEnforcement;
        this.hearingJson = hearingJson;
    }

    buildPayment() {
        const paymentTerms = new PaymentTerms();
        const allPromptsFromJudicialResults = this.collectPromptsFromJudicialResults(this.complianceEnforcement.paymentTermsResults);
        const paymentTermsType = this.getPaymentTermsType();
        if (paymentTermsType !== null) {
            paymentTerms.paymentTermsType = paymentTermsType;
            paymentTerms.payByDate = this.getPayByDate(allPromptsFromJudicialResults, paymentTermsType);
            paymentTerms.lumpSumAmount = this.getLumpSumAmount(allPromptsFromJudicialResults, paymentTermsType);
            paymentTerms.defaultDaysInJail = this.getDefaultDaysInJail(allPromptsFromJudicialResults);
            paymentTerms.instalmentAmount = this.getInstallmentAmount(allPromptsFromJudicialResults);
            paymentTerms.instalmentStartDate = this.getInstalmentStartDate(allPromptsFromJudicialResults);
            paymentTerms.parentGuardianToPay = this.getParentGuardianToPay(allPromptsFromJudicialResults);
            paymentTerms.paymentCardRequired = this.getPaymentCardRequired(PAYMENT_CARD_REQUIRED_UUID, allPromptsFromJudicialResults);
        } else {
            //default payment type is Instalments only with instalment amount 20, monthly, instalment start date 3 months from decision date
            paymentTerms.paymentTermsType = PaymentTermsType.MONTHLY_INSTALMENTS;
            paymentTerms.instalmentAmount = 20;
            paymentTerms.instalmentStartDate = moment().add(90, 'd').format('YYYY-MM-DD');
        }
        return paymentTerms;
    }

    getParentGuardianToPay(allPromptsFromJudicialResults) {
        const parentGuardianPromptValue = this.getPromptValueByReference(allPromptsFromJudicialResults,
                                                                      PromptType.PARENT_GUARDIAN_TOPAY_PROMPT_REFERENCE);
        return !!(parentGuardianPromptValue && (parentGuardianPromptValue.toUpperCase().startsWith('Y')
                                                || parentGuardianPromptValue.toUpperCase().startsWith('T')));

    }

    getInstalmentStartDate(allPromptsFromJudicialResults) {
        const installmentStartDate = this.getPromptValueByReference(allPromptsFromJudicialResults,
                                                                      PromptType.INSTALMENT_START_DATE);
        if(installmentStartDate) {
            return moment(installmentStartDate, 'DD/MM/YYYY').format('YYYY-MM-DD');
        }
    }

    getInstallmentAmount(allPromptsFromJudicialResults) {
        let installmentAmount = this.getPromptValueByReference(allPromptsFromJudicialResults,
                                                                      PromptType.INSTALMENT_AMOUNT);
        if(installmentAmount) {
            return parseFloat(installmentAmount.startsWith('£') ? installmentAmount.split('£')[1]
                                                                : installmentAmount);
        }
    }

    getDefaultDaysInJail(allPromptsFromJudicialResults) {
        const defaultDaysInJail = this.getPromptValueByReference(allPromptsFromJudicialResults,
                                                                      PromptType.DEFAULT_DAYS_IN_JAIL_PROMPT_REFERENCE);
        return defaultDaysInJail ? Number(defaultDaysInJail) : undefined;
    }

    getLumpSumAmount(allPromptsFromJudicialResults, paymentTermsType) {
        if ([PaymentTermsType.WEEKLY_INSTALMENTS_THEN_LUMP_SUM, PaymentTermsType.FORTNIGHTLY_INSTALMENTS_THEN_LUMP_SUM,
             PaymentTermsType.MONTHLY_INSTALMENTS_THEN_LUMP_SUM].includes(paymentTermsType)) {
            const lumpSumAmount = allPromptsFromJudicialResults.find(resultPrompt => resultPrompt.promptReference === PromptType.LUMP_SUM_AMOUNT);
            return lumpSumAmount ? lumpSumAmount.value : undefined;
        }
        return undefined;
    }

    getPaymentCardRequired(paymentCardRequiredPromptRef, allPromptsFromJudicialResults) {
        const paymentCardRequired = allPromptsFromJudicialResults.find(resultPrompt => resultPrompt.promptReference === PromptType.PAYMENT_CARD_REQUIRED_PROMPT_REFERENCE
                && paymentCardRequiredPromptRef === resultPrompt.judicialResultPromptTypeId);
        if (paymentCardRequired) {
            if (paymentCardRequired.value.toUpperCase().startsWith('Y')
                || paymentCardRequired.value.toUpperCase().startsWith('T')) {
                return 'Y';
            }
            return 'N';
        }
        return undefined;
    }

    getPayByDate(promptsFromJudicialResults, paymentTermsType) {
        if (paymentTermsType === PaymentTermsType.BY_DATE) {
            const payByDatePrompt = promptsFromJudicialResults.find(resultPrompt => resultPrompt.promptReference === PromptType.PAY_BY_DATE);
            return payByDatePrompt ? moment(payByDatePrompt.value, 'DD/MM/YYYY').format('YYYY-MM-DD') : undefined;
        }
        return undefined;
    }

    getPaymentTermsType() {
        if(this.complianceEnforcement.paymentTermsResults){
            for(let paymentTermsResult of this.complianceEnforcement.paymentTermsResults){
                if(paymentTermsResult.judicialResultTypeId === ResultDefinitions.PAY_BY_DATE){
                    return PaymentTermsType.BY_DATE;
                }else if(paymentTermsResult.judicialResultTypeId === ResultDefinitions.INSTALLMENTS_ONLY){
                    return this.getPaymentTypeForInstallmentOnly(paymentTermsResult.judicialResultPrompts);
                }else if(paymentTermsResult.judicialResultTypeId === ResultDefinitions.LUMP_SUM_PLUS_INSTALLMENTS){
                    return this.getPaymentTypeForLumpSumInstallments(paymentTermsResult.judicialResultPrompts);
                }else if(paymentTermsResult.judicialResultTypeId === ResultDefinitions.PAYMENT_TERMS_ON_RELEASE){
                    return PaymentTermsType.BY_DATE;
                }
            }
        }
        return null;
    }
    getPaymentTypeForLumpSumInstallments(resultPrompts) {
        const paymentFrequencyPrompt = resultPrompts.find(resultPrompt => resultPrompt.promptReference === PromptType.PAYMENT_FREQUENCY);
        switch (paymentFrequencyPrompt && paymentFrequencyPrompt.value) {
            case PromptType.WEEKLY:
                return PaymentTermsType.WEEKLY_INSTALMENTS_THEN_LUMP_SUM
            case PromptType.FORTNIGHTLY:
                return PaymentTermsType.FORTNIGHTLY_INSTALMENTS_THEN_LUMP_SUM
            case PromptType.MONTHLY:
                return PaymentTermsType.MONTHLY_INSTALMENTS_THEN_LUMP_SUM
            default:
                return null;
        }
        return null;
    }

    getPaymentTypeForInstallmentOnly(resultPrompts) {
        const paymentFrequencyPrompt = resultPrompts.find(resultPrompt => resultPrompt.promptReference === PromptType.PAYMENT_FREQUENCY);
        switch (paymentFrequencyPrompt && paymentFrequencyPrompt.value) {
            case PromptType.WEEKLY:
                return PaymentTermsType.WEEKLY_INSTALMENTS
            case PromptType.FORTNIGHTLY:
                return PaymentTermsType.FORTNIGHTLY_INSTALMENTS
            case PromptType.MONTHLY:
                return PaymentTermsType.MONTHLY_INSTALMENTS
            default:
                return null;

        }
        return null;
    }

}

module.exports = PaymentTermsMapper;