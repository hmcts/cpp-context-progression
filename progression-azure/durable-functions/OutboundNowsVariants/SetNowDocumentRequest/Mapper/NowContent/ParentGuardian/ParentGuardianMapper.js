const NowPartyMapper = require('../NowPartyMapper');

class ParentGuardianMapper extends NowPartyMapper {
    constructor(nowVariant, hearingJson) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
    }

    buildParentGuardian() {
        const defendantFromHearingJson = this.getDefendant();
        const parentGuardianFromHearing = this.getParentGuardian(defendantFromHearingJson);
        if(parentGuardianFromHearing) {
            return this.getNowParty(parentGuardianFromHearing);
        }
    }
}

module.exports = ParentGuardianMapper;
