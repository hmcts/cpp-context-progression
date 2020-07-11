const AddressMapper = require('../AddressMapper');

describe('Address Mapper', () => {
    test('Should return undefined if null address details', () => {

        const mapper = new AddressMapper(null);

        const result = mapper.build();

        expect(result).toEqual([]);
    });

    test('Should return correct values', () => {
        const fakeAddress = {
            address1: "Father - Flat 1",
            address2: "Father - 1 Old Road",
            address3: "Father - London",
            address4: "Father - Merton",
            postcode: "Father - SW99 1AA"
        };

        const result = new Address(fakeAddress).build();

        expect(result).toBeTruthy();
        expect(result.address1).toBe("Father - Flat 1");
        expect(result.address2).toBe("Father - 1 Old Road");
        expect(result.address3).toBe("Father - London");
        expect(result.address4).toBe("Father - Merton");
        expect(result.postCode).toBe("Father - SW99 1AA");

    });
});