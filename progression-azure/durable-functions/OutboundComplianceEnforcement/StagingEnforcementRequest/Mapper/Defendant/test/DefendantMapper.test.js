const DefendantMapper = require('../DefendantMapper');

describe('Defendant mapper works correctly', () => {
    test('build defendant', () => {
        const hearingJson = require(
            '../../../../test/case-level-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require(
            './compliance-enforcement-reserve-terms.json');
        const defendantMapper = new DefendantMapper(complianceEnforcementReserveTermsJson, hearingJson);
        const defendant = defendantMapper.buildDefendant();
        expect(defendant.forenames).toBe('Fred');
        expect(defendant.dateOfBirth).toBe('1965-12-27');
        expect(defendant.address1).toBe('Flat 1');
        expect(defendant.address2).toBe('1 Old Road');
        expect(defendant.address3).toBe('London');
        expect(defendant.address4).toBe('Merton');
        expect(defendant.address5).toBe(undefined);
        expect(defendant.postcode).toBe('SW99 1AA');
        expect(defendant.title).toBe('MR');
        expect(defendant.surname).toBe('Smith');
        expect(defendant.companyName).toBe(undefined);
        expect(defendant.nationalInsuranceNumber).toBe('ABCD1234');
        expect(defendant.telephoneNumberHome).toBe('044-012345566');
        expect(defendant.telephoneNumberBusiness).toBe('01234-67859512');
        expect(defendant.telephoneNumberMobile).toBe('0712345678');
        expect(defendant.emailAddress1).toBe('sajan@test.com');
        expect(defendant.emailAddress2).toBe(undefined);
        expect(defendant.statementOfMeansProvided).toBe(undefined);
        expect(defendant.benefitsTypes).toBe(undefined);
        expect(defendant.vehicleMake).toBe('PASSENGER_CARRYING_VEHICLE');
        expect(defendant.vehicleRegistrationMark).toBe('TJ07JRD');
        expect(defendant.hearingLanguage).toBe('ENGLISH');
        expect(defendant.dateOfSentence).toBe('20-01-2020');
        expect(defendant.documentLanguage).toBe('ENGLISH');
        expect(defendant.dateOfSentence).toBe('20-01-2020');
    })
})
