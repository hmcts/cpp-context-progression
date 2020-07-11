const YouthDefendantMapper = require('../YouthDefendantMapper');
const context = require('../../../../../testing/defaultContext');

describe('YouthDefendantMapper Mapper', () => {

    let fakeHearingResultedJson;
    let fakeYoutDefendants;
    let fakeRegisterDefendant;

    beforeEach(() => {
        jest.resetModules();
        fakeHearingResultedJson = require('./hearing-resulted.json');
        fakeYoutDefendants = require('./youth-defendants.json');
        fakeRegisterDefendant = require('./defendant-context-base.json');
    });

    test('Should return correct values', () => {

        const mapper = new YouthDefendantMapper(context, fakeYoutDefendants, fakeHearingResultedJson, fakeRegisterDefendant);
        const expectedYouthDefendant = fakeHearingResultedJson.prosecutionCases[0].defendants[0].personDefendant.personDetails;
        const result = mapper.build();
        expect(result).toBeTruthy();
        expect(result.length).toBe(1);
        expect(result[0].masterDefendantId).toBe(fakeYoutDefendants[0].masterDefendantId);
        expect(result[0].name).toBe(expectedYouthDefendant.firstName + ' ' + expectedYouthDefendant.lastName);
        expect(result[0].dateOfBirth).toBe(expectedYouthDefendant.dateOfBirth);
        expect(result[0].gender).toBe(expectedYouthDefendant.gender);
        expect(result[0].nationality).toBe(expectedYouthDefendant.nationalityDescription);
        expect(result[0].address.address1).toBe(expectedYouthDefendant.address.address1);
        expect(result[0].address.address2).toBe(expectedYouthDefendant.address.address2);
        expect(result[0].address.address3).toBe(expectedYouthDefendant.address.address3);
        expect(result[0].address.address4).toBe(expectedYouthDefendant.address.address4);
        expect(result[0].address.address5).toBe(expectedYouthDefendant.address.address5);
        expect(result[0].address.postCode).toBe(expectedYouthDefendant.address.postcode);
        expect(result[0].postHearingCustodyStatus).toBe('Not Applicable');
    });
});