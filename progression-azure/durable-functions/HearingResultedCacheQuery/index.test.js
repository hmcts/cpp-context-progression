const httpFunction = require('./index');
const context = require('../testing/defaultContext');
const axios = require('axios');
const sinon = require('sinon');

jest.mock('axios');

describe('hearing resulted cache query', () => {

    beforeEach(() => {
        hearing = require('../testing/hearing.1828f356-f746-4f2d-932b-79ef2df95c80.test.json');
    });
    
    test('should fetch hearing if not in cache, if CJSCCPUID is supplied', async () => {
    
        axios.get.mockImplementation(() => Promise.resolve({ data: hearing }));
    
        var redisClientFake = {
            get: sinon.stub().callsArgWith(1, null, null),
            on: sinon.stub().returns(true)
        };
    
        context.bindings = {
            params:  {
                hearingId: '1828f356-f746-4f2d-932b-79ef2df95c80',
                cjscppuid: 'dummy_key_value',
                redisClient: redisClientFake
            }
        };
    
        const response = await httpFunction(context);
    
        expect(response.prosecutionCases[0].defendants[0].id).toBe('6647df67-a065-4d07-90ba-a8daa064ecc4');
    });


    test('should throw an exception if not in cache and CJSCCPUID is not supplied', async () => {

        axios.get.mockImplementation(() => Promise.resolve({ data: hearing }));

        var redisClientFake = {
            get: sinon.stub().callsArgWith(1, null, null),
            on: sinon.stub().returns(true)
        };

        context.bindings = {
            params:  {
                hearingId: '1828f356-f746-4f2d-932b-79ef2df95c80',
                redisClient: redisClientFake
            }
        };

        expect.assertions(1);

        try {
            await httpFunction(context);
        } catch (e) {
            expect(e).toMatch(/Hearing (.*) not found in cache and no CJSCPPUID supplied/);
        }
    });

    test('should fetch hearing if it is in the cache', async () => {

        axios.get.mockImplementation(() => Promise.resolve({ data: null }));

        var redisClientFake = {
            get: sinon.stub().callsArgWith(1, null, JSON.stringify(hearing)),
            on: sinon.stub().returns(true)
        };

        context.bindings = {
            params:  {
                hearingId: '1828f356-f746-4f2d-932b-79ef2df95c80',
                redisClient: redisClientFake
            }
        };

        const response = await httpFunction(context);

        expect(response.prosecutionCases[0].defendants[0].id).toBe('6647df67-a065-4d07-90ba-a8daa064ecc4');
    });

});