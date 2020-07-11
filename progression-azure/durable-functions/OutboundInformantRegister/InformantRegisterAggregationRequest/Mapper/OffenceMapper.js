const {Offence} = require('../Model/HearingVenue');
const ResultMapper = require('./ResultMapper');

class OffenceMapper {

    constructor(hearing, informantRegister, defendantContext) {
        this.hearing = hearing;
        this.informantRegister = informantRegister;
        this.defendantContext = defendantContext;
    }

    build() {
        return (this.hearing.prosecutionCases || [])
            .filter((c) => c.prosecutionCaseIdentifier.prosecutionAuthorityId === this.informantRegister.prosecutionAuthorityId)
            .map(c => c.defendants)
            .reduce((a, b) => a.concat(b))
            .filter(d => d.masterDefendantId === this.defendantContext.masterDefendantId)
            .map(d => d.offences)
            .reduce((a, b) => a.concat(b))
            .map(o => {
                const offence = new Offence();
                offence.offenceCode = o.offenceCode;
                offence.orderIndex = o.orderIndex;
                offence.offenceTitle = o.offenceTitle;
                offence.pleaValue = (o.plea || {}).pleaValue;
                offence.verdictCode = ((o.verdict || {}).verdictType || {}).description;
                offence.offenceResults = new ResultMapper(this.defendantContext).buildOffenceLevelResults(o);
                return offence;
            });
    }
}

module.exports = OffenceMapper;