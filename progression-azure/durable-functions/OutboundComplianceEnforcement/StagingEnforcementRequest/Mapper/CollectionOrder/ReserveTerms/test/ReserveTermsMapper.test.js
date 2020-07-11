const ReserveTermsMapper = require('../ReserveTermsMapper');
const {Installments} = require('../../../../Model/CollectionOrder');

describe('Reserve terms mapper works correctly', () => {

    test('build reserve terms', () => {
        const hearingJson = require(
            '../../../../../test/case-level-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require(
            '../../test/compliance-enforcement-reserve-terms.json');

        const reserveTermsMapper = new ReserveTermsMapper(complianceEnforcementReserveTermsJson,
                                                          hearingJson);
        const installments = new Installments();
        installments.startDate = '2020-12-12';
        installments.frequency = 'WEEKLY';
        installments.amount = '40.00';

        reserveTermsMapper.getLumpSumMapper = jest.fn(()=>{
            return {
                buildLumpSum(){}
            }
        });
        reserveTermsMapper.getInstallmentMapper = jest.fn(() => {
            return {
                getInstallments() {
                    return installments;
                }
            }
        });
        const reserveTerms = reserveTermsMapper.getReserveTerms();
        expect(reserveTermsMapper.getInstallmentMapper.mock.calls.length).toBe(1);
        expect(reserveTermsMapper.getLumpSumMapper.mock.calls.length).toBe(1);
        expect(reserveTerms.installments).toEqual(installments);
        expect(reserveTerms.reserveTermsType).toBe("INSTALMENTS_ONLY")
    })
})