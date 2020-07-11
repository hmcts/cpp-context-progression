const ReferenceDataService = require ('../NowsHelper/service/ReferenceDataService');
const SubscriptionsService = require ('../NowsHelper/service/SubscriptionsService');
const SubscriptionObject = require ('../NowsHelper/service/SubscriptionObject');

class PrisonCourtRegisterSubscriptions {
    constructor (context) {
        this.context = context;
    }

    async build() {

        const prisonCourtRegisters = this.context.bindings.params.prisonCourtRegisters;

        const orderDate = this.getOrderedDate(prisonCourtRegisters);

        const subscriptionsMetaData = await new ReferenceDataService().getSubscriptionsMetadata(this.context, orderDate);

        if (subscriptionsMetaData && subscriptionsMetaData.nowSubscriptions) {

            const prisonCourtRegisterSubscriptions = subscriptionsMetaData.nowSubscriptions.filter(nowsRefDataSubscription => nowsRefDataSubscription.isPrisonCourtRegisterSubscription);

            if ((prisonCourtRegisterSubscriptions || []).length === 0) {
                return prisonCourtRegisters;
            }

            prisonCourtRegisters.forEach(prisonCourtRegister => {
                const subscriptionObj = this.buildSubscription(prisonCourtRegister, prisonCourtRegisterSubscriptions);
                this.context.log('subscriptionObj : ' + JSON.stringify(subscriptionObj.vocabulary));
                prisonCourtRegister.matchedSubscriptions = new SubscriptionsService().getSubscriptions(subscriptionObj);
                prisonCourtRegister.registerDate = orderDate;
            });
        }

        return prisonCourtRegisters;
    }

    buildSubscription(prisonCourtRegister, prisonCourtRegisterSubscriptions) {
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.vocabulary = prisonCourtRegister.registerDefendant.vocabulary;
        subscriptionObj.subscriptions = prisonCourtRegisterSubscriptions;
        subscriptionObj.judicialResults = this.collectJudicialResults(prisonCourtRegister);
        return subscriptionObj;
    }

    getOrderedDate(prisonCourtRegisters) {
        const resultWithOrderedDate = prisonCourtRegisters.find(pr => pr.registerDefendant.results.find(result => result.judicialResult.orderedDate));
        if(resultWithOrderedDate.registerDefendant.results.length) {
            return resultWithOrderedDate.registerDefendant.results[0].judicialResult.orderedDate;
        }
    }

    async collectJudicialResults(prisonCourtRegister) {
        const judicialResults = [];
        const defendantResults = prisonCourtRegister.registerDefendant ? prisonCourtRegister.registerDefendant.results : [];
        if(defendantResults.length) {
            defendantResults.forEach(result => judicialResults.push(result.judicialResult));
        }
        return judicialResults;
    }
}

module.exports = async (context) => {
    return await new PrisonCourtRegisterSubscriptions(context).build();
};