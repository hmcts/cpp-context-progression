const HearingVenueMapper = require('../HearingVenueMapper');


describe('HearingVenueMapper builds correctly', () => {
    test('build hearing venue with all values', () => {
        const hearingResult = require('./hearing.json');

        const hearingVenueMapper = new HearingVenueMapper(hearingResult);
        const result = hearingVenueMapper.build();

        expect(result.ljaName).toBe("LJA Name");
        expect(result.courtHouse).toBe("Court Centre Name");
    });

});