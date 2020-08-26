const ReferenceDataService = require ('../NowsHelper/service/ReferenceDataService');
const SubscriptionsService = require ('../NowsHelper/service/SubscriptionsService');
const SubscriptionObject = require ('../NowsHelper/service/SubscriptionObject');

class NowsVariantsSubscriptions {
    constructor (context) {
        this.context = context;
    }

    async build() {

        const mdeVariants = this.context.bindings.params.mdeVariants;
        this.context.log.warn('After applying MDE Rules. ' + mdeVariants.length + ' variants created.');
        const objectVariantsSubscriptions = [];

        const variantWithOrderDate = mdeVariants.find(variant => variant.orderDate);
        const orderDate = variantWithOrderDate.orderDate ? variantWithOrderDate.orderDate : undefined;

        const subscriptionsMetaData = await new ReferenceDataService().getSubscriptionsMetadata(this.context, orderDate);

        if(subscriptionsMetaData && subscriptionsMetaData.nowSubscriptions) {

            const nowSubscriptions = subscriptionsMetaData.nowSubscriptions.filter((nowsRefDataSubscription) => nowsRefDataSubscription.isNowSubscription);
            const edtSubscriptions = subscriptionsMetaData.nowSubscriptions.filter((nowsRefDataSubscription) => nowsRefDataSubscription.isEDTSubscription);

            mdeVariants.forEach(objectVariant => {
                const subscriptionsToBuild = objectVariant.now.isEDT ? edtSubscriptions : nowSubscriptions;

                if ((subscriptionsToBuild || []).length) {
                    const subscriptionObj = this.buildSubscription(objectVariant, subscriptionsToBuild);
                    const subscriptions = new SubscriptionsService().getSubscriptions(subscriptionObj);
                    if(subscriptions) {
                        subscriptions.forEach((subscription) => {
                            const cloneObjectVariant = Object.assign({}, objectVariant);
                            cloneObjectVariant.matchedSubscription = subscription;
                            objectVariantsSubscriptions.push(cloneObjectVariant);
                            this.context.log.warn('Applying ' + JSON.stringify(subscription.name)+ ' subscription to ' +JSON.stringify(objectVariant.now.name) + (objectVariant.now.isEDT ? ' EDT.' : ' NOWs.'));
                        });
                    }
                }
            });
        }

        this.context.log.warn('After applying Subscriptions Rules - ' + objectVariantsSubscriptions.length + ' variants');
        objectVariantsSubscriptions.forEach(objectVariantsSubscription => {
            if(objectVariantsSubscription.userGroup) {
                const type = objectVariantsSubscription.userGroup.type;
                const usergroup = type === 'exclude' || type === undefined ? 'public' : objectVariantsSubscription.userGroup.userGroups;
                this.context.log.warn('Master Defendant Id - ' + objectVariantsSubscription.masterDefendantId +' - '+objectVariantsSubscription.now.name + ' - ' + usergroup + ' copy has ' + objectVariantsSubscription.matchedSubscription.name + ' subscription');
            }
        });

        return objectVariantsSubscriptions;
    }

    buildSubscription(objectVariant, nowSubscriptions) {
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.nowId = objectVariant.now.id;
        subscriptionObj.userGroup = objectVariant.userGroup;
        subscriptionObj.vocabulary = objectVariant.vocabulary;
        subscriptionObj.subscriptions = nowSubscriptions;
        subscriptionObj.judicialResults = objectVariant.results;
        return subscriptionObj;
    }
}

module.exports = async (context) => {
    return await new NowsVariantsSubscriptions(context).build();
};

