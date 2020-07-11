const referenceDataService = require('../../NowsHelper/service/ReferenceDataService');
const subscriptionsService = require('../../NowsHelper/service/SubscriptionsService');

const nowVariantsSubscription = require('../index');
const context = require('../../testing/defaultContext');

jest.mock('ReferenceDataService');
jest.mock('SubscriptionsService');

class MdeVariant {
    constructor(nowId) {
        this.now = {};
        this.now.id = nowId;
        this.orderDate = '2020-04-20'
        this.userGroup = new UserGroup()
    }
}

class UserGroup {
    constructor() {
        this.userGroups = [];
        this.type = UserGroupType;
    }
}

class UserGroupType {
    static get INCLUDE() {
        return "include";
    }

    static get EXCLUDE() {
        return "exclude";
    }

}

class SubscriptionMetaData {
    constructor(id) {
        this.id = id;
    }
}

describe('Now subscriptions', () => {

    beforeEach(() => {
        context.log.warn = () => {};
    });

    test('Should create NO clone when there is no subscription', async () => {
        const mdeVariants = [];
        mdeVariants.push(new MdeVariant('first-now-id'));
        mdeVariants.push(new MdeVariant('second-now-id'));

        context.bindings = {
            params: {
                cjscppuid: 'undefined',
                mdeVariants: mdeVariants
            }
        };

        referenceDataService.mockImplementation(() => {
            return {
                getSubscriptionsMetadata: () => {
                    return Promise.resolve([]);
                }
            };
        });

        const cloneNowVariants = await nowVariantsSubscription(context);

        expect(cloneNowVariants.length).toBe(0);
    });

    test('Should create one clone for each variants based on subscriptions', async () => {

        const mdeVariants = [new MdeVariant('first-now-id'), new MdeVariant('second-now-id')];

        context.bindings = {
            params: {
                cjscppuid: 'undefined',
                mdeVariants: mdeVariants
            }
        };

        const subscriptionsMetaData = require('./subscriptions.json');

        const subscriptions = [new SubscriptionMetaData('subscription-id')];

        referenceDataService.mockImplementation(() => {
            return {
                getSubscriptionsMetadata: () => {
                    return Promise.resolve(subscriptionsMetaData);
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

        const cloneNowVariants = await nowVariantsSubscription(context);

        expect(cloneNowVariants.length).toBe(2);
        expect(cloneNowVariants[0].now.id).toBe('first-now-id');
        expect(cloneNowVariants[1].now.id).toBe('second-now-id');
        expect(cloneNowVariants[0].matchedSubscription.id).toBe('subscription-id');
        expect(cloneNowVariants[1].matchedSubscription.id).toBe('subscription-id');
    });

    test('Should create multiple clones for one variants when multi subscriptions matches with prompts', async () => {

        const mdeVariants = [new MdeVariant('first-now-id')];

        context.bindings = {
            params: {
                cjscppuid: 'undefined',
                mdeVariants: mdeVariants
            }
        };

        const subscriptionsMetaData = require('./subscriptions.json');

        const subscriptions = [new SubscriptionMetaData('subscription-id-1'), new SubscriptionMetaData('subscription-id-2')];

        referenceDataService.mockImplementation(() => {
            return {
                getSubscriptionsMetadata: () => {
                    return Promise.resolve(subscriptionsMetaData);
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

        const cloneNowVariants = await nowVariantsSubscription(context);

        expect(cloneNowVariants.length).toBe(2);
        expect(cloneNowVariants[0].now.id).toBe('first-now-id');
        expect(cloneNowVariants[1].now.id).toBe('first-now-id');
        expect(cloneNowVariants[0].matchedSubscription.id).toBe('subscription-id-1');
        expect(cloneNowVariants[1].matchedSubscription.id).toBe('subscription-id-2');

    });


    test('Should create one clone for each variants based on subscriptions for EDT subscriptions', async () => {

        const mdeVariants = [new MdeVariant('first-now-id'), new MdeVariant('second-now-id')];

        context.bindings = {
            params: {
                cjscppuid: 'undefined',
                mdeVariants: mdeVariants
            }
        };

        mdeVariants[0].isEDT = true;

        const subscriptionsMetaData = require('./subscriptions.json');

        const subscriptions = [new SubscriptionMetaData('subscription-id')];

        referenceDataService.mockImplementation(() => {
            return {
                getSubscriptionsMetadata: () => {
                    return Promise.resolve(subscriptionsMetaData);
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

        const cloneNowVariants = await nowVariantsSubscription(context);

        expect(cloneNowVariants.length).toBe(2);
        expect(cloneNowVariants[0].now.id).toBe('first-now-id');
        expect(cloneNowVariants[0].matchedSubscription.id).toBe('subscription-id');
        expect(cloneNowVariants[0].isEDT).toBeTruthy();

        expect(cloneNowVariants[1].now.id).toBe('second-now-id');
        expect(cloneNowVariants[1].matchedSubscription.id).toBe('subscription-id');
        expect(cloneNowVariants[1].isEDT).toBeUndefined();
    });
});