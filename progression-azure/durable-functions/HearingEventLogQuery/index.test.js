const httpFunction = require('./index');
const context = require('../testing/defaultContext');
const axios = require('axios');

jest.mock('axios');

describe('get hearing event log', () => {

    beforeEach(() => {
        eventLog = require('../testing/eventlog.1828f356-f746-4f2d-932b-79ef2df95c80.test.json');
    });

    test('Should return null if filtered json is empty', async () => {

        context.bindings = {
            params: {
                hearingId: '52a96eb7-9523-4c50-af90-45ce21335456',
                hearingDate: '2020-03-20',
                filteredJson: {},
                cjscppuid: 'dummy_key_value'
            }
        };

        const response = await httpFunction(context);

        expect(response).toBe(null);
    });

    test('Should return event log json when getHearingEventLog returns json', async () => {
        const subscriberJson = require('../testing/hearing.1828f356-f746-4f2d-932b-79ef2df95c80.test.json');

        axios.get.mockImplementation(() => Promise.resolve({ data: eventLog }));

        context.bindings = {
            params: {
                hearingId: '52a96eb7-9523-4c50-af90-45ce21335456',
                hearingDate: '2020-03-20',
                filteredJson: subscriberJson,
                cjscppuid: 'dummy_key_value'
            }
        };

        const response = await httpFunction(context);

        expect(response).toBe(eventLog);
    });
    
});