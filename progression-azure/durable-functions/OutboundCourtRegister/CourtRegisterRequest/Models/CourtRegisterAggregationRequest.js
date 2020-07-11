class CourtRegisterAggregationRequest {
    constructor() {
        this.registerDate = undefined;
        this.courtCentreId = undefined;
        this.fileName = undefined;
        this.recipients = [];
        this.defendants = [];
        this.hearingVenue = undefined;
    }
}


module.exports = CourtRegisterAggregationRequest;