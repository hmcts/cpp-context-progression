const OffenceMapper = require('../OffenceMapper');


describe('OffenceMapper builds correctly', () => {
    test('build offence with all values', () => {
        const offencesJon = require('./offences.json');
        const registerDefendant = require ('./defendant-context-base')

        const offenceMapper = new OffenceMapper(offencesJon.offences,registerDefendant);
        const results = offenceMapper.build();

        expect(results.length).toBe(2);
        expect(results[0].offenceCode).toBe('OffCode1');
        expect(results[0].offenceTitle).toBe('Offence Title1');
        expect(results[0].orderIndex).toBe('1');
        expect(results[0].pleaValue).toBe('NOT_GUILTY');
        expect(results[0].verdictCode).toBe('verdict desc1');
        expect(results[0].indicatedPleaValue).toBe('INDICATED_GUILTY');
        // expect(results[0].allocationDecision).toBe('Allocation Decision');
        expect(results[0].convictionDate).toBe('21/03/2020');
        expect(results[0].pleaDate).toBe('2019-11-14');
        expect(results[0].wording).toBe('On 21/10/2018 at Euston Train Station, London robbed Alyssa Hill of her Mangalsutra.');

        expect(results[1].offenceCode).toBe('OffCode2');
        expect(results[1].offenceTitle).toBe('Offence Title2');
        expect(results[1].orderIndex).toBe('2');
        expect(results[1].pleaValue).toBe('NOT_GUILTY');
        expect(results[1].verdictCode).toBe('verdict desc2');
        expect(results[1].indicatedPleaValue).toBe('INDICATED_GUILTY');
        // expect(results[1].allocationDecision).toBe('Allocation Decision 2');
        expect(results[1].convictionDate).toBe('');
        expect(results[1].pleaDate).toBe('2019-11-14');
        expect(results[1].wording).toBe('wordinggggggggg');
    });
});
