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

        if(subscription.forDistribution && subscription.firstClassLetterDelivery) {
            const nowDistribution = new NowDistribution()
            nowDistribution.firstClassLetter = subscription.firstClassLetterDelivery;
            return nowDistribution;
        }

        if(subscription.forDistribution && subscription.secondClassLetterDelivery) {
            const nowDistribution = new NowDistribution()
            nowDistribution.secondClassLetter = subscription.secondClassLetterDelivery;
            return nowDistribution;
        }

        if(subscription.forDistribution && subscription.emailDelivery) {
            const nowDistribution = new NowDistribution()
            nowDistribution.email = subscription.emailDelivery;
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
}

module.exports = NowDistributionMapper;
