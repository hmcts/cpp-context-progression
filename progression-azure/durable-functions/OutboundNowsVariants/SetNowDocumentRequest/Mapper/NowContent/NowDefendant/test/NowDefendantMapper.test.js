const NowDefendantMapper = require('../NowDefendantMapper');

describe('Defendant mapper builds correctly', () => {
    test('build defendant with all values', () => {
        const hearingJson = require(
            '../../test/hearing.json');
        const nowVariantJson = require(
            '../../test/now-variant.json');
        const defendantMapper = new NowDefendantMapper(nowVariantJson, hearingJson);
        const defendant = defendantMapper.buildDefendant();

        expect(defendant.name).toBe('Fred Smith');
        expect(defendant.address.line1).toBe('Flat 1');
        expect(defendant.address.line2).toBe('1 Old Road');
        expect(defendant.address.line3).toBe('London');
        expect(defendant.address.line4).toBe('Merton');
        expect(defendant.address.line5).toBe(undefined);
        expect(defendant.address.postCode).toBe('SW99 1AA');
        expect(defendant.dateOfBirth).toBe('1965-12-27');
        expect(defendant.pncId).toBe('123456');
        expect(defendant.nationality).toBe('British');
        expect(defendant.prosecutingAuthorityReference).toBe('ASN2,ASN3,ASN4');
        expect(defendant.landLineNumber).toBe('044-012345566');
        expect(defendant.mobileNumber).toBe('0712345678');
        expect(defendant.nationalInsuranceNumber).toBe('SG12345DC');
        expect(defendant.ethnicity).toBe('AAAA');
        expect(defendant.gender).toBe('MALE');
        expect(defendant.driverNumber).toBe('ABCD1234');
        expect(defendant.solicitor.name).toBe('DEFENCE ORGANISATION');
        expect(defendant.solicitor.address.line1).toBe('Flat 1');
        expect(defendant.defendantResults.length).toBe(1);
        expect(defendant.defendantResults[0].label).toBe("Imprisonment");
        expect(defendant.defendantResults[0].nowRequirementText.length).toBe(1);
        expect(defendant.occupation).toBe('Lorry Driver');
        expect(defendant.aliasNames[0]).toBe('Mr John Smith');
        expect(defendant.aliasNames[1]).toBe('John2 Duncan2 Smith2');
        expect(defendant.title).toBe('MR');
        expect(defendant.selfDefinedEthnicity).toBe('AAAA');
        expect(defendant.interpreterLanguageNeeds).toBe('ENGLISH');
        expect(defendant.specialNeeds).toBe('Translation');
        expect(defendant.isYouth).toBe('Y');
    });
});