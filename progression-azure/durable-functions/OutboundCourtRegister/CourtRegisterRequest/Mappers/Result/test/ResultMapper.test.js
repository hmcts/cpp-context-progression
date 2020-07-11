const ResultMapper = require('../ResultMapper');

describe('Result Mapper', () => {

    test('Should return correct values', () => {
        const fakeResults = [
            {
                "cjsCode": "Fine",
                "resultText": "Pay by date"
            }
        ];

        const result = new ResultMapper(fakeResults).build();
        expect(result).toBeTruthy();
        expect(result.length).toBe(1);
        expect(result[0].resultText).toBe(fakeResults[0].resultText);
        expect(result[0].cjsResultCode).toBe(fakeResults[0].cjsCode);
    });
});