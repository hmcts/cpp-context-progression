class Offence {
    constructor() {
        this.offenceCode;
        this.orderIndex = undefined;
        this.offenceTitle;
        this.pleaValue = undefined;
        this.verdictCode = undefined;
        this.indicatedPleaValue = undefined;
        this.allocationDecision;
        this.convictionDate=undefined;
        this.pleaDate=undefined;
        this.wording;
        this.results=[];
    }
}

module.exports = Offence;