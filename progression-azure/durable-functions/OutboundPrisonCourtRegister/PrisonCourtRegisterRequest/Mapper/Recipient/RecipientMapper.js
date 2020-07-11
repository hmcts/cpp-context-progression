const Recipient = require('../../Model/Recipient');
const _ = require('lodash');

class RecipientMapper {
    constructor(prisonCourtRegister, prisonsCustodySuitesRefData, hearingJson) {
        this.hearingJson = hearingJson;
        this.prisonCourtRegister = prisonCourtRegister;
        this.prisonsCustodySuitesRefData = prisonsCustodySuitesRefData;
    }

    build() {
        const recipients = [];
        if(this.prisonCourtRegister && this.prisonCourtRegister.matchedSubscriptions) {
            this.prisonCourtRegister.matchedSubscriptions.map((subscription) => {
                if(subscription.forDistribution && subscription.emailDelivery && subscription.recipient && subscription.recipient.recipientFromCase && subscription.recipient.isApplyDefendantCustodyDetails) {
                    const defendant = this.getDefendant();
                    const emails = this.getEmailAddresses(defendant);
                    if(emails && emails.length) {
                        const recipient = new Recipient();
                        recipient.recipientName = this.getRecipientName(defendant);
                        recipient.emailAddress1 = emails[0].emailAddress;
                        recipient.emailAddress2 = this.getEmailAddress2(emails);
                        recipient.emailTemplateName = subscription.emailTemplateName ? subscription.emailTemplateName : 'pcr_standard';
                        if (recipient.emailAddress1 !== undefined) {
                            recipients.push(recipient);
                        }
                    }
                }

                if(subscription.forDistribution && subscription.emailDelivery && subscription.recipient && subscription.recipient.recipientFromResults) {
                    if(this.prisonCourtRegister.matchedSubscriptions && this.prisonCourtRegister.matchedSubscriptions.length) {
                        this.prisonCourtRegister.matchedSubscriptions.forEach(matchedSubscription => {
                            const nameReference = matchedSubscription.recipient.organisationNameResultPromptReference;
                            const emailAddress1Reference = matchedSubscription.recipient.emailAddress1ResultPromptReference;
                            const emailAddress2Reference = matchedSubscription.recipient.emailAddress2ResultPromptReference;
                            const judicialResults = this.prisonCourtRegister.registerDefendant.results;

                            const allPromptsFromJudicialResults = [];
                            judicialResults.forEach(judicialResult => {
                                if (judicialResult.judicialResult.judicialResultPrompts
                                    && judicialResult.judicialResult.judicialResultPrompts.length) {
                                    judicialResult.judicialResult.judicialResultPrompts.forEach(
                                        prompt => {
                                            allPromptsFromJudicialResults.push(prompt);
                                        });
                                }
                            });

                            const recipient = new Recipient();
                            recipient.recipientName = this.getPromptValueByReference(allPromptsFromJudicialResults, nameReference);
                            recipient.emailAddress1 = this.getPromptValueByReference(allPromptsFromJudicialResults, emailAddress1Reference);
                            recipient.emailAddress2 = this.getPromptValueByReference(allPromptsFromJudicialResults, emailAddress2Reference);
                            recipient.emailTemplateName = subscription.emailTemplateName ? subscription.emailTemplateName : 'pcr_standard';

                            if (recipient.emailAddress1 !== undefined) {
                                recipients.push(recipient);
                            }
                        })
                    }
                }
            });
        }

        if(recipients.length) {
            return recipients;
        }
    }

    getPromptValueByReference(allPromptsFromJudicialResults, promptReference) {
        const matchingPromptReference = allPromptsFromJudicialResults && allPromptsFromJudicialResults.find(
            resultPrompt => resultPrompt.promptReference === promptReference
        );
        return matchingPromptReference && matchingPromptReference.value;
    }

    getEmailAddress2(emails) {
        if(emails.length > 1) {
            return emails[1].emailAddress;
        }
        return undefined;
    }

    getRecipientName(defendant) {
        if(defendant.personDefendant && defendant.personDefendant.custodialEstablishment) {
            return defendant.personDefendant.custodialEstablishment.name;
        }
        return undefined;
    }

    getEmailAddresses(defendant) {
        if(defendant.personDefendant && defendant.personDefendant.custodialEstablishment) {
            const custodialEstablishment = defendant.personDefendant.custodialEstablishment;
            const custodyLocation = this.prisonsCustodySuitesRefData['prisons-custody-suites'].find(prisonCustodySuite => prisonCustodySuite.id === custodialEstablishment.id);
            return custodyLocation.emails;
        }
    }

    getDefendant() {
        return _(this.hearingJson.prosecutionCases).flatMapDeep('defendants').value()
            .find(defendant => defendant.masterDefendantId
                               === this.prisonCourtRegister.registerDefendant.masterDefendantId);
    }
}

module.exports = RecipientMapper;