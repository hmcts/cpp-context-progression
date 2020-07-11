const VocabularyService = require('../NowsHelper/service/VocabularyService');
const DefendantContextBaseService = require('../NowsHelper/service/DefendantContextBaseService');
const dateService = require('../NowsHelper/service/DateService');
const { getLatestOrderedDate, getHearingDate, filterResultsAvailableForCourtExtract } = require('../NowsHelper/service/RegisterFragmentService');

class InformantRegisterFragment {
    constructor() {
        this.registerDate = undefined;
        this.hearingDate = undefined;
        this.hearingId = undefined;
        this.prosecutionAuthorityId = undefined;
        this.prosecutionAuthorityCode = undefined;
        this.prosecutionAuthorityName = undefined;
        this.majorCreditorCode = undefined;
        this.prosecutionAuthorityName = undefined;
        this.registerDefendants = [];
        this.matchedSubscriptions = undefined;
    }
}

class SetInformantRegisterBuilder {

    constructor(context, hearingResultedObj, sharedTime) {
        this.context = context;
        this.hearingResultedObj = hearingResultedObj;
        this.sharedTime = sharedTime;
    }

    build() {

        const informantRegisterFragments = [];
        const prosecutionCaseIdentifiers = [];
        const prosecutionCaseIdentifiersSet = new Set();

        this.hearingResultedObj.prosecutionCases.forEach(prosecutionCase => {
            if(!prosecutionCaseIdentifiersSet.has(prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityId)) {
                const caseDetails = {
                    prosecutionAuthorityId: prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityId,
                    prosecutionAuthorityCode: prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityCode,
                    majorCreditorCode: prosecutionCase.prosecutionCaseIdentifier.majorCreditorCode,
                    prosecutionAuthorityName: prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityName
                };
                prosecutionCaseIdentifiersSet.add(prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityId);
                prosecutionCaseIdentifiers.push(caseDetails);
            }
        });

        const defendantContextBaseList = new DefendantContextBaseService(this.hearingResultedObj).getDefendantContextBaseList();
        const orderedDates = this.getOrderedDates(defendantContextBaseList);

        const latestOrderedDate = getLatestOrderedDate(orderedDates);

        filterResultsAvailableForCourtExtract(defendantContextBaseList);

        this.setVocabulary(defendantContextBaseList);

        prosecutionCaseIdentifiers.forEach(detail => {
            const informantRegisterFragment = new InformantRegisterFragment();
            informantRegisterFragment.hearingDate = getHearingDate(latestOrderedDate, this.hearingResultedObj);
            informantRegisterFragment.registerDate = dateService.getLocalDateTime(this.sharedTime);
            informantRegisterFragment.hearingId = this.hearingResultedObj.id;
            informantRegisterFragment.prosecutionAuthorityId = detail.prosecutionAuthorityId;
            informantRegisterFragment.prosecutionAuthorityCode = detail.prosecutionAuthorityCode;
            informantRegisterFragment.majorCreditorCode = detail.majorCreditorCode;
            informantRegisterFragment.prosecutionAuthorityName = detail.prosecutionAuthorityName;
            informantRegisterFragment.registerDefendants = this.getDefendantsFromContext(defendantContextBaseList, detail.prosecutionAuthorityId);
            informantRegisterFragments.push(informantRegisterFragment);
        });

        return informantRegisterFragments;
    }

    getOrderedDates(defendantContextBaseList) {
        const datesToSort = new Set();

        defendantContextBaseList.forEach((defendantContextBase) => {
            defendantContextBase.results.forEach(r => {
                datesToSort.add(r.judicialResult.orderedDate);
            });
        });

        return datesToSort;
    }

    getDefendantsFromContext(defendantContextBaseList, prosecutionAuthorityId) {
        const masterDefendantIdSet = new Set();
        (this.hearingResultedObj.prosecutionCases || [])
            .filter(prosecutionCase => prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityId === prosecutionAuthorityId)
            .map(prosecutionCase => prosecutionCase.defendants)
            .reduce((defendant1, defendant2) => defendant1.concat(defendant2))
            .map(defendant => defendant.masterDefendantId)
            .forEach(defendant => masterDefendantIdSet.add(defendant));

        return defendantContextBaseList.filter(defendant => masterDefendantIdSet.has(defendant.masterDefendantId));
    }

    setVocabulary(defendantContextBaseList) {
        defendantContextBaseList.forEach(defendantContextBase => {
            defendantContextBase.vocabulary = new VocabularyService(this.hearingResultedObj, defendantContextBase).getVocabularyInfo();
        });
    }
}

module.exports = async (context) => {

    const hearingResultedObj = context.bindings.params.hearingResultedObj;
    const sharedTime = context.bindings.params.sharedTime;

    return await new SetInformantRegisterBuilder(context, hearingResultedObj, sharedTime).build();
};