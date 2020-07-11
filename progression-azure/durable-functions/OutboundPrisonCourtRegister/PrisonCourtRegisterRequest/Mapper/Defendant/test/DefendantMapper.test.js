const DefendantMapper = require('../DefendantMapper');
const context = require('../../../../../testing/defaultContext');

describe('DefendantMapper builds correctly', () => {

    test('build defendant with all values', () => {
        const hearing = require('./hearing-with-results.json').hearing;
        const prisonCourtRegister = require('./prisonCourtRegister.json');

        const defendantMapper = new DefendantMapper(context, hearing, prisonCourtRegister);
        const person = prisonCourtRegister.registerDefendant.personDefendant.personDetails;

        const result = defendantMapper.build();

        expect(result).toBeTruthy();

        expect(result.masterDefendantId).toEqual(prisonCourtRegister.registerDefendant.masterDefendantId);
        expect(result.name).toEqual(person.firstName + ' ' +person.lastName);
        expect(result.dateOfBirth).toEqual(person.dateOfBirth);
        expect(result.gender).toEqual(person.gender);
        expect(result.nationality).toEqual(person.nationalityDescription);
        expect(result.address.address1).toEqual(person.address.address1);
        expect(result.address.address2).toEqual(person.address.address2);
        expect(result.address.address3).toEqual(person.address.address3);
        expect(result.address.address4).toEqual(person.address.address4);
        expect(result.address.address5).toEqual(person.address.address5);
        expect(result.address.postCode).toEqual(person.address.postcode);
    });

});


