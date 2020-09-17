const InformantRegisterAggregation = require('./InformantRegisterAggregationRequest/Model/InformantRegisterAggregation');
const HearingVenueMapper = require('./InformantRegisterAggregationRequest/Mapper/HearingVenueMapper');
const RecipientMapper = require('./InformantRegisterAggregationRequest/Mapper/RecipientMapper');
const dateService = require('../NowsHelper/service/DateService');

class OutboundInformantRegister {
    constructor(context, hearingJson, informantRegisterSubscriptions) {
        this.context = context;
        this.informantRegisterSubscriptions = informantRegisterSubscriptions;
        this.hearingJson = hearingJson;
    }

    build() {
        const informantRegisterRequestArray = [];
        this.informantRegisterSubscriptions.forEach(informantRegister => {
            if(informantRegister.matchedSubscriptions && informantRegister.matchedSubscriptions.length) {
                const informantRegisterRequest = new InformantRegisterAggregation();
                informantRegisterRequest.registerDate = informantRegister.registerDate;
                informantRegisterRequest.hearingDate = informantRegister.hearingDate;
                informantRegisterRequest.hearingId = informantRegister.hearingId;
                informantRegisterRequest.prosecutionAuthorityId = informantRegister.prosecutionAuthorityId;
                informantRegisterRequest.prosecutionAuthorityCode = informantRegister.prosecutionAuthorityCode;
                informantRegisterRequest.majorCreditorCode = informantRegister.majorCreditorCode;
                informantRegisterRequest.prosecutionAuthorityName = informantRegister.prosecutionAuthorityName;
                informantRegisterRequest.fileName = this.populateFileName(informantRegister);
                informantRegisterRequest.recipients = this.getRecipients(informantRegister);
                informantRegisterRequest.hearingVenue = this.getHearingVenue(informantRegister);
                informantRegisterRequestArray.push(informantRegisterRequest);
            }
        });
        this.informantRegisterSubscriptions = {};
        return informantRegisterRequestArray;
    }

    getHearingVenue(informantRegister) {
        return new HearingVenueMapper(this.context, informantRegister, this.hearingJson).build();
    }

    getRecipients(informantRegister) {
        return new RecipientMapper(this.context, informantRegister.matchedSubscriptions).build();
    }

    populateFileName(obj) {
        return 'InformantRegister_' + obj.prosecutionAuthorityCode + '_' + dateService.getLocalDate(obj.registerDate) + '.csv';
    }
}

module.exports = async function (context) {
    const hearingJson = context.bindings.params.hearingResultedObj;
    const informantRegisterSubscriptions = context.bindings.params.informantRegisterSubscriptions;
    return await new OutboundInformantRegister(context, hearingJson, informantRegisterSubscriptions).build();
};
