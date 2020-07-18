const SubscriptionsService = require('../SubscriptionsService');
const SubscriptionObject = require('../SubscriptionObject');
const UserGroup = require('../../../SetNowVariants/UserGroup');
const UserGroupType = require('../../../SetNowVariants/UserGroupType');

class VocabularyInfo {
    constructor() {
        this.appearedByVideoLink = false;
        this.appearedInPerson = false;
        this.inCustody = false;
        this.custodyLocationIsPrison = false;
        this.custodyLocationIsPolice = false;
        this.allNonCustodialResults = false;
        this.atleastOneNonCustodialResult = false;
        this.atleastOneCustodialResult = false;
        this.youthDefendant = false;
        this.adultDefendant = false;
        this.adultOrYouthDefendant = false;
        this.welshCourtHearing = false;
        this.englishCourtHearing = false;
        this.anyCourtHearing = false;
    }
}

describe('Building Subscriptions Objects', () => {
    let subscriptions;
    let subscriptions1;
    let subscriptions2;
    let resultsWithIncludedPrompts;
    let resultsWithExcludedPrompts;
    let resultsWithIncludedResults;
    let resultsWithExcludedResults;

    beforeEach(() => {
        subscriptions = JSON.parse(JSON.stringify(require('./Subscriptions.json')));
        subscriptions1 = JSON.parse(JSON.stringify(require('./subscriptions-with-prompts.json')));
        subscriptions2 = JSON.parse(JSON.stringify(require('./subscriptions-with-inc-exc-results.json')));
        resultsWithIncludedPrompts = JSON.parse(JSON.stringify(require('./judicial-results-with-included-prompts.json')));
        resultsWithExcludedPrompts = JSON.parse(JSON.stringify(require('./judicial-results-with-excluded-prompts.json')));
        resultsWithIncludedResults = JSON.parse(JSON.stringify(require('./judicial-results-with-included-prompts.json')));
        resultsWithExcludedResults = JSON.parse(JSON.stringify(require('./judicial-results-with-excluded-prompts.json')));
    });

    test('Should exclude subscriptions if subscription vocabulary is not defined', async () => {
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.subscriptions = subscriptions;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);
        expect(response.length).toBe(0);
    });

    test('Should exclude subscriptions if subscription vocabulary is defined but NOT matched', async () => {
        subscriptions[0].subscriptionVocabulary.appearedByVideoLink = true;
       
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.vocabulary = new VocabularyInfo();
        subscriptionObj.subscriptions = subscriptions;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);
        expect(response.length).toBe(0);
    });

    test('Should include subscriptions if subscription vocabulary is defined AND matched', async () => {
        subscriptions[0].subscriptionVocabulary.appearedByVideoLink = true;
        subscriptions[0].subscriptionVocabulary.anyCourtHearing = true;
        subscriptions[0].subscriptionVocabulary.adultOrYouthDefendant = true;
        subscriptions[0].subscriptionVocabulary.inCustody = true;
        subscriptions[0].subscriptionVocabulary.allNonCustodialResults = false;
        subscriptions[0].subscriptionVocabulary.atleastOneNonCustodialResult = true;
        subscriptions[0].subscriptionVocabulary.atleastOneCustodialResult = true;

        const vocabulary = new VocabularyInfo();
        vocabulary.appearedByVideoLink = true;
        vocabulary.anyCourtHearing = true;
        vocabulary.adultOrYouthDefendant = true;
        vocabulary.inCustody = true;
        vocabulary.allNonCustodialResults = false;
        vocabulary.atleastOneNonCustodialResult = true;
        vocabulary.atleastOneCustodialResult = true;

        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.vocabulary = vocabulary;
        subscriptionObj.subscriptions = subscriptions;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);
        expect(response.length).toBe(1);
    });

    test('Should include subscriptions if subscription vocabulary is defined AND matched with results', async () => {
        subscriptions[0].subscriptionVocabulary.appearedByVideoLink = true;
        subscriptions[0].subscriptionVocabulary.anyCourtHearing = true;
        subscriptions[0].subscriptionVocabulary.adultOrYouthDefendant = true;
        subscriptions[0].subscriptionVocabulary.inCustody = true;
        subscriptions[0].subscriptionVocabulary.allNonCustodialResults = true;
        subscriptions[0].subscriptionVocabulary.atleastOneNonCustodialResult = true;
        subscriptions[0].subscriptionVocabulary.atleastOneCustodialResult = false;

        const vocabulary = new VocabularyInfo();
        vocabulary.appearedByVideoLink = true;
        vocabulary.anyCourtHearing = true;
        vocabulary.adultOrYouthDefendant = true;
        vocabulary.inCustody = true;
        vocabulary.allNonCustodialResults = true;
        vocabulary.atleastOneNonCustodialResult = true;
        vocabulary.atleastOneCustodialResult = false;

        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.vocabulary = vocabulary;
        subscriptionObj.subscriptions = subscriptions;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);
        expect(response.length).toBe(1);
    });

    test('Should include subscriptions if includedNOWS matched', async () => {

        subscriptions[0].subscriptionVocabulary.anyAppearance = true;
        subscriptions[0].subscriptionVocabulary.anyCourtHearing = true;
        subscriptions[0].subscriptionVocabulary.adultOrYouthDefendant = true;
        subscriptions[0].subscriptionVocabulary.ignoreCustody = true;
        subscriptions[0].subscriptionVocabulary.ignoreResults = true;

        const vocabulary = new VocabularyInfo();
        vocabulary.anyCourtHearing = true;
        vocabulary.adultOrYouthDefendant = true;

        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.nowId = '10115268-8efc-49fe-b8e8-feee216a03da';
        subscriptionObj.vocabulary = vocabulary;
        subscriptionObj.subscriptions = subscriptions;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);
        expect(response.length).toBe(1);
    });

    test('Should include subscriptions if includedNOWS NOT matched', async () => {
        const vocabulary = new VocabularyInfo();
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.nowId = 'dummy-now-id';
        subscriptionObj.vocabulary = vocabulary;
        subscriptionObj.subscriptions = subscriptions;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);
        expect(response.length).toBe(0);
    });

    test('Should NOT include subscriptions if excludedNOWS matched', async () => {
        subscriptions[0].includedNOWS = [];
        subscriptions[0].excludedNOWS = ['10115268-8efc-49fe-b8e8-feee216a03da'];
        const vocabulary = new VocabularyInfo();
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.nowId = '10115268-8efc-49fe-b8e8-feee216a03da';
        subscriptionObj.vocabulary = vocabulary;
        subscriptionObj.subscriptions = subscriptions;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);
        expect(response.length).toBe(0);
    });

    test('Should not return subscription where defence user group is excluded', async () => {
        const subscriptions = require('./SubscriptionWithUserGroup.json');
        const vocabulary = new VocabularyInfo();
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.subscriptions = subscriptions;
        subscriptionObj.vocabulary = vocabulary;

        const userGroup = new UserGroup();
        userGroup.type = UserGroupType.EXCLUDE;
        userGroup.userGroups.push('Defence');

        subscriptionObj.userGroup = userGroup;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);

        expect(response.length).toBe(0);
    });

    test('Should return subscription where Probation user group is excluded', async () => {
        const subscriptions = require('./SubscriptionWithUserGroup.json');
        const vocabulary = new VocabularyInfo();
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.subscriptions = subscriptions;
        subscriptionObj.vocabulary = vocabulary;

        const userGroup = new UserGroup();
        userGroup.type = UserGroupType.EXCLUDE;
        userGroup.userGroups.push('Probation');

        subscriptionObj.userGroup = userGroup;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);

        expect(response.length).toBe(1);
    });

    test('Should not return subscription where Probation user group is included and Defence User group is Included in Subscription metadata', async () => {
        const subscriptions = require('./SubscriptionWithUserGroup.json');
        const vocabulary = new VocabularyInfo();
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.subscriptions = subscriptions;
        subscriptionObj.vocabulary = vocabulary;

        const userGroup = new UserGroup();
        userGroup.type = UserGroupType.INCLUDE;
        userGroup.userGroups.push('Probation');

        subscriptionObj.userGroup = userGroup;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);

        expect(response.length).toBe(0);
    });

    test('Should return subscription where defence usergroup is included', async () => {
        const subscriptions = require('./SubscriptionWithUserGroup.json');
        const vocabulary = new VocabularyInfo();
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.subscriptions = subscriptions;
        subscriptionObj.vocabulary = vocabulary;

        const userGroup = new UserGroup();
        userGroup.type = UserGroupType.INCLUDE;
        userGroup.userGroups.push('Defence');
        subscriptionObj.userGroup = userGroup;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);

        expect(response.length).toBe(1);
    });

    test('Should return subscription where no usergroup is set in the variant', async () => {
        const subscriptions = require('./SubscriptionWithUserGroup.json');
        const vocabulary = new VocabularyInfo();
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.subscriptions = subscriptions;
        subscriptionObj.vocabulary = vocabulary;

        subscriptionObj.userGroup = undefined;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);

        expect(response.length).toBe(1);
    });

    test('Should return subscription where Defence usergroup is included in the variant and no userGroupVariant in Subscription metadata', async () => {
        subscriptions[0].subscriptionVocabulary.anyAppearance = true;
        subscriptions[0].subscriptionVocabulary.anyCourtHearing = true;
        subscriptions[0].subscriptionVocabulary.adultOrYouthDefendant = true;
        subscriptions[0].subscriptionVocabulary.ignoreCustody = true;
        subscriptions[0].subscriptionVocabulary.ignoreResults = true;

        const vocabulary = new VocabularyInfo();
        vocabulary.anyCourtHearing = true;
        vocabulary.adultOrYouthDefendant = true;

        const userGroup = new UserGroup();
        userGroup.type = UserGroupType.INCLUDE;
        userGroup.userGroups.push('Defence');

        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.vocabulary = vocabulary;
        subscriptionObj.userGroup = userGroup;
        subscriptionObj.subscriptions = subscriptions;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);

        expect(response.length).toBe(1);
    });

    test('Should return subscription where Defence usergroup is excluded in the variant and no userGroupVariant in Subscription metadata', async () => {
        subscriptions[0].subscriptionVocabulary.anyAppearance = true;
        subscriptions[0].subscriptionVocabulary.anyCourtHearing = true;
        subscriptions[0].subscriptionVocabulary.adultOrYouthDefendant = true;
        subscriptions[0].subscriptionVocabulary.ignoreCustody = true;
        subscriptions[0].subscriptionVocabulary.ignoreResults = true;

        const vocabulary = new VocabularyInfo();
        vocabulary.anyCourtHearing = true;
        vocabulary.adultOrYouthDefendant = true;

        const userGroup = new UserGroup();
        userGroup.type = UserGroupType.EXCLUDE;
        userGroup.userGroups.push('Defence');

        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.vocabulary = vocabulary;
        subscriptionObj.userGroup = userGroup;
        subscriptionObj.subscriptions = subscriptions;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);

        expect(response.length).toBe(1);
    });

    test('Should create a clone for child subscription', async () => {
        const childSubscription = Object.assign({}, subscriptions[0]);
        subscriptions[0].childSubscriptions = [];
        subscriptions[0].childSubscriptions.push(childSubscription);

        subscriptions[0].subscriptionVocabulary.anyAppearance = true;
        subscriptions[0].subscriptionVocabulary.anyCourtHearing = true;
        subscriptions[0].subscriptionVocabulary.adultOrYouthDefendant = true;
        subscriptions[0].subscriptionVocabulary.ignoreCustody = true;
        subscriptions[0].subscriptionVocabulary.ignoreResults = true;

        const vocabulary = new VocabularyInfo();
        vocabulary.anyCourtHearing = true;
        vocabulary.adultOrYouthDefendant = true;

        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.vocabulary = vocabulary;
        subscriptionObj.subscriptions = subscriptions;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);
        expect(response.length).toBe(2);
    });

    test('Should return the correct subscriptions for court register', async () => {

        subscriptions[0].isNowSubscription = false;
        subscriptions[0].isCourtRegisterSubscription = true;
        subscriptions[0].selectedCourtHouses = ['OU_CODE'];
        subscriptions[0].subscriptionVocabulary.youthDefendant = true;
        subscriptions[0].subscriptionVocabulary.anyAppearance = true;
        subscriptions[0].subscriptionVocabulary.anyCourtHearing = true;
        subscriptions[0].subscriptionVocabulary.ignoreCustody = true;
        subscriptions[0].subscriptionVocabulary.ignoreResults = true;

        const vocabulary = new VocabularyInfo();
        vocabulary.anyCourtHearing = true;
        vocabulary.youthDefendant = true;

        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.vocabulary = vocabulary;
        subscriptionObj.subscriptions = subscriptions;
        subscriptionObj.ouCode = 'OU_CODE';
        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);
        expect(response.length).toBe(1);
    });

    test('Should include subscriptions if included Prompts are matched with result prompts', async () => {
        subscriptions1[0].subscriptionVocabulary.anyAppearance = true;
        subscriptions1[0].subscriptionVocabulary.anyCourtHearing = true;
        subscriptions1[0].subscriptionVocabulary.adultOrYouthDefendant = true;
        subscriptions1[0].subscriptionVocabulary.ignoreCustody = true;
        subscriptions1[0].subscriptionVocabulary.ignoreResults = true;

        const vocabulary = new VocabularyInfo();
        vocabulary.anyCourtHearing = true;
        vocabulary.adultOrYouthDefendant = true;

        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.nowId = '10115268-8efc-49fe-b8e8-feee216a03da';
        subscriptionObj.vocabulary = vocabulary;
        subscriptionObj.subscriptions = subscriptions1;
        subscriptionObj.judicialResults = resultsWithIncludedPrompts.judicialResults;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);
        expect(response.length).toBe(1);
    });

    test('Should Not include subscriptions if excluded Prompts are matched with result prompts', async () => {
        subscriptions1[0].subscriptionVocabulary.anyAppearance = true;
        subscriptions1[0].subscriptionVocabulary.anyCourtHearing = true;
        subscriptions1[0].subscriptionVocabulary.adultOrYouthDefendant = true;
        subscriptions1[0].subscriptionVocabulary.ignoreCustody = true;
        subscriptions1[0].subscriptionVocabulary.ignoreResults = true;

        const vocabulary = new VocabularyInfo();
        vocabulary.anyCourtHearing = true;
        vocabulary.adultOrYouthDefendant = true;

        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.nowId = '10115268-8efc-49fe-b8e8-feee216a03da';
        subscriptionObj.vocabulary = vocabulary;
        subscriptionObj.subscriptions = subscriptions1;
        subscriptionObj.judicialResults = resultsWithExcludedPrompts.judicialResults;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);
        expect(response.length).toBe(0);
    });

    test('Should include subscriptions if included results are matched with results', async () => {
        subscriptions[0].subscriptionVocabulary.anyAppearance = true;
        subscriptions[0].subscriptionVocabulary.anyCourtHearing = true;
        subscriptions[0].subscriptionVocabulary.adultOrYouthDefendant = true;
        subscriptions[0].subscriptionVocabulary.ignoreCustody = true;
        subscriptions[0].subscriptionVocabulary.ignoreResults = true;

        const vocabulary = new VocabularyInfo();
        vocabulary.anyCourtHearing = true;
        vocabulary.adultOrYouthDefendant = true;

        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.nowId = '10115268-8efc-49fe-b8e8-feee216a03da';
        subscriptionObj.vocabulary = vocabulary;
        subscriptionObj.subscriptions = subscriptions;
        subscriptionObj.judicialResults = resultsWithIncludedResults.judicialResults;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);
        expect(response.length).toBe(1);
    });

    test('Should Not include subscriptions if excluded results are matched with results', async () => {
        const vocabulary = new VocabularyInfo();
        const subscriptionObj = new SubscriptionObject();
        subscriptionObj.nowId = '10115268-8efc-49fe-b8e8-feee216a03da';
        subscriptionObj.vocabulary = vocabulary;
        subscriptionObj.subscriptions = subscriptions2;
        subscriptionObj.judicialResults = resultsWithExcludedResults.judicialResults;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObj);
        expect(response.length).toBe(0);
    });

    test('Should return the correct subscriptions for court register 2', async () => {
        const subscriptionObject = JSON.parse(JSON.stringify(require('./subscriptionObject.json')));

        const vocabulary = new VocabularyInfo();
        vocabulary.anyCourtHearing = true;
        vocabulary.adultOrYouthDefendant = true;

        const response = await new SubscriptionsService().getSubscriptions(subscriptionObject);
        expect(response.length).toBe(1);
    });

});
