const referenceDataService = require('../../NowsHelper/service/ReferenceDataService');
const subscriptionsService = require('../../NowsHelper/service/SubscriptionsService');
const PrisonCourtRegisterSubscriptions = require('../index');
const context = require('../../testing/defaultContext');

jest.mock('ReferenceDataService');
jest.mock('SubscriptionsService');

describe('Prison Court Register Subscriptions', () => {
    beforeEach(() => {
        jest.resetModules();
        subscriptionsService.mockClear();
        referenceDataService.mockClear();
    });
    test('Should set matchedSubscription property of Prison Court Register', async () => {
        const prisonCourtRegisters = require('./prison-court-registers-two.json');
        context.bindings = {
            params: {
                prisonCourtRegisters: prisonCourtRegisters
            }
        };

        const referenceDataServiceResult = require('./reference-data-service-result-all-true.json');
        const subscriptionServiceResult = require('./subscription-service-result-single.json');

        referenceDataService.mockImplementation(() => {
            return {
                getSubscriptionsMetadata: () => {
                    return referenceDataServiceResult;
                }
            };
        });

        subscriptionsService.mockImplementation(() => {
            return {
                getSubscriptions: () => {
                    return subscriptionServiceResult;
                }
            };
        });

        const prisonCourtRegisterSubscriptions = await PrisonCourtRegisterSubscriptions(context);
        expect(prisonCourtRegisterSubscriptions.length).toBe(2);
        expect(prisonCourtRegisterSubscriptions[0].matchedSubscriptions.nowSubscriptions.length)
            .toBe(1);
        expect(prisonCourtRegisterSubscriptions[1].matchedSubscriptions.nowSubscriptions.length)
            .toBe(1);
    });

    test(
        'should not set subscriptions if there are no prison court register subscriptions in the reference data',
        async () => {
            const prisonCourtRegisters = require('./prison-court-registers-two.json');

            context.bindings = {
                params: {
                    prisonCourtRegisters: prisonCourtRegisters
                }
            };

            const referenceDataServiceResult = require(
                './reference-data-service-result-all-false.json');
            const subscriptionServiceResult = require('./subscription-service-result-single.json');

            referenceDataService.mockImplementation(() => {
                return {
                    getSubscriptionsMetadata: () => {
                        return referenceDataServiceResult;
                    }
                };
            });

            subscriptionsService.mockImplementation(() => {
                return {
                    getSubscriptions: () => {
                        return subscriptionServiceResult;
                    }
                };
            });

            const prisonCourtRegisterSubscriptions = await PrisonCourtRegisterSubscriptions(
                context);
            expect(prisonCourtRegisterSubscriptions.length).toBe(2);
            expect(prisonCourtRegisterSubscriptions[0].matchedSubscriptions).toBe(undefined);
            expect(prisonCourtRegisterSubscriptions[1].matchedSubscriptions).toBe(undefined);
        });

    test(
        'should call subscriptions service as many times as the number of prison court register subscriptions in the reference data',
        async () => {
            const prisonCourtRegisters = require('./prison-court-registers-two.json');
            context.bindings = {
                params: {
                    prisonCourtRegisters: prisonCourtRegisters
                }
            };
            const referenceDataServiceResult = require(
                './reference-data-service-result-1-3-true.json');
            const subscriptionServiceResult = require('./subscription-service-result-single.json');

            referenceDataService.mockImplementation(() => {
                return {
                    getSubscriptionsMetadata: () => {
                        return referenceDataServiceResult;
                    }
                };
            });

            subscriptionsService.mockImplementation(() => {
                return {
                    getSubscriptions: () => {
                        return subscriptionServiceResult;
                    }
                };
            });

            const prisonCourtRegisterSubscriptions = await PrisonCourtRegisterSubscriptions(
                context);

            expect(prisonCourtRegisterSubscriptions.length).toBe(2);
            expect(prisonCourtRegisterSubscriptions[0].matchedSubscriptions.nowSubscriptions.length)
                .toBe(1);
            expect(prisonCourtRegisterSubscriptions[1].matchedSubscriptions.nowSubscriptions.length)
                .toBe(1);
        });

    test(
        'should set the same matched subscription if more than one subscriptions return from the subscription service ',
        async () => {
            const prisonCourtRegisters = require('./prison-court-registers-two.json');
            context.bindings = {
                params: {
                    prisonCourtRegisters: prisonCourtRegisters
                }
            };
            const referenceDataServiceResult = require(
                './reference-data-service-result-1-3-true.json');
            const subscriptionServiceResult = require('./subscription-service-result-double.json');

            referenceDataService.mockImplementation(() => {
                return {
                    getSubscriptionsMetadata: () => {
                        return referenceDataServiceResult;
                    }
                };
            });

            subscriptionsService.mockImplementation(() => {
                return {
                    getSubscriptions: () => {
                        return subscriptionServiceResult;
                    }
                };
            });

            const actual = await PrisonCourtRegisterSubscriptions(context);
            const expected = require('./expected-result-multi.json');
            expect(actual).toEqual(expected);
        });
});