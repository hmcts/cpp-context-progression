const CourtSessionMapper = require('../CourtSessionMapper');
const {Hearing} = require('./ModelObjects');
const {HearingDay} = require('./ModelObjects');
const {InformantRegisterSubscription} = require('./ModelObjects');
const context = require('../../../../testing/defaultContext');
const moment = require('moment-timezone');

jest.mock('../DefendantMapper');

describe('CourtSession mapper works correctly', () => {

    test('when hearing and informant register then mapper should give court room', () => {
        const informantRegister = new InformantRegisterSubscription('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        const hearing = new Hearing();
        hearing.courtCentre.roomName = 'Room-1';
        hearing.hearingDays = [new HearingDay('2020-06-19T09:00:00.000Z', '13:00:00')];

        const courtSession = new CourtSessionMapper(context, informantRegister, hearing).build();

        expect(courtSession.courtRoom).toBe('Room-1');
    });

    test('when hearing and informant register then mapper should give hearing start time of earlies sitting day', () => {
        const informantRegister = new InformantRegisterSubscription('31af405e-7b60-4dd8-a244-c24c2d3fa595');
        const hearing = new Hearing();
        const sittingDay = '2020-06-19T09:08:03.001Z';
        hearing.hearingDays = [new HearingDay(sittingDay, '13:00:00'), new HearingDay('2020-06-21T09:00:00.000Z', '14:00:00')];

        const courtSession = new CourtSessionMapper(context, informantRegister, hearing).build();
        const expectedDateTime = moment.tz(sittingDay, "Europe/London").format('YYYY-MM-DDTHH:mm:ss')+'Z';

        expect(courtSession.hearingStartTime).toBe(expectedDateTime);
    });
});