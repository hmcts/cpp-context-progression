const Mapper = require('../Mapper');
const Defendant = require('../../Model/Defendant');
const _ = require('lodash');

const COMPANY_TITLE = "Co";

class DefendantMapper extends Mapper {

    constructor(complianceEnforcement, hearingJson) {
        super(complianceEnforcement, hearingJson);
        this.complianceEnforcement = complianceEnforcement;
        this.hearingJson = hearingJson;
    }

    buildDefendant() {
        const defendant = new Defendant();
        const defendantFromHearingJson = this.getDefendant();
        defendant.address1 = this.address1(defendantFromHearingJson);
        defendant.address2 = this.address2(defendantFromHearingJson);
        defendant.address3 = this.address3(defendantFromHearingJson);
        defendant.address4 = this.address4(defendantFromHearingJson);
        defendant.address5 = this.address5(defendantFromHearingJson);
        // defendant.gender = this.gender(defendantFromHearingJson);
        defendant.benefitsTypes = undefined;
        defendant.companyName = this.companyName(defendantFromHearingJson);
        defendant.dateOfBirth = this.dateOfBirth(defendantFromHearingJson);
        defendant.dateOfSentence = this.complianceEnforcement.orderedDate;
        defendant.documentLanguage = this.hearingLanguage();
        defendant.emailAddress1 = this.emailAddress1(defendantFromHearingJson);
        defendant.emailAddress2 = this.emailAddress2(defendantFromHearingJson);
        defendant.forenames = this.foreNames(defendantFromHearingJson);
        defendant.hearingLanguage = this.hearingLanguage();
        defendant.nationalInsuranceNumber = this.nationalInsuranceNumber(defendantFromHearingJson);
        defendant.postcode = this.postcode(defendantFromHearingJson);
        defendant.statementOfMeansProvided = undefined;
        defendant.surname = this.surname(defendantFromHearingJson);
        defendant.telephoneNumberBusiness = this.telephoneNumberBusiness(defendantFromHearingJson);
        defendant.telephoneNumberHome = this.telephoneNumberHome(defendantFromHearingJson);
        defendant.telephoneNumberMobile = this.telephoneNumberMobile(defendantFromHearingJson);
        defendant.title = this.title(defendantFromHearingJson);
        defendant.vehicleMake = this.vehicleMake(defendantFromHearingJson);
        defendant.vehicleRegistrationMark = this.vehicleRegistration(defendantFromHearingJson);
        defendant.postHearingCustodyStatus = this.postHearingCustodyStatus(defendantFromHearingJson);
        // defendant.masterDefendantId = this.complianceEnforcement.masterDefendantId;
        return defendant;
    }

