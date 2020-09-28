const setNowVariants = require('../index');
const context = require('../../testing/defaultContext');
const referenceDataService = require('../../NowsHelper/service/ReferenceDataService');

jest.mock('ReferenceDataService');

describe('Set Now Variants', () => {
    const complianceEnforcementJson = require('./compliance-enforcement.json');
    const nowMetadataJson = require('./now-metadata-2.json');

    beforeEach(() => {
        referenceDataService.mockImplementation(() => {
            return {
                getNowMetadata: () => {
                    return Promise.resolve(nowMetadataJson);
                }
            };
        });

        context.log.warn = () => {};
    });

    test('should construct valid now variants', async () => {
        const hearingJsonWithPrimaryResults = require('./hearing-results.json');
        const complianceEnforcementJson = require('./enforcement.json');

        context.bindings = {
            params: {
                hearingResultedJson: hearingJsonWithPrimaryResults,
                complianceEnforcements: complianceEnforcementJson
            }
        };

        const nowVariants = await setNowVariants(context);

        expect(nowVariants.length).toBe(6);
    });

    test('should construct valid now variants based on judicial primary results', async () => {
        const hearingJsonWithPrimaryResults = require('./hearing-results-for-primary-nows.json');

        context.bindings = {
            params: {
                hearingResultedJson: hearingJsonWithPrimaryResults,
                complianceEnforcements: complianceEnforcementJson
            }
        };

        const nowVariants = await setNowVariants(context);

        expect(nowVariants.length).toBe(8);

    });

    test('should construct valid now variants based on judicial primary and non-primary results', async () => {

        const hearingJsonWithPrimaryAndSecondaryResults = require(
            './hearing-results-and-prompts-for-primary-and-secondary-nows.json');

        context.bindings = {
            params: {
                hearingResultedJson: hearingJsonWithPrimaryAndSecondaryResults,
                complianceEnforcements: complianceEnforcementJson
            }
        };

        const nowVariants = await setNowVariants(context);

        expect(nowVariants.length).toBe(6);
    });

    test('should construct valid now variants ', async () => {

        const remandedInCustody = require('./remanded-in-custody-2.json');

        context.bindings = {
            params: {
                hearingResultedJson: remandedInCustody,
                complianceEnforcements: []
            }
        };

        const nowVariants = await setNowVariants(context);

        expect(nowVariants.length).toBe(2);
        expect(nowVariants[0].results.length).toBe(12);
        expect(nowVariants[1].results.length).toBe(12);

    });

    test('should construct sequence number for now variants ', async () => {

        const disqualification = require('./disqualification.json');
        context.bindings = {
            params: {
                hearingResultedJson: disqualification,
                complianceEnforcements: []
            }
        };

        const nowVariants = await setNowVariants(context);
        expect(nowVariants.length).toBe(2);
        expect(nowVariants[0].results.length).toBe(2);
        expect(nowVariants[1].results.length).toBe(2);
        expect(nowVariants[0].results[0].sequence).toBe(200);
        expect(nowVariants[0].results[1].sequence).toBe(undefined);
        expect(nowVariants[1].results[0].sequence).toBe(1900);
        expect(nowVariants[1].results[1].sequence).toBe(100);

    });
});