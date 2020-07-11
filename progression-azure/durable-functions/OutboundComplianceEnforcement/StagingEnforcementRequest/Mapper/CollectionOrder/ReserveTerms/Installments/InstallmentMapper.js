const Mapper = require('../../../Mapper');
const {Installments} = require('../../../../Model/CollectionOrder');
const PromptType = require('../../../../../PromptType');
const ReserveTermsType = require('../ReserveTermsType');
const moment = require('moment');

class InstallmentMapper extends Mapper {

    constructor(complianceEnforcement, hearingJson) {
        super(complianceEnforcement, hearingJson);
        this.complianceEnforcement = complianceEnforcement;
        this.hearingJson = hearingJson;
    }

    getInstallments(reserveTermType) {
        if(reserveTermType != ReserveTermsType.LUMP_SUM){
            const installments = new Installments();
            const reserveTermsResultPrompts = this.collectPromptsFromJudicialResults(
                this.complianceEnforcement.reserveTermsResults);
            const amount = this.getInstallmentAmount(reserveTermsResultPrompts);
            if (amount) {
                installments.amount = this.getInstallmentAmount(reserveTermsResultPrompts);
                installments.frequency = this.getInstallmentFrequency(reserveTermsResultPrompts);
                installments.startDate = this.getInstallmentStartDate(reserveTermsResultPrompts);
            }
            return installments;
        }
    }

    getInstallmentFrequency(reserveTermsResultPrompts) {
        return this.getPromptValueByReference(reserveTermsResultPrompts,
                                              PromptType.PAYMENT_FREQUENCY);
    }

    getInstallmentAmount(reserveTermsResultPrompts) {
        return this.getPromptValueByReference(reserveTermsResultPrompts,
                                              PromptType.INSTALMENT_AMOUNT);
    }

    getInstallmentStartDate(reserveTermsResultPrompts) {
        const installmentStartDate = this.getPromptValueByReference(reserveTermsResultPrompts,
                                                                    PromptType.INSTALMENT_START_DATE);
        if(installmentStartDate) {
            return moment(installmentStartDate, 'DD/MM/YYYY').format('YYYY-MM-DD');
        }
    }
}

module.exports = InstallmentMapper;