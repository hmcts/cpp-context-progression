const ReferenceDataService = require('../../NowsHelper/service/ReferenceDataService');
const SubscriptionsService = require('../../NowsHelper/service/SubscriptionsService');
const SubscriptionObject = require('../../NowsHelper/service/SubscriptionObject');

const courtRegisterSubscription = require('../index');
const context = require('../../testing/defaultContext');

jest.mock('../../NowsHelper/service/ReferenceDataService');
jest.mock('../../NowsHelper/service/SubscriptionsService');

class CourtRegisterFragment {
    constructor() {
        this.matchedSubscriptions = undefined;
        this.registerDefendants = [];
        this.registerDate = '2020-04-20';
    }
}

class SubscriptionMetaData {
    constructor() {
        this.isCourtRegisterSubscription = true;
    }
}

describe('Court Register subscriptions', () => {

    beforeEach(() => {
        jest.resetModules();
        SubscriptionsService.mockClear();
        ReferenceDataService.mockClear();
    });
    test('Should NOT set matchedSubscriptions property when there is NO subscription', async () => {
        const courtRegisterFragment = new CourtRegisterFragment();
        context.bindings = {
            params: {
                courtRegister: courtRegisterFragment
            }
        };

        ReferenceDataService.mockImplementation(() => {
            return {
                getSubscriptionsMetadata: jest.fn(() => {
                }),
            };
        });

        SubscriptionsService.mockImplementation(() => {
            return {
                getSubscriptions: jest.fn(() => {
                    [new SubscriptionMetaData()];
                }),
            };
        });

        const returnCourtRegisterFragment = await courtRegisterSubscription(context);

        expect(returnCourtRegisterFragment.matchedSubscriptions).toBe(undefined);
    });

    test('Should set matchedSubscriptions property of court register fragment object', async () => {
        const registerDefendants = require('./register-defendant.json');
        const courtRegisterFragment = new CourtRegisterFragment();
        courtRegisterFragment.registerDefendants = registerDefendants;
        context.bindings = {
            params: {
                courtRegister: courtRegisterFragment
            }
        };
        ReferenceDataService.mockImplementation(() => {
            return {
                getSubscriptionsMetadata: jest.fn()
                    .mockReturnValueOnce({nowSubscriptions: [new SubscriptionMetaData()]}),
            };
        });

        SubscriptionsService.mockImplementation(() => {
            return {
                getSubscriptions: jest.fn(() => [new SubscriptionMetaData()]),
            };
        });

        const returnCourtRegisterFragment = await courtRegisterSubscription(context);

        expect(returnCourtRegisterFragment.matchedSubscriptions.length).toBe(1);
        expect(returnCourtRegisterFragment.registerDefendants.length).toBe(1);
    });

    test('Should set multiple matchedSubscriptions property of court register fragment object',
         async () => {
             const registerDefendants = require('./register-defendant.json');
             const courtRegisterFragment = new CourtRegisterFragment();
             courtRegisterFragment.registerDefendants = registerDefendants;
             context.bindings = {
                 params: {
                     courtRegister: courtRegisterFragment
                 }
             };

             ReferenceDataService.mockImplementation(() => {
                 return {
                     getSubscriptionsMetadata: jest.fn()
                         .mockReturnValueOnce({nowSubscriptions: [new SubscriptionMetaData()]}),
                 };
             });

             SubscriptionsService.mockImplementation(() => {
                 return {
                     getSubscriptions: jest.fn().mockReturnValueOnce(
                         [new SubscriptionMetaData(), new SubscriptionMetaData()]),
                 };
             });

             const returnCourtRegisterFragment = await courtRegisterSubscription(context);

             expect(returnCourtRegisterFragment.matchedSubscriptions.length).toBe(2);
         });

    test('Should filter out the subscription which is not court register subscription',
         async () => {
             const registerDefendants = require('./register-defendant.json');
             const courtRegisterFragment = new CourtRegisterFragment();
             courtRegisterFragment.registerDefendants = registerDefendants;
             context.bindings = {
                 params: {
                     courtRegister: courtRegisterFragment
                 }
             };

             const subscription1 = new SubscriptionMetaData();
             const subscription2 = new SubscriptionMetaData();
             subscription2.isCourtRegisterSubscription = false;

             const subscriptions = [subscription1, subscription2];
             const courtRegSubscriptions = [subscription1];

             ReferenceDataService.mockImplementation(() => {

                 return {
                     getSubscriptionsMetadata: jest.fn()
                         .mockReturnValueOnce({nowSubscriptions: subscriptions}),
                 };
             });

             const fakeGetSubscriptions = jest.fn();
             SubscriptionsService.mockImplementation(() => {
                 return {
                     getSubscriptions: fakeGetSubscriptions,
                 };
             });

             await courtRegisterSubscription(context);

             let subscriptionObject = new SubscriptionObject();
             subscriptionObject.subscriptions = courtRegSubscriptions;

             expect(fakeGetSubscriptions).toHaveBeenCalledTimes(1);
             // expect(fakeGetSubscriptions).toHaveBeenLastCalledWith(subscriptionObject);
         });
});