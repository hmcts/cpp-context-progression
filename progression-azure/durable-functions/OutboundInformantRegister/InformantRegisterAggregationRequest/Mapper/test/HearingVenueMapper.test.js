const HearingVenueMapper = require('../HearingVenueMapper');
const {Hearing} = require('./ModelObjects');
const {InformantRegisterSubscription} = require('./ModelObjects');
const context = require('../../../../testing/defaultContext');

jest.mock('../CourtSessionMapper');

describe('HearingVenue mapper works correctly', () => {

    test('when hearing and informant register then mapper should give court center name', () => {
        const informantRegister = new InformantRegisterSubscription();
        const hearing = new Hearing();
        hearing.courtCentre.name = 'CourtCenter';

        const hearingVenue = new HearingVenueMapper(context, informantRegister, hearing).build();

        expect(hearingVenue.courtHouse).toBe('CourtCenter');
    });


    test('when lja name then mapper should set ljaName', () => {
        const informantRegister = new InformantRegisterSubscription();
        const hearing = new Hearing();
        hearing.courtCentre.lja = {};
        hearing.courtCentre.lja.ljaName = 'LJA-Name';

        const hearingVenue = new HearingVenueMapper(context, informantRegister, hearing).build();

        expect(hearingVenue.ljaName).toBe('LJA-Name');
    });

});