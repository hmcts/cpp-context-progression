const httpFunction = require('./index');
const context = require('../testing/defaultContext')
const getHearing = require('../HearingResultedCacheQuery/index');

jest.mock('../HearingResultedCacheQuery/index')

describe('Test get hearing trigger', () => {
    
    beforeEach(() => {
        hearing = require('../testing/hearing.1828f356-f746-4f2d-932b-79ef2df95c80.test.json');
    });

    test('Http trigger should return hearing json', async () => {

        getHearing.mockImplementation(() => hearing);
    
        const request = {
            query: { 
                hearingId: '1828f356-f746-4f2d-932b-79ef2df95c80' 
            },
            headers: {
                cjscppuid: '996cbb52-b703-4a03-b2e1-7ebda9fd0b4b'
            }
        };

        await httpFunction(context, request);
        expect(context.res.body.hearing.prosecutionCases[0].defendants[0].id).toBe('e06f1327-0a23-4749-b61e-d4e901365fe2');

    });
});