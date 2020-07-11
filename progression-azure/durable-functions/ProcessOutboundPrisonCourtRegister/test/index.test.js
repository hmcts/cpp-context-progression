const ProcessOutboundPrisonCourtRegister = require('../index');
const context = require('../../testing/defaultContext');
const axios = require('axios');

jest.mock('axios');
jest.mock('process');

describe('Process prison court register request successfully', () => {

    test('submit to progression ', async () => {
        process.env = {
            PROGRESSION_CONTEXT_API_BASE_URI: 'http://localhost:8080'
        };

        axios.post.mockImplementation(() => {
            Promise.resolve({status: 202});
        });
        context.bindings = {
            params: {
                outboundPrisonCourtRegisters: require('./prison-court-register-requests')
            }
        };

        await ProcessOutboundPrisonCourtRegister(context);

        const call0 = ['http://localhost:8080/progression-command-api/command/api/rest/progression/prison-court-register', context.bindings.params.outboundPrisonCourtRegisters[0], {
            'headers': {
                'CJSCPPUID': undefined,
                'Content-type': 'application/vnd.progression.add-prison-court-register+json'
            }
        }];

        const call1 = ['http://localhost:8080/progression-command-api/command/api/rest/progression/prison-court-register', context.bindings.params.outboundPrisonCourtRegisters[1], {
            'headers': {
                'CJSCPPUID': undefined,
                'Content-type': 'application/vnd.progression.add-prison-court-register+json'
            }
        }];

        expect(axios.post.mock.calls.length).toBe(2);
        expect(axios.post.mock.calls[0]).toEqual(expect.arrayContaining(call0));
        expect(axios.post.mock.calls[1]).toEqual(expect.arrayContaining(call1));
    });
});