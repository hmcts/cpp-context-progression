const ProcessOutboundInformantRegister = require('../index');
const context = require('../../testing/defaultContext');
const axios = require('axios');

jest.mock('axios');
jest.mock('process');

describe('Process outbound informant register requests successfully', () => {

    test('save outbound informant register request to progression', async () => {
        process.env = {
            PROGRESSION_CONTEXT_API_BASE_URI: 'http://localhost:8080'
        };
        axios.post.mockImplementation(() => {
            Promise.resolve({status: 202});
        });

        context.bindings = {
            params: {
                outboundInformantRegisters: require('./outbound-informant-requests')
            }
        };

        await ProcessOutboundInformantRegister(context);

        const call0 = ['http://localhost:8080/progression-command-api/command/api/rest/progression/informant-register', context.bindings.params.outboundInformantRegisters[0], {
            'headers': {
                'CJSCPPUID': undefined,
                'Content-type': 'application/vnd.progression.add-informant-register+json'
            }
        }];

        expect(axios.post.mock.calls.length).toBe(1);
        expect(axios.post.mock.calls[0]).toEqual(expect.arrayContaining(call0));
    });
});