const ReferenceDataService = require ('../NowsHelper/service/ReferenceDataService');
const SubscriptionsService = require ('../NowsHelper/service/SubscriptionsService');
const SubscriptionObject = require ('../NowsHelper/service/SubscriptionObject');

class InformantRegisterSubscriptions {
    constructor (context) {
        this.context = context;
    }

    async build() {
        const informantRegisters = this.context.bindings.params.informantRegisters;

        const informantRegisterWithRegisterDate = informantRegisters.find(register => register.registerDate);
        const registerDate = informantRegisterWithRegisterDate.registerDate ? informantRegisterWithRegisterDate.registerDate : undefined;

        const subscriptionsMetaData = await new ReferenceDataService().getSubscriptionsMetadata(this.context, registerDate);

        if(subscriptionsMetaData && subscriptionsMetaData.nowSubscriptions) {
            const informantRegisterSubscriptions = subscriptionsMetaData.nowSubscriptions.filter((subscription) => subscription.isInformantRegisterSubscription);

            if ((informantRegisterSubscriptions || []).length === 0) {
                return informantRegisters;
            }

            informantRegisters.forEach(informantRegister => {
                const subscriptionObj = this.buildSubscription(informantRegister, informantRegisterSubscriptions);
                informantRegister.matchedSubscriptions = new SubscriptionsService().getSubscriptions(subscriptionObj);
            });
        }

        return informantRegisters;
    }

    buildSubscription(informantRegister, informantRegisterSubscriptions) {
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.vocabulary = informantRegister.vocabulary;
        subscriptionObj.ouCode = informantRegister.majorCreditorCode;
        subscriptionObj.subscriptions = informantRegisterSubscriptions;
        subscriptionObj.judicialResults = this.collectJudicialResults(informantRegister);
        return subscriptionObj;
    }

    collectJudicialResults(informantRegister) {
        const judicialResults = [];
        const defendantContextBaseList = informantRegister.registerDefendants;
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

module.exports = async (context) => {
    return await new InformantRegisterSubscriptions(context).build();
};