const VocabularyService = require('../NowsHelper/service/VocabularyService');
const DefendantContextBaseService = require('../NowsHelper/service/DefendantContextBaseService');
const dateService = require('../NowsHelper/service/DateService');
const { getLatestOrderedDate, getHearingDate, filterResultsAvailableForCourtExtract } = require('../NowsHelper/service/RegisterFragmentService');

class CourtRegisterFragment {
    constructor() {
        this.courtCenterId = undefined;
        this.registerDate = undefined;
        this.hearingDate = undefined;
        this.hearingId = undefined;
        this.registerDefendants = [];
        this.courtCentreOUCode = undefined;
        this.matchedSubscriptions = [];
    }
}

class CourtRegisterBuilder {

    constructor(hearingResultedObj, sharedTime) {
        this.hearingResultedObj = hearingResultedObj
        this.sharedTime = sharedTime;
    }

    build() {

        const defendantContextBaseList = new DefendantContextBaseService(this.hearingResultedObj).getDefendantContextBaseList();
        const orderedDates = this.getOrderedDates(defendantContextBaseList);

        const latestOrderedDate = getLatestOrderedDate(orderedDates);

        filterResultsAvailableForCourtExtract(defendantContextBaseList);

        this.setVocabulary(defendantContextBaseList);

        const courtRegisterFragment = new CourtRegisterFragment();

        courtRegisterFragment.registerDefendants = defendantContextBaseList;
        courtRegisterFragment.courtCenterId = this.hearingResultedObj.courtCentre.id;
        courtRegisterFragment.courtCentreOUCode = this.hearingResultedObj.courtCentre.code;
        courtRegisterFragment.hearingDate = getHearingDate(latestOrderedDate, this.hearingResultedObj);
        courtRegisterFragment.registerDate = dateService.getLocalDateTime(this.sharedTime);
        courtRegisterFragment.hearingId = this.hearingResultedObj.id;

        return courtRegisterFragment;
    }

    setVocabulary(defendantContextBaseList) {
        defendantContextBaseList.forEach(defendantContextBase => {
            defendantContextBase.vocabulary = new VocabularyService(this.hearingResultedObj, defendantContextBase).getVocabularyInfo();
        });
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
}

module.exports = async function (context) {
    const hearingResultedObj = context.bindings.params.hearingResultedObj;
    const sharedTime = context.bindings.params.sharedTime;
    return new CourtRegisterBuilder(hearingResultedObj, sharedTime).build();
};
