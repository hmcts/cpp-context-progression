const HearingVenueMapper = require('./CourtRegisterRequest/Mappers/HearingVenue/HearingVenueMapper');
const RecipientMapper = require('./CourtRegisterRequest/Mappers/Recipient/RecipientMapper');
const YouthDefendantMapper = require('./CourtRegisterRequest/Mappers/YouthDefendant/YouthDefendantMapper');
const CourtRegisterAggregationRequest = require('./CourtRegisterRequest/Models/CourtRegisterAggregationRequest');

class OutboundCourtRegisterRequestBuilder {
    constructor(context, courtRegisterFragment, hearingResultedObj) {
        this.context = context;
        this.courtRegisterFragment = courtRegisterFragment;
        this.hearingResultedObj = hearingResultedObj;
    }

    build() {
        if (!this.courtRegisterFragment.matchedSubscriptions || !this.courtRegisterFragment.matchedSubscriptions.length) {
            this.context.log(`No subscriptions matched for court centre - ${this.courtRegisterFragment.courtCentreId}`);
            return null;
        }

        const youthDefendants = this.courtRegisterFragment.registerDefendants.filter(defendant => defendant.isYouthDefendant);
        if (youthDefendants.length === 0) {
            this.context.log(`No youth defendant found in the hearing - ${this.hearingResultedObj.id}`);
            return null;
        }

        const courtRegisterAggregation = new CourtRegisterAggregationRequest();
        courtRegisterAggregation.hearingDate = this.courtRegisterFragment.hearingDate;
        courtRegisterAggregation.registerDate = this.courtRegisterFragment.registerDate;
        courtRegisterAggregation.hearingId = this.courtRegisterFragment.hearingId;
        courtRegisterAggregation.courtCentreId = this.courtRegisterFragment.courtCenterId;
        courtRegisterAggregation.fileName = `court-register_${this.courtRegisterFragment.registerDate}_${this.hearingResultedObj.courtCentre.code}.pdf`;
        courtRegisterAggregation.hearingVenue = this.getHearingVenueMapper();
        courtRegisterAggregation.recipients = this.getRecipientMapper();
        courtRegisterAggregation.defendants = this.getYouthDefendantMapper(youthDefendants);
        return courtRegisterAggregation;
    }

    getYouthDefendantMapper(youthDefendants) {
        return new YouthDefendantMapper(this.context, youthDefendants, this.hearingResultedObj, this.courtRegisterFragment).build();
    }

    getHearingVenueMapper() {
        return new HearingVenueMapper(this.hearingResultedObj).build();
    }

    getRecipientMapper() {
        return new RecipientMapper(this.context, this.courtRegisterFragment).build();
    }

}

module.exports = async function (context) {
    const hearingResultedObj = context.bindings.params.hearingResultedObj;
    const courtRegisterFragment = context.bindings.params.courtRegisterSubscriptions;

    return new OutboundCourtRegisterRequestBuilder(context, courtRegisterFragment, hearingResultedObj).build();
};