const RecipientMapper = require('../RecipientMapper');
const context = require('../../../../../testing/defaultContext');
const referenceDataService = require('../../../../../NowsHelper/service/ReferenceDataService');

jest.mock('ReferenceDataService');

describe('Outbound prison court register', () => {
    const prisonCustodySuitesJson = require('./prisons-custody-suites.json')
    beforeEach(() => {
        referenceDataService.mockImplementation(() => {
            return {
                getPrisonsCustodySuites: () => {
                    return prisonCustodySuitesJson;
                }
            };
        });
    });

    test('Should return null if there is no recipient', async () => {

        const fakePrisonCourtRegisterFragment = require('./prison-court-register-full.json');
        const fakeHearingResultedJson = require('./hearing-with-results-full-case.json');

        context.bindings = {
            params: {
                hearingResultedObj: fakeHearingResultedJson.hearing,
                prisonCourtRegisterSubscriptions: fakePrisonCourtRegisterFragment
            }
        };

        const recipients = await new RecipientMapper(fakePrisonCourtRegisterFragment,prisonCustodySuitesJson,fakeHearingResultedJson).build();
        expect(recipients.length).toBe(1)
    });

});