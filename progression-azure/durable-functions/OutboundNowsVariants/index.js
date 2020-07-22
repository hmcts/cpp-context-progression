const ReferenceDataService = require('../NowsHelper/service/ReferenceDataService');
const NowDocumentRequestMapper = require('../OutboundNowsVariants/SetNowDocumentRequest/Mapper/NowDocumentRequest/NowDocumentRequestMapper');
const _ = require('lodash');

class OutboundNowsVariants {
    constructor(nowsVariantsSubscriptions, hearingJson, organisationUnitsRefData, enforcementAreaByLjaCode, enforcementAreaByPostCodeMap, context) {
        this.nowsVariantsSubscriptions = nowsVariantsSubscriptions;
        this.hearingJson = hearingJson;
        this.organisationUnitsRefData = organisationUnitsRefData;
        this.enforcementAreaByLjaCode = enforcementAreaByLjaCode;
        this.enforcementAreaByPostCodeMap = enforcementAreaByPostCodeMap;
        this.context = context;
    }

    buildNows() {
        const nowDocumentRequests = [];

        this.nowsVariantsSubscriptions.forEach(nowVariant => {
            const nowDocumentRequest = this.getNowDocumentRequestMapper(nowVariant);
            nowDocumentRequests.push(nowDocumentRequest);
        });

        return nowDocumentRequests;
    }

    getNowDocumentRequestMapper(nowVariant) {
        return new NowDocumentRequestMapper(nowVariant, this.hearingJson, this.organisationUnitsRefData, this.enforcementAreaByLjaCode, this.enforcementAreaByPostCodeMap, this.context).buildNowDocumentRequest();
    }
}

function getDefendant(hearingJson, nowVariant) {
    return _(hearingJson.prosecutionCases).flatMapDeep('defendants').value()
        .find(defendant => defendant.masterDefendantId
                           === nowVariant.masterDefendantId); //find the first defendant
}

function isAddressAvailable(defendantJson) {
    return !!(defendantJson.personDefendant &&
              defendantJson.personDefendant.personDetails.address);
}

function isLegalEntityDefendantAddressAvailable(defendantJson) {
    return !!(defendantJson.legalEntityDefendant &&
              defendantJson.legalEntityDefendant.organisation.address);

}

function postcode(defendantFromHearingJson) {
    if (isAddressAvailable(defendantFromHearingJson) &&
        defendantFromHearingJson.personDefendant.personDetails.address.postcode) {
        return defendantFromHearingJson.personDefendant.personDetails.address.postcode;
    } else if (isLegalEntityDefendantAddressAvailable(defendantFromHearingJson) &&
               defendantFromHearingJson.legalEntityDefendant.organisation.address.postcode) {
        return defendantFromHearingJson.legalEntityDefendant.organisation.address.postcode;
    }
}

async function processMultiplePostcodes(defendantPostCodes, context) {
    const enforcementAreaByPostCodeMap = new Map();
    await Promise.all(defendantPostCodes.filter(defendantPostCode => defendantPostCode !== undefined).map(async (defendantPostCode) => {
        try {
            const enforcementAreaByPostCode = await new ReferenceDataService().getEnforcementAreaByPostcode(defendantPostCode, context);
            enforcementAreaByPostCodeMap.set(defendantPostCode, enforcementAreaByPostCode);
        } catch (error) {
            console.log('error'+ error);
        }
    }));
    return enforcementAreaByPostCodeMap;
}

function anyFinancialNows(nowsVariantsSubscriptions) {
    const financialNows = nowsVariantsSubscriptions.find(nowVariant => nowVariant.now.financial)
    if(financialNows) {
        return true;
    }
}

module.exports = async (context) => {
    const defendantPostCodes = new Set();
    let enforcementAreaByLjaCode = undefined;
    let organisationUnitsRefData = undefined;
    let enforcementAreaByPostCodeMap = undefined;

    const hearingJson = context.bindings.params.hearingResultedObj;
    const courtCentre = hearingJson.courtCentre;
    const nowsVariantsSubscriptions = context.bindings.params.nowsVariantsSubscriptions;
    const hasFinancialNows = anyFinancialNows(nowsVariantsSubscriptions);

    if(hasFinancialNows) {
        organisationUnitsRefData = await new ReferenceDataService().getOrganisationUnit(courtCentre.id, context);

        if(courtCentre.lja) {
            enforcementAreaByLjaCode = await new ReferenceDataService().getEnforcementAreaByLja(courtCentre.lja.ljaCode, context);
        }

        enforcementAreaByPostCodeMap = await processMultiplePostcodes([...defendantPostCodes], context);
    }

    nowsVariantsSubscriptions.forEach(nowsVariantsSubscription => {
        const defendantPostCode = postcode(getDefendant(hearingJson, nowsVariantsSubscription));
        defendantPostCodes.add(defendantPostCode);
    });

    return await new OutboundNowsVariants(nowsVariantsSubscriptions, hearingJson, organisationUnitsRefData, enforcementAreaByLjaCode, enforcementAreaByPostCodeMap, context).buildNows();
};
