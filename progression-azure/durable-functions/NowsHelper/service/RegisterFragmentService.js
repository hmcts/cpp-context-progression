const dateService = require('./DateService');

const filterResultsAvailableForCourtExtract = (defendantContextBaseList) => {
    defendantContextBaseList.forEach(defendantContextBase => {
        // available for court extract at result level
        const filteredJudicialResults = defendantContextBase.results.filter(r => (r.judicialResult.isAvailableForCourtExtract && !r.judicialResult.publishedForNows));

        // available for court extract at prompt level
        filteredJudicialResults.forEach(r => {
            if(r.judicialResult.judicialResultPrompts) {
                r.judicialResult.judicialResultPrompts =
                    r.judicialResult.judicialResultPrompts.filter(p => p.isAvailableForCourtExtract);
            }
        });

        defendantContextBase.results = filteredJudicialResults;
    });
};

const filterJudicialResultsApplicableForRegisters = (defendantContextBase) => {
    const filteredJudicialResults = defendantContextBase.results.filter(r => !r.judicialResult.publishedForNows)
    defendantContextBase.results = filteredJudicialResults;
}

const getLatestOrderedDate = (datesToSort) => {
    try {
        const sortedDates = [...datesToSort].sort((a, b) => {
            return dateService.parse(b).getTime() - dateService.parse(a).getTime();
        });

        return sortedDates[0];
    }
    catch (ex) {
        //TODO HOW THE ORCHESTRATOR WILL HANDLE THIS.
        this.context.log('Could not parse date', ex);
    }

    return undefined;
};

const getHearingDate = (orderedDate, hearingResultedObj) => {
    if(hearingResultedObj.hearingDays) {
        const hearingDay = hearingResultedObj.hearingDays.find(hearingDay => dateService.getLocalDate(hearingDay.sittingDay) === orderedDate);
        if(hearingDay) {
            return dateService.getLocalDateTime(hearingDay.sittingDay);
        } else {
            return dateService.getLocalDateTime(orderedDate);
        }
    }
}

module.exports = { getLatestOrderedDate, getHearingDate, filterResultsAvailableForCourtExtract, filterJudicialResultsApplicableForRegisters };