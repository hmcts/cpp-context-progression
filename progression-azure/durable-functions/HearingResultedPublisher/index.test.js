const httpFunction = require('./index');
const context = require('../testing/defaultContext');
const axios = require('axios');

jest.mock('axios');

describe('filter hearing', () => {

    test('Should filter on LAA subscribers', async () => {
        const subscriberJson = require('../testing/subscribers.json');

        axios.post.mockImplementation(() => Promise.resolve({ data: true }));

        context.bindings = {
            hearingId: '52a96eb7-9523-4c50-af90-45ce21335456',
            subscriberJson: subscriberJson,
        };

        const response = await httpFunction(context);

        expect(response.published_to.length).toBe(2);
        expect(response.published_to[0].subscriber_endpoint_uri).toBe('https://2.laa.example.com');
        expect(response.published_to[1].subscriber_endpoint_uri).toBe('https://4.laa.example.com');
    });
    
});