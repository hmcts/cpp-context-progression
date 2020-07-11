const Address = require('../../Models/Address');

class AddressMapper {
    constructor(addressInfo) {
        this.addressInfo = addressInfo;
    }

    build() {
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


module.exports = AddressMapper;