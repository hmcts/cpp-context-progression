const PaymentTermsMapper = require('../PaymentTermsMapper');

describe('Payment terms mapper build correctly', () => {
    test('pay by date result', () => {
        const hearingJson = require(
            '../../../../test/case-level-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require(
            './compliance-enforcement-payment-terms-pay-by-date.json');
        const paymentTermsMapper = new PaymentTermsMapper(complianceEnforcementReserveTermsJson, hearingJson);
        const paymentTerms = paymentTermsMapper.buildPayment();
        expect(paymentTerms.paymentTermsType).toBe('BY_DATE');
        expect(paymentTerms.defaultDaysInJail).toBe(20);
        expect(paymentTerms.instalmentAmount).toBe(12.13);
        expect(paymentTerms.instalmentStartDate).toBe('2020-01-01');
        expect(paymentTerms.parentGuardianToPay).toBe(true)
        expect(paymentTerms.lumpSumAmount).toBe(undefined);
        expect(paymentTerms.payByDate).toBe('2020-12-12');
        expect(paymentTerms.paymentCardRequired).toBe('Y');
    });

    test('ptfor result', () => {
        const hearingJson = require(
            '../../../../test/case-level-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require(
            './compliance-enforcement-payment-terms-ptfor.json');
        const paymentTermsMapper = new PaymentTermsMapper(complianceEnforcementReserveTermsJson, hearingJson);
        const paymentTerms = paymentTermsMapper.buildPayment();
        expect(paymentTerms.paymentTermsType).toBe('BY_DATE');
        expect(paymentTerms.payByDate).toBe('2020-12-12');
        expect(paymentTerms.paymentCardRequired).toBe(undefined);
        expect(paymentTerms.parentGuardianToPay).toBe(false);
        expect(paymentTerms.instalmentStartDate).toBe(undefined);
        expect(paymentTerms.defaultDaysInJail).toBe(undefined);
        expect(paymentTerms.instalmentAmount).toBe(undefined);

    });

    test('installment only result', () => {
        const hearingJson = require(
            '../../../../test/case-level-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require(
            './compliance-enforcement-payment-terms-installment.json');
        const paymentTermsMapper = new PaymentTermsMapper(complianceEnforcementReserveTermsJson, hearingJson);
        const paymentTerms = paymentTermsMapper.buildPayment();
        expect(paymentTerms.paymentTermsType).toBe('WEEKLY_INSTALMENTS');
        expect(paymentTerms.defaultDaysInJail).toBe(20);
        expect(paymentTerms.instalmentAmount).toBe(12.13);
        expect(paymentTerms.instalmentStartDate).toBe('2020-01-01');
        expect(paymentTerms.parentGuardianToPay).toBe(true)
        expect(paymentTerms.lumpSumAmount).toBe(undefined);
        expect(paymentTerms.payByDate).toBe(undefined);
        expect(paymentTerms.paymentCardRequired).toBe('Y');
    });

    test('lump sum only result', () => {
        const hearingJson = require(
            '../../../../test/case-level-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require(
            './compliance-enforcement-payment-terms-lump-sum.json');
        const paymentTermsMapper = new PaymentTermsMapper(complianceEnforcementReserveTermsJson, hearingJson);
        const paymentTerms = paymentTermsMapper.buildPayment();
        expect(paymentTerms.paymentTermsType).toBe('WEEKLY_INSTALMENTS_THEN_LUMP_SUM');
        expect(paymentTerms.defaultDaysInJail).toBe(20);
        expect(paymentTerms.instalmentAmount).toBe(12.13);
        expect(paymentTerms.instalmentStartDate).toBe('2020-01-01');
        expect(paymentTerms.parentGuardianToPay).toBe(true)
        expect(paymentTerms.lumpSumAmount).toBe(undefined);
        expect(paymentTerms.payByDate).toBe(undefined);
        expect(paymentTerms.paymentCardRequired).toBe('Y');
    });
})