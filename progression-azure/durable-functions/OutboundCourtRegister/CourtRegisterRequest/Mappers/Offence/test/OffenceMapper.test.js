const OffenceMapper = require('../OffenceMapper');
const context = require('../../../../../testing/defaultContext');

describe('Offence Mapper', () => {

    let fakeOffences;
    let fakeRegisterDefendant;
    beforeEach(() => {
        jest.resetModules();
        fakeOffences = require('./offences.json');
        fakeRegisterDefendant = require('./defendant-context-base.json');
    });

    test('Should return correct values', () => {

        const result = new OffenceMapper(context, fakeOffences, fakeRegisterDefendant).build();

        expect(result).toBeTruthy();
        expect(result.length).toBe(1);
        expect(result[0].offenceCode).toBe(fakeOffences[0].offenceCode);
        expect(result[0].offenceTitle).toBe(fakeOffences[0].offenceTitle);
        expect(result[0].orderIndex).toBe(fakeOffences[0].orderIndex);

        expect(result[0].pleaValue).toBe(fakeOffences[0].plea.pleaValue);
        expect(result[0].verdictCode).toBe(fakeOffences[0].verdict.verdictType.description);
        expect(result[0].indicatedPleaValue).toBe(fakeOffences[0].indicatedPlea.indicatedPleaValue);
        expect(result[0].pleaDate).toBe(fakeOffences[0].plea.pleaDate);

    });
});