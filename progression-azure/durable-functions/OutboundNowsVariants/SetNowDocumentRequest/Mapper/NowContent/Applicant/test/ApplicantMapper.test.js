const ApplicantMapper = require('../ApplicantMapper');

describe('Applicant mapper builds correctly', () => {
    test('build applicant with all values', () => {
        const hearingJson = require('./hearing-with-defendant.json');
        const nowVariantJson = require('../../test/now-variant.json');

        const applicantMapper = new ApplicantMapper(nowVariantJson, hearingJson);

        const applicants = applicantMapper.buildApplicants();

        expect(applicants[0].name).toBe('Fred Smith');
        expect(applicants[0].firstName).toBe('Fred');
        expect(applicants[0].lastName).toBe('Smith');
        expect(applicants[0].address.line1).toBe('Flat 1');
        expect(applicants[0].address.line2).toBe('1 Old Road');
        expect(applicants[0].address.line3).toBe('London');
        expect(applicants[0].address.line4).toBe('Merton');
        expect(applicants[0].address.line5).toBe(undefined);
        expect(applicants[0].address.postCode).toBe('SW99 1AA');
    });

    test('build applicant with organisation values', () => {
        const hearingJson = require('./hearing-with-organisation.json');
        const nowVariantJson = require('../../test/now-variant.json');

        const applicantMapper = new ApplicantMapper(nowVariantJson, hearingJson);

        const applicants = applicantMapper.buildApplicants();

        expect(applicants[0].name).toBe('Organisation Name');
        expect(applicants[0].firstName).toBe(undefined);
        expect(applicants[0].lastName).toBe(undefined);
        expect(applicants[0].address.line1).toBe('Flat 1');
        expect(applicants[0].address.line2).toBe('1 Old Road');
        expect(applicants[0].address.line3).toBe('London');
        expect(applicants[0].address.line4).toBe('Merton');
        expect(applicants[0].address.line5).toBe(undefined);
        expect(applicants[0].address.postCode).toBe('SW99 1AA');
    });
});