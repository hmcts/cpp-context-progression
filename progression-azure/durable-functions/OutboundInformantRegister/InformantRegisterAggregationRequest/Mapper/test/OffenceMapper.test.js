const OffenceMapper = require('../OffenceMapper');
const {Hearing} = require('./ModelObjects');
const {InformantRegisterSubscription} = require('./ModelObjects');
const {ProsecutionCase} = require('./ModelObjects');
const {Defendant} = require('./ModelObjects');
const {Offence} = require('./ModelObjects');
const {DefendantContextBase} = require('./ModelObjects');

describe('Offence mapper works correctly', () => {

    test('when defendant has offence then mapper should give offence', () => {
        const informantRegister = new InformantRegisterSubscription('31af405e-7b60-4dd8-a244-c24c2d3fa595');

        const offence = new Offence('Code', 1001, 'Title');
        offence.verdict = {};
        offence.plea = {pleaValue: 'plea'};
        offence.verdict.verdictType = {verdictCode: 'verdict-code', description: 'verdict-desc'};
        const defendant = new Defendant([offence]);
        defendant.masterDefendantId = 'MASTER_10001';


        const prosecutionCase = new ProsecutionCase('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        prosecutionCase.defendants = [defendant];

        const hearing = new Hearing();
        hearing.prosecutionCases = [prosecutionCase];

        const defendantContext = new DefendantContextBase('MASTER_10001');

        const offences = new OffenceMapper(hearing, informantRegister, defendantContext).build();

        expect(offences.length).toBe(1);
        expect(offences[0].offenceCode).toBe('Code');
        expect(offences[0].orderIndex).toBe(1001);
        expect(offences[0].offenceTitle).toBe('Title');
        expect(offences[0].pleaValue).toBe('plea');
        expect(offences[0].verdictCode).toBe('verdict-desc');
    });

    test('when defendant has multiple offence then mapper should give multiple offence', () => {
        const informantRegister = new InformantRegisterSubscription('31af405e-7b60-4dd8-a244-c24c2d3fa595');

        const offence = new Offence('Code', 1001, 'Title');
        offence.verdict = {};
        offence.plea = {pleaValue: 'plea'};
        offence.verdict.verdictType = {verdictCode: 'verdict-code'};
        const offence2 = Object.assign({}, offence);
        const defendant = new Defendant([offence, offence2]);
        defendant.masterDefendantId = 'MASTER_10001';

        const prosecutionCase = new ProsecutionCase('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        prosecutionCase.defendants = [defendant];

        const hearing = new Hearing();
        hearing.prosecutionCases = [prosecutionCase];

        const defendantContext = new DefendantContextBase('MASTER_10001');

        const offences = new OffenceMapper(hearing, informantRegister, defendantContext).build();

        expect(offences.length).toBe(2);
    });

    test('when defendant has multiple offence different case then mapper should give multiple offence', () => {
        const informantRegister = new InformantRegisterSubscription('31af405e-7b60-4dd8-a244-c24c2d3fa595');

        const offence = new Offence('Code', 1001, 'Title');
        offence.verdict = {};
        offence.plea = {pleaValue: 'plea'};
        offence.verdict.verdictType = {verdictCode: 'verdict-code'};
        const defendant = new Defendant([offence]);
        defendant.masterDefendantId = 'MASTER_10001';
        const prosecutionCase = new ProsecutionCase('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        prosecutionCase.defendants = [defendant];

        const offence2 = new Offence('Code', 1001, 'Title');
        offence2.verdict = {};
        offence2.plea = {pleaValue: 'plea'};
        offence2.verdict.verdictType = {verdictCode: 'verdict-code'};
        const defendant2 = new Defendant([offence2]);
        defendant2.masterDefendantId = 'MASTER_10001';
        const prosecutionCase2 = new ProsecutionCase('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        prosecutionCase2.defendants = [defendant2];

        const hearing = new Hearing();
        hearing.prosecutionCases = [prosecutionCase, prosecutionCase2];

        const defendantContext = new DefendantContextBase('MASTER_10001');

        const offences = new OffenceMapper(hearing, informantRegister, defendantContext).build();

        expect(offences.length).toBe(2);
    });
});