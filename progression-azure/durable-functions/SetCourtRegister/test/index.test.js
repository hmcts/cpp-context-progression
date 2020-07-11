const setCourtRegister = require('../index');
const context = require('../../testing/defaultContext');

describe('Set Court Register', () => {

    const hearingJson = require(
        './hearing-results-for-court-register.json');

    test('should return the correct court register fragment', async () => {

        context.bindings = {
            params: {
                hearingResultedObj: hearingJson.hearing,
                sharedTime: '2020-06-01T10:00:00Z'
            }
        };

        const courtRegisterFragment = await setCourtRegister(context);

        expect(courtRegisterFragment.registerDefendants.length).toBe(1);
        expect(courtRegisterFragment.registerDefendants[0].results.length).toBe(4);
        expect(courtRegisterFragment.registerDefendants[0].results[0].judicialResult.publishedForNows).toBe(false);
        expect(courtRegisterFragment.registerDefendants[0].results[1].judicialResult.publishedForNows).toBe(false);
        expect(courtRegisterFragment.registerDefendants[0].results[2].judicialResult.publishedForNows).toBe(false);
        expect(courtRegisterFragment.registerDefendants[0].results[3].judicialResult.publishedForNows).toBe(false);
        expect(courtRegisterFragment.registerDate).toBe('2020-06-01T11:00:00Z');
        expect(courtRegisterFragment.hearingDate).toBe('2020-01-20T00:00:00Z');
        expect(courtRegisterFragment.hearingId).toBe('1828f356-f746-4f2d-932b-79ef2df95c80');
    });
});
