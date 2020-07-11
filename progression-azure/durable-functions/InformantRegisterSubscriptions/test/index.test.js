const referenceDataService = require ('../../NowsHelper/service/ReferenceDataService');
const subscriptionsService = require ('../../NowsHelper/service/SubscriptionsService');

const informantRegisterSubscription = require('../index');
const context = require('../../testing/defaultContext');

jest.mock('ReferenceDataService');
jest.mock('SubscriptionsService');

class InformantRegister {
    constructor() {
        this.matchedSubscription = undefined;
        this.registerDate = '2020-04-20'
    }
}

class SubscriptionMetaData {
    constructor(id){
        this.id = id;
        this.isInformantRegisterSubscription = true;
        this.matchedSubscription = undefined;
    }
}

describe('Informant Register Subscriptions', () => {

    test('Should NOT set matchedSubscription when there is no subscription', async () => {
        const informantRegisters = [new InformantRegister(), new InformantRegister()];
        context.bindings = {
            params: {
                informantRegisters: informantRegisters
            }
        };

        referenceDataService.mockImplementation(() => {
            return {
                getSubscriptionsMetadata: () => {
                    return Promise.resolve([]);
                }
            };
        });

        const returnInformantRegisters = await informantRegisterSubscription(context);

        expect(returnInformantRegisters.matchedSubscription).toBe(undefined);
    });

    test('Should set matchedSubscription property of informant register object', async () => {
        const informantRegisters = [new InformantRegister(), new InformantRegister()];
        context.bindings = {
            params: {
                informantRegisters: informantRegisters
            }
        };

        const subscriptions = [new SubscriptionMetaData()];

        referenceDataService.mockImplementation(() => {
            return {
                getSubscriptionsMetadata: () => {
                    return Promise.resolve([new SubscriptionMetaData()]);
                }
            };
        });

        subscriptionsService.mockImplementation(() => {
            return {
                getSubscriptions: () => {
                    return subscriptions;
                }
            };
        });

        const returnInformantRegisters = await informantRegisterSubscription(context);

        expect(returnInformantRegisters.length).toBe(2);
    });

    test('Should have multiple subscriptions if multiple subscription returns ', async () => {
        const registerDefendants = require('./register-defendant.json');
        const informantRegister = new InformantRegister();
        informantRegister.registerDefendants = registerDefendants;
        const informantRegisters = [informantRegister];
        context.bindings = {
            params: {
                informantRegisters: informantRegisters
            }
        };

        const subscriptions = [new SubscriptionMetaData('first-subscription-id'), new SubscriptionMetaData('second-subscription-id')];

        referenceDataService.mockImplementation(() => {
            return {
                getSubscriptionsMetadata: () => {
                    return Promise.resolve([new SubscriptionMetaData()]);
                }
            };
        });

        subscriptionsService.mockImplementation(() => {
            return {
                getSubscriptions: () => {
                    return subscriptions;
                }
            };
        });

        const returnInformantRegisters = await informantRegisterSubscription(context);

        expect(returnInformantRegisters.length).toBe(1);
        expect(returnInformantRegisters[0].registerDefendants.length).toBe(1);
    });
});