    address1(defendantFromHearingJson) {
        if (this.isAddressAvailable(defendantFromHearingJson)) {
            return defendantFromHearingJson.personDefendant.personDetails.address.address1;
        } else if (this.isLegalEntityDefendantAddressAvailable(defendantFromHearingJson)) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.address.address1;
        }
    }

    address2(defendantFromHearingJson) {
        if (this.isAddressAvailable(defendantFromHearingJson) &&
            defendantFromHearingJson.personDefendant.personDetails.address.address2) {
            return defendantFromHearingJson.personDefendant.personDetails.address.address2;
        } else if (this.isLegalEntityDefendantAddressAvailable(defendantFromHearingJson) &&
                   defendantFromHearingJson.legalEntityDefendant.organisation.address.address2) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.address.address2;
        }
    }

    address3(defendantFromHearingJson) {
        if (this.isAddressAvailable(defendantFromHearingJson) &&
            defendantFromHearingJson.personDefendant.personDetails.address.address3) {
            return defendantFromHearingJson.personDefendant.personDetails.address.address3;
        } else if (this.isLegalEntityDefendantAddressAvailable(defendantFromHearingJson) &&
                   defendantFromHearingJson.legalEntityDefendant.organisation.address.address3) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.address.address3;
        }
    }

    address4(defendantFromHearingJson) {
        if (this.isAddressAvailable(defendantFromHearingJson) &&
            defendantFromHearingJson.personDefendant.personDetails.address.address4) {
            return defendantFromHearingJson.personDefendant.personDetails.address.address4;
        } else if (this.isLegalEntityDefendantAddressAvailable(defendantFromHearingJson) &&
                   defendantFromHearingJson.legalEntityDefendant.organisation.address.address4) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.address.address4;
        }
    }

    address5(defendantFromHearingJson) {
        if (this.isAddressAvailable(defendantFromHearingJson) &&
            defendantFromHearingJson.personDefendant.personDetails.address.address5) {
            return defendantFromHearingJson.personDefendant.personDetails.address.address5;
        } else if (this.isLegalEntityDefendantAddressAvailable(defendantFromHearingJson) &&
                   defendantFromHearingJson.legalEntityDefendant.organisation.address.address5) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.address.address5;
        }
    }

    postcode(defendantFromHearingJson) {
        if (this.isAddressAvailable(defendantFromHearingJson) &&
            defendantFromHearingJson.personDefendant.personDetails.address.postcode) {
            return defendantFromHearingJson.personDefendant.personDetails.address.postcode;
        } else if (this.isLegalEntityDefendantAddressAvailable(defendantFromHearingJson) &&
                   defendantFromHearingJson.legalEntityDefendant.organisation.address.postcode) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.address.postcode;
        }
    }

    companyName(defendantFromHearingJson) {
        if (defendantFromHearingJson.legalEntityDefendant) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.name;
        }
        return undefined;
    }

    dateOfBirth(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant) {
            return defendantFromHearingJson.personDefendant.personDetails.dateOfBirth;
        }
    }

    postHearingCustodyStatus(defendantFromHearingJson) {
        // Yet to implement, not clear how to do it..
    }

    emailAddress1(defendantFromHearingJson) {
        if (this.isContactAvailable(defendantFromHearingJson) &&
            defendantFromHearingJson.personDefendant.personDetails.contact.primaryEmail) {
            return defendantFromHearingJson.personDefendant.personDetails.contact.primaryEmail;
        } else if (this.isLegalEntityDefendantContactAvailable(defendantFromHearingJson) &&
                   defendantFromHearingJson.legalEntityDefendant.organisation.contact.primaryEmail) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.contact.primaryEmail;
        }
    }

    emailAddress2(defendantFromHearingJson) {
        if (this.isContactAvailable(defendantFromHearingJson) &&
            defendantFromHearingJson.personDefendant.personDetails.contact.secondaryEmail) {
            return defendantFromHearingJson.personDefendant.personDetails.contact.secondaryEmail;
        } else if (this.isLegalEntityDefendantContactAvailable(defendantFromHearingJson) &&
                   defendantFromHearingJson.legalEntityDefendant.organisation.contact.secondaryEmail) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.contact.secondaryEmail;
        }
    }

    foreNames(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant) {
            const personDetails = defendantFromHearingJson.personDefendant.personDetails;
            return [personDetails.firstName, personDetails.middleName].filter(item => item).join(' ').trim();
        }
    }

    gender(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant) {
            return defendantFromHearingJson.personDefendant.personDetails.gender;
        }
    }

    hearingLanguage() {
        if(this.hearingJson.hearingLanguage) {
            return this.hearingJson.hearingLanguage;
        }

        return "ENGLISH"; //Default value to satisfy enforcement request
    }

    nationalInsuranceNumber(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant) {
            return defendantFromHearingJson.personDefendant.personDetails.nationalInsuranceNumber;
        }
    }

    surname(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant) {
            return defendantFromHearingJson.personDefendant.personDetails.lastName;
        }
    }

    telephoneNumberBusiness(defendantFromHearingJson) {
        if (this.isContactAvailable(defendantFromHearingJson) &&
            defendantFromHearingJson.personDefendant.personDetails.contact.work) {
            return defendantFromHearingJson.personDefendant.personDetails.contact.work;
        } else if (this.isLegalEntityDefendantContactAvailable(defendantFromHearingJson) &&
                   defendantFromHearingJson.legalEntityDefendant.organisation.contact.work) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.contact.work;
        }
    }

    telephoneNumberHome(defendantFromHearingJson) {
        if (this.isContactAvailable(defendantFromHearingJson) &&
            defendantFromHearingJson.personDefendant.personDetails.contact.home) {
            return defendantFromHearingJson.personDefendant.personDetails.contact.home;
        }
    }

    telephoneNumberMobile(defendantFromHearingJson) {
        if (this.isContactAvailable(defendantFromHearingJson) &&
            defendantFromHearingJson.personDefendant.personDetails.contact.mobile) {
            return defendantFromHearingJson.personDefendant.personDetails.contact.mobile;
        }
    }

    title(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant) {
            return defendantFromHearingJson.personDefendant.personDetails.title;
        } else if (defendantFromHearingJson.legalEntityDefendant) {
            return COMPANY_TITLE;
        }
    }

    vehicleMake(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant && defendantFromHearingJson.offences) {
            const offenceWithOffenceFacts = defendantFromHearingJson.offences.find(offence => offence.offenceFacts);
            return offenceWithOffenceFacts ? offenceWithOffenceFacts.offenceFacts.vehicleCode : undefined;
        }
        return undefined;
    }

    vehicleRegistration(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant && defendantFromHearingJson.offences) {
            const offenceWithOffenceFacts = defendantFromHearingJson.offences.find(offence => offence.offenceFacts);
            return offenceWithOffenceFacts ? offenceWithOffenceFacts.offenceFacts.vehicleRegistrationMark : undefined;
        }
        return undefined;
    }

    isAddressAvailable(defendantFromHearingJson) {
        return !!(defendantFromHearingJson.personDefendant &&
                  defendantFromHearingJson.personDefendant.personDetails.address);
    }

    isLegalEntityDefendantAddressAvailable(defendantFromHearingJson) {
        return !!(defendantFromHearingJson.legalEntityDefendant &&
                  defendantFromHearingJson.legalEntityDefendant.organisation.address);
    }

    isContactAvailable(defendantJson) {
        return !!(defendantJson.personDefendant &&
                  defendantJson.personDefendant.personDetails.contact);
    }

    isLegalEntityDefendantContactAvailable(defendantJson) {
        return !!(defendantJson.legalEntityDefendant &&
                  defendantJson.legalEntityDefendant.organisation.contact);
    }

}

module.exports = DefendantMapper;
