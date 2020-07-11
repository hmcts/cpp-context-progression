const NowPartyMapper = require('../NowPartyMapper');

class ThirdPartyMapper extends NowPartyMapper {
    constructor(nowVariant, hearingJson) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
    }

    buildThirdParties() {
        if(this.getThirdParties()) {
            const thirdParties = []
            const thirdPartiesFromHearing = this.getThirdParties();

            thirdPartiesFromHearing.forEach(appParty => {
                const thirdParty = this.getNowPartyMapper(appParty.partyDetails);
                if(thirdParties.length && this.isDistinctNowParty(thirdParties, thirdParty)) {
                    thirdParties.push(thirdParty);
                }
                if(!thirdParties.length) {
                    thirdParties.push(thirdParty);
                }
            });

            if(thirdParties.length) {
                return thirdParties;
            }
        }
    }

    getNowPartyMapper(courtApplicationParty) {
        return new NowPartyMapper(this.nowVariant, this.hearingJson, courtApplicationParty).buildNowParty();
    }

}

module.exports = ThirdPartyMapper;
