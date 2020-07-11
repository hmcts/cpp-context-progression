const OutboundComplianceEnforcement = require('../index');
const context = require('../../testing/defaultContext');

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
    })
})