const Mapper = require('../../Mapper');
const {OrderCourt, NowAddress} = require('../../../Model/NowContent');

class OrderCourtMapper extends Mapper {
    constructor(nowVariant, hearingJson) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
    }

    buildOrderCourt() {
        const courtCentre = this.hearingJson.courtCentre;
        const orderCourt = new OrderCourt();
        orderCourt.ljaCode = courtCentre.lja ? courtCentre.lja.ljaCode.toString() : undefined;
        orderCourt.ljaName = courtCentre.lja ? courtCentre.lja.ljaName : undefined;
        orderCourt.welshLjaName = this.welshLjaName(courtCentre);
        orderCourt.courtCentreName = courtCentre.name;
        orderCourt.welshCourtCentreName = courtCentre.welshName ? courtCentre.welshName : undefined;
        orderCourt.welshCourtCentre = this.welshCourtCentre();

        if(this.isAddressAvailable(courtCentre)) {
            const address = new NowAddress();
            address.line1 = this.address1(courtCentre);
            address.line2 = this.address2(courtCentre);
            address.line3 = this.address3(courtCentre);
            address.line4 = this.address4(courtCentre);
            address.line5 = this.address5(courtCentre);
            address.postCode = this.postcode(courtCentre);
            orderCourt.address = address;
        }

        if(this.isWelshAddressAvailable(courtCentre)) {
            const welshAddress = new NowAddress();
            welshAddress.line1 = this.welshAddress1(courtCentre);
            welshAddress.line2 = this.welshAddress2(courtCentre);
            welshAddress.line3 = this.welshAddress3(courtCentre);
            welshAddress.line4 = this.welshAddress4(courtCentre);
            welshAddress.line5 = this.welshAddress5(courtCentre);
            welshAddress.postCode = this.welshPostcode(courtCentre);
            orderCourt.welshAddress = welshAddress;
        }

        return orderCourt;
    }

    welshCourtCentre() {
        return !!this.hearingJson.courtCentre.welshCourtCentre;
    }

    welshLjaName(courtCentre) {
        if (courtCentre.lja && courtCentre.lja.welshLjaName) {
            return courtCentre.lja.welshLjaName;
        }
    }

    address1(courtCentre) {
        if (this.isAddressAvailable(courtCentre)) {
            return courtCentre.address.address1;
        }
    }

    address2(courtCentre) {
        if (this.isAddressAvailable(courtCentre) &&
            courtCentre.address.address2) {
            return courtCentre.address.address2;
        }
    }

    address3(courtCentre) {
        if (this.isAddressAvailable(courtCentre) &&
            courtCentre.address.address3) {
            return courtCentre.address.address3;
        }
    }

    address4(courtCentre) {
        if (this.isAddressAvailable(courtCentre) &&
            courtCentre.address.address4) {
            return courtCentre.address.address4;
        }
    }

    address5(courtCentre) {
        if (this.isAddressAvailable(courtCentre) &&
            courtCentre.address.address5) {
            return courtCentre.address.address5;
        }
    }

    postcode(courtCentre) {
        if (this.isAddressAvailable(courtCentre) &&
            courtCentre.address.postcode) {
            return courtCentre.address.postcode;
        }
    }

    welshAddress1(courtCentre) {
        if (this.isWelshAddressAvailable(courtCentre)) {
            return courtCentre.welshAddress.address1;
        }
    }

    welshAddress2(courtCentre) {
        if (this.isWelshAddressAvailable(courtCentre) &&
            courtCentre.welshAddress.address2) {
            return courtCentre.welshAddress.address2;
        }
    }

    welshAddress3(courtCentre) {
        if (this.isWelshAddressAvailable(courtCentre) &&
            courtCentre.welshAddress.address3) {
            return courtCentre.welshAddress.address3;
        }
    }

    welshAddress4(courtCentre) {
        if (this.isWelshAddressAvailable(courtCentre) &&
            courtCentre.welshAddress.address4) {
            return courtCentre.welshAddress.address4;
        }
    }

    welshAddress5(courtCentre) {
        if (this.isWelshAddressAvailable(courtCentre) &&
            courtCentre.welshAddress.welshAddress5) {
            return courtCentre.welshAddress.address5;
        }
    }

    welshPostcode(courtCentre) {
        if (this.isWelshAddressAvailable(courtCentre) &&
            courtCentre.welshAddress.postcode) {
            return courtCentre.welshAddress.postcode;
        }
    }

    isAddressAvailable(courtCentre) {
        return !!courtCentre.address;
    }

    isWelshAddressAvailable(courtCentre) {
        return !!courtCentre.welshAddress;
    }
}

module.exports = OrderCourtMapper;