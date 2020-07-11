const context = require('../../testing/defaultContext');
const setComplianceEnforcement = require('../index');
const moment = require('moment');

describe('Set compliance Enforcement', function () {

    test('single case multiple defendants ', async function () {
        const hearingJson = require(
            './single-case-multiple-defendants-hearing.json');
        context.bindings = {
            params: {
                hearingResultedObj: hearingJson
            }
        };
        const complianceEnforcementResponse = await setComplianceEnforcement(context);
        assertComplianceEnforcementForSingleCaseMultipleDefendant(complianceEnforcementResponse, hearingJson);
    });

    test('multiple cases multiple defendants ', async function () {
        const hearingJson = require(
            './multiple-case-multiple-defendants-hearing.json');
        context.bindings = {
            params: {
                hearingResultedObj: hearingJson
            }
        };
        const complianceEnforcementResponse = await setComplianceEnforcement(context);
        assertComplianceEnforcementForMultipleCases(complianceEnforcementResponse, hearingJson);
    });

    test('single case, multiple defendants with judicial prompts', async function () {
        const hearingJson = require(
            './single-case-multiple-defendants-judicial-prompts-hearing.json');
        context.bindings = {
            params: {
                hearingResultedObj: hearingJson
            }
        };
        const complianceEnforcementResponse = await setComplianceEnforcement(context);
        assertComplianceEnforcementForSingleCaseMultipleDefendant(complianceEnforcementResponse,
                                                                  hearingJson);

    });

    //Test is skipped till we get confirmation whether we support standalone application or not.
    test.skip('application with multiple defendants', async function () {
        const hearingJson = require(
            './application-with-multiple-defendants-hearing.json');
        context.bindings = {
            params: {
                hearing: hearingJson,
                sharedDateTime: moment()
            }
        };
        const complianceEnforcementResponse = await setComplianceEnforcement(context);
        expect(complianceEnforcementResponse).toHaveLength(1);
        expect(complianceEnforcementResponse[0].masterDefendantId)
            .toBe("6647df67-a065-4d07-90ba-a8daa064ecc4");
        expect(complianceEnforcementResponse[0].complianceCorrelationId).toBeDefined();
        expect(complianceEnforcementResponse[0].prosecutionCaseURNPRN).toBe("APR35890458");
        expect(complianceEnforcementResponse[0].prosecutingAuthorityCode).toBeUndefined();
        expect(complianceEnforcementResponse[0].includesGuiltyPlea).toBe(false);
        expect(complianceEnforcementResponse[0].employerResults).toBeUndefined();
        expect(complianceEnforcementResponse[0].paymentTermsResults).toHaveLength(1);
        expect(complianceEnforcementResponse[0].paymentTermsResults[0])
            .toEqual(
                hearingJson.courtApplications[0].applicant.defendant.offences[0].judicialResults[0]);
    });

    test('case level judicial results for defendants', async function () {
        const hearingJson = require(
            './case-level-judicial-results-hearing.json');
        context.bindings = {
            params: {
                hearingResultedObj: hearingJson
            }
        };
        const complianceEnforcementResponse = await setComplianceEnforcement(context);
        assertComplianceEnforcementForCaseLevelJudicialResults(complianceEnforcementResponse,
                                                               hearingJson);
    });

    test('single case, single defendant with minor creditor as defendant', async function () {
        const hearingJson = require(
            './hearing-with-minor-creditor-as-defendant.json');
        context.bindings = {
            params: {
                hearingResultedObj: hearingJson
            }
        };
        const complianceEnforcementResponse = await setComplianceEnforcement(context);
        assertComplianceEnforcementForMinorCreditorAsDefendant(complianceEnforcementResponse,
                                                                  hearingJson);

    });

    test('single case, single defendant with minor creditor as organisation', async function () {
        const hearingJson = require(
            './hearing-with-minor-creditor-as-organisation.json');
        context.bindings = {
            params: {
                hearingResultedObj: hearingJson
            }
        };
        const complianceEnforcementResponse = await setComplianceEnforcement(context);
        assertComplianceEnforcementForMinorCreditorAsOrganisation(complianceEnforcementResponse,
                                                                hearingJson);

    });

    function assertComplianceEnforcementForCaseLevelJudicialResults(complianceEnforcementResponse,
                                                                    hearingJson) {
        expect(complianceEnforcementResponse).toHaveLength(2);
        expect(complianceEnforcementResponse[0].masterDefendantId)
            .toBe("6647df67-a065-4d07-90ba-a8daa064ecc4");
        expect(complianceEnforcementResponse[0].complianceCorrelationId).toBeDefined();
        expect(complianceEnforcementResponse[0].prosecutionCaseURNPRN).toBe("TFL4359536");
        expect(complianceEnforcementResponse[0].prosecutingAuthorityCode).toBe("TFL");
        expect(complianceEnforcementResponse[0].includesGuiltyPlea).toBe(false);
        expect(complianceEnforcementResponse[0].employerResults).toBeUndefined();
        expect(complianceEnforcementResponse[0].collectionOrderResults).toHaveLength(1);
        expect(complianceEnforcementResponse[0].collectionOrderResults[0])
            .toEqual(hearingJson.defendantJudicialResults[0].judicialResult);
        expect(complianceEnforcementResponse[0].reserveTermsResults).toHaveLength(1);
        expect(complianceEnforcementResponse[0].reserveTermsResults[0])
            .toEqual(hearingJson.prosecutionCases[0].defendants[0].offences[0].judicialResults[0]);

        expect(complianceEnforcementResponse[1].masterDefendantId)
            .toBe("ded76309-c912-436b-a21b-a4c4450bc052");
        expect(complianceEnforcementResponse[1].complianceCorrelationId).toBeDefined();
        expect(complianceEnforcementResponse[1].prosecutionCaseURNPRN).toBe("TFL4359536");
        expect(complianceEnforcementResponse[1].prosecutingAuthorityCode).toBe("TFL");
        expect(complianceEnforcementResponse[1].includesGuiltyPlea).toBe(true);
        expect(complianceEnforcementResponse[1].employerResults).toBeUndefined();
        expect(complianceEnforcementResponse[1].collectionOrderResults).toBeUndefined();
        expect(complianceEnforcementResponse[1].reserveTermsResults).toHaveLength(2);
        expect(complianceEnforcementResponse[1].reserveTermsResults[0])
            .toEqual(hearingJson.prosecutionCases[0].defendants[1].offences[0].judicialResults[1]);
        expect(complianceEnforcementResponse[1].reserveTermsResults[1])
            .toEqual(hearingJson.defendantJudicialResults[1].judicialResult);
        // expect(complianceEnforcementResponse[1].impositionResults).toHaveLength(1);
        /*expect(complianceEnforcementResponse[1].impositionResults[0])
            .toEqual(hearingJson.prosecutionCases[0].defendants[1].offences[0].judicialResults[0]);*/

    }

    function assertComplianceEnforcementForSingleCaseMultipleDefendant(complianceEnforcementResponse,
                                                                       hearingJson) {
        expect(complianceEnforcementResponse).toHaveLength(2);
        expect(complianceEnforcementResponse[0].masterDefendantId)
            .toBe("6647df67-a065-4d07-90ba-a8daa064ecc4");
        expect(complianceEnforcementResponse[0].complianceCorrelationId).toBeDefined();
        expect(complianceEnforcementResponse[0].prosecutionCaseURNPRN).toBe("TFL4359536");
        expect(complianceEnforcementResponse[0].prosecutingAuthorityCode).toBe("TFL");
        expect(complianceEnforcementResponse[0].includesGuiltyPlea).toBe(false);
        expect(complianceEnforcementResponse[0].employerResults).toBeUndefined();
        expect(complianceEnforcementResponse[0].paymentTermsResults).toHaveLength(1);
        expect(complianceEnforcementResponse[0].paymentTermsResults[0])
            .toEqual(hearingJson.prosecutionCases[0].defendants[0].offences[0].judicialResults[0]);

        expect(complianceEnforcementResponse[1].masterDefendantId)
            .toBe("ded76309-c912-436b-a21b-a4c4450bc052");
        expect(complianceEnforcementResponse[1].complianceCorrelationId).toBeDefined();
        expect(complianceEnforcementResponse[1].prosecutionCaseURNPRN).toBe("TFL4359536");
        expect(complianceEnforcementResponse[1].prosecutingAuthorityCode).toBe("TFL");
        expect(complianceEnforcementResponse[1].includesGuiltyPlea).toBe(true);
        expect(complianceEnforcementResponse[1].employerResults).toHaveLength(1);
        expect(complianceEnforcementResponse[1].employerResults[0])
            .toEqual(hearingJson.prosecutionCases[0].defendants[1].offences[0].judicialResults[1])
        expect(complianceEnforcementResponse[1].paymentTermsResults).toBeUndefined();
    }

    function assertComplianceEnforcementForMultipleCases(complianceEnforcementResponse,
                                                         hearingJson) {
        expect(complianceEnforcementResponse).toHaveLength(4);

        expect(complianceEnforcementResponse[0].masterDefendantId)
            .toBe("6647df67-a065-4d07-90ba-a8daa064ecc4");
        expect(complianceEnforcementResponse[0].complianceCorrelationId).toBeDefined();
        expect(complianceEnforcementResponse[0].prosecutionCaseURNPRN).toBe("TFL4359536");
        expect(complianceEnforcementResponse[0].prosecutingAuthorityCode).toBe("TFL");
        expect(complianceEnforcementResponse[0].includesGuiltyPlea).toBe(false);
        expect(complianceEnforcementResponse[0].employerResults).toBeUndefined();
        expect(complianceEnforcementResponse[0].paymentTermsResults).toHaveLength(1);
        expect(complianceEnforcementResponse[0].paymentTermsResults[0])
            .toEqual(hearingJson.prosecutionCases[0].defendants[0].offences[0].judicialResults[0]);

        expect(complianceEnforcementResponse[1].masterDefendantId)
            .toBe("ded76309-c912-436b-a21b-a4c4450bc052");
        expect(complianceEnforcementResponse[1].complianceCorrelationId).toBeDefined();
        expect(complianceEnforcementResponse[1].prosecutionCaseURNPRN).toBe("TFL4359536");
        expect(complianceEnforcementResponse[1].prosecutingAuthorityCode).toBe("TFL");
        expect(complianceEnforcementResponse[1].includesGuiltyPlea).toBe(true);
        expect(complianceEnforcementResponse[1].employerResults).toHaveLength(1);
        expect(complianceEnforcementResponse[1].employerResults[0])
            .toEqual(hearingJson.prosecutionCases[0].defendants[1].offences[0].judicialResults[1]);
        expect(complianceEnforcementResponse[1].paymentTermsResults).toBeUndefined();

        expect(complianceEnforcementResponse[2].masterDefendantId)
            .toBe("f21c2bdc-1602-46b1-bd4c-44199a0aeabc");
        expect(complianceEnforcementResponse[2].complianceCorrelationId).toBeDefined();
        expect(complianceEnforcementResponse[2].prosecutionCaseURNPRN).toBe("TVL298320922");
        expect(complianceEnforcementResponse[2].prosecutingAuthorityCode).toBe("TVL");
        expect(complianceEnforcementResponse[2].includesGuiltyPlea).toBe(false);
        expect(complianceEnforcementResponse[2].employerResults).toBeUndefined();
        expect(complianceEnforcementResponse[2].paymentTermsResults).toHaveLength(1);
        expect(complianceEnforcementResponse[2].paymentTermsResults[0])
            .toEqual(hearingJson.prosecutionCases[1].defendants[0].offences[0].judicialResults[0]);

        expect(complianceEnforcementResponse[3].masterDefendantId)
            .toBe("b48a6e2e-a3e5-47e3-8243-ac17b378d04d");
        expect(complianceEnforcementResponse[3].complianceCorrelationId).toBeDefined();
        expect(complianceEnforcementResponse[3].prosecutionCaseURNPRN).toBe("TVL298320922");
        expect(complianceEnforcementResponse[3].prosecutingAuthorityCode).toBe("TVL");
        expect(complianceEnforcementResponse[3].includesGuiltyPlea).toBe(true);
        expect(complianceEnforcementResponse[3].employerResults).toHaveLength(1);
        expect(complianceEnforcementResponse[3].employerResults[0])
            .toEqual(hearingJson.prosecutionCases[1].defendants[1].offences[0].judicialResults[1]);
        expect(complianceEnforcementResponse[3].paymentTermsResults).toHaveLength(1);
        expect(complianceEnforcementResponse[3].paymentTermsResults[0])
            .toEqual(hearingJson.prosecutionCases[1].defendants[1].offences[0].judicialResults[0]);
    }

    function assertComplianceEnforcementForMinorCreditorAsDefendant(complianceEnforcementResponse,
                                                                       hearingJson) {
        expect(complianceEnforcementResponse).toHaveLength(1);
        expect(complianceEnforcementResponse[0].masterDefendantId)
            .toBe("0c9ba7ad-1e8e-4857-a4e1-bf0771466957");
        expect(complianceEnforcementResponse[0].complianceCorrelationId).toBeDefined();
        expect(complianceEnforcementResponse[0].prosecutionCaseURNPRN).toBe("TVL53544BJRMP");
        expect(complianceEnforcementResponse[0].prosecutingAuthorityCode).toBe("TVL");
        expect(complianceEnforcementResponse[0].includesGuiltyPlea).toBe(true);
        expect(complianceEnforcementResponse[0].employerResults).toBeUndefined();
        expect(complianceEnforcementResponse[0].minorCreditorResults[0])
            .toEqual(hearingJson.prosecutionCases[0].defendants[0].defendantCaseJudicialResults[0]);
    }

    function assertComplianceEnforcementForMinorCreditorAsOrganisation(complianceEnforcementResponse,
                                                                    hearingJson) {
        expect(complianceEnforcementResponse).toHaveLength(1);
        expect(complianceEnforcementResponse[0].masterDefendantId)
            .toBe("6cd470dc-0de9-4f50-8740-c65b74e58893");
        expect(complianceEnforcementResponse[0].complianceCorrelationId).toBeDefined();
        expect(complianceEnforcementResponse[0].prosecutionCaseURNPRN).toBe("TVL53969I766M");
        expect(complianceEnforcementResponse[0].prosecutingAuthorityCode).toBe("TVL");
        expect(complianceEnforcementResponse[0].includesGuiltyPlea).toBe(true);
        expect(complianceEnforcementResponse[0].employerResults).toBeUndefined();
        expect(complianceEnforcementResponse[0].minorCreditorResults[0])
            .toEqual(hearingJson.prosecutionCases[0].defendants[0].offences[0].judicialResults[0]);
    }

});
