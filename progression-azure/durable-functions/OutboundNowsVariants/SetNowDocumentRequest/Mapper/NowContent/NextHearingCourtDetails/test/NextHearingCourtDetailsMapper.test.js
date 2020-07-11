const NextHearingCourtDetailsMapper = require('../NextHearingCourtDetailsMapper');
const moment = require('moment-timezone');

describe('HearingCourtDetails mapper builds correctly', () => {
    test('build hearing court details with all values', () => {
        const hearingJson = require('../../test/hearing.json');
        const nowVariantJson = require('../../test/now-variant.json');
        const nextHearingDetailsMapper = new NextHearingCourtDetailsMapper(nowVariantJson,
                                                                           hearingJson);
        const nextHearingDetails = nextHearingDetailsMapper.buildNextHearingDetails();

        const hearingDate = '2020-08-18T09:00:00.000Z';
        const expectedDate = moment.tz(hearingDate, "Europe/London").format('YYYY-MM-DD');
        const expectedTime = moment.tz(hearingDate, "Europe/London").format('HH:mm');


        expect(nextHearingDetails.courtName).toBe('Wood Green Crown Court');
        expect(nextHearingDetails.welshCourtName).toBe('Llys y Goron Wood Green');
        expect(nextHearingDetails.roomName).toBe('Court 12');
        expect(nextHearingDetails.welshRoomName).toBe('Corte 12');

        expect(nextHearingDetails.hearingDate).toBe(expectedDate);
        expect(nextHearingDetails.hearingTime).toBe(expectedTime);
        expect(nextHearingDetails.hearingWeekCommencing).toBeUndefined();

        expect(nextHearingDetails.courtAddress.line1).toBe('Woodall House');
        expect(nextHearingDetails.courtAddress.line2).toBe('Lordship Ln');
        expect(nextHearingDetails.courtAddress.line3).toBe('Wood Green');
        expect(nextHearingDetails.courtAddress.line4).toBe('London');
        expect(nextHearingDetails.courtAddress.line5).toBe('GB');
        expect(nextHearingDetails.courtAddress.postCode).toBe('N22 5LF');

        expect(nextHearingDetails.welshCourtAddress.line1).toBe('welshAddress 1');
        expect(nextHearingDetails.welshCourtAddress.line2).toBe('welshAddress 2');
        expect(nextHearingDetails.welshCourtAddress.line3).toBe('welshAddress 3');
        expect(nextHearingDetails.welshCourtAddress.line4).toBe('welshAddress 4');
        expect(nextHearingDetails.welshCourtAddress.line5).toBe('GB');
        expect(nextHearingDetails.welshCourtAddress.postCode).toBe('W33 5LH');

    });
});