const HearingVenueMapper = require('../HearingVenueMapper');

describe('Hearing Venue Mapper', () => {

    test('Should return correct values', () => {
        const fakeHearingJson = {
            "courtCentre": {
                "name": "Carmarthen Magistrates' Court",
                "lja": {
                    "ljaName": "Mersey Courts"
                }
            }
        };

        const result = new HearingVenueMapper(fakeHearingJson).build();

        expect(result).toBeTruthy();
        expect(result.courtHouse).toBe(fakeHearingJson.courtCentre.name);
        expect(result.ljaName).toBe(fakeHearingJson.courtCentre.lja.ljaName);

    });
});