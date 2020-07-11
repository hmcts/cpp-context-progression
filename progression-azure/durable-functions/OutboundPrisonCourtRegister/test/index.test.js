const PrisonCourtRegisterRequestBuilder = require('../index');
const context = require('../../testing/defaultContext');
const referenceDataService = require('../../NowsHelper/service/ReferenceDataService');

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

    test('Should  not return null if there is a recipient', async () => {

        const fakePrisonCourtRegisterFragment = require('./prison-court-register-full.json');
        const fakeHearingResultedJson = require('./hearing-with-results-full-case.json');

        context.bindings = {
            params: {
                hearingResultedObj: fakeHearingResultedJson.hearing,
                prisonCourtRegisterSubscriptions: fakePrisonCourtRegisterFragment
            }
        };

        const prisonCourtRegisterRequest = await PrisonCourtRegisterRequestBuilder(context);
        expect(prisonCourtRegisterRequest).toHaveLength(1);
        expect(prisonCourtRegisterRequest[0].hearingId).toBe("598bbda4-f99a-4fa6-958f-10e70ae57858");
        expect(prisonCourtRegisterRequest[0].hearingDate).toBe("2020-03-25T01:00:00Z");
        expect(prisonCourtRegisterRequest[0].recipients[0].recipientName).toBe("HMP Dovegate");
        expect(prisonCourtRegisterRequest[0].recipients[0].emailAddress1).toBe("Hmpdovegate.warrants@serco.cjsm.net");
        expect(prisonCourtRegisterRequest[0].recipients[0].emailAddress2).toBe("reception.dovegate@justice.gov.uk");
        expect(prisonCourtRegisterRequest[0].recipients[0].emailTemplateName).toBe("pcr_standard");
    });

});