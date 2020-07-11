const _ = require('lodash');
const Mapper = require('../../Mapper');
const {NowDefendant, NowAddress} = require('../../../Model/NowContent');
const NowSolicitorMapper = require('../NowSolicitor/NowSolicitorMapper');

class NowDefendantMapper extends Mapper {

    constructor(nowVariant, hearingJson) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
    }

    buildDefendant() {
        const defendantFromHearingJson = this.getDefendant();
        const defendant = new NowDefendant();
        defendant.name = this.name(defendantFromHearingJson);
        defendant.dateOfBirth = this.dateOfBirth(defendantFromHearingJson);
        defendant.address = this.getDefendantAddress(defendantFromHearingJson);
        defendant.pncId = this.getPNCId(defendantFromHearingJson);
        defendant.nationality = this.getNationality(defendantFromHearingJson);
        defendant.prosecutingAuthorityReference = this.getArrestSummonNumbers();
        defendant.landLineNumber = this.getLandlineNumber(defendantFromHearingJson);
        defendant.mobileNumber = this.getMobileNumber(defendantFromHearingJson);
        defendant.nationalInsuranceNumber = this.getNationalInsuranceNumber(defendantFromHearingJson);
        defendant.ethnicity = this.getEthnicity(defendantFromHearingJson);
        defendant.gender = this.getGender(defendantFromHearingJson);
        defendant.driverNumber = this.getDriverNumber(defendantFromHearingJson);
        defendant.solicitor = this.getSolicitor(defendantFromHearingJson);
        defendant.defendantResults = this.getDefendantResults(defendantFromHearingJson);
        defendant.occupation = this.occupation(defendantFromHearingJson);
        defendant.occupationWelsh = undefined;
        defendant.title = this.title(defendantFromHearingJson);
        defendant.firstName = this.firstName(defendantFromHearingJson);
        defendant.middleName = this.middleName(defendantFromHearingJson);
        defendant.lastName  = this.lastName(defendantFromHearingJson);
        defendant.aliasNames = this.aliasNames(defendantFromHearingJson);
        defendant.isCivil = false;
        defendant.selfDefinedEthnicity = this.getSelfDefinedEthnicity(defendantFromHearingJson);
        defendant.interpreterLanguageNeeds = this.interpreterLanguageNeeds(defendantFromHearingJson);
        defendant.specialNeeds = this.specialNeeds(defendantFromHearingJson);
        defendant.isYouth = this.getIsYouthDefendant();
        return defendant;
    }

    getNationality(defendantFromHearingJson) {
        if (this.nowVariant.now.includeNationality) {
            return this.nationality(defendantFromHearingJson);
        }
    }

    getPNCId(defendantFromHearingJson) {
        if (this.nowVariant.now.includePNCID) {
            return this.pncId(defendantFromHearingJson);
        }
    }

    getDefendantAddress(defendantFromHearingJson) {
        if (this.isAddressAvailable(defendantFromHearingJson) ||
            this.isLegalEntityDefendantAddressAvailable(defendantFromHearingJson)) {
            const address = new NowAddress();
            address.line1 = this.address1(defendantFromHearingJson);
            address.line2 = this.address2(defendantFromHearingJson);
            address.line3 = this.address3(defendantFromHearingJson);
            address.line4 = this.address4(defendantFromHearingJson);
            address.line5 = this.address5(defendantFromHearingJson);
            address.postCode = this.postcode(defendantFromHearingJson);

            if (!address.line1) {
                address.emailAddress1 = this.primaryEmailAddress(defendantFromHearingJson);
                address.emailAddress2 = this.secondaryEmailAddress(defendantFromHearingJson);
            }
            return address;
        }
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

    dateOfBirth(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant) {
            return defendantFromHearingJson.personDefendant.personDetails.dateOfBirth;
        }
    }

    primaryEmailAddress(defendantFromHearingJson) {
        if (this.isContactAvailable(defendantFromHearingJson) &&
            defendantFromHearingJson.personDefendant.personDetails.contact.primaryEmail) {
            return defendantFromHearingJson.personDefendant.personDetails.contact.primaryEmail;
        } else if (this.isLegalEntityDefendantContactAvailable(defendantFromHearingJson) &&
                   defendantFromHearingJson.legalEntityDefendant.organisation.contact.primaryEmail) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.contact.primaryEmail;
        }
    }

    secondaryEmailAddress(defendantFromHearingJson) {
        if (this.isContactAvailable(defendantFromHearingJson) &&
            defendantFromHearingJson.personDefendant.personDetails.contact.secondaryEmail) {
            return defendantFromHearingJson.personDefendant.personDetails.contact.secondaryEmail;
        } else if (this.isLegalEntityDefendantContactAvailable(defendantFromHearingJson) &&
                   defendantFromHearingJson.legalEntityDefendant.organisation.contact.secondaryEmail) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.contact.secondaryEmail;
        }
    }

    name(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant) {
            return this.getFullNameOfPerson(defendantFromHearingJson.personDefendant);
        } else if(defendantFromHearingJson.legalEntityDefendant) {
            return defendantFromHearingJson.legalEntityDefendant.organisation.name;
        }
    }

    firstName(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant) {
            return defendantFromHearingJson.personDefendant.personDetails.firstName;
        }
    }

    middleName(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant) {
            return defendantFromHearingJson.personDefendant.personDetails.middleName;
        }
    }

    lastName(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant) {
            return defendantFromHearingJson.personDefendant.personDetails.lastName;
        }
    }

    gender(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant) {
            return defendantFromHearingJson.personDefendant.personDetails.gender;
        }
    }

    nationalInsuranceNumber(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant) {
            return defendantFromHearingJson.personDefendant.personDetails.nationalInsuranceNumber;
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

    pncId(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant
            && defendantFromHearingJson.personDefendant.pncId) {
            return defendantFromHearingJson.personDefendant.pncId;
        }
    }

    driverNumber(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant
            && defendantFromHearingJson.personDefendant.driverNumber) {
            return defendantFromHearingJson.personDefendant.driverNumber;
        }
    }

    ethnicity(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant
            && defendantFromHearingJson.personDefendant.personDetails.ethnicity) {
            return defendantFromHearingJson.personDefendant.personDetails.ethnicity.observedEthnicityDescription;
        }
    }

    nationality(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant
            && defendantFromHearingJson.personDefendant.personDetails.nationalityDescription) {
            return defendantFromHearingJson.personDefendant.personDetails.nationalityDescription;
        }
    }

    isAddressAvailable(defendantJson) {
        return !!(defendantJson.personDefendant &&
                  defendantJson.personDefendant.personDetails.address);
    }

    isLegalEntityDefendantAddressAvailable(defendantJson) {
        return !!(defendantJson.legalEntityDefendant &&
                  defendantJson.legalEntityDefendant.organisation.address);
    }

    isContactAvailable(defendantJson) {
        return !!(defendantJson.personDefendant &&
                  defendantJson.personDefendant.personDetails.contact);
    }

    isLegalEntityDefendantContactAvailable(defendantJson) {
        return !!(defendantJson.legalEntityDefendant &&
                  defendantJson.legalEntityDefendant.organisation.contact);
    }

    getSolicitorMapper() {
        return new NowSolicitorMapper(this.nowVariant, this.hearingJson).buildNowSolicitor();
    }

    occupation(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant
            && defendantFromHearingJson.personDefendant.personDetails.occupation) {
            return defendantFromHearingJson.personDefendant.personDetails.occupation;
        }
    }

    title(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant
            && defendantFromHearingJson.personDefendant.personDetails.title) {
            return defendantFromHearingJson.personDefendant.personDetails.title;
        }
    }

    aliasNames(defendantFromHearingJson) {
        if (defendantFromHearingJson.aliases && defendantFromHearingJson.aliases.length) {
            const aliasNames = [];
            defendantFromHearingJson.aliases.forEach(alias => {
                aliasNames.push([alias.title, alias.firstName, alias.middleName,
                                 alias.lastName].filter(item => item).join(' ').trim());
            });
            return aliasNames;
        }
    }

    getLandlineNumber(defendantFromHearingJson) {
        if (this.nowVariant.now.includeDefendantLandlineNumber) {
            return this.telephoneNumberHome(defendantFromHearingJson);
        }
    }

    getMobileNumber(defendantFromHearingJson) {
        if (this.nowVariant.now.includeDefendantMobileNumber) {
            return this.telephoneNumberMobile(defendantFromHearingJson);
        }
    }

    getNationalInsuranceNumber(defendantFromHearingJson) {
        if (this.nowVariant.now.includeDefendantNINO) {
            return this.nationalInsuranceNumber(defendantFromHearingJson);
        }
    }

    getEthnicity(defendantFromHearingJson) {
        if (this.nowVariant.now.includeDefendantEthnicity) {
            return this.ethnicity(defendantFromHearingJson);
        }
    }

    getSelfDefinedEthnicity(defendantFromHearingJson) {
        if (this.nowVariant.now.includeDefendantEthnicity) {
            return this.selfDefinedEthnicity(defendantFromHearingJson);
        }
    }

    getGender(defendantFromHearingJson) {
        if (this.nowVariant.now.includeDefendantGender) {
            return this.gender(defendantFromHearingJson);
        }
    }

    getDriverNumber(defendantFromHearingJson) {
        if (this.nowVariant.now.includeDriverNumber) {
            return this.driverNumber(defendantFromHearingJson);
        }
    }

    getSolicitor(defendantFromHearingJson) {
        if (this.nowVariant.now.includeSolicitorsNameAddress && defendantFromHearingJson.defenceOrganisation) {
            return this.getSolicitorMapper();
        }
    }

    getDefendantResults(defendantFromHearingJson) {
        if (this.nowVariant.masterDefendantId === defendantFromHearingJson.masterDefendantId) {
            return this.getFilterDefendantResults();
        }
    }

    getArrestSummonNumbers() {
        const arrestSummonNumbers = new Set();
        _(this.hearingJson.prosecutionCases).flatMapDeep('defendants').value()
            .filter(defendant => defendant.masterDefendantId === this.nowVariant.masterDefendantId)
            .filter(defendant => defendant.personDefendant && defendant.personDefendant.arrestSummonsNumber)
            .forEach(defendant => {
                arrestSummonNumbers.add(defendant.personDefendant.arrestSummonsNumber);
            });

        if(arrestSummonNumbers.size) {
            return [...arrestSummonNumbers].join(',');
        }
    }

    getIsYouthDefendant() {
        if(this.nowVariant.registerDefendant.isYouthDefendant) {
            return 'Y';
        } else {
            return 'N';
        }
    }

    selfDefinedEthnicity(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant
            && defendantFromHearingJson.personDefendant.personDetails.ethnicity
            && defendantFromHearingJson.personDefendant.personDetails.ethnicity.selfDefinedEthnicityDescription) {
            return defendantFromHearingJson.personDefendant.personDetails.ethnicity.selfDefinedEthnicityDescription;
        }
    }

    interpreterLanguageNeeds(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant
            && defendantFromHearingJson.personDefendant.personDetails.interpreterLanguageNeeds) {
            return defendantFromHearingJson.personDefendant.personDetails.interpreterLanguageNeeds;
        }
    }

    specialNeeds(defendantFromHearingJson) {
        if (defendantFromHearingJson.personDefendant
            && defendantFromHearingJson.personDefendant.personDetails.specificRequirements) {
            return defendantFromHearingJson.personDefendant.personDetails.specificRequirements;
        }
    }
}

module.exports = NowDefendantMapper;