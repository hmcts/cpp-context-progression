const Mapper = require('../Mapper');
const _ = require('lodash');
const { NowDistribution, EmailRenderingVocabulary} = require('../../Model/NowDistribution');

class NowDistributionMapper extends Mapper {
    constructor(nowVariant, hearingJson) {
        super(nowVariant, hearingJson);
        this.nowVariant = nowVariant;
        this.hearingJson = hearingJson;
    }

    buildNowDistribution() {
        const subscription = this.nowVariant.matchedSubscription;
        return this.getNowDistribution(subscription);
    }

    getNowDistribution(subscription) {

        if(subscription.firstClassLetterDelivery) {
            const nowDistribution = new NowDistribution()
            nowDistribution.firstClassLetter = subscription.firstClassLetterDelivery;
            return nowDistribution;
        }

        if(subscription.secondClassLetterDelivery) {
            const nowDistribution = new NowDistribution()
            nowDistribution.secondClassLetter = subscription.secondClassLetterDelivery;
            return nowDistribution;
        }

        if(subscription.emailDelivery) {
            const nowDistribution = new NowDistribution()
            nowDistribution.email = subscription.emailDelivery;
            nowDistribution.emailAddress = this.getEmailAddress(subscription);
            nowDistribution.emailTemplateName = subscription.emailTemplateName ? subscription.emailTemplateName : 'defaultEmailTemplate';
            nowDistribution.bilingualEmailTemplateName = undefined;
            nowDistribution.emailContent = this.getEmailContent();
            return nowDistribution;
        }

        return undefined;
    }

    getEmailContent(){
        const emailContent = [];
        const defendant = this.getDefendant();
        if(defendant.personDefendant) {
            const personDetails = defendant.personDefendant.personDetails;
            const courtName = this.hearingJson.courtCentre.name;
            const defendantName = [personDetails.firstName, personDetails.lastName].filter(item => item).join(' ').trim();
            const defendantDOB = personDetails.dateOfBirth;
            const caseReferences = [];
            const caseIds = Array.from(this.nowVariant.registerDefendant.cases);

            caseIds.forEach(caseId => {
                const prosecutionCase = this.getProsecutionCase(caseId);
                const reference = this.getReference(prosecutionCase);
                caseReferences.push(reference);
            })

            const caseNumbers = caseReferences.join(',');
            const personalisation = [courtName, defendantName, defendantDOB, caseNumbers].filter(item => item).join('; ').trim();
            emailContent.push(new EmailRenderingVocabulary('subject', 'DIGITAL WARRANT: ' + personalisation));

            if(emailContent.length) {
                return emailContent;
            }
        }
    }

    getProsecutionCase(caseId) {
        return _(this.hearingJson.prosecutionCases).value()
            .find(pcase => pcase.id === caseId);
    }

    getEmailAddress(subscription) {
        if(subscription.emailDelivery) {

            if(subscription.recipient.recipientFromCase) {
                if(subscription.recipient.isApplyDefenceOrganisationDetails) {
                    const defenceOrganisation = this.getDefenceOrganisation();
                    if(defenceOrganisation) {
                        if(defenceOrganisation.contact) {
                            return defenceOrganisation.contact.primaryEmail;
                        }
                    }
                }

                if(subscription.recipient.isApplyParentGuardianDetails) {
                    const parentGuardian = this.getParentGuardianDetails();
                    if(parentGuardian) {
                        if(parentGuardian.contact) {
                            return defenceOrganisation.contact.primaryEmail;
                        }
                    }
                }

                if(subscription.recipient.isApplyDefendantDetails) {
                    const defendant = this.getDefendant();
                    if(defendant) {
                        return this.primaryEmailAddress(defendant);
                    }
                }

                if(subscription.recipient.isApplyDefendantCustodyDetails) {
                    //TODO:
                }

                if(subscription.recipient.isApplyApplicantDetails) {
                    //TODO:
                }

                if(subscription.recipient.isApplyRespondentDetails) {
                    //TODO:
                }
            }

            if(subscription.recipient.recipientFromSubscription) {
                return subscription.recipient.emailAddress1;
            }

            if(subscription.recipient.recipientFromResults) {
                const emailAddress1Reference = subscription.recipient.emailAddress1ResultPromptReference;
                console.log('emailAddress1Reference ========== ' + emailAddress1Reference + ' ' + this.nowVariant.results.length);
                const allPromptsFromJudicialResults = [];
                this.nowVariant.results.forEach(judicialResult => {
                    if(judicialResult.judicialResultPrompts && judicialResult.judicialResultPrompts.length) {
                        judicialResult.judicialResultPrompts.forEach(prompt => {
                            allPromptsFromJudicialResults.push(prompt);
                        });
                    }
                });
                console.log('allPromptsFromJudicialResults ========== ' + JSON.stringify(allPromptsFromJudicialResults));
                return this.getPromptValueByReference(allPromptsFromJudicialResults, emailAddress1Reference);
            }
        }
    }
}

module.exports = NowDistributionMapper;
