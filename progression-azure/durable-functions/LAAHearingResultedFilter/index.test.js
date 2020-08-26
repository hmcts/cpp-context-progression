const httpFunction = require('./index');
const context = require('../testing/defaultContext');
const axios = require('axios');

jest.mock('axios');

describe('filter offences', () => {

    test('should remove defendants that do not have an LAA reference', async() => {
        const hearingJson = require('../testing/hearing.1828f356-f746-4f2d-932b-79ef2df95c80.test.json');

        expect(hearingJson.hearing.prosecutionCases[0].defendants.length).toBe(3);
        expect(hearingJson.hearing.prosecutionCases[0].defendants[0].id).toBe('6647df67-a065-4d07-90ba-a8daa064ecc4');
        expect(hearingJson.hearing.prosecutionCases[0].defendants[1].id).toBe('e06f1327-0a23-4749-b61e-d4e901365fe2');
        expect(hearingJson.hearing.prosecutionCases[0].defendants[2].id).toBe('aeb6328d-19d4-49e8-8426-290f096b81dc');

        expect(hearingJson.hearing.prosecutionCases[0].defendants[0].offences.length).toBe(4);
        expect(hearingJson.hearing.prosecutionCases[0].defendants[1].offences.length).toBe(4);
        expect(hearingJson.hearing.prosecutionCases[0].defendants[2].offences.length).toBe(4);

        context.bindings = {
            unfilteredJson: hearingJson
        };

        const response = await httpFunction(context);

        expect(response.hearing.prosecutionCases[0].defendants.length).toBe(2);
        expect(response.hearing.prosecutionCases[0].defendants[0].id).toBe('e06f1327-0a23-4749-b61e-d4e901365fe2');
        expect(response.hearing.prosecutionCases[0].defendants[1].id).toBe('aeb6328d-19d4-49e8-8426-290f096b81dc');

        expect(response.hearing.prosecutionCases[0].defendants[0].offences.length).toBe(4);
        expect(response.hearing.prosecutionCases[0].defendants[0].offences[0].id).toBe('a154d828-eaa7-47f0-b8d5-e81227831d94');
        expect(response.hearing.prosecutionCases[0].defendants[0].offences[1].id).toBe('9d81ce7b-78b0-4853-9d21-f38a7b609207');
        expect(response.hearing.prosecutionCases[0].defendants[0].offences[2].id).toBe('bb7e8c03-3046-4024-ad65-2f220de45f2c');
        expect(response.hearing.prosecutionCases[0].defendants[0].offences[3].id).toBe('6b45095a-5da8-4829-8644-d6f6ff0bb691');

        expect(response.hearing.prosecutionCases[0].defendants[1].offences.length).toBe(4);
        expect(response.hearing.prosecutionCases[0].defendants[1].offences[0].id).toBe('10df5e52-0f18-4021-a99f-c354b93cc2b0');
        expect(response.hearing.prosecutionCases[0].defendants[1].offences[1].id).toBe('70165fe7-e8af-4465-af41-56a4304c9adc');
        expect(response.hearing.prosecutionCases[0].defendants[1].offences[2].id).toBe('68a591be-981f-467b-8ac8-3c2b3b32484e');
        expect(response.hearing.prosecutionCases[0].defendants[1].offences[3].id).toBe('c7f865bd-3057-4c9f-9db4-77caf452e5e3');

    });

    test('should remove defendants that do not have an LAA reference (2)', async() => {
        const hearingJson = require('../testing/hearing.ee7b9c09-4a6e-49e3-a484-193dc93a4575.test.json');
        const unifiedSearchResult = require('../testing/unifiedsearch.results.json');

        expect(hearingJson.hearing.prosecutionCases[0].defendants[0].offences.length).toBe(1);
        expect(hearingJson.hearing.prosecutionCases[0].defendants[0].id).toBe('ad03a626-d438-44a5-84b6-14111ec363fa');

        axios.get.mockImplementation(() => Promise.resolve({data: unifiedSearchResult}));
        context.bindings = {
            unfilteredJson: hearingJson,
            params: {
                cjscppuid: 'dummy_key_value'
            }
        };

        const response = await httpFunction(context);

        expect(response.hearing.prosecutionCases[0].defendants.length).toBe(1);
        expect(response.hearing.prosecutionCases[0].defendants[0].id).toBe('ad03a626-d438-44a5-84b6-14111ec363fa');

        expect(response.hearing.prosecutionCases[0].defendants[0].offences.length).toBe(1);
        expect(response.hearing.prosecutionCases[0].defendants[0].offences[0].id).toBe('7dc1b279-805f-4ba8-97ea-be635f5764a7');
    });

    test('should remove defendants that do not have an LAA reference (3)', async() => {
        const hearingJson = require('../testing/hearing.multiple.defendants.with.no.laareference.test.json');
        const unifiedSearchResult = require('../testing/unifiedsearch.multipledefendant.results.json');

        expect(hearingJson.hearing.prosecutionCases[0].defendants[0].offences.length).toBe(4);
        expect(hearingJson.hearing.prosecutionCases[0].defendants[0].id).toBe('6647df67-a065-4d07-90ba-a8daa064ecc4');

        axios.get.mockImplementation(() => Promise.resolve({data: unifiedSearchResult}));
        context.bindings = {
            unfilteredJson: hearingJson,
            params: {
                cjscppuid: 'dummy_key_value'
            }
        };

        const response = await httpFunction(context);

        expect(response.hearing.prosecutionCases[0].defendants.length).toBe(2);
        expect(response.hearing.prosecutionCases[0].defendants[0].id).toBe('6647df67-a065-4d07-90ba-a8daa064ecc4');

        expect(response.hearing.prosecutionCases[0].defendants[0].offences.length).toBe(4);
        expect(response.hearing.prosecutionCases[0].defendants[0].offences[0].id).toBe('a154d828-1234-47f0-b8d5-e81227831d94');
    });

});
