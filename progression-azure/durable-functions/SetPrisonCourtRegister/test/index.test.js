const setPrisonCourtRegister = require('../index');
const context = require('../../testing/defaultContext');

describe('Set Prison Court Register', () => {

    const hearingJson = require('./hearing-results-for-prison-court-register.json');

    test('should return the correct number of defendant', async () => {

        context.bindings = {
            params: {
                hearingResultedObj: hearingJson,

            }
        };

        const prisonCourtRegisterList = await setPrisonCourtRegister(context);

        expect(prisonCourtRegisterList.length).toBe(3);
        expect(prisonCourtRegisterList[0].hearingDate).toBe('2020-01-20T00:00:00Z');
        expect(prisonCourtRegisterList[0].hearingId).toBe('1828f356-f746-4f2d-932b-79ef2df95c80');
        expect(prisonCourtRegisterList[0].registerDefendant.results[0].judicialResult.publishedForNows).toBe(false);
        expect(prisonCourtRegisterList[1].registerDefendant.results[0].judicialResult.publishedForNows).toBe(false);
        expect(prisonCourtRegisterList[1].hearingDate).toBe('2020-01-20T00:00:00Z');
        expect(prisonCourtRegisterList[1].hearingId).toBe('1828f356-f746-4f2d-932b-79ef2df95c80');
        expect(prisonCourtRegisterList[2].registerDefendant.results[0].judicialResult.publishedForNows).toBe(false);
        expect(prisonCourtRegisterList[2].hearingDate).toBe('2020-01-20T00:00:00Z');
        expect(prisonCourtRegisterList[2].hearingId).toBe('1828f356-f746-4f2d-932b-79ef2df95c80');
    });
});