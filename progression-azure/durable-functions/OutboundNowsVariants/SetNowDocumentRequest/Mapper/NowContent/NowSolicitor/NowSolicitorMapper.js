const Mapper = require('../../Mapper');
const { NowSolicitor, NowAddress } = require('../../../Model/NowContent');

class NowSolicitorMapper extends Mapper {
    constructor(nowVariant, hearingJson) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
    }

    buildNowSolicitor() {
        const nowSolicitor = new NowSolicitor();
        const defendantFromHearingJson = this.getDefendant();
        nowSolicitor.name = this.name(defendantFromHearingJson);

        const address = new NowAddress();
        const solicitorAddress = this.getSolicitorAddress(defendantFromHearingJson);
        const solicitorContact = this.getSolicitorContact(defendantFromHearingJson);

        if(solicitorAddress) {
            address.line1 = solicitorAddress.address1;
            address.line2 = solicitorAddress.address2 ? solicitorAddress.address2 : undefined;
            address.line3 = solicitorAddress.address3 ? solicitorAddress.address3 : undefined;
            address.line4 = solicitorAddress.address4 ? solicitorAddress.address4 : undefined;
            address.line5 = solicitorAddress.address5 ? solicitorAddress.address5 : undefined;
            address.postCode = solicitorAddress.postcode ? solicitorAddress.postcode : undefined;
        }

        if(!address.line1 && solicitorContact) {
            address.emailAddress1 = solicitorContact.primaryEmail ? solicitorContact.primaryEmail : undefined;
            address.emailAddress2 = solicitorContact.secondaryEmail ? solicitorContact.secondaryEmail : undefined;
        }

        nowSolicitor.address = address;

        return nowSolicitor;
    }

    name(defendantFromHearingJson) {
        if(defendantFromHearingJson.associatedDefenceOrganisation) {
            return defendantFromHearingJson.associatedDefenceOrganisation.defenceOrganisation.organisation.name;
        }
    }

    getSolicitorAddress(defendantJson) {
        if (defendantJson.associatedDefenceOrganisation &&
            defendantJson.associatedDefenceOrganisation.defenceOrganisation.organisation.address) {
            return defendantJson.associatedDefenceOrganisation.defenceOrganisation.organisation.address;
        }
    }

    getSolicitorContact(defendantJson) {
        if (defendantJson.associatedDefenceOrganisation &&
            defendantJson.associatedDefenceOrganisation.defenceOrganisation.organisation.contact) {
            return defendantJson.associatedDefenceOrganisation.defenceOrganisation.organisation.contact;
        }
    }
}

module.exports = NowSolicitorMapper;