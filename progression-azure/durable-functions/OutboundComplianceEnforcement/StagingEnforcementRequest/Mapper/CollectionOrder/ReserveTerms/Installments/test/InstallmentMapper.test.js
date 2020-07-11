const context = require('../../../../../../../testing/defaultContext');
const InstallmentMapper = require('../InstallmentMapper');
const ReserveTermsType = require('../../ReserveTermsType');


describe('Installment mapper works correctly', () => {

    test('build installment object', () => {
        const hearingJson = require(
            '../../../../../../test/case-level-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require(
            '../../../test/compliance-enforcement-reserve-terms.json');
        const installmentMapper = new InstallmentMapper(complianceEnforcementReserveTermsJson,
                                                        hearingJson);
        const installment = installmentMapper.getInstallments(ReserveTermsType.INSTALMENTS_ONLY);
        expect(installment.amount).toBe("40");
        expect(installment.frequency).toBe("WEEKLY");
        expect(installment.startDate).toBe("2020-12-12");
    })
});