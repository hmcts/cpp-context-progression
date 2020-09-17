const HearingVenueMapper = require('./PrisonCourtRegisterRequest/Mapper/HearingVenue/HearingVenueMapper');
const RecipientMapper = require('./PrisonCourtRegisterRequest/Mapper/Recipient/RecipientMapper');
const DefendantMapper = require('./PrisonCourtRegisterRequest/Mapper/Defendant/DefendantMapper');
const CustodyLocationMapper = require('./PrisonCourtRegisterRequest/Mapper/CustodyLocation/CustodyLocationMapper');
const PrisonCourtRegisterRequest = require('./PrisonCourtRegisterRequest/Model/PrisonCourtRegisterRequest');
const ReferenceDataService = require('../NowsHelper/service/ReferenceDataService');

class PrisonCourtRegisterRequestBuilder {
    constructor(context, prisonCourtRegisters, hearingJson, prisonsCustodySuitesRefData) {
        this.context = context;
        this.prisonCourtRegisters = prisonCourtRegisters;
        this.hearingJson = hearingJson;
        this.prisonsCustodySuitesRefData = prisonsCustodySuitesRefData;
    }

    build() {

        const outboundPrisonCourtRegisterRequests = [];

        if (this.prisonCourtRegisters.length) {
            this.prisonCourtRegisters.forEach(prisonCourtRegister => {

                if (!prisonCourtRegister.matchedSubscriptions || !prisonCourtRegister.matchedSubscriptions.length) {
                    this.context.log(`No subscriptions matched for court centre - ${prisonCourtRegister.courtCentreId}`);
                    return null;
                }
                const prisonCourtRegisterRequest = new PrisonCourtRegisterRequest();
                prisonCourtRegisterRequest.courtCentreId = this.hearingJson.courtCentre.id;
                prisonCourtRegisterRequest.hearingDate = prisonCourtRegister.hearingDate;
                prisonCourtRegisterRequest.hearingId = prisonCourtRegister.hearingId;
                prisonCourtRegisterRequest.hearingVenue = this.getHearingVenueMapper();
                prisonCourtRegisterRequest.recipients = this.getRecipientMapper(prisonCourtRegister);
                prisonCourtRegisterRequest.defendant = this.getDefendantMapper(prisonCourtRegister);
                prisonCourtRegisterRequest.custodyLocation = this.getCustodyLocationMapper(prisonCourtRegister);
                outboundPrisonCourtRegisterRequests.push(prisonCourtRegisterRequest);
            });
        }
        this.prisonCourtRegisters = {};
        return outboundPrisonCourtRegisterRequests;
    }

    getDefendantMapper(prisonCourtRegister) {
        return new DefendantMapper(this.context, this.hearingJson, prisonCourtRegister).build();
    }

    getHearingVenueMapper() {
        return new HearingVenueMapper(this.hearingJson).build();
    }

    getRecipientMapper(prisonCourtRegister) {
        return new RecipientMapper(prisonCourtRegister, this.prisonsCustodySuitesRefData, this.hearingJson).build();
    }

    getCustodyLocationMapper(prisonCourtRegister) {
        return new CustodyLocationMapper(prisonCourtRegister, this.prisonsCustodySuitesRefData, this.hearingJson).build();
    }
}

module.exports = async function (context) {
    const hearingJson = context.bindings.params.hearingResultedObj;
    const prisonCourtRegisters = context.bindings.params.prisonCourtRegisterSubscriptions;
    const prisonsCustodySuitesRefData = await new ReferenceDataService().getPrisonsCustodySuites(context);
    return await new PrisonCourtRegisterRequestBuilder(context, prisonCourtRegisters, hearingJson, prisonsCustodySuitesRefData).build();
};
