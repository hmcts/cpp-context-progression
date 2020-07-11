const {CourtSession} = require('../Model/HearingVenue');
const DefendantMapper = require('../Mapper/DefendantMapper');
const dateService = require('../../../NowsHelper/service/DateService');

class CourtSessionMapper {

    constructor(context, informantRegister, hearingJson) {
        this.context = context;
        this.informantRegister = informantRegister;
        this.hearingJson = hearingJson;
    }

    build() {
        const courtSession = new CourtSession();
        courtSession.courtRoom = this.hearingJson.courtCentre.roomName;
        courtSession.hearingStartTime = this.getHearingStartTime();
        courtSession.defendants = this.getDefendants();
        return courtSession;
    }

    getDefendants() {
        return new DefendantMapper(this.context, this.informantRegister, this.hearingJson).build();
    }

    getHearingStartTime() {
        if(this.hearingJson.hearingDays[0].sittingDay) {
            const hearingTime = this.hearingJson.hearingDays[0].sittingDay;
            return dateService.getLocalDateTime(hearingTime);
        }
    }
}

module.exports = CourtSessionMapper;