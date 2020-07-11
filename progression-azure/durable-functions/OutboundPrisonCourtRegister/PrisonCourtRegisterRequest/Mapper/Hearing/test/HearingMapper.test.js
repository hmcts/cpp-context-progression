const HearingMapper = require('../HearingMapper');


describe('HearingMapper builds correctly', () => {
    test('build hearing with all values', () => {
        const hearingResult = require('./hearing.json');
        const prisonCourtRegister = require('./prisonCourtRegister.json');
        const fakeDefendant = {
            'id': '6647df67-a065-4d07-90ba-a8daa064ecc4',
            'defenceOrganisation': {
                'name': 'some org'
            }
        };

        const hearingMapper = new HearingMapper(hearingResult, prisonCourtRegister, fakeDefendant);
        const result = hearingMapper.build();

        expect(result).toBeTruthy();
        expect(result.jurisdiction).toBe(hearingResult.jurisdictionType);
        expect(result.hearingType).toBe(hearingResult.type.description);
        expect(result.defendantPresent).toBe(true);
        expect(result.attendingSolicitorName).toBe(prisonCourtRegister.registerDefendant.associatedDefenceOrganisation.defenceOrganisation.organisation.name.name);
    });

});


