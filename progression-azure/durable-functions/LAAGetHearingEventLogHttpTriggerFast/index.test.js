const httpFunction = require('./index');
const context = require('../testing/defaultContext');
const axios = require('axios');
const hearingQuery = require('../HearingResultedCacheQuery/index');
let {laaFilter} = require('../LAAHearingResultedFilter/functions.js');


jest.mock('axios');
jest.mock('../HearingResultedCacheQuery/index');
laaFilter = jest.fn();

describe('get hearing event log http trigger fast', () => {

    beforeEach(() => {
        hearingJson = require('../testing/hearing.1828f356-f746-4f2d-932b-79ef2df95c80.test.json');
    });

    test('Should return empty if filtered json is empty', async () => {

        context.bindings = {
            params: {
                hearingId: '52a96eb7-9523-4c50-af90-45ce21335456',
                hearingDate: '2020-03-20',
                filteredJson: {},
                cjscppuid: 'dummy_key_value'
            }
        };

        const req = {
            query:{
                date: '2020-10-10',
                hearingId:'52a96eb7-9523-4c50-af90-45ce21335456'
            },
            headers:{
                cjscppuid:'1828f356-f746-4f2d-932b-79ef2df95c80'
            }
        }

        hearingQuery.mockImplementation(()=>hearingJson);
        await httpFunction(context, req);

        expect(context.res.status).toBe(200);
        expect(Object.keys(context.res.body).length).toBe(0);
    });

});
