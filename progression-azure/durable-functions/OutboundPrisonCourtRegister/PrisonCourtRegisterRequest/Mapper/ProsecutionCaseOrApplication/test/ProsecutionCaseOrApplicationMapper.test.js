const ProsecutionCaseOrApplicationMapper = require('../ProsecutionCaseOrApplicationMapper');

describe('ProsecutionCaseOrApplication Mapper', () => {

    let fakeHearingResultedJson;
    let fakeRegisterDefendant;

    beforeEach(() => {
        jest.resetModules();
        fakeHearingResultedJson = require('./hearing-resulted.json');
        fakeRegisterDefendant = require('./defendant-context-base.json');
    });


    test('Should return correct values', () => {

        const mapper = new ProsecutionCaseOrApplicationMapper(fakeRegisterDefendant, fakeHearingResultedJson);

        const result = mapper.build();
        expect(result).toBeTruthy();
        expect(result.length).toBe(2);
    });
});