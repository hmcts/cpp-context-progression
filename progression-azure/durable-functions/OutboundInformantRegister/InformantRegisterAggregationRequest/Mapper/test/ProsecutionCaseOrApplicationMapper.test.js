const ProsecutionCaseOrApplicationMapper = require('../ProsecutionCaseOrApplicationMapper');
const context = require('../../../../testing/defaultContext');


describe('ProsecutionCaseOrApplicationMapper mapper works correctly', () => {
    test('should populate prosecution case or application information', () => {

        const hearingJson = require('./hearing.json');
        const informantRegister = require('./informantRegister.json');

        const prosecutionCasesOrApplications = new ProsecutionCaseOrApplicationMapper(hearingJson, informantRegister, informantRegister.registerDefendants[0]).build();

        expect(prosecutionCasesOrApplications.length).toBe(1);
        expect(prosecutionCasesOrApplications[0].caseOrApplicationReference).toBe('TFL4359536');
        expect(prosecutionCasesOrApplications[0].results.length).toBe(1);
        expect(prosecutionCasesOrApplications[0].offences.length).toBe(1);
        expect(prosecutionCasesOrApplications[0].arrestSummonsNumber).toBe('asn1234');
    });

    test('should populate prosecution cases or application information if multiple cases found for that defendant', () => {

        const hearingJson = require('./hearing_with_multiple_cases.json');
        const informantRegister = require('./informantRegister.json');

        const prosecutionCasesOrApplications = new ProsecutionCaseOrApplicationMapper(hearingJson, informantRegister, informantRegister.registerDefendants[0]).build();

        expect(prosecutionCasesOrApplications.length).toBe(2);
        expect(prosecutionCasesOrApplications[0].caseOrApplicationReference).toBe('TFL4359536');
        expect(prosecutionCasesOrApplications[0].results.length).toBe(1);
        expect(prosecutionCasesOrApplications[0].offences.length).toBe(2);
        expect(prosecutionCasesOrApplications[0].arrestSummonsNumber).toBe('asn1234');
        expect(prosecutionCasesOrApplications[1].caseOrApplicationReference).toBe('TFL4359536');
        expect(prosecutionCasesOrApplications[1].results.length).toBe(1);
        expect(prosecutionCasesOrApplications[1].offences.length).toBe(2);
        expect(prosecutionCasesOrApplications[1].arrestSummonsNumber).toBe('asn1234');
    });
});