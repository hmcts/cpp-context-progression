const CaseMapper = require('../CaseMapper');

describe('Case mapper builds correctly', () => {
    test('build prosecution case with all values', () => {
        const hearingJson = require('../../test/hearing.json');
        const nowVariantJson = require('../../test/now-variant.json');
        const caseMapper = new CaseMapper(nowVariantJson, hearingJson);
        const cases = caseMapper.buildProsecutionCases();

        expect(cases.length).toBe(1);
        expect(cases[0].reference).toBe("TFL4359536");
        expect(cases[0].caseMarkers[0]).toBe("WP");
        expect(cases[0].caseMarkers[1]).toBe("PN");
        expect(cases[0].defendantCaseOffences[0].title).toBe("Public service vehicle - passenger use altered / defaced   ticket");
        expect(cases[0].defendantCaseOffences[0].results[0].label).toBe("Financial Penalty");
        expect(cases[0].defendantCaseOffences[0].code).toBe("Offence Code");
        expect(cases[0].defendantCaseOffences[0].modeOfTrial).toBe("Summary");
        expect(cases[0].defendantCaseOffences[0].allocationDecision).toBe("Mot reason");
        expect(cases[0].defendantCaseOffences[0].verdictType).toBe("Verdict Type Description");
        expect(cases[0].prosecutor).toBe("Prosecution Authority Name1");
        expect(cases[0].applicationType).toBe("applicationType");
        expect(cases[0].applicationTypeWelsh).toBe("applicationTypeWelsh");
        expect(cases[0].applicationLegislation).toBe("applicationLegislation");
        expect(cases[0].applicationLegislationWelsh).toBe("applicationLegislationWelsh");
        expect(cases[0].applicationReceivedDate).toBe("13/06/2020");
        expect(cases[0].applicationParticulars).toBe("applicationParticulars");
        expect(cases[0].allegationOrComplaintStartDate).toBeUndefined();
        expect(cases[0].allegationOrComplaintEndDate).toBeUndefined();
        expect(cases[0].applicationCode).toBe("applicationCode");
    });
});