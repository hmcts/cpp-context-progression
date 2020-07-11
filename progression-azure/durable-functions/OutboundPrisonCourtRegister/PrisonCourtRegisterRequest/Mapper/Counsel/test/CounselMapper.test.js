const CounselMapper = require('../CounselMapper');


describe('CounselMapper builds correctly', () => {
    test('build counsel with all values', () => {
        const defendantCounsels = require('./defendentCounsels');


        const counselMapper = new CounselMapper(defendantCounsels);
        const result = counselMapper.build();

        expect(result.length).toBe(3);
        expect(result[0].name).toBe("MidName LastName");
        expect(result[0].status).toBe("Status");

        expect(result[1].name).toBe("FirstName2 LastName2");
        expect(result[1].status).toBe("Status2");

        expect(result[2].name).toBe("FirstName3 MidName3 LastName3");
        expect(result[2].status).toBe("Status3");
    });

    test('build undefined counsels is empty', () => {

        const counselMapper = new CounselMapper();
        const result = counselMapper.build();

        expect(result).toEqual(undefined);
    });
});
