const HearingVenue = require('../../Model/HearingVenue');
const Address = require('../../Model/Address');

class HearingVenueMapper {
    constructor(hearingJson) {
        this.hearingJson = hearingJson;
        this.addressInfo = this.hearingJson.courtCentre.address;
    }

    build() {
        const hearingVenue = new HearingVenue();
        hearingVenue.courtHouse = this.hearingJson.courtCentre.name;
        hearingVenue.ljaName = this.hearingJson.courtCentre.lja ? this.hearingJson.courtCentre.lja.ljaName : undefined;
        hearingVenue.address = this.address();
        return hearingVenue;
    }

    address() {
        if (!this.addressInfo) {
            return undefined;
        }
        const address = new Address();
        address.address1 = this.addressInfo.address1;
        address.address2 = this.addressInfo.address2;
        address.address3 = this.addressInfo.address3;
        address.address4 = this.addressInfo.address4;
        address.address5 = this.addressInfo.address5;
        address.postCode = this.addressInfo.postcode;
        return address;
    }
}

module.exports = HearingVenueMapper;
