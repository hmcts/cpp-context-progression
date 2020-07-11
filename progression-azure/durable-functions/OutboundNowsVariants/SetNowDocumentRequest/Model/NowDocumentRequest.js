const {NowContent} = require('./NowContent');

class NowDocumentRequest {
    constructor() {
        this.hearingId;
        this.masterDefendantId = undefined;
        this.welshCourtCentre;
        this.nowTypeId;
        this.materialId;
        this.requestId = undefined;
        this.cases = undefined;
        this.applications = undefined;
        this.visibleToUserGroups = undefined;
        this.notVisibleToUserGroups = undefined;
        this.nowDistribution = undefined;        
        this.storageRequired;
        this.nowContent = new NowContent();
        this.templateName;
        this.subTemplateName;
        this.bilingualTemplateName = undefined;
        this.subscriberName;
    }
}

module.exports = { NowDocumentRequest };