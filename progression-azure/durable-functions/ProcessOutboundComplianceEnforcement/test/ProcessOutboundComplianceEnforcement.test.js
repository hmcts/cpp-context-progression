const ProcessOutboundComplianceEnforcement = require('../index');
const context = require('../../testing/defaultContext');
const axios = require('axios');

jest.mock('axios');
jest.mock('process');

describe('Process outbound compliance enforcement successfully', () => {

    test('submit to staging enforcement', async () => {
        process.env = {
            ENFORCEMENT_CONTEXT_API_BASE_URI: "http://localhost:8080"
        };
        axios.post.mockImplementation(() => {
            Promise.resolve({status: 202})
        });
        context.bindings = {
            params: {
                stagingEnforcementRequests: require('./staging-enforcement-requests')
            }
        };

        await ProcessOutboundComplianceEnforcement(context);

        const call0 = ["http://localhost:8080/stagingenforcement-command-api/command/api/rest/stagingenforcement/enforce-financial-imposition",
                       context.bindings.params.stagingEnforcementRequests[0], {
                "headers": {
                    "CJSCPPUID": undefined,
                    "Content-type": "application/vnd.stagingenforcement.enforce-financial-imposition+json"
                }
            }];

        const call1 = ["http://localhost:8080/stagingenforcement-command-api/command/api/rest/stagingenforcement/enforce-financial-imposition",
                       context.bindings.params.stagingEnforcementRequests[1], {
                "headers": {
                    "CJSCPPUID": undefined,
                    "Content-type": "application/vnd.stagingenforcement.enforce-financial-imposition+json"
                }
            }];

        expect(axios.post.mock.calls.length).toBe(2);
        expect(axios.post.mock.calls[0]).toEqual(expect.arrayContaining(call0));
        expect(axios.post.mock.calls[1]).toEqual(expect.arrayContaining(call1));
    });
});