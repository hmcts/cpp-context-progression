const DefendantMapper = require('../DefendantMapper');
const {Hearing} = require('./ModelObjects');
const {InformantRegisterSubscription} = require('./ModelObjects');
const {ProsecutionCase} = require('./ModelObjects');
const {Defendant} = require('./ModelObjects');
const {DefendantContextBase} = require('./ModelObjects');
const context = require('../../../../testing/defaultContext');

jest.mock('../OffenceMapper');
jest.mock('../ResultMapper');

describe('Defendant mapper works correctly', () => {
    test('when all information given should populate the personal details of defendant', () => {

        const informantRegister = new InformantRegisterSubscription('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        informantRegister.registerDefendants = [{
            'masterDefendantId' : 'MASTER_10001',
            'cases': ['caseId1']
        }];
        const defendant = new Defendant();
        defendant.masterDefendantId = 'MASTER_10001';
        defendant.personDefendant = {
            personDetails: {
                firstName: 'First',
                middleName: 'Middle',
                lastName: 'Last',
                title: 'title',
                nationalityCode: 'GB',
                dateOfBirth: '01/01/1900',
                address: {
                    address1: 'A-1',
                    address2: 'A-2',
                    address3: 'A-3',
                    address4: 'A-4',
                    address5: 'A-5',
                    postcode: 'PostCode'
                }
            }
        };

        const prosecutionCase = new ProsecutionCase('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        prosecutionCase.defendants = [defendant];
        prosecutionCase.id = 'caseId1';

        const hearing = new Hearing();
        hearing.prosecutionCases = [prosecutionCase];

        const defendants = new DefendantMapper(context, informantRegister, hearing).build();

        expect(defendants.length).toBe(1);
        expect(defendants[0].name).toBe('First Middle Last');
        expect(defendants[0].dateOfBirth).toBe('01/01/1900');
        expect(defendants[0].nationality).toBe('GB');
        expect(defendants[0].address1).toBe('A-1');
        expect(defendants[0].address2).toBe('A-2');
        expect(defendants[0].address3).toBe('A-3');
        expect(defendants[0].address4).toBe('A-4');
        expect(defendants[0].address5).toBe('A-5');
        expect(defendants[0].firstName).toBe('First');
        expect(defendants[0].lastName).toBe('Last');
        expect(defendants[0].title).toBe('title');
        expect(defendants[0].postCode).toBe('PostCode');
        expect(defendants[0].prosecutionCasesOrApplications.length).toBe(1);
        expect(defendants[0].prosecutionCasesOrApplications[0].caseOrApplicationReference).toBe('caseURN');
    });

    test('when the case have two defendant then mapper return two defendant', () => {

        const informantRegister = new InformantRegisterSubscription('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        informantRegister.registerDefendants = [new DefendantContextBase('MASTER_10001'),
            new DefendantContextBase('MASTER_10002')];

        const defendant = new Defendant();
        defendant.masterDefendantId = 'MASTER_10001';
        defendant.personDefendant = {
            personDetails: {
                firstName: 'Luis',
            }
        };

        const defendant2 = new Defendant();
        defendant2.masterDefendantId = 'MASTER_10002';
        defendant2.personDefendant = {
            personDetails: {
                firstName: 'Anna',
            }
        };

        const prosecutionCase = new ProsecutionCase('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        prosecutionCase.defendants = [defendant, defendant2];

        const hearing = new Hearing();
        hearing.prosecutionCases = [prosecutionCase];

        const defendants = new DefendantMapper(context, informantRegister, hearing).build();
        expect(defendants.length).toBe(2);
        expect(defendants[0].name).toBe('Luis');
        expect(defendants[1].name).toBe('Anna');
        expect(defendants[0].firstName).toBe('Luis');
        expect(defendants[1].firstName).toBe('Anna');
    });

    test('when multiple cases_same_prosecutionAuthId have one defendant for each then mapper return two defendant', () => {

        const informantRegister = new InformantRegisterSubscription('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        informantRegister.registerDefendants = [{ 'masterDefendantId' : 'MASTER_10001'}, {'masterDefendantId' : 'MASTER_10002'}];

        const defendant = new Defendant();
        defendant.masterDefendantId = 'MASTER_10001';
        defendant.personDefendant = {
            personDetails: {
                firstName: 'Luis',
            }
        };

        const defendant2 = new Defendant();
        defendant2.masterDefendantId = 'MASTER_10002';
        defendant2.personDefendant = {
            personDetails: {
                firstName: 'Anna',
            }
        };

        const prosecutionCase = new ProsecutionCase('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        prosecutionCase.defendants = [defendant];

        const prosecutionCase2 = new ProsecutionCase('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        prosecutionCase2.defendants = [defendant2];

        const hearing = new Hearing();
        hearing.prosecutionCases = [prosecutionCase, prosecutionCase2];

        const defendants = new DefendantMapper(context, informantRegister, hearing).build();
        expect(defendants.length).toBe(2);
    });

    test('when multiple cases_same_prosecutionAuthId have same defendant then mapper return one defendant', () => {

        const informantRegister = new InformantRegisterSubscription('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        informantRegister.registerDefendants = [{ 'masterDefendantId' : 'MASTER_10001'}];
        const defendant = new Defendant();
        defendant.masterDefendantId = 'MASTER_10001';
        defendant.personDefendant = {
            personDetails: {
                firstName: 'Luis',
            }
        };

        const defendant2 = new Defendant();
        defendant2.masterDefendantId = 'MASTER_10001';
        defendant2.personDefendant = {
            personDetails: {
                firstName: 'Luis',
            }
        };

        const prosecutionCase = new ProsecutionCase('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        prosecutionCase.defendants = [defendant];

        const prosecutionCase2 = new ProsecutionCase('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        prosecutionCase2.defendants = [defendant2];

        const hearing = new Hearing();
        hearing.prosecutionCases = [prosecutionCase, prosecutionCase2];

        const defendants = new DefendantMapper(context, informantRegister, hearing).build();
        expect(defendants.length).toBe(1);
    });

    test('when multiple cases_NOT_same_prosecutionAuthId then mapper return defendants from the same prosecutionAuthId only', () => {

        const informantRegister = new InformantRegisterSubscription('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        informantRegister.registerDefendants = [{ 'masterDefendantId' : 'MASTER_10001'}];

        const defendant = new Defendant();
        defendant.masterDefendantId = 'MASTER_10001';
        defendant.personDefendant = {
            personDetails: {
                firstName: 'Luis',
            }
        };

        const defendant2 = new Defendant();
        defendant2.masterDefendantId = 'MASTER_10001';
        defendant2.personDefendant = {
            personDetails: {
                firstName: 'Luis',
            }
        };

        const prosecutionCase = new ProsecutionCase('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        prosecutionCase.defendants = [defendant];

        const prosecutionCase2 = new ProsecutionCase('31af405e-7b60-4dd8-a244-different');
        prosecutionCase2.defendants = [defendant2];

        const hearing = new Hearing();
        hearing.prosecutionCases = [prosecutionCase, prosecutionCase2];

        const defendants = new DefendantMapper(context, informantRegister, hearing).build();
        expect(defendants.length).toBe(1);
    });

    test('when multiple cases_same_prosecutionAuthId have same defendant for each then defendant will have two arrestSummonsNumber', () => {

        const informantRegister = new InformantRegisterSubscription('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        informantRegister.registerDefendants = [new DefendantContextBase('MASTER_10001')];

        const defendant = new Defendant();
        defendant.masterDefendantId = 'MASTER_10001';
        defendant.personDefendant = {
            arrestSummonsNumber: 'ASN_0001',
            personDetails: {
                firstName: 'Luis',
            }
        };

        const defendant2 = new Defendant();
        defendant2.masterDefendantId = 'MASTER_10001';
        defendant2.personDefendant = {
            arrestSummonsNumber: 'ASN_0002',
            personDetails: {
                firstName: 'Luis',
            }
        };

        const prosecutionCase = new ProsecutionCase('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        prosecutionCase.defendants = [defendant];

        const prosecutionCase2 = new ProsecutionCase('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        prosecutionCase2.defendants = [defendant2];

        const hearing = new Hearing();
        hearing.prosecutionCases = [prosecutionCase, prosecutionCase2];

        const defendants = new DefendantMapper(context, informantRegister, hearing).build();
        expect(defendants.length).toBe(1);
    });
});