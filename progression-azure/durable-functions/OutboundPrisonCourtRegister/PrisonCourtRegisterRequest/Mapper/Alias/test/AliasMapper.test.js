const AliasMapper = require('../AliasMapper');


describe('AliasMapper builds correctly', () => {
    test('build alias with all values', () => {
        const defendantAliases = require('./defendentAliases');


        const aliasMapper = new AliasMapper(defendantAliases);
        const result = aliasMapper.build();

        expect(result.length).toBe(2);
        expect(result[0].title).toBe("Mr");
        expect(result[0].firstName).toBe("FirstName");
        expect(result[0].middleName).toBe("MidName");
        expect(result[0].lastName).toBe("LastName");

        expect(result[1].title).toBe("Mr2");
        expect(result[1].firstName).toBe("FirstName2");
        expect(result[1].middleName).toBe("MidName2");
        expect(result[1].lastName).toBe("LastName2");
    });

    test('build empty array', () => {

        const aliasMapper = new AliasMapper({}.aliases=[]);
        const result = aliasMapper.build();

        expect(result).toEqual([])
    });
});
