const ProcessOutboundCourtRegister = require('../index');
const context = require('../../testing/defaultContext');
const axios = require('axios');

jest.mock('axios');
jest.mock('process');

describe('Process Court Register Outbound', () => {

    let fakeCourtRegisterDocumentRequest;

    beforeEach(() => {
        jest.resetModules();
        fakeCourtRegisterDocumentRequest = require('./court-register-document-request.json');
    });

    test('Should submit to progression', async () => {
        process.env = {
            PROGRESSION_CONTEXT_API_BASE_URI: 'http://localhost:8080'
        };
        axios.post.mockImplementation(() => {
            Promise.resolve({status: 202});
        });
        context.bindings = {
            params: {
                outboundCourtRegister: fakeCourtRegisterDocumentRequest
            }
        };

        await ProcessOutboundCourtRegister(context);

        const call0 = ['http://localhost:8080/progression-command-api/command/api/rest/progression/court-register', context.bindings.params.outboundCourtRegister, {
            'headers': {
                'CJSCPPUID': undefined,
                'Content-type': 'application/vnd.progression.add-court-register+json'
            }
        }];

        expect(axios.post.mock.calls.length).toBe(1);
        expect(axios.post.mock.calls[0]).toEqual(expect.arrayContaining(call0));
    });
});