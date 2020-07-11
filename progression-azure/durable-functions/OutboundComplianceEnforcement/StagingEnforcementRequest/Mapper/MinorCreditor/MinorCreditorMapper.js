const MinorCreditor = require('../../Model/MinorCreditor');
const MinorCreditorConstants = require('./MinorCreditorConstants');

class MinorCreditorMapper {
    constructor(complianceEnforcement) {
        this.complianceEnforcement = complianceEnforcement;
    }

    buildMinorCreditor() {
        const minorCreditorResults = this.complianceEnforcement.minorCreditorResults;
        if(minorCreditorResults && minorCreditorResults.length) {
            const minorCreditors = [];
            minorCreditorResults.forEach(judicialResult => {
                if(judicialResult.judicialResultPrompts) {
                    const judicialResultPrompts = judicialResult.judicialResultPrompts;
                    const minorCreditor = new MinorCreditor();
                    const organisationName = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.ORGANISATION_NAME);

                    if(organisationName) {
                        minorCreditor.companyName = organisationName;
                    } else {
                        const firstName = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.FIRST_NAME);
                        const middleName = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.MIDDLE_NAME);
                        minorCreditor.minorCreditorSurname = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.LAST_NAME);
                        minorCreditor.minorCreditorForenames = this.foreNames(firstName, middleName);
                        minorCreditor.minorCreditorInitials = undefined;
                    }

                    minorCreditor.minorCreditorId = judicialResult.minorCreditorId;
                    minorCreditor.minorCreditorTitle = undefined;
                    minorCreditor.minorCreditorAddressLine1 = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.ADDRESS1);
                    minorCreditor.minorCreditorAddressLine2 = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.ADDRESS2);
                    minorCreditor.minorCreditorAddressLine3 = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.ADDRESS3);
                    minorCreditor.minorCreditorAddressLine4 = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.ADDRESS4);
                    minorCreditor.minorCreditorAddressLine5 = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.ADDRESS5);
                    minorCreditor.minorCreditorPostcode = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.POST_CODE);

                    minorCreditor.minorCreditorPayByBACS = 'N';
                    minorCreditor.bankAccountType = undefined;
                    minorCreditor.bankSortCode = undefined;
                    minorCreditor.bankAccountNumber = undefined;
                    minorCreditor.accountName = undefined;
                    minorCreditor.bankAccountReference = undefined;

                    if(minorCreditors.length && this.isDistinctMinorCreditor(minorCreditors, minorCreditor)) {
                        minorCreditors.push(minorCreditor);
                    }
                    if(!minorCreditors.length) {
                        minorCreditors.push(minorCreditor);
                    }

                }
            });

            if(minorCreditors.length) {
                return minorCreditors;
            }
        }
    }

    foreNames(firstName, middleName) {
        return [firstName, middleName].filter(item => item).join(' ').trim();
    }

    getPromptValue(judicialResultPrompts, promptReference) {
        const matchingPrompt = judicialResultPrompts.find(prompt => prompt.promptReference && prompt.promptReference === promptReference);
        if(matchingPrompt && matchingPrompt.value) {
            return matchingPrompt.value;
        }
    }

    isDistinctMinorCreditor(minorCreditors, currentMinorCreditor) {
        let matchingMinorCreditor;
        if(currentMinorCreditor.companyName) {
            matchingMinorCreditor = minorCreditors.find(creditor =>
                                                            creditor.companyName === currentMinorCreditor.companyName &&
                                                            creditor.minorCreditorAddressLine1 === currentMinorCreditor.minorCreditorAddressLine1 &&
                                                            creditor.minorCreditorAddressLine2 === currentMinorCreditor.minorCreditorAddressLine2 &&
                                                            creditor.minorCreditorAddressLine3 === currentMinorCreditor.minorCreditorAddressLine3 &&
                                                            creditor.minorCreditorAddressLine4 === currentMinorCreditor.minorCreditorAddressLine4 &&
                                                            creditor.minorCreditorAddressLine5 === currentMinorCreditor.minorCreditorAddressLine5 &&
                                                            creditor.minorCreditorPostcode === currentMinorCreditor.minorCreditorPostcode);
        } else {
            matchingMinorCreditor = minorCreditors.find(creditor =>
                                                            creditor.minorCreditorForenames === currentMinorCreditor.minorCreditorForenames &&
                                                            creditor.minorCreditorSurname === currentMinorCreditor.minorCreditorSurname &&
                                                            creditor.minorCreditorAddressLine1 === currentMinorCreditor.minorCreditorAddressLine1 &&
                                                            creditor.minorCreditorAddressLine2 === currentMinorCreditor.minorCreditorAddressLine2 &&
                                                            creditor.minorCreditorAddressLine3 === currentMinorCreditor.minorCreditorAddressLine3 &&
                                                            creditor.minorCreditorAddressLine4 === currentMinorCreditor.minorCreditorAddressLine4 &&
                                                            creditor.minorCreditorAddressLine5 === currentMinorCreditor.minorCreditorAddressLine5 &&
                                                            creditor.minorCreditorPostcode === currentMinorCreditor.minorCreditorPostcode);
        }

        if(!matchingMinorCreditor) {
            return true;
        }
    }
}

module.exports = MinorCreditorMapper;