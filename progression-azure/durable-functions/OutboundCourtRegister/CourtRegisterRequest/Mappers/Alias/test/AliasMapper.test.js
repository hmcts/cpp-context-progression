const AliasMapper = require('../AliasMapper');

describe('Alias Mapper', () => {
    test('Should return empty array if null alias', () => {
        const fakeAliases = null;

        const mapper = new AliasMapper(fakeAliases);

        const result = mapper.build();

        expect(result).toEqual(undefined);
    });

    test('Should return correct values', () => {
        const fakeAliases = [{
            title: "Mr",
            firstName: "John",
            middleName: "Duncan",
            lastName: "Smith",
            legalEntityName: "legal name"
        }];

        const mapper = new AliasMapper(fakeAliases);

        const result = mapper.build();

        expect(result).toBeTruthy();
        expect(result.length).toBe(1);
        expect(result[0].title).toBe(fakeAliases[0].title);
        expect(result[0].firstName).toBe(fakeAliases[0].firstName);
        expect(result[0].middleName).toBe(fakeAliases[0].middleName);
        expect(result[0].lastName).toBe(fakeAliases[0].lastName);

    });
});