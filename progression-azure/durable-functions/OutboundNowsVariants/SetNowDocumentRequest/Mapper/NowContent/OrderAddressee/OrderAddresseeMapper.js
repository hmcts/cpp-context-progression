const Mapper = require('../../Mapper');
const {OrderAddressee, NowAddress} = require('../../../Model/NowContent');

class OrderAddresseeMapper extends Mapper {
    constructor(nowVariant, hearingJson, context) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
        this.context = context;
    }

    buildOrderAddressee() {
        if(this.nowVariant.matchedSubscription.recipient) {
            let orderAddressee = new OrderAddressee();
            if (this.nowVariant.matchedSubscription.recipient.recipientFromSubscription) {
                orderAddressee = this.recipientFromSubscription();
            } else if (this.nowVariant.matchedSubscription.recipient.recipientFromResults) {
                orderAddressee = this.recipientFromResults();
            } else if (this.nowVariant.matchedSubscription.recipient.recipientFromCase) {
                orderAddressee = this.recipientFromCase();
            }

            return this.validateOrderAddressee(orderAddressee);
        }
    }

    validateOrderAddressee(orderAddressee) {
        if (orderAddressee.name !== undefined || (orderAddressee.address !== undefined
                                                  && (orderAddressee.address.line1 !== undefined
                                                      || orderAddressee.address.emailAddress1 !== undefined))) {
            return orderAddressee;
        }
    }

    recipientFromSubscription() {
        const subscription = this.nowVariant.matchedSubscription;
        const orderAddressee = new OrderAddressee();
        const address = new NowAddress();
        orderAddressee.name = subscription.recipient.organisationName;

        address.line1 = subscription.recipient.address1;
        address.line2 = subscription.recipient.address2;
        address.line3 = subscription.recipient.address3;
        address.line4 = subscription.recipient.address4;
        address.line5 = subscription.recipient.address5;
        address.postCode = subscription.recipient.postCode;
        address.emailAddress1 = subscription.recipient.emailAddress1;
        address.emailAddress2 = subscription.recipient.emailAddress2;

        if(address.line1 || address.emailAddress1) {
            orderAddressee.address = address;
        }
        return orderAddressee;
    }

    recipientFromResults() {
        const subscription = this.nowVariant.matchedSubscription;
        const nameReference =
            subscription.recipient.organisationNameResultPromptReference ?
            subscription.recipient.organisationNameResultPromptReference :
            subscription.recipient.lastNameResultPromptReference;

        const address1Reference = subscription.recipient.address1ResultPromptReference;
        const address2Reference = subscription.recipient.address2ResultPromptReference;
        const address3Reference = subscription.recipient.address3ResultPromptReference;
        const address4Reference = subscription.recipient.address4ResultPromptReference;
        const address5Reference = subscription.recipient.address5ResultPromptReference;
        const postCodeReference = subscription.recipient.postCodeResultPromptReference;
        const emailAddress1Reference = subscription.recipient.emailAddress1ResultPromptReference;
        const emailAddress2Reference = subscription.recipient.emailAddress2ResultPromptReference;

        const judicialResults = this.nowVariant.results;

        const allPromptsFromJudicialResults = [];
        judicialResults.forEach(judicialResult => {
            if(judicialResult.judicialResultPrompts && judicialResult.judicialResultPrompts.length) {
                judicialResult.judicialResultPrompts.forEach(prompt => {
                    allPromptsFromJudicialResults.push(prompt);
                });
            }
        });

        const orderAddressee = new OrderAddressee();
        orderAddressee.name = this.getPromptValueByReference(allPromptsFromJudicialResults, nameReference);
        if(address1Reference || emailAddress1Reference) {

            const address = new NowAddress();
            address.line1 = this.getPromptValueByReference(allPromptsFromJudicialResults, address1Reference);
            address.line2 = this.getPromptValueByReference(allPromptsFromJudicialResults, address2Reference);
            address.line3 = this.getPromptValueByReference(allPromptsFromJudicialResults, address3Reference);
            address.line4 = this.getPromptValueByReference(allPromptsFromJudicialResults, address4Reference);
            address.line5 = this.getPromptValueByReference(allPromptsFromJudicialResults, address5Reference);
            address.postCode = this.getPromptValueByReference(allPromptsFromJudicialResults, postCodeReference);
            address.emailAddress1 = this.getPromptValueByReference(allPromptsFromJudicialResults, emailAddress1Reference);
            address.emailAddress2 = this.getPromptValueByReference(allPromptsFromJudicialResults, emailAddress2Reference);

            if(address.line1 || address.emailAddress1) {
                orderAddressee.address = address;
            }
        }
        return orderAddressee;
    }

    recipientFromCase() {
        const subscription = this.nowVariant.matchedSubscription;
        const orderAddressee = new OrderAddressee();
        const address = new NowAddress();

        if (subscription.recipient.isApplyDefenceOrganisationDetails) {
            const defenceOrganisation = this.getDefenceOrganisation();
            if (defenceOrganisation) {
                orderAddressee.name = defenceOrganisation.name;
                if (defenceOrganisation.address) {
                    address.line1 = defenceOrganisation.address.address1;
                    address.line2 = defenceOrganisation.address.address2;
                    address.line3 = defenceOrganisation.address.address3;
                    address.line4 = defenceOrganisation.address.address4;
                    address.line5 = defenceOrganisation.address.address5;
                    address.postCode = defenceOrganisation.address.postcode;
                }
                if (defenceOrganisation.contact) {
                    address.emailAddress1 = defenceOrganisation.contact.primaryEmail;
                    address.emailAddress2 = defenceOrganisation.contact.secondaryEmail;
                }
            }
        }

        if (subscription.recipient.isApplyParentGuardianDetails) {
            const parentGuardian = this.getParentGuardianDetails();
            if (parentGuardian) {
                orderAddressee.name = parentGuardian.name;
                address.line1 = parentGuardian.address1;
                address.line2 = parentGuardian.address2;
                address.line3 = parentGuardian.address3;
                address.line4 = parentGuardian.address4;
                address.line5 = parentGuardian.address5;
                address.postCode = parentGuardian.postCode;
                address.emailAddress1 = parentGuardian.emailAddress1;
                address.emailAddress2 = parentGuardian.emailAddress2;
            }
        }

        if (subscription.recipient.isApplyDefendantDetails) {
            const defendant = this.getDefendant();

            if (defendant.personDefendant) {
                orderAddressee.name = this.getFullNameOfPerson(defendant.personDefendant);
            } else if (defendant.legalEntityDefendant) {
                orderAddressee.name = defendant.legalEntityDefendant.organisation.name;
            }

            address.line1 = this.address1(defendant);
            address.line2 = this.address2(defendant);
            address.line3 = this.address3(defendant);
            address.line4 = this.address4(defendant);
            address.line5 = this.address5(defendant);
            address.postCode = this.postcode(defendant);

            address.emailAddress1 = this.primaryEmailAddress(defendant);
            address.emailAddress2 = this.secondaryEmailAddress(defendant);
        }

        if (subscription.recipient.isApplyDefendantCustodyDetails) {
            //TODO: currently we don't have custody address
            /*const defendant = this.getDefendant();
            if (defendant.personDefendant
                && defendant.personDefendant.defendantCustodyLocation) {
                const defendantCustodyLocation = defendant.personDefendant.defendantCustodyLocation;
                orderAddressee.name = defendantCustodyLocation.name;
                address = this.getAddress(defendantCustodyLocation);
            }*/
        }

        if (subscription.recipient.isApplyApplicantDetails) {
            const applicantFromHearingJson = this.getApplicantDetails();
            //Note:
            // 1. what happens if we have multiple court applications
            // 2. Applicant can be any one [Person, Organisation, organisationPersons,
            // prosecutingAuthority, representationOrganisation or defendant]
            /*if (applicantFromHearingJson.personDetails) {
                const personDetails = applicantFromHearingJson.personDetails;
                orderAddressee.name = this.name(personDetails);
                address = this.getAddress(personDetails);
            }

            if (!orderAddressee.name && applicantFromHearingJson.organisation) {
                const organisation = applicantFromHearingJson.organisation;
                orderAddressee.name = organisation.name;
                address = this.getAddress(organisation);
            }

            if (!orderAddressee.name && applicantFromHearingJson.organisationPersons && applicantFromHearingJson.organisationPersons.length) {
                const person = applicantFromHearingJson.organisationPersons[0].person;

                orderAddressee.name = this.name(person);
                address = this.getAddress(person);
                console.log("Address" + JSON.stringify(address));
            }

            if (!orderAddressee.name && applicantFromHearingJson.prosecutingAuthority) {
                const prosecutingAuthority = applicantFromHearingJson.prosecutingAuthority;
                orderAddressee.name = prosecutingAuthority.name;
                address = this.getAddress(prosecutingAuthority);
            }

            if (!orderAddressee.name && applicantFromHearingJson.representationOrganisation) {
                const organisation = applicantFromHearingJson.representationOrganisation;
                orderAddressee.name = organisation.name;
                address = this.getAddress(organisation);
            }
            */

            if (applicantFromHearingJson && applicantFromHearingJson.defendant) {
                const defendant = applicantFromHearingJson.defendant;
                if (defendant && defendant.personDefendant) {
                    orderAddressee.name = this.getFullNameOfPerson(defendant.personDefendant);
                    address.line1 = this.address1(defendant);
                    address.line2 = this.address2(defendant);
                    address.line3 = this.address3(defendant);
                    address.line4 = this.address4(defendant);
                    address.line5 = this.address5(defendant);
                    address.postCode = this.postcode(defendant);
                    address.emailAddress1 = this.primaryEmailAddress(defendant);
                    address.emailAddress2 = this.secondaryEmailAddress(defendant);
                }
            }
        }

        if (subscription.recipient.isApplyRespondentDetails) {
            //Note:
            // 1. what happens if we have multiple court applications
            // 2. Respondent can be [Empty, Person, Organisation, organisationPersons,
            // prosecutingAuthority, representationOrganisation or defendant] TODO
        }

        if (subscription.recipient.isApplyProsecutionAuthorityDetails) {
            const prosecutionCase = this.getProsecutionCase();
            if(prosecutionCase) {
                orderAddressee.name = prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityName;
            }
            if(prosecutionCase.prosecutionCaseIdentifier.address) {
                address.line1 = prosecutionCase.prosecutionCaseIdentifier.address.address1;
                address.line2 = prosecutionCase.prosecutionCaseIdentifier.address.address2;
                address.line3 = prosecutionCase.prosecutionCaseIdentifier.address.address3;
                address.line4 = prosecutionCase.prosecutionCaseIdentifier.address.address4;
                address.line5 = prosecutionCase.prosecutionCaseIdentifier.address.address5;
                address.postCode = prosecutionCase.prosecutionCaseIdentifier.address.postcode;
            }
            if (prosecutionCase.prosecutionCaseIdentifier.contact) {
                address.emailAddress1 = prosecutionCase.prosecutionCaseIdentifier.contact.primaryEmail;
                address.emailAddress2 = prosecutionCase.prosecutionCaseIdentifier.contact.secondaryEmail;
            }
        }

        if(address.line1 || address.emailAddress1) {
            orderAddressee.address = address;
        }
        return orderAddressee;
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
        } else if (defendant.legalEntityDefendant &&
                   defendant.legalEntityDefendant.organisation.address &&
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

    primaryEmailAddress(defendant) {
        if (this.isContactAvailable(defendant) &&
            defendant.personDefendant.personDetails.contact.primaryEmail) {
            return defendant.personDefendant.personDetails.contact.primaryEmail;
        } else if (this.isLegalEntityDefendantContactAvailable(defendant) &&
                   defendant.legalEntityDefendant.organisation.contact.primaryEmail) {
            return defendant.legalEntityDefendant.organisation.contact.primaryEmail;
        }
    }

    secondaryEmailAddress(defendant) {
        if (this.isContactAvailable(defendant) &&
            defendant.personDefendant.personDetails.contact.secondaryEmail) {
            return defendant.personDefendant.personDetails.contact.secondaryEmail;
        } else if (this.isLegalEntityDefendantContactAvailable(defendant) &&
                   defendant.legalEntityDefendant.organisation.contact.secondaryEmail) {
            return defendant.legalEntityDefendant.organisation.contact.secondaryEmail;
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
}

module.exports = OrderAddresseeMapper;
