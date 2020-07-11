const NowPartyMapper = require('../NowPartyMapper');

class ApplicantMapper extends NowPartyMapper {
    constructor(nowVariant, hearingJson) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
    }

    buildApplicants() {
        if(this.getApplicantDetails()) {
            const applicants = []
            const applicantsFromHearing = this.getApplicantDetails();
            applicantsFromHearing.forEach(appParty => {
                const applicant = this.getNowPartyMapper(appParty);
                if(applicants.length && this.isDistinctNowParty(applicants, applicant)) {
                    applicants.push(applicant);
                }
                if(!applicants.length) {
                    applicants.push(applicant);
                }
            });
            if(applicants.length) {
                return applicants;
            }
        }
    }

    getNowPartyMapper(courtApplicationParty) {
        return new NowPartyMapper(this.nowVariant, this.hearingJson, courtApplicationParty).buildNowParty();
    }
}

module.exports = ApplicantMapper;
