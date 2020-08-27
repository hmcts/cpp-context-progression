const httpFunction = require('./index');
const context = require('../testing/defaultContext')
const getHearing = require('../HearingResultedCacheQuery/index');
const axios = require('axios');
jest.mock('axios');

jest.mock('../HearingResultedCacheQuery/index')

describe('Test get hearing trigger', () => {
    
    beforeEach(() => {
        hearing = require('../testing/hearing.1828f356-f746-4f2d-932b-79ef2df95c80.test.json');
    });

    test('Http trigger should return hearing json', async () => {
        const unifiedSearchResult = require('../testing/unifiedsearch.multipledefendant1.results.json');
        axios.get.mockImplementation(() => Promise.resolve({data: unifiedSearchResult}));

        getHearing.mockImplementation(() => hearing);
    
        const request = {
            query: { 
                hearingId: '1828f356-f746-4f2d-932b-79ef2df95c80' 
            },
            headers: {
                cjscppuid: '996cbb52-b703-4a03-b2e1-7ebda9fd0b4b'
            }
        };
        context.bindings = {
            params: {
                cjscppuid: 'dummy_key_value'
            }
        };

        await httpFunction(context, request);
        expect(context.res.body.hearing.prosecutionCases[0].defendants[0].id).toBe('e06f1327-0a23-4749-b61e-d4e901365fe2');

    });
});
