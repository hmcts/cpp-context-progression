const VocabularyService = require('../NowsHelper/service/VocabularyService');
const DefendantContextBaseService = require('../NowsHelper/service/DefendantContextBaseService');
const { getLatestOrderedDate, getHearingDate, filterJudicialResultsApplicableForRegisters } = require('../NowsHelper/service/RegisterFragmentService');

class PrisonCourtRegisterFragment {
    constructor() {
        this.courtCentreId = undefined;
        this.registerDefendant = undefined;
        this.matchedSubscriptions = undefined;
        this.hearingDate = undefined;
        this.hearingId = undefined;
    }
}

class PrisonCourtRegisterBuilder {
    constructor(hearingResultedObj) {
        this.hearingResultedObj = hearingResultedObj;
    }

    build() {
        const prisonCourtRegisters = [];
        const defendantContextBaseList = new DefendantContextBaseService(this.hearingResultedObj).getDefendantContextBaseList();
        defendantContextBaseList.forEach(defendantContextBase => {
            defendantContextBase.vocabulary = new VocabularyService(this.hearingResultedObj, defendantContextBase).getVocabularyInfo();

            const orderedDates = this.getOrderedDates(defendantContextBase);
            const latestOrderedDate = getLatestOrderedDate(orderedDates);

            const prisonCourtRegister = new PrisonCourtRegisterFragment();
            filterJudicialResultsApplicableForRegisters(defendantContextBase);
            prisonCourtRegister.registerDefendant = defendantContextBase;
            prisonCourtRegister.hearingDate = getHearingDate(latestOrderedDate, this.hearingResultedObj);
            prisonCourtRegister.hearingId = this.hearingResultedObj.id;
            prisonCourtRegisters.push(prisonCourtRegister);
        });
        return prisonCourtRegisters;
    }

    getOrderedDates(defendantContextBase) {
        const datesToSort = new Set();

        defendantContextBase.results.forEach(r => {
            datesToSort.add(r.judicialResult.orderedDate);
        });

        return datesToSort;
    }
}

module.exports = async (context) => {
    const hearingResultedObj = context.bindings.params.hearingResultedObj;
    return await new PrisonCourtRegisterBuilder(hearingResultedObj).build();
};