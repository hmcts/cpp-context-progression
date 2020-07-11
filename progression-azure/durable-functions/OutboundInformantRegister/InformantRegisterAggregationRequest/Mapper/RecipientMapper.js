const Recipient = require('../Model/Recipient');

class RecipientMapper {

    constructor(context, subscriptions) {
        this.context = context;
        this.subscriptions = subscriptions;
    }

    build() {
        const recipients = [];
        this.subscriptions.map((subscription) => {

            if(subscription.forDistribution && subscription.emailDelivery && subscription.recipient) {
                const recipient = new Recipient();
                recipient.recipientName = subscription.recipient.organisationName;
                recipient.emailAddress1 = subscription.recipient.emailAddress1;
                recipient.emailAddress2 = subscription.recipient.emailAddress2 ? subscription.recipient.emailAddress2 : undefined;
                recipient.emailTemplateName = subscription.emailTemplateName ? subscription.emailTemplateName : 'ir_standard';

                if (recipient.emailAddress1 !== undefined) {
                    recipients.push(recipient);
                }
            }

            if(subscription.firstClassLetterDelivery) {
                this.context.log('Informant Register does not support first class letter delivery distribution');
            }

            if(subscription.secondClassLetterDelivery) {
                this.context.log('Informant Register does not support second class letter delivery distribution');
            }
        });

        if(recipients.length) {
            return recipients;
        }
    }
}

module.exports = RecipientMapper;