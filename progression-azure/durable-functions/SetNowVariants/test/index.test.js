const setNowVariants = require('../index');
const context = require('../../testing/defaultContext');
const referenceDataService = require('../../NowsHelper/service/ReferenceDataService');

jest.mock('ReferenceDataService');

describe('Set Now Variants', () => {
    const complianceEnforcementJson = require('./compliance-enforcement.json');
    const nowMetadataJson = require('./now-metadata.json');

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

        expect(nowVariants.length).toBe(5);
    });

    test('should construct valid now variants based on judicial primary results', async () => {
        const hearingJsonWithPrimaryResults = require(
            './hearing-results-for-primary-nows.json');

        context.bindings = {
            params: {
                hearingResultedJson: hearingJsonWithPrimaryResults,
                complianceEnforcements: complianceEnforcementJson
            }
        };

        const nowVariants = await setNowVariants(context);

        expect(nowVariants.length).toBe(4);

        assertPrimaryNowVariants(nowVariants);
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

        expect(nowVariants.length).toBe(3);

        assertPrimaryAndSecondaryNowVariants(nowVariants);
    });

    function assertPrimaryNowVariants(nowVariants) {
        expect(nowVariants[0].masterDefendantId).toBe("6647df67-a065-4d07-90ba-a8daa064ecc4");
        expect(nowVariants[0].results[0].judicialResultTypeId).toBe("cfbe83c8-935b-4410-a68c-49452519f2d3");
        // expect(nowVariants[0].results[0].primary).toBe(true);
        expect(nowVariants[0].complianceCorrelationId).toBe("4b5f8fe2-c5a0-43f2-9b06-4db0293f1696");
        expect(nowVariants[0].amendmentDate).toBe("2019-06-24");
        expect(nowVariants[0].orderDate).toBe("2020-04-20");
        expect(nowVariants[0].vocabulary.allNonCustodialResults).toBe(true);

        expect(nowVariants[1].masterDefendantId).toBe("ded76309-c912-436b-a21b-a4c4450bc052");
        expect(nowVariants[1].results[0].judicialResultTypeId).toBe("96349367-2d04-4265-978f-6c6b417497fd");
        // expect(nowVariants[1].results[0].primary).toBe(true);
        expect(nowVariants[1].results[1].judicialResultTypeId).toBe("3f608dba-20ad-4710-bebc-d78b4b3ff08d");
        // expect(nowVariants[1].results[1].primary).toBe(true);
        expect(nowVariants[1].orderDate).toBe("2020-04-20");
        expect(nowVariants[1].complianceCorrelationId).toBe("9137025b-aba3-41fa-b761-9ba78eb47982");
        expect(nowVariants[1].vocabulary.allNonCustodialResults).toBe(true);

        expect(nowVariants[2].masterDefendantId).toBe("f21c2bdc-1602-46b1-bd4c-44199a0aeabc");
        expect(nowVariants[2].results[0].judicialResultTypeId).toBe("4f640ea6-88d4-4a3a-b816-ff5a79eaaa14");
        // expect(nowVariants[2].results[0].primary).toBe(true);
        expect(nowVariants[2].orderDate).toBe("2020-04-20");
        expect(nowVariants[2].vocabulary.allNonCustodialResults).toBe(true);

        expect(nowVariants[3].masterDefendantId).toBe("b48a6e2e-a3e5-47e3-8243-ac17b378d04d");
        expect(nowVariants[3].results[0].judicialResultTypeId).toBe("cf70d7ed-8049-4fe2-b02f-5a23b5e39184");
        // expect(nowVariants[3].results[0].primary).toBe(true);
        expect(nowVariants[3].results[1].judicialResultTypeId).toBe("abb95a52-2a75-40c3-8d3f-a1d75a199c47");
        // expect(nowVariants[3].results[1].primary).toBe(true);
        expect(nowVariants[3].orderDate).toBe("2020-04-21");
        expect(nowVariants[3].complianceCorrelationId).toBe("468af24f-24e8-47d4-b810-23327a762973");
        expect(nowVariants[3].vocabulary.allNonCustodialResults).toBe(true);

    }

    function assertPrimaryAndSecondaryNowVariants(nowVariants) {
        expect(nowVariants[0].masterDefendantId).toBe("6647df67-a065-4d07-90ba-a8daa064ecc4");
        expect(nowVariants[0].results[0].judicialResultTypeId).toBe("cfbe83c8-935b-4410-a68c-49452519f2d3");
        // expect(nowVariants[0].results[0].primary).toBe(true);
        // expect(nowVariants[0].results[1].judicialResultTypeId).toBe("c514dcec-804c-11e8-adc0-fa7ae01bbebc"); TODO
        // expect(nowVariants[0].results[1].primary).toBe(false); TODO
        expect(nowVariants[0].complianceCorrelationId).toBe("4b5f8fe2-c5a0-43f2-9b06-4db0293f1696");

        expect(nowVariants[1].masterDefendantId).toBe("ded76309-c912-436b-a21b-a4c4450bc052");
        expect(nowVariants[1].results[0].judicialResultTypeId).toBe("96349367-2d04-4265-978f-6c6b417497fd");
        // expect(nowVariants[1].results[0].primary).toBe(true);
        // expect(nowVariants[1].results[1].judicialResultTypeId).toBe("abb95a52-2a75-40c3-8d3f-a1d75a199c48"); TODO
        // expect(nowVariants[1].results[1].primary).toBe(false); TODO
        expect(nowVariants[1].complianceCorrelationId).toBe(undefined);

        expect(nowVariants[2].masterDefendantId).toBe("b48a6e2e-a3e5-47e3-8243-ac17b378d04d");
        expect(nowVariants[2].results[0].judicialResultTypeId).toBe("cf70d7ed-8049-4fe2-b02f-5a23b5e39184");
        // expect(nowVariants[2].results[0].primary).toBe(true);
        // expect(nowVariants[2].results[1].judicialResultTypeId).toBe("6cb15971-c945-4398-b7c9-3f8b743a4de3"); TODO
        // expect(nowVariants[2].results[1].primary).toBe(false); TODO
        // expect(nowVariants[2].results[2].judicialResultTypeId).toBe("abb95a52-2a75-40c3-8d3f-a1d75a199c48"); TODO
        // expect(nowVariants[2].results[2].primary).toBe(true); TODO
        expect(nowVariants[2].complianceCorrelationId).toBe("468af24f-24e8-47d4-b810-23327a762973");
    }
});