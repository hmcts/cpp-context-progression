const {Defendant} = require('../Model/HearingVenue');
const ProsecutionCasesOrApplicationsMapper = require('./ProsecutionCaseOrApplicationMapper');
const ResultMapper = require('./ResultMapper');
const _ = require('lodash');

class DefendantMapper {

    constructor(context, informantRegister, hearingJson) {
        this.context = context;
        this.informantRegister = informantRegister;
        this.hearingJson = hearingJson;
    }

    build() {
        const defendantsInfo = [];

        const registerDefendants = this.informantRegister.registerDefendants;

        registerDefendants.forEach((registerDefendant) => {

            const defendants = this.getDefendants(registerDefendant.masterDefendantId);

            if(defendants.length) {
                // Take the first defendant
                const defendant = defendants[0];

                const defendantInfo = new Defendant();
                defendantInfo.name = this.name(defendant);
                defendantInfo.dateOfBirth = this.dateOfBirth(defendant);
                defendantInfo.nationality = this.nationality(defendant);
                defendantInfo.address1 = this.address1(defendant);
                defendantInfo.address2 = this.address2(defendant);
                defendantInfo.address3 = this.address3(defendant);
                defendantInfo.address4 = this.address4(defendant);
                defendantInfo.address5 = this.address5(defendant);
                defendantInfo.postCode = this.postcode(defendant);
                defendantInfo.title = this.title(defendant);
                defendantInfo.firstName = this.firstName(defendant);
                defendantInfo.lastName = this.lastName(defendant);
                defendantInfo.results = this.getResultMapper(registerDefendant);
                defendantInfo.prosecutionCasesOrApplications = this.getProsecutionCasesOrApplicationsMapper(registerDefendant)
                defendantsInfo.push(defendantInfo);
            }
        });

        return defendantsInfo;
    }

    address1(defendant) {
        if (this.isAddressAvailable(defendant)) {
            return defendant.personDefendant.personDetails.address.address1;
        } else if (this.isLegalEntityDefendantAddressAvailable(defendant)) {
            return defendant.legalEntityDefendant.organisation.address.address1;
        }
    }

    address2(defendant) {
        if (this.isAddressAvailable(defendant) &&
            defendant.personDefendant.personDetails.address.address2) {
            return defendant.personDefendant.personDetails.address.address2;
        } else if (this.isLegalEntityDefendantAddressAvailable(defendant) &&
                   defendant.legalEntityDefendant.organisation.address.address2) {
            return defendant.legalEntityDefendant.organisation.address.address2;
        }
    }

    address3(defendant) {
        if (this.isAddressAvailable(defendant) &&
            defendant.personDefendant.personDetails.address.address3) {
            return defendant.personDefendant.personDetails.address.address3;
        } else if (this.isLegalEntityDefendantAddressAvailable(defendant) &&
                   defendant.legalEntityDefendant.organisation.address.address3) {
            return defendant.legalEntityDefendant.organisation.address.address3;
        }
    }

    address4(defendant) {
        if (this.isAddressAvailable(defendant) &&
            defendant.personDefendant.personDetails.address.address4) {
            return defendant.personDefendant.personDetails.address.address4;
        } else if (this.isLegalEntityDefendantAddressAvailable(defendant) &&
                   defendant.legalEntityDefendant.organisation.address.address4) {
            return defendant.legalEntityDefendant.organisation.address.address4;
        }
    }

    address5(defendant) {
        if (this.isAddressAvailable(defendant) &&
            defendant.personDefendant.personDetails.address.address5) {
            return defendant.personDefendant.personDetails.address.address5;
        } else if (this.isLegalEntityDefendantAddressAvailable(defendant) &&
                   defendant.legalEntityDefendant.organisation.address.address5) {
            return defendant.legalEntityDefendant.organisation.address.address5;
        }
    }

    postcode(defendant) {
        if (this.isAddressAvailable(defendant) &&
            defendant.personDefendant.personDetails.address.postcode) {
            return defendant.personDefendant.personDetails.address.postcode;
        } else if (this.isLegalEntityDefendantAddressAvailable(defendant) &&
                   defendant.legalEntityDefendant.organisation.address.postcode) {
            return defendant.legalEntityDefendant.organisation.address.postcode;
        }
    }

    dateOfBirth(defendant) {
        if (defendant.personDefendant) {
            return defendant.personDefendant.personDetails.dateOfBirth;
        }
    }

    title(defendant) {
        if (defendant.personDefendant
            && defendant.personDefendant.personDetails.title) {
            return defendant.personDefendant.personDetails.title;
        }
    }

    nationality(defendant) {
        if (defendant.personDefendant
            && defendant.personDefendant.personDetails.nationalityCode) {
            return defendant.personDefendant.personDetails.nationalityCode;
        }
    }

    isAddressAvailable(defendant) {
        return !!(defendant.personDefendant &&
                  defendant.personDefendant.personDetails.address);
    }

    isLegalEntityDefendantAddressAvailable(defendant) {
        return !!(defendant.legalEntityDefendant &&
                  defendant.legalEntityDefendant.organisation.address);
    }

    getProsecutionCasesOrApplicationsMapper(registerDefendant) {
        return new ProsecutionCasesOrApplicationsMapper(this.hearingJson, this.informantRegister, registerDefendant).build();
    }

    getResultMapper(registerDefendant) {
        return new ResultMapper(registerDefendant).buildDefendantLevelResults();
    }

    firstName(defendant) {
        if (defendant.personDefendant && defendant.personDefendant.personDetails.firstName) {
            return defendant.personDefendant.personDetails.firstName;
        }
    }

    lastName(defendant) {
        if (defendant.personDefendant) {
            return defendant.personDefendant.personDetails.lastName;
        } else if(defendant.legalEntityDefendant) {
            return defendant.legalEntityDefendant.organisation.name;
        }
    }

    name(defendant) {
        if (defendant.personDefendant) {
            return this.getFullNameOfPerson(defendant.personDefendant);
        } else if(defendant.legalEntityDefendant) {
            return defendant.legalEntityDefendant.organisation.name;
        }
    }

    getFullNameOfPerson(person) {
        if (person && person.personDetails) {
            const personDetails = person.personDetails;
            return [personDetails.firstName, personDetails.middleName,
                    personDetails.lastName].filter(item => item).join(' ').trim();
        }
    }

    getDefendants(masterDefendantId) {
        return _(this.hearingJson.prosecutionCases).flatMap('defendants').value().filter(defendant => defendant.masterDefendantId === masterDefendantId);
    }
}

module.exports = DefendantMapper;