const LumpSumMapper = require('../LumpSumMapper');
const ReserveTermsType = require('../../ReserveTermsType');

describe('LumpSum mapper works correctly', () => {
    test('build Lumpsum object with amounts', () => {
        const hearingJson = require(
            '../../../../../../test/case-level-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require(
            './compliance-enforcement-with-reserve-terms.json');
        const lumpSumMapper = new LumpSumMapper(complianceEnforcementReserveTermsJson,
                                                        hearingJson);
        const lumpSum = lumpSumMapper.buildLumpSum(ReserveTermsType.LUMP_SUM_PLUS_INSTALMENTS);
        expect(lumpSum.amount).toBe(12.00);
        expect(lumpSum.amountImposed).toBe(0);
    });
});
