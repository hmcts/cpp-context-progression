const NowPartyMapper = require('../NowPartyMapper');

class RespondentMapper extends NowPartyMapper {
    constructor(nowVariant, hearingJson) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
    }

    buildRespondents() {
        if(this.getRespondentDetails()) {
            const respondents = []
            const respondentsFromHearing = this.getRespondentDetails();

            respondentsFromHearing.forEach(appParty => {
                const respondent = this.getNowPartyMapper(appParty.partyDetails);
                if(respondents.length && this.isDistinctNowParty(respondents, respondent)) {
                    respondents.push(respondent);
                }
                if(!respondents.length) {
                    respondents.push(respondent);
                }
            });

            if(respondents.length) {
                return respondents;
            }
        }
    }

    getNowPartyMapper(courtApplicationParty) {
        return new NowPartyMapper(this.nowVariant, this.hearingJson, courtApplicationParty).buildNowParty();
    }
}

module.exports = RespondentMapper;
