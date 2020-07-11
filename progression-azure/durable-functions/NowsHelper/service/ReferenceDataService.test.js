const referenceDataService = require('./ReferenceDataService');
const context = require('../../testing/defaultContext');
const axios = require('axios');

jest.mock('axios');

describe('Reference data query', () => {

    beforeEach(() => {
        nowsMetadata = require('../../SetNowVariants/test/now-metadata.json');
        organisationUnitJson = require('./test/organisation-unit.json');
        enforcementAreaJson = require('./test/enforcement-area.json');
        prisonsCustodySuitesJson = require('./test/prisons-custody-suites.json')

        context.bindings = {
            params: {
                cjscppuid: 'undefined'
            }
        };
    });

    test('should fetch nows metadata, if CJSCCPUID is supplied', async () => {
        axios.get.mockImplementation(() => Promise.resolve({data: nowsMetadata}));

        const response = await new referenceDataService().getNowMetadata(context);

        expect(response.nows[0].id).toBe('b4b55110-1d50-11e8-accf-0ed5f89f718b');
        expect(response.nows[0].name).toBe('Warrant for custodial sentence');
        expect(response.nows[0].templateName).toBe('NoticeOrderWarrants');
    });

    test('should not fetch nows metadata, if CJSCCPUID is not supplied', async () => {
        axios.get.mockImplementation(() => Promise.resolve({data: nowsMetadata}));

        try {
            await new referenceDataService().getNowMetadata(context);
        } catch (e) {
            expect(e).toMatch(/No CJSCPPUID supplied/);
        }
    });

    test('should fetch organisation unit, if CJSCCPUID and coutCentreId is supplied', async () => {
        axios.get.mockImplementation(() => Promise.resolve({data: organisationUnitJson}));

        const response = await new referenceDataService().getOrganisationUnit("courtCentreId", context);

        expect(response.id).toBe('f8254db1-1683-483e-afb3-b87fde5a0a26');
        expect(response.oucode).toBe('B01LY00');
        expect(response.lja).toBe('2577');
    });

    test('should fetch enforcement area, if CJSCCPUID and postCode is supplied', async () => {
        axios.get.mockImplementation(() => Promise.resolve({data: enforcementAreaJson}));

        const response = await new referenceDataService().getEnforcementAreaByPostcode("postcode", context);

        expect(response.enforcingCourtCode).toBe(2222);
        expect(response.accountDivisionCode).toBe(1111);
        expect(response.localJusticeArea.nationalCourtCode).toBe('1080');
    });

    test('should fetch enforcement area, if CJSCCPUID and ljaCode is supplied', async () => {
        axios.get.mockImplementation(() => Promise.resolve({data: enforcementAreaJson}));

        const response = await new referenceDataService().getEnforcementAreaByLja("ljaCode", context);

        expect(response.enforcingCourtCode).toBe(2222);
        expect(response.accountDivisionCode).toBe(1111);
        expect(response.localJusticeArea.nationalCourtCode).toBe('1080');
    });

    test('should fetch prisons reference data, if CJSCCPUID is supplied', async () => {
        axios.get.mockImplementation(() => Promise.resolve({data: prisonsCustodySuitesJson}));

        const response = await new referenceDataService().getPrisonsCustodySuites(context);

        expect(response['prisons-custody-suites'][0].id).toBe('70ed3219-378f-3d4c-9bdc-61b399ab95f9');
        expect(response['prisons-custody-suites'][0].name).toBe('HMIRC The Verne');
    });
});

