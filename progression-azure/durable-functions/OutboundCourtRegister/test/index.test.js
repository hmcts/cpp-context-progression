const OutboundCourtRegister = require('../index');
const context = require('../../testing/defaultContext');

describe('Outbound court register', () => {

    let fakeHearingResultedJson;
    let fakeCourtRegisterFragment;

    beforeEach(() => {
        jest.resetModules();
        fakeHearingResultedJson = require('./hearing-resulted.json');
        fakeCourtRegisterFragment = require('./court-register-fragment.json');
    });

    test('Should return null if no subscriptions', async () => {

        fakeCourtRegisterFragment.matchedSubscriptions = null;

        context.bindings = {
            params: {
                hearingResultedObj: fakeHearingResultedJson,
                courtRegisterSubscriptions: fakeCourtRegisterFragment
            }
        };

        const courtRegisterFragment = await OutboundCourtRegister(context);
        expect(courtRegisterFragment).toBe(null);
    });

    test('Should return null if no youth defendants', async () => {

        fakeCourtRegisterFragment.registerDefendants[0].isYouthDefendant = false;

        context.bindings = {
            params: {
                hearingResultedObj: fakeHearingResultedJson,
                courtRegisterSubscriptions: fakeCourtRegisterFragment
            }
        };

        const courtRegisterFragment = await OutboundCourtRegister(context);
        expect(courtRegisterFragment).toBe(null);
    });

    test('Should return a valid outbound court register', async () => {

        const cases = new Set();
        cases.add('c10e3b71-6a6d-45ef-9b62-34df4d54971a')
        fakeCourtRegisterFragment.registerDefendants[0].cases = cases;
        fakeCourtRegisterFragment.registerDate = '2020-06-01T11:00:00Z';
        fakeCourtRegisterFragment.hearingDate = '2019-02-01T00:00:00Z';
        fakeCourtRegisterFragment.hearingId = '1828f356-f746-4f2d-932b-79ef2df95c80';

        context.bindings = {
            params: {
                hearingResultedObj: fakeHearingResultedJson,
                courtRegisterSubscriptions: fakeCourtRegisterFragment
            }
        };

        const courtRegisterFragment = await OutboundCourtRegister(context);
        expect(courtRegisterFragment).toBeTruthy();
        expect(courtRegisterFragment.courtCentreId).toBe(fakeCourtRegisterFragment.courtCenterId);
        expect(courtRegisterFragment.fileName).toContain('.pdf');
        expect(courtRegisterFragment.registerDate).toBe('2020-06-01T11:00:00Z');
        expect(courtRegisterFragment.hearingDate).toBe('2019-02-01T00:00:00Z');
        expect(courtRegisterFragment.hearingId).toBe('1828f356-f746-4f2d-932b-79ef2df95c80');
    });

});