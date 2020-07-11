const ResultMapper = require('../ResultMapper');
const {Result} = require('./ModelObjects');
const {Offence} = require('./ModelObjects');
const {DefendantContextBase} = require('./ModelObjects');
const LEVEL_TYPE = require('../../../../NowsHelper/service/LevelTypeEnum');

describe('Result mapper works correctly', () => {

    test('when defendant level results then mapper should give results', () => {
        const defendantContext = new DefendantContextBase('MASTER_10001');
        defendantContext.results = [
            {
                level: LEVEL_TYPE.DEFENDANT,
                judicialResult: new Result('cjsCode_1', 'Text_1')
            }
        ];

        const results = new ResultMapper(defendantContext).buildDefendantLevelResults();

        expect(results.length).toBe(1);
        expect(results[0].cjsResultCode).toBe('cjsCode_1');
        expect(results[0].resultText).toBe('Text_1');
    });

    test('when defendant level results is empty then mapper should return undefined', () => {
        const defendantContext = new DefendantContextBase('MASTER_10001');
        defendantContext.results = [];

        const results = new ResultMapper(defendantContext).buildDefendantLevelResults();

        expect(results).toBe(undefined);
    });

    test('when multiple defendant level results then mapper should give multiple results', () => {
        const defendantContext = new DefendantContextBase('MASTER_10001');
        defendantContext.results = [
            {
                level: LEVEL_TYPE.DEFENDANT,
                judicialResult: new Result('cjsCode_1', 'Text_1')
            },
            {
                level: LEVEL_TYPE.DEFENDANT,
                judicialResult: new Result('cjsCode_2', 'Text_2')
            }
        ];

        const results = new ResultMapper(defendantContext).buildDefendantLevelResults();

        expect(results.length).toBe(2);
    });

    test('when multiple defendant case level results then mapper should give multiple results', () => {
        const defendantContext = new DefendantContextBase('MASTER_10001');
        defendantContext.results = [
            {
                level: LEVEL_TYPE.CASE,
                judicialResult: new Result('cjsCode_1', 'Text_1'),
                prosecutionCaseId: 'caseId_1'
            },
            {
                level: LEVEL_TYPE.CASE,
                judicialResult: new Result('cjsCode_2', 'Text_2'),
                prosecutionCaseId: 'caseId_1'
            }
        ];

        const results = new ResultMapper(defendantContext).buildDefendantCaseLevelResults('caseId_1');

        expect(results.length).toBe(2);
    });

    test('should filter defendant case level results', () => {
        const defendantContext = new DefendantContextBase('MASTER_10001');
        defendantContext.results = [
            {
                level: LEVEL_TYPE.CASE,
                judicialResult: new Result('cjsCode_1', 'Text_1'),
                prosecutionCaseId: 'caseId_1'
            },
            {
                level: LEVEL_TYPE.CASE,
                judicialResult: new Result('cjsCode_2', 'Text_2'),
                prosecutionCaseId: 'caseId_2'
            }
        ];

        const results = new ResultMapper(defendantContext).buildDefendantCaseLevelResults('caseId_1');

        expect(results.length).toBe(1);
    });

    test('when offence level results then mapper should give results', () => {
        const defendantContext = new DefendantContextBase('MASTER_10001');
        defendantContext.results = [
            {
                level: LEVEL_TYPE.OFFENCE,
                judicialResult: new Result('cjsCode_1', 'Text_1'),
                offenceId: 'offence-id-1'
            }
        ];

        const offence = new  Offence();
        offence.id = 'offence-id-1';

        const results = new ResultMapper(defendantContext).buildOffenceLevelResults(offence);

        expect(results.length).toBe(1);
        expect(results[0].cjsResultCode).toBe('cjsCode_1');
        expect(results[0].resultText).toBe('Text_1');
    });

    test('when multiple offence level results then mapper should give results', () => {
        const defendantContext = new DefendantContextBase('MASTER_10001');
        defendantContext.results = [
            {
                level: LEVEL_TYPE.OFFENCE,
                judicialResult: new Result('cjsCode_1', 'Text_1'),
                offenceId: 'offence-id-1'
            },
            {
                level: LEVEL_TYPE.OFFENCE,
                judicialResult: new Result('cjsCode_2', 'Text_2'),
                offenceId: 'offence-id-1'
            }
        ];

        const offence = new  Offence();
        offence.id = 'offence-id-1';

        const results = new ResultMapper(defendantContext).buildOffenceLevelResults(offence);

        expect(results.length).toBe(2);
    });
});