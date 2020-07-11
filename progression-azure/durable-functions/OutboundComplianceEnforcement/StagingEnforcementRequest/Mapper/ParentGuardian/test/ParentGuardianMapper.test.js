const ParentGuardianMapper = require('../ParentGuardianMapper');

describe('Parent guardian mapper build correctly', ()=> {
    test('maps fields correctly', ()=>{
        const hearingJson = require(
            '../../../../test/case-level-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require(
            './compliance-enforcement-payment-terms-parent-guardian.json');
        const parentGuardianMapper = new ParentGuardianMapper(complianceEnforcementReserveTermsJson, hearingJson);
        const parentGuardian = parentGuardianMapper.buildParentGuardian();
        expect(parentGuardian.name).toBe("Father - Fred Father - Smith" );
        expect(parentGuardian.address1).toBe("Father - Flat 1");
        expect(parentGuardian.address2).toBe("Father - 1 Old Road");
        expect(parentGuardian.address3).toBe("Father - London");
        expect(parentGuardian.address4).toBe("Father - Merton");
        expect(parentGuardian.address5).toBe(undefined);
        expect(parentGuardian.postCode).toBe("Father - SW99 1AA");
    })
})