const ImpositionMapper = require('../ImpositionMapper');

describe('Imposition Mapper build correctly', () => {
    test('maps correctly', () => {
        const hearingJson = require(
            '../../../../test/case-level-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require(
            './compliance-enforcement-imposition.json');
        const impositionMapper = new ImpositionMapper(complianceEnforcementReserveTermsJson, hearingJson);
        const imposition = impositionMapper.buildImposition();
        expect(imposition.length).toBe(1);
        expect(imposition[0].offenceCode).toBe('PS90010');
        expect(imposition[0].impositionResultCode).toBe('FCOST');
        expect(imposition[0].impositionAmount).toBe(20);
        expect(imposition[0].prosecutionAuthorityId).toBe(undefined);
        expect(imposition[0].majorCreditor).toBe('TFL2');
    })
})