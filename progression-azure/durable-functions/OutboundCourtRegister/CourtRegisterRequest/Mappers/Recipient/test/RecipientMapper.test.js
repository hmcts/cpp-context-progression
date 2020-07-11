const RecipientMapper = require('../RecipientMapper');
const context = require('../../../../../testing/defaultContext');

describe('Recipient Mapper', () => {

    test('Should return correct values', () => {
        const fakeCourtRegisterFragment = {
            "matchedSubscriptions": [{
                recipient: {
                    emailAddress1: "some email",
                    emailAddress2: "email two",
                    emailTemplateName: "templatename",
                    organisationName: "John Smith"
                },
                "emailDelivery": true,
                "forDistribution": true
            }]
        };

        const result = new RecipientMapper(context, fakeCourtRegisterFragment).build();
        expect(result).toBeTruthy();
        expect(result.length).toBe(1);
        expect(result[0].emailAddress1).toBe(fakeCourtRegisterFragment.matchedSubscriptions[0].recipient.emailAddress1);
        expect(result[0].emailAddress2).toBe(fakeCourtRegisterFragment.matchedSubscriptions[0].recipient.emailAddress2);
        expect(result[0].recipientName).toBe(fakeCourtRegisterFragment.matchedSubscriptions[0].recipient.organisationName);

    });

    test('Should return correct values when there is no recipient', () => {
        const fakeCourtRegisterFragment = {
            "matchedSubscriptions": [{
                "emailDelivery": true,
                "forDistribution": true
            }]
        };

        const result = new RecipientMapper(context, fakeCourtRegisterFragment).build();
        expect(result).toBeUndefined();

    });
});