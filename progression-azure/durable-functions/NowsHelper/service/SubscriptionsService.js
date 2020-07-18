const INCLUDE = 'include';
const EXCLUDE = 'exclude';

class SubscriptionsService {

    getSubscriptions(subscriptionObject) {

        const subscriptions = [];
        subscriptionObject.subscriptions.forEach(subscription => {

            if (matchCourtHouse(subscription, subscriptionObject.ouCode) && matchVocabularyRules(subscription, subscriptionObject)) {
                subscriptions.push(subscription);
                return;
            }

            if (matchProsecutor(subscription, subscriptionObject.ouCode) && matchVocabularyRules(subscription, subscriptionObject)) {
                subscriptions.push(subscription);
                return;
            }

            if ((subscription.isNowSubscription ||  subscription.isEDTSubscription) && matchSubscriptionRules(subscriptionObject, subscription)) {
                subscriptions.push(subscription);
                (subscription.childSubscriptions || []).forEach(
                    (childSubscription) => {
                        if (matchSubscriptionRules(subscriptionObject, childSubscription)) {
                            subscriptions.push(childSubscription);
                        }
                    });
            }

            if (subscription.isPrisonCourtRegisterSubscription && matchVocabularyRules(subscription, subscriptionObject)) {
                subscriptions.push(subscription);
            }
        });

        console.log('subscriptions -->>', JSON.stringify(subscriptions));

        return subscriptions;
    }
}

function matchProsecutor(subscription, ouCode) {
    return subscription.informantCode && subscription.informantCode === ouCode;
}

function matchCourtHouse(subscription, ouCode) {
    return subscription.selectedCourtHouses && subscription.selectedCourtHouses.includes(ouCode);
}

function matchSubscriptionRules(subscriptionObject, subscription) {
    const nowId = subscriptionObject.nowId;

    if (nowId && (subscription.excludedNOWS || []).includes(nowId)) {
        return false;
    }

    if (nowId && subscription.includedNOWS && !subscription.includedNOWS.includes(nowId)) {
        return false;
    }

    // Check user groups whitelist
    if (subscriptionObject.userGroup) {
        return matchVariantUserGroupsWithSubscriptionMetadata(subscription, subscriptionObject);
    } else {
        return matchVocabularyRules(subscription, subscriptionObject);
    }
}

function matchVariantUserGroupsWithSubscriptionMetadata(subscription, subscriptionObject) {
    const userGroupObj = subscriptionObject.userGroup;
    const failedToMatchIncludedUserGroups = checkIfIncludedUserGroupsAreNotMatchingWithSubscription(userGroupObj, subscription);

    if(failedToMatchIncludedUserGroups) {
        return false;
    }
    const matchedWithExcludedUserGroups = checkIfExcludedUserGroupAreMatchingWithSubscription(userGroupObj, subscription);

    if(matchedWithExcludedUserGroups) {
        return false;
    }

    return matchVocabularyRules(subscription, subscriptionObject);
}

function checkIfIncludedUserGroupsAreNotMatchingWithSubscription(userGroupObj, subscription) {
    if(userGroupObj.type === INCLUDE && subscription.userGroupVariants && subscription.userGroupVariants.length) {
        return userGroupObj.userGroups.some((userGroup) => !(subscription.userGroupVariants).includes(userGroup));
    }
}

function checkIfExcludedUserGroupAreMatchingWithSubscription(userGroupObj, subscription) {
    if(userGroupObj.type === EXCLUDE && subscription.userGroupVariants && subscription.userGroupVariants.length) {
        return userGroupObj.userGroups.some((userGroup) => (subscription.userGroupVariants || []).includes(userGroup));
    }
}

function matchVocabularyRules(subscription, subscriptionObject) {

    if (subscription.applySubscriptionRules) {
        if (subscriptionObject.vocabulary === undefined) {
            return false;
        }

        if (subscription.subscriptionVocabulary) {

            //check attendanceType
            if(!checkIfAttendanceTypeMatch(subscription.subscriptionVocabulary, subscriptionObject.vocabulary)) {
                console.log(subscription.name + ' checkIfAttendanceTypeMatch failed.');
                return false;
            }

            //check court house
            if(!checkIfCourtHouseMatch(subscription.subscriptionVocabulary, subscriptionObject.vocabulary)) {
                console.log(subscription.name + ' checkIfCourtHouseMatch failed.');
                return false;
            }

            //check Defendant
            if(!checkIfDefendantMatch(subscription.subscriptionVocabulary, subscriptionObject.vocabulary)) {
                console.log(subscription.name + ' checkIfDefendantMatch failed.');
                return false;
            }

            //check custody
            if(!checkIfCustodyMatch(subscription.subscriptionVocabulary, subscriptionObject.vocabulary)) {
                console.log(subscription.name + ' checkIfCustodyMatch failed.');
                return false;
            }

            //check custodial Results
            if(!checkIfCustodialResultMatch(subscription.subscriptionVocabulary, subscriptionObject.vocabulary)) {
                console.log(subscription.name + ' checkIfCustodialResultMatch failed.');
                return false;
            }

            if (subscription.subscriptionVocabulary.includedPrompts) {
                const hasMatchedPrompts = checkForMatchedPrompts(subscriptionObject.judicialResults, subscription.subscriptionVocabulary.includedPrompts);
                if (!hasMatchedPrompts) {
                    return false;
                }
            }

            if (subscription.subscriptionVocabulary.excludedPrompts) {
                const hasMatchedPrompts = checkForMatchedPrompts(subscriptionObject.judicialResults, subscription.subscriptionVocabulary.excludedPrompts);
                if (hasMatchedPrompts) {
                    return false;
                }
            }

            if (subscription.subscriptionVocabulary.includedResults) {
                const hasMatchedResults = checkForMatchedResults(subscriptionObject.judicialResults, subscription.subscriptionVocabulary.includedResults);
                if (!hasMatchedResults) {
                    return false;
                }
            }

            if (subscription.subscriptionVocabulary.excludedResults) {
                const hasMatchedResults = checkForMatchedResults(subscriptionObject.judicialResults, subscription.subscriptionVocabulary.excludedResults);
                if (hasMatchedResults) {
                    return false;
                }
            }
        }
    }
    return true;
}

