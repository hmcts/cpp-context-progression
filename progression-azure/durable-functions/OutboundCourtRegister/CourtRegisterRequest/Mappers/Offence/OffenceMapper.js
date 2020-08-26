const Offence = require('../../Models/Offence');
const ResultMapper = require('../Result/ResultMapper');
const LEVEL_TYPE = require('../../../../NowsHelper/service/LevelTypeEnum');

class OffenceMapper {
    constructor(context, offences, registerDefendant) {
        this.context = context;
        this.offences = offences;
        this.registerDefendant = registerDefendant;
    }

    build() {
        return this.offences.map(offenceInfo => {
            const offence = new Offence();
            offence.offenceCode = offenceInfo.offenceCode;
            offence.offenceTitle = offenceInfo.offenceTitle;
            offence.wording = offenceInfo.wording + '####' + offenceInfo.offenceLegislation;
            offence.orderIndex = offenceInfo.orderIndex;
            offence.pleaValue =  (offenceInfo.plea || {}).pleaValue;
            offence.verdictCode = ((offenceInfo.verdict || {}).verdictType || {}).description;
            offence.indicatedPleaValue = offenceInfo.indicatedPlea ? offenceInfo.indicatedPlea.indicatedPleaValue : undefined;
            offence.allocationDecision = offenceInfo.allocationDecision ? offenceInfo.allocationDecision.motReasonDescription : undefined;
            offence.convictionDate = offenceInfo.convictionDate;
            offence.pleaDate = (offenceInfo.plea || {}).pleaDate;
            const offenceJudicialResults = this.registerDefendant.results.filter(r => r.level === LEVEL_TYPE.OFFENCE && r.offenceId === offenceInfo.id).map(r => r.judicialResult);
            offence.results = this.getResultMapper(offenceJudicialResults).build();
            return offence;
        });
    }

    getResultMapper(results) {
        return new ResultMapper(results);
    }
}

module.exports = OffenceMapper;