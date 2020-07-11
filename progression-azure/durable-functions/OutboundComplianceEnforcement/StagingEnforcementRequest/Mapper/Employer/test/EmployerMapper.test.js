const EmployerMapper = require('../EmployerMapper');

describe('Employer mapper build correctly', ()=>{
    test('employer model has correct values', ()=>{
        const hearingJson = require(
            '../../../../test/case-level-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require(
            './compliance-enforcement-employer.json');
        const employerMapper = new EmployerMapper(complianceEnforcementReserveTermsJson, hearingJson);
        const employer = employerMapper.buildEmployer();
        expect(employer.employerReference).toBe("EMP1025");
        expect(employer.employerCompanyName).toBe("HMCTS");
        expect(employer.employerAddress1).toBe("102");
        expect(employer.employerAddress2).toBe("Petty France");
        expect(employer.employerAddress3).toBe("Westminster");
        expect(employer.employerAddress4).toBe("London");
        expect(employer.employerAddress5).toBe("2");
        expect(employer.employerPostcode).toBe("Rg14 7EZ");
        expect(employer.telephoneNumber).toBe(undefined);
        expect(employer.emailAddress).toBe(undefined);
    });
})