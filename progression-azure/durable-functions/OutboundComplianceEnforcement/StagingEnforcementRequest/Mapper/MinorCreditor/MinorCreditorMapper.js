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
                        minorCreditor.minorCreditorTitle = 'CO';
                        minorCreditor.companyName = organisationName;
                    } else {
                        const firstName = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.FIRST_NAME);
                        const middleName = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.MIDDLE_NAME);
                        minorCreditor.minorCreditorSurname = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.LAST_NAME);
                        minorCreditor.minorCreditorForenames = this.foreNames(firstName, middleName);
                        minorCreditor.minorCreditorInitials = undefined;
                        minorCreditor.minorCreditorTitle = undefined;
                    }

                    minorCreditor.minorCreditorId = judicialResult.minorCreditorId;
                    minorCreditor.minorCreditorAddress1 = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.ADDRESS1);
                    minorCreditor.minorCreditorAddress2 = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.ADDRESS2);
                    minorCreditor.minorCreditorAddress3 = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.ADDRESS3);
                    minorCreditor.minorCreditorAddress4 = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.ADDRESS4);
                    minorCreditor.minorCreditorAddress5 = this.getPromptValue(judicialResultPrompts, MinorCreditorConstants.ADDRESS5);
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
                                                            creditor.minorCreditorAddress1 === currentMinorCreditor.minorCreditorAddress1 &&
                                                            creditor.minorCreditorAddress2 === currentMinorCreditor.minorCreditorAddress2 &&
                                                            creditor.minorCreditorAddress3 === currentMinorCreditor.minorCreditorAddress3 &&
                                                            creditor.minorCreditorAddress4 === currentMinorCreditor.minorCreditorAddress4 &&
                                                            creditor.minorCreditorAddress5 === currentMinorCreditor.minorCreditorAddress5 &&
                                                            creditor.minorCreditorPostcode === currentMinorCreditor.minorCreditorPostcode);
        } else {
            matchingMinorCreditor = minorCreditors.find(creditor =>
                                                            creditor.minorCreditorForenames === currentMinorCreditor.minorCreditorForenames &&
                                                            creditor.minorCreditorSurname === currentMinorCreditor.minorCreditorSurname &&
                                                            creditor.minorCreditorAddress1 === currentMinorCreditor.minorCreditorAddress1 &&
                                                            creditor.minorCreditorAddress2 === currentMinorCreditor.minorCreditorAddress2 &&
                                                            creditor.minorCreditorAddress3 === currentMinorCreditor.minorCreditorAddress3 &&
                                                            creditor.minorCreditorAddress4 === currentMinorCreditor.minorCreditorAddress4 &&
                                                            creditor.minorCreditorAddress5 === currentMinorCreditor.minorCreditorAddress5 &&
                                                            creditor.minorCreditorPostcode === currentMinorCreditor.minorCreditorPostcode);
        }

        if(!matchingMinorCreditor) {
            return true;
        }
    }
}

module.exports = MinorCreditorMapper;