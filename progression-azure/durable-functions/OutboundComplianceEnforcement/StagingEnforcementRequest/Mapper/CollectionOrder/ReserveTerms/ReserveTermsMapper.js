const Mapper = require('../../Mapper');
const InstallmentMapper = require('./Installments/InstallmentMapper');
const ReserveTermsType = require('./ReserveTermsType');
const LumpSumMapper = require('./LumpSum/LumpSumMapper');
const {ReserveTerms} = require('../../../Model/CollectionOrder');
const ResultDefinitionConstants = require(
    '../../../../../NowsHelper/constants/ResultDefinitionConstants');

class ReserveTermsMapper extends Mapper {

    constructor(complianceEnforcement, hearingJson) {
        super(complianceEnforcement, hearingJson);
        this.complianceEnforcement = complianceEnforcement;
        this.hearingJson = hearingJson;
    }

    getReserveTerms() {
        if (this.complianceEnforcement.reserveTermsResults) {
            const reserveTerms = new ReserveTerms();
            let reserveTermType = this.getReserveTermType();
            reserveTerms.reserveTermsType = reserveTermType;
            if (reserveTermType) {
                if (reserveTermType) {
                    reserveTerms.installments =
                        this.getInstallmentMapper().getInstallments(reserveTermType);
                    reserveTerms.lumpSum = this.getLumpSumMapper().buildLumpSum(reserveTermType);
                }
            }
            return reserveTerms;
        }
        return undefined;
    }

    getLumpSumMapper() {
        return new LumpSumMapper(this.complianceEnforcement, this.hearingJson);
    }

    getInstallmentMapper() {
        return new InstallmentMapper(this.complianceEnforcement,
                                     this.hearingJson);
    }

    getReserveTermType() {
        for (let reserveTermsResult of this.complianceEnforcement.reserveTermsResults) {
            if (reserveTermsResult.judicialResultPromptTypeId
                === ResultDefinitionConstants.RESERVE_TERMS_LUMP_SUM) {
                return ReserveTermsType.LUMP_SUM;
            } else if (reserveTermsResult.judicialResultTypeId
                       === ResultDefinitionConstants.RESERVE_TERMS_LUMP_SUM_PLUS_INSTALLMENT) {
                return ReserveTermsType.LUMP_SUM_PLUS_INSTALMENTS;
            } else if (reserveTermsResult.judicialResultTypeId
                       === ResultDefinitionConstants.RESERVE_TERMS_INSTALLMENTS_ONLY) {
                return ReserveTermsType.INSTALMENTS_ONLY;
            }
        }
    }

}

module.exports = ReserveTermsMapper;