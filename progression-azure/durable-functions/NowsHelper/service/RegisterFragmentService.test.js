
const { getLatestOrderedDate, getHearingDate, filterResultsAvailableForCourtExtract } = require('./RegisterFragmentService');

describe('RegisterFragmentService', () => {

    test('getLatestOrderedDate', () => {
        expect(getLatestOrderedDate).toBeInstanceOf(Function);
    });

    test('getHearingDate', () => {
        expect(getHearingDate).toBeInstanceOf(Function);
    });

    test('filterResultsAvailableForCourtExtract', () => {
        expect(filterResultsAvailableForCourtExtract).toBeInstanceOf(Function);
    });
});