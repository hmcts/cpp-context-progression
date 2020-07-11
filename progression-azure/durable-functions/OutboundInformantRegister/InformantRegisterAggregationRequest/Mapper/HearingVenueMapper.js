const {HearingVenue} = require('../Model/HearingVenue');
const CourtSessionMapper = require('./CourtSessionMapper');

class HearingVenueMapper {

    constructor(context, informantRegister, hearingJson) {
        this.context = context;
        this.informantRegister = informantRegister;
        this.hearingJson = hearingJson;
    }
    build() {
        const hearingVenue = new HearingVenue();
        hearingVenue.courtHouse = this.hearingJson.courtCentre.name;
        hearingVenue.ljaName = (this.hearingJson.courtCentre.lja || {}).ljaName;
        hearingVenue.courtSessions = [new CourtSessionMapper(this.context, this.informantRegister, this.hearingJson).build()];
        return hearingVenue;
    }
}

module.exports = HearingVenueMapper;