const Mapper = require('../Mapper');
const {NowParty, NowAddress} = require('../../Model/NowContent');

class NowPartyMapper extends Mapper {
    constructor(nowVariant, hearingJson, courtApplicationParty) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
        this.courtApplicationParty = courtApplicationParty;
    }

    buildNowParty() {
        const applicationParty = this.getApplicationParty();
        const nowParty = this.getNowParty(applicationParty);

        return nowParty;
    }

    getNowParty(applicationParty) {
        const nowParty = new NowParty();
        const address = new NowAddress();

        if (applicationParty.address) {
            address.line1 = this.address1(applicationParty);
            address.line2 = this.address2(applicationParty);
            address.line3 = this.address3(applicationParty);
            address.line4 = this.address4(applicationParty);
            address.line5 = this.address5(applicationParty);
            address.postCode = this.postcode(applicationParty);
        }

        if (!address.line1 && applicationParty.contact) {
            address.emailAddress1 = this.primaryEmailAddress(applicationParty);
            address.emailAddress2 = this.secondaryEmailAddress(applicationParty);
        }

        nowParty.name = this.name(applicationParty);
        nowParty.title = this.title(applicationParty);
        nowParty.firstName = this.firstName(applicationParty);
        nowParty.middleName = this.middleName(applicationParty);
        nowParty.lastName  = this.lastName(applicationParty);
        nowParty.dateOfBirth = this.dateOfBirth(applicationParty);
        nowParty.address = address;

        return nowParty;
    }

    getApplicationParty() {
        if(this.courtApplicationParty.personDetails) {
            return this.courtApplicationParty.personDetails;
        }
        if(this.courtApplicationParty.organisation) {
            return this.courtApplicationParty.organisation;
        }
        if(this.courtApplicationParty.organisationPersons && this.courtApplicationParty.organisationPersons.length) {
            return this.courtApplicationParty.organisationPersons[0].person;
        }
        if(this.courtApplicationParty.prosecutingAuthority) {
            return this.courtApplicationParty.prosecutingAuthority;
        }
        if(this.courtApplicationParty.defendant) {
            return this.courtApplicationParty.defendant.personDefendant ? this.courtApplicationParty.defendant.personDefendant.personDetails
             : this.courtApplicationParty.defendant.legalEntityDefendant.organisation;
        }
        if(this.courtApplicationParty.representationOrganisation) {
            return this.courtApplicationParty.representationOrganisation;
        }
    }

    isDistinctNowParty(nowParties, currentNowParty) {
        const matchingNowParty = nowParties.find(party =>
                                                    party.title === currentNowParty.title &&
                                                    party.firstName === currentNowParty.firstName &&
                                                    party.middleName === currentNowParty.middleName &&
                                                    party.lastName === currentNowParty.lastName &&
                                                    party.dateOfBirth === currentNowParty.dateOfBirth &&
                                                    party.address.line1 === currentNowParty.address.line1 &&
                                                    party. address.postCode === currentNowParty. address.postCode);
        if(!matchingNowParty) {
            return true;
        }
    }

    name(partyJson) {
        if (partyJson.name) {
            return partyJson.name;
        } else if (partyJson.firstName) {
           return [partyJson.firstName, partyJson.middleName,
                   partyJson.lastName].filter(item => item).join(' ').trim();
        }
    }

    dateOfBirth(partyJson) {
        if (partyJson.dateOfBirth) {
            return partyJson.dateOfBirth;
        }
    }

    firstName(partyJson) {
        if (partyJson.firstName) {
            return partyJson.firstName;
        }
    }

    middleName(partyJson) {
        if (partyJson.middleName) {
            return partyJson.middleName;
        }
    }

    lastName(partyJson) {
        if (partyJson.lastName) {
            return partyJson.lastName;
        }
    }

    title(partyJson) {
        if (partyJson.title) {
            return partyJson.title;
        }
    }

    address1(partyJson) {
        if (partyJson.address) {
            return partyJson.address.address1;
        }
    }

    address2(partyJson) {
        if (partyJson.address &&
            partyJson.address.address2) {
            return partyJson.address.address2;
        }
    }

    address3(partyJson) {
        if (partyJson.address &&
            partyJson.address.address3) {
            return partyJson.address.address3;
        }
    }

    address4(partyJson) {
        if (partyJson.address &&
            partyJson.address.address4) {
            return partyJson.address.address4;
        }
    }

    address5(partyJson) {
        if (partyJson.address &&
            partyJson.address.address5) {
            return partyJson.address.address5;
        }
    }

    postcode(partyJson) {
        if (partyJson.address &&
            partyJson.address.postcode) {
            return partyJson.address.postcode;
        }
    }

    primaryEmailAddress(partyJson) {
        if (partyJson.contact &&
            partyJson.contact.primaryEmail) {
            return partyJson.contact.primaryEmail;
        }
    }

    secondaryEmailAddress(partyJson) {
        if (partyJson.contact &&
            partyJson.contact.secondaryEmail) {
            return partyJson.contact.secondaryEmail;
        }
    }
}

module.exports = NowPartyMapper;
