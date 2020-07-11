const Mapper = require('../Mapper');
const {NowDocumentRequest} = require('../../Model/NowDocumentRequest');
const NowDistributionMapper = require('../NowDistribution/NowDistributionMapper');
const NowContentMapper = require('../NowContent/NowContentMapper');
const UserGroupType = require('../../../../SetNowVariants/UserGroupType');
const uuidv4 = require('uuid/v4');

class NowDocumentRequestMapper extends Mapper {
    constructor(nowVariant, hearingJson, organisationUnitsRefData, enforcementAreaByLjaCode, enforcementAreaByPostCodeMap, context) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
        this.organisationUnitsRefData = organisationUnitsRefData;
        this.enforcementAreaByLjaCode = enforcementAreaByLjaCode;
        this.enforcementAreaByPostCodeMap = enforcementAreaByPostCodeMap;
        this.context = context;
    }

    buildNowDocumentRequest() {
        const now = this.nowVariant.now;
        const nowDocumentRequest = new NowDocumentRequest();
        nowDocumentRequest.materialId = uuidv4();
        if(now.financial && this.nowVariant.complianceCorrelationId) {
            nowDocumentRequest.requestId = this.nowVariant.complianceCorrelationId;
        }
        nowDocumentRequest.hearingId = this.hearingJson.id;
        nowDocumentRequest.cases = this.getCases(this.nowVariant);
        nowDocumentRequest.applications = this.getApplications(this.nowVariant);
        nowDocumentRequest.masterDefendantId = this.nowVariant.masterDefendantId;
        nowDocumentRequest.nowTypeId = now.id;
        nowDocumentRequest.storageRequired = now.storageRequired;
        nowDocumentRequest.priority = undefined;
        nowDocumentRequest.templateName = now.templateName;
        nowDocumentRequest.subTemplateName = this.subTemplateName(now);
        nowDocumentRequest.bilingualTemplateName = now.bilingualTemplateName;
        nowDocumentRequest.subscriberName = this.nowVariant.matchedSubscription.name;
        nowDocumentRequest.visibleToUserGroups = this.visibleToUserGroups(this.nowVariant);
        nowDocumentRequest.notVisibleToUserGroups = this.notVisibleToUserGroups(this.nowVariant);
        nowDocumentRequest.nowDistribution = this.getNowDistributionMapper(this.nowVariant);
        nowDocumentRequest.nowContent = this.getNowContentMapper(nowDocumentRequest.requestId);
        return nowDocumentRequest;

    }

    visibleToUserGroups(nowVariant) {
        if (nowVariant.userGroup && nowVariant.userGroup.type === UserGroupType.INCLUDE) {
            return nowVariant.userGroup.userGroups;
        }
    }

    notVisibleToUserGroups(nowVariant) {
        if (nowVariant.userGroup && nowVariant.userGroup.type === UserGroupType.EXCLUDE) {
            return nowVariant.userGroup.userGroups;
        }
    }

    subTemplateName(now) {
        if (now.subTemplateName) {
            return now.subTemplateName;
        }
    }

    getCases(nowVariant) {
        if(nowVariant.registerDefendant.cases) {
            return Array.from(nowVariant.registerDefendant.cases);
        }
    }

    getApplications(nowVariant) {
        if(nowVariant.registerDefendant.applications) {
            return Array.from(nowVariant.registerDefendant.applications);
        }
    }

    getNowDistributionMapper(nowVariant) {
        return new NowDistributionMapper(nowVariant, this.hearingJson).buildNowDistribution();
    }

    getNowContentMapper(requestId) {
        return new NowContentMapper(requestId, this.nowVariant, this.hearingJson,
                                    this.organisationUnitsRefData, this.enforcementAreaByLjaCode, this.enforcementAreaByPostCodeMap, this.context)
            .buildNowContent();
    }
}

module.exports = NowDocumentRequestMapper;