function checkForMatchedPrompts(judicialResults, promptsFromRefData) {
    const judicialResultsWithPrompts = judicialResults.filter(judicialResult => judicialResult.judicialResultPrompts);
    const hasMatchedPrompts = judicialResultsWithPrompts.some(
        result =>
            result.judicialResultPrompts.some(
                judicialPrompt =>
                    promptsFromRefData.some(prompt => getMatchingPrompt(prompt, judicialPrompt))));

    return !!hasMatchedPrompts;
}

function getMatchingPrompt(prompt, judicialPrompt) {
    return judicialPrompt.promptReference
           && judicialPrompt.promptReference.toLowerCase().includes(prompt.resultPromptReference.toLowerCase());
}

function checkForMatchedResults(judicialResults, resultsFromRefData) {
    const hasMatchedResults = judicialResults.some(
        judicialResult => resultsFromRefData.some(
            resultFromRefData =>
                resultFromRefData === judicialResult.judicialResultTypeId));
    return !!hasMatchedResults;
}

function checkIfAnyPropertyFailedToMatch(subscriptionVocabulary, objectVocabulary, property) {
    return (subscriptionVocabulary[property] && !objectVocabulary[property]);
}

function checkIfAttendanceTypeMatch(subscriptionVocabulary, vocabulary) {

    if(subscriptionVocabulary.anyAppearance && !vocabulary.appearedByVideoLink && !vocabulary.appearedInPerson) {
        return true;
    }

    if((subscriptionVocabulary.anyAppearance && vocabulary.appearedByVideoLink) ||
       (subscriptionVocabulary.anyAppearance && vocabulary.appearedInPerson))  {
        return true;
    }

    if(subscriptionVocabulary.appearedByVideoLink && vocabulary.appearedByVideoLink) {
        return true;
    }

    return subscriptionVocabulary.appearedInPerson && vocabulary.appearedInPerson;
}

function checkIfCourtHouseMatch(subscriptionVocabulary, vocabulary) {

    if((subscriptionVocabulary.anyCourtHearing && vocabulary.anyCourtHearing) ||
       (subscriptionVocabulary.anyCourtHearing && vocabulary.englishCourtHearing) ||
       (subscriptionVocabulary.anyCourtHearing && vocabulary.welshCourtHearing))  {
        return true;
    }

    if(subscriptionVocabulary.englishCourtHearing && vocabulary.englishCourtHearing) {
        return true;
    }

    return subscriptionVocabulary.welshCourtHearing && vocabulary.welshCourtHearing;
}

function checkIfDefendantMatch(subscriptionVocabulary, vocabulary) {

    if((subscriptionVocabulary.adultOrYouthDefendant && vocabulary.adultOrYouthDefendant) ||
       (subscriptionVocabulary.adultOrYouthDefendant && vocabulary.youthDefendant) ||
       (subscriptionVocabulary.adultOrYouthDefendant && vocabulary.adultDefendant))  {
        return true;
    }

    if(subscriptionVocabulary.youthDefendant && vocabulary.youthDefendant) {
        return true;
    }

    return subscriptionVocabulary.adultDefendant && vocabulary.adultDefendant;
}

function checkIfCustodyMatch(subscriptionVocabulary, vocabulary) {

    if(subscriptionVocabulary.ignoreCustody) {
        return true;
    }

    if(subscriptionVocabulary.inCustody && !subscriptionVocabulary.custodyLocationIsPolice &&
       !subscriptionVocabulary.custodyLocationIsPrison && vocabulary.inCustody) {
        return true;
    }

    if(subscriptionVocabulary.inCustody &&
       subscriptionVocabulary.custodyLocationIsPolice &&
       !subscriptionVocabulary.custodyLocationIsPrison &&
       vocabulary.custodyLocationIsPolice) {
        return true;
    }

    return subscriptionVocabulary.inCustody &&
           !subscriptionVocabulary.custodyLocationIsPolice &&
           subscriptionVocabulary.custodyLocationIsPrison &&
           vocabulary.custodyLocationIsPrison;
}

function checkIfCustodialResultMatch(subscriptionVocabulary, vocabulary) {

    if(subscriptionVocabulary.ignoreResults) {
        return true;
    }

    if(subscriptionVocabulary.allNonCustodialResults && vocabulary.allNonCustodialResults
       && (subscriptionVocabulary.atleastOneCustodialResult === vocabulary.atleastOneCustodialResult)) {
        return true;
    }

    return subscriptionVocabulary.atleastOneNonCustodialResult && vocabulary.atleastOneNonCustodialResult
           && (subscriptionVocabulary.atleastOneCustodialResult === vocabulary.atleastOneCustodialResult);
}

module.exports = SubscriptionsService;
