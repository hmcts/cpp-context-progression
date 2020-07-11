const RecipientMapper = require('../RecipientMapper');
const context = require('../../../../testing/defaultContext');

describe('Recipient Mapper should build recipients correctly', () => {
    test('Recipient should be null when there is no recipient', async ()=>{
        const hearingJson = require('../../../test/hearing.json');
        const informantRegisterSubscriptions = require('./informantRegisterSubscriptions.json');
        context.bindings = {
            params: {
                hearingResultedObj: hearingJson,
                informantRegisterSubscriptions: informantRegisterSubscriptions
            }
        };

        const recipients = await new RecipientMapper(context,informantRegisterSubscriptions).build();
        expect(recipients).toBeUndefined();
    });
});