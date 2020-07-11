const ReferenceDataService = require('../NowsHelper/service/ReferenceDataService');
const SubscriptionsService = require('../NowsHelper/service/SubscriptionsService');
const SubscriptionObject = require('../NowsHelper/service/SubscriptionObject');

class CourtRegisterSubscriptions {
    constructor(context) {
        this.context = context;
    }

    async build() {
        const courtRegisterFragment = this.context.bindings.params.courtRegister;

        const registerDate = courtRegisterFragment.registerDate ? courtRegisterFragment.registerDate : undefined;

        const subscriptionsMetaData = await new ReferenceDataService().getSubscriptionsMetadata(this.context, registerDate);

        if (!subscriptionsMetaData || !subscriptionsMetaData.nowSubscriptions) {
            this.context.log('No Subscriptions found.');
            return courtRegisterFragment;
        }

        const courtRegisterSubscriptionsMetaData = subscriptionsMetaData.nowSubscriptions.filter((nowsRefDataSubscription) => nowsRefDataSubscription.isCourtRegisterSubscription);

        if ((courtRegisterSubscriptionsMetaData || []).length === 0) {
            this.context.log('No Court Register Subscriptions found');
            return courtRegisterFragment;
        }

        const subscriptionObj = this.buildSubscription(courtRegisterFragment, courtRegisterSubscriptionsMetaData);
        courtRegisterFragment.matchedSubscriptions = new SubscriptionsService().getSubscriptions(subscriptionObj);

        return courtRegisterFragment;
    }

    buildSubscription(objectVariant, nowsSubscriptionsMetaData) {
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.vocabulary = objectVariant.registerDefendants[0].vocabulary;
        subscriptionObj.subscriptions = nowsSubscriptionsMetaData;
        subscriptionObj.ouCode = objectVariant.courtCentreOUCode;
        subscriptionObj.judicialResults = this.collectJudicialResults(objectVariant);
        return subscriptionObj;
    }

    collectJudicialResults(objectVariant) {
        const judicialResults = [];
        const defendantContextBaseList = objectVariant.registerDefendants;
        if(defendantContextBaseList && defendantContextBaseList.length) {
            defendantContextBaseList.forEach(defendantContextBase => {
                if(defendantContextBase.results && defendantContextBase.results.length) {
                    defendantContextBase.results.forEach(result => judicialResults.push(result.judicialResult));
                }
            });
        }
        return judicialResults;
    }
}

module.exports = async function (context) {
    return await new CourtRegisterSubscriptions(context).build();
};