const ResultMapper = require('../ResultMapper');


describe('ResultMapper builds correctly', () => {
    test('build result mapper with all values', () => {
        const judicialResults = require('./judicialResult.json');


        const resultMapper = new ResultMapper(judicialResults);
        const result = resultMapper.build();

        expect(result.length).toBe(2);
        expect(result[0].resultText).toBe("Result Text");
        expect(result[0].cjsResultCode).toBe("CjsCode");
        expect(result[1].resultText).toBe("Result Text2");
        expect(result[1].cjsResultCode).toBe("CjsCode2");
    })

});