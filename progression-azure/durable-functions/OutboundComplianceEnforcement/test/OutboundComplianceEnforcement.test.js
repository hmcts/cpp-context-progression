const OutboundComplianceEnforcement = require('../index');
const context = require('../../testing/defaultContext');
const moment = require('moment');

describe('Outbound compliance enforcement works correctly', ()=>{
    test('build staging enforcement request', async ()=>{
        const hearingJson = require(
            './case-level-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require(
            './compliance-enforcement-array.json');
        const expectedResponse = require('./expected-staging-enforcement-request.json');
        context.bindings = {
            params: {
                complianceEnforcements: complianceEnforcementReserveTermsJson,
                hearingResultedJson: hearingJson
            }
        };
        const outboundComplianceEnforcement = await OutboundComplianceEnforcement(context);
        expect(JSON.stringify(outboundComplianceEnforcement)).toMatch(JSON.stringify(expectedResponse));
    });

    test('build staging enforcement request for Sending Slavery and Trafficking Reparation Order', async ()=>{
        const hearingJson = require('./stro-judicial-results-hearing.json');
        const complianceEnforcementReserveTermsJson = require('./compliance-enforcement-array-for-stro.json');
        let expectedResponse = require('./stro-expected-staging-enforcement-request.json');
        context.bindings = {
            params: {
                complianceEnforcements: complianceEnforcementReserveTermsJson,
                hearingResultedJson: hearingJson
            }
        };
        const outboundComplianceEnforcement = await OutboundComplianceEnforcement(context);
        expectedResponse.paymentTerms.instalmentStartDate = moment().add(90, 'd').format('YYYY-MM-DD');
        expect(JSON.stringify(outboundComplianceEnforcement)).toMatch(JSON.stringify(expectedResponse));
    });
})