const CollectionOrderMapper = require('../CollectionOrderMapper');
const ReserveTermsType = require('../ReserveTerms/ReserveTermsType');

describe('Collection order mapper works correctly', () => {
    test('build collection order', () => {
        const hearingJson = require(
            '../../../../test/case-level-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require(
            './compliance-enforcement-reserve-terms.json');
        const collectionOrderMapper = new CollectionOrderMapper(
            complianceEnforcementReserveTermsJson,
            hearingJson);
        const reserveTerms = {
            reserveTermsType: ReserveTermsType.LUMP_SUM_PLUS_INSTALMENTS,
            lumpSum: 200,
            installments: {}
        };
        collectionOrderMapper.getReserveTermsMapper = jest.fn(() => {
            return {
                getReserveTerms() {
                    return reserveTerms;
                }
            }
        });
        const collectionOrder = collectionOrderMapper.buildCollectionOrder();
        expect(collectionOrderMapper.getReserveTermsMapper.mock.calls.length).toBe(1);
        expect(collectionOrder.isCollectionOrderMade).toBe(true);
        expect(collectionOrder.isApplicationForBenefitsDeduction).toBe(false);
        expect(collectionOrder.isAttachmentOfEarnings).toBe(false);
        expect(collectionOrder.reserveTerms).toEqual(reserveTerms);
    })

    test('build collection order ptfor', () => {
        const hearingJson = require(
            '../../../../test/case-level-judicial-results-hearing.json');
        const complianceEnforcementPtforJson = require(
            './compliance-enforcement-ptfor.json');
        const collectionOrderMapper = new CollectionOrderMapper(
            complianceEnforcementPtforJson,
            hearingJson);
        const collectionOrder = collectionOrderMapper.buildCollectionOrder();
        expect(collectionOrder.isCollectionOrderMade).toBe(false);
        expect(collectionOrder.isApplicationForBenefitsDeduction).toBe(false);
        expect(collectionOrder.isAttachmentOfEarnings).toBe(false);
        expect(collectionOrder.isPaymentTermsOnRelease).toBe(true);
        expect(collectionOrder.reserveTerms).toBeUndefined();
    })
})