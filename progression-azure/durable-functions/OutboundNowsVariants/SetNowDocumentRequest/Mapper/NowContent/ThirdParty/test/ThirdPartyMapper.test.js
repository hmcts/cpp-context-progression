const ThirdPartyMapper = require('../ThirdPartyMapper');

describe('Respondent mapper builds correctly', () => {
    test('build respondent with all values', () => {
        const hearingJson = require('./hearing-with-defendant.json');
        const nowVariantJson = require('../../test/now-variant.json');
        const thirdPartyMapper = new ThirdPartyMapper(nowVariantJson, hearingJson);
        const thirdParties = thirdPartyMapper.buildThirdParties();

        expect(thirdParties.length).toBe(1);
        expect(thirdParties[0].name).toBe('Fred Smith');
        expect(thirdParties[0].firstName).toBe('Fred');
        expect(thirdParties[0].lastName).toBe('Smith');
        expect(thirdParties[0].address.line1).toBe('Flat 1');
        expect(thirdParties[0].address.line2).toBe('1 Old Road');
        expect(thirdParties[0].address.line3).toBe('London');
        expect(thirdParties[0].address.line4).toBe('Merton');
        expect(thirdParties[0].address.line5).toBe(undefined);
        expect(thirdParties[0].address.postCode).toBe('SW99 1AA');
    });
});