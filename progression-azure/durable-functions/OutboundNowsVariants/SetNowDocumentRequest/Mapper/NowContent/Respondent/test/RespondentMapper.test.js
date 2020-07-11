const RespondentMapper = require('../RespondentMapper');

describe('Respondent mapper builds correctly', () => {
    test('build respondent with all values', () => {
        const hearingJson = require('./hearing-with-defendant.json');
        const nowVariantJson = require('../../test/now-variant.json');
        const respondentMapper = new RespondentMapper(nowVariantJson, hearingJson);
        const respondents = respondentMapper.buildRespondents();

        expect(respondents.length).toBe(2);
        expect(respondents[0].name).toBe('Fred Smith');
        expect(respondents[0].firstName).toBe('Fred');
        expect(respondents[0].lastName).toBe('Smith');
        expect(respondents[0].address.line1).toBe('Flat 1');
        expect(respondents[0].address.line2).toBe('1 Old Road');
        expect(respondents[0].address.line3).toBe('London');
        expect(respondents[0].address.line4).toBe('Merton');
        expect(respondents[0].address.line5).toBe(undefined);
        expect(respondents[0].address.postCode).toBe('SW99 1AA');
        expect(respondents[1].name).toBe('John Smith');
    });

    test('build respondent with distinct respondent values', () => {
        const hearingJson = require('./hearing-with-same-defendant.json');
        const nowVariantJson = require('../../test/now-variant.json');
        const respondentMapper = new RespondentMapper(nowVariantJson, hearingJson);
        const respondents = respondentMapper.buildRespondents();

        expect(respondents.length).toBe(1);
        expect(respondents[0].name).toBe('Fred Smith');
        expect(respondents[0].firstName).toBe('Fred');
        expect(respondents[0].lastName).toBe('Smith');
        expect(respondents[0].address.line1).toBe('Flat 1');
        expect(respondents[0].address.line2).toBe('1 Old Road');
        expect(respondents[0].address.line3).toBe('London');
        expect(respondents[0].address.line4).toBe('Merton');
        expect(respondents[0].address.line5).toBe(undefined);
        expect(respondents[0].address.postCode).toBe('SW99 1AA');
    });
});