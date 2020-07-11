const CounselMapper = require('../CounselMapper');

describe('Counsel Mapper', () => {
    test('Should return empty array if null counsels', () => {
        const fakeCounsels = null;

        const result = new CounselMapper(fakeCounsels).build();

        expect(result).toEqual(undefined);
    });

    test('Should return correct values', () => {
        const fakeCounsels = [{
                id: "b92ed582-99ef-452c-b756-8ec9dc803548",
                title: "QC",
                firstName: "James",
                middleName: "Benjamin",
                lastName: "Simpson",
                status: "Junior QC"
            }];

        const result = new CounselMapper(fakeCounsels).build();

        expect(result).toBeTruthy();
        expect(result.length).toBe(1);
        expect(result[0].status).toBe(fakeCounsels[0].status);
    });
});