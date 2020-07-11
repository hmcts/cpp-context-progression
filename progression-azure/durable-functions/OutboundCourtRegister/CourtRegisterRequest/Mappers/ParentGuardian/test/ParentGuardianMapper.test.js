const ParentGuardianMapper = require('../ParentGuardianMapper');

describe('Parent guardian mapper', () => {
    let fakeHearingResultedJson;
    let fakeRegisterDefendant;

    beforeEach(() => {
        jest.resetModules();
        fakeHearingResultedJson = require('./hearing-resulted.json');
        fakeRegisterDefendant = require('./defendant-context-base.json');
    });

    test('Should return correct values', () => {

        const mapper = new ParentGuardianMapper(fakeRegisterDefendant, fakeHearingResultedJson);
        const result = mapper.build();
        expect(result).toBeTruthy();
        expect(result.name).toBe("Father - Fred Father - Smith" );
        expect(result.address.address1).toBe("Father - Flat 1");
        expect(result.address.address2).toBe("Father - 1 Old Road");
        expect(result.address.address3).toBe("Father - London");
        expect(result.address.address4).toBe("Father - Merton");
        expect(result.address.address5).toBe(undefined);
        expect(result.address.postCode).toBe("Father - SW99 1AA");
    });

